import java.io.Serializable;

public class DataBlockPointer implements Serializable {
  private int pfsNumber; // PFS file number, 3 digits
  private int blockNumber; // Block number, 4 digits
  private int recordNumber; // Record number, 1 digits, from 0 to 5
  private String ptrString; // Combined 8 digit string representation

  // Constructor using integers for PFS number and block number
  // if pfc # = 1, block # = 0, record # = 1. the ptrString will be 001000001.
  public DataBlockPointer(int pfsNumber, int blockNumber, int recordNumber) {
    this.pfsNumber = pfsNumber;
    this.blockNumber = blockNumber;
    this.recordNumber = recordNumber;
    // Combine the numbers into a 8 character string, ensuring correct formatting
    this.ptrString = String.format("%03d%04d%01d", pfsNumber, blockNumber, recordNumber);
  }

  // Constructor using a 8 character string
  public DataBlockPointer(String ptrString) {
    if (ptrString == null || ptrString.length() != 8) {
      throw new IllegalArgumentException("PtrString must be exactly 8 digits long");
    }
    this.ptrString = ptrString;
    // Parse the PFS number and block number from the string
    this.pfsNumber = Integer.parseInt(ptrString.substring(0, 3));
    this.blockNumber = Integer.parseInt(ptrString.substring(3, 7));
    this.recordNumber = Integer.parseInt(ptrString.substring(7, 8));
  }

  // Getter for the PFS file number
  public int getPfsNumber() {
    return this.pfsNumber;
  }

  // Getter for the block number
  public int getBlockNumber() {
    return this.blockNumber;
  }

  // Getter for the block number
  public int getRecordNumber() {
    return this.recordNumber;
  }

  // Getter for the pointer string
  public String getPtrString() {
    return this.ptrString;
  }



  // Override toString() for easy printing
  @Override
  public String toString() {
    return "BlockPointer{" +
            "pfsNumber=" + pfsNumber +
            ", blockNumber=" + blockNumber +
            ", recordNumber=" + recordNumber +
            ", ptrString='" + ptrString + '\'' +
            '}';
  }
}