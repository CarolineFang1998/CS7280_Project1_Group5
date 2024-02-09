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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

final class Btree {

  /* Size of Node. Mininum is 2. */
  private static final int NODESIZE = 2;

  /* Node array, initialized with length = 1. i.e. root node */
  private Node[] nodes = new Node[1];

  /* Number of currently used nodes. Default is 0. */
  private int cntNodes;

  /* Pointer to the root node. Default is 0. */
  private int root;

  /* Number of currently used values. Default is 0. */
  private int cntValues;

  /*
   * B tree Constructor.
   */
  public Btree() {
    root = initNode();
  }

  /*********** B tree functions for Public ******************/

  /*
   * Lookup(int value)
   *   - True if the value was found.
   */
  public boolean Lookup(int value) {
    return nodeLookup(value, root);
  }

  public void Insert(int value) {
    int result = nodeInsert(value, root);
    if (result == -1) {
        cntValues++; // Value successfully inserted, increment the count of values
        System.out.println("Insertion complete: " + value + " has been added.");
    } else if (result == -2) {
        System.out.println("Insertion failed: " + value + " already exists.");
    }
    // Handle other cases as necessary
}

  public void DisplayEntileBTree() {
    System.out.println("-------------------------------------------------\n");
    Display(this.root);
    System.out.println("\nTotal number of values (cntValues): " + cntValues);
    System.out.println("Total number of nodes (cntNodes): " + cntNodes);
    System.out.println("-------------------------------------------------\n");
  }

  public void Display(int nodeId) {
    if (nodeId < 0 || nodes[nodeId] == null)
        return;

    Queue<Integer> queue = new LinkedList<>();
    queue.add(nodeId);

    while (!queue.isEmpty()) {
        int levelLength = queue.size();

        for (int i = 0; i < levelLength; i++) {
            int currentId = queue.poll();
            Node currentNode = nodes[currentId];

            // Print all values within the current node
            System.out.print("[");
            for (int val : currentNode.values) {                  
              if (val != -1) { 
                System.out.print(val + " ");
              } else {
                break;
              }
            } 
            System.out.print("]");

            // System.out.print("(childrenSize"+ currentNode.childrenSize + ")");
            // System.out.print("(size"+ currentNode.size + ")");
            // Add child nodes of the current node to the queue for later processing
            for (int j = 0; j <= NODESIZE; j++) { // Iterate through all possible children
                int childId = currentNode.children[j];
                if (childId != -1) { 
                    queue.add(childId);
                } else {
                  break;
                }
            }

            System.out.print("\t"); // Tab-space for separating nodes at the same level
        }

        System.out.println(); // Newline after each level is processed
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
    while (i >= 0 && i < node.size && value > node.values[i]) {
        i++;
    }

    // If the value matches the key at index i in the node
    if (i >= 0 && i < node.size && value == node.values[i]) {
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

  /**
   * Splits the specified full child of a given parent node.
   * 
   * @param parent   The pointer of the parent node in the nodes array.
   * @param i        The index within the parent node's children array where the full child is located.
   * @param fullChild The pointer of the full child node in the nodes array that needs to be split.
   */

  private void splitChild(int parent, int childIndex, int fullChild) {
    int newChild = initNode();
    Node child = nodes[fullChild];
    Node newNode = nodes[newChild];

    // Calculate the number of values to move to the new node.
    // For even NODESIZE, this will be NODESIZE/2.
    // For odd NODESIZE, this will be NODESIZE/2 + 1, ensuring the median moves up.
    int splitIndex = (NODESIZE - 1) / 2; // Index of the median value for odd NODESIZE, left median for even.
    int numberOfValuesToMove = NODESIZE - splitIndex - 1; // Adjust based on whether NODESIZE is even or odd.

    // Distribute the second half (or the last NODESIZE/2 elements for even, NODESIZE/2 + 1 for odd) to the new node.
    System.arraycopy(child.values, splitIndex + 1, newNode.values, 0, numberOfValuesToMove);
    Arrays.fill(child.values, splitIndex + 1, NODESIZE, -1); // Clear moved values

    newNode.size = numberOfValuesToMove;
    child.size = splitIndex;

    if (!isLeaf(child)) {
        // If the child is not a leaf, distribute the corresponding children.
        System.arraycopy(child.children, splitIndex + 1, newNode.children, 0, numberOfValuesToMove + 1);
        Arrays.fill(child.children, splitIndex + 1, NODESIZE + 1, -1); // Clear moved children references

        newNode.childrenSize = numberOfValuesToMove + 1;
        child.childrenSize = splitIndex + 1;
    }

    // Make room for the new child pointer in the parent node.
    System.arraycopy(nodes[parent].children, childIndex + 1, nodes[parent].children, childIndex + 2, nodes[parent].size - childIndex);
    nodes[parent].children[childIndex + 1] = newChild;

    // Move the median value to the parent node.
    System.arraycopy(nodes[parent].values, childIndex, nodes[parent].values, childIndex + 1, nodes[parent].size - childIndex);
    nodes[parent].values[childIndex] = child.values[splitIndex];
    Arrays.fill(child.values, splitIndex, splitIndex + 1, -1); // Clear the median value

    // Update the size of the parent node.
    nodes[parent].size++;
    nodes[parent].childrenSize++;
}

  private int insertNonFull(int nodeIndex, int value) {
    Node node = nodes[nodeIndex];
    int i = node.size - 1;

    if (isLeaf(node)) {
        // Insert the new value into the correct position in a leaf node.
        while (i >= 0 && value < node.values[i]) {
            node.values[i + 1] = node.values[i];
            i--;
        }
        node.values[i + 1] = value;
        node.size++;
        return -1; // Indicate successful insertion.
    } else {
        // Find the child node that should receive the new value.
        while (i >= 0 && value < node.values[i]) {
            i--;
        }
        i++;

        if (nodes[node.children[i]].size == NODESIZE) {
            // If the found child is full, split it.
            splitChild(nodeIndex, i, node.children[i]);
            
            // After splitting, determine which of the two new children should receive the new value.
            if (value > node.values[i]) {
                i++;
            }
        }
        return insertNonFull(node.children[i], value); // Recursively insert into the correct child node.
    }
}


  /*
   * nodeInsert(int value, int pointer)
   *    - -2 if the value already exists in the specified node
   *    - -1 if the value is inserted into the node
   */
  private int nodeInsert(int value, int pointer) {
    Node currNode = nodes[pointer];
    if (currNode.size == NODESIZE ) { // If current node is full
        if (pointer == root) { // if current node is root
          // create a new root and split the old root
            int newRoot = initNode();
            nodes[newRoot].children[0] = pointer;
            nodes[newRoot].childrenSize++;
            splitChild(newRoot, 0, pointer);
            root = newRoot;
            return insertNonFull(newRoot, value); // Insert into new root after split
        } else {
            return pointer; // Need parent to handle split
        }
    }
    return insertNonFull(pointer, value); // Direct insert into non-full node
  }


  /*********** Functions for accessing node  ******************/

  /*
   * isLeaf(Node node)
   *    - True if the specified node is a leaf node.
   *         (Leaf node -> a missing children)
   */
  boolean isLeaf(Node node) {
    return node.childrenSize == 0;
  }
  

  /*
   * initNode(): Create a new node and returns the pointer.
   *    - return node pointer
   */
  int initNode() {
    Node node = new Node();
    node.values = new int[NODESIZE];
    node.children =  new int[NODESIZE + 1];

    // init node values and children into -1;
    Arrays.fill(node.values, -1);
    Arrays.fill(node.children, -1);

    checkSize();
    nodes[cntNodes] = node;
    return cntNodes++;
  }

  // /*
  //  * createLeaf(): Creates a new leaf node and returns the pointer.
  //  *    - return node pointer
  //  */
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

  /* Number of children*/
  int childrenSize;
}
