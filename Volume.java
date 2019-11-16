import java.io.*;
import java.nio.*;
import java.util.*;
import java.lang.ref.*;
import java.text.*;

public class Volume {
    private String vol_filename;
    private Inode root_inode;
    private File cwd;

    private int num_blocks;
    private int num_inodes;
    private int blocks_per_gp;
    private int inodes_per_gp;
    private int inode_size;
    public ByteBuffer bb;

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
            bb = ByteBuffer.wrap(bytes);

            /**byte[] test = new byte[8390670];
            Helper helper = new Helper();
            bb.get(test);
            helper.dumpHexBytes(test);**/

            bb.order(ByteOrder.LITTLE_ENDIAN);

            //System.out.println("Magic Number: " + bb.getShort(1080));
            num_blocks = bb.getInt(1028);
            System.out.println("# of blocks: " + num_blocks);
            num_inodes = bb.getInt(1024);
            System.out.println("# of inodes: " + num_inodes);
            //System.out.println("Filesystem block size: " + bb.getInt(1048));
            blocks_per_gp = bb.getInt(1056);
            System.out.println("# of blocks per group: " + blocks_per_gp);
            inodes_per_gp = bb.getInt(1064);
            System.out.println("# of inodes per group: " + inodes_per_gp);
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
            System.out.println("Inode table pointer: " + bb.getInt(2056));
            System.out.println("Free block count: " + bb.getShort(2060));
            System.out.println("Free inode count: " + bb.getShort(2062));
            System.out.println("Used directories count: " + bb.getShort(2064));**/
System.out.println("Inode table pointer: " + bb.getInt(2056));
            inode_table_pointer = bb.getInt(2056) * 1024;

            //System.out.println("-----------------Inode 2----------------");

            root_inode = new Inode(inode_table_pointer + 128); // inode 2
            cwd = new File("", root_inode);

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
            return new Inode(inode_offset + 128 * (inode_num - 1));
        } else if (inode_num == 13) {
            return new Inode(Volume.this.bb.getInt(inode_offset + 128 * 13) * 1024);
        } else if (inode_num == 14) {
            return new Inode(Volume.this.bb.getInt(Volume.this.bb.getInt(inode_offset + 128 * 14) * 1024) * 1024);
        } else if (inode_num == 15) {
            return new Inode(Volume.this.bb.getInt(Volume.this.bb.getInt(Volume.this.bb.getInt(inode_offset + 128 * 15) * 1024) * 1024) * 1024);
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

    public Boolean change_dir(String filename) {
        Inode lookup_result = cwd.get_inode().lookup(filename);
        if (lookup_result == null) return false;
        cwd = new File(filename, lookup_result);
        return true;
    }

    public class Inode {
        int offset;

        public Inode(int offset) {
            this.offset = offset;
        }

        public Inode lookup(String filename) { // void for now
            int dir_entries_offset = Volume.this.bb.getInt(offset + 40) * 1024;
            int dir_entry_pointer = 0;
            int dir_entry_len = 0;
            do {
                char[] current_filename = new char[Volume.this.bb.get(dir_entries_offset + dir_entry_pointer + 6)];
                for (int i = 0; i < current_filename.length; i++) {
                    current_filename[i] = (char)Volume.this.bb.get(i + dir_entries_offset + dir_entry_pointer + 8);
                }
                if (filename.equals(new String(current_filename))) {
                    return get_inode(Volume.this.bb.getShort(dir_entries_offset + dir_entry_pointer));
                }
                dir_entry_len = Volume.this.bb.getShort(dir_entries_offset + dir_entry_pointer + 4);
                dir_entry_pointer += dir_entry_len;
            } while (dir_entry_len != 0 && dir_entry_pointer < 1024);
            return null;
        }

        public void list() {
            int dir_entries_offset = Volume.this.bb.getInt(offset + 40) * 1024; // first datablock
            int dir_entry_pointer = 0;
            int dir_entry_len = 0;
            do {
                short num_hard_links = Volume.this.bb.getShort(offset + 26);
                char[] current_filename = new char[Volume.this.bb.get(dir_entries_offset + dir_entry_pointer + 6)];
                for (int i = 0; i < current_filename.length; i++) {
                    current_filename[i] = (char)Volume.this.bb.get(i + dir_entries_offset + dir_entry_pointer + 8);
                }
                System.out.print(new String(current_filename) + "  ");
                dir_entry_len = Volume.this.bb.getShort(dir_entries_offset + dir_entry_pointer + 4);
                dir_entry_pointer += dir_entry_len;
            } while (dir_entry_len != 0 && dir_entry_pointer < 1024);
            System.out.print("\n");
        }

        public void list_long() {
            int dir_entries_offset = Volume.this.bb.getInt(offset + 40) * 1024; // first datablock
            int dir_entry_pointer = 0;
            int dir_entry_len = 0;
            TablePrinter table = new TablePrinter();
            do {
                Inode current_inode = get_inode(Volume.this.bb.getInt(dir_entries_offset + dir_entry_pointer));
                SimpleDateFormat format = new SimpleDateFormat("MMM d HH:mm");
                char[] current_filename = new char[Volume.this.bb.get(dir_entries_offset + dir_entry_pointer + 6)];
                for (int i = 0; i < current_filename.length; i++) {
                    current_filename[i] = (char)Volume.this.bb.get(i + dir_entries_offset + dir_entry_pointer + 8);
                }
                String[] row = {
                    current_inode.file_perm_string(),
                    Short.toString(current_inode.num_hard_links()),
                    Short.toString(current_inode.owner_user_id()),
                    Short.toString(current_inode.owner_group_id()),
                    "SIZE HERE",
                    format.format(current_inode.last_modified()),
                    filename_color() + new String(current_filename) + Main.RESET
                };
                table.add_row(row);
                dir_entry_len = Volume.this.bb.getShort(dir_entries_offset + dir_entry_pointer + 4);
                dir_entry_pointer += dir_entry_len;
            } while (dir_entry_len != 0 && dir_entry_pointer < 1024);
            table.print_table();
        }

        public String filename_color() {
            String color_string = new String();
            if ((Volume.this.bb.getShort(offset) & 0x4000) == 0x4000) {
                color_string += Main.BOLD_FONT + Main.BLUE_COL;
            }
            return color_string;
        }

        public short owner_user_id() {
            return Volume.this.bb.getShort(offset + 2);
        }

        public short owner_group_id() {
            return Volume.this.bb.getShort(offset + 24);
        }

        public short num_hard_links() {
            return Volume.this.bb.getShort(offset + 26);
        }

        public String file_perm_string() {
            short file_mode = Volume.this.bb.getShort(offset);
            String file_perm_string = "";
            int[] file_types = {0xC000, 0xA000, 0x8000, 0x6000, 0x4000, 0x2000, 0x1000};
            char[] file_type_symbols = {'s', 'l', '-', 'b', 'd', 'c', 'p'};
            for (int i = 0; i < 7; i++) {
                if ((file_mode & file_types[i]) == file_types[i]) {
                    file_perm_string += file_type_symbols[i];
                }
            }
            int[] file_perms = {0x0100, 0x0080, 0x0040, 0x0020, 0x0010, 0x0008, 0x0004, 0x0002, 0x0001};
            for (int i = 0; i < 3; i++) {
                file_perm_string += ((file_mode & file_perms[i * 3]) == file_perms[i * 3]) ? 'r' : '-';
                file_perm_string += ((file_mode & file_perms[i * 3 + 1]) == file_perms[i * 3 + 1]) ? 'w' : '-';
                file_perm_string += ((file_mode & file_perms[i * 3 + 2]) == file_perms[i * 3 + 2]) ? 'x' : '-';
            }
            return file_perm_string;
        }

        public Date last_modified() {
            return new Date(Volume.this.bb.getInt(offset + 16));
        }

        //public void stat() {

        //}
    }

    public class File {
        String filename;
        LinkedList<String> path;
        Inode inode;

        public File(String filename, Inode inode) {
            this.filename = filename;
            this.inode = inode;
        }

        public Inode get_inode() {
            return inode;
        }
    }
}
