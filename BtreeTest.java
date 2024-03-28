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
//        int[] values = new int[] {29,41,44,62,46,49,27,76,91,30,100,47,34,53,9,45};
        KeyPointer[] keyPointers = new KeyPointer [] {
                new KeyPointer(29, "0000001"),
                new KeyPointer(11, "0000002"),
                new KeyPointer(44, "0000003"),
                new KeyPointer(99, "0000004"),
                new KeyPointer(46, "0000005"),
                new KeyPointer(49, "0000006"),
                new KeyPointer(27, "0000007"),
                new KeyPointer(76, "0000008"),
                new KeyPointer(91, "0000009"),
                new KeyPointer(30, "0000010"),
                new KeyPointer(100, "0000011"),
                new KeyPointer(47, "0000012"),
                new KeyPointer(34, "0000013"),
                new KeyPointer(53, "0000014"),
                new KeyPointer(9, "0000015"),
                new KeyPointer(45, "0000016"),
                new KeyPointer(1, "0000017"),
                new KeyPointer(5, "0000018"),
                new KeyPointer(3, "0000019"),
                new KeyPointer(4, "0000020"),
                new KeyPointer(5, "0000021"),
                new KeyPointer(6, "0000022"),
                new KeyPointer(70, "0000023"),
                new KeyPointer(8, "0000024"),
                new KeyPointer(10, "0000025"), // Skipping 9 as it's already used in your example
                new KeyPointer(20, "0000026"), // Skipping 11
                new KeyPointer(13, "0000027"),
                new KeyPointer(14, "0000028"),
                new KeyPointer(60, "0000029"),
                new KeyPointer(16, "0000030"),
                new KeyPointer(17, "0000031"),
                new KeyPointer(18, "0000032")

        };

        System.out.println("Insert Values...");
        for(KeyPointer v : keyPointers) {
          tree.Insert(v);
          // Uncomment when you want to see the step-by-step insert..
           tree.DisplayEntileBTree();
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
//                    tree.Insert(insertKey); // Insert the key into the tree
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
