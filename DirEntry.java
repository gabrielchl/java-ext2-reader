/**
+--------+--------+---------------------+
| Offset | Length | Description         |
| 0      | 4      | Inode               |
| 4      | 2      | Length              |
| 6      | 1      | Name length         |
| 7      | 1      | File type indicator |
| 8 + N  | N      | Name                |
+--------+--------+---------------------+
**/
public class DirEntry {
    private Inode inode;
    private short name_len;
    private short file_type;
    private char[] filename;
    public DirEntry(Volume vol, int offset) {
        //System.out.println(vol.bb.get(offset));
        inode = vol.get_inode(vol.bb.get(offset));
        name_len = vol.bb.get(offset + 6);
        file_type = vol.bb.get(offset + 7);
        char[] filename = new char[vol.bb.get(offset + 6)];
        for (int i = 0; i < filename.length; i++) {
            filename[i] = (char)vol.bb.get(i + offset + 8);
        }
        System.out.println("Filename: " + new String(filename));
    }
}
