package com.corti.files;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.corti.javalogger.LoggerUtils;

public class CompareFileAttributesInSerializedFiles {
 
  private Logger logger;
  private Instant startInstant;
  private Path firstPath, secondPath; 
  
  // Default constructor
  private CompareFileAttributesInSerializedFiles() { }
  
  // 
  public CompareFileAttributesInSerializedFiles(Path _firstPath, Path _secondPath) {
    this.firstPath  = _firstPath;
    this.secondPath = _secondPath;
  }
  
  public void performCompare() throws Exception {      
    logger       = (new LoggerUtils()).getLogger("TestDosAttributes", "TestDosAttributesLogger");
    startInstant = null;

    logger.info("Starting deserialization");    
    getElapsedTimeInMilliseconds(true);
    
    List<FileAttributes> firstFileAttributesList =
      (ArrayList<FileAttributes>) SerializeOrDeserializeFileAttributes.deserializeFileAttributesList(firstPath);
    List<FileAttributes> secondFileAttributesList =
      (ArrayList<FileAttributes>) SerializeOrDeserializeFileAttributes.deserializeFileAttributesList(secondPath);

    // Define lists for results
    List<Integer> inFirstNotSecond = new ArrayList<Integer>(2000);
    List<Integer> inSecondNotFirst = new ArrayList<Integer>(2000);
    List<PairOfInts> inBothButDiff = new ArrayList<PairOfInts>(2000);
        
    Double elapsedMillis = new Long(getElapsedTimeInMilliseconds(false)).doubleValue();
    logger.info("Deserialization done, elapsed time: " + Double.toString(elapsedMillis/1000.0));

    compArrays(firstFileAttributesList, secondFileAttributesList,
               inFirstNotSecond, inSecondNotFirst, inBothButDiff);
    
    elapsedMillis = new Long(getElapsedTimeInMilliseconds(false)).doubleValue();
    logger.info("Done comparing, elapsed time: " + Double.toString(elapsedMillis/1000.0));
    
    // Now for the results
    dumpMissing("In_"+firstPath.getFileName()+"_not"+secondPath.getFileName(),
                inFirstNotSecond, firstFileAttributesList);
    dumpMissing("In_"+secondPath.getFileName()+"_not_"+firstPath.getFileName(),
                 inSecondNotFirst, secondFileAttributesList);
    
    Path diffPath = Paths.get("differencesFromFileOfSerializedFileAttributes.txt");
    System.out.println("Writing differences to: " + diffPath.getFileName());
    try(BufferedWriter writer = Files.newBufferedWriter(diffPath))  {
      for (PairOfInts pairOfInts : inBothButDiff) {
        writer.write(firstFileAttributesList.get(pairOfInts.a).getPathFromBaseAsUnix()
                     + "," + firstFileAttributesList.get(pairOfInts.a).getFileExtension()
                     + "," + firstFileAttributesList.get(pairOfInts.a).getLastModifiedTime().toString()
                     + "," + secondFileAttributesList.get(pairOfInts.b).getLastModifiedTime().toString());
        writer.newLine();
      }
    }
    System.out.println("Done");
    
  }    
  
  // Compare the arrays, basically call helpers to setup map and do the compare
  public void compArrays(List<FileAttributes> beforeList, List<FileAttributes> afterList,
                         List<Integer> inBeforeNotAfter,
                         List<Integer> inAfterNotBefore,
                         List<PairOfInts> inBothButDiff) {
    Map<String, Integer> fileNameAndPathMapBefore = new HashMap<String, Integer>();
    Map<String, Integer> fileNameAndPathMapAfter  = new HashMap<String, Integer>();
    
    setMapArray(beforeList, fileNameAndPathMapBefore);
    setMapArray(afterList, fileNameAndPathMapAfter);
    
    compareList(beforeList, fileNameAndPathMapAfter, afterList,
                inBeforeNotAfter, inBothButDiff, true);
    compareList(afterList, fileNameAndPathMapBefore, beforeList,
        inAfterNotBefore, null, false);
  }
    
  // Compare lists, the arguments are:
  //   sourceList ------------ The source FileAttributes list
  //   targetMap ------------- The map to lookup the fileAndPath in
  //   targetList ------------ The target FileAttributes list
  //   listOfRecordsNotFound - Has the index from sourceList of records not found in the targetMap
  //   listOfChangedRecords -- This list has the sourceIndex and targetIndex of records that differ
  //   compareForChanges ----- Boolean to see if we should look for changes 
  private void compareList(List<FileAttributes> sourceList, 
                           Map<String,Integer> targetMap, 
                           List<FileAttributes> targetList,
                           List<Integer> listOfRecordsNotFound, 
                           List<PairOfInts> listOfRecordsChanged, 
                           boolean compareForChanges) {    
    Integer thePos = null;
    for (int i = 0; i < sourceList.size(); i++) {
      String key2Search = sourceList.get(i).getPathFromBaseAsUnix();
      thePos = targetMap.get(key2Search);
      if (thePos != null) {
        if (compareForChanges) {
          if (fileAttributesDiffer(sourceList.get(i), targetList.get(thePos.intValue())) == true) {
            // Add record to array, we put index positions from source and target FileAttribute arrays
            listOfRecordsChanged.add(new PairOfInts(i, thePos.intValue()));
          }          
        }
      }
      else {
        // Not found add to list of not found
        listOfRecordsNotFound.add(i);
      }
    }       
  }

  // Write out the missing records to the filename passed in
  private void dumpMissing(String fileNameToOutput, 
                           List<Integer> missingRecords, 
                           List<FileAttributes> fattr) throws Exception {
    Path path = Paths.get(fileNameToOutput);  
    if (Files.exists(path) == true) {
      System.out.println("File exists");
      throw(new IOException("file exists: " + fileNameToOutput));
    }
    System.out.println("Writing missing files to: " + fileNameToOutput);
    try (BufferedWriter writer = Files.newBufferedWriter(path)) {
      for (Integer thePos : missingRecords) {
        writer.write(fattr.get(thePos.intValue()).absolutePath
                     + "," + fattr.get(thePos.intValue()).getFileExtension());
        writer.newLine();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }    
  }
  
  private boolean fileAttributesDiffer(FileAttributes fa1, FileAttributes fa2) {  
    // Treat nulls like empty string
    String checkSum1 = fa1.getCheckSumValue();
    String checkSum2 = fa2.getCheckSumValue();
    if (checkSum1 == null) checkSum1 = "";
    if (checkSum2 == null) checkSum2 = "";
    
    if (checkSum1.compareTo(checkSum2) != 0) return true;
    if (fa1.getLastModifiedTime().toMillis() != fa2.getLastModifiedTime().toMillis()) return true;
    return false;
  }
      
  private long getElapsedTimeInMilliseconds(boolean startIt) {
    if (startIt || startInstant == null) {
      startInstant = Instant.now(); 
      return 0;
    }
    else {
      Instant ending = Instant.now();
      return Duration.between(startInstant,  ending).toMillis();
    }
  } 
  

  // Helper method to set the map to have fileNameAndPath as key and index position of the record in the
  //   fileAttributesList
  private void setMapArray(List<FileAttributes> fileAttributesList, Map<String, Integer> fileNameAndPathMap) {    
    for (int i = 0; i < fileAttributesList.size(); i++) {
      String key2Search = fileAttributesList.get(i).getPathFromBaseAsUnix();
      fileNameAndPathMap.put(key2Search, Integer.valueOf(i));
    }    
  }
}
