public class BlockPointer {
  private int pfsNumber; // PFS file number, 3 digits
  private int blockNumber; // Block number, 4 digits
  private String ptrString; // Combined 7 digit string representation

  // Constructor using integers for PFS number and block number
  // if pfc # = 1, block # = 0. the ptrString will be 0010000.
  public BlockPointer(int pfsNumber, int blockNumber) {
    this.pfsNumber = pfsNumber;
    this.blockNumber = blockNumber;
    // Combine the numbers into a 7 character string, ensuring correct formatting
    this.ptrString = String.format("%03d%04d", pfsNumber, blockNumber);
  }

  // Constructor using a 7 character string
  public BlockPointer(String ptrString) {
    if (ptrString == null || ptrString.length() != 7) {
      throw new IllegalArgumentException("PtrString must be exactly 7 digits long");
    }
    this.ptrString = ptrString;
    // Parse the PFS number and block number from the string
    this.pfsNumber = Integer.parseInt(ptrString.substring(0, 3));
    this.blockNumber = Integer.parseInt(ptrString.substring(3, 7));
  }

  // Getter for the PFS file number
  public int getPfsNumber() {
    return this.pfsNumber;
  }

  // Getter for the block number
  public int getBlockNumber() {
    return this.blockNumber;
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
            ", ptrString='" + ptrString + '\'' +
            '}';
  }
}
