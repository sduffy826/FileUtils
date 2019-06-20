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
    final int num2Output = 20;
    int sizeOfArray;
    int record2Output;
    
    Path serializedFile = Paths.get("windowsSeanduff.ser");
  //  Path serializedFile = Paths.get("appleSeanduff.ser");
    List<FileAttributes> fileAttributes =  
        SerializeOrDeserializeFileAttributes.deserializeFileAttributesList(serializedFile);
    sizeOfArray = fileAttributes.size();
    assertTrue(sizeOfArray > 0);
    
    System.out.println("Number of records in the file: " + sizeOfArray);
    
    for (int numOutput = 0; numOutput < num2Output; numOutput++) {
      record2Output = (int) (Math.random() * sizeOfArray); // returns between 0 and (size-1)
      FileAttributes fileAttributesRecord = fileAttributes.get(record2Output);
      fileAttributesRecord.setStartingBasePath("C:/seanduff/");
      System.out.println("Record: " + record2Output +
        " StartingBasePath: " + fileAttributes.get(record2Output).getStartingBasePath() +
        " getPathFromBaseAsUnix: " + fileAttributes.get(record2Output).getPathFromBaseAsUnix());
      
    }    
  }

}
