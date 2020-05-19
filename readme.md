This repo has file/directory utilities, some of them
- Get all directories and subdirectories from a given path
- Get all the files for a directory (detail file attributes on each file)
Both of utils above allow filtering (include/excludes) and limits
- Serialize array of file attributes to a file
- CheckSum - various algorithm (i.e. MD", "SHA-1, SHA-256)

Test code below needs cleaning up but, it'll show you how some of the code works
- CheckSum example: src/test/java/com/corti/files/checksum/CheckSumTest.java
- Compare files (from serialized file listings): src/test/java/com/corti/files/CompareFileAttributesInSerializedFilesTest.java
- Shows how to create a serialized file of attributes: src/test/java/com/corti/files/GetFileAttributesSerializationFileTest.java

I'll update this readme and clean up code when I get some time