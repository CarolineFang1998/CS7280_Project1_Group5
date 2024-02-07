/*
 * CS7280 Special Topics in Database Management
 * Project 1: B-tree implementation.
 *
 * You need to code for the following functions in this program
 *   1. Lookup(int value) -> nodeLookup(int value, int node)
 *   2. Insert(int value) -> nodeInsert(int value, int node)
 *   3. Display(int node)
 *
 */

final class Btree {

  /* Size of Node. */
  private static final int NODESIZE = 5;

  /* Node array, initialized with length = 1. i.e. root node */
  private Node[] nodes = new Node[1];

  /* Number of currently used nodes. Default is 0. */
  private int cntNodes;

  /* Pointer to the root node. Default is 0. */
  private int root;

  /* Number of currently used values. Default is 0. */
  private int cntValues;

  /*
   * B+ tree Constructor.
   */
  public Btree() {
    root = initNode();
    nodes[root].children[0] = createLeaf();
  }

  /*********** B tree functions for Public ******************/

  /*
   * Lookup(int value)
   *   - True if the value was found.
   */
  public boolean Lookup(int value) {
    return nodeLookup(value, root);
  }

  /*
   * Insert(int value)
   *    - If -1 is returned, the value is inserted and increase cntValues.
   *    - If -2 is returned, the value already exists.
   */
  public void Insert(int value) {
    if(nodeInsert(value, root) == -1) cntValues++;
  }

  public void Display(int nodeId) {
    Node node = nodes[nodeId];
    if (node == null) return;
    if (isLeaf(node)) { // Leaf node
        for (int i = 0; i < node.size; i++) {
            System.out.print(node.values[i] + " ");
        }
        System.out.println();
    } else { // Internal node
        for (int i = 0; i < node.size; i++) {
            Display(node.children[i]);
            System.out.print("[" + node.values[i] + "] ");
        }
        Display(node.children[node.size]);
    }

    if (nodeId == root) {
      // If this is the root node, print a newline at the end
      System.out.println();
    }
  }

  /*
   * CntValues()
   *    - Returns the number of used values.
   */
  public int CntValues() {
    return cntValues;
  }

  /*********** B-tree functions for Internal  ******************/

  /*
   * nodeLookup(int value, int pointer)
   *    - True if the value was found in the specified node.
   *
   */
  private boolean nodeLookup(int value, int pointer) {
    Node node = nodes[pointer];
    int i = 0;

    // Iterate through keys in the node to find the smallest index i such that value <= node.values[i]
    while (i < node.size && value > node.values[i]) {
        i++;
    }

    // If the value matches the key at index i in the node
    if (i < node.size && value == node.values[i]) {
        return true; // The value is found
    }

    // If the node is a leaf, then the search is unsuccessful
    if (isLeaf(node)) {
        return false;
    } else {
        // Recur to search the appropriate subtree
        return nodeLookup(value, node.children[i]);
    }
  }

  // TODO: might use binary search for nodeLookup:

//   private boolean nodeLookup(int value, int pointer) {
//     Node node = nodes[pointer];
    
//     // Perform a binary search to find the smallest index i such that value <= node.values[i]
//     int left = 0;
//     int right = node.size - 1;
//     int i = 0;

//     while (left <= right) {
//         int mid = left + (right - left) / 2;

//         if (node.values[mid] == value) {
//             return true; // The value is found
//         } else if (node.values[mid] < value) {
//             left = mid + 1;
//         } else {
//             i = mid; // Record the position to recurse on if the value is not found
//             right = mid - 1;
//         }
//     }

//     // If the node is a leaf, then the search is unsuccessful
//     if (isLeaf(node)) {
//         return false;
//     } else {
//         // Recur to search the appropriate subtree
//         // Note: The 'children' array holds pointers to the children nodes
//         // We assume that children nodes exist and are loaded in memory
//         // In an actual disk-based B-tree implementation, you would perform a disk read operation here
//         return nodeLookup(value, node.children[i]);
//     }
// }


  /*
   * nodeInsert(int value, int pointer)
   *    - -2 if the value already exists in the specified node
   *    - -1 if the value is inserted into the node or
   *            something else if the parent node has to be restructured
   */
  private int nodeInsert(int value, int pointer) {
    //
    //
    // TODO
    //
    //
    return XXX;
  }


  /*********** Functions for accessing node  ******************/

  /*
   * isLeaf(Node node)
   *    - True if the specified node is a leaf node.
   *         (Leaf node -> a missing children)
   */
  boolean isLeaf(Node node) {
    // A node is considered a leaf if it has no children.
    // check if the 'children' field is null.
    if (node.children == null) {
      return true;
    }
    
    // check if the 'children' list is empty.
    return node.children.length == 0;
  }
  

  /*
   * initNode(): Initialize a new node and returns the pointer.
   *    - return node pointer
   */
  int initNode() {
    Node node = new Node();
    node.values = new int[NODESIZE];
    node.children =  new int[NODESIZE + 1];

    checkSize();
    nodes[cntNodes] = node;
    return cntNodes++;
  }

  /*
   * createLeaf(): Creates a new leaf node and returns the pointer.
   *    - return node pointer
   */
  int createLeaf() {
    Node node = new Node();
    node.values = new int[NODESIZE];

    checkSize();
    nodes[cntNodes] = node;
    return cntNodes++;
  }

  /*
   * checkSize(): Resizes the node array if necessary.
   */
  private void checkSize() {
    // if node array is already full, temp will be cntNodes * 2
    if(cntNodes == nodes.length) {
      Node[] tmp = new Node[cntNodes << 1];
      System.arraycopy(nodes, 0, tmp, 0, cntNodes);
      nodes = tmp;
    }
  }
}

/*
 * Node data structure.
 *   - This is the simplest structure for nodes used in B-tree
 *   - This will be used for both internal and leaf nodes.
 */
final class Node {
  /* Node Values (Leaf Values / Key Values for the children nodes).  */
  int[] values;

  /* Node Array, pointing to the children nodes.
   * This array is not initialized for leaf nodes.
   */
  int[] children;

  /* Number of entries
   * (Rule in B Trees:  d <= size <= 2 * d).
   */
  int size;

  // TODO: need a constructor
}
