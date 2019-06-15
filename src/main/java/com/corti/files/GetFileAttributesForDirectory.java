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

public class GetFileAttributesForDirectory {
  private static final int num2Process = -1;  // -1 to do all records
  private static final boolean DEBUGIT = false;
  
  private List<Path> fileList;
  private List<FileAttributes> fileAttributeList;
  private Path startingPath; 
  
  // Default constructor
  private GetFileAttributesForDirectory() { }
  
  public GetFileAttributesForDirectory(Path startingPath) {
    this.startingPath = startingPath;    
  }
  
  // Main processing
  public List<FileAttributes> getFiles() throws Exception {          
    fileList          = new ArrayList<Path>(500);            // List of files
    fileAttributeList = new ArrayList<FileAttributes>(500);  // List of file attribute objects
    
    FileSystem fileSystem = FileSystems.getDefault();
    
    // Define base path and set 'startingAbsolutePath'; that's important... for lookup purposes
    //   to compare to other machines we only want to search from the path we start with
    //   going down.  The method FileAttributes.getPathFromBaseAsUnix returns the absolute
    //   path of a file with the 'startingAbsolutePath' removed, i.e. if startingAbsolutePath
    //   was c:\\seanduff\\workspace and we have file c:\\seanduffy\workspace\TestIO\src\test.java
    //   then method above would return TestIO/src/test.java
    //--------------------------------------------------------------------------------------------
    String startingAbsolutePath = startingPath.toAbsolutePath().toString();
     
    addPathsFromPath(startingPath, fileList);
    
    // If it has more data than we want to process then trim list to size we want
    if (num2Process > 0 && fileList.size() > num2Process)  
      fileList = fileList.subList(0, num2Process);
  
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
    return fileAttributeList;
  }
      
  // Get all the files for the path passed in, we don't recurse down
  private void addPathsFromPath(Path _dirPath, List<Path> pathList) {
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(_dirPath)) {
      for (Path entry : stream) {
        if (Files.isDirectory(entry) == false) {
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
}
