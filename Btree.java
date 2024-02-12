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
  private static final int NODESIZE = 3;

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
    return nodeLookup(value, root, "");
  }

  public void Insert(int value) {
    int result = nodeInsert(value, root);
    if (result == -1) {
        cntValues++; // Value successfully inserted, increment the count of values
        System.out.println("Insertion complete: " + value + " has been added.");
    } else if (result == -2) {
        System.out.println("Insertion failed: " + value + " already exists.");
    }
}

  public void DisplayEntileBTree() {
    System.out.println("--------------Display Entile Tree----------------\n");
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
    int level = 0;

    while (!queue.isEmpty()) {
        int levelLength = queue.size();
        System.out.print("L-"+level+": ");
        level++;
        for (int i = 0; i < levelLength; i++) {
            int currentId = queue.poll();
            Node currentNode = nodes[currentId];

            // Print all values within the current node
            System.out.print(currentId+"[");
            int count = 0;
            for (int val : currentNode.values) {    
              if (count > 0) {
                System.out.print(",");
              }              
              if (val != -1) { 
                System.out.print(val );
              } else {
                System.out.print(" ");
              }
              count++;
            } 
            System.out.print("]");

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
  private boolean nodeLookup(int value, int pointer, String s) {
    Node node = nodes[pointer];
    int i = 0;
    s += "" + pointer;

    // Iterate through keys in the node to find the smallest index i such that value <= node.values[i]
    while (i >= 0 && i < node.size && value > node.values[i]) {
        i++;
    }

    // If the value matches the key at index i in the node
    if (i >= 0 && i < node.size && value == node.values[i]) {
        System.out.println("Founded " + value + " Path: " + s );
        return true; // The value is found
    }

    // If the node is a leaf, then the search is unsuccessful
    if (isLeaf(node)) {
      System.out.println( "No key found");
      return false;
    } else {
       s += " -> " ;
      // Recur to search the appropriate subtree
        return nodeLookup(value, node.children[i], s);
    }
  }
    private void splitChild(int parent, int i, int fullChild) {
        int newChild = initNode();
        Node child = nodes[fullChild];
        Node newNode = nodes[newChild];

        // Determine indices based on NODESIZE being even or odd
        int promoteIndex = (NODESIZE % 2 == 0) ? (NODESIZE / 2) - 1 : NODESIZE / 2;
        int startIndexOfNewNode = promoteIndex + 1;

        // For odd NODESIZE, the right half includes the middle element
        int numOfValuesToNewNode = NODESIZE / 2 ;
        newNode.size = numOfValuesToNewNode;

        // Copy values to the new node, starting from startIndexOfNewNode
        System.arraycopy(child.values, startIndexOfNewNode, newNode.values, 0, numOfValuesToNewNode);

        if (!isLeaf(child)) {
            // Calculate the number of children to move for both even and odd NODESIZE
            int childrenToMove = NODESIZE + 1 - startIndexOfNewNode;
            newNode.childrenSize = childrenToMove;

            // Copy the children to the new node
            System.arraycopy(child.children, startIndexOfNewNode, newNode.children, 0, childrenToMove);
            Arrays.fill(child.children, startIndexOfNewNode, child.children.length, -1); // Clear moved children references
        }

        // Adjust the size of the original child node
        child.size = promoteIndex;

        // Make room and promote the value to the parent node
        System.arraycopy(nodes[parent].children, i + 1, nodes[parent].children, i + 2, nodes[parent].size - i);
        nodes[parent].children[i + 1] = newChild;
        System.arraycopy(nodes[parent].values, i, nodes[parent].values, i + 1, nodes[parent].size - i);
        nodes[parent].values[i] = child.values[promoteIndex];

        // Clear the values that were moved to the new node
        Arrays.fill(child.values, promoteIndex, NODESIZE, -1);

        // Update the parent node's size
        nodes[parent].size++;
        nodes[parent].childrenSize++;
    }


    /**
   * Splits the specified full child of a given parent node.
   * 
   * @param parent   The pointer of the parent node in the nodes array.
   * @param i        The index within the parent node's children array where the full child is located.
   * @param fullChild The pointer of the full child node in the nodes array that needs to be split.
   */
  private int insertNonFull(int nodeIndex, int value) {
    Node node = nodes[nodeIndex];
    int i = node.size - 1;

    // Check if the value already exists 
    for (int j = 0; j < node.size; j++) {
      if (node.values[j] == value) {
          return -2; // Value already exists
      }
  }

    // System.out.println("isLeaf?" + isLeaf(node));
    if (isLeaf(node)) { 
        // Insert the value into the correct position in a leaf node
        while (i >= 0 && value < node.values[i]) {
            node.values[i + 1] = node.values[i];
            i--;
        }
        node.values[i + 1] = value;
        node.size++;
        return -1; // Value inserted successfully
    } 

    // Determine the correct child node to descend into
    while (i >= 0 && value < node.values[i]) {
      i--;
    }
    i++;

    // if the node is full, split the node
    if (nodes[node.children[i]].size == NODESIZE) {
        splitChild(nodeIndex, i, node.children[i]); 
        if (value > node.values[i]) {
            i++;
        }
    }
    // Recurse into the appropriate child node for insertion
    return insertNonFull(node.children[i], value);
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
            // Recurse to insert the value into the non-full node
            return insertNonFull(newRoot, value);

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