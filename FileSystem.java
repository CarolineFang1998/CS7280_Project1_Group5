import java.io.*;
import java.util.*;

/**
 * A simple file system simulation for managing .db0 database files and
 * implementing basic database operations through a command-line interface (CLI).
 */
public class FileSystem {
  public static final int BLOCK_SIZE = 256; // The size for a block. Unit is byte
  public List<DB> dbList = new ArrayList<>();

  /**
   * Finds and returns a set of unique database file names in the current directory,
   * specifically targeting files with the .db0 extension.
   *
   * @return A Set of unique database names, excluding the .db0 extension.
   */
  public Set<String> findUniqueDb0Files() {
    // Create a File object for the current directory
    File currentDir = new File(".");

    // FilenameFilter to filter only files with .db0 extension
    FilenameFilter filter = new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return name.endsWith(".db0");
      }
    };

    // List all files in the directory that match the filter
    File[] files = currentDir.listFiles(filter);

    // Create a HashSet to store unique file names (without the extension)
    Set<String> uniqueFileNames = new HashSet<>();

    // Iterate over the filtered files, extracting the file names without extension
    if (files != null) {
      for (File file : files) {
        String fileName = file.getName();
        String fileNameWithoutExtension = fileName.substring(0, fileName.length() - 4); // Remove ".db0" extension
        uniqueFileNames.add(fileNameWithoutExtension);
      }
    }

    return uniqueFileNames;
  }
  public void saveDatabase(DB database) {
    try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(database.getName() + ".db0"))) {
      oos.writeObject(database);
    } catch (IOException e) {
      System.err.println("Error saving database: " + e.getMessage());
    }
  }
  public DB loadDatabase(String dbName) {
    try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(dbName + ".db0"))) {
      return (DB) ois.readObject();
    } catch (IOException | ClassNotFoundException e) {
      System.err.println("Error loading database: " + e.getMessage());
      return null;
    }
  }

  public String showDatabase() {
    StringBuilder sb = new StringBuilder();
    for (DB db : dbList) {
      sb.append(db.getName()).append("\n");
    }
    System.out.println(sb.toString());
    return sb.toString();
  }

  public static void main(String[] args) throws IOException {
    FileSystem fileSystem = new FileSystem();
    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    //    String currentDatabase = null; // Track the currently open database
    DB currentDatabase = null; // Track the currently open database

    // find all the unique database
    Set<String> uniqueDb0Files = fileSystem.findUniqueDb0Files();
    System.out.println("Database in the current directory:");
    for (String fileName : uniqueDb0Files) {
      System.out.println(fileName);
    }
    System.out.println("...........................................");


    System.out.println("NoSQL CLI started. Type 'quit' to exit.");
    while (true) {
      System.out.print("NoSQL> ");
      if (currentDatabase != null) {
        System.out.print(currentDatabase.getName() + "> ");
      }
      String input = reader.readLine().trim();
      String[] commandParts = input.split(" ", 3); // Split the command and arguments
      String command = commandParts[0];


      if ("kill".equalsIgnoreCase(command)) {
        if (commandParts.length > 1) {
          String dbNameToKill = commandParts[1];
          // Make sure database name is valid
          if (uniqueDb0Files.contains(dbNameToKill)) {

            if (currentDatabase != null && currentDatabase.getName().equals(dbNameToKill)) {
              currentDatabase = null; // Reset current database if it's killed
            }

            // Delete files
            File currentDir = new File(".");
            FilenameFilter filter = (dir, name) -> name.startsWith(dbNameToKill);
            File[] filesToKill = currentDir.listFiles(filter);
            for (File file : filesToKill) {
              if (!file.delete()) {
                System.out.println("Failed to delete file: " + file.getName());
              } else {
                System.out.println("Deleted file: " + file.getName());
              }
            }
          } else {
            System.out.println("No matching db for 'kill' command.");
          }
        } else {
          System.out.println("Missing database name for 'kill' command.");
        }
      } else if ("quit".equalsIgnoreCase(command)) {
        System.out.println("Exiting NoSQL CLI...");
        break;
      } else if ("showdatabase".equalsIgnoreCase(command)) {
        fileSystem.showDatabase();

      } else if ("open".equalsIgnoreCase(command)) {
        if (commandParts.length > 1) {
          String databaseName = commandParts[1];
          //check if database exist
          // if the database does not exist: create a new database
          if (uniqueDb0Files.size() == 0 || !uniqueDb0Files.contains(databaseName)) {
            System.out.println("Database does not exist: creating a new database...");
            // create a new database.db0-> input (string name, block size)
            currentDatabase = new DB(databaseName, BLOCK_SIZE, false);
            uniqueDb0Files.add(databaseName);
            // save the database
            fileSystem.saveDatabase(currentDatabase);
            fileSystem.dbList.add(currentDatabase);
            fileSystem.loadDatabase(databaseName);


          } else {
            // if the database exists: load the old database
            currentDatabase = new DB(databaseName, BLOCK_SIZE, true);

          }
        } else {
          System.out.println("Missing database name for 'open' command.");
        }

      } else if (currentDatabase != null) {
        if ("put".equalsIgnoreCase(command)) {
          if (commandParts.length > 1) {
            // check if the file is exist in the current /csv dir, if not print "incorrect file name"
            // if the file name is correct
            // Define the directory where you expect the CSV files to be
            String directoryPath = "./csvs";
            File directory = new File(directoryPath);

            // Ensure the directory exists
            if (!directory.exists() || !directory.isDirectory()) {
              System.out.println("The CSV directory does not exist.");
              // Optionally, you can create the directory here if you want
              // directory.mkdirs();
            } else {
              // Construct the path to the expected file within the ./csv directory
              String filePath = directoryPath + "/" + commandParts[1];
              File file = new File(filePath);

              // Check if the file exists and is not a directory
              if (file.exists() && !file.isDirectory()) {
                // If the file exists, proceed with uploading the file to the database
                currentDatabase.uploadFCBFile(commandParts[1]);
              } else {
                // If the file does not exist, print an error message
                System.out.println("Incorrect file name or the file does not exist in the ./csv directory.");
              }
            }
          } else {
            System.out.println("Missing filename for 'put' command.");
          }

        } else if ("get".equalsIgnoreCase(command)) {
          if (commandParts.length > 1) {
            // todo: implement get
            String fileName = commandParts[1];
            // find the fcb
            if (currentDatabase.findFCBByName(fileName) == null) {
              System.out.println("FCB file not found.");
              continue;
            }
            FCB fcb = currentDatabase.findFCBByName(fileName);
            List<char[]> blocksData = currentDatabase.getFirstPFS().getBlocksByFCB(fcb);
            currentDatabase.getFirstPFS().writeRecordsToCSV(fileName, blocksData);
//            for (char[] blockData : blocksData) {
//
//              List<String> records =currentDatabase.getFirstPFS().extractRecordsFromBlock(blockData);
//              currentDatabase.getFirstPFS().writeRecordsToCSV(fileName, records);
//              for (String record : records) {
//                System.out.println(record);
            // write to a new csv file and store in the download directory


//              }
//


          } else {
            System.out.println("Missing filename for 'get' command.");
          }

        } else if ("dir".equalsIgnoreCase(command)) {
          //if fcblist is empty
          if (currentDatabase.getFcbList().size() == 0) {
            System.out.println("No FCB files found.");
            continue;
          }
          currentDatabase.showPFSFCBMetadata();
//
        } else if ("show".equalsIgnoreCase(command)) {
//          currentDatabase.showPFSFCBMetadata();
          currentDatabase.showPFSContent();
//        } else if ("t".equalsIgnoreCase(command)) {
////          currentDatabase.showPFSFCBMetadata();
//          if (commandParts.length > 1) {
//            String FCBName = commandParts[1]; //"movies-small.csv"
//            // find the fcb
//            if (currentDatabase.findFCBByName(FCBName) == null) {
//              System.out.println("FCB file not found.");
//              continue;
//            }
//            FCB fcb = currentDatabase.findFCBByName(FCBName);
////            String startPointer = fcb.getDataStartBlock();
////            int startPointerInt = Integer.parseInt(startPointer);
//            String indexBlock = fcb.getIndexStartBlock();
//            int indexBlockInt = Integer.parseInt(indexBlock);
//            currentDatabase.traverseAndUpdateBitmap(indexBlockInt);
//
//
//          } else {
//            System.out.println("Missing filename for 't' command.");
//          }

        } else if ("tree".equalsIgnoreCase(command)) { // given a fcb name, show the btree
          if (commandParts.length > 1) {
            String name = commandParts[1];
            Btree btree = currentDatabase.getBtree(name);
            FCB fcb = currentDatabase.findFCBByName(name);
            String indexBlock = fcb.getIndexStartBlock();
            int indexBlockInt = Integer.parseInt(indexBlock);
            System.out.println("Index block: " + indexBlockInt);

          } else {
            System.out.println("Missing filename for 'tree' command.");
          }
        } else if ("fcb".equalsIgnoreCase(command)) {
          if (commandParts.length > 1) {
            String name = commandParts[1];
            FCB fcb = currentDatabase.findFCBByName(name);
//            currentDatabase.getFirstPFS().showIndexBlockByFCB(fcb);
//            currentDatabase.getFirstPFS().removeDataBlock(fcb);
            currentDatabase.freeDataBlock(fcb);
            currentDatabase.processRowForBlockPointers(fcb);
//            currentDatabase.freeIndexBlock(fcb);
          } else {
            System.out.println("Missing filename for 'fcb' command.");
          }

//
        } else if ("find".equalsIgnoreCase(command)) {
          if (commandParts.length > 1) {
            String nameandkey = commandParts[1]; //"movies-small.csv.100"
            // split the name and key
            // name = movies-small.csv
            String name = nameandkey.split("\\.")[0] + "." + nameandkey.split("\\.")[1]; //name = movies-small.csv
            int key = Integer.parseInt(nameandkey.split("\\.")[2]); //key = 100
            // find the fcb
            if (currentDatabase.findFCBByName(name) == null) {
              System.out.println("FCB file not found.");
              continue;
            }
            FCB fcb = currentDatabase.findFCBByName(name);
            // find the btree
            Btree bTree = currentDatabase.getBtree(name);
            bTree.DisplayEntileBTree();
//            // print key and pointer
//            System.out.println(currentDatabase.getKeyPointerList(name));


            //name = movies-small
            String FCBName = name.split("\\.")[0];
            String pointer = currentDatabase.search(key, name);
            if (pointer == null) {

              continue;
            }
            DataBlockPointer dataBlockPointer = new DataBlockPointer(pointer);
            String record = currentDatabase.getRecordbyDataBlockPointer(dataBlockPointer.getPfsNumber(),
                    dataBlockPointer.getBlockNumber(), dataBlockPointer.getRecordNumber());

            System.out.println("Record found: " + record);


          } else {
            System.out.println("Missing key for 'find' command.");
          }
        } else if ("rm".equalsIgnoreCase(command)) {
          if (commandParts.length > 1) {
            String FCBName = commandParts[1]; //"movies-small.csv"
            // find the fcb
            if (currentDatabase.findFCBByName(FCBName) == null) {
              System.out.println("FCB file not found.");
              continue;
            }
            FCB fcb = currentDatabase.findFCBByName(FCBName);
            int fcbIndex = currentDatabase.getFcbList().indexOf(fcb);
            System.out.println("fcb index: " + fcbIndex);

            // remove the fcb from the fcb list
//              currentDatabase.removeFCB(FCBName);
            //print the fcb content
            int existingMetadataCount = currentDatabase.getNumOfFCBFiles();
            char[] fcbBlock = currentDatabase.getFirstPFS().getContent()[5];
            for (int i = 0; i < 57; i++) {
              System.out.print(fcbBlock[i]);
            }
            //print fcb content
            System.out.println("FCB content before removal:");
            System.out.println(fcbBlock);
            PFS pfs = currentDatabase.getFirstPFS();
            pfs.removeElements(fcbBlock, fcbIndex);
            // update bitmap
            pfs.freeBlocksByFCB(FCBName);


//
            currentDatabase.deleteOneFCBFile();
            currentDatabase.getFirstPFS().updateSuperBlock();
            // remove from fcb list
            currentDatabase.removeFCB(FCBName);
            System.out.println("FCB content after removal:");
            System.out.println(fcbBlock);
            pfs.updateFCBBlock(5, fcbBlock);


            // remove the fcb from the pfs
//              currentDatabase.getFirstPFS().deleteFCBMetadata(fcb);
//              System.out.println("FCB file removed: " + FCBName);

          } else {
            System.out.println("Missing filename for 'rm' command.");
          }


        } else {
          System.out.println("Unknown command or command not available outside a database context.");
        }


      } else {
        System.out.println("No database open. Use 'open <dbname>' to open a database.");
      }


    }
  }

}
