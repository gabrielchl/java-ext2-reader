import java.util.*;

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
