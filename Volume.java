import java.io.*;
import java.nio.*;
import java.util.*;
import java.lang.ref.*;
import java.text.*;
import java.nio.channels.FileChannel;

/**
 * The filesystem as a volume.
 */
public class Volume {
    public static final int BLOCK_LEN = 1024;
    public static final int DATABLOCK_PT_LEN = 4;
    public static final int INODE_LEN = 128;

    private String vol_filename;
    private Inode root_inode;

    private int num_blocks;
    private int num_inodes;
    private int blocks_per_gp;
    private int inodes_per_gp;
    private int inode_size;
    public MappedByteBuffer bb;

    /**
     * Opens the Volume represented by the host Windows / Linux file filename.
     *
     * @param   filename    Filename of the "volume file"
     */
    public Volume(String filename) {
        vol_filename = filename;
        try {
            RandomAccessFile vol_file = new RandomAccessFile("ext2fs", "r");
            int length = (int)vol_file.length();
            byte[] bytes = new byte[length];
            vol_file.readFully(bytes);
            bb = vol_file.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, length);

            /**byte[] test = new byte[8390670];
            Helper helper = new Helper();
            bb.get(test);
            helper.dumpHexBytes(test);**/

            bb.order(ByteOrder.LITTLE_ENDIAN);

            //System.out.println("Magic Number: " + bb.getShort(1080));
            num_blocks = bb.getInt(1028);
            //System.out.println("# of blocks: " + num_blocks);
            num_inodes = bb.getInt(1024);
            //System.out.println("# of inodes: " + num_inodes);
            //System.out.println("Filesystem block size: " + bb.getInt(1048));
            blocks_per_gp = bb.getInt(1056);
            //System.out.println("# of blocks per group: " + blocks_per_gp);
            inodes_per_gp = bb.getInt(1064);
            //System.out.println("# of inodes per group: " + inodes_per_gp);
            inode_size = bb.getInt(1112);
            //System.out.println("Size of each inode: " + inode_size);
            char[] label = new char[16];
            for (int i = 0; i < 16; i++) {
                label[i] = (char)bb.get(i + 1144);
            }
            /**System.out.println("Volume label: " + new String(label));

            System.out.println("-----------Block Group 1 Desc-----------");

            System.out.println("Block bitmap pointer: " + bb.getInt(2048));
            System.out.println("Inode bitmap pointer: " + bb.getInt(2052));

            System.out.println("Free block count: " + bb.getShort(2060));
            System.out.println("Free inode count: " + bb.getShort(2062));
            System.out.println("Used directories count: " + bb.getShort(2064));**/
            //System.out.println("Inode table pointer: " + bb.getInt(2056));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Prints volume details.
     * Similar to command df in linux.
     */
    public void print_vol_details() {
        System.out.println("Filesystem: " + vol_filename);
        System.out.println("1K-blocks:  " + num_blocks);
        System.out.println("Used:       "); // TODO
        System.out.println("Use %:      "); // TODO;
    }

    /**
     * Gets inode from inode number.
     *
     * @param   id  Inode number
     * @return  Inode of id: id
     */
    public Inode get_inode(int id) {
        int block_group_num = (id - 1) / 1712;
        int inode_num = (id - 1) % 1712;
        int inode_offset = BLOCK_LEN + BLOCK_LEN + 32 * block_group_num + 8;
        inode_offset = bb.getInt(inode_offset) * BLOCK_LEN;
        return new Inode(this, id, inode_offset + INODE_LEN * inode_num);
    }

    /**
     * Gets the root inode.
     *
     * @return  The root inode
     */
    public Inode get_root_inode() {
        return get_inode(2);
    }
}
