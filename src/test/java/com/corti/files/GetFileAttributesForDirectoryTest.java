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
    // This tests paths to be included in the search, for this one we show how you
    //   can include paths and ignore the 'case' of the directory name
    GetDirectoriesFromPath me = new GetDirectoriesFromPath("/seanduff");
    me.setDebugFlag(false);
    me.setPathMatcherIgnoreCase(true);
    
    me.setPaths2Include("glob:**/brs/**");
    me.runIt();
    System.out.println("Number of directories: " + me.getFiles().size());
  
    List<FileAttributes> allFiles = new ArrayList<FileAttributes>(500);
    
    List<Path> theDirectories = me.getFiles();
    for (Path aPath : theDirectories) {
      System.out.println("Processing directory: " + aPath.toString());
      
      GetFileAttributesForDirectory getFileAttributesForDirectory = new GetFileAttributesForDirectory(aPath);
      List<FileAttributes> tempList = getFileAttributesForDirectory.getFiles();
      allFiles.addAll(tempList);      
    }
    
    System.out.println("Number of files: " + allFiles.size());
    
    
  }

}
