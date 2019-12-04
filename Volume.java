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
    public final int BLOCK_LEN = 1024;
    public final int DATABLOCK_PT_LEN = 4;

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
            RandomAccessFile vol_file = new RandomAccessFile(vol_filename, "r");
            int length = (int)vol_file.length();
            bb = vol_file.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, length);
        } catch (Exception e) {
            e.printStackTrace();
        }
        bb.order(ByteOrder.LITTLE_ENDIAN);

        num_blocks = bb.getInt(1028);
        num_inodes = bb.getInt(1024);
        blocks_per_gp = bb.getInt(1056);
        inodes_per_gp = bb.getInt(1064);
        inode_len = bb.getInt(1112);
    }

    /**
     * Prints volume details.
     * Similar to command df in linux.
     */
    public void print_vol_details() {
        System.out.println("Filesystem: " + vol_filename);
        System.out.println(" 1K-blocks: " + num_blocks);
        int block_group_count = (int)Math.ceil(num_blocks / (double)bb.getInt(BLOCK_LEN + 32));
        int used_block_count = num_blocks;
        for (int i = 0; i < block_group_count; i++) {
            used_block_count -= bb.getShort(BLOCK_LEN + BLOCK_LEN + 32 * i + 12);
        }
        System.out.println("      Used: " + used_block_count);
        System.out.println("     Use %: " + (int)(used_block_count / (double)num_blocks * 100));
    }

    /**
     * Prints super block.
     */
    public void print_super_block() {
        System.out.println("       Magic number: " + Integer.toHexString(bb.getShort(BLOCK_LEN + 56)));
        System.out.println("        # of inodes: " + num_inodes);
        System.out.println("        # of blocks: " + num_blocks);
        System.out.println("         Block size: " + BLOCK_LEN);
        System.out.println("# of inodes / group: " + inodes_per_gp);
        System.out.println("# of blocks / group: " + blocks_per_gp);
        System.out.println(" Size of each inode: " + inode_len);
        char[] label = new char[16];
        for (int i = 0; i < 16; i++) {
            label[i] = (char)bb.get(BLOCK_LEN + 120 + i);
        }
        System.out.println("       Volume label: " + new String(label));
    }

    /**
     * Prints group description
     *
     * @param   group_desc_num  Group descriptor number
     */
    public void print_group_desc(int group_desc_num) {
        int offset = BLOCK_LEN + BLOCK_LEN + group_desc_num * 32;
        System.out.println("  Block bitmap pointer: " + bb.getInt(offset));
        System.out.println("  Inode bitmap pointer: " + bb.getInt(offset + 4));
        System.out.println("   Inode table pointer: " + bb.getInt(offset + 8));
        System.out.println("      Free block count: " + bb.getShort(offset + 12));
        System.out.println("      Free inode count: " + bb.getShort(offset + 14));
        System.out.println("Used directories count: " + bb.getShort(offset + 16));
    }

    /**
     * Prints inode
     *
     * @param   id  ID of the inode
     */
    public void print_inode(int id) {
        Inode inode = get_inode(id);
        int offset = inode.offset;
        System.out.println("                               File mode: " + bb.getShort(offset));
        System.out.println("        User ID of owner (lower 16 bits): " + bb.getShort(offset + 2));
        System.out.println("      File size in bytes (lower 32 bits): " + bb.getInt(offset + 4));
        System.out.println("                        Last Access time: " + bb.getInt(offset + 8));
        System.out.println("                           Creation time: " + bb.getInt(offset + 12));
        System.out.println("                      Last Modified time: " + bb.getInt(offset + 16));
        System.out.println("                            Deleted time: " + bb.getInt(offset + 20));
        System.out.println("       Group ID of owner (lower 16 bits): " + bb.getShort(offset + 24));
        System.out.println("   Number of hard links referencing file: " + bb.getShort(offset + 26));
        System.out.println("       Number of 512 byte Blocks in file: " + bb.getInt(offset + 28));
        System.out.println("                                   Flags: " + bb.getInt(offset + 32));
        System.out.println("                  OS Dependent/ Reserved: " + bb.getInt(offset + 36));
        System.out.println("                 Pointers to datablock 1: " + bb.getInt(offset + 40));
        System.out.println("                 Pointers to datablock 2: " + bb.getInt(offset + 44));
        System.out.println("                 Pointers to datablock 3: " + bb.getInt(offset + 48));
        System.out.println("                 Pointers to datablock 4: " + bb.getInt(offset + 52));
        System.out.println("                 Pointers to datablock 5: " + bb.getInt(offset + 56));
        System.out.println("                 Pointers to datablock 6: " + bb.getInt(offset + 60));
        System.out.println("                 Pointers to datablock 7: " + bb.getInt(offset + 64));
        System.out.println("                 Pointers to datablock 8: " + bb.getInt(offset + 68));
        System.out.println("                 Pointers to datablock 9: " + bb.getInt(offset + 72));
        System.out.println("                Pointers to datablock 10: " + bb.getInt(offset + 76));
        System.out.println("                Pointers to datablock 11: " + bb.getInt(offset + 80));
        System.out.println("                Pointers to datablock 12: " + bb.getInt(offset + 84));
        System.out.println("                        Indirect pointer: " + bb.getInt(offset + 88));
        System.out.println("                 Double Indirect pointer: " + bb.getInt(offset + 92));
        System.out.println("                 Triple Indirect pointer: " + bb.getInt(offset + 96));
        System.out.println("Generation (for Network filesystem/ NFS): " + bb.getInt(offset + 100));
        System.out.println("           File Access Control List/ ACL: " + bb.getInt(offset + 104));
        System.out.println("      File size in bytes (upper 32 bits): " + bb.getInt(offset + 108));
        System.out.println("              Fragment address/ obsolete: " + bb.getInt(offset + 112));
        System.out.println("                         Fragment number: " + bb.get(offset + 116));
        System.out.println("                           Fragment size: " + bb.get(offset + 117));
        System.out.println("                       Padding/ Reserved: " + bb.getShort(offset + 118));
        System.out.println("        User ID of owner (upper 16 bits): " + bb.getShort(offset + 120));
        System.out.println("       Group ID of owner (upper 16 bits): " + bb.getShort(offset + 122));
        System.out.println("                                Reserved: " + bb.getInt(offset + 124));
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
