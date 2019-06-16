package com.corti.files;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.corti.javalogger.LoggerUtils;
import com.corti.jsonutils.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.FileOwnerAttributeView;

public class SerializeOrDeserializeFileAttributes {
  private Instant startInstant;
    
  // Get elapsed time, you call this at the start of initialization and then at end; if
  //   you call it multiple times then the elapsed is cumulative from the beginning
  private long getElapsedTimeInMilliseconds(boolean startIt) {
    if (startIt || startInstant == null) {
      startInstant = Instant.now(); 
      return 0;
    }
    else {
      Instant ending = Instant.now();
      return Duration.between(startInstant,  ending).toMillis();
    }
  }

  // Deserialize the file passed in to a List of FileAttributes
  public static List<FileAttributes> deserializeFileAttributesList(Path inputPath) {
    ArrayList<FileAttributes> fileAttributesList = new ArrayList<FileAttributes>(2000);
    try (FileInputStream fis   = new FileInputStream(inputPath.toFile());
         ObjectInputStream ois = new ObjectInputStream(fis)) {

      fileAttributesList = (ArrayList) ois.readObject();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ClassNotFoundException c) {
      System.out.println("Class not found");
      c.printStackTrace();
    }
    return fileAttributesList;
  }
  
  //Serialize a FileAttributes list to the filename passed in
  public static boolean serializeFileAttributesList(List<FileAttributes> fileAttributesList, Path outputPath) {
    boolean rtnValue = false;
    try (FileOutputStream fos   = new FileOutputStream(outputPath.toFile());
         ObjectOutputStream oos = new ObjectOutputStream(fos)) {
      oos.writeObject(fileAttributesList);
      rtnValue = true;
    } catch (IOException e) {
        e.printStackTrace();
    }
    return rtnValue;
  }  
}
