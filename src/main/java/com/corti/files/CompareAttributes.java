package com.corti.files;

// Simple class for a pair of numbers
public class CompareAttributes {
  public int a;
  public int b;  
  public ComparisonType deltaType;
  CompareAttributes(int a, int b, ComparisonType compType) {
    this.a = a;
    this.b = b;
    this.deltaType = compType;
  }
}
