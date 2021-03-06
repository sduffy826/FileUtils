package com.corti.files;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.corti.files.GetDirectoriesFromPath;

class GetFileAttributesForDirectoryTest {
 
  @Test
  void testPath2IncludeAndFiles() throws Exception {
    
    //Path startingPath = Paths.get("/home/dev/workspace/");
    Path startingPath = Paths.get("./");
    
    // This tests paths to be included in the search, for this one we show how you
    //   can include paths and ignore the 'case' of the directory name
    GetDirectoriesFromPath me = new GetDirectoriesFromPath(startingPath);
    me.setDebugFlag(true);
    me.setMaxDepth(10);
    me.setPathMatcherIgnoreCase(true);
    
    // me.setPaths2Include("glob:**/FileUtils/**");  // This won't match the FileUtils directory
    me.setPaths2Include("glob:**/FileUtils**");   // This is if you want to match the FileUtils dir
    me.runIt();
    System.out.println("Number of directories: " + me.getFiles().size());
  
    List<FileAttributes> allFiles = new ArrayList<FileAttributes>(500);
    
    List<Path> theDirectories = me.getFiles();
    for (Path aPath : theDirectories) {
      System.out.println("Processing directory: " + aPath.toString());
      
      // Get the files (limited to java, properties and .gitignore) for this directory
      //   and add them to the list of allFiles
      GetFileAttributesForDirectory getFileAttributesForDirectory = 
          new GetFileAttributesForDirectory(aPath, startingPath);
      getFileAttributesForDirectory.setPaths2Include("glob:**.{java,properties,gitignore}");
      
      getFileAttributesForDirectory.setDebugFlag(false);
      List<FileAttributes> tempList = getFileAttributesForDirectory.getFilesAttributes();
      allFiles.addAll(tempList);      
    }
    
    System.out.println("Number of files: " + allFiles.size());   
    for (FileAttributes fileAttributes: allFiles) {
      System.out.println("fileAttributes: " + fileAttributes.toString());
    }
  }

}
