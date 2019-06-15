package com.corti.files;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Class represents a DOS file.
 */
public class DosFileAttributes extends FileAttributes {
  private boolean readOnlyFile;
  private boolean systemFile;
  private boolean hiddenFile;
  private boolean archiveFlag;
  
  // Constructors, either with string version of filename or a path; the default is really for 
  // deserialization (json; setters should be called when that happens)
  public DosFileAttributes(String fileNameAndPath) throws Exception {
    super(fileNameAndPath);
    init();
  }
  
  public DosFileAttributes(Path path) throws Exception {
    super(path);
    init();    
  }
  
  public DosFileAttributes() throws Exception {
    super();
    init();
  }  
  
  // Initialization of local vars
  private void init() throws IOException {
    if (path != null) {
      java.nio.file.attribute.DosFileAttributes dosFileAttributes = Files.readAttributes(path,  java.nio.file.attribute.DosFileAttributes.class);
      readOnlyFile = dosFileAttributes.isReadOnly();
      systemFile   = dosFileAttributes.isSystem();
      hiddenFile   = dosFileAttributes.isHidden();
      archiveFlag  = dosFileAttributes.isArchive();
    }
  }
  
  public boolean isReadOnlyFile() {
    return readOnlyFile;
  }

  public boolean isSystemFile() {
    return systemFile;
  }

  public boolean isHiddenFile() {
    return hiddenFile;
  }

  public boolean isArchiveFlag() {
    return archiveFlag;
  }

  public void setReadOnlyFile(boolean readOnlyFile) {
    this.readOnlyFile = readOnlyFile;
  }

  public void setSystemFile(boolean systemFile) {
    this.systemFile = systemFile;
  }

  public void setHiddenFile(boolean hiddenFile) {
    this.hiddenFile = hiddenFile;
  }

  public void setArchiveFlag(boolean archiveFlag) {
    this.archiveFlag = archiveFlag;
  }

  @Override
  public String toString() {
    return super.toString() + " DosFileAttributes [readOnlyFile=" + readOnlyFile + ", systemFile="
        + systemFile + ", hiddenFile=" + hiddenFile + ", archiveFlag="
        + archiveFlag + "]";
  }
 }
