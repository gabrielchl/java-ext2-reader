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

    public String vol_filename;
    public Inode root_inode;

    public int num_blocks;
    public int num_inodes;
    public int blocks_per_gp;
    public int inodes_per_gp;
    public int inode_len;
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
            bb = vol_file.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, length);

            bb.order(ByteOrder.LITTLE_ENDIAN);

            //System.out.println("Magic Number: " + bb.getShort(1080));
            num_blocks = bb.getInt(1028);
            num_inodes = bb.getInt(1024);
            blocks_per_gp = bb.getInt(1056);
            inodes_per_gp = bb.getInt(1064);
            inode_len = bb.getInt(1112);

            /**
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
        int block_group_count = (int)Math.ceil(num_blocks / (double)bb.getInt(BLOCK_LEN + 32));
        int used_block_count = num_blocks;
        for (int i = 0; i < block_group_count; i++) {
            used_block_count -= bb.getShort(BLOCK_LEN + BLOCK_LEN + 32 * i + 12);
        }
        System.out.println("Used:       " + used_block_count);
        System.out.println("Use %:      " + (int)(used_block_count / (double)num_blocks * 100));
    }

    /**
     * Prints super block.
     */
    public void print_super_block() {
        System.out.println("Magic number:        " + Integer.toHexString(bb.getShort(BLOCK_LEN + 56)));
        System.out.println("# of inodes:         " + num_inodes);
        System.out.println("# of blocks:         " + num_blocks);
        System.out.println("Block size:          " + BLOCK_LEN);
        System.out.println("# of inodes / group: " + inodes_per_gp);
        System.out.println("# of blocks / group: " + blocks_per_gp);
        System.out.println("Size of each inode:  " + inode_len);
        char[] label = new char[16];
        for (int i = 0; i < 16; i++) {
            label[i] = (char)bb.get(BLOCK_LEN + 120 + i);
        }
        System.out.println("Volume label:        " + new String(label));
    }

    /**
     * Prints group description
     *
     * @param   group_desc_num  Group descriptor number
     */
    public void print_group_desc(int group_desc_num) {
        int offset = BLOCK_LEN + BLOCK_LEN + group_desc_num * 32;
        System.out.println("Block bitmap pointer:   " + bb.getInt(offset));
        System.out.println("Inode bitmap pointer:   " + bb.getInt(offset + 4));
        System.out.println("Inode table pointer:    " + bb.getInt(offset + 8));
        System.out.println("Free block count:       " + bb.getShort(offset + 12));
        System.out.println("Free inode count:       " + bb.getShort(offset + 14));
        System.out.println("Used directories count: " + bb.getShort(offset + 16));
    }

    /**
     * Prints inode
     *
     * @param   id  ID of the inode
     */
    public void print_inode(int id) {
        Inode inode = get_inode(id);
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
        return new Inode(this, id, inode_offset + inode_len * inode_num);
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
