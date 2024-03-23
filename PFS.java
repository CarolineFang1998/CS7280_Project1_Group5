import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PFS {
  private DB db; // A DB object
  private int sequenceNumber; // An integer sequence number
  private char[][] content;
  private int blockLeft;
  private int emptyBlock;

  public PFS(DB db, int PFSNumber) {
    System.out.println("creating PFS" + PFSNumber);
    this.db = db;
    this.sequenceNumber = PFSNumber; // if .db0, sequenceNumber = 0
    this.content = new char[4000][db.getBlockSize()]; // first block is always bitmap

    // check if this file is already exist
    if(db.getNumOfPFSFiles() >=  sequenceNumber + 1) {
      this.blockLeft = this.calculateBlocksLeft();
      // this.content = loadExistingPFS()
      // TODO: load existing PFS
    } else {
      this.blockLeft = 4000;
      if(PFSNumber == 0) {
        System.out.println("PFSNumber == 0");
        // init the .db0 with write all the superblock info & BitMap(with first 3 blocks full), leave 1 block for FCB block
        initFirstPFS();
        // write this into .db0 file
        System.out.println("Number of FCB Files out: " + this.db.getNumOfFCBFiles());
      } else {
        // TODO: write create .dbN
        // only create a .dbN file and init bitmap
//        initMorePFS();
        // write this into .dbN file
      }

    }

    this.emptyBlock = findNextFreeBlock();
    System.out.println("this.emptyBlock " + this.emptyBlock);

    // write the current char array to .dbfile
    try {
      writeCharArrayToFile();
      System.out.println("File written successfully.");
    } catch (IOException e) {
      System.err.println("An error occurred while writing the file: " + e.getMessage());
    }
  }

  // blocks is the already produced blocks,
  // continuePFSNum means the end block will be in the next PFS file, -1 is this is the only
  // datablock(no space): dblock0 dblock1 dblock2 dblock3 dblock4 dblock5 ->
  public String addData(List<char[]> blocks, int continuePFSNum) {
    BlockPointer startBp = new BlockPointer(this.sequenceNumber, this.emptyBlock);
    int counter = 0;
    for (char[] block : blocks) {
      counter ++;

      if (this.emptyBlock >= content.length) {
        System.out.println("No more empty blocks available.");
        break; // Exit if there are no more empty blocks
      }

      System.out.println("Empty Block " + emptyBlock + ":");
      System.out.println(new String(block));

      System.arraycopy(block, 0, this.content[this.emptyBlock], 0, block.length);
//      this.content[this.emptyBlock] = block;
//      for (int i = 0; i < block.length; i++) {
//        this.content[this.emptyBlock][i] = block[i];
//      }

      char[] pointerCharArray = null;

      updateBitMap(this.emptyBlock, true);
      int nextEmptyBlock = findNextFreeBlock();

      if (counter < blocks.size()) {
        // Generate the 7-digit pointer as a char array
        BlockPointer bp = new BlockPointer(sequenceNumber, nextEmptyBlock);
        String pointerString = bp.getPtrString();
        pointerCharArray = pointerString.toCharArray();
      } else {
        if(continuePFSNum == -1) {
          // the end data block
          String pointerString = "9999999";
          pointerCharArray = pointerString.toCharArray();
        } else {
          // TODO: this might be continue in next PFS file
        }


      }

      // Insert the 7-digit pointer into the last 7 characters of the block in content
      int pointerStartIndex = db.getBlockSize() - 7; // Start index for the 7-digit pointer
      for (int i = 0; i < pointerCharArray.length; i++) {
        content[this.emptyBlock][pointerStartIndex + i] = pointerCharArray[i];
      }

      this.emptyBlock = nextEmptyBlock;
    }

    try {
      writeCharArrayToFile();
      System.out.println("File written successfully.");
    } catch (IOException e) {
      System.err.println("An error occurred while writing the file: " + e.getMessage());
    }
    updateSuperBlock();

    return startBp.getPtrString();
  }

  public void initFirstPFS(){
    db.setNumOfPFSFiles(db.getNumOfPFSFiles()+1);

    // fill the four line with Hexadecimal bit map 0-F, first 3 blocks char
    for (int i = 0; i < 256; i++) {
      this.content[0][i] = '0';
      this.content[1][i] = '0';
      this.content[2][i] = '0';
    }
    // update the block 0, 1, 2 full
    updateBitMap(0, true);
    updateBitMap(1, true);
    updateBitMap(2, true);



    // leave the third block, but mark it empty. it will be the FCB infos.
    updateBitMap(4, true);

    // fill the fifth line with superblock info, update the block 0 tobe full
    // db name(first 30 , offset 0~29), db numOfFCBFiles(1 byte), db numOfPFSFiles(5 bytes), db blocksize(3 bytes), .
    updateSuperBlock();
  }

  // TODO: check if this is correct
  public int calculateBlocksLeft() {
    int blocksLeft = 0;

    // Iterate over each character in the bitmap
    for (int line = 0; line < 3; line++) { // Assuming the bitmap is in the first three lines
      for (int i = 0; i < 256; i++) {
        char hexChar = this.content[line][i];
        int value = Character.digit(hexChar, 16);

        // Convert the hexadecimal value to binary and count the zeros
        for (int bit = 0; bit < 4; bit++) {
          if ((value & (1 << bit)) == 0) {
            blocksLeft++; // Increment for each block that is free
          }
        }

      }
    }
    return blocksLeft;
  }

  // TODO: check if this is correct
  public int findNextFreeBlock() {
    // Loop through each character in the bitmap (each character represents 4 blocks)
    for (int hexIndex = 0; hexIndex < this.content[0].length; hexIndex++) {
      char hexChar = this.content[0][hexIndex];
      int value = Character.digit(hexChar, 16);

      // Convert the hex value to its binary representation
      for (int bitPosition = 0; bitPosition < 4; bitPosition++) {
        // Check if the bit is 0 (indicating a free block)
        if ((value & (1 << (3 - bitPosition))) == 0) {
          // Calculate and return the block's index
          return hexIndex * 4 + bitPosition;
        }
      }
    }
    // If no free blocks are found, return -1 or an appropriate value to indicate this
    return -1;
  }

  // put fcb metadata into content
  public void updateFCBMetadeta(String FCBName, LocalDateTime time, int size,
                                  String dataBlockStart, String indexStartPointer) {
    char[] metadeta = generateFCBMetadata(FCBName, time, size, dataBlockStart, indexStartPointer);
//    this.content[4] = metadeta;
    System.arraycopy(metadeta, 0, this.content[4], 0, metadeta.length);

    // update super block
    db.setNumOfFCBFiles(db.getNumOfFCBFiles()+1);
    updateSuperBlock();

    // write the current char array to .dbfile
    try {
      writeCharArrayToFile();
      System.out.println("File written successfully.");
    } catch (IOException e) {
      System.err.println("An error occurred while writing the file: " + e.getMessage());
    }
  }

  public char[] generateFCBMetadata(String FCBName, LocalDateTime time, int size,
                             String dataBlockStart, String indexStartPointer) {

    // Ensure the FCBName fits into 20 bytes, truncating if necessary
    if (FCBName.length() > 20) {
      FCBName = FCBName.substring(0, 20);
    } else {
      // Pad the FCBName to ensure it is exactly 20 characters long
      while (FCBName.length() < 20) {
        FCBName += " ";
      }
    }

    // Format the time into a 14-byte string
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MMM/yy:HHa");
    String formattedTime = time.format(formatter);

    // Convert the size to a String and ensure it is exactly 10 bytes
    String sizeStr = String.valueOf(size);
    while (sizeStr.length() < 10) {
      sizeStr = "0" + sizeStr; // Pad with spaces to align to the right
    }
    System.out.println("sizeStr " + sizeStr);

    // Prepare the final metadata string
    String metadataStr = FCBName + formattedTime + sizeStr + dataBlockStart + indexStartPointer;
    // Convert the metadata string to a char array and return
    return metadataStr.toCharArray();
  }

  public void updateSuperBlock() {
    // Update SuperBlock will be in the block 3
    updateBitMap(3, true);

    System.out.println("Database Name: " + this.db.getName());
    System.out.println("Number of FCB Files: " + this.db.getNumOfFCBFiles());
    System.out.println("Number of PFS Files: " + this.db.getNumOfPFSFiles());
    System.out.println("Block Size: " + this.db.getBlockSize());

    String dbName = this.db.getName();
    int numOfFCBFiles = this.db.getNumOfFCBFiles();
    int numOfPFSFiles = this.db.getNumOfPFSFiles();
    int blockSize = this.db.getBlockSize();

    // 0-29 will be db name
    // Ensure the database name is not longer than 30 characters
    if (dbName.length() > 30) {
      dbName = dbName.substring(0, 30);
    } else {
      // Right-pad the database name with spaces to ensure it fills 30 characters
      while (dbName.length() < 30) {
        dbName += " ";
      }
    }
    // fillin db name
    for (int i = 0; i < dbName.length(); i++) {
      this.content[3][i] = dbName.charAt(i);
    }


    // 30 fillin fcb size
    this.content[3][30] = String.valueOf(numOfFCBFiles).toCharArray()[0];


    // 31-35 is # of PFC file number
    String numOfPFSFilesString = String.valueOf(numOfPFSFiles);
    char[] numOfPFSFilesChars = numOfPFSFilesString.toCharArray();
    int it=31;
    int startingI = 31;

    for( ;it <= 35; it++) {
      if(it < startingI + numOfPFSFilesChars.length) {
        this.content[3][it] = numOfPFSFilesChars[it-startingI];
      } else {
        this.content[3][it] = ' ';
      }
    }


    // 36-38 is # of block size
    String blockSizeString = String.valueOf(blockSize);
    char[] blockSizeChars = blockSizeString.toCharArray();
    startingI = 36;

    for(; it <= 38; it++) {
      if(it < startingI + blockSizeChars.length) {
        this.content[3][it] = blockSizeChars[it - startingI];
      } else {
        this.content[3][it] = ' ';
      }
    }
  }




  // function which accept the position in int, and mark the bitMap empty, and update the block size
  // blockNum is from 0 to 3999, isBlockEmpty true means set the block to empty
  public void updateBitMap(int blockNum, boolean isBecomeFull) {
    int hexIndex = blockNum / 4; // Determine the hex character's index in the bitmap
    int bitPosition = blockNum % 4; // Determine the bit's position within the hex character
    System.out.println("hexIndex"+hexIndex+" bitPosition " + bitPosition);

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

    // Adjust blockSize based on the change
    if (isCurrentlyEmpty && isBecomeFull) {
      // If the block was empty (0) and is now used (1), dec blockLeft
      this.blockLeft -=  1;
    } else if (!isCurrentlyEmpty && !isBecomeFull) {
      // If the block was used (1) and is now empty (0), inc blockLeft
      this.blockLeft += 1;
    }
    // Note: If the status does not change, do not adjust blockSize

    // Update the binary value based on isBlockEmpty
    binary[bitPosition] = isBecomeFull ? 1 : 0;

    // Convert the binary back to a single hexadecimal character
    // 1000 (8) is for 1st block full, 0100 (4) is for 2nd block full
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


  public int getSequenceNumber() {
    return sequenceNumber;
  }

  public void setSequenceNumber(int sequenceNumber) {
    this.sequenceNumber = sequenceNumber;
  }

  public int getBlockLeft() {
    return blockLeft;
  }
}
