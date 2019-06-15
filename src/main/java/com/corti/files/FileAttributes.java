package com.corti.files;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import com.corti.files.checksum.CheckSum;

// We don't want instantiationMethod to be deserialized, but it can be serialized, you
//  can add other attributes to array if needed
@JsonIgnoreProperties(value={ "instantiationMethod" }, allowGetters=true)
public abstract class FileAttributes implements Serializable {
  // These statics are used to represent how the object was instantiated; when instantiated
  //   from DEFAULT it's probably from deserialization so the path could reflect 
  private static final int DEFAULT = 0;
  private static final int FROMFILENAMEANDPATH = 1;
  private static final int FROMPATH = 2;
  private static final int FROMJAVASERIALIZATION = 3;
  
  protected static final String CHECKSUMALGORITHM = "SHA-256";
  private int instantiationMethod;
  
  protected String className;
  protected Path path;
  protected String startingBasePath;  // The starting base path that objects created from
  
  protected String absolutePath;  // Needed when instantiated from json object
  protected String fileExtension; // Wanted to ignore certain types
  
  @JsonSerialize(using = com.corti.jsonutils.FileTimeSerializer.class)
  @JsonDeserialize(using = com.corti.jsonutils.FileTimeDeSerializer.class)
  protected FileTime creationTime;
  
  @JsonSerialize(using = com.corti.jsonutils.FileTimeSerializer.class)
  @JsonDeserialize(using = com.corti.jsonutils.FileTimeDeSerializer.class)
  protected FileTime lastAccessTime;
  
  @JsonSerialize(using = com.corti.jsonutils.FileTimeSerializer.class)
  @JsonDeserialize(using = com.corti.jsonutils.FileTimeDeSerializer.class)
  protected FileTime lastModifiedTime;
  protected boolean isDirectory;
  protected boolean isRegularFile;
  protected boolean isSymbolicLink;
  protected boolean isOther;
  protected long sizeInBytes;
  protected String checkSumValue;

  protected String exceptionMessage;
  
  // This one is really for deserialization; that's the only time objects should be
  // instantiated without knowing path or filename
  FileAttributes() {
    this.path = null;
    this.instantiationMethod = DEFAULT;
    init();        
  }
  
  FileAttributes(Path path) throws Exception {
    this.path                = path;
    this.instantiationMethod = FROMPATH;
    init();
  }
  
  FileAttributes(String fileNameAndPath) throws Exception {
    this.path                = Paths.get(fileNameAndPath);
    this.instantiationMethod = FROMFILENAMEANDPATH;
    init();
  }
  
  private void init() {    
    this.className = this.getClass().getName();
    this.exceptionMessage = "";
    this.checkSumValue = null;
    if (this.path != null) {
      try {
        BasicFileAttributes basicFileAttributes = Files.readAttributes(this.path, BasicFileAttributes.class);    
        
        this.creationTime     = basicFileAttributes.creationTime();
        this.lastAccessTime   = basicFileAttributes.lastAccessTime();
        this.lastModifiedTime = basicFileAttributes.lastModifiedTime();
        this.isDirectory      = basicFileAttributes.isDirectory();
        this.isRegularFile    = basicFileAttributes.isRegularFile();
        this.isSymbolicLink   = basicFileAttributes.isSymbolicLink();
        this.isOther          = basicFileAttributes.isOther();
        this.sizeInBytes      = basicFileAttributes.size();
        
        // We will set checkSumValue in getter (lazy instantiation) done for performance
        //   since only need to set if we need it :)
        
        this.absolutePath = "";
        Path absolutePathObject = path.toAbsolutePath();
        if (absolutePathObject != null ) {
          this.absolutePath = absolutePathObject.toString();
        }
        
        this.fileExtension = "";
        if (this.isDirectory == false) 
          this.fileExtension = getFileExtension(path);        
      } catch (Exception e) {
        exceptionMessage = e.getMessage();
      }      
    }
  }

  public String getFileExtension(Path path) {
    String rtnValue = "";
    String theName = path.getFileName().toString();
    int thePos = theName.lastIndexOf('.');
    if (thePos > 0) // found and the filename doesn't start with .
      rtnValue = theName.substring(thePos+1);
    return rtnValue;
  }
  
  // All the getters
  public String getChecksumAlgorithm() {
    return CHECKSUMALGORITHM;
  }   
  public Path getPath() {
    return path;
  }
  public String getFileName() {
    return path.getFileName().toString();
  }
  public String getPathRoot() {
    Path root = path.getRoot();
    return root != null ? root.toString() : null;
  }
  public String getPathParent() {
    Path parent = path.getParent();
    return parent != null ? parent.toString() : null;
  }
  public String getAbsolutePath() {
    return absolutePath;
  }   
  public String getFileExtension() {
    return fileExtension;
  }
  public FileTime getCreationTime() {
    return creationTime;
  }
  public FileTime getLastAccessTime() {
    return lastAccessTime;
  }
  public FileTime getLastModifiedTime() {
    return lastModifiedTime;
  }
  public boolean isDirectory() {
    return isDirectory;
  }
  public boolean isRegularFile() {
    return isRegularFile;
  }
  public boolean isSymbolicLink() {
    return isSymbolicLink;
  }
  public boolean isOther() {
    return isOther;
  }
  public long getSizeInBytes() {
    return sizeInBytes;
  }
  
  // Lazy instantiaion of the checksum value
  public String getCheckSumValue() {
    if (checkSumValue == null && path != null && this.isRegularFile) { 
      try {
        this.checkSumValue = CheckSum.getFileCheckSumValue(path, CHECKSUMALGORITHM);
      }
      catch (Exception e) {
        System.out.println("CheckSum raised error on:" + path.toString() );
      }
    }
    return checkSumValue;
  }
  public int getInstantiationMethod() {
    return instantiationMethod;
  }
  public String getClassName() {
    return className;
  }
  public String getExceptionMessage() {
    return exceptionMessage;
  }

  // return boolean if an exception was raised with this object
  public boolean hasException() {
    return (exceptionMessage.length() > 0);
  }
  public String getStartingBasePath() {
    return startingBasePath;
  }
  
  public String getPathFromBaseAsUnix() {
    String rtnString = absolutePath.replace('\\', '/');
    String strtString = startingBasePath.replace('\\','/');
    
    int theLen = strtString.length();
    if (theLen > 0 && theLen < rtnString.length()) {
      if (rtnString.substring(0, theLen).compareTo(strtString) == 0) {
        rtnString = rtnString.substring(theLen);
        if (rtnString.startsWith("/")) rtnString = rtnString.substring(1);
      }
    }
    return rtnString;
  }
  
  // -----------------------------------------------------------------------------
  // Setters; these are called when object is deserialized (i.e. from json object)  
  public void setPath(Path path) {
    this.path = path;
  }
  public void setCreationTime(FileTime creationTime) {
    this.creationTime = creationTime;
  }
  public void setLastAccessTime(FileTime lastAccessTime) {
    this.lastAccessTime = lastAccessTime;
  }
  public void setLastModifiedTime(FileTime lastModifiedTime) {
    this.lastModifiedTime = lastModifiedTime;
  }
  public void setDirectory(boolean isDirectory) {
    this.isDirectory = isDirectory;
  }
  public void setRegularFile(boolean isRegularFile) {
    this.isRegularFile = isRegularFile;
  }
  public void setSymbolicLink(boolean isSymbolicLink) {
    this.isSymbolicLink = isSymbolicLink;
  }
  public void setOther(boolean isOther) {
    this.isOther = isOther;
  }
  public void setSizeInBytes(long sizeInBytes) {
    this.sizeInBytes = sizeInBytes;
  }
  public void setCheckSumValue(String checkSumValue) {
    this.checkSumValue = checkSumValue;
  }  
  public void setAbsolutePath(String absolutePath) {
    this.absolutePath = absolutePath;
  }
  public void setFileExtension(String fileExtension) {
    this.fileExtension = fileExtension;
  }
  public void setStartingBasePath(String startingBasePath) {
    this.startingBasePath = startingBasePath;
  }
  
  @Override
  public String toString() {
    return "FileAttributes [instantiationMethod=" + instantiationMethod
        + ", path=" + path 
        + ", getFileName()=" + getFileName() 
        + ", getPathRoot()=" + getPathRoot() 
        + ", getPathParent()=" + getPathParent() 
        + ", absolutePath=" + absolutePath
        + ", startingBasePath=" + startingBasePath
        + ", getPathFromBaseAsUnix()=" + getPathFromBaseAsUnix()
        + ", fileExtension=" + fileExtension
        + ", creationTime=" + creationTime
        + ", lastAccessTime=" + lastAccessTime 
        + ", lastModifiedTime=" + lastModifiedTime 
        + ", isDirectory=" + isDirectory 
        + ", isRegularFile=" + isRegularFile 
        + ", isSymbolicLink=" + isSymbolicLink 
        + ", isOther=" + isOther 
        + ", sizeInBytes=" + sizeInBytes 
        + ", checkSumValue=" + getCheckSumValue() 
        + ", exceptionMessage=" + exceptionMessage + "]";
  }

  // Deserialize object  
  private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
    instantiationMethod = objectInputStream.readInt();
    className           = objectInputStream.readUTF();
    path                = Paths.get(objectInputStream.readUTF());
    startingBasePath    = objectInputStream.readUTF();
    absolutePath        = objectInputStream.readUTF();
    fileExtension       = objectInputStream.readUTF();
    creationTime        = FileTime.fromMillis(objectInputStream.readLong());
    lastAccessTime      = FileTime.fromMillis(objectInputStream.readLong());
    lastModifiedTime    = FileTime.fromMillis(objectInputStream.readLong());
    isDirectory         = objectInputStream.readBoolean();
    isRegularFile       = objectInputStream.readBoolean();
    isSymbolicLink      = objectInputStream.readBoolean();
    isOther             = objectInputStream.readBoolean();
    sizeInBytes         = objectInputStream.readLong();
    checkSumValue       = objectInputStream.readUTF();
    exceptionMessage    = objectInputStream.readUTF();
    if (checkSumValue.length() == 0) checkSumValue = null;  // Set back to null if applicable
  }

  // Serialize object
  private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
    objectOutputStream.writeInt(FROMJAVASERIALIZATION);
    objectOutputStream.writeUTF(className);
    objectOutputStream.writeUTF(path.toString());
    objectOutputStream.writeUTF(startingBasePath);
    objectOutputStream.writeUTF(absolutePath);
    objectOutputStream.writeUTF(fileExtension);
    objectOutputStream.writeLong(creationTime.toMillis());
    objectOutputStream.writeLong(lastAccessTime.toMillis());
    objectOutputStream.writeLong(lastModifiedTime.toMillis());
    objectOutputStream.writeBoolean(isDirectory);
    objectOutputStream.writeBoolean(isRegularFile);
    objectOutputStream.writeBoolean(isSymbolicLink);
    objectOutputStream.writeBoolean(isOther);
    objectOutputStream.writeLong(sizeInBytes);
    objectOutputStream.writeUTF((getCheckSumValue()==null ? "" : getCheckSumValue()));
    objectOutputStream.writeUTF(exceptionMessage); 
  }  
}
