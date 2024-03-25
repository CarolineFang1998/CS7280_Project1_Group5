public class KeyPointer {
  private Integer key;
  private String pointer;
  private String keyPointerStr;

  /**
   * Constructs a KeyPointer object with the given key and pointer.
   * @param key The key associated with this KeyPointer.
   * @param pointer The pointer string associated with this KeyPointer, which should be an 8-digit string
   *                following the DataBlockPointer format (PPPBBBBR).
   */
  public KeyPointer(int key, String pointer) {
    if (pointer == null || pointer.length() != 7) {
      throw new IllegalArgumentException("Pointer must be exactly 7 digits long");
    }
    this.key = key;
    this.pointer = pointer;
    String keyStr = String.format("%07d", key);
    this.keyPointerStr = keyStr + pointer;
  }

  // Constructor using a 15 character string
  public KeyPointer(String keyPointerStr) {
    if (keyPointerStr == null || keyPointerStr.length() != 15) {
      throw new IllegalArgumentException("PtrString must be exactly 8 digits long");
    }
    this.keyPointerStr = keyPointerStr;
    // Parse the PFS number and block number from the string
    this.key = Integer.parseInt(keyPointerStr.substring(0, 8));
    this.pointer = keyPointerStr.substring(8, 15);
  }

  // Getter for the key
  public int getKey() {
    return key;
  }

  // Getter for the pointer
  public String getPointer() {
    return pointer;
  }

  public String getKeyPointerStr(){
    return keyPointerStr;
  }

  // Override toString() for easy printing
  @Override
  public String toString() {
    return "KeyPointer{" +
            "key=" + key +
            ", pointer='" + pointer +
            ", keyPointerStr='" + keyPointerStr
            + '\'' +
            '}';
  }
}
