import java.util.*;

public class Helper {
    /**
     * Outputs an array of bytes as returned by read( ) in a readable
     * hexadecimal format, perhaps with printable ASCII codes by the side. Need
     * to be able to neatly handle having too few bytes -- note the XX entries.
     * For example,
     * 00 00 00 00 00 00 00 00 | 00 00 00 00 00 00 00 00 | ........ | ........
     * 00 00 00 00 00 00 00 00 | 00 00 00 00 00 00 00 00 | ........ | ........
     * 48 65 6C 6C 6F 20 57 6F | 72 6C 44 00 00 00 00 00 | Hello Wo | rld.....
     * 00 00 00 00 00 00 00 00 | 00 00 00 00 00 00 00 00 | ........ | ........
     * 00 00 00 00 00 00 00 00 | 00 00 00 00 00 XX XX XX | ........ | .....
     *
     * @param   bytes   Bytes to be dumped
     */
    public void dumpHexBytes(byte[] bytes) {
        int print_col_count = 0;
        int print_row_count = 0;
        int blank_bytes_count = 16 - bytes.length % 16;
        if (blank_bytes_count == 16) blank_bytes_count = 0;
        String row_output = "";
        String row_in_string = "";
        for (byte b : bytes) {
            row_output += Integer.toHexString(b) + " ";
            row_in_string += (char)b;
            print_col_count += 1;
            if (print_col_count % 8 == 0) {
                row_output += "| ";
                if (print_col_count % 16 != 0)
                    row_in_string += " | ";
            }
            if (print_col_count % 16 == 0) {
                row_output += row_in_string;
                System.out.println(row_output);
                row_output = "";
                row_in_string = "";
                print_col_count = 0;
            }
        }
        for (int i = 0; i < blank_bytes_count; i++) {
            print_col_count++;
            row_output += "\u001b[38;5;245m" + "XX " + "\u001b[0m";
            row_in_string += "\u001b[38;5;245m" + "." + "\u001b[0m";
            if (print_col_count % 8 == 0) {
                row_output += "| ";
                if (print_col_count % 16 != 0)
                    row_in_string += " | ";
            }
        }
        row_output += row_in_string;
        print_row_count += 1;
        if (blank_bytes_count > 0)
            System.out.println(row_output);
    }
}
