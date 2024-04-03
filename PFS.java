import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * This object is the PFS which is content of .db file.
 * It could help the database stores metadata, data blocks and index block.
 * For all the .db files, block 0~3 are hexadecimal bitmap.
 * For .db0, block 4 is the database metadata. block 5 is the FCB metadetas
 */
public class PFS {
  private DB db; // A DB object
  private int sequenceNumber; // An integer sequence number
  private char[][] content; // The char[4000][256] stores all the data
  private int blockLeft; // how many block left for this PFS file
  private int emptyBlock; // block # for next empty block
  private String fileName; // the file name for this PFS file
//  private List<KeyPointer> keyPointerList;


  /**
   * Constructor for creating a new PFS instance.
   * Initializes the content array, checks for the existence of the file, and prepares the bitmap.
   *
   * @param db        The DB object this PFS is associated with.
   * @param PFSNumber The sequence number for this PFS file. Start with 0
   */
  public PFS(DB db, int PFSNumber) {
    System.out.println("creating PFS .db" + PFSNumber + "...");
    this.db = db;
    this.sequenceNumber = PFSNumber; // if .db0, sequenceNumber = 0
    this.content = new char[4000][db.getBlockSize()]; // first block is always bitmap
    this.fileName = db.getName() + ".db" + PFSNumber;
//    this.keyPointerList = new ArrayList<>();

    // check if this file is already exist
    if (db.getNumOfPFSFiles() >= sequenceNumber + 1) {
      this.blockLeft = this.calculateBlocksLeft();
      this.content = loadExistingPFS();
      // TODO: load existing PFS
    } else {
      this.blockLeft = 4000;
      if (this.sequenceNumber == 0) {
        System.out.println("PFSNumber == 0");
        // init the .db0 with write all the superblock info & BitMap(with first 3 blocks full),
        // leave 1 block for FCB block
        // write this into .db0 file
        initFirstPFS();
        System.out.println("Number of FCB Files out: " + this.db.getNumOfFCBFiles());
        System.out.println("blockLeft: " + this.blockLeft);
      } else {
        System.out.println("PFSNumber == " + PFSNumber);
        // only create a .dbN file and init bitmap
        // write this into .dbN file
        initMorePFS();
      }
    }

    this.emptyBlock = findNextFreeBlock();
//    System.out.println("this.emptyBlock " + this.emptyBlock);

    // write the current char array to .dbfile
    try {
      writeCharArrayToFile();
      System.out.println("File written successfully.");
    } catch (IOException e) {
      System.err.println("An error occurred while writing the file: " + e.getMessage());
    }
  }


  // blocks is the already produced blocks,
  // keyPointerList is the start and end pointer in string
  // datablock(no space): dblock0 dblock1 dblock2 dblock3 dblock4 dblock5 -> block pointer
  // returns the start pointer and end pointer in string

  /**
   * Adds data blocks to the PFS file and returns pointers to the start and end of the written blocks.
   * Updates the bitmap and block pointers accordingly.
   *
   * @param blocks         The list of data blocks. 6 records
   *                       sample blocks(no space in block):
   *                       List<{dblock0 dblock1 dblock2 dblock3 dblock4 dblock5 ->block pointer}>
   * @param keyPointerList A list of Strings within the data blocks.
   *                       sample:        List<{String key, String value}>
   *                       {"1", "1,Toy Story (1995),Adventure|Animation|C"}
   * @return A list containing the start and end pointers to the added data blocks.
   * {start pointer,end pointer}  pointer is a block pointer with 7 char.
   */
  public List<String> addData(List<char[]> blocks, List<KeyPointer> keyPointerList) {
    this.emptyBlock = findNextFreeBlock();

    BlockPointer startBp = new BlockPointer(this.sequenceNumber, this.emptyBlock);

    List<String> startNEndPtrs = new ArrayList<>();
    startNEndPtrs.add(startBp.getPtrString()); // add the start pointer into startNEndPtrs

    // Inserting blocks to data block
    int counter = 0;
    for (char[] block : blocks) {
      counter++;
      int currBlock = this.emptyBlock;

      if (currBlock >= content.length) {
        System.out.println("No more empty blocks available.");
        break; // Exit if there are no more empty blocks
      }

      // Put one block in corresponding this.content
      System.arraycopy(block, 0, this.content[currBlock], 0, block.length);

      updateBitMap(currBlock, true); // mark this block full and update blockLeft

      // Update block pointer
      int nextEmptyBlock = findNextFreeBlock();
      String pointerString;
      if (counter < blocks.size()) {
        // if this is not the end block, point to the next empty block
        BlockPointer bp = new BlockPointer(sequenceNumber, nextEmptyBlock);
        pointerString = bp.getPtrString();
      } else {
        // if this is the end block
        pointerString = "9999999";
        BlockPointer curDP = new BlockPointer(sequenceNumber, currBlock);
        startNEndPtrs.add(curDP.getPtrString()); // add the end pointer into startNEndPtrs
      }
      updateBlockPointer(currBlock, pointerString);

      // insert value into keyPointerList
      updateKeyPointerList(block, keyPointerList, currBlock);
      this.emptyBlock = nextEmptyBlock;
    }

    try {
      writeCharArrayToFile();
      System.out.println("File written successfully.");
    } catch (IOException e) {
      System.err.println("An error occurred while writing the file: " + e.getMessage());
    }

//    System.out.println("keyPointerList.size() " + keyPointerList.size());
    return startNEndPtrs;
  }

  /**
   * Updates the keyPointerList so it could help us to identify where the data stores.
   * List<{key:dataBlockPointer}>
   *
   * @param block          The list of data blocks. 6 records
   *                       sample blocks(no space in block):
   *                       List<{dblock0 dblock1 dblock2 dblock3 dblock4 dblock5 ->block pointer}>
   * @param keyPointerList A list of pointers to keys within the data blocks.
   *                       sample:        List<{String key, String value}>
   *                       {"1", "1,Toy Story (1995),Adventure|Animation|C"}
   * @param blockNum       The block number where the data is stored. From 0 to 3999
   */
  public void updateKeyPointerList(char[] block, List<KeyPointer> keyPointerList, int blockNum) {
    // 6 records in one data block, each 40 characters long
    int recordLength = 40;
    for (int i = 0; i < 6; i++) {
      // Calculate the start and end indices for the current record
      int start = i * recordLength;

      // Extract the current record from the block
      String record = new String(block, start, Math.min(recordLength, block.length - start));

      // Find the index of the first comma to separate the key from the rest of the record
      int commaIndex = record.indexOf(',');
      if (commaIndex == -1) {
        // Handle the case where the comma is missing or this is an empty record
        continue;
      }

      // Extract the key (everything before the comma)
      String key = record.substring(0, commaIndex);

      // Generate the DataBlockPointer for this record
      DataBlockPointer dbPointer = new DataBlockPointer(this.sequenceNumber, blockNum, i);

      // Create the current record list containing the key and DataBlockPointer string
      KeyPointer currKeyPtr = new KeyPointer(Integer.valueOf(key), dbPointer.getPtrString());
//      System.out.println(currKeyPtr.getKey() + " " + currKeyPtr.getKeyPointerStr());

      // Add the current record to the keyPointerList
      keyPointerList.add(currKeyPtr);
    }
//    // store the keyPointerList to this.keyPointerList
//    this.keyPointerList = keyPointerList;
  }


  /**
   * Updates the pointer at the end of a specified block with a new value.
   *
   * @param blockNum The block number to update the pointer for.
   * @param pointer  The new pointer value to write at the end of the block. String size will be 7.
   *                 example:
   *                 0010005 means 001 is store in .db1, 0005 block# 5
   */
  void updateBlockPointer(int blockNum, String pointer) {
    // Validate the pointer length
    if (pointer == null || pointer.length() != 7) {
      throw new IllegalArgumentException("Pointer must be exactly 7 characters long.");
    }

    // Calculate the start index for the 7-character pointer within the block
    int pointerStartIndex = this.db.getBlockSize() - 7;

    // Convert the pointer string to a char array
    char[] pointerChars = pointer.toCharArray();

    // Update the last 7 characters of the specified block
    for (int i = 0; i < pointerChars.length; i++) {
      this.content[blockNum][pointerStartIndex + i] = pointerChars[i];
    }
  }

  // todo: updateDataBlockPointer
  // this pointer is used in index file
//  int void updateDataBlockPointer(int blockNum, int recordNum, String Pointer) {
//
//  }

  /**
   * Initializes the bitmap for the PFS file, setting up the initial state of the blocks.
   */
  public void initBitMap() {
    // fill the four line with Hexadecimal bit map 0-F, first 3 blocks char
    for (int i = 0; i < 256; i++) {
      this.content[0][i] = '0';
      this.content[1][i] = '0';
      this.content[2][i] = '0';
      this.content[3][i] = '0';
    }
    // update the block 0, 1, 2, 3 full
    updateBitMap(0, true);
    updateBitMap(1, true);
    updateBitMap(2, true);
    updateBitMap(3, true);
  }

  /**
   * Initializes the first PFS file (.db0), setting up the superblock and bitmap.
   */
  public void initFirstPFS() {
    db.setNumOfPFSFiles(db.getNumOfPFSFiles() + 1);

    initBitMap();

    // leave the 6th block, but mark it empty. it will be the FCB infos.
    updateBitMap(5, true);

    // fill the 5th line with superblock info, update the block 0 tobe full
    // db name(first 30 , offset 0~29), db numOfFCBFiles(1 byte), db numOfPFSFiles(5 bytes),
    // db blocksize(3 bytes) .
    updateSuperBlock();

  }

  /**
   * Initializes additional PFS files (.db1, .db2, etc.), setting up only the bitmap.
   */
  public void initMorePFS() {
    db.setNumOfPFSFiles(db.getNumOfPFSFiles() + 1);

    initBitMap();
  }

  /**
   * Updates the number of PFS files stored in the superblock of the .db0 file.
   *
   * @param numOfPFSFiles The new number of PFS files to record.
   */
  public void updateSuperBlockNumOfPFSFiles(int numOfPFSFiles) {
    if (this.sequenceNumber == 0) {
      int superBlockNum = 4; // super block is in 5th block
      // 31-35 is # of PFC file number
      String numOfPFSFilesString = String.valueOf(numOfPFSFiles);
      char[] numOfPFSFilesChars = numOfPFSFilesString.toCharArray();
      int it = 31;
      int startingI = 31;

      for (; it <= 35; it++) {
        if (it < startingI + numOfPFSFilesChars.length) {
          this.content[superBlockNum][it] = numOfPFSFilesChars[it - startingI];
        } else {
          this.content[superBlockNum][it] = ' ';
        }
      }

    } else {
      System.out.println("only update SuperBlock info in .db0");
    }

    // write the current char array to .dbfile
    try {
      writeCharArrayToFile();
      System.out.println("File written successfully.");
    } catch (IOException e) {
      System.err.println("An error occurred while writing the file: " + e.getMessage());
    }
  }

  /**
   * Updates the number of FCB files stored in the superblock of the .db0 file.
   *
   * @param numOfFCBFiles The new number of FCB files to record.
   */
  public void updateSuperBlockNumOfFCBFiles(int numOfFCBFiles) {
    if (this.sequenceNumber == 0) {
      int superBlockNum = 4; // super block is in 5th block
      // 30 fillin fcb size
      this.content[superBlockNum][30] = String.valueOf(numOfFCBFiles).toCharArray()[0];
    } else {
      System.out.println("only update SuperBlock info in .db0");
    }

    // write the current char array to .dbfile
    try {
      writeCharArrayToFile();
      System.out.println("File written successfully.");
    } catch (IOException e) {
      System.err.println("An error occurred while writing the file: " + e.getMessage());
    }
  }

  /**
   * Calculates and returns the number of free blocks left in the PFS file.
   *
   * @return The number of free blocks left.
   */
  public int calculateBlocksLeft() {
    int blocksLeft = 0;
    int counter = 1000;
    // Iterate over each character in the bitmap
    for (int line = 0; line < 4; line++) { // Assuming the bitmap is in the first four lines
      for (int i = 0; i < 256; i++) {
        if (counter <= 0) return blocksLeft;
        counter--;
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

  /**
   * Finds the next free block in the PFS file by examining the bitmap.
   *
   * @return The block number of the next free block.
   */
  public int findNextFreeBlock() {
    // Loop through each row and column of the content array
    int counter = 1000;
    for (int row = 0; row < 4; row++) {
      for (int col = 0; col < 256; col++) {
        // If no free block is found, return -1
        if (counter == 0) return -1;
        counter--;
        char hexChar = this.content[row][col];

        // Convert the hexadecimal character to a binary string
        String binaryString = Integer.toBinaryString(Character.digit(hexChar, 16));

        // Left-pad the binary string with zeros to ensure it has 4 bits
        binaryString = String.format("%4s", binaryString).replace(' ', '0');

        // Check each bit to find the first '0'
        for (int bit = 0; bit < 4; bit++) {
          if (binaryString.charAt(bit) == '0') {

            // Calculate and return the block number
            int blockNumber = (row * 256 * 4) + (col * 4) + bit;
//            System.out.println("Next empty blockNumber " + blockNumber);
            return blockNumber;
          }
        }
      }
    }
    // If no free block is found, return -1
    return -1;
  }

  public void findEmptyBlocks(int assignedBlock, List<String> emptyBlocks) {
    this.emptyBlock = findNextFreeBlock(); // make sure emptyBlock variable is the latest
    for(int i = 0; i < assignedBlock; i++) {
      int curr_block = this.emptyBlock;
      BlockPointer bp = new BlockPointer(this.sequenceNumber, curr_block);
      emptyBlocks.add(bp.getPtrString());

      // update BitMap status and mark curr_block full
      updateBitMap(curr_block, true);
      this.emptyBlock = findNextFreeBlock(); // make sure emptyBlock variable is the latest
    }
  }

  /**
   * Updates the FCB (File Control Block) metadata with provided details and stores it in the PFS.
   * This includes the FCB name, creation or modification time, size, and pointers to data and index blocks.
   *
   * @param FCBName           Name of the FCB.
   * @param time              Timestamp for the FCB, typically creation or modification time.
   * @param size              Size of the FCB, often reflecting the size of the data it controls.
   * @param dataBlockStart    Pointer to the start of the data block for this FCB.
   * @param indexStartPointer Pointer to the start of the index block for this FCB.
   */
  public void updateFCBMetadeta(String FCBName, LocalDateTime time, int size,
                                String dataBlockStart, String indexStartPointer) {
    char[] metadata = generateFCBMetadata(FCBName, time, size, dataBlockStart, indexStartPointer);
//    System.arraycopy(metadeta, 0, this.content[5], 0, metadeta.length);
    int existingMetadataCount= this.db.getNumOfFCBFiles();
    if (existingMetadataCount < 4) {
      // There is space, append the new metadata
      appendMetadataToBlock(this.content[5], metadata, existingMetadataCount);
    } else {
      // The block is full, find a new empty block and update the pointer in the current block
      int newBlockIndex = findNextFreeBlock(); // Implement this to find an empty block
      if (newBlockIndex != -1) {
        // Update the pointer in the current block to the new block
        updateBlockPointer(newBlockIndex, String.format("%07d", newBlockIndex));

        // Store the new metadata in the new block
        appendMetadataToBlock(this.content[newBlockIndex], metadata, 0);
      } else {
        System.err.println("No empty block available.");
        return;
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
private void appendMetadataToBlock(char[] block, char[] metadata, int existingMetadataCount) {
  final int METADATA_SIZE = 57; // Size of each metadata entry
  final int MAX_ENTRIES = 4; // Maximum number of metadata entries per block
  final int POINTER_SIZE = 10; // Size of the block pointer
  final int BLOCK_SIZE = METADATA_SIZE * MAX_ENTRIES + POINTER_SIZE; // Total block size

  // Calculate the starting position for the new metadata in the block
  int startPosition = existingMetadataCount * METADATA_SIZE;

  if (existingMetadataCount >= MAX_ENTRIES) {
    System.err.println("Block is already full. Cannot append more metadata.");
    return;
  }

  if (startPosition + METADATA_SIZE > BLOCK_SIZE - POINTER_SIZE) {
    System.err.println("Insufficient space in the block for new metadata.");
    return;
  }

  // Append the metadata to the block
  for (int i = 0; i < metadata.length; i++) {
    block[startPosition + i] = metadata[i];
  }

  if (existingMetadataCount + 1 == MAX_ENTRIES) {
    // The block is now full after adding this metadata; find and write the next block pointer
    int nextBlockIndex = findNextFreeBlock(); // Implement this method to find the index of the next empty block
    if (nextBlockIndex != -1) {
      // Assuming the pointer is stored as a fixed-size string representation of the block index
      String pointerStr = String.format("%" + POINTER_SIZE + "s", nextBlockIndex);
      char[] pointerCharArray = pointerStr.toCharArray();
      System.arraycopy(pointerCharArray, 0, block, BLOCK_SIZE - POINTER_SIZE, POINTER_SIZE);
    } else {
      System.err.println("No empty block available to store the next pointer.");
    }
  }

  // No need to explicitly write the block back if `block` is a reference to `this.content[5]`

}


  /**
   * Generates and returns the metadata for an FCB as a char array. This metadata includes the FCB's name,
   * timestamp, size, and pointers to its data and index blocks.
   *
   * @param FCBName           Name of the FCB.
   * @param time              Timestamp for the FCB, typically creation or modification time.
   * @param size              Size of the FCB, often reflecting the size of the data it controls.
   * @param dataBlockStart    Pointer to the start of the data block for this FCB.
   * @param indexStartPointer Pointer to the start of the index block for this FCB.
   * @return A char array containing the formatted FCB metadata.
   */
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
//    System.out.println("sizeStr " + sizeStr);

    // Prepare the final metadata string
    String metadataStr = FCBName + formattedTime + sizeStr + dataBlockStart + indexStartPointer;
    // Convert the metadata string to a char array and return
    return metadataStr.toCharArray();
  }

  /**
   * Updates the superblock information, including database name, number of FCB and PFS files, and block size.
   * This method is primarily used to maintain metadata consistency across the PFS files.
   */
  public void updateSuperBlock() {
    if (this.sequenceNumber != 0) {
      System.out.println("only update SuperBlock info in .db0");
    }
    int superBlockNum = 4; // super block is in 5th block
    // Update SuperBlock will be in the block 3
    updateBitMap(superBlockNum, true);

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
      this.content[superBlockNum][i] = dbName.charAt(i);
    }

    // 30 fillin fcb size
    this.content[superBlockNum][30] = String.valueOf(numOfFCBFiles).toCharArray()[0];

    // 31-35 is # of PFC file number
    String numOfPFSFilesString = String.valueOf(numOfPFSFiles);
    char[] numOfPFSFilesChars = numOfPFSFilesString.toCharArray();
    int it = 31;
    int startingI = 31;

    for (; it <= 35; it++) {
      if (it < startingI + numOfPFSFilesChars.length) {
        this.content[superBlockNum][it] = numOfPFSFilesChars[it - startingI];
      } else {
        this.content[superBlockNum][it] = ' ';
      }
    }

    // 36-38 is # of block size
    String blockSizeString = String.valueOf(blockSize);
    char[] blockSizeChars = blockSizeString.toCharArray();
    startingI = 36;

    for (; it <= 38; it++) {
      if (it < startingI + blockSizeChars.length) {
        this.content[superBlockNum][it] = blockSizeChars[it - startingI];
      } else {
        this.content[superBlockNum][it] = ' ';
      }
    }
  }

  /**
   * Updates the bitmap to reflect the occupancy status of a specific block. Marks the block as either full or empty,
   * and adjusts the count of blocks left accordingly.
   *
   * @param blockNum     The block number to update. From 0 to 3999
   * @param isBecomeFull A boolean indicating whether the block is becoming full (true) or empty (false).
   */
  public void updateBitMap(int blockNum, boolean isBecomeFull) {
    int row = blockNum / (256 * 4);
    int col = (blockNum / 4) % 256;
    int bitPosition = blockNum % 4;

//    System.out.println("hexIndex"+hexIndex+" bitPosition " + bitPosition);

    // Convert the hex character to binary
    char hexChar = this.content[row][col];
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
      this.blockLeft -= 1;
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
    this.content[row][col] = Integer.toHexString(newValue).toUpperCase().charAt(0);
  }

  public void writeContent(int blockNum, char[] newBlockContent) {
    if(blockNum < 0 || blockNum >= 4000) {
      System.out.println("Invalid block number, please input from 0 to 3999");
      return;
    }

    if(newBlockContent.length != 256) {
      System.out.println("Invalid block content, please input char length 256");
      return;
    }

    this.content[blockNum] = newBlockContent;
  }


  // TODO: load Existings PFS, return the char[][]

  // char[][] loadExistingPFS()


  /**
   * Writes the current state of the `content` 2D char array to the associated .db file. This method is used to persist
   * changes made to the PFS structure, including updates to metadata, data blocks, and the bitmap.
   *
   * @throws IOException If an error occurs during file writing.
   */
  public void writeCharArrayToFile() throws IOException {
    String fileName = this.db.getName() + ".db" + this.sequenceNumber;
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
      for (char[] row : this.content) {
        writer.write(row);
        // Todo: remove this when demo.
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



  /**
   * Load the content of the PFS file into the 2D char array, considering the special structure of .db0.
   */
  public char[][] loadExistingPFS() {
    try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
      if (sequenceNumber == 0) {
        // Special handling for .db0
        return loadDb0(reader);
      } else {
        // General handling for .db1, .db2, etc.
        return loadDbN(reader);
      }
    } catch (IOException e) {
      System.err.println("An error occurred while loading PFS file: " + e.getMessage());
      throw new RuntimeException("Failed to load PFS file.");
    }
  }
  /*
      * Loads the content of the PFS file into the 2D char array, considering the special structure of .db0.
      * This method is used to handle the specific structure of the .db0 file, including the bitmap, superblock, and FCB.
      * @param reader The BufferedReader object used to read the file.
      * @return The 2D char array containing the loaded content.
   */
  private char[][] loadDb0(BufferedReader reader) throws IOException {
    // Specific logic to handle .db0 file
    // This could involve processing the bitmap,  superblock and FCB specifically
    // Then loading the rest of the blocks as usual


    readBitmap(reader);
    readSuperBlock(reader);
    readFCBs(reader);
    // Continue reading the rest of the blocks
    readBlocks(reader);
    return content;
  }
  /*
    loads the content of the PFS file into the 2D char array, considering the general structure of .db1, .db2, etc.
    @param reader The BufferedReader object used to read the file.
    @return The 2D char array containing the loaded content.
   */
  private char[][] loadDbN(BufferedReader reader) throws IOException {
    // General logic to handle files other than .db0
    // this could just involve loading the bitmap and then the rest of the blocks

    readBitmap(reader);
    // Continue reading the rest of the blocks
    readBlocks(reader);
    return content;
  }

  /*
    * Reads the bitmap section of the PFS file.
    * the bitmap is directly stored as hexadecimal characters in this.content array.
   */
  private void readBitmap(BufferedReader reader) throws IOException {
    for (int i = 0; i < 4; i++) {
      String line = reader.readLine();
      if (line == null) {
        throw new IOException("Unexpected end of file while reading the bitmap.");
      }
      for (int j = 0; j < line.length() && j < 256; j++) { // Ensure we don't exceed the line length or 256 characters
        char hexChar = line.charAt(j);
        // Assuming the bitmap line directly reflects the hex representation of block status
        this.content[i][j] = hexChar;


        // directly stores the read hex values into `this.content`
      }
    }
  }
  /*
    * Reads the superblock section of the PFS file.
    * The superblock is assumed to be a single line of text.
   */
  private void readSuperBlock(BufferedReader reader) throws IOException {
    //the superblock is at the 5th row (index 4)
    // skip the first 4 blocks
//    for (int i = 0; i < 4; i++) {
//      reader.readLine();
//    }
    String superBlockLine = reader.readLine();
    if (superBlockLine == null) {
      throw new IOException("Superblock information is missing.");
    }

    // Directly store the superblock characters into the corresponding row of 'content'

//    for (int colIndex = 0; colIndex < superBlockLine.length() && colIndex < db.getBlockSize(); colIndex++) {
      for (int colIndex = 0; colIndex < superBlockLine.length() &&colIndex < 256; colIndex++) {
      this.content[4][colIndex] = superBlockLine.charAt(colIndex); // Assuming blockSize matches or exceeds superBlockLine.length()
    }

    // Optionally fill the remainder of the block with spaces if superBlockLine is shorter than db.getBlockSize()
    for (int colIndex = superBlockLine.length(); colIndex < db.getBlockSize(); colIndex++) {
      this.content[4][colIndex] = ' ';
    }
  }

 /*
    FCBs only exists in .db0 file, and it is stored in the 6th block.
    load the FCB metadata from the file.
  */
  private void readFCBs(BufferedReader reader) throws IOException {
    // Skip directly to the 6th block
//    for (int i = 0; i < 5; i++) {
//      reader.readLine(); // Skip the initial 5 blocks.
//    }
    int blockIndex = 5; // Starting from the 6th block for FCB data.
    String fcbLine = reader.readLine();
    if (fcbLine == null) {
      throw new IOException("FCB information is missing.");
    }
    // store each character of the line in this.content array
    for (int colIndex = 0; colIndex < fcbLine.length() && colIndex < db.getBlockSize(); colIndex++) {
      this.content[blockIndex][colIndex] = fcbLine.charAt(colIndex);
    }
    // Optionally fill the remainder of the block with spaces if fcbLine is shorter than db.getBlockSize()
    for (int colIndex = fcbLine.length(); colIndex < db.getBlockSize(); colIndex++) {
      this.content[blockIndex][colIndex] = ' ';
    }


  }

  public void readBlocks(BufferedReader reader) throws IOException {
    int rowIndex = 6; // Start directly after FCB block
    while (rowIndex < 4000) {
      String line = reader.readLine();
      if (line == null) break; // End of file
      for (int colIndex = 0; colIndex < line.length() && colIndex < 256; colIndex++) {
        content[rowIndex][colIndex] = line.charAt(colIndex);
      }
      rowIndex++;
    }
  }

  public void showFCBMetadata() {
    int blockIndex = 5; // The block where FCB data is stored
    StringBuilder fcbDataBuilder = new StringBuilder();

    // Collect the FCB data from the 6th block
    for (int colIndex = 0; colIndex < db.getBlockSize(); colIndex++) {
      char c = this.content[blockIndex][colIndex];
      fcbDataBuilder.append(c);
    }

    String fcbData = fcbDataBuilder.toString();
    // Split the FCB data by newlines to handle multiple FCB entries
    String[] fcbEntries = fcbData.split("\n");

    for (String entry : fcbEntries) {
      // Skip empty entries, which could exist due to splitting by newline
      if (entry.isEmpty()) {
        continue;
      }

      // Assuming the FCB data is comma-separated within each entry
      String[] fcbMetadataParts = entry.split(" ");

      if (fcbMetadataParts.length >= 3) {
        // Extract FCB metadata for each entry
        String FCBName = fcbMetadataParts[0].trim();
        String time = fcbMetadataParts[1].trim();
        int numberOfBlocks = Integer.parseInt(fcbMetadataParts[2].trim());

        // Print FCB metadata, each entry on a new line
        System.out.printf("FCB Name: %s, Time: %s, Number of Blocks: %d\n", FCBName, time, numberOfBlocks);
      } else {
        System.err.println("Invalid or incomplete FCB metadata encountered in entry: " + entry);
      }
    }
  }
  // printout this.content
  public void showContent() {
    for (int i = 0; i < 4000; i++) {
      for(int j = 0; j < 256; j++) {

        if (this.content[i][j] != ' ') {
          // don't print out the space
          System.out.print(this.content[i][j]);
        }

      }

      }
//    System.out.println("\n" + "datapointer");
//
//    for (KeyPointer kp : keyPointerList) {
//
//      System.out.println(kp.getPointer());
//    }

  }

  // printout this.content[5]
  public void showFCBContent() {
    final int METADATA_SIZE = 57; // Size of each metadata entry
    char[] block = this.content[5]; // Assuming this is the metadata block
    StringBuilder builder = new StringBuilder();

    // Iterate over each metadata entry
    for (int i = 0; i < block.length; i += METADATA_SIZE) {
      // Check for the presence of actual data to avoid printing empty or uninitialized space
      boolean hasData = false;
      for (int j = 0; j < METADATA_SIZE && (i + j) < block.length; j++) {
        if (block[i + j] != '\0') { // Assuming '\0' marks the end or empty space
          builder.append(block[i + j]);
          hasData = true;
        }
      }

      // If actual data was found, append a delimiter after the entry, except for the last one
      if (hasData && (i + METADATA_SIZE) < block.length) {
        builder.append("\n"); // Delimiter between entries
      }
    }

    System.out.println(builder.toString());
  }


//  public List<FCB> loadFCBsFromBlock(int blockNum) {
//    List<FCB> fcbs = new ArrayList<>();
//
//    // Logic to read the specified block (e.g., the 6th block) and extract FCB data
//    // This might involve reading from a file, decoding the block's content,
//    // and creating FCB instances from that content
//    String blockContent = readFCBs(reader); // Implement this method based on your storage
//
//    // Split the block content into individual FCB metadata entries
//    // Assuming each FCB entry is separated by a newline or another delimiter
//    String[] fcbEntries = blockContent.split("\n"); // Adjust delimiter as necessary
//    for (String entry : fcbEntries) {
//      FCB fcb = FCB.fromString(entry);
//      fcbs.add(fcb);
//    }
//
//    return fcbs;
//  }


//  /**
//   * Reads a block from the PFS file based on the block number.
//   *
//   * @param blockNumber The block number to read.
//   * @return The data of the block as a char array.
//   * @throws IOException If reading the file fails.
//   */
//  public char[] readBlockByBlockNumber(int blockNumber) throws IOException {
//    RandomAccessFile file = new RandomAccessFile(fileName, "r");
//
//    // Calculate the offset in the file where the block starts
//    long offset = (long) blockNumber * 256;
//
//    // Move to the start of the block
//    file.seek(offset);
//
//    // Read the block into a byte array
//    byte[] bytes = new byte[256];
//    file.readFully(bytes);
//    file.close();
//
//    // Convert the byte array to a char array
//    char[] chars = new char[256];
//    for (int i = 0; i < 256; i++) {
//      chars[i] = (char) (bytes[i] & 0xFF); // Convert each byte to char
//    }
//
//    return chars;
//  }
  /**
   * Reads a block from the PFS file based on the block number.
   *
   * @param blockNumber The block number to read.
   * @return The data of the block as a char array.
   * @throws IOException If reading the file fails.
   */
  public String getRecord(int blockNumber, int recordNumber) {
    if (blockNumber < 0 || blockNumber >= content.length) {
      throw new IllegalArgumentException("Block number out of range.");
    }
    if (recordNumber < 0 || recordNumber >= 6) { // Assuming max 6 records per block
      throw new IllegalArgumentException("Record number out of range.");
    }

    int recordSize = 40; // fixed size for each record
    int startIndex = recordNumber * recordSize;

    char[] block = content[blockNumber];
    char[] recordChars = new char[recordSize];
    System.arraycopy(block, startIndex, recordChars, 0, recordSize);

    return new String(recordChars).trim();
  }

  //read the blocks from the PFS content based on the block pointers or indexes stored in the FCB
  public List<char[]> getBlocksByFCB(FCB fcb) {
    List<char[]> blocksData = new ArrayList<>();
    // Assuming the FCB has methods to get the starting and ending block indexes
    String startBlock = fcb.getDataStartBlock();
    // parse the startBlock to get the block number
    int startBlockNum = Integer.parseInt(startBlock);


    for (int i = startBlockNum; i <= startBlockNum+fcb.getSize()-2; i++) {
      blocksData.add(content[i]);
    }
    return blocksData;
  }
  public List<String> extractRecordsFromBlock(char[] block) {
    List<String> records = new ArrayList<>();
    int recordLength = 40; // Each record is 40 characters long
    int totalRecords = 6; // There are 6 records in a block

    for (int i = 0; i < totalRecords; i++) {
      int start = i * recordLength;
      String record = new String(block, start, recordLength).trim(); // Trim any trailing whitespace
      records.add(record);
    }

    return records;
  }
  // write the records to a CSV file
    public void writeRecordsToCSV(String fileName, List<char[]>blocksData)
            throws IOException {
      String directoryPath = "./download"; // The name of the directory to store the CSV files
      try {
        // Ensure the download directory exists
        Files.createDirectories(Paths.get(directoryPath));

        // Construct the full path for the new CSV file within the download directory
        File file = new File(directoryPath, fileName);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
          for (char[] blockData : blocksData) {
            List<String> records = extractRecordsFromBlock(blockData);
            for (String record : records) {
              writer.write(record);

            }

          }
        }
//        System.out.println("CSV file written successfully: " + file.getAbsolutePath());
        } catch (IOException e) {
          e.printStackTrace();
        }

    }

  // print out the records
  public void showRecords(List<String> records) {
    for (String record : records) {
      System.out.println(record);
    }
  }


  //print out the blocks data
    public void showBlocksData(List<char[]> blocks) {
        for (char[] block : blocks) {
          //TODO : deal with pointers
        System.out.println(new String(block).trim());
        }
    }
  // Assuming this method is also in the PFS class or a utility class
//  public void writeBlocksToCSV(String fcbName, List<char[]> blocks) throws IOException {
//    String newFileName = "reconstructed_" + fcbName; // New CSV file name
//    try (BufferedWriter writer = new BufferedWriter(new FileWriter(newFileName))) {
//      for (char[] block : blocks) {
//        String blockData = new String(block).trim(); // Convert to string and trim
//        writer.write(blockData);
//        writer.newLine(); // Assuming CSV data does not span multiple blocks
//      }
//    }
//  }
//  public void reconstructCSVFromFCB(String fcbName) {
//    FCB fcb = db.findFCBByName(fcbName);
//    if (fcb == null) {
//      System.out.println("FCB not found for name: " + fcbName);
//      return;
//    }
//
//    List<char[]> blocks = getBlocksByFCB(fcb);
//    try {
//      writeBlocksToCSV(fcbName, blocks);
//      System.out.println("CSV file reconstructed: " + fcbName);
//    } catch (IOException e) {
//      e.printStackTrace();
//    }
//  }



  // write a method to find out the block number of a given FCB
    public int findBlockNumberByFCB(FCB fcb) {
        // Assuming the FCB has a method to get the starting block index
        String startBlock = fcb.getDataStartBlock();
        return Integer.parseInt(startBlock);
    }

  public char[][] getContent() {
    return this.content;
  }
  public void removeElements(char[] block, int startIndex) {
    final int LENGTH_TO_REMOVE = 57;
    int endIndex = startIndex + LENGTH_TO_REMOVE - 1;

    // Ensure endIndex does not exceed the array bounds
    if (endIndex >= block.length) {
      System.err.println("End index is out of bounds.");
      return;
    }

    // Shift elements to the left to overwrite the elements to be "removed"
    int shiftStartIndex = endIndex + 1;
    for (int i = startIndex; i < block.length - LENGTH_TO_REMOVE; i++) {
      if (shiftStartIndex < block.length) {
        block[i] = block[shiftStartIndex++];
      } else {
        block[i] = '\0'; // Fill the remaining space with null characters or any default value
      }
    }

//    // Optional: Fill the end of the array with a default value if desired
//    for (int i = block.length - LENGTH_TO_REMOVE; i < block.length; i++) {
//      block[i] = '\0'; // Assuming you want to clear the shifted elements
//    }
  }
  //TODO: update the FCB block in the PFS file
   //write updated block to the file
    public void updateFCBBlock(int blockNum, char[] updatedBlock) {
      if (blockNum < 0 || blockNum >= 4000) {
        System.out.println("Invalid block number, please input from 0 to 3999");
        return;
      }

      if (updatedBlock.length != 256) {
        System.out.println("Invalid block content, please input char length 256");
        return;
      }

      this.content[blockNum] = updatedBlock;
    }
    // write this.content to the file
    public void writeContentToFile() {
      try {
        writeCharArrayToFile();
        System.out.println("File written successfully.");
      } catch (IOException e) {
        System.err.println("An error occurred while writing the file: " + e.getMessage());
      }
    }
  //TODO: traverse pfs based on given fcb,  update the bitmap from 1 to 0


  // iterate keypointer list, and update the bitmap
  public void freeBlocksByFCB(String fcbName) {
    List<KeyPointer> keyPointerList = db.getKeyPointerList(fcbName);
    for (KeyPointer kp : keyPointerList) {
      int blockNum = Integer.parseInt(kp.getPointer());
//      System.out.println("Freeing block: " + blockNum);
      updateBitMap(blockNum, false);

    }


  }




}






