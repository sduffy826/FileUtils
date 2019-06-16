package com.corti.files;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttributeView;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.attribute.PosixFileAttributeView;

public class GetFileAttributesForDirectory extends GetFileOrDirectoriesCommon {
  private List<FileAttributes> fileAttributeList;
  private Path startingPath; 
  
  // Default constructor is private, must instantiate with a starting path
  private GetFileAttributesForDirectory() { }
  
  public GetFileAttributesForDirectory(Path startingPath) {
    super();
    this.startingPath = startingPath;    
  }
  
  // Main processing
  public List<FileAttributes> getFilesAttributes() throws Exception {          
    fileAttributeList = new ArrayList<FileAttributes>(500);  // List of file attribute objects
    
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
        if (debugIt) System.out.println("fileAttributes: " + fileAttributes.toString());
      } catch (Exception e) {
        System.out.println("Exception raised with " + thePath.toString());
        e.printStackTrace();
      }
    }
    return fileAttributeList;
  }
      
  // Get all the files for the path passed in, we don't recurse down
  private void addPathsFromPath(Path _dirPath, List<Path> pathList) {
    List<Path> skipList = new ArrayList<Path>(500);
    Path path2Compare;
    List<PathMatcher> pathMatchers2Check;
    int isGood;
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(_dirPath)) {
      for (Path entry : stream) {
        if (Files.isDirectory(entry) == false) {
          path2Compare = entry.toAbsolutePath();
          if (pathMatcherIgnoreCase) 
            path2Compare = Paths.get(entry.toAbsolutePath().toString().toLowerCase());

          if (debugIt) System.out.println("In addPathsFromPath() file: " + entry.toString());
                 
          // Check both path matchers
          int loopCnt = 0;
          isGood = -2; // Flag, if weWant file by default
          while (loopCnt < 2) {
            loopCnt++;
            if (loopCnt == 1)
              pathMatchers2Check = getPaths2Include();
            else
              pathMatchers2Check = getPaths2Exclude();

            if (pathMatchers2Check.size() > 0) {
              isGood = -1;
              for (PathMatcher pathMatcher : pathMatchers2Check) {
                if (pathMatcher.matches(entry)) {
                  if (loopCnt == 1) { // match an include it's good
                    isGood = 1;
                    break;
                  } else { // It matches an exclude, it's bad
                    isGood = 0;
                    break;
                  }
                }
              }
              // If checking includes and didn't match any then we can exit
              if (loopCnt == 1 && isGood != 1) {
                isGood = 0;  
                loopCnt = 3; 
              } 
            } // end of if check that patchMatchers2Check.size() > 0
          } // end of while loop
                        
          if (isGood == 0) {  // Don't want it, if in debug mode write it to skippy :)
            skipList.add(entry);
          }
          else { // Matched or no pathMatchers defined or didn't match an exclude
            pathList.add(entry);
          }
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
