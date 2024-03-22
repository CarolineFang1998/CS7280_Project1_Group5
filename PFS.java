import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class PFS {
  private DB db; // A DB object
  private int sequenceNumber; // An integer sequence number
  private char[][] content;
  private int blockLeft;

  public PFS(DB db, int PFSNumber) {
    System.out.println("creating PFS");
    this.db = db;
    this.sequenceNumber = PFSNumber; // if .db0, sequenceNumber = 0
    this.content = new char[4000][db.getBlockSize()]; // first block is always bitmap
    this.blockLeft = 4000;
    // check if this file is already exist
    if(db.getNumOfPFSFiles() <=  sequenceNumber + 1) {
      // this.content = loadExistingPFS()
    } else {
      if(PFSNumber == 0) {
        System.out.println("PFSNumber == 0");
        // init the .db0 with write all the superblock info & BitMap(with first 3 blocks full), leave 1 block for FCB block
        initFirstPFS();
        // write this into .db0 file
      } else {
        // only create a .dbN file and init bitmap
//        initMorePFS();
        // write this into .dbN file
      }
    }

    // write the current char array to .dbfile
    try {
      writeCharArrayToFile();
      System.out.println("File written successfully.");
    } catch (IOException e) {
      System.err.println("An error occurred while writing the file: " + e.getMessage());
    }
  }

  public void initFirstPFS(){
    // fill the four line with Hexadecimal bit map 0-F, first 250 char, update the block 0 full
    for (int i = 0; i < 250; i++) {
      this.content[0][i] = '0';
    }

    updateBitMap(0, false);

    // fill the fifth line with superblock info, update the block 0 tobe full
    // db name(first 30 , offset 0~29), db numOfFCBFiles, db numOfPFSFiles, db blocksize(), .
    // update
//    updateBitMap(5, false);

    // leave the third block, but mark it empty. it will be the FCB infos.
//    updateBitMap(6, false);
  }

  // TODO: write a function which accept the position in int, and mark the bitMap empty, and update the block size
  // blockNum is from 0 to 3999, isBlockEmpty true means set the block to empty
  public void updateBitMap(int blockNum, boolean isBlockEmpty) {
    int hexIndex = blockNum / 4; // Determine the hex character's index in the bitmap
    int bitPosition = blockNum % 4; // Determine the bit's position within the hex character

    // Convert the hex character to binary
    char hexChar = this.content[0][hexIndex];
    int value = Character.digit(hexChar, 16);
    int[] binary = new int[4];
    for (int i = 3; i >= 0; i--) {
      binary[i] = value % 2;
      value /= 2;
    }

    // Check the current status before changing it
    boolean isCurrentlyEmpty = binary[bitPosition] == 0;

    // Update the binary value based on isBlockEmpty
    binary[bitPosition] = isBlockEmpty ? 0 : 1;

    // Adjust blockSize based on the change
    if (isCurrentlyEmpty && !isBlockEmpty) {
      // If the block was empty (0) and is now used (1), decrease blockSize
      this.db.setBlockSize(this.blockLeft - 1);
    } else if (!isCurrentlyEmpty && isBlockEmpty) {
      // If the block was used (1) and is now empty (0), increase blockSize
      this.db.setBlockSize(this.blockLeft + 1);
    }
    // Note: If the status does not change, do not adjust blockSize

    // Convert the binary back to a single hexadecimal character
    int newValue = binary[0] * 8 + binary[1] * 4 + binary[2] * 2 + binary[3];
    this.content[0][hexIndex] = Integer.toHexString(newValue).toUpperCase().charAt(0);
  }



  // TODO: load Existings PFS, return the char[][]
  // char[][] loadExistingPFS()

  // load the char[4000][256] into .db file
  public void writeCharArrayToFile() throws IOException {
    String fileName = this.db.getName() + ".db" + this.sequenceNumber;
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
      for (char[] row : this.content) {
        writer.write(row);
        writer.newLine(); // Use this if you want each row in a new line, remove if not needed
      }
    }
  }


  // TODO: add file. Method to add a char[] to the data array if there's space
//  public boolean addData(char[] newData) {
//    for (int i = 0; i < data.length; i++) {
//      if (data[i] == null) { // Find the first null (empty) slot
//        data[i] = newData; // Assign the new char array to this slot
//        return true; // Data added successfully
//      }
//    }
//    return false; // No space available
//  }

  public int getSequenceNumber() {
    return sequenceNumber;
  }

  public void setSequenceNumber(int sequenceNumber) {
    this.sequenceNumber = sequenceNumber;
  }
}
