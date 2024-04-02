import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a database, handling the creation and management of PFS files
 * and the upload and storage of data into these files.
 */
public class DB {
  // Variables
  private String name; // Name of the database.
  private int blockSize; // Size of one blocks within the PFS files. Unit is byte.
  private int numOfFCBFiles; // Number of FCB files, default value 0
  private int numOfPFSFiles; // Number of PFC files, default value 1
  private List<PFS> pfsList; // List of PFS instances associated with this database.
  private List<FCB> fcbList; // List of FCB instances associated with this database.
  private Btree btree;

  /**
   * Constructor for the DB class. Initializes a new database or loads an existing one.
   *
   * @param name The name of the database.
   * @param blockSize The size of blocks within the PFS files.
   * @param isLoad Indicates whether to load an existing database (true) or create a new one (false).
   */
  public DB(String name, int blockSize, boolean isLoad) {
    System.out.println("creating DB " + name);
    this.name = name;
    this.blockSize = blockSize;
    this.pfsList = new ArrayList<>();
    this.fcbList = new ArrayList<>();
    this.btree = new Btree();
    if (!isLoad) {
      init();
    } else {
      // todo, load the previous pfs files
      for (PFS file : pfsList) {
        file.loadExistingPFS();
        // todo: load the fcb lists
//        for (FCB fcb : fcbList) {
//          fcb.loadExistingFCB();
//
//        }

      }
    }
  }

  /**
   * Initializes the database by creating the initial PFS file and setting up the superblock.
   */
  public void init () {
    this.numOfFCBFiles = 0; // Default value
    this.numOfPFSFiles = 0; // Default value

    // create a new PFS file db0
    // -> write all the BitMap(with first 3 blocks full) & superblock info, leave 1 block for FCB
    //    block
    PFS pfs = new PFS(this, 0);
    pfsList.add(pfs);
  }


  /**
   * Converts the content of a CSV file into a 2D char array, with each row representing a record.
   *
   * @param filePath The path to the CSV file.
   * @return A 2D char array containing the records from the CSV file.
   * @throws IOException If an error occurs while reading the file.
   */
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
    System.out.println("records.length " + records.length);

    for (int i = 0; i < lines.size(); i++) {
      char[] lineChars = lines.get(i).toCharArray();
      System.arraycopy(lineChars, 0, records[i], 0, lineChars.length);
    }

    return records;
  }

  // Method to combine 6 records into 1 blocks
  /**
   * Combines multiple records into blocks, adhering to the specified block size.
   *  Combine 6 records and one pointer into 1 blocks
   *  List<{dblock0 dblock1 dblock2 dblock3 dblock4 dblock5 -> block pointer}>
   *
   * @param data A 2D char array of records to be combined into blocks.
   * @return A list of blocks, each represented as a char array.
   */
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

  /**
   * Uploads a CSV file as an FCB file, converting its contents into data blocks and storing them within PFS files.
   *
   * @param fileName The name of the CSV file to upload.
   */
  public void uploadFCBFile(String fileName) {
    // load file, calculate the record size
    // transfer the file into a datablock char[]

    System.out.println("Uploading FCB File: " + fileName);
    String filePath = "./csvs/" + fileName;
    try {
      char[][] data = convertCSVToCharArray(filePath);
      System.out.println("data.length " + data.length);
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
      // todo: block size should add up index block
      storeBlocksInPFS(blocks, fileName, blocks.size());

    } catch (IOException e) {
      System.err.println("An error occurred while reading the file: " + e.getMessage());
    }
  }

  /**
   * Stores data blocks and index block into PFS files,  managing block allocation and updating
   * superblock and FCB metadata as necessary.
   *
   * @param blocks A list of data blocks to store.
   * @param fileName The name of the file associated with these data blocks.
   * @param blocksSize The total number of blocks to store.
   * @return A string representing the starting pointer of the stored data blocks.
   */
  public void storeBlocksInPFS(List<char[]> blocks, String fileName, int blocksSize) {
    List<KeyPointer> keyPointerList = new ArrayList<>();

    String dataStartPtr =  storeDataInPFSs(blocks, blocksSize, keyPointerList);

    // add index block
    int indexBlockSize = 0;
    // Generate a b-tree which inserted all the keyPointers
     Btree btree =  generateBTree(keyPointerList);
//     btree.DisplayEntileBTree();

    // Find how many space we need and generate a List<Empty Block Lists String>
    List<String> emptyBlocks = findEmptyBlocks(btree.getCntNodes());


//    System.out.println("....................");
//    System.out.println("emptyBlocks:");
//    for(String s : emptyBlocks) {
//      System.out.println(s);
//    }
//    System.out.println("....................");

    // Put the index block into corresponding place
    // Replace all the pointer to corresponding String
    String indexRootPtr = storeIndexToEmptyBlocks(emptyBlocks, btree);

    // todo: implement the fcb metadata class
    // FCB name: 20 char, time: 14 char (sample:  "15/SEP/23:25PM")
    // number of blocks 10 int.  data start block(7 char): default: 9999999,
    // index start block(7 char): default: 9999999,
    pfsList.get(0).updateFCBMetadeta(fileName, LocalDateTime.now(),
            blocksSize + btree.getCntNodes(), dataStartPtr, indexRootPtr);
    FCB newFCB = new FCB(fileName, LocalDateTime.now(), blocksSize + btree.getCntNodes(), dataStartPtr, indexRootPtr);
    fcbList.add(newFCB);

    System.out.println("empty space" + pfsList.get(0).getBlockLeft());
    System.out.println("calculate empty space" + pfsList.get(0).calculateBlocksLeft());

    this.numOfFCBFiles++;
    pfsList.get(0).updateSuperBlock();
  }

  // stores b-tree nodes into empty blocks and write the corresponding files
  public String storeIndexToEmptyBlocks(List<String> emptyBlocks, Btree btree) {
    // handling root as return
    String rootBlockPointer = emptyBlocks.get(btree.getRoot());
    Node[] nodes = btree.getNodes();

    for(int i=0; i<emptyBlocks.size(); i++) {
//      System.out.println("block: " + i);
      // generate a index block string
      int j=0;
      String temp = "";
      for(; j<nodes[i].size; j++){
        int childPointer = nodes[i].children[j];

        if(childPointer == -1 ) {
          temp += "9999999";
//          System.out.print("9999999");
        } else {
          temp += emptyBlocks.get(childPointer);
//          System.out.print(emptyBlocks.get(childPointer));
        }
//        System.out.print(" (" + nodes[i].values[j].getKeyPointerStr() + ") ");
        temp += nodes[i].values[j].getKeyPointerStr();
      }

      int laseChildPointer = nodes[i].children[j];
      if(laseChildPointer == -1 ) {
        temp += "9999999";
//        System.out.print(" 9999999 ");
      } else {
        temp += emptyBlocks.get(laseChildPointer);
//        System.out.print(" " + emptyBlocks.get(laseChildPointer));
      }

//      System.out.println();
      // write this block in content[][]
      BlockPointer bp = new BlockPointer(emptyBlocks.get(i));
      char[] tempCharArray = new char[this.blockSize];
      for (int k = 0; k < temp.toCharArray().length; k++) {
        tempCharArray[k] = temp.charAt(k);
      }
//      System.out.println("temp " + String.valueOf(tempCharArray));
      this.pfsList.get(bp.getPfsNumber()).writeContent(bp.getBlockNumber(), tempCharArray);
    }

    // write into .db file
    for(int i=0; i<pfsList.size(); i++) {
      try {
        pfsList.get(i).writeCharArrayToFile();
        System.out.println("File written successfully.");
      } catch (IOException e) {
        System.err.println("An error occurred while writing the file: " + e.getMessage());
      }
    }
    // rootBlockPointer is the root block pointer of
    return rootBlockPointer;
  }
  // get the root block number



  // get the btree size and find empty blocks in database
  public List<String> findEmptyBlocks(int btreeSize) {
    System.out.println("findEmptyBlocks btreeSize " + btreeSize);
    List<String> emptyBlocks = new ArrayList<>(); // list of BlockPointer String

    int blockleft = btreeSize; // counter for data block needs to insert

    // try to put data in existing PFS file
    for(int i = 0; i < this.pfsList.size(); i++) {
      if (blockleft > 0 && pfsList.get(i).getBlockLeft() > 0) {
        // calculate how many blocks should I put in current file i
        int assignedBlock = Math.min(blockleft, pfsList.get(i).getBlockLeft());

        // find empty blocks and update emptyBlocks List with empty BlockPointer String
        pfsList.get(i).findEmptyBlocks(assignedBlock, emptyBlocks);

//        for(String blockStr: emptyBlocks){
//          System.out.println(blockStr);
//        }

        System.out.println("Inserted index block " + assignedBlock +" to .db" + pfsList.get(i).getSequenceNumber());
        // blockeleft mi
        blockleft -= assignedBlock;

        try {
          pfsList.get(i).writeCharArrayToFile();
          System.out.println("File written successfully.");
        } catch (IOException e) {
          System.err.println("An error occurred while writing the file: " + e.getMessage());
        }
      }
    }

    // If the current file are full, create new file and try to put blocks in it
    while(blockleft > 0) {
      // create a new PFS file
      PFS pfs = new PFS(this, this.numOfPFSFiles);
      pfsList.add(pfs);
      pfsList.get(0).updateSuperBlockNumOfPFSFiles(this.numOfPFSFiles);

      // calculate how many blocks should I put in current file i
      int assignedBlock = Math.min(blockleft, pfs.getBlockLeft());

      // find empty blocks and update emptyBlocks List with empty BlockPointer String
      pfs.findEmptyBlocks(assignedBlock, emptyBlocks);

      System.out.println(emptyBlocks.get(emptyBlocks.size()-1));

      System.out.println("Inserted " + assignedBlock +" to .db" + pfs.getSequenceNumber());

      // write the current char array to .dbfile
      try {
        pfs.writeCharArrayToFile();
        System.out.println("File written successfully.");
      } catch (IOException e) {
        System.err.println("An error occurred while writing the file: " + e.getMessage());
      }

      // update counters
      blockleft -= assignedBlock;
    }


    return emptyBlocks;
  }


  // inserted all the keys and genarate a B-tree
  public Btree generateBTree(List<KeyPointer> keyPointerList){
//    Btree btree = new Btree();
    this.btree = new Btree();

    for(KeyPointer currKeyPtr:keyPointerList) {
//      System.out.println(currKeyPtr.getKey() + " " + currKeyPtr.getKeyPointerStr());
//      btree.Insert(currKeyPtr);
//      btree.DisplayEntileBTree();
      this.btree.Insert(currKeyPtr);
    }
    return btree;
  }



  public int getRootBlockNumber(Btree btree) {
    return btree.getRoot();
  }


  /**
   * Stores data blocks across one or more PFS files, managing the distribution of blocks based on available space.
   * This method allocates blocks to existing PFS files and creates new PFS files if needed to accommodate all blocks.
   * It returns a pointer to the start of the data in the PFS structure, facilitating access to the stored data.
   *
   * @param blocks A list of data blocks (char arrays) that need to be stored in the PFS files.
   * @param blocksSize The total number of blocks to be stored, guiding how many PFS files might be needed.
   * @return A String representing the starting pointer of the data within the PFS structure, which can be used
   *         to locate the data for future retrieval or modification.
   */
  public String storeDataInPFSs(List<char[]> blocks, int blocksSize, List<KeyPointer> keyPointerList) {
    // start and end pointers
    // List<{startPointerString, endPointerString}>
    List<List<String>> dataStartNEndPtrs = new ArrayList<>();

    // List of keyValues
    // List<{key:dataBlockPointer}>
    int blockleft = blocksSize; // counter for data block needs to insert
    int blockCounter = 0; // counter for data block needs to insert


    // try to put data in existing PFS file
    for(int i = 0; i < this.pfsList.size(); i++) {
      if (blockleft > 0 && pfsList.get(i).getBlockLeft() > 0) {
        // calculate how many blocks should I put in current file i
        int assignedBlock = Math.min(blockleft, pfsList.get(i).getBlockLeft());

        // add data to pfs file i, and return the start and end BlockPointer in string
        List<String> currStartNEndPtr = pfsList.get(i)
                .addData(new ArrayList<>(blocks.subList(blockCounter, blockCounter + assignedBlock)),
                        keyPointerList);

        System.out.println("Inserted data node " + assignedBlock +" to .db" + pfsList.get(i).getSequenceNumber());

        // if already inserted in another PFS file,
        if (dataStartNEndPtrs.size() > 0) {
          // update the last pfs end block pointer to the next pfs begin pointer
          BlockPointer lastBP =
                  new BlockPointer(dataStartNEndPtrs.get(dataStartNEndPtrs.size()-1).get(1));
          pfsList.get(lastBP.getPfsNumber())
                  .updateBlockPointer(lastBP.getBlockNumber(), currStartNEndPtr.get(0));
        }
        dataStartNEndPtrs.add(currStartNEndPtr);
        blockleft -= assignedBlock;
        blockCounter += assignedBlock;
      }
      if (blockleft == 0) break;
    }

    // If the current file are full, create new file and try to put blocks in it
    while(blockleft > 0) {
      // create a new PFS file
      PFS pfs = new PFS(this, this.numOfPFSFiles);
      pfsList.add(pfs);
      pfsList.get(0).updateSuperBlockNumOfPFSFiles(this.numOfPFSFiles);

      // calculate how many blocks should I put in current file i
      int assignedBlock = Math.min(blockleft, pfs.getBlockLeft());

      // add blocks to current pfs file
      List<String> currStartNEndPtr = pfs.addData(new ArrayList<>(blocks.subList(blockCounter,
              blockCounter + assignedBlock)), keyPointerList);

      System.out.println("Inserted " + assignedBlock +" to .db" + pfs.getSequenceNumber());

      // if already inserted in another PFS file,
      // update the last pfs end block pointer to the next pfs begin pointer
      if (dataStartNEndPtrs.size() > 0) {
        BlockPointer lastBP = new BlockPointer(dataStartNEndPtrs.get(dataStartNEndPtrs.size()-1).get(1));
//        System.out.println("updating db" + lastBP.getPfsNumber() + " at block " + lastBP.getBlockNumber()
//                + " to " + currStartNEndPtr.get(0));
        pfsList.get(lastBP.getPfsNumber())
                .updateBlockPointer(lastBP.getBlockNumber(), currStartNEndPtr.get(0));

        // write the current char array to .dbfile
        try {
          pfsList.get(lastBP.getPfsNumber()).writeCharArrayToFile();
          System.out.println("File written successfully.");
        } catch (IOException e) {
          System.err.println("An error occurred while writing the file: " + e.getMessage());
        }

      }
      dataStartNEndPtrs.add(currStartNEndPtr);

      // update counters
      blockleft -= assignedBlock;
      blockCounter += assignedBlock;
    }

    return dataStartNEndPtrs.get(0).get(0); // return the begin pointer
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

  // show PFS'S fcb metadata
  public void showPFSFCBMetadata() {
//        pfsList.get(0).showFCBContent();
    for(FCB fcb: fcbList) {

      fcb.showContent();
    }
  }
  //show pfs this.content
  public void showPFSContent() {
    for(PFS pfs: pfsList) {
      pfs.showContent();
    }
  }
  public Btree getBtree() {
    return btree;
  }

  /**
   * Searches for a key in the database.
   *
   * @param key The key to search for.
   * @return Datablock pointer associated with the key, or null if the key is not found. 00000061
   */
    public String search(int key) {
      // Use the B-tree's Lookup method to determine if the key exists
      SearchResult result = this.btree.Lookup(key);
        if (result.found) {
            // If the key exists, return the pointer associated with the key
          //  get the pointer from the btree, and print out the key's record
//            System.out.println("Key found.");
//            System.out.println(result.keyPointer.getKeyPointerStr());


            //return the data block pointer
            return result.keyPointer.getPointer();


        } else {
            // If the key does not exist, print a message and return null
            System.out.println("Key not found.");
            return null;
        }
    }

  public String getRecordbyDataBlockPointer(int pfsNumber, int blockNumber, int recordNumber) {
    if (pfsNumber < 0 || pfsNumber >= pfsList.size()) {
      throw new IllegalArgumentException("PFS number out of range.");
    }

    PFS pfs = pfsList.get(pfsNumber);


    // Use PFS.getRecord to fetch the specific record
    return pfs.getRecord(blockNumber, recordNumber);
  }






}