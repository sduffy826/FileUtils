package com.corti.files.checksum;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.io.File;

/**
 * Class to calculate file checksum, you can invoke constructor several different ways:
 * Note: if the algorithm isn't passed then it's assumed to be SHA-256 (other algorithm types are MD5 and SHA-1)
 * Constructor:
 *   No args - then you should pass filename (optional algorithm) when calling getFileCheckSum
 *   String fileName - the filename to process
 *   String fileName, String algorithm - you know what this is
 * Method getFileCheckSum returns the checksum for a file, the ways this can be called
 *   No args - will assume values were set in constructor
 *   String fileName - the filename to process
 *   File file - similar to above but pass in file object
 *   String fileName, String algorithm - you pass filename and algorithm to use
 *   File file, String algorithm - you know what this is :) *
 */
public class CheckSum {

  File file2Process;
  String algorithmName;
  
  // Static method
  public static String getFileCheckSumValue(Path path, String algorithm) throws NoSuchAlgorithmException, IOException {
    CheckSum me = new CheckSum();
    return me.getFileCheckSum(path, algorithm);
  }
  
  public CheckSum() {
    file2Process  = null;
    algorithmName = "SHA-256"; // Use this as default
  }
  
  public CheckSum(String fileNameAndPath) {
    file2Process  = new File(fileNameAndPath);
    algorithmName = "SHA-256";
  }

  public CheckSum(String fileNameAndPath, String algorithm) {
    file2Process  = new File(fileNameAndPath);
    algorithmName = algorithm;
  }
  
  // Didn't pass any args then assume vars are set with constructor
  public String  getFileCheckSum() throws IOException, NoSuchAlgorithmException{
    return getFileCheckSum(file2Process, algorithmName);
  }
    
  // If just pass in string then assume it's the filename :)
  public String getFileCheckSum(String fileName) throws IOException, NoSuchAlgorithmException{
    file2Process = new File(fileName);
    return getFileCheckSum(file2Process, algorithmName);
  }  
  
  // This one you pass in algorithm and string filename
  public String getFileCheckSum(String fileName, String algorithm) throws IOException, NoSuchAlgorithmException{
    file2Process  = new File(fileName);
    return getFileCheckSum(file2Process, algorithm);
  }  
  
  // Passed in a path and algorithm
  public String getFileCheckSum(Path path, String algorithm)  throws IOException, NoSuchAlgorithmException{
    return getFileCheckSum(path.toFile(), algorithm);
  }  
  
  // Method to return files check sum, algorithm name should be MD5, SHA-1 or SHA-256
  public String getFileCheckSum(File file2Process, String algorithmName) throws IOException, NoSuchAlgorithmException{
    // Get message digest
    MessageDigest messageDigest = MessageDigest.getInstance(algorithmName);
    
    // Get file input stream for reading the file content
    FileInputStream fis = new FileInputStream(file2Process);

    // Create byte array to read data in chunks
    byte[] byteArray = new byte[1024];
    int bytesCount = 0; 

    // Read file data and update in message digest
    while ((bytesCount = fis.read(byteArray)) != -1) {
      messageDigest.update(byteArray, 0, bytesCount);
    };

    // close the stream; We don't need it now.
    fis.close();

    // Get the hash's bytes
    byte[] bytes = messageDigest.digest();

    // This bytes[] has bytes in decimal format;
    // Convert it to hexadecimal format
    StringBuilder sb = new StringBuilder();
    for(int i=0; i< bytes.length ;i++)
    {
      sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
    }

    // return complete hash
    return sb.toString();
  }
}