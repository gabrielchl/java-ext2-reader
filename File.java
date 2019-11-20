import java.util.*;
import java.text.*;

public class File {
    Volume vol;
    int offset;
    String filename;
    LinkedList<String> path;
    Inode inode;

    public File(Volume vol, String filename, LinkedList<String> path, Inode inode) {
        this.vol = vol;
        offset = inode.datablock_pointer();
        this.filename = filename;
        this.path = path;
        this.inode = inode;
    }

    public LinkedList<String> get_path() {
        return path;
    }

    public String get_path_string() {
        if (path.size() == 1) return "/";
        return String.join("/", path);
    }

    public Inode get_inode() {
        return inode;
    }

    public String get_filename() {
        return filename;
    }

    public LinkedList<File> read_dir() { // WRONG INODE FOR NOW
        LinkedList<File> dir_files = new LinkedList<File>();
        int dir_entry_pointer = 0;
        int dir_entry_len = 0;
        do {
            char[] current_filename = new char[vol.bb.get(offset + dir_entry_pointer + 6)];
            for (int i = 0; i < current_filename.length; i++) {
                current_filename[i] = (char)vol.bb.get(i + offset + dir_entry_pointer + 8);
            }
            dir_files.add(new File(vol, new String(current_filename), path, vol.get_inode(vol.bb.getInt(offset + dir_entry_pointer))));
            dir_entry_len = vol.bb.getShort(offset + dir_entry_pointer + 4);
            dir_entry_pointer += dir_entry_len;
        } while (dir_entry_len != 0 && dir_entry_pointer < 1024);
        return dir_files;
    }
}
