package com.corti.files;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SerializeOrDeserializeFileAttributesTest {

  @Test
  @DisplayName("Deserialize file")
  void test() {
    final int num2Output = 200000;
    final boolean randomOutput = false;
    int sizeOfArray;
    int record2Output;
    
  //  Path serializedFile = Paths.get("windowsSerializedFile.ser");
    Path serializedFile = Paths.get("appleSerializedFile.ser");
    List<FileAttributes> fileAttributes =  
        SerializeOrDeserializeFileAttributes.deserializeFileAttributesList(serializedFile);
    sizeOfArray = fileAttributes.size();
    assertTrue(sizeOfArray > 0);
    
    System.out.println("Number of records in the file: " + sizeOfArray);
    
    // Random
    if (randomOutput) {
      for (int numOutput = 0; numOutput < num2Output; numOutput++) {
        record2Output = (int) (Math.random() * sizeOfArray); // returns between
                                                             // 0 and (size-1)
        FileAttributes fileAttributesRecord = fileAttributes.get(record2Output);
        // fileAttributesRecord.setStartingBasePath("C:/seanduff/");
        System.out.println("Record: " + record2Output + " StartingBasePath: "
              + fileAttributes.get(record2Output).getStartingBasePath()
              + " getPathFromBaseAsUnix: "
              + fileAttributes.get(record2Output).getPathFromBaseAsUnix());
      }
    }
    else {
      for (int numOutput = 0; numOutput < num2Output && numOutput < sizeOfArray; numOutput++) {
        FileAttributes fileAttributesRecord = fileAttributes.get(numOutput);
        //  fileAttributesRecord.setStartingBasePath("C:/seanduff/");
        if (fileAttributesRecord.getPathFromBaseAsUnix().substring(0,1).equals("d")) {
          System.out.println("Record: " + numOutput +
            " StartingBasePath: " + fileAttributes.get(numOutput).getStartingBasePath() +
            " getPathFromBaseAsUnix: " + fileAttributes.get(numOutput).getPathFromBaseAsUnix());
        }
      }
    }
  }

}
