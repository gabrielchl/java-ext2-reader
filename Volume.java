import java.io.*;
import java.nio.*;
import java.util.*;

public class Volume {
    private int num_blocks;
    private int num_inodes;
    private int blocks_per_gp;
    private int inodes_per_gp;
    private int inode_size;
    public ByteBuffer bb;
    /**
     * Opens the Volume represented by the host Windows / Linux file filename.
     *
     * @param   filename    Filename of the "volume file"
     */
    public Volume(String filename) {
        try {
            RandomAccessFile vol_file = new RandomAccessFile("ext2fs", "r");
            int length = (int)vol_file.length();
            byte[] bytes = new byte[length];
            vol_file.readFully(bytes);
            bb = ByteBuffer.wrap(bytes);

            byte[] test = new byte[86256];
            Helper helper = new Helper();
            bb.get(test);
            helper.dumpHexBytes(test);

            bb.order(ByteOrder.LITTLE_ENDIAN);

            System.out.println("Magic Number: " + bb.getShort(1080));
            num_blocks = bb.getInt(1028);
            System.out.println("# of blocks: " + num_blocks);
            num_inodes = bb.getInt(1024);
            System.out.println("# of inodes: " + num_inodes);
            System.out.println("Filesystem block size: " + bb.getInt(1048));
            blocks_per_gp = bb.getInt(1056);
            System.out.println("# of blocks per group: " + blocks_per_gp);
            inodes_per_gp = bb.getInt(1064);
            System.out.println("# of inodes per group: " + inodes_per_gp);
            inode_size = bb.getInt(1112);
            System.out.println("Size of each inode: " + inode_size);
            char[] label = new char[16];
            for (int i = 0; i < 16; i++) {
                label[i] = (char)bb.get(i + 1144);
            }
            System.out.println("Volume label: " + new String(label));

            System.out.println("-----------Block Group 1 Desc-----------");

            System.out.println("Block bitmap pointer: " + bb.getInt(2048));
            System.out.println("Inode bitmap pointer: " + bb.getInt(2052));
            System.out.println("Inode table pointer: " + bb.getInt(2056));
            System.out.println("Free block count: " + bb.getShort(2060));
            System.out.println("Free inode count: " + bb.getShort(2062));
            System.out.println("Used directories count: " + bb.getShort(2064));

            System.out.println("-----------------Inode 2----------------");

            Inode i2 = new Inode(this, 1024 * bb.getInt(2056) + 128);

            System.out.println("------------Inode 2 > Block 0-----------");

            System.out.println("Inode: " + bb.getInt(bb.getInt(1024 * 84 + 128 + 40) * 1024));
            System.out.println("Length: " + bb.getShort(bb.getInt(1024 * 84 + 128 + 40) * 1024 + 4));
            System.out.println("Name length: " + bb.get(bb.getInt(1024 * 84 + 128 + 40) * 1024 + 6));
            System.out.println("File type: " + bb.get(bb.getInt(1024 * 84 + 128 + 40) * 1024 + 7));
            char[] f_filename = new char[1];
            for (int i = 0; i < f_filename.length; i++) {
                f_filename[i] = (char)bb.get(i + bb.getInt(1024 * 84 + 128 + 40) * 1024 + 8);
            }
            System.out.println("Filename: " + new String(f_filename));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
