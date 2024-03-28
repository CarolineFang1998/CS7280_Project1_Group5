import java.util.Scanner;


/*
 * CS7280 Special Topics in Database Management
 * Project 1: B-tree Test program.
 *
 * The test data will be changed when TA grades.
 * Thus, you need to test various cases.
 */
/**
 * 
 * Test simple string array.
 *
 * int[] values = new int[] { 1, 2};
 *
 * int[] values = new int[] { 1, 2, 3, 4, 5, 6};
 *
 * int[] values = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 , 11, 12, 13, 14 , 15, 16, 17, 18, 19, 20};
 *
 * int[] values = new int[] { 20, 19, 18, 17, 16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1 };
 *
 * int[] values = new int[] { 10, 20, 30, 40, 50, 15, 60, 85, 95, 100, 11, 12, 13, 22, 32, 33, 34, 1, 2, 3, 4, 5, 6 };
 *
 * duplicated
 * int[] values = new int[] { 1, 2, 3, 3, 4, 5 };
 *
 * large number of values, e.g., 1 to 1000
 * int[] values = new int[1000];
 * for (int i = 0; i < values.length; i++) {
 *   values[i] = i + 1;
 * }
 *
 * int[] values = new int[] { 10, 20, 30, 40, 50, 60 };
 */

public final class BtreeTest {

    public static void main(String[] args) {
        System.out.println("*** B+tree Testing ***\n");
        Scanner scanner = new Scanner(System.in); // Create a Scanner object
        Btree tree = new Btree(); // Initialize your B-tree here

        // TODO: Input your array here
        int[] values = new int[] {29,41,44,62,46,49,27,76,91,30,100,47,34,53,9,45};

        System.out.println("Insert Values...");
        for(int v : values) {
          tree.Insert(v);
          // Uncomment when you want to see the step-by-step insert..
          // tree.DisplayEntileBTree();
        }
        tree.DisplayEntileBTree();

        boolean running = true;

        while (running) {
            System.out.println("(1) Look-up, (2) Insert, or (q) Quit");
            String choice = scanner.nextLine(); // Read user input

            switch (choice) {
                case "1": // Look-up
                    System.out.println("Which key are you searching?");
                    int searchKey = Integer.parseInt(scanner.nextLine()); // Read the key to search
                    tree.Lookup(searchKey); // Assume Lookup returns boolean
                    break;
                case "2": // Insert
                    System.out.println("Enter Key?");
                    int insertKey = Integer.parseInt(scanner.nextLine()); // Read the key to insert
                    tree.Insert(insertKey); // Insert the key into the tree
                    tree.DisplayEntileBTree(); // Display the tree
                    break;
                case "q": // Quit
                    running = false;
                    break;
                default:
                    System.out.println("Invalid option. Please enter 1, 2, or q.");
                    break;
            }
        }

        scanner.close(); // Close the scanner
        System.out.println("*** Finished Testing ***\n");
    }
}
