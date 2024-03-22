find . -name "*.class" -exec rm {} +
find . -name "*.db*" -exec rm {} +
javac FileSystem.java
java FileSystem
