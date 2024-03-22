public class DB {
  // Variables
  private String name;
  private int blockSize;
  private int numOfFCBFiles; // Number of FCB files, default value 0
  private int numOfPFSFiles; // Number of PFC files, default value 1

  // Constructor
  public DB(String name, int blockSize) {
    System.out.println("creating DB "  + name);
    this.name = name;
    this.blockSize = blockSize;
    this.numOfFCBFiles = 0; // Default value
    this.numOfPFSFiles = 0; // Default value

    // TODO: init a PFS file.
    // create a new PFS file db0-> write all the superblock info & BitMap(with first 3 blocks full), leave 1 block for FCB block
    PFS pfs = new PFS(this, 0);

  }

  // Getters and Setters
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getBlockSize() {
    return blockSize;
  }

  public void setBlockSize(int blockSize) {
    this.blockSize = blockSize;
  }

  public int getNumOfFCBFiles() {
    return numOfFCBFiles;
  }

  public void setNumOfFCBFiles(int numOfFCBFiles) {
    this.numOfFCBFiles = numOfFCBFiles;
  }

  public int getNumOfPFSFiles() {
    return numOfPFSFiles;
  }

  public void setNumOfPFSFiles(int numOfPFSFiles) {
    this.numOfPFSFiles = numOfPFSFiles;
  }

  // Additional methods related to DB operations can be added here
}

