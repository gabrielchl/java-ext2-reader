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

    public static void main(String[] args) {
        new Main();
    }

    public Main() {
        vol = new Volume("./ext2fs");

        Scanner scanner = new Scanner(System.in);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.out.println("\nGood bye! :)");
            }
        });

        while (true) {
            System.out.print(BOLD_FONT + BLUE_COL + vol.get_cwd().get_path_string() + " $ " + RESET);
            /**String raw_input = new String();
            Boolean typing = true;
            while (typing) {
                String temp = scanner.next();
                System.out.println("hey");
                if (temp.equals("x") || temp.equals("\n") || temp.equals("\r\n")) {
                    typing = false;
                    System.out.println("test");
                }
                raw_input += temp;
            }
            String[] input = raw_input.trim().split("[ ]+");**/
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
                        System.out.println(vol.get_cwd().get_path_string());
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
                            vol.change_dir(arguments[0]);
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

    public void cmd_ls(String command, String[] arguments) throws FileSystemException {
        if (Arrays.asList(arguments).contains("-l")) { // should be equal
            //vol.get_cwd().get_inode().list_long();
            TablePrinter table = new TablePrinter();
            for (File file : vol.get_cwd().read_dir()) {
                int[] file_stat = file.get_inode().stat();
                String[] row = {
                    file_perm_string(file_stat[1]), // rwxrwxrwx
                    Integer.toString(file_stat[2]), // num hard links
                    Integer.toString(file_stat[3]), // uid
                    Integer.toString(file_stat[4]), // gid
                    Long.toString((long)file_stat[6] << 32 | file_stat[5]), // size
                    format_date(file_stat[7], "MMM d HH:mm"), // last access time
                    "" // filename
                };
                if (file.get_inode().is_directory()) {
                    row[6] = BOLD_FONT + BLUE_COL + file.get_filename() + RESET + " ";
                } else {
                    row[6] = file.get_filename() + " ";
                }
                table.add_row(row);
            }
            table.print_table();
        } else {
            for (File file : vol.get_cwd().read_dir()) {
                if (file.get_inode().is_directory()) {
                    System.out.print(BOLD_FONT + BLUE_COL + file.get_filename() + RESET + " ");
                } else {
                    System.out.print(file.get_filename() + " ");
                }
            }
            System.out.print("\n");
        }
    }

    public void cmd_stat(String command, String[] arguments) throws FileSystemException {
        String target_filename = arguments[0];
        try {
            Inode target_inode = vol.get_cwd().get_inode().lookup(target_filename);
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
        return simple_date_format.format(date_time);
    }

    public String file_perm_string(int file_mode) throws FileSystemException {
        String file_perm_string = "";
        int[] file_types = {0xC000, 0xA000, 0x8000, 0x6000, 0x4000, 0x2000, 0x1000};
        char[] file_type_symbols = {'s', 'l', '-', 'b', 'd', 'c', 'p'};
        for (int i = 0; i < 7; i++) {
            if ((file_mode & 0xC000) == file_types[i]) {
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

    public LinkedList<String> parse_path(String input_path) throws FileSystemException {
        String[] path_filenames = input_path.split("/");
        LinkedList<String> ret = new LinkedList<String>();
        if (vol.get_cwd().get_path().size() > 1 && input_path != "" && input_path.charAt(0) != '/') {
            for (String path_filename : vol.get_cwd().get_path()) {
                if (!path_filename.equals("")) {
                    ret.add(path_filename);
                }
            }
        }
        for (String path_filename : path_filenames) {
            if (!path_filename.equals("")) {
                ret.add(path_filename);
            }
        }
        return ret;
    }

    public Inode parse_path(LinkedList<String> path) throws FileSystemException { // TODO stop if errora
        Inode temp = vol.get_root_inode();
        for (String path_filename : path) {
            try {
                temp = temp.lookup(path_filename);
            } catch (FileSystemException e) {
                e.name = path_filename;
                throw e;
            }
        }
        return temp;
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
        File target_file = file_from_filename(filename);
        read_file(filename, offset, target_file.get_size());
    }

    public void read_file(String filename, long offset, long length) throws FileSystemException {
        File target_file = file_from_filename(filename);
        for (long i = 0; i < length; i += 10240) {
            long temp_length = (i <= length - 10240) ? 10240 : length - i;
            if (offset < 0) {
                offset = target_file.get_size() - length + i;
            }
            System.out.print(new String(target_file.read(offset, temp_length)));
        }
    }

    public File file_from_filename(String filename) throws FileSystemException { // TODO filetype limit
        try {
            Inode inode = parse_path(parse_path(filename));
            if (inode.is_directory()) {
                throw new FileSystemException(21);
            }
            LinkedList<String> path = new LinkedList<String>(vol.get_cwd().get_path());
            path.add(filename);
            File file = new File(vol, filename, path, inode);
            return file;
        } catch (FileSystemException e) {
            e.name = filename;
            throw e;
        }
    }
}
