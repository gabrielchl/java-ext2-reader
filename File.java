import java.util.*;
import java.text.*;

public class File {
    public long position;

    public final Volume vol;
    public final String filename;
    public final List<String> path;
    public final Inode inode;

    public File(Volume vol, String filename, List<String> path, Inode inode) {
        position = 0;
        this.vol = vol;
        this.filename = filename;
        this.path = path;
        this.inode = inode;
    }

    public String get_path_string() {
        if (path.size() == 0) return "/";
        return "/" + String.join("/", path);
    }

    public byte[] read(long offset, long length) {
        position += (offset > get_size() - position) ? get_size() - position : offset;
        return read(length);
    }

    public byte[] read(long length) {
        long return_length = (length > get_size() - position) ? get_size() - position : length;
        byte[] ret = new byte[(int)return_length];
        long temp_pos = position;
        int datablock_pt = inode.get_datablock_pt((int)(temp_pos / Volume.BLOCK_LEN));
        vol.bb.position(datablock_pt + (int)(temp_pos % Volume.BLOCK_LEN));
        for (long i = 0; i < return_length;) {
            if (temp_pos % Volume.BLOCK_LEN == 0) {
                datablock_pt = inode.get_datablock_pt((int)(temp_pos / Volume.BLOCK_LEN));// (int)temp_pos / Volume.BLOCK_LEN = block # of the position in the file
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
        int[] file_stat = inode.stat();
        return (long)file_stat[6] << 32 | file_stat[5];
    }

    public List<String> read_dir() {
        List<String> filenames = new LinkedList<String>();

        for (int direntry_offset : inode.iter_direntries()) {
            int datablock_pt = inode.get_datablock_pt(direntry_offset / Volume.BLOCK_LEN);
            char[] current_filename = new char[vol.bb.get(datablock_pt + direntry_offset % Volume.BLOCK_LEN + 6)];

            for (int i = 0; i < current_filename.length; i++)
                current_filename[i] = (char)vol.bb.get(datablock_pt + direntry_offset % Volume.BLOCK_LEN + 8 + i);

            filenames.add(new String(current_filename));
        }

        return filenames;
    }
}
