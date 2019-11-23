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
        //Volume vol = new Volume('./ext2fs');
        Helper helper = new Helper();
        /**helper.dumpHexBytes("ABC".getBytes());
        System.out.println("");
        helper.dumpHexBytes("ABCDEFG#".getBytes());
        System.out.println("");
        helper.dumpHexBytes("ABCDEFG#HIJKL".getBytes());
        System.out.println("");
        helper.dumpHexBytes("ABCDEFG#HIJKLMN#".getBytes());
        System.out.println("");
        helper.dumpHexBytes("ABCDEFG#HIJKLMN#OP".getBytes());
        System.out.println("");
        helper.dumpHexBytes("ABCDEFG#HIJKLMN#OPQRSTU#".getBytes());
        System.out.println("");
        helper.dumpHexBytes("ABCDEFG#HIJKLMN#OPQRSTU#VW".getBytes());
        System.out.println("");
        helper.dumpHexBytes("ABCDEFG#HIJKLMN#OPQRSTU#VWXYZAA#".getBytes());**/

        /**try {
            RandomAccessFile vol = new RandomAccessFile("ext2fs", "r");
            int length = (int)vol.length();
            byte[] b = new byte[1124];
            vol.readFully(b);
            helper.dumpHexBytes(b);
        } catch (Exception e) {

        }**/

        vol = new Volume("./ext2fs");
        //vol.get_root_inode().lookup(".").details();
        //vol.get_root_inode().lookup("deep").details();

        Scanner scanner = new Scanner(System.in);
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
                    ls(command, arguments);
                    break;
                case "stat":
                    if (arguments.length >= 1) {
                        stat(command, arguments);
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
                        cat(command, arguments);
                    }
                    break;
                case "quit":
                case "exit":
                    System.out.println("Good bye! :)");
                    System.exit(0);
                    break;
                default:
                    System.out.println(command + ": command not found");
                    break;
            }
        }
    }

    public void ls(String command, String[] arguments) {
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
                    file.get_filename()
                };
                table.add_row(row);
            }
            table.print_table();
        } else {
            for (File file : vol.get_cwd().read_dir()) {
                System.out.print(file.get_filename() + " ");
            }
            System.out.print("\n");
        }
    }

    public void stat(String command, String[] arguments) {
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
                "Device: N/A ", // LIGHT_GREY_COL + "Device: N/A " + RESET (color not supported in table printer for now)
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
            e.print_err_msg("stat: " + target_filename);
        }
    }

    public String file_type_string(int file_mode) {
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

    public String format_date(int date_time, String format) {
        SimpleDateFormat simple_date_format = new SimpleDateFormat(format);
        return simple_date_format.format(date_time);
    }

    public String file_perm_string(int file_mode) {
        String file_perm_string = "";
        char[] file_type_symbols = {'?', '-', 'd', 'c', 'b', 'p', 's', 'l'};
        System.out.println(file_mode);
        file_perm_string += file_type_symbols[(file_mode >> 9) & 1];
        for (int i = 0; i < 3; i++) {
            file_perm_string += ((file_mode >> ((2 - i) * 3 + 2) & 1) == 1) ? 'r' : '-';
            file_perm_string += ((file_mode >> ((2 - i) * 3 + 1) & 1) == 1) ? 'w' : '-';
            file_perm_string += ((file_mode >> ((2 - i) * 3) & 1) == 1) ? 'x' : '-';
        }
        return file_perm_string;
    }

    public void cat(String command, String[] arguments) { // TODO loop
        String target_filename = arguments[0];
        try {
            Inode target_inode = parse_path(parse_path(target_filename));
            if (target_inode.is_directory()) {
                throw new FileSystemException(21);
            }
            LinkedList<String> target_path = new LinkedList<String>(vol.get_cwd().get_path());
            target_path.add(target_filename);
            File target_file = new File(vol, target_filename, target_path, target_inode);
            for (long i = 0; i < target_file.get_size(); i += 10240) {
                long length = (i <= target_file.get_size() - 10240) ? 10240 : target_file.get_size() - i;
                System.out.print(new String(target_file.read(length)));
            }
            //System.out.print(new String(target_file.read(target_file.get_size())));
            /**char[] content = new char[target_inode.file_size()];
            for (int i = 0; i < target_inode.file_size(); i++) {
                content[i] = (char)vol.bb.get(target_inode.datablock_pointer() + i);
            }
            System.out.println(new String(content));**/
        } catch (FileSystemException e) {
            e.print_err_msg("cat: " + target_filename);
        }
    }

    public LinkedList<String> parse_path(String input_path) {
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

    public Inode parse_path(LinkedList<String> path) {
        Inode temp = vol.get_root_inode();
        for (String path_filename : path) {
            try {
                temp = temp.lookup(path_filename);
            } catch (FileSystemException e) {
                e.print_err_msg(path_filename);
                break;
            }
        }
        return temp;
    }
}
