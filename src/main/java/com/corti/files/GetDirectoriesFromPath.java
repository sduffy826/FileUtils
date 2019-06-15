package com.corti.files;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.corti.PropertyHelper;

/**
 * This class Gets the directories for a given path, you can instantiate and give it the starting path
 *   or call 'setStartingPath(pathName)'; other methods of interest:
 * - setNum2Process(int): limit the number of files you want this to look at 
 * - setDebugFlag(boolean): pass 'true' if you want to see output to the console, also, the directories that
 *     are skipped will be written to 'skippyList.$$$' so you can check that out after a run
 * - runIt(): call this when you want to have the program start searching for files (made separate so you could
 *     call methods above prior to running)
 * Exclude/Include directory note:
 * - if you want to include/exclude directories to be case insensative then call 
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
public class GetDirectoriesFromPath {
  private boolean debugIt = false;
  private int num2Process = -1; // -1 to do all records
  private String startingPath = null;
  private String outputName = null;
  private List<Path> fileList;
  private boolean pathMatcherIgnoreCase = false;
  List<PathMatcher> pathMatchers2Exclude;  // Paths to exclude
  List<PathMatcher> pathMatchers2Include;
  private FileSystem fileSystem;
  
  private final ReentrantReadWriteLock rwLock;
  private final Lock readLock;
  private final Lock writeLock;
  
  private String skippyOutputFile;
  
  // ========================= C o n s t r u c t o r s ========================= 
  public GetDirectoriesFromPath() {
    rwLock    = new ReentrantReadWriteLock();
    readLock  = rwLock.readLock();
    writeLock = rwLock.writeLock();
    
    fileList         = new ArrayList<Path>(500); // List of directories
    skippyOutputFile = "skippyList.$$$"; // File will have list of directories skipped (only when debugIt)
    fileSystem       = FileSystems.getDefault();
    
    pathMatchers2Exclude = new ArrayList<PathMatcher>(10);
    pathMatchers2Include = new ArrayList<PathMatcher>(10);
    setGroupPaths2Exclude("eclipse"); // set it default to exclude the eclipse stuff we don't want    
  }
  
  public GetDirectoriesFromPath(String startingPath) {
    this();  // Call default constructor
    setStartingPath(startingPath);
  }

  // ========================= G e t t e r s =========================

  // Return the list of files
  public List<Path> getFiles() {    
    readLock.lock();
    try {
      return fileList;
    } finally {
      readLock.unlock();
    }
  }
  
  // Return number of files found
  public int getNumberOfEntries() {    
    readLock.lock();
    try {
      return fileList.size();
    } finally {
      readLock.unlock();
    }
  }
 
  // Get indicator if ignoring case on searches
  public boolean getPathMatcherIgnoreCase() {
    return pathMatcherIgnoreCase;
  }
  
  // Return a list of paths to exclude
  public List<PathMatcher> getPaths2Exclude() {    
    readLock.lock();
    try {
      return pathMatchers2Exclude;
    } finally {
      readLock.unlock();      
    }
  }
 
  // Return a list of paths to exclude
  public List<PathMatcher> getPaths2Include() {    
    readLock.lock();
    try {
      return pathMatchers2Include;
    } finally {
      readLock.unlock();      
    }
  }
   
  //Utility method to get a property from the file passed in and return it as a string list, the char
  //   to split on is passed in
  public List<String> getPropertyList(String propertyFileName, String propertyName, String splitChar) {
    PropertyHelper.setDebugValue(true);    
    String propertyValueAsString = PropertyHelper.getProperty(propertyFileName, propertyName);
   
    // Logic below: convert string[] to stream, trim each element and collect to a list :)
    return Stream.of(propertyValueAsString.split(splitChar))
                     .map(String::trim)
                     .collect(Collectors.toList());
  }
    
  // ========================= S e t t e r s =========================
  
  // Remove the filter on paths to exclude
  public void clearPaths2Exclude() {
    writeLock.lock();
    try {
      pathMatchers2Exclude.clear();
    } finally {
      writeLock.unlock();      
    }
  }
  
  // Remove the filter on paths to exclude
  public void clearPaths2Include() {
    writeLock.lock();
    try {
      pathMatchers2Include.clear();
    } finally {
      writeLock.unlock();      
    }
  }
    
  //Public method to set the debugIt flag (will show output on console if on)
  public void setDebugFlag(boolean debugIt) {
    this.debugIt = debugIt;
  }
 
  // May be groupings we want to exclude, change this down the road to use properties but for
  //   now it's just hardcoded
  public void setGroupPaths2Exclude(String groupingName) {
    switch (groupingName) {
    case "eclipse":
      setPaths2Exclude("glob:**/target*");
      setPaths2Exclude("glob:**/workspace/.metadata*");
      setPaths2Exclude("glob:**/workspace/.recommenders*");
      break;
    default:
    }
  }

  //Public method allows caller to limit number of files
  public void setNum2Process(int num2Process) {
    this.num2Process = num2Process;
  }
 
  // Set the paths to be excluded from the search
  private void setPaths2IncludeOrExclude(boolean isInclude, String path2Exclude) {
    writeLock.lock();
    if (pathMatcherIgnoreCase) 
      path2Exclude = path2Exclude.toLowerCase();
    try {
      if (isInclude) {
        pathMatchers2Include.add(fileSystem.getPathMatcher(path2Exclude));
      }
      else {
        pathMatchers2Exclude.add(fileSystem.getPathMatcher(path2Exclude));
      }
    } finally {
      writeLock.unlock();      
    }
  }
  
  // Set boolean to ignore case when doing path matcher comparisions
  public void setPathMatcherIgnoreCase(boolean ignoreCase) {
    pathMatcherIgnoreCase = ignoreCase;
  }
 
  // Set the paths to be excluded from the search
  public void setPaths2Exclude(String path2Exclude) {
    setPaths2IncludeOrExclude(false, path2Exclude); // Helper does the work
  }
 
  // Set the paths to be excluded from the search
  public void setPaths2Include(String path2Include) {
    setPaths2IncludeOrExclude(true, path2Include); // Helper does the work
  }  
  
  // Helper to process the paths to include or exclude, the first arg determines which list.
  // Since property files don't support lists so have the values in the file with
  //   a delimeter.
  //   This takes a boolean and a variable number of arguments, the second parameter should be the
  //   parameter filename, second parmeter is the parameter name (default 'paths2Exclude') and the
  //   optional third parm is the delimetter to split on (default is comma), example:
  //      glob:**/temp/*,glob:**/foo/*
  //   This would ignore directories temp and foo (the ** means at any nesting level), google the
  //     'java pathpatcher glob' keyword for more :)
  private void setPaths2FromProperty(boolean isInclude, String... arguments) {
    String propName = (arguments.length >= 2 ? arguments[1] : "paths2Exclude");
    String delim    = (arguments.length >= 3 ? arguments[2] : ",");  
    if (debugIt) {
      System.out.println("setPaths2ExcludeFromProperty() propertyFile: " + arguments[0]
                        + " propName: " + propName
                        + " delim: " + delim);
    }
    List<String> dirs2Ignore = getPropertyList(arguments[0], propName, delim);
    for (String dirName : dirs2Ignore) {
      if (isInclude) {
        setPaths2Include(dirName);
      }
      else {
        setPaths2Exclude(dirName);
      }
    }    
  }
 
  // Set paths 2 excluded
  public void setPaths2ExcludeFromProperty(String... arguments) {
    setPaths2FromProperty(false, arguments); // helper does work    
  }

  // Set paths 2 included
  public void setPaths2IncludeFromProperty(String... arguments) {
    setPaths2FromProperty(true, arguments); 
  }
  
  // Set the name of the output file that has directories being skipped
  public void setSkipOutputFile(String skippyOutputFile) {
    this.skippyOutputFile = skippyOutputFile;
  }

  // Set the path we'll start the search from
  public void setStartingPath(String startingPath) {
    this.startingPath = startingPath;
  }

  // ========================= P r o c e s s i n g =========================
  
  // Get all the files from the path passed in, will recurse depth
  private void addPathsFromPath(List<Path> pathList) {
    
    List<Path> tempList = new ArrayList<Path>(pathList);  // Copy starting list to tempList
    
    Path path2Compare;
    boolean wrote2Skippy = false;
    boolean skipIt = false;
    try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(skippyOutputFile))) {
      for (int idx = 0; idx < tempList.size(); idx++) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(tempList.get(idx))) {
          for (Path entry : stream) {
            if (Files.isDirectory(entry)) {
              if (debugIt) System.out.println("addPathsFromPath: " + entry.toString());
              
              path2Compare = entry.toAbsolutePath();
              if (pathMatcherIgnoreCase) 
                path2Compare = Paths.get(entry.toAbsolutePath().toString().toLowerCase());
              
              skipIt = false;
              // See if in list to exclude
              for (PathMatcher pathMatcher : pathMatchers2Exclude) {
                if (pathMatcher.matches(path2Compare) == true) {
                  skipIt = true;
                  break; // don't need to look at more
                }
              }
              
              // Check list to include
              
              if (skipIt) {  // Don't want it, if in debug mode write it to skippy :)
                if (debugIt) {
                  writer.write(entry.toAbsolutePath().toString());
                  writer.newLine();
                  wrote2Skippy = true;
                }
              }
              else {  // Good directory add it to the list
                tempList.add(entry);
                if (debugIt) System.out.println("Path: " + entry + " good will search it");
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
            if (debugIt) {
              writer.write(aPath.toAbsolutePath().toString());
              writer.newLine();
              wrote2Skippy = true;
            }
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
    } 
    catch (IOException e) {
    }
    if (wrote2Skippy) System.out.println("Directories skipped are in: " + skippyOutputFile);
  }
  
  // Main routine, this gets directories (using helper - addPathsFromPath
  public void runIt() throws Exception {
    writeLock.lock();
    try {      
      Path basePath = Paths.get(startingPath);
      String startingAbsolutePath = basePath.toAbsolutePath().toString();
      if (debugIt) System.out.println("startingAbsolutePath: " + startingAbsolutePath);
     
      fileList.add(basePath);
      addPathsFromPath(fileList);

      // If it has more data than we want to process then trim list to size we want
      if (num2Process > 0 && fileList.size() > num2Process)
        fileList = fileList.subList(0, num2Process);
      
      if (debugIt) System.out.println("runit() done, number entries: " + fileList.size());

    } finally {
      writeLock.unlock();
    }
  }
  
  // Write entries to file caller specifies, return true if success, false on failure
  public boolean writeEntriesToFile(String outputName) {
    readLock.lock();
    try {
      this.outputName = outputName;

      Path outputPath = Paths.get(outputName);
      System.out.println("Writing directories to " + outputPath.getFileName());
      try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
        for (Path thePath : fileList) {
          writer.write("\"" + thePath.toAbsolutePath().toString() + "\"");
          writer.newLine();
        }
      } catch (IOException e) {
        e.printStackTrace();
        return false;
      }
      return true;

    } finally {
      readLock.unlock();
    }
  }
}