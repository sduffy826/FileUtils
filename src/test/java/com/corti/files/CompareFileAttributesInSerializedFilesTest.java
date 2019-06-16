package com.corti.files;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

class CompareFileAttributesInSerializedFilesTest {

  @Test
  void test() {
    Path appleWorkspace = Paths.get("../appleWorkspaceSerialized.ser");
    Path windowsWorkspace = Paths.get("../windowsWorkspaceSerialized.ser");
    
    CompareFileAttributesInSerializedFiles me = new CompareFileAttributesInSerializedFiles(appleWorkspace, windowsWorkspace);
    
    try {
      me.performCompare();
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

}
