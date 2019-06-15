package com.corti.files.checksum;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;

import org.junit.jupiter.api.Test;

class CheckSumTest {
  
  @Test
  public void test() {
    String file2Check = "/seanduff/BACKDIR.BAT";
    
    CheckSum checkSum = new CheckSum(file2Check);
   
    try {
      // We're measuring performance so don't want file creation time to pollute numbers :)
      File file = new File(file2Check);;
      
      String[] theList = { "MD5", "SHA-1", "SHA-256" };
      for (String algoType : theList) {
        System.out.println("Trying: " + algoType);
        long startTime = System.currentTimeMillis();

        //Get the checksum
        String checksum = checkSum.getFileCheckSum(file, algoType);

        long endTime = System.currentTimeMillis();
        long elapsedMilliSeconds = endTime - startTime;
        double elapsedSeconds = elapsedMilliSeconds / 1000.0;
        System.out.println(elapsedSeconds);

        //see checksum
        System.out.println(Integer.toString(checksum.length()) + " " + checksum);
      }
    }
    catch(Exception e) { 
      e.printStackTrace(); 
    }       
  
    assertEquals(true, true);
  }
}
