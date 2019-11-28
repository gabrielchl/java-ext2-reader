import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.regex.*;
import java.text.*;

public class Main {
    public static final String BOLD_FONT = "\u001b[1m";
    public static final String LIGHT_GREY_COL = "\u001b[38;5;248m";
    public static final String GREY_COL = "\u001b[38;5;245m";
    public static final String BLUE_COL = "\u001b[38;5;110m";
    public static final String RESET = "\u001b[0m";

    private Volume vol;
    private File cwd;

    public static void main(String[] args) {
        new Main();
    }

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
                        System.out.println("h       print this help text");
                        System.out.println("df      print volume details");
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

    public void cmd_cd(String command, String[] arguments) throws FileSystemException {
        String target_filename = arguments[0];
        try {
            cwd = open(target_filename, true);
        } catch (FileSystemException e) {
            e.name = target_filename;
            throw e;
        }
    }

    public void cmd_ls(String command, String[] arguments) throws FileSystemException { // TODO parse path
        if (Arrays.asList(arguments).contains("-l")) {
            TablePrinter table = new TablePrinter();
            for (String filename : cwd.read_dir()) {
                List<String> path = realpath(filename);
                Inode inode = path_to_inode(path);
                int[] file_stat = inode.stat();
                String[] row = {
                    file_perm_string(file_stat[1]), // rwxrwxrwx
                    Integer.toString(file_stat[2]), // num hard links
                    Integer.toString(file_stat[3]), // uid
                    Integer.toString(file_stat[4]), // gid
                    Long.toString((long)file_stat[6] << 32 | file_stat[5]), // size
                    format_date(file_stat[10], "MMM d HH:mm"), // last modify time
                    "" // filename
                };
                if (inode.is_directory()) {
                    row[6] = BOLD_FONT + BLUE_COL + filename + RESET + " ";
                } else {
                    row[6] = filename + " ";
                }
                table.add_row(row);
            }
            table.print_table();
        } else {
            for (String filename : cwd.read_dir()) {
                List<String> path = realpath(filename);
                Inode inode = path_to_inode(path);
                if (inode.is_directory()) {
                    System.out.print(BOLD_FONT + BLUE_COL + filename + RESET + " ");
                } else {
                    System.out.print(filename + " ");
                }
            }
            System.out.print("\n");
        }
    }

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

    public String format_date(int date_time, String format) throws FileSystemException {
        SimpleDateFormat simple_date_format = new SimpleDateFormat(format);
        return simple_date_format.format(new Date((long)date_time));
    }

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

    public void cmd_cat(String command, String[] arguments) throws FileSystemException {
        read_file(arguments[0], 0);
    }

    public void cmd_head(String command, String[] arguments) throws FileSystemException {
        if (arguments[0].charAt(0) == '-') {
            if (arguments[0].equals("-c")) {
                if (arguments.length >= 3) {
                    read_file(arguments[2], 0, Integer.parseInt(arguments[1]));
                }
            }
        }
    }

    public void cmd_tail(String command, String[] arguments) throws FileSystemException {
        if (arguments[0].charAt(0) == '-') {
            if (arguments[0].equals("-c")) {
                if (arguments.length >= 3) {
                    read_file(arguments[2], Integer.parseInt(arguments[1]) * -1, Integer.parseInt(arguments[1]));
                }
            }
        }
    }

    public void read_file(String filename, long offset) throws FileSystemException {
        File target_file = open(filename, false);
        read_file(filename, offset, target_file.get_size());
    }

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

    public File open(String filename, boolean directory) throws FileSystemException {
        try {
            List<String> path = realpath(filename);
            Inode inode = path_to_inode(path);
            if (directory != inode.is_directory()) {
                throw new FileSystemException(directory ? 20 : 21);
            }

            File file = new File(vol, filename, path, inode);
            return file;
        } catch (FileSystemException e) {
            e.name = filename;
            throw e;
        }
    }

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
