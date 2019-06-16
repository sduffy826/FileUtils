package com.corti.files;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * This class Gets the directories for a given path, you can instantiate and give it the starting path
 *   or call 'setStartingPath(pathName)'; other methods of interest:
 * - setNum2Process(int): limit the number of files you want this to look at 
 * - setMaxDepth(int): max number of directories to go below original 1, 0 if only want original directory
 * - setDebugFlag(boolean): pass 'true' if you want to see output to the console, also, the directories that
 *     are skipped will be written to 'skippyList.$$$' so you can check that out after a run
 * - runIt(): call this when you want to have the program start searching for files (made separate so you could
 *     call methods above prior to running)
 * Exclude/Include directory note:
 * - if you want to include/exclude directories to be case insensitive then call 
 *     pathMatcherIgnoreCase(true)  
 *    this should be done before you start adding paths to include/exclude 
 * Excluding directories:
 * - It uses the java pathmatcher (part of nio) to ignore certain paths, by default I'll ignore the ones
 *     in eclipse that you wouldn't want (target, metadata...).  You can prevent that by doing 'clearIgnoreFilter'
 *     clearPaths2Exclude() - clears filter, i.e. this would clear the eclipse default
 *     setPaths2Exclude(String path2Exclude) - add a path to the exclusion list
 *     setPaths2ExcludeFromProperty(String propertyFileName, String propertyName, String delimiter) - allows you to
 *       use a property file, since property files don't support lists you need to separate the values by a 
 *       delimiter; the propertyName is optional, if you don't specify then it'll default to 'paths2Exclude', the
 *       delimiter is also optional, if you don't specify it then comma is used.  
 *     setGroupPaths2Exclude(String groupPathName) - exclude paths related to group name, currently only
 *       has 'eclipse' but more may be added
 * Include directories:
 * - Similar to above, the method names are similar to above so read more on them
 *   clearPaths2Include() - clear filter
 *   setPaths2Include(String path2Include) - add path
 *   setPaths2IncludeFromProperty(String propertyFileName, string propertyName, string delimiter)
 *   (doesn't have a 'setGroupPaths2Include' didn't think need it (at least now))
 * 
 *  Methods to get results:
 *  
 * - getNumberOfEntries(): returns the number of files
 * - getFiles(): returns the files (List<Path>)
 * - writeEntriesToFile(String outputFileName): writes files to file name given, returns true if success
 */
public class GetDirectoriesFromPath extends GetFileOrDirectoriesCommon {
  private Path startingPath = null;
  private int maxDepth = -1;  // Max level to go
  private int startingNameCount;
  
  // ========================= C o n s t r u c t o r s ========================= 
  public GetDirectoriesFromPath() {    
    super();
    setGroupPaths2Exclude("eclipse"); // set it default to exclude the eclipse stuff we don't want    
  }
  
  public GetDirectoriesFromPath(String startingPathName) {
    this();  // Call default constructor
    setStartingPath(startingPathName);
  }
    
  public GetDirectoriesFromPath(Path startingPath) {
    this();
    this.startingPath = startingPath;
  }
  
  // ========================= S e t t e r s =========================
  
  // Set the path we'll start the search from
  public void setStartingPath(String startingPathName) {
    this.startingPath = Paths.get(startingPathName);    
  }

  // Set the max depth to go, 0 is at the starting path
  public void setMaxDepth(int maxDepth) {
    this.maxDepth = maxDepth;
  }
  
  public int getMaxDepth() {
    return maxDepth;
  }
  
  // ========================= P r o c e s s i n g =========================
  
  // Get all the files from the path passed in, will recurse depth
  private void addPathsFromPath(List<Path> pathList) {    
    List<Path> tempList = new ArrayList<Path>(pathList);  // Working list, easier cause of needed filtering 
    List<Path> skipList = new ArrayList<Path>(500);
    Path path2Compare;
    boolean skipIt = false;
    int pathNameCount;
    int maxDepth = getMaxDepth();
    startingNameCount = startingPath.getNameCount();  // Get starting level (# directories)
    for (int idx = 0; idx < tempList.size(); idx++) {
      try (DirectoryStream<Path> stream = Files.newDirectoryStream(tempList.get(idx))) {
        for (Path entry : stream) {
          if (Files.isDirectory(entry)) {
            pathNameCount = entry.getNameCount();  // Get number of levels in this path
            if (maxDepth >= (pathNameCount - startingNameCount)) { 
              if (debugIt)
                System.out.println("addPathsFromPath: " + entry.toString() +
                                   " level: " + (pathNameCount - startingNameCount));

              path2Compare = entry.toAbsolutePath();
              if (pathMatcherIgnoreCase)
                path2Compare = Paths
                    .get(entry.toAbsolutePath().toString().toLowerCase());

              skipIt = false;
              // See if in list to exclude
              for (PathMatcher pathMatcher : pathMatchers2Exclude) {
                if (pathMatcher.matches(path2Compare) == true) {
                  skipIt = true;
                  break; // don't need to look at more
                }
              }

              if (skipIt) { // Don't want it, if in debug mode write it to
                            // skippy :)
                skipList.add(entry);
              } else { // Good directory add it to the list
                tempList.add(entry);
                if (debugIt)
                  System.out.println("Path: " + entry + " good will search it");
              }
            }
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
      
    // We have all the directories... we couldn't filter out the include list above cause 
    //   the path could be one that's nested several layers deep, so we'll filter them out here
    if (pathMatchers2Include.size() > 0) {
      pathList.clear();
      for (Path aPath : tempList) {
        skipIt = true;
        
        path2Compare = aPath.toAbsolutePath();
        if (pathMatcherIgnoreCase) 
          path2Compare = Paths.get(aPath.toAbsolutePath().toString().toLowerCase());
          
        for (PathMatcher pathMatcher : pathMatchers2Include) {
          if (pathMatcher.matches(path2Compare) == true) {
            skipIt = false;
            break;
          }
        }
        if (skipIt) { // Don't want this path
          skipList.add(aPath);
        } else { // Want this one, add to pathList
          pathList.add(aPath);
        }
      }
    } else {
      // Copy tempList to pathList, left the next line for reference, it doesn't work
      //   the .collect returns a new list, assigning that to pathList doesn't do any
      //   good since you're reassigning the reference, which is ignored when this
      //   method returns, that's why it's updated in the loopo
      // pathList = tempList.stream().collect(Collectors.toList());
      for (Path tempPath : tempList) {
        pathList.add(tempPath);
      }
    } 

    if (debugIt && skipList.size() > 0) {
      writeEntriesToFile(skipList, skippyOutputFile);
    }
  }
  
  // Main routine, this gets directories (using helper - addPathsFromPath
  public void runIt() throws Exception {
    writeLock.lock();
    try {      
      String startingAbsolutePath = startingPath.toAbsolutePath().toString();
      if (debugIt) System.out.println("startingAbsolutePath: " + startingAbsolutePath);
     
      fileList.add(startingPath);
      addPathsFromPath(fileList);

      // If it has more data than we want to process then trim list to size we want
      if (num2Process > 0 && fileList.size() > num2Process)
        fileList = fileList.subList(0, num2Process);
      
      if (debugIt) System.out.println("runit() done, number entries: " + fileList.size());

    } finally {
      writeLock.unlock();
    }
  }
}