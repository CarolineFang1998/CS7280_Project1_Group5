import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
      System.out.println("creating DB " + name);
      init();
      loadExistingPFSs();
    } else {
      System.out.println("loading DB " + name);
      // todo, load the previous pfs files
      loadExistingPFSs();
      System.out.println("loading PFS size" + numOfPFSFiles);
      // need to create a pfs first
      // todo: load the fcb lists
      this.numOfFCBFiles = this.pfsList.get(0).loadExistingFCB(this.fcbList);
      System.out.println("loading fcb size" + numOfFCBFiles);
    }
  }


  public void loadExistingPFSs() {
    // find fcbs in the current dir and count how many of them
    this.numOfPFSFiles = countPFSFiles();
    System.out.println("counting numOfPFSFiles " + numOfPFSFiles);
    for(int i=0; i < this.numOfPFSFiles; i++) {
      PFS pfs = new PFS(this, i);
      this.pfsList.add(pfs);
    }
  }

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
     Btree btree =  generateBTree(keyPointerList, fileName);
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

    // todo: implement the fcb metadata class and dynamically update
    // FCB name: 20 char, time: 14 char (sample:  "15/SEP/23:25PM")
    // number of blocks 10 int.  data start block(7 char): default: 9999999,
    // index start block(7 char): default: 9999999,
    LocalDateTime time = LocalDateTime.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MMM/yy:HHa");
    String formattedTime = time.format(formatter);
    FCB newFCB = new FCB(fileName, formattedTime, blocksSize + btree.getCntNodes(), dataStartPtr, indexRootPtr);
    fcbList.add(newFCB);

    pfsList.get(0).updateFCBMetadata(fcbList);

//    System.out.println("empty space" + pfsList.get(0).getBlockLeft());
//    System.out.println("calculate empty space" + pfsList.get(0).calculateBlocksLeft());

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
  public Btree generateBTree(List<KeyPointer> keyPointerList, String fcbFilename){
    Btree btree = new Btree();
//    this.btree = new Btree();

    for(KeyPointer currKeyPtr:keyPointerList) {
//      System.out.println(currKeyPtr.getKey() + " " + currKeyPtr.getKeyPointerStr());
      btree.Insert(currKeyPtr);
//      btree.DisplayEntileBTree();
//      this.btree.Insert(currKeyPtr);
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
//            System.out.println("Key found.");
//            System.out.println(result.keyPointer.getKeyPointerStr());
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
  public void freeDataBlock(FCB fcb) {
    String dataStartBlock = fcb.getDataStartBlock();
    int dataStartBlockNumber = Integer.parseInt(dataStartBlock);
    int endBlockNumber = Integer.parseInt(fcb.getIndexStartBlock()) - 1;
    for (int i = dataStartBlockNumber; i <= endBlockNumber; i++) {
//            System.out.println(pfsList.get(0).getContent()[i]);
      pfsList.get(0).updateBitMap(i, false);

    }
  }

  public Queue<Integer> clean(FCB fcb) {
    String indexStartBlock = fcb.getBlockNumber();
    int indexStartBlockNumber = Integer.parseInt(indexStartBlock);
    Queue<Integer> queue = new LinkedList<>();
    queue.add(indexStartBlockNumber);
    System.out.println("first q" +queue);
//                processRowForBlockPointers(indexStartBlockNumber);
    // get first element of the queue
//        int firstBlockNumber = queue.peek();
//        pfsList.get(0).updateBitMap(firstBlockNumber, false);
//        // overwrite the block with empty char array
//        Arrays.fill(pfsList.get(0).getContent()[firstBlockNumber], ' ');
//    System.out.println("this block content"+ firstBlockNumber + " " + Arrays.toString(pfsList.get(0).getContent()[firstBlockNumber]));
//        queue.poll();


//    // clean the data block
    while (!queue.isEmpty()) {
      int currentBlockNumber = queue.peek();


      Queue<Integer> childPointers = processRowForBlockPointers(currentBlockNumber);
//            System.out.println("childPointers" + childPointers);
      pfsList.get(0).updateBitMap(currentBlockNumber, false);
//      // overwrite the block with empty char array
      Arrays.fill(pfsList.get(0).getContent()[currentBlockNumber], ' ');

      queue.addAll(childPointers);
      queue.poll();
//            System.out.println("queue" + queue);

    }
    try {
      this.pfsList.get(0).writeCharArrayToFile();
      System.out.println("File updated successfully.");
    } catch (IOException e) {
      System.err.println("An error occurred while writing the file: " + e.getMessage());
    }

    System.out.println("empty blocks" + pfsList.get(0).getBlockLeft());
    return queue;

  }

  public Queue<Integer> processRowForBlockPointers(int blockNumber) {
    // extract all contents from pfsList
//        for (PFS pfs : pfsList) {
//            char[][] pfsContent = pfs.getContent();
//            // Calculate new size for the merged array
//            char[][] newPfsContent = new char[this.allPfsContent.length + pfsContent.length][];
//
//            // Copy existing content into the new array
//            System.arraycopy(this.allPfsContent, 0, newPfsContent, 0, this.allPfsContent.length);
//
//            // Copy new content from the current PFS into the new array
//            System.arraycopy(pfsContent, 0, newPfsContent, this.allPfsContent.length, pfsContent.length);
//
//            // Update allPfsContent to point to the new, merged array
//            this.allPfsContent = newPfsContent;
//        }
    Map<Integer, char[][]> pfsContents = new HashMap<>();
    for (PFS pfs : pfsList) {
      char[][] content = pfs.getContent();


      pfsContents.put(pfs.getSequenceNumber(), content);
    }

//        char[][] specificPfsContent = pfsContents.get(pfsNumber);
    char[][] content = pfsList.get(0).getContent();
//        char[][] content = this.allPfsContent;
    Queue<Integer> blockPointersQueue = new LinkedList<>();
//        blockPointersQueue.add(blockNumber);
    final int BLOCK_POINTER_SIZE = 7; // Size of block pointer
    final int KEY_POINTER_SIZE = 15; // Size of key pointer
    final String TERMINATOR = "9999999";

    // Extract the row from content based on rowIndex
    char[] row = content[blockNumber];

    // Initialize the starting index for block pointer extraction
    int index = 0;

    // Loop through the row, extracting block pointers and key pointers alternately
    while (index < row.length) {
      if (row[index] == '\0') break;
      // Extract the block pointer
      String blockPointerStr = new String(row, index, BLOCK_POINTER_SIZE);

      // Check if the block pointer is the terminator
      if (!TERMINATOR.equals(blockPointerStr)) {
        try {
          // Parse the block pointer and add to the queue if not the terminator
          int blockPointer = Integer.parseInt(blockPointerStr);
          blockPointersQueue.add(blockPointer);
        } catch (NumberFormatException e) {
//          System.err.println("Invalid block pointer encountered: " + blockPointerStr);
        }
      }

      // Move the index to skip over the next key pointer
      index += BLOCK_POINTER_SIZE + KEY_POINTER_SIZE;

      // If the next position exceeds the row's length, break the loop
      if (index >= row.length) break;
      // if the next block pointer is the terminator and the next char is null, break the loop
      if (row[index] == '\0' && row[index + 1] == '\0') break;


      // Check if the remaining characters are less than a block pointer size
      // This can happen if the row's data structure is not strictly followed
      if (row.length - index < BLOCK_POINTER_SIZE && row[index] == '\0') {
        System.err.println("Incomplete block pointer at the end of the row.");
        break;
      }
    }
//        System.out.println("blockPointersQueue: " + blockPointersQueue);
    return blockPointersQueue;
  }

  public Queue<Integer> cleanAll(FCB fcb) {
    String indexStartBlock = fcb.getBlockNumber();
    int indexStartBlockNumber = Integer.parseInt(indexStartBlock);
    Queue<Integer> queue = new LinkedList<>();
    queue.add(indexStartBlockNumber);
    System.out.println("first q" +queue);


//    // clean the data block
    while (!queue.isEmpty()) {
      int currentBlockNumber = queue.peek();


      Queue<Integer> childPointers = processRowForBlockPointers(currentBlockNumber);
//            System.out.println("childPointers" + childPointers);
      pfsList.get(0).updateBitMap(currentBlockNumber, false);
//      // overwrite the block with empty char array
      Arrays.fill(pfsList.get(0).getContent()[currentBlockNumber], ' ');

      queue.addAll(childPointers);
      queue.poll();
//            System.out.println("queue" + queue);

    }
    try {
      this.pfsList.get(0).writeCharArrayToFile();
      System.out.println("File updated successfully.");
    } catch (IOException e) {
      System.err.println("An error occurred while writing the file: " + e.getMessage());
    }

    System.out.println("empty blocks" + pfsList.get(0).getBlockLeft());
    return queue;

  }

  public Queue<Integer> processAllIndexBlocks(int blockNumber, int pfsNumber) {
    // extract all contents from pfsList

//        }
    Map<Integer, char[][]> pfsContents = new HashMap<>();
    for (PFS pfs : pfsList) {
      char[][] content = pfs.getContent();


      pfsContents.put(pfsNumber, content);
    }

        char[][] specificPfsContent = pfsContents.get(pfsNumber);
//    char[][] content = pfsList.get(0).getContent();
//        char[][] content = this.allPfsContent;
    Queue<Integer> blockPointersQueue = new LinkedList<>();
//        blockPointersQueue.add(blockNumber);
    final int BLOCK_POINTER_SIZE = 7; // Size of block pointer
    final int KEY_POINTER_SIZE = 15; // Size of key pointer
    final String TERMINATOR = "9999999";

    // Extract the row from content based on rowIndex
    char[] row = specificPfsContent[blockNumber];

    // Initialize the starting index for block pointer extraction
    int index = 0;

    // Loop through the row, extracting block pointers and key pointers alternately
    while (index < row.length) {
      if (row[index] == '\0') break;
      // Extract the block pointer
      String blockPointerStr = new String(row, index, BLOCK_POINTER_SIZE);

      // Check if the block pointer is the terminator
      if (!TERMINATOR.equals(blockPointerStr)) {
        try {
          // Parse the block pointer and add to the queue if not the terminator
          int blockPointer = Integer.parseInt(blockPointerStr);
          blockPointersQueue.add(blockPointer);
        } catch (NumberFormatException e) {
//          System.err.println("Invalid block pointer encountered: " + blockPointerStr);
        }
      }

      // Move the index to skip over the next key pointer
      index += BLOCK_POINTER_SIZE + KEY_POINTER_SIZE;

      // If the next position exceeds the row's length, break the loop
      if (index >= row.length) break;
      // if the next block pointer is the terminator and the next char is null, break the loop
      if (row[index] == '\0' && row[index + 1] == '\0') break;


      // Check if the remaining characters are less than a block pointer size
      // This can happen if the row's data structure is not strictly followed
      if (row.length - index < BLOCK_POINTER_SIZE && row[index] == '\0') {
        System.err.println("Incomplete block pointer at the end of the row.");
        break;
      }
    }
//        System.out.println("blockPointersQueue: " + blockPointersQueue);
    return blockPointersQueue;
  }






}