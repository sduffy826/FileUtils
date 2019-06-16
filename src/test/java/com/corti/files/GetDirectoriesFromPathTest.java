package com.corti.files;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.corti.files.GetDirectoriesFromPath;

class GetDirectoriesFromPathTest {

  @Test
  void test() {
    GetDirectoriesFromPath me = new GetDirectoriesFromPath("/seanduff");
    me.setDebugFlag(false);
    // Showing it doesn't get directories yet, u need to do me.runIt()
    System.out.println("Number of directories: " + me.getFiles().size());
    System.out.println("Size of pathExclusingList: " + me.getPaths2Exclude().size());
    
    // Should be default in directory
    assertTrue(me.getPaths2Exclude().size() > 0);
  }

  @Test
  public void testGet10Directories() throws Exception {
    GetDirectoriesFromPath me = new GetDirectoriesFromPath("/seanduff/workspace/JavaUtilities");
    me.setDebugFlag(true);
    me.setNum2Process(10);
    me.setGroupPaths2Exclude("glob:**/java/*");  // Just for fun ignore java dirs
    me.runIt();
    List<Path> rtnList = me.getFiles();
    for (Path aPath : rtnList) {
      System.out.println("In testGet10Directories: " + aPath.toString());
    }
    System.out.println("testGet10Directories(), number of directories: " + me.getFiles().size());
    assertEquals(10,me.getFiles().size(),"Pulled 10 directories only");
  }

  
  @Test
  void testClearPath() {
    GetDirectoriesFromPath me = new GetDirectoriesFromPath("/seanduff");
    me.setDebugFlag(false);
    
    // Make sure it clears
    me.clearPaths2Exclude();
    System.out.println("Size of pathExclusingList: " + me.getPaths2Exclude().size());
    
    assertTrue(me.getPaths2Exclude().size() == 0);    
  }
  
  @Test
  void testLoadPropertyExclude() {
    GetDirectoriesFromPath me = new GetDirectoriesFromPath("/seanduff");
    me.setDebugFlag(false);
    
    // Load from property file, you pass the property filename and the variable with
    //   the data
    me.setPaths2ExcludeFromProperty("worksSpaceSkipDirs.properties","paths2Ignore",",");
    System.out.println("Size of pathExclusingList: " + me.getPaths2Exclude().size());
    
    List<PathMatcher> paths2Ignore = me.getPaths2Exclude();
    for (PathMatcher aPathMatcher : paths2Ignore) {
      System.out.println("PathToIgnore (not that this is valuable):" + aPathMatcher.toString() + ":");
    }
    assertTrue(me.getPaths2Exclude().size() > 0);
  }
  
  @Test
  void testLoadPath2Exclude() {
    GetDirectoriesFromPath me = new GetDirectoriesFromPath("/seanduff");
    me.setDebugFlag(false);
    
    // Add one item and verify that's all that's there
    me.clearPaths2Exclude();
    me.setPaths2Exclude("glob:**/deleteMe/**");
    System.out.println("Size of pathExclusingList: " + me.getPaths2Exclude().size());
    assertTrue(me.getPaths2Exclude().size() == 1);
    
    // deleteMe_pathMatcher, it's where 'deleteMe' is anywhere in path
    PathMatcher deleteMe_PathMatcher = me.getPaths2Exclude().get(0);
    
    Path testPath = Paths.get("/seanduff/foo/deleteMe/bar/dog/sample.txt");
    assertTrue(deleteMe_PathMatcher.matches(testPath));
    
    // check if it was off root
    testPath = Paths.get("/deleteMe/cat.txt");
    assertTrue(deleteMe_PathMatcher.matches(testPath));
    
    // The /deleteMe/ alone will fail... rather it didn't but wildcard is expecting something
    // after it (/deleteMe/.) would match
    testPath = Paths.get("/deleteMe/");
    assertFalse(deleteMe_PathMatcher.matches(testPath));
    
    testPath = Paths.get("/deleteMe/.");
    assertTrue(deleteMe_PathMatcher.matches(testPath));    
  }
  
  @Test
  void testPath2Include() throws Exception {
    // This tests paths to be included in the search, for this one we show how you
    //   can include paths and ignore the 'case' of the directory name
    GetDirectoriesFromPath me = new GetDirectoriesFromPath("/seanduff");
    me.setDebugFlag(false);
    me.setPathMatcherIgnoreCase(true);
    
    me.setPaths2Include("glob:**/brs/**");
    me.runIt();
    System.out.println("Number of directories: " + me.getFiles().size());
    System.out.println("Path matcher ignores case: " + me.getPathMatcherIgnoreCase());
    List<Path> theDirectories = me.getFiles();
    for (Path aPath : theDirectories) {
      System.out.println("Filter on **/src/** returned: " + aPath.toString());
    }
  }

}
