import java.io.*;
import java.nio.*;

public class Ext2Reader {
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
    }
}
