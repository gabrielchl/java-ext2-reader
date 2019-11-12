import java.io.*;
import java.nio.*;

public class Ext2Reader {
    public static void main(String[] args) {
        //Volume vol = new Volume('./ext2fs');
        Helper helper = new Helper();
        helper.dumpHexBytes("ABCDEFGH IJKLMNO PQRST".getBytes());
    }
}
