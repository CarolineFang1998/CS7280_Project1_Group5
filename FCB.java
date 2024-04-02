import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FCB {
    private String name; // FCB name, limited to 20 characters
//    private String time; // Timestamp, formatted as "15/SEP/23:25PM", limited to 14 characters
    private LocalDateTime time; // Timestamp, formatted as "15/SEP/23:25PM", limited to 14 characters
    private int size; // Number of blocks, assumed to be an integer
    private String dataStartBlock; // Pointer to data start block, 7 characters, default "9999999"
    private String indexStartBlock; // Pointer to index start block, 7 characters, default "9999999"

    // Constructor
    public FCB(String name, LocalDateTime time, int size) {
        this.name = name.length() > 20 ? name.substring(0, 20) : name;
        this.time = time;
        this.size = size;
        this.dataStartBlock = "9999999"; // Default value
        this.indexStartBlock = "9999999"; // Default value
    }

    // Additional constructor to specify all fields
    public FCB(String name, LocalDateTime time, int size, String dataStartBlock, String indexStartBlock) {
        this(name, time, size); // Reuse the first constructor for common initializations
        this.dataStartBlock = dataStartBlock.length() == 7 ? dataStartBlock : "9999999";
        this.indexStartBlock = indexStartBlock.length() == 7 ? indexStartBlock : "9999999";
    }

    // Getters and Setters
    public String getName() {
        return name;
    }
    public String getTime() {
        return time.format(DateTimeFormatter.ofPattern("dd/MMM/HH:mma"));
    }
    public int getSize() {
        return size;
    }
    public void showContent() {
        System.out.println(name+" "+time.format(DateTimeFormatter.ofPattern("dd/MMM/HH:mma"))+" "+size*256+" Bytes");


    }


}
