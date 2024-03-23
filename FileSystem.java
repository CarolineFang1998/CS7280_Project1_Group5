import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

public class FileSystem {
  public static final int BLOCK_SIZE = 256;

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

  public static void main(String[] args) throws IOException {
    FileSystem fileSystem = new FileSystem();
    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    //    String currentDatabase = null; // Track the currently open database
    DB currentDatabase = null; // Track the currently open database

    // find all the unique database
    Set<String> uniqueDb0Files = fileSystem.findUniqueDb0Files();
    System.out.println("Unique .db0 files in the current directory:");
    for (String fileName : uniqueDb0Files) {
      System.out.println(fileName);
    }


    System.out.println("NoSQL CLI started. Type 'quit' to exit.");
    while (true) {
      System.out.print("NoSQL> ");
      String input = reader.readLine().trim();
      String[] commandParts = input.split(" ", 3); // Split the command and arguments
      String command = commandParts[0];

      if ("quit".equalsIgnoreCase(command)) {
        System.out.println("Exiting NoSQL CLI...");
        break;
      } else if ("open".equalsIgnoreCase(command)) {
        if (commandParts.length > 1) {
            String databaseName = commandParts[1];
          //check if database exist
          // if the database does not exist: create a new database
          if (uniqueDb0Files.size()==0 || !uniqueDb0Files.contains(databaseName)) {
            System.out.println("if the database does not exist: create a new database");
            // create a new database.db0-> input (string name, block size)
            currentDatabase = new DB(databaseName, BLOCK_SIZE, false);
            uniqueDb0Files.add(databaseName);

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
          } else {
            System.out.println("Missing filename for 'get' command.");
          }

        } else if ("dir".equalsIgnoreCase(command)) {
//          // todo: implement list all the file
        } else if ("find".equalsIgnoreCase(command)) {
          if (commandParts.length > 1) {
            // todo: implement find
          } else {
            System.out.println("Missing key for 'find' command.");
          }

        } else if ("kill".equalsIgnoreCase(command)) {
          if (commandParts.length > 1) {
            // todo: implement kill
            if (commandParts[1].equals(currentDatabase)) {
              currentDatabase = null; // Reset current database if it's killed
            }

          } else {
            System.out.println("Missing database name for 'kill' command.");
          }

        } else {
          System.out.println("Unknown command or command not available outside a database context.");
        }

      } else {
        System.out.println("No database open. Use 'open <dbname>' to open a database.");
      }
    }
  }

  // Placeholder methods for database operations
  private static void openDatabase(String dbName) {
    System.out.println("Opening database: " + dbName);
    // Implement database opening logic here
  }

  private static void putFile(String dbName, String fileName) {
    System.out.println("Putting file into database: " + fileName);
    // Implement put file logic here
  }

  private static void getFile(String dbName, String fileName) {
    System.out.println("Getting file from database: " + fileName);
    // Implement get file logic here
  }

  private static void listFiles(String dbName) {
    System.out.println("Listing files in database.");
    // Implement file listing logic here
  }

  private static void findRecord(String dbName, String key) {
    System.out.println("Finding record with key: " + key);
    // Implement find record logic here
  }

  private static void killDatabase(String dbName) {
    System.out.println("Killing database: " + dbName);
    // Implement database killing logic here
  }
}
