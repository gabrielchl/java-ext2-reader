import java.util.*;

/**
Inode contents
+--------+--------+--------------------------------------------------+
| Offset | Length | Description                                      |
| 0      | 2      | File mode                                        |
| 2      | 2      | User ID of owner (lower 16 bits)                 |
| 4      | 4      | File size in bytes (lower 32 bits)               |
| 8      | 4      | Last Access time                                 |
| 12     | 4      | Creation time                                    |
| 16     | 4      | Last Modified time                               |
| 20     | 4      | Deleted time                                     |
| 24     | 2      | Group ID of owner (lower 16 bits)                |
| 26     | 2      | Number of hard links referencing file            |
| 28     | 4      | Number of 512 byte Blocks in file                |
| 32     | 4      | Flags                                            |
| 36     | 4      | OS Dependent/ Reserved                           |
| 40     | 12 x 4 | Pointers to first 12 data blocks (block numbers) |
| 88     | 4      | Indirect pointer                                 |
| 92     | 4      | Double Indirect pointer                          |
| 96     | 4      | Triple Indirect pointer                          |
| 100    | 4      | Generation (for Network filesystem/ NFS)         |
| 104    | 4      | File Access Control List/ ACL                    |
| 108    | 4      | File size in bytes (upper 32 bits)               |
| 112    | 4      | Fragment address/ obsolete                       |
| 116    | 1      | Fragment number                                  |
| 117    | 1      | Fragment size                                    |
| 118    | 2      | Padding/ Reserved                                |
| 120    | 2      | User ID of owner (upper 16 bits)                 |
| 122    | 2      | Group ID of owner (upper 16 bits)                |
| 124    | 4      | Reserved                                         |
+--------+--------+--------------------------------------------------+

Directory entry Contents
+--------+--------+---------------------+
| Offset | Length | Description         |
| 0      | 4      | Inode               |
| 4      | 2      | Length              |
| 6      | 1      | Name length         |
| 7      | 1      | File type indicator |
| 8 + N  | N      | Name                |
+--------+--------+---------------------+
**/
public class Inode {
    Volume vol;
    int id;
    int offset;

    public Inode(Volume vol, int id, int offset) {
        this.vol = vol;
        this.id = id;
        this.offset = offset;
    }

    public Inode lookup(String filename) { // void for now
        int dir_entries_offset = vol.bb.getInt(offset + 40) * 1024;
        int dir_entry_pointer = 0;
        int dir_entry_len = 0;
        do {
            char[] current_filename = new char[vol.bb.get(dir_entries_offset + dir_entry_pointer + 6)];
            for (int i = 0; i < current_filename.length; i++) {
                current_filename[i] = (char)vol.bb.get(i + dir_entries_offset + dir_entry_pointer + 8);
            }
            if (filename.equals(new String(current_filename))) {
                return vol.get_inode(vol.bb.getShort(dir_entries_offset + dir_entry_pointer));
            }
            dir_entry_len = vol.bb.getShort(dir_entries_offset + dir_entry_pointer + 4);
            dir_entry_pointer += dir_entry_len;
        } while (dir_entry_len != 0 && dir_entry_pointer < 1024);
        return null;
    }

    public int datablock_pointer() {
        return vol.bb.getInt(offset + 40) * 1024;
    }

    public String filename_color() {
        String color_string = new String();
        if ((vol.bb.getShort(offset) & 0x4000) == 0x4000) {
            color_string += Main.BOLD_FONT + Main.BLUE_COL;
        }
        return color_string;
    }

    public short owner_user_id() {
        return vol.bb.getShort(offset + 2);
    }

    public short owner_group_id() {
        return vol.bb.getShort(offset + 24);
    }

    public short num_hard_links() {
        return vol.bb.getShort(offset + 26);
    }

    public Boolean is_directory() {
        if ((vol.bb.getShort(offset) & 0x4000) == 0x4000) {
            return true;
        }
        return false;
    }

    public int file_size() { // lower only
        return vol.bb.getInt(offset + 4);
    }

    public int[] stat() {
        int[] stat = {
            id,// inode num
            new Integer(vol.bb.getShort(offset)), // file mode
            new Integer(vol.bb.getShort(offset + 26)), // num hard links
            new Integer(vol.bb.getShort(offset + 2)), // uid
            new Integer(vol.bb.getShort(offset + 24)), // gid
            vol.bb.getInt(offset + 4), // size (lower)
            vol.bb.getInt(offset + 108), // size (upper)
            vol.bb.getInt(offset + 28), // num of 512 blocks allocated,
            vol.bb.getInt(offset + 8), // last access time
            vol.bb.getInt(offset + 12), // creation time
            vol.bb.getInt(offset + 16), // last modified time
        };
        return stat;
    }

    public Date last_modified() {
        return new Date(vol.bb.getInt(offset + 16));
    }

    //public void stat() {

    //}
}
