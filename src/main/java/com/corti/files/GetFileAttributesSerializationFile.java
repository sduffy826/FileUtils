package com.corti.files;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
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

public class GetFileAttributesSerializationFile {
  private static final int num2Process = -1;  // -1 to do all records
  private static final boolean DEBUGIT = false;
  
  private Logger logger;
  private Instant startInstant;
  private List<Path> fileList;
  private List<FileAttributes> fileAttributeList;
  private String startingPath; 
  
  // Default constructor
  GetFileAttributesSerializationFile() { }
  
  // Mainline
  public static void main(String[] args) throws Exception {
    GetFileAttributesSerializationFile me = new GetFileAttributesSerializationFile();                      
    me.runIt(args);
  }
  
  // Main processing
  public void runIt(String[] args) throws Exception {      
    logger = (new LoggerUtils()).getLogger("SerializationLogger", "GetFileAttributesSerializationFile");

    startInstant = null;
    
    fileList          = new ArrayList<Path>(2000);            // List of files
    fileAttributeList = new ArrayList<FileAttributes>(2000);  // List of file attribute objects
    
    startingPath = (args.length > 0 ? args[0] : "c:/seanduff/workspace");
    FileSystem fileSystem = FileSystems.getDefault();
    List<PathMatcher> pathsToIgnore = new ArrayList<PathMatcher>(5);
    
    pathsToIgnore.add(fileSystem.getPathMatcher("glob:**/target*"));
    pathsToIgnore.add(fileSystem.getPathMatcher("glob:**/workspace/.metadata*"));
    pathsToIgnore.add(fileSystem.getPathMatcher("glob:**/workspace/.recommenders*"));
    
    Path serializationPath = Paths.get(args.length > 1 ? args[1] : "FileAttributesSerialized.ser" );
    
    // Define base path and set 'startingAbsolutePath'; that's important... for lookup purposes
    //   to compare to other machines we only want to search from the path we start with
    //   going down.  The method FileAttributes.getPathFromBaseAsUnix returns the absolute
    //   path of a file with the 'startingAbsolutePath' removed, i.e. if startingAbsolutePath
    //   was c:\\seanduff\\workspace and we have file c:\\seanduffy\workspace\TestIO\src\test.java
    //   then method above would return TestIO/src/test.java
    //--------------------------------------------------------------------------------------------
    Path basePath               = Paths.get(startingPath);
    String startingAbsolutePath =  basePath.toAbsolutePath().toString();
     
    addPathsFromPath(basePath, fileList, pathsToIgnore);
    
    // If it has more data than we want to process then trim list to size we want
    if (num2Process > 0 && fileList.size() > num2Process)  
      fileList = fileList.subList(0, num2Process);
  
    logger.info("Size of fileList is: " + fileList.size());    
        
    // Set starting timer
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
   
    // Test serialization and deserialization
    serializeFileAttributesList(fileAttributeList,serializationPath); 
    System.out.println("Done, serialization file written to: " + serializationPath.toAbsolutePath().toString());
  }
      
  // Get all the files from the path passed in, will recurse down... we will ignore the paths
  //   that match the ones specified in paths2Ignore
  private void addPathsFromPath(Path _dirPath, List<Path> pathList, List<PathMatcher> paths2Ignore) {
    boolean skipIt;
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(_dirPath)) {
      for (Path entry : stream) {
        if (Files.isDirectory(entry)) {
          skipIt = false;
          for (PathMatcher pathMatcher: paths2Ignore) {
            if (pathMatcher.matches(entry.toAbsolutePath()) == true ) 
              skipIt = true;
          }
          if (skipIt == false)
            addPathsFromPath(entry,pathList, paths2Ignore);
        }
        else {
          pathList.add(entry);
        }                
      }
    } catch (IOException e) {      
      e.printStackTrace();
    }
  }
  
  // 
  public static void printDetails(FileStore fs, Class<? extends FileAttributeView> attribClass) {
    boolean isSupported = fs.supportsFileAttributeView(attribClass);
    System.out.format("%s is supported %s%n", attribClass.getSimpleName(), isSupported);    
  } 
  
  // Get elapsed time, you call this at the start of initialization and then at end; if
  //   you call it multiple times then the elapsed is cumulative from the beginning
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
  
  //Serialize a FileAttributes list to the filename passed in
  public void serializeFileAttributesList(List<FileAttributes> fileAttributesList, Path outputPath) {
    try (FileOutputStream fos   = new FileOutputStream(outputPath.toFile());
         ObjectOutputStream oos = new ObjectOutputStream(fos)) {
      oos.writeObject(fileAttributesList);
    } catch (IOException e) {
        e.printStackTrace();
    }
  }  
}
