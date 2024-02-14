
# Indexing Structure for NoSQL database

This Java project implements a B-tree data structure for NoSQL database, offering efficient algorithms for insertion, 
lookup, and display operations. 

## How to Run the Code

1. **Compilation:** Compile the Java files in your development environment or using a command line, e.g., `javac Btree.java BtreeTest.java`.
2. **Execution:** Run the compiled `BtreeTest` class to execute predefined tests, e.g., `java BtreeTest`.

## Running Tests

The `BtreeTest` class includes a `main` method that executes multiple predefined test cases. Uncomment any test case in the `main` method to run it:

```java
// Example test case
test(new int[] {29,41,44,62,46,49,27,76,91,30,100,47,34,53,9,45});
```

## Reason to choose B-tree 

- High Fanout and Efficient Disk Access: 
   B-trees has a high branching factor. Each node can contain a large number of keys. This reduces the number of levels
   in the tree. A n-node B-tree has height O(lgn), and many I/O operations can be within O(lgn) time.
- Ordered Data Storage: 
   B-trees store data in a sorted order. This makes range queries and range scans very efficient.
- Balanced Tree: 
   B-tree is self balanced. So the height of the tree is always logarithmic with respect to the number 
   of keys in the tree.Even if number of keys are very large, the height of the tree is always O(lgn).
- Handling dynamic data: 
   B-tree ensures dynamic data insertion and deletion while maintaining balance and performance.
- Scalability: 
   B-tree is widely used in database and file systems. It is used to store large amount of data and is 
   scalable to store even more data.
## Functions:
  This project includes the following functions:

  - **Lookup (int keyValue)**: find the specified value. If the value exists, returning value is True.
  - **Insert (int keyValue)**: insert the specified value.
  - **Display (int node)**: print out the indexing tree structure under specified node.
## B-tree Properties:
  -**Data type**: int is used for key value.

  -**NODESIZE**: The number of values a node can hold is >= 3 .Referencing from Introduction to Algorithms(P502)
     t is the minimum degree of the B-tree, and t>=2. Since each node can hold at most 2t-1 keys, NODESIZE >= 3 is required.

  -**Root**: The root node is the topmost node in the tree. 
## Algorithm Overview
- **Lookup:**
    - Begins at the root and traverses down the tree comparing the target value with the values in each node until it finds the target or reaches a leaf node.
    - if not a leaf node, it will continue to search in the child node.
- **Display:**
    - Recursively prints the values of each node starting from the root, showing the structure of the tree.
- **Insertion:**
    - Starts at the root and finds the correct position for the new value.
    - If a node is full (node size equals to `NODESIZE` values), it is split into two nodes and promote the middle value. 
    - if the parent node is full, it is split recursively. To avoid going back the tree, we split every full node on the way
       down to the tree. When we need to split a node, the parent node is not full. Inserting a new key into the B-tree will be one way from the root to a leaf node
- **Splitting:**
    - if a node is full, it is split into two nodes. and the middle value (or middle-left for even `NODESIZE`) is promoted to the parent node.
    - According to Introduction to Algorithms(P508), the split operation is as follows:
![Alt text](https://github.com/CarolineFang1998/CS7280_Project1_Group5/blob/master/Splitting.png)
   Nodesize is 7, there are 8 pointers and 7 keys. The middle key is 4, and the middle pointer is 5.
   After the split, the middle key is promoted to the parent node, and the middle pointer is the new child node.
- **Promotion:**
    - When a node is split, the middle value is promoted to the parent node. If the parent node is full, it is split recursively.
    - after the promotion, the new key is inserted to the child node.
- ![Alt text](https://github.com/CarolineFang1998/CS7280_Project1_Group5/blob/master/insert_2.png)
- **Summary**
  - insertion order: check if node is full, splitting-promote-insert
  - Split the tree when needed: tree grows in height only when absolutely necessary
  - Efficient Handling of Full Nodes: This is because it avoids the need to backtrack and split the node after finding it's full
  - Minimum disk access: split from root to leaf once. only o(h)time disk accesses. h is the height of the tree.
  - Handling duplicate keys: duplicate key values are not allowed in the B-tree.
  - Able to deal with both odd and even NODESIZE
  
   



## Limitations
- **Data Type:** This implementation is designed for integer values. Modifying the data type requires changes to the `Btree` class.
- **NODESIZE Configuration:** `NODESIZE` is a fixed value determining the maximum number of values a node can hold. This implementation may require manual adjustment of `NODESIZE` based on specific needs.
- **Large Datasets:** While B-trees are efficient for large datasets, extremely large datasets may require optimizations not covered in this basic implementation.
- **Concurrency:** This implementation does not include concurrency controls for multi-threaded scenarios.

## Customization

To customize `NODESIZE` or implement additional features, modify the `Btree` class accordingly. Ensure any changes maintain the B-tree properties to ensure performance and correctness.

## References

Introduction to Algorithms,4th Edition(Thomas H. Cormen, Charles E. Leiserson, Ronald L. Rivest, Clifford Stein),MIT Press,2009
https://dl.ebooksworld.ir/books/Introduction.to.Algorithms.4th.Leiserson.Stein.Rivest.Cormen.MIT.Press.9780262046305.EBooksWorld.ir.pdf
---
## Contributors
- Caroline Fang
- Luyan Deng
