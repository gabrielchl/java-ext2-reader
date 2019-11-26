import java.util.*;
import java.text.*;

public class File {
    long position;

    Volume vol;
    int offset;
    String filename;
    LinkedList<String> path;
    Inode inode;

    public File(Volume vol, String filename, LinkedList<String> path, Inode inode) {
        position = 0;
        this.vol = vol;
        offset = inode.get_datablock_pt(0);
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

    public byte[] read(long offset, long length) {
        position += (offset > get_size() - position) ? get_size() - position : offset;
        return read(length);
    }

    public byte[] read(long length) {
        long return_length = (length > get_size() - position) ? get_size() - position : length;
        byte[] ret = new byte[(int)return_length];
        long temp_pos = position;
        int datablock_pt = inode.get_datablock_pt((int)(temp_pos / 1024));
        vol.bb.position(datablock_pt + (int)(temp_pos % 1024));
        for (long i = 0; i < return_length;) {
            if (temp_pos % 1024 == 0) {
                datablock_pt = inode.get_datablock_pt((int)(temp_pos / 1024)); // (int)temp_pos / 1024 = block # of the position in the file
                //System.out.println(datablock_pt);
                vol.bb.position(datablock_pt);
            }
            if (datablock_pt != 0) {
                ret[(int)i] = vol.bb.get();
            }
            i++;
            temp_pos++;
        }
        position += return_length;
        return ret;
    }

    public void seek(long position) {

    }

    public long get_position() {
        return position;
    }

    public long get_size() {
        int[] file_stat = get_inode().stat();
        return (long)file_stat[6] << 32 | file_stat[5];
    }

    public LinkedList<File> read_dir() {
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
