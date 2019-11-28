import java.util.*;
import java.text.*;

/**
 * A file in the filesystem.
 */
public class File {
    public long position;

    public final Volume vol;
    public final String filename;
    public final List<String> path;
    public final Inode inode;

    /**
     * Sets up the file object.
     *
     * @param   vol         The volume the file is in
     * @param   filename    The filename
     * @param   path        The absolute path
     * @param   inode       The inode for the file
     */
    public File(Volume vol, String filename, List<String> path, Inode inode) {
        position = 0;
        this.vol = vol;
        this.filename = filename;
        this.path = path;
        this.inode = inode;
    }

    /**
     * Returns path as a string.
     *
     * @return  Path as a string
     */
    public String get_path_string() {
        if (path.size() == 0) return "/";
        return "/" + String.join("/", path);
    }

    /**
     * Reads file from (offset) to (offset + length).
     *
     * @param   offset  Offset from 1st byte to read
     * @param   length  Number of bytes to read
     * @return  Byte array of the read file content
     */
    public byte[] read(long offset, long length) {
        position += (offset > get_size() - position) ? get_size() - position : offset;
        return read(length);
    }

    /**
     * Reads file from (position) to (position + length).
     *
     * @param   length  Number of bytes to read
     * @return  Byte array of the read file content
     */
    public byte[] read(long length) {
        long read_length = (length > get_size() - position) ? get_size() - position : length;
        byte[] content = new byte[(int)read_length];
        int datablock_pt = inode.get_datablock_pt((int)(position / Volume.BLOCK_LEN));
        vol.bb.position(datablock_pt + (int)(position % Volume.BLOCK_LEN));
        for (long i = 0; i < read_length;) {
            if (position % Volume.BLOCK_LEN == 0) {
                datablock_pt = inode.get_datablock_pt((int)(position / Volume.BLOCK_LEN));// (int)temp_pos / Volume.BLOCK_LEN = block # of the position in the file
                //System.out.println(datablock_pt);
                vol.bb.position(datablock_pt);
            }
            if (datablock_pt != 0) {
                content[(int)i] = vol.bb.get();
            }
            i++;
            position++;
        }
        return content;
    }

    /**
     * Gets the file size.
     *
     * @return  File size
     */
    public long get_size() {
        int[] file_stat = inode.stat();
        return (long)file_stat[6] << 32 | file_stat[5];
    }

    /**
     * Gets filenames in current directory.
     *
     * @return  A list of filenames in current directory.
     */
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
