
public class FCB {
    private String name; // FCB name, limited to 20 characters
    private String time; // Timestamp, formatted as "15/SEP/23:25PM", limited to 14 characters
    private int size; // Number of blocks, assumed to be an integer
    private String dataStartBlock; // Pointer to data start block, 7 characters, default "9999999"
    private String indexStartBlock; // Pointer to index start block, 7 characters, default "9999999"

    // Constructor
    public FCB(String name, String time, int size) {
        this.name = name.length() > 20 ? name.substring(0, 20) : name;
        this.time = time;
        this.size = size;
        this.dataStartBlock = "9999999"; // Default value
        this.indexStartBlock = "9999999"; // Default value
    }

    // Additional constructor to specify all fields
    public FCB(String name, String time, int size, String dataStartBlock, String indexStartBlock) {
        this(name, time, size); // Reuse the first constructor for common initializations
        this.dataStartBlock = dataStartBlock.length() == 7 ? dataStartBlock : "9999999";
        this.indexStartBlock = indexStartBlock.length() == 7 ? indexStartBlock : "9999999";
    }

    // Additional constructor to specify all fields
    public FCB(char[] fcbContent) {
        // Ensure that fcbContent has the correct length
        if (fcbContent.length != 58) {
            throw new IllegalArgumentException("FCB content should be exactly 58 characters long.");
        }

        // Name: First 20 characters, trim whitespace
        this.name = new String(fcbContent, 0, 20).trim();

        //if this is empty or deleted
        if (name == "") {
            // this block is empty
            this.name = "";
            this.time = "";
            this.size = 0;
            this.dataStartBlock = ""; // Default value
            this.indexStartBlock = ""; // Default value
            return;
        }

        // Time: Characters 20 to 33, parse to LocalDateTime
        this.time = new String(fcbContent, 20, 14);

        // Size: Characters 34 to 43, parse to int
        String sizeStr = new String(fcbContent, 34, 10).trim();

        try {
            this.size = Integer.parseInt(sizeStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid size format. Expected an integer.");
        }

        // Data start block: Characters 44 to 50
        this.dataStartBlock = new String(fcbContent, 44, 7);

        // Index start block: Characters 50 to 57
        this.indexStartBlock = new String(fcbContent, 51, 7);
    }


    // Getters and Setters
    public String getName() {
        return name;
    }
    public String getTime() {return time;}
    public int getSize() {
        return size;
    }
    public String getDataStartBlock() {
        return dataStartBlock;
    }
    public String getIndexStartBlock() {
        return indexStartBlock;
    }


    public void showContent() {
        System.out.println(name+" "+time+" "+size*256+" Bytes");
    }

    public void print() {

        System.out.println(name+" "+time+" "+size+" " + this.dataStartBlock +" "+ this.indexStartBlock);
    }

    public String toString() {
        String newName = name;
        // Pad the FCBName to ensure it is exactly 20 characters long
        while (newName.length() < 20) {
            newName += " ";
        }

        // Convert the size to a String and ensure it is exactly 10 bytes
        String sizeStr = String.valueOf(size);
        while (sizeStr.length() < 10) {
            sizeStr = "0" + sizeStr; // Pad with spaces to align to the right
        }
//    System.out.println("sizeStr " + sizeStr);

        // Prepare the final metadata string
        String metadataStr = newName + time + sizeStr + dataStartBlock + indexStartBlock;
        // Convert the metadata string to a char array and return
        return metadataStr;
    }

}
