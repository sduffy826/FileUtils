package com.corti.files;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

class CompareFileAttributesInSerializedFilesTest {

  @Test
  void test() {
    Path appleWorkspace = Paths.get("appleSerializedFile.ser");
    Path windowsWorkspace = Paths.get("windowsSerializedFile.ser");
    
    CompareFileAttributesInSerializedFiles me = 
        new CompareFileAttributesInSerializedFiles(appleWorkspace, windowsWorkspace);
    
    try {
      me.performCompare();
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

}
