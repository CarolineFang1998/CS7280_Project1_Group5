import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

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

  private Map<String, Btree> filenameToBtreeMap;
  private Map<String,List<KeyPointer>> keyPointerMap;

  /**
   * Constructor for the DB class. Initializes a new database or loads an existing one.
   *
   * @param name The name of the database.
   * @param blockSize The size of blocks within the PFS files.
   * @param isLoad Indicates whether to load an existing database (true) or create a new one (false).
   */
  public DB(String name, int blockSize, boolean isLoad) {

    this.name = name;
    this.blockSize = blockSize;
    this.pfsList = new ArrayList<>();
    this.fcbList = new ArrayList<>();

    this.filenameToBtreeMap = new HashMap<>();
    this.keyPointerMap = new HashMap<>();
    if (!isLoad) {
      System.out.println("creating DB " + name + "...");
      init();
      loadExistingPFSs();
    } else {
      System.out.println("loading DB " + name + "...");
      loadExistingPFSs();
//      System.out.println("loading PFS size" + numOfPFSFiles);
      // need to create a pfs first
      this.numOfFCBFiles = this.pfsList.get(0).loadExistingFCB(this.fcbList);
//      System.out.println("loading fcb size" + numOfFCBFiles);
    }
  }

  /**
   * Find the corresponding record and print it out
   * @param root the root of the b-tree
   * @param key the key we are looking for
   */
  public void find(BlockPointer root, int key) {
    String dataBlockPtrStr = findDataBlockPtr(root, key, 0);
    if(dataBlockPtrStr != "") {
      findDataBlockContent(dataBlockPtrStr);
    }
  }

  /**
   * Find the corresponding record and print it out
   * @param dataBlockPtrStr where record located
   */
  public void findDataBlockContent(String dataBlockPtrStr) {
    DataBlockPointer dbp = new DataBlockPointer(dataBlockPtrStr);
    // get content[] from that block
    char[] content = this.pfsList.get(dbp.getPfsNumber()).getContent()[dbp.getBlockNumber()];
    int recordSize = 40;
    String data = new String(content, dbp.getRecordNumber() * recordSize, recordSize);
    System.out.println("Found record:");
    System.out.println(data);
  }

  /**
   * Find the right dataBlockPointer which the key is located.
   * @param root root of b tree
   * @param key the key we are looking for
   * @param counter count how many blocks we are looking at
   * @return the data block pointer String
   */
  public String findDataBlockPtr(BlockPointer root, int key, int counter) {
    // find the node
    counter++;

    char[] blockContent = pfsList.get(root.getPfsNumber()).getContent()[root.getBlockNumber()];
    // generate a block pointer array
    List<KeyPointer> keypointerList = generateBTreeKeyPointerArray(blockContent);
    // generate a keyPointer array
    List<BlockPointer> blockPointerList =  generateBTreeChildBlockPointerArray(blockContent);

    int i = 0;

    // Iterate through keys in the node to find the smallest index i such that value <= node.values[i]
    while (i < keypointerList.size() && key > keypointerList.get(i).getKey()) {
      i++;
    }

    // If the value matches the key at index i in the node
    if (i < keypointerList.size() && key == keypointerList.get(i).getKey()) {
      // plus 1 metadata block and 1 data block
      System.out.println("Found key after search " + (counter + 2) + " blocks.");
      return keypointerList.get(i).getPointer(); // The value is found
    }

    // If the node is a leaf, then the search is unsuccessful
    if (blockPointerList.size() == 0) {
      System.out.println("Can't find " + key);
      return "";
    } else {
      // Recur to search the appropriate subtree
      return findDataBlockPtr(blockPointerList.get(i), key, counter);
    }
  }


  /**
   * Generate a b-tree KeyPointer List which is contains the integer key and a DataBlockPointer
   * which point to the data block
   * @param blockContent blockContent the current block contents
   * @return a list of Key pointer which contains the key information and record location pointer
   */
  public List<KeyPointer> generateBTreeKeyPointerArray(char[] blockContent){
    List<KeyPointer> result = new ArrayList<>();
    int blockPointerSize = 7;
    int keyPointerSize = 15;
    for(int i = 0; i < 11; i++) {
      String temp = new String(blockContent,
              (i * keyPointerSize) + ((i + 1) * blockPointerSize ),
              keyPointerSize);

      // if all white space in the following
      if(! temp.trim().isEmpty()) {
          KeyPointer kp = new KeyPointer(temp);
//        System.out.println("result.getKey()" +kp.getKey());
//        System.out.println("result.getPointer()" +kp.getPointer());
          result.add(kp);
      } else {
        break;
      }
    }
    return result;
  }

  /**
   * Generate B-tree child block which contains BlockPointers where the children block located
   * @param blockContent the current block contents
   * @return a list of block pointer which point to the children blocks
   */
  public List<BlockPointer> generateBTreeChildBlockPointerArray(char[] blockContent){
    List<BlockPointer> result = new ArrayList<>();
    int blockPointerSize = 7;
    int keyPointerSize = 15;
    for(int i = 0; i < 11; i++) {
      String temp = new String(blockContent,
              (i * keyPointerSize) + (i * blockPointerSize ),
              blockPointerSize);

      // if all white space in the following
      if(! temp.trim().isEmpty() && ! temp.equals("9999999")) {
        BlockPointer bp = new BlockPointer(temp);
        result.add(bp);
      } else {
        break;
      }
    }
    return result;
  }

  /**
   * Loaded existing PFS
   */
  public void loadExistingPFSs() {
    // find fcbs in the current dir and count how many of them
    this.numOfPFSFiles = countPFSFiles();
    for(int i=0; i < this.numOfPFSFiles; i++) {
      PFS pfs = new PFS(this, i);
      this.pfsList.add(pfs);
    }
  }

  /**
   * show FCB
   */
  public void showFCBs() {
    for(int i=0; i<this.fcbList.size(); i++) {
      if(fcbList.get(i).getName() != "") {
        fcbList.get(i).showContent();
      }
    }
    System.out.println();
  }

  /**
   * Counts files in the current directory that start with the value of this.name and end with ".db" followed by a number.
   *
   * @return The count of matching files.
   */
  public int countPFSFiles() {
    Path currentDirectory = Paths.get("");
    // Dynamically insert this.name into the regex pattern
    final Pattern pattern = Pattern.compile("^" + Pattern.quote(this.name) + ".*\\.db\\d+$");
    final int[] count = {0}; // Use an array to allow modification inside the lambda

    try (Stream<Path> files = Files.list(currentDirectory)) {
      count[0] = (int) files
              .map(Path::getFileName)
              .map(Path::toString)
              .filter(fileName -> pattern.matcher(fileName).matches())
              .count();
    } catch (IOException e) {
      System.err.println("An error occurred while listing files: " + e.getMessage());
    }

    return count[0];
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

    System.out.println("Uploading FCB File: " + fileName + "...");
    String filePath = "./csvs/" + fileName;
    try {
      char[][] data = convertCSVToCharArray(filePath);
      List<char[]> blocks = recordsToBlock(data);

      storeBlocksInPFS(blocks, fileName, blocks.size());

    } catch (IOException e) {
      System.err.println("An error occurred while reading the file: " + e.getMessage());
    }
  }

  /**loading
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

    // Generate a b-tree which inserted all the keyPointers
     Btree btree =  generateBTree(keyPointerList, fileName);
//     btree.DisplayEntileBTree();

    // Find how many space we need and generate a List<Empty Block Lists String>
    List<String> emptyBlocks = findEmptyBlocks(btree.getCntNodes());

    // Put the index block into corresponding place
    // Replace all the pointer to corresponding String
    String indexRootPtr = storeIndexToEmptyBlocks(emptyBlocks, btree);

    LocalDateTime time = LocalDateTime.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MMM/yy:HHa");
    String formattedTime = time.format(formatter);
    FCB newFCB = new FCB(fileName, formattedTime, blocksSize + btree.getCntNodes(), dataStartPtr, indexRootPtr);
    fcbList.add(newFCB);

    pfsList.get(0).updateFCBMetadata(fcbList);

    this.numOfFCBFiles++;
    pfsList.get(0).updateSuperBlock();

    // write the current char array to .dbfile
    try {
      pfsList.get(0).writeCharArrayToFile();
    } catch (IOException e) {
      System.err.println("An error occurred while writing the file: " + e.getMessage());
    }
  }

  // stores b-tree nodes into empty blocks and write the corresponding files
  public String storeIndexToEmptyBlocks(List<String> emptyBlocks, Btree btree) {
    // handling root as return
    String rootBlockPointer = emptyBlocks.get(btree.getRoot());
    Node[] nodes = btree.getNodes();

    for(int i=0; i<emptyBlocks.size(); i++) {
      // generate a index block string
      int j=0;
      String temp = "";
      for(; j<nodes[i].size; j++){
        int childPointer = nodes[i].children[j];

        if(childPointer == -1 ) {
          temp += "9999999";
        } else {
          temp += emptyBlocks.get(childPointer);
        }
        temp += nodes[i].values[j].getKeyPointerStr();
      }

      int laseChildPointer = nodes[i].children[j];
      if(laseChildPointer == -1 ) {
        temp += "9999999";
      } else {
        temp += emptyBlocks.get(laseChildPointer);
      }

      // write this block in content[][]
      BlockPointer bp = new BlockPointer(emptyBlocks.get(i));
      char[] tempCharArray = new char[this.blockSize];
      for (int k = 0; k < temp.toCharArray().length; k++) {
        tempCharArray[k] = temp.charAt(k);
      }
      this.pfsList.get(bp.getPfsNumber()).writeContent(bp.getBlockNumber(), tempCharArray);
    }

    // write into .db file
    for(int i=0; i<pfsList.size(); i++) {
      try {
        pfsList.get(i).writeCharArrayToFile();
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
    List<String> emptyBlocks = new ArrayList<>(); // list of BlockPointer String

    int blockleft = btreeSize; // counter for data block needs to insert

    // try to put data in existing PFS file
    for(int i = 0; i < this.pfsList.size(); i++) {
      if (blockleft > 0 && pfsList.get(i).getBlockLeft() > 0) {
        // calculate how many blocks should I put in current file i
        int assignedBlock = Math.min(blockleft, pfsList.get(i).getBlockLeft());

        // find empty blocks and update emptyBlocks List with empty BlockPointer String
        pfsList.get(i).findEmptyBlocks(assignedBlock, emptyBlocks);

        System.out.println("Inserted index block " + assignedBlock +" to .db" + pfsList.get(i).getSequenceNumber());
        // blockeleft mi
        blockleft -= assignedBlock;

        try {
          pfsList.get(i).writeCharArrayToFile();
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

      System.out.println("Inserted " + assignedBlock +" to .db" + pfs.getSequenceNumber());

      // write the current char array to .dbfile
      try {
        pfs.writeCharArrayToFile();
      } catch (IOException e) {
        System.err.println("An error occurred while writing the file: " + e.getMessage());
      }

      // update counters
      blockleft -= assignedBlock;
    }


    return emptyBlocks;
  }


  // inserted all the keys and genarate a B-tree
  public Btree generateBTree(List<KeyPointer> keyPointerList, String fcbFilename){
    Btree btree = new Btree();
//    this.btree = new Btree();

    for(KeyPointer currKeyPtr:keyPointerList) {
      btree.Insert(currKeyPtr);
//      btree.DisplayEntileBTree();
    }
    // Add the B-tree to the mapping with its corresponding FCB filename
    this.filenameToBtreeMap.put(fcbFilename, btree);
    this.keyPointerMap.put(fcbFilename, keyPointerList);

    return btree;
  }
  public List<KeyPointer> getKeyPointerList(String fcbFilename) {
    return this.keyPointerMap.get(fcbFilename);
  }

  //get btree using fcb filename
  public Btree getBtree(String fcbFilename) {
    Btree btree = this.filenameToBtreeMap.get(fcbFilename);
//    btree.DisplayEntileBTree();
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
        pfsList.get(lastBP.getPfsNumber())
                .updateBlockPointer(lastBP.getBlockNumber(), currStartNEndPtr.get(0));

        // write the current char array to .dbfile
        try {
          pfsList.get(lastBP.getPfsNumber()).writeCharArrayToFile();
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
  public int deleteOneFCBFile() {
    return --numOfFCBFiles;
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

  /**
   * Searches for a key in a B-tree and returns the associated data block pointer if the key is found.
   *
   * @param key The key to search for.
   * @return Datablock pointer associated with the key, or null if the key is not found. 00000061
   */
    public String search(int key, String fcbFilename) {
      // Use the B-tree's Lookup method to determine if the key exists
      Btree btree = getBtree(fcbFilename);
      SearchResult result = btree.Lookup(key);
        if (result.found) {
            // If the key exists, return the pointer associated with the key
          //  get the pointer from the btree, and print out the key's record
          // total block accessed = block accessed in btree + one headerblock
            int totalBlocksAccessed = result.getBlockAccesses()+1;
            System.out.println("# of Blocks = " + totalBlocksAccessed);
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

 /*
  @param name The name of the FCB to search for.
    @return The FCB with the specified name, or null if no FCB with that name exists.
  */
  public FCB findFCBByName(String name) {
    // Assuming fcbList is a List<FCB> storing all FCBs
    for (FCB fcb : fcbList) {
      if (fcb.getName().equals(name)) {
        return fcb;
      }
    }
    return null; // FCB not found
  }
  // DB get pfsList's first element
    public PFS getFirstPFS() {
        return pfsList.get(0);
    }
    // DB get fcbList
    public List<FCB> getFcbList() {
        return fcbList;
    }



    // rm related methods
    public void removeFCB(String name) {
        FCB fcb = findFCBByName(name);
        if (fcb != null) {
            fcbList.remove(fcb);
            System.out.println("FCB " + name + " removed.");
        } else {
            System.out.println("FCB " + name + " not found.");
        }
    }

  public void downloadFCBFile(FCB fcb) {
    // Ensure the ./download directory exists or create it
    File downloadDir = new File("./download");
    if (!downloadDir.exists()) {
      downloadDir.mkdirs();
    }
    // Specify the path to the CSV file
    String outputPath = "./download/" + fcb.getName();
    int recordSize = 40;

    String currBPStr = fcb.getDataStartBlock();

    char[] content ;
    String nextPointer ;

    try (FileWriter writer = new FileWriter(outputPath)) {
      while (!currBPStr.equals("9999999")) {
        BlockPointer currBP = new BlockPointer(currBPStr);
        content = this.pfsList.get(currBP.getPfsNumber()).getContent()[currBP.getBlockNumber()];
        nextPointer = new String(content, blockSize - 7, 7);
        // extract each 40 block and write to a .csv file to ./download


        // Process each record
        for (int i = 0; i < recordSize*6; i += 40) {
          // Extract each record as a String, trimming trailing spaces
          String record = new String(content, i, 40);
          if(record.trim() == "") break;
          // Write the record to the file, appending a new line
          writer.write(record + "\n");
        }

        // update pointer
        currBPStr = nextPointer;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  public void deleteFCBDataBlock(FCB fcb) {
    String currBPStr = fcb.getDataStartBlock();

    char[] content ;
    String nextPointer ;

    while (!currBPStr.equals("9999999")) {
      BlockPointer currBP = new BlockPointer(currBPStr);
      content = this.pfsList.get(currBP.getPfsNumber()).getContent()[currBP.getBlockNumber()];
      nextPointer = new String(content, blockSize - 7, 7);

      // Set currBPStr block to empty
      this.pfsList.get(currBP.getPfsNumber()).setContentBlockEmpty(currBP.getBlockNumber());

      // update pointer
      currBPStr = nextPointer;
    }
  }

  public void deleteFCBFile(FCB fcb) {
    deleteFCBDataBlock(fcb);
    // deleteFCBIndexBlock(fcb);

    // delete FCB metadata and update size
    // deleteFCBinSuperBlock(fcb);
    for(int i=0; i< this.pfsList.size(); i++) {
      this.pfsList.get(i).writeContentToFile();
    }
  }




}