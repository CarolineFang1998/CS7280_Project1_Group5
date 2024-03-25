import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class DB {
  // Variables
  private String name;
  private int blockSize;
  private int numOfFCBFiles; // Number of FCB files, default value 0
  private int numOfPFSFiles; // Number of PFC files, default value 1
  private List<PFS> pfsList;
//  private List<FCB> fcbList;

  // Constructor
  public DB(String name, int blockSize, boolean isLoad) {
    System.out.println("creating DB "  + name);
    this.name = name;
    this.blockSize = blockSize;
    this.pfsList = new ArrayList<>();
//    this.fcbList = new ArrayList<>();

    // todo, load the previous pfs
    init();
  }

  public void init () {
    this.numOfFCBFiles = 0; // Default value
    this.numOfPFSFiles = 0; // Default value

    // create a new PFS file db0
    // -> write all the BitMap(with first 3 blocks full) & superblock info, leave 1 block for FCB block
    PFS pfs = new PFS(this, 0);
    pfsList.add(pfs);
  }

  // todo: init & load the previoud PFS file

  private char[][] convertCSVToCharArray(String filePath) throws IOException {
    List<String> lines = new ArrayList<>();
    try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
      String line;
      boolean firstLine = true;
      while ((line = br.readLine()) != null) {
        if (firstLine) {
          firstLine = false; // Skip the header line
          continue;
        }
        // Truncate or pad the line to ensure it is exactly 40 characters
        if (line.length() > 40) {
          line = line.substring(0, 40);
        } else {
          line = String.format("%-40s", line); // Right-pad with spaces to make it 40 chars
        }
        lines.add(line);
      }
    }

    // Since every line is now exactly 40 characters, set the second dimension to 40
    char[][] records = new char[lines.size()][40];

    for (int i = 0; i < lines.size(); i++) {
      char[] lineChars = lines.get(i).toCharArray();
      System.arraycopy(lineChars, 0, records[i], 0, lineChars.length);
      System.out.println(records[i].toString());
    }

    return records;
  }

  // Method to combine records into blocks
  public List<char[]> recordsToBlock(char[][] data) {
    List<char[]> blocks = new ArrayList<>();
    int blockSize = 256; // The size of each block
    int recordsPerBlock = 6; // Number of records per block
    int recordLength = 40; // Length of each record
    int recordsInCurrentBlock = 0; // Counter for records in the current block
    StringBuilder blockBuilder = new StringBuilder(blockSize);

    for (char[] record : data) {
      blockBuilder.append(record);
      recordsInCurrentBlock++;

      // Check if the block is full or if it's the last record
      if (recordsInCurrentBlock == recordsPerBlock || record.equals(data[data.length - 1])) {
        // Fill the rest of the block with spaces if it's not full
        while (blockBuilder.length() < blockSize) {
          blockBuilder.append(' ');
        }

        // Convert StringBuilder to char[] and add to blocks list
        blocks.add(blockBuilder.toString().toCharArray());

        // Reset for the next block
        blockBuilder.setLength(0); // Clear the StringBuilder
        recordsInCurrentBlock = 0; // Reset the counter
      }
    }

    return blocks;
  }


  public void uploadFCBFile(String fileName) {
    // load file, calculate the record size
    // transfer the file into a datablock char[]

    System.out.println("Uploading FCB File: " + fileName);
    String filePath = "./csvs/" + fileName;
    try {
      char[][] data = convertCSVToCharArray(filePath);
      List<char[]> blocks = recordsToBlock(data);

//      System.out.println("Printing records (first 40 chars each):");
//      for (char[] record : data) {
//        System.out.println(new String(record));
//      }

//      int blockNumber = 1;
//      for (char[] block : blocks) {
//        System.out.println("Block " + blockNumber + ":");
//        System.out.println(new String(block));
//        System.out.println(); // Add an extra newline for better readability between blocks
//        blockNumber++;
//      }

      // todo: block size shoule add up index block
      storeDataInPFS(blocks, fileName, blocks.size());

    } catch (IOException e) {
      System.err.println("An error occurred while reading the file: " + e.getMessage());
    }

  }
  public void storeDataInPFS(List<char[]> blocks, String fileName, int blocksSize) {
    // start and end pointers
    // List<{startPointerString, endPointerString}>
    List<List<String>> dataStartNEndPtrs = new ArrayList<>();

    // List of keyValues
    // List<{key:dataBlockPointer}>
    List<List<String>> keyPointerList = new ArrayList<>();

    // if there is space left in the pfs
    if (pfsList.get(0).getBlockLeft() > 0) {
        // todo: implement the fcb metadata class
      // FCB name: 20 char, time: 14 char (sample:  "15/SEP/23:25PM")
      // number of blocks 10 int.  data start block(7 char): default: 9999999,
      // index start block(7 char): default: 9999999,

      dataStartNEndPtrs.add(pfsList.get(0).addData(blocks, keyPointerList, -1));
      // add index block
      // TODO: function1: generate a index list char[]
      // TODO: function2: see how many space we need and generate a List<Empty Block Lists String> and
      //  replace all the pointer to corresponding String

      // todo: hard coded, change it
      pfsList.get(0).updateFCBMetadeta(fileName, LocalDateTime.now(), blocksSize,
              dataStartNEndPtrs.get(0).get(0), "9999999");

      // data to key:value pair






      // TODO: add index block as well
      // TODO: the end pointer could be in the next pfs file
    }
    // TODO: check other block could store
  }

  // todo: updateDataBlockPointer



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

