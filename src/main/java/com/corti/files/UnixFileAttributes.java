package com.corti.files;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.util.Set;

public class UnixFileAttributes extends FileAttributes {
  private transient Set<PosixFilePermission> permissions;
  transient PosixFileAttributes posixFileAttributes;
  
  public UnixFileAttributes(String fileNameAndPath) throws Exception {
    super(fileNameAndPath);
    init();
  }
  
  public UnixFileAttributes(Path path) throws Exception {
    super(path);
    init();    
  }
  
  public UnixFileAttributes() throws Exception {
    super();
    init();
  }
  
  private void init() throws IOException {
    if (path != null) {
      permissions = Files.getPosixFilePermissions(path);
      posixFileAttributes = Files.getFileAttributeView(path, PosixFileAttributeView.class)
          .readAttributes();
    }
  }

  public Set<PosixFilePermission> getPermissions() {
    return permissions;
  }

  public PosixFileAttributes getPosixFileAttributes() {
    return posixFileAttributes;
  }

  public void setPermissions(Set<PosixFilePermission> permissions) {
    this.permissions = permissions;
  }

  public void setPosixFileAttributes(PosixFileAttributes posixFileAttributes) {
    this.posixFileAttributes = posixFileAttributes;
  }
  
  public String getOwner() throws IOException {
    UserPrincipal userPrincipal = posixFileAttributes.owner();
    return userPrincipal.getName();
  }
  
  public String getGroup() throws IOException {
    GroupPrincipal groupPrincipal = posixFileAttributes.group();
    return groupPrincipal.getName();
  }

  @Override
  public String toString() {
    return super.toString() + " UnixFileAttributes [permissions=" + PosixFilePermissions.toString(permissions) + "]";
  }

}
