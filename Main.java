import java.io.*;
import java.nio.*;
import java.util.*;

public class Main {
    public static final String BOLD = "\u001b[1m";
    public static final String GREY_COL = "\u001b[38;5;245m";
    public static final String BLUE_COL = "\u001b[38;5;39m";
    public static final String RESET_COL = "\u001b[0m";

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

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print(BOLD + BLUE_COL + "/ $ " + RESET_COL);
            String command = scanner.nextLine();
            switch (command) {
                case "help":
                case "h":
                    System.out.println("help");
                    System.out.println("h       print this help text");
                    System.out.println("df      print volume details");
                    break;
                case "df":
                    vol.print_vol_details();
                    break;
                case "exit":
                    System.out.println("Good bye! :)");
                    System.exit(0);
                    break;
                default:
                    break;
            }
        }
    }
}
