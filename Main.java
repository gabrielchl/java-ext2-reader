import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.regex.*;

public class Main {
    public static final String BOLD_FONT = "\u001b[1m";
    public static final String GREY_COL = "\u001b[38;5;245m";
    public static final String BLUE_COL = "\u001b[38;5;110m";
    public static final String RESET = "\u001b[0m";

    public static void main(String[] args) {
        //Volume vol = new Volume('./ext2fs');
        Helper helper = new Helper();
        /**helper.dumpHexBytes("ABC".getBytes());
        System.out.println("");
        helper.dumpHexBytes("ABCDEFG#".getBytes());
        System.out.println("");
        helper.dumpHexBytes("ABCDEFG#HIJKL".getBytes());
        System.out.println("");
        helper.dumpHexBytes("ABCDEFG#HIJKLMN#".getBytes());
        System.out.println("");
        helper.dumpHexBytes("ABCDEFG#HIJKLMN#OP".getBytes());
        System.out.println("");
        helper.dumpHexBytes("ABCDEFG#HIJKLMN#OPQRSTU#".getBytes());
        System.out.println("");
        helper.dumpHexBytes("ABCDEFG#HIJKLMN#OPQRSTU#VW".getBytes());
        System.out.println("");
        helper.dumpHexBytes("ABCDEFG#HIJKLMN#OPQRSTU#VWXYZAA#".getBytes());**/

        /**try {
            RandomAccessFile vol = new RandomAccessFile("ext2fs", "r");
            int length = (int)vol.length();
            byte[] b = new byte[1124];
            vol.readFully(b);
            helper.dumpHexBytes(b);
        } catch (Exception e) {

        }**/

        Volume vol = new Volume("./ext2fs");
        //vol.get_root_inode().lookup(".").details();
        //vol.get_root_inode().lookup("deep").details();

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print(BOLD_FONT + BLUE_COL + "/ $ " + RESET);
            String[] input = scanner.nextLine().trim().split("[ ]+");
            String command = input[0];
            String[] argument = Arrays.copyOfRange(input, 1, input.length);

            switch (command) {
                case "help":
                case "h":
                    System.out.println("help");
                    System.out.println("h       print this help text");
                    System.out.println("df      print volume details");
                    break;
                case "df":
                case "vol-details":
                    vol.print_vol_details();
                    break;
                case "ls":
                    vol.get_cwd().get_inode().list();
                    //vol.get_cwd().get_inode().list_long();
                    break;
                case "cd":
                    Boolean cd_result = vol.change_dir(argument[0]);
                    if (cd_result == false) System.out.println("cd: " + argument[0] + ": No such file or directory");
                    break;
                case "quit":
                case "exit":
                    System.out.println("Good bye! :)");
                    System.exit(0);
                    break;
                default:
                    System.out.println(command + ": command not found");
                    break;
            }
        }
    }
}
