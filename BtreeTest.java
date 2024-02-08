/*
 * CS7280 Special Topics in Database Management
 * Project 1: B-tree Test program.
 *
 * The test data will be changed when TA grades.
 * Thus, you need to test various cases.
 */
public final class BtreeTest {

  public static void main(String[] args) {
    System.out.println("*** B+tree Testing ***\n");

    /** Test simple string array. */
    // test(new int[] { 1, 2});
    // test(new int[] { 1, 2, 3, 4, 5, 6});
    // test(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 , 11, 12, 13, 14 , 15, 16, 17, 18, 19, 20});
    // test(new int[] { 20, 19, 18, 17, 16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1 });
    // test(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20 });
    test(new int[] { 10, 20, 30, 40, 50, 15, 60, 85, 95, 100, 11, 12, 13, 22, 32, 33, 34, 1, 2, 3, 4, 5, 6 });

    System.out.println("*** Finished Testing ***\n");
  }

  /* Initialize and test a B-tree with the specified values. */

  public static void test(int[] values) {
    // number of strings to be inserted
    int cntValues = values.length;

    System.out.println("Create B-tree with " + cntValues + " Values...\n");
    Btree tree = new Btree();

    System.out.println("Insert Values...");
    for(int v : values) {
      tree.Insert(v);
      tree.DisplayEntileBTree();
    }
      

    // int size = tree.CntValues();
    // System.out.println("Stored Nodes: " + size + "\n");

    // System.out.println("Finding Values...");
    // int found = 0;
    // for(int v : values) if(tree.Lookup(v)) found++;
    // System.out.println(found + " found, " + cntValues + " expected.\n");

    // System.out.println("Reinsert Values... ");
    // for(int v : values) tree.Insert(v);
    // System.out.println(tree.CntValues() + " stored, " + size + " expected.\n");

    // System.out.println("Finding Values...");
    // found = 0;
    // for(int v : values) if(tree.Lookup(v)) found++;
    // System.out.println(found + " found, " + cntValues + " expected.\n");


  }
}
