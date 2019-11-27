import java.util.*;
import java.lang.Math;
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
    public final Volume vol;
    public final int id;
    public final int offset;

    public Inode(Volume vol, int id, int offset) {
        this.vol = vol;
        this.id = id;
        this.offset = offset;
    }

    public Inode lookup(String filename) throws FileSystemException { // TODO not just look in 1 datablock
        for (int direntry_offset : iter_direntries()) {
            int datablock_pt = get_datablock_pt(direntry_offset / Volume.BLOCK_LEN);
            char[] current_filename = new char[vol.bb.get(datablock_pt + direntry_offset % Volume.BLOCK_LEN + 6)];

            for (int i = 0; i < current_filename.length; i++)
                current_filename[i] = (char)vol.bb.get(datablock_pt + direntry_offset % Volume.BLOCK_LEN + 8 + i);

            if (filename.equals(new String(current_filename)))
                return vol.get_inode(vol.bb.getInt(datablock_pt + direntry_offset % Volume.BLOCK_LEN));
        }

        throw new FileSystemException(2);
    }

    public int datablock_pointer() {
        return vol.bb.getInt(offset + 40) * 1024;
    }

    public String filename_color() {
        String color_string = new String();
        if (is_directory()) {
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

    public boolean is_directory() {
        return (vol.bb.getShort(offset) & 0xF000) == 0x4000;
    }

    public int get_datablock_pt(int datablock_id) {
        if (datablock_id < 12) {
            return vol.bb.getInt(offset + 40 + datablock_id * 4) * Volume.BLOCK_LEN;
        }

        datablock_id -= 12;
        datablock_id++;

        int block_size = 1;
        int level;
        for (level = 0; datablock_id >= block_size; level++) {
            datablock_id -= block_size;
            block_size *= 256;
        }

        int[] inds = new int[level];
        for (int i = 0; i < level; i++) {
            inds[level-i-1] = datablock_id % 256;
            datablock_id /= 256;
        }

        assert datablock_id == 0;

        datablock_id = vol.bb.getInt(offset + 40 + (11 + level) * 4) * Volume.BLOCK_LEN;

        for (int i = 0; i < level; i++) {
            if (datablock_id == 0)
                return datablock_id;

            datablock_id = vol.bb.getInt(datablock_id + inds[i] * 4) * Volume.BLOCK_LEN;
        }

        return datablock_id;
    }

    public int file_size() { // lower only
        return vol.bb.getInt(offset + 4);
    }

    public int[] stat() {
        int[] stat = {
            id,                           // inode num
            vol.bb.getShort(offset),      // file mode
            vol.bb.getShort(offset + 26), // num hard links
            vol.bb.getShort(offset + 2),  // uid
            vol.bb.getShort(offset + 24), // gid
            vol.bb.getInt(offset + 4),    // size (lower)
            vol.bb.getInt(offset + 108),  // size (upper)
            vol.bb.getInt(offset + 28),   // num of 512 blocks allocated,
            vol.bb.getInt(offset + 8),    // last access time
            vol.bb.getInt(offset + 12),   // creation time
            vol.bb.getInt(offset + 16),   // last modified time
        };
        return stat;
    }

    public Date last_modified() {
        return new Date(vol.bb.getInt(offset + 16));
    }

    public Iterable<Integer> iter_direntries() {
        return new DirEntryIterable();
    }

    public class DirEntryIterable implements Iterable<Integer> {
        public Iterator<Integer> iterator() {
            return new DirEntryIterator();
        }
    }

    public class DirEntryIterator implements Iterator<Integer> {
        private int direntry_offset;

        public DirEntryIterator() {
            direntry_offset = 0;

            valid_direntry();
        }

        public boolean hasNext() {
            return direntry_offset < file_size();
        }

        public Integer next() {
            if (!hasNext()) {
                throw new NoSuchElementException("There are no more directory elements.");
            } else {
                int old_direntry_offset = direntry_offset;

                next_direntry();
                valid_direntry();

                return old_direntry_offset;
            }
        }

        private void valid_direntry() {
            while (hasNext()) {
                int datablock_pt = get_datablock_pt(direntry_offset / Volume.BLOCK_LEN);
                int inode = vol.bb.getInt(datablock_pt + direntry_offset % Volume.BLOCK_LEN);
                if (inode != 0)
                    break;
                next_direntry();
            }
        }

        private void next_direntry() {
            int datablock_pt = get_datablock_pt(direntry_offset / Volume.BLOCK_LEN);
            direntry_offset += vol.bb.getShort(datablock_pt + direntry_offset % Volume.BLOCK_LEN + 4);
        }
    }
}
