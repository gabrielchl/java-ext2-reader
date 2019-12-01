import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.regex.*;
import java.text.*;

/**
 * Main class of the ext2 file system reader.
 */
public class Main {
    public static final String BOLD_FONT = "\u001b[1m";
    public static final String LIGHT_GREY_COL = "\u001b[38;5;248m";
    public static final String GREY_COL = "\u001b[38;5;245m";
    public static final String BLUE_COL = "\u001b[38;5;110m";
    public static final String RESET = "\u001b[0m";

    private Volume vol;
    private File cwd;

    /**
     * Creates the main object.
     */
    public static void main(String[] args) {
        new Main();
    }

    /**
     * The main object.
     */
    public Main() {
        vol = new Volume("./ext2fs");
        cwd = new File(vol, "", new LinkedList<String>(), vol.get_root_inode());

        Scanner scanner = new Scanner(System.in);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.out.println("\n");
            }
        });

        while (true) {
            System.out.print(BOLD_FONT + BLUE_COL + cwd.get_path_string() + " $ " + RESET);
            String[] input = scanner.nextLine().trim().split("[ ]+");
            String command = input[0];
            String[] arguments = Arrays.copyOfRange(input, 1, input.length);

            try {
                switch (command) {
                    case "help":
                    case "h":
                        System.out.println("help");
                        System.out.println("h                 print this help text");
                        System.out.println("print-super-block");
                        System.out.println("psb               print super block contents");
                        System.out.println("print-group-desc <group_id>");
                        System.out.println("pgd <group_id>    print group description contents");
                        System.out.println("print-inode <inode_id>");
                        System.out.println("pi <inode_id>     print group description contents");
                        System.out.println("print-dir-entries");
                        System.out.println("pde               print directory entries");
                        System.out.println("vol-details");
                        System.out.println("df                print volume details");
                        System.out.println("pwd               print current path");
                        System.out.println("ls <filename>     print current path");
                        System.out.println("stat <filename>   print current path");
                        System.out.println("cd <filename>     print current path");
                        System.out.println("cat <filename>    print current path");
                        System.out.println("head -c <num_bytes> <filename>   print current path");
                        System.out.println("tail -c <num_bytes> <filename>   print current path");
                        break;
                    case "print-super-block":
                    case "psb":
                        vol.print_super_block();
                        break;
                    case "print-group-desc":
                    case "pgd":
                        if (arguments.length >= 1) {
                            vol.print_group_desc(Integer.parseInt(arguments[0]));
                        }
                        break;
                    case "print-inode":
                    case "pi":
                        if (arguments.length >= 1) {
                            vol.print_inode(Integer.parseInt(arguments[0]));
                        } else {
                            vol.print_inode(cwd.inode.id);
                        }
                        break;
                    case "print-dir-entries":
                    case "pde":
                        cmd_pde();
                        break;
                    case "df":
                    case "vol-details":
                        vol.print_vol_details();
                        break;
                    case "pwd":
                        System.out.println(cwd.get_path_string());
                        break;
                    case "ls":
                        cmd_ls(command, arguments);
                        break;
                    case "stat":
                        if (arguments.length >= 1) {
                            cmd_stat(command, arguments);
                        } else {
                            System.out.println("stat: missing operand");
                        }
                        break;
                    case "cd":
                        if (arguments.length >= 1) {
                            cmd_cd(command, arguments);
                        }
                        break;
                    case "cat":
                        if (arguments.length >= 1) {
                            cmd_cat(command, arguments);
                        }
                        break;
                    case "head":
                        if (arguments.length >= 1) {
                            cmd_head(command, arguments);
                        }
                        break;
                    case "tail":
                        if (arguments.length >= 1) {
                            cmd_tail(command, arguments);
                        }
                        break;
                    case "quit":
                    case "exit":
                        System.exit(0);
                        break;
                    default:
                        System.out.println(command + ": command not found");
                        break;
                }
            } catch (FileSystemException e) {
                e.print_err_msg(command);
            }
        }
    }

    /**
     * Change directory (cd) command handler.
     *
     * @param   command     The command used
     * @param   arguments   Arguments provided
     */
    public void cmd_cd(String command, String[] arguments) throws FileSystemException {
        String target_filename = arguments[0];
        try {
            cwd = open(target_filename, true);
        } catch (FileSystemException e) {
            e.name = target_filename;
            throw e;
        }
    }

    /**
     * List directory (ls) command handler.
     *
     * @param   command     The command used
     * @param   arguments   Arguments provided
     */
    public void cmd_ls(String command, String[] arguments) throws FileSystemException { // TODO parse path
        if (Arrays.asList(arguments).contains("-l")) {
            TablePrinter table = new TablePrinter();
            File file;
            if (arguments.length < 2) {
                file = cwd;
            } else {
                file = open(arguments[1], true);
            }
            for (String filename : file.read_dir()) {
                List<String> file_path = new LinkedList<String>(file.path);
                file_path.add(filename);
                int[] file_stat = path_to_inode(file_path).stat();
                String[] row = {
                    file_perm_string(file_stat[1]), // rwxrwxrwx
                    Integer.toString(file_stat[2]), // num hard links
                    Integer.toString(file_stat[3]), // uid
                    Integer.toString(file_stat[4]), // gid
                    Long.toString((long)file_stat[6] << 32 | file_stat[5]), // size
                    format_date(file_stat[10], "MMM d HH:mm"), // last modify time
                    "" // filename
                };
                if (path_to_inode(file_path).is_directory()) {
                    row[6] = BOLD_FONT + BLUE_COL + filename + RESET + " ";
                } else {
                    row[6] = filename + " ";
                }
                table.add_row(row);
            }
            table.print_table();
        } else {
            File file;
            if (arguments.length == 0) {
                file = cwd;
            } else {
                file = open(arguments[0], true);
            }
            for (String filename : file.read_dir()) {
                List<String> file_path = new LinkedList<String>(file.path);
                file_path.add(filename);
                if (path_to_inode(file_path).is_directory()) {
                    System.out.print(BOLD_FONT + BLUE_COL + filename + RESET + " ");
                } else {
                    System.out.print(filename + " ");
                }
            }
            System.out.print("\n");
        }
    }

    /**
     * Stat (stat) command handler.
     *
     * @param   command     The command used
     * @param   arguments   Arguments provided
     */
    public void cmd_stat(String command, String[] arguments) throws FileSystemException {
        String target_filename = arguments[0];
        try {
            Inode target_inode = path_to_inode(realpath(target_filename));
            int[] stat = target_inode.stat();

            System.out.println("  File: " + target_filename);

            TablePrinter table_printer = new TablePrinter();
            long file_size = stat[6] << 32 | stat[5];
            table_printer.add_row(new String[]{
                "  Size: " + file_size + " ",
                "Blocks: " + stat[7] + " ",
                "IO Block: 1024 ",
                file_type_string(stat[1])
            });
            table_printer.add_row(new String[]{
                LIGHT_GREY_COL + "Device: N/A " + RESET, //(color not supported in table printer for now)
                "Inode: " + stat[0] + " ",
                "Links: " + stat[2],
                ""
            });
            table_printer.print_table();

            System.out.print("Access: (" + Integer.toOctalString(stat[1]).substring(1) + "/" + file_perm_string(stat[1]) + ")  ");
            System.out.print("Uid: " + stat[3] + "  ");
            System.out.print("Gid: " + stat[4] + "  ");
            System.out.print("\n");

            System.out.println("Access: " + format_date(stat[8], "YYYY-MM-dd HH:mm:ss.SSS Z"));
            System.out.println("Modify: " + format_date(stat[9], "YYYY-MM-dd HH:mm:ss.SSS Z"));
            System.out.println("Change: " + format_date(stat[9], "YYYY-MM-dd HH:mm:ss.SSS Z"));
            System.out.println(" Birth: " + format_date(stat[10], "YYYY-MM-dd HH:mm:ss.SSS Z"));
        } catch (FileSystemException e) {
            e.name = target_filename;
            throw e;
        }
    }

    /**
     * Concatenate (cat) command handler.
     *
     * @param   command     The command used
     * @param   arguments   Arguments provided
     */
    public void cmd_cat(String command, String[] arguments) throws FileSystemException {
        read_file(arguments[0], 0);
    }

    /**
     * Head (head) command handler.
     *
     * @param   command     The command used
     * @param   arguments   Arguments provided
     */
    public void cmd_head(String command, String[] arguments) throws FileSystemException {
        if (arguments[0].charAt(0) == '-') {
            if (arguments[0].equals("-c")) {
                if (arguments.length >= 3) {
                    read_file(arguments[2], 0, Integer.parseInt(arguments[1]));
                }
            }
        }
    }

    /**
     * Tail (tail) command handler.
     *
     * @param   command     The command used
     * @param   arguments   Arguments provided
     */
    public void cmd_tail(String command, String[] arguments) throws FileSystemException {
        if (arguments[0].charAt(0) == '-') {
            if (arguments[0].equals("-c")) {
                if (arguments.length >= 3) {
                    read_file(arguments[2], Integer.parseInt(arguments[1]) * -1, Integer.parseInt(arguments[1]));
                }
            }
        }
    }

    /**
     * Print directory entries (pde / print-dir-entries) command handler.
     */
    public void cmd_pde() {
        TablePrinter table = new TablePrinter();
        table.add_row(new String[]{"Inode", "Length", "Name length", "File type", "Filename"});
        for (int direntry_offset : cwd.inode.iter_direntries()) {
            int datablock_pt = cwd.inode.get_datablock_pt(direntry_offset / Volume.BLOCK_LEN);
            String[] row = new String[5];
            row[0] = Integer.toString(vol.bb.getInt(datablock_pt + direntry_offset % Volume.BLOCK_LEN));
            row[1] = Integer.toString(vol.bb.getShort(datablock_pt + direntry_offset % Volume.BLOCK_LEN + 4));
            char[] filename = new char[vol.bb.get(datablock_pt + direntry_offset % Volume.BLOCK_LEN + 6)];
            row[2] = Integer.toString(filename.length);
            row[3] = Integer.toString(vol.bb.get(datablock_pt + direntry_offset % Volume.BLOCK_LEN + 7));
            for (int i = 0; i < filename.length; i++) {
                filename[i] = (char)vol.bb.get(datablock_pt + direntry_offset % Volume.BLOCK_LEN + 8 + i);
            }
            row[4] = new String(filename);
            table.add_row(row);
        }
        table.print_table();
    }

    /**
     * Converts file mode from a number to a file type string.
     *
     * @param   file_mode   The file mode
     * @return  File type
     */
    public String file_type_string(int file_mode) throws FileSystemException {
        int[] file_types = {0xC000, 0xA000, 0x8000, 0x6000, 0x4000, 0x2000, 0x1000}; // TODO octal
        String[] file_type_names = {
            "socket",
            "symbolic link",
            "regular file",
            "block device",
            "directory",
            "character deice",
            "FIFO"
        };
        for (int i = 0; i < 7; i++) {
            if ((file_mode & file_types[i]) == file_types[i]) {
                return file_type_names[i];
            }
        }
        return "unknown file type";
    }

    /**
     * Returns a formatted date string.
     *
     * @param   date_time   Number of seconds since epoch
     * @param   format      The desired format
     * @return  Formatted date string
     */
    public String format_date(int date_time, String format) throws FileSystemException {
        SimpleDateFormat simple_date_format = new SimpleDateFormat(format);
        return simple_date_format.format(new Date((long)date_time * 1000));
    }

    /**
     * Converts file mode from a number to a file permission string.
     *
     * @param   file_mode   The file mode
     * @return  File permission settings
     */
    public String file_perm_string(int file_mode) throws FileSystemException {
        String file_perm_string = "";
        int[] file_types = {0xC000, 0xA000, 0x8000, 0x6000, 0x4000, 0x2000, 0x1000};
        char[] file_type_symbols = {'s', 'l', '-', 'b', 'd', 'c', 'p'};
        for (int i = 0; i < 7; i++) {
            if ((file_mode & 0xF000) == file_types[i]) {
                file_perm_string += file_type_symbols[i];
            }
        }
        for (int i = 0; i < 3; i++) {
            file_perm_string += ((file_mode >> ((2 - i) * 3 + 2) & 1) == 1) ? 'r' : '-';
            file_perm_string += ((file_mode >> ((2 - i) * 3 + 1) & 1) == 1) ? 'w' : '-';
            file_perm_string += ((file_mode >> ((2 - i) * 3) & 1) == 1) ? 'x' : '-';
        }
        return file_perm_string;
    }

    /**
     * Prints (filename), starting from (offset) bytes.
     *
     * @param   filename    Filename to read
     * @param   offset      Offset in bytes
     */
    public void read_file(String filename, long offset) throws FileSystemException {
        File target_file = open(filename, false);
        read_file(filename, offset, target_file.get_size());
    }

    /**
     * Prints (filename), starting from (offset) bytes, to (offset + length) bytes.
     *
     * @param   filename    Filename to read
     * @param   offset      Offset in bytes
     * @param   length      Length in bytes
     */
    public void read_file(String filename, long offset, long length) throws FileSystemException {
        File target_file = open(filename, false);
        for (long i = 0; i < length; i += 10240) {
            long temp_length = (i <= length - 10240) ? 10240 : length - i;
            if (offset < 0) {
                offset = target_file.get_size() - length + i;
            }
            System.out.print(new String(target_file.read(offset, temp_length)));
        }
    }

    /**
     * Gets file from path.
     *
     * @param   path        Path of the file to get
     * @param   directory   If the file should be a directory or not
     * @return  The file object
     */
    public File open(String path, boolean directory) throws FileSystemException {
        try {
            List<String> real_path = realpath(path);
            Inode inode = path_to_inode(real_path);
            if (directory != inode.is_directory()) {
                throw new FileSystemException(directory ? 20 : 21);
            }

            File file = new File(vol, path, real_path, inode);
            return file;
        } catch (FileSystemException e) {
            e.name = path;
            throw e;
        }
    }

    /**
     * Gets inode from path.
     *
     * @param   path_ll Path as a list
     * @return  The inode
     */
    public Inode path_to_inode(List<String> path_ll) throws FileSystemException {
        Inode ino = vol.get_root_inode();
        for (String path_filename : path_ll) {
            try {
                ino = ino.lookup(path_filename);
            } catch (FileSystemException e) {
                e.name = path_filename;
                throw e;
            }
        }
        return ino;
    }

    /**
     * Gets the clean absolute path.
     *
     * @param   path_str    Path as a string
     * @return  The path as a list
     */
    public List<String> realpath(String path_str) {
        String[] filenames = path_str.split("/");
        List<String> path_ll = new LinkedList<String>();

        if (path_str.charAt(0) != '/') {
            for (String filename : cwd.path) {
                path_ll.add(filename);
            }
        }
        for (String filename : filenames) {
            switch (filename) {
                case ".":
                case "":
                    break;
                case "..":
                    if (path_ll.size() != 0) {
                        path_ll.remove(path_ll.size() - 1);
                    }
                    break;
                default:
                    path_ll.add(filename);
                    break;
            }
        }
        return path_ll;
    }
}
