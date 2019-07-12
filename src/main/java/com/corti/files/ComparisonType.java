package com.corti.files;

public enum ComparisonType {
  MATCH ("Match"),     // Records match
  CHECKSUM ("Checksum"),  // Records have different checksum value
  FILETIME ("Filetime");  // Different filetimes
  
  private final String theDesc;
  ComparisonType(String theDesc) {
    this.theDesc = theDesc;
  }
  
  public String getDesc() {
    return theDesc;
  }
}
