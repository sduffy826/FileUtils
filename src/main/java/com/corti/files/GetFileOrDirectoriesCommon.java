package com.corti.files;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.corti.PropertyHelper;

/**
 * Common class for classes that get file and directories.  See the description at the
 * top of GetDirectoriesFromPath, it has info that'll clarify usage.
 */
public class GetFileOrDirectoriesCommon {

  protected boolean debugIt = false;
  protected int num2Process = -1;
  private String outputName = null;
  protected boolean pathMatcherIgnoreCase = false;   // Handles case sensitive matches
  protected List<PathMatcher> pathMatchers2Exclude;  // Matcher for things to exclude
  protected List<PathMatcher> pathMatchers2Include;  // Matcher for includes
  protected final ReentrantReadWriteLock rwLock;    
  protected final Lock readLock;
  protected final Lock writeLock;
  protected String skippyOutputFile;                 // If debugging then skipped values written here
  protected FileSystem fileSystem;
  protected List<Path> fileList; 


  public GetFileOrDirectoriesCommon() {
    super();
    rwLock    = new ReentrantReadWriteLock();
    readLock  = rwLock.readLock();
    writeLock = rwLock.writeLock();
    
    skippyOutputFile = "skippyList.$$$"; // File will have list of directories skipped (only when debugIt)
    fileSystem       = FileSystems.getDefault();
    fileList         = new ArrayList<Path>(500); // List of directories

    pathMatchers2Exclude = new ArrayList<PathMatcher>(10);
    pathMatchers2Include = new ArrayList<PathMatcher>(10);  
  }

  public List<Path> getFiles() {    
    readLock.lock();
    try {
      return fileList;
    } finally {
      readLock.unlock();
    }
  }

  public boolean getPathMatcherIgnoreCase() {
    return pathMatcherIgnoreCase;
  }

  public List<PathMatcher> getPaths2Exclude() {    
    readLock.lock();
    try {
      return pathMatchers2Exclude;
    } finally {
      readLock.unlock();      
    }
  }

  public List<PathMatcher> getPaths2Include() {    
    readLock.lock();
    try {
      return pathMatchers2Include;
    } finally {
      readLock.unlock();      
    }
  }

  public List<String> getPropertyList(String propertyFileName, String propertyName, String splitChar) {
    PropertyHelper.setDebugValue(true);    
    String propertyValueAsString = PropertyHelper.getProperty(propertyFileName, propertyName);
  
    // Logic below: convert string[] to stream, trim each element and collect to a list :)
    return Stream.of(propertyValueAsString.split(splitChar))
                     .map(String::trim)
                     .collect(Collectors.toList());
  }

  public void clearPaths2Exclude() {
    writeLock.lock();
    try {
      pathMatchers2Exclude.clear();
    } finally {
      writeLock.unlock();      
    }
  }

  public void clearPaths2Include() {
    writeLock.lock();
    try {
      pathMatchers2Include.clear();
    } finally {
      writeLock.unlock();      
    }
  }

  public void setDebugFlag(boolean debugIt) {
    this.debugIt = debugIt;
  }

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

  public void setNum2Process(int num2Process) {
    this.num2Process = num2Process;
  }

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

  public void setPathMatcherIgnoreCase(boolean ignoreCase) {
    pathMatcherIgnoreCase = ignoreCase;
  }

  public void setPaths2Exclude(String path2Exclude) {
    setPaths2IncludeOrExclude(false, path2Exclude); // Helper does the work
  }

  public void setPaths2Include(String path2Include) {
    setPaths2IncludeOrExclude(true, path2Include); // Helper does the work
  }

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

  public void setPaths2ExcludeFromProperty(String... arguments) {
    setPaths2FromProperty(false, arguments); // helper does work    
  }

  public void setPaths2IncludeFromProperty(String... arguments) {
    setPaths2FromProperty(true, arguments); 
  }

  public void setSkipOutputFile(String skippyOutputFile) {
    this.skippyOutputFile = skippyOutputFile;
  }

  public boolean writeEntriesToFile(List<Path> list2Output, String outputName) {
    readLock.lock();
    try {
      this.outputName = outputName;
  
      Path outputPath = Paths.get(outputName);
      System.out.println("Writing directories to " + outputPath.getFileName());
      try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
        for (Path thePath : list2Output) {
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