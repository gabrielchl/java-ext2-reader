import java.util.*;
/**
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
**/
public class Inode {
    private Volume vol;
    private String filename;
    private int offset;

    private short file_mode;
    private short owner_user;
    private int file_size_l; // TODO
    private Date accessed;
    private Date created;
    private Date modified;
    private Date deleted;
    private short owner_group;
    private short num_hard_links;
    private DataBlock[] data_blocks = new DataBlock[12];
    private Inode ip;
    private Inode dip;
    private Inode tip;
    private int file_size_u; // TODO

    public Inode(Volume vol, int offset) {
        this.vol = vol;
        this.offset = offset;

        file_mode = vol.bb.getShort(offset);
        owner_user = vol.bb.getShort(offset + 2);
        file_size_l = vol.bb.getInt(offset + 4);
        accessed = new Date(vol.bb.getInt(offset + 8));
        created = new Date(vol.bb.getInt(offset + 12));
        modified = new Date(vol.bb.getInt(offset + 16));
        deleted = new Date(vol.bb.getInt(offset + 20));
        owner_group = vol.bb.getShort(offset + 24);
        num_hard_links = vol.bb.getShort(offset + 26);

        for (int i = 0; i < 12; i++) {
            if (vol.bb.getInt(offset + 40 + i * 4) == 0) {
                data_blocks[i] = null;
            } else {
                data_blocks[i] = new DataBlock(vol, this, vol.bb.getInt(offset + 40 + i * 4) * 1024);
            }
        }

        //ip = (vol.bb.getInt(offset + 88) == 0) ? null : new Inode(vol, vol.bb.getInt(2056) + (vol.bb.getInt(offset + 88) - 1) * 128); // TODO 1024 dynamic
        //dip = (vol.bb.getInt(offset + 92) == 0) ? null : new Inode(vol, vol.bb.getInt(2056) + (vol.bb.getInt(offset + 92) - 1) * 128); // TODO 1024 dynamic
        //tip = (vol.bb.getInt(offset + 96) == 0) ? null : new Inode(vol, vol.bb.getInt(2056) + (vol.bb.getInt(offset + 96) - 1) * 128); // TODO 1024 dynamic

        System.out.println("File mode:              " + file_perm_string(file_mode));
        System.out.println("Last access:            " + new Date(vol.bb.getInt(offset + 8)));
        System.out.println("Created:                " + new Date(vol.bb.getInt(offset + 12)));
        System.out.println("Last modified:          " + new Date(vol.bb.getInt(offset + 16)));
        System.out.println("Direct block pointer 0: " + vol.bb.getInt(offset + 40));
        System.out.println("Direct block pointer 1: " + vol.bb.getInt(offset + 44));
        System.out.println("Direct block pointer 2: " + vol.bb.getInt(offset + 48));
        System.out.println("File size1:             " + vol.bb.getInt(offset + 4));
        System.out.println("File size2:             " + vol.bb.getInt(offset + 108));
    }

    public String file_perm_string(short file_mode) {
        String file_perm_string = "";
        int[] file_perms = {0x0100, 0x0080, 0x0040, 0x0020, 0x0010, 0x0008, 0x0004, 0x0002, 0x0001};
        for (int i = 0; i < 3; i++) {
            file_perm_string += ((file_mode & file_perms[i * 3]) == file_perms[i * 3]) ? 'r' : '-';
            file_perm_string += ((file_mode & file_perms[i * 3 + 1]) == file_perms[i * 3 + 1]) ? 'w' : '-';
            file_perm_string += ((file_mode & file_perms[i * 3 + 2]) == file_perms[i * 3 + 2]) ? 'x' : '-';
        }
        return file_perm_string;
    }
}
