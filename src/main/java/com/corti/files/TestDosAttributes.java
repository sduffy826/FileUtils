package com.corti.files;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.corti.javalogger.LoggerUtils;
import com.corti.jsonutils.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.FileOwnerAttributeView;

public class TestDosAttributes {
  private static final int num2Process = 5;  // -1 to do all records
  private static final boolean DEBUGIT = true;
  
  private Logger logger;
  private JsonUtils jsonUtils;
  private Instant startInstant;
  private List<Path> fileList;
  private List<FileAttributes> fileAttributeList;
  private List<String> jsonObjectList;
  private String startingPath; 
  
  // Default constructor
  TestDosAttributes() { }
  
  // Mainline
  public static void main(String[] args) throws Exception {
    TestDosAttributes me = new TestDosAttributes();
    System.out.println("File separator: " + System.getProperty("file.separator") +
                       " File.pathSeparator: " + File.pathSeparator +
                       " File.separator: " + File.separator );
                       
    me.runIt(args);
  }
  
  public void runIt(String[] args) throws Exception {      
    logger    = (new LoggerUtils()).getLogger("TestDosAttributes", "TestDosAttributesLogger");
    jsonUtils = new JsonUtils();
    jsonUtils.setFailOnUnknowProperties(false);
    
    startInstant = null;
    
    fileList          = new ArrayList<Path>(2000);            // List of files
    fileAttributeList = new ArrayList<FileAttributes>(2000);  // List of file attribute objects
    jsonObjectList    = new ArrayList<String>(2000);          // List of Json FileAttributes for the files
    
    startingPath = (args.length > 0 ? args[0] : "c:/seanduff/workspace/TestIO/src");

    // Add all the paths to fileList
    Path basePath = Paths.get(startingPath);
    String startingAbsolutePath =  basePath.toAbsolutePath().toString();
     
    addPathsFromPath(basePath, fileList);
    
    // If it has more data than we want to process trim it
    if (num2Process > 0 && fileList.size() > num2Process)  
      fileList = fileList.subList(0, num2Process);
  
    logger.info("Size of fileList is: " + fileList.size());    
        
    getElapsedTimeInMilliseconds(true);
        
    // Process each file in the list 
    for (Path thePath : fileList) {
      try {         
        FileStore fs = Files.getFileStore(thePath);
        FileAttributes fileAttributes;
        if (fs.supportsFileAttributeView(PosixFileAttributeView.class)) {
          fileAttributes = new UnixFileAttributes(thePath);
        }
        else {
          fileAttributes = new DosFileAttributes(thePath);
        }
        fileAttributes.setStartingBasePath(startingAbsolutePath);
        fileAttributeList.add(fileAttributes);
        if (DEBUGIT) System.out.println("fileAttributes: " + fileAttributes.toString());
      } catch (Exception e) {
        System.out.println("Exception raised with " + thePath.toString());
        e.printStackTrace();
      }
    }
  
    Double elapsedMillis = new Long(getElapsedTimeInMilliseconds(false)).doubleValue();
    logger.info("Elapsed time: " + Double.toString(elapsedMillis/1000.0));
    
    logger.info("Converting FileAttributes to Json Strings");
    // Now we convert them to json and put them into the jsonObjectList
    for (FileAttributes fileAttributes : fileAttributeList) {
      jsonObjectList.add(jsonUtils.getJsonStringFromPojo(fileAttributes));
    }
    
    elapsedMillis = new Long(getElapsedTimeInMilliseconds(false)).doubleValue();
    logger.info("Total elapsed time: " + Double.toString(elapsedMillis/1000.0));
    
    logger.info("Converting json strings back to FileAttribute pojo's");
    
    // ------------------------------------------------------------------
    // Down here is for testing, we'll convert the json objects back into
    //   FileAttributes
    // ------------------------------------------------------------------    
    List<FileAttributes> fileAttributesList = new ArrayList<FileAttributes>(10);
    
    // Loop thru jsonObjectList and convert to FileAttributes
    for (int i = 0; i < jsonObjectList.size(); i++) {
      String tempString = jsonObjectList.get(i);      
      JsonNode jsonNode = jsonUtils.getJsonNodeForJsonString(tempString);
      
      if (DEBUGIT) logger.info(tempString);;
      if (DEBUGIT) logger.info(jsonUtils.prettifyIt(tempString));   
      
      // Get the name of the class that we should use to create the pojo... the
      //   FileAttributes is abstract, the concrete class is a child of it
      String fullClassName = jsonNode.get("className").asText();
      
      Class<?> someClass = Class.forName(fullClassName);
      if (DEBUGIT) logger.info("someClass: " + someClass.getName());
            
      String classNm = fullClassName+".class";
      if (DEBUGIT) logger.info("classNm: " + classNm);
      FileAttributes fa = (FileAttributes) jsonUtils.getPojoFromJsonNode(jsonNode, someClass);
      
      // Write the object created
      if (DEBUGIT) logger.info("Deserialized obj: " + fa.toString());
      fileAttributesList.add(fa);
    }
        
    elapsedMillis = new Long(getElapsedTimeInMilliseconds(false)).doubleValue();
    logger.info("Total elapsed time: " + Double.toString(elapsedMillis/1000.0));
  
    compArrays(fileAttributeList,fileAttributesList);    
  
    // Test serialization and deserialization
    serializeFileAttributesList(fileAttributeList,"fileAttributesSerialized.ser");
    
    List<FileAttributes> newFileAttributesList =
        (ArrayList<FileAttributes>) deserializeFileAttributesList("fileAttributesSerialized.ser");
    compArrays(fileAttributeList,newFileAttributesList);
  }
    
  public void compArrays(List<FileAttributes> beforeList, List<FileAttributes> afterList) {
     
    // The maps return the index position in the list, that position has a list of the
    //   index positions of the records in the file attributes list.
    Map<String, Integer> fileNamesMapBefore       = new HashMap<String, Integer>();
    Map<String, Integer> fileNamesMapAfter        = new HashMap<String, Integer>();
    Map<String, Integer> fileNameAndPathMapAfter  = new HashMap<String, Integer>();
    Map<String, Integer> fileNameAndPathMapBefore = new HashMap<String, Integer>();
    Map<String, Integer> checkSumsMapBefore       = new HashMap<String, Integer>();
    Map<String, Integer> checkSumsMapAfter        = new HashMap<String, Integer>();
    
    List<ArrayList<Integer>> fileNamesListBefore = new ArrayList<ArrayList<Integer>>();
    List<ArrayList<Integer>> fileNamesListAfter = new ArrayList<ArrayList<Integer>>();  
    List<ArrayList<Integer>> checkSumsListBefore = new ArrayList<ArrayList<Integer>>();
    List<ArrayList<Integer>> checkSumsListAfter = new ArrayList<ArrayList<Integer>>();
    
    System.out.println("Creating arrays for compare");
    
    // Build array's to compare
    setArrays(beforeList, 
                fileNamesMapBefore, fileNameAndPathMapBefore, checkSumsMapBefore, 
                fileNamesListBefore, checkSumsListBefore);
    setArrays(afterList, 
                fileNamesMapAfter, fileNameAndPathMapAfter, checkSumsMapAfter, 
                fileNamesListAfter, checkSumsListAfter);  
    
    System.out.println("Checking duplicate filenames");
    checkDupeNames(beforeList, fileNamesMapBefore, fileNamesListBefore);
    
    System.out.println("Checking duplicate checksums");
    checkDupeCheckSums(beforeList, checkSumsMapBefore, checkSumsListBefore);
    
  }
  
  // Write out all the files that have duplicate names (case insensative
  private void checkDupeNames(List<FileAttributes> fileAttributesList, Map<String, Integer> fileNamesMap,
                              List<ArrayList<Integer>> fileNamesList) {

    // First want to see duplicate filenames
    for (FileAttributes fileAttributes: fileAttributesList) {
      String key2Search = fileAttributes.getFileName().toLowerCase();
      Integer thePos = fileNamesMap.get(key2Search);
      if (thePos != null) {
        int arraySize = fileNamesList.get(thePos.intValue()).size();
        if (arraySize > 1) {
          System.out.println("File name duplication at:");
          for (int i = 0; i < arraySize; i++) {
            int idxPos = fileNamesList.get(thePos.intValue()).get(i);
            System.out.println("  " + fileAttributesList.get(idxPos).getAbsolutePath());
          }
        }
      }
      else System.out.println("File not in map: " + fileAttributes.getAbsolutePath());
    }
  }
  
  // Write out all the files that have duplicate checksum values
  private void checkDupeCheckSums(List<FileAttributes> fileAttributesList,
                                  Map<String, Integer> checkSumsMap,
                                  List<ArrayList<Integer>> checkSumsList) {

    Integer thePos;
    // First want to see duplicate filenames
    for (FileAttributes fileAttributes: fileAttributesList) {
      String key2Search = fileAttributes.getCheckSumValue();
      thePos = null;
      if (key2Search != null) thePos = checkSumsMap.get(key2Search);
      if (thePos != null) {
        int arraySize = checkSumsList.get(thePos.intValue()).size();
        if (arraySize > 1) {
          System.out.println("File has duplicate checksums at:");
          for (int i = 0; i < arraySize; i++) {
            int idxPos = checkSumsList.get(thePos.intValue()).get(i);
            System.out.println("  " + fileAttributesList.get(idxPos).getAbsolutePath());
          }
        }
      }
      else System.out.println("File not in map: " + fileAttributes.getAbsolutePath());
    }     
  }
  
  // This takes the FileAttribute list passed in and creates the following
  //   fileNamesMap ------- has a map of the fileName and the index position of the item in the
  //                          the two dimensional array fileNamesList (note for this the case
  //                          of the filename doesn't matter
  //   fileNameAndPathMap - had a map of the fileNameAndPath, this is the name and path from
  //                          the starting base... i.e. it's not the absolute path, it's the path
  //                          relative from the starting base.  This fileNameAndPath is used
  //                          to find the same file on another machine or path
  //   checkSumsMap ------- this is a map of the files checksum, the value that it points to is
  //                          the index position in checkSumsList 
  //   fileNamesList ------ This is a two dimentional array, the first dimension is the value
  //                          from fileNamesMap, the second is an array of integers, they hold
  //                          the index position of associated record in the FileAttribute list.
  //                          The reason it's a list is that there may be multiple records in the
  //                          FileAttributes list with the same filename, and I wanted to identify
  //                          all of them.
  //   checkSumsList ------ This is also a multi dimentional array, the first pos contains the
  //                          value from checkSumsMap, the second dimension is an array that
  //                          has the index position within FileAttributes list of the records
  //                          that have this checksum value.
  // --------------------------------------------------------------------------------------------
  private void setArrays(List<FileAttributes> fileAttributesList, 
                         Map<String, Integer> fileNamesMap,
                         Map<String, Integer> fileNameAndPathMap,
                         Map<String, Integer> checkSumsMap, 
                         List<ArrayList<Integer>> fileNamesList,
                         List<ArrayList<Integer>> checkSumsList) {
    for (int i = 0; i < fileAttributesList.size(); i++) {
      // Process filename first
      String key2Search = fileAttributesList.get(i).getFileName().toLowerCase();
      Integer thePos = fileNamesMap.get(key2Search);
      if (thePos == null) {
        // Add new element to the fileNames array
        fileNamesList.add(new ArrayList<Integer>());
        thePos = fileNamesList.size()-1;
        fileNamesMap.put(key2Search, thePos);
      }
      if (DEBUGIT) System.out.println("Key: " + key2Search + " ThePos: " + thePos.intValue());
      fileNamesList.get(thePos.intValue()).add(i);
      
      // Have map of the path and name from the starting path, this is the key that
      //   we'll use to check other systems.
      key2Search = fileAttributesList.get(i).getPathFromBaseAsUnix();
      fileNameAndPathMap.put(key2Search, Integer.valueOf(i));
            
      // Now process the checksum value
      key2Search = fileAttributesList.get(i).getCheckSumValue();
      if (key2Search != null) {
        thePos = checkSumsMap.get(key2Search);
        if (thePos == null) {
          // Add new element to the fileNames array
          checkSumsList.add(new ArrayList<Integer>());
          thePos = checkSumsList.size()-1;
          checkSumsMap.put(key2Search, thePos);
        }
        if (DEBUGIT) System.out.println("Key: " + key2Search + " ThePos: " + thePos.intValue());
        checkSumsList.get(thePos.intValue()).add(i);          
      }      
    }    
  }
  
  // Get all the files from the path passed in, will recurse depth
  private void addPathsFromPath(Path _dirPath, List<Path> pathList) {
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(_dirPath)) {
      for (Path entry : stream) {
        if (Files.isDirectory(entry)) {
          addPathsFromPath(entry,pathList);
        }
        else {
          pathList.add(entry);
        }                
      }
    } catch (IOException e) {      
      e.printStackTrace();
    }
  }
  
  // Serialize a FileAttributes list to the filename passed in
  public void serializeFileAttributesList(List<FileAttributes> fileAttributesList, String outputFile) {
    try
    {
      FileOutputStream fos = new FileOutputStream(outputFile);
      ObjectOutputStream oos = new ObjectOutputStream(fos);
      oos.writeObject(fileAttributesList);
      oos.close();
      fos.close();
    } catch (IOException e) {
        e.printStackTrace();
    }
  }
  
  public List<FileAttributes> deserializeFileAttributesList(String inputFile) {
    ArrayList<FileAttributes> fileAttributesList = new ArrayList<FileAttributes>(2000);
    try {
      FileInputStream fis   = new FileInputStream(inputFile);
      ObjectInputStream ois = new ObjectInputStream(fis);

      fileAttributeList = (ArrayList) ois.readObject();

      ois.close();
      fis.close();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ClassNotFoundException c) {
      System.out.println("Class not found");
      c.printStackTrace();
    }
    return fileAttributeList;
  }
    
  public static void printDetails(FileStore fs, Class<? extends FileAttributeView> attribClass) {
    boolean isSupported = fs.supportsFileAttributeView(attribClass);
    System.out.format("%s is supported %s%n", attribClass.getSimpleName(), isSupported);    
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
}
