import java.util.*;

public class DataBlock {
    private Volume vol;
    private Inode inode;
    private int offset;
    private LinkedList<DirEntry> dir_entries = new LinkedList<DirEntry>();

    public DataBlock(Volume vol, Inode inode, int offset) {
        this.vol = vol;
        this.inode = inode;
        this.offset = offset;

        int dir_entry_offset = 0;
        int dir_entry_len = 0;
        do {
            dir_entries.add(new DirEntry(vol, offset + dir_entry_offset));
            dir_entry_len = vol.bb.getShort(offset + dir_entry_offset + 4);
            dir_entry_offset += dir_entry_len;
        } while (dir_entry_len != 0 && dir_entry_offset < 1024);
    }
}
