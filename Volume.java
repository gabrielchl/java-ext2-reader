import java.io.*;
import java.nio.*;
import java.util.*;
import java.lang.ref.*;
import java.text.*;
import java.nio.channels.FileChannel;

public class Volume {
    public static final int BLOCK_LEN = 1024;
    public static final int DATABLOCK_PT_LEN = 4;

    private String vol_filename;
    private Inode root_inode;
    private File cwd;

    private int num_blocks;
    private int num_inodes;
    private int blocks_per_gp;
    private int inodes_per_gp;
    private int inode_size;
    public MappedByteBuffer bb;

    private int inode_table_pointer; // TODO temp till i get what a block group is
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
            inode_table_pointer = bb.getInt(2056) * 1024; // in bytes

            //System.out.println("-----------------Inode 2----------------");

            root_inode = new Inode(this, 2, inode_table_pointer + 128); // inode 2
            LinkedList<String> root_path = new LinkedList<String>(Arrays.asList(""));
            cwd = new File(this, "", root_path, root_inode);

            /**System.out.println("------------Inode 2 > Block 0-----------");

            System.out.println("Inode: " + bb.getInt(bb.getInt(1024 * 84 + 128 + 40) * 1024));
            System.out.println("Length: " + bb.getShort(bb.getInt(1024 * 84 + 128 + 40) * 1024 + 4));
            System.out.println("Name length: " + bb.get(bb.getInt(1024 * 84 + 128 + 40) * 1024 + 6));
            System.out.println("File type: " + bb.get(bb.getInt(1024 * 84 + 128 + 40) * 1024 + 7));**/
            char[] f_filename = new char[1];
            for (int i = 0; i < f_filename.length; i++) {
                f_filename[i] = (char)bb.get(i + bb.getInt(1024 * 84 + 128 + 40) * 1024 + 8);
            }
            //System.out.println("Filename: " + new String(f_filename));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void print_vol_details() {
        System.out.println("Filesystem: " + vol_filename);
        System.out.println("1K-blocks:  " + num_blocks);
        System.out.println("Used:       "); // TODO
        System.out.println("Use %:      "); // TODO;
    }

    public Inode get_inode(int id) {
        int block_group_num = id / 1712;
        int inode_num = id % 1712;
        int inode_offset = 1024 + 1024 + 32 * block_group_num + 8; // boot block + super block + group descriptors + offset to inode table pointer
        inode_offset = bb.getInt(inode_offset) * 1024;
        if (inode_num < 13) {
            return new Inode(this, id, inode_offset + 128 * (inode_num - 1));
        } else if (inode_num == 13) { // 13, 14, 15 is probably not right, not very clear about how indirect inode works currently
            return new Inode(this, id, Volume.this.bb.getInt(inode_offset + 128 * 13) * 1024);
        } else if (inode_num == 14) {
            return new Inode(this, id, Volume.this.bb.getInt(Volume.this.bb.getInt(inode_offset + 128 * 14) * 1024) * 1024);
        } else if (inode_num == 15) {
            return new Inode(this, id, Volume.this.bb.getInt(Volume.this.bb.getInt(Volume.this.bb.getInt(inode_offset + 128 * 15) * 1024) * 1024) * 1024);
        }
        return null;
        //indirect inode # = 1024 / 4
    }

    public Inode get_root_inode() {
        return get_inode(2);
    }

    public File get_cwd() {
        return cwd;
    }

    public void change_dir(String filename) {
        try {
            Inode lookup_result = cwd.get_inode().lookup(filename);
            if (!lookup_result.is_directory()) {
                throw new FileSystemException(20);
            }
            LinkedList<String> path = cwd.get_path();
            switch (filename) {
                case ".":
                    break;
                case "..":
                    if (path.size() != 1) path.removeLast();
                    break;
                default:
                    path.add(filename);
                    break;
            }
            cwd = new File(this, filename, path, lookup_result);
        } catch (FileSystemException e) {
            e.print_err_msg("stat: " + filename);
        }
    }
}
