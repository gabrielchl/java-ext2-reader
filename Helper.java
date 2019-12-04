import java.util.*;

/**
 * A helper class.
 */
public class Helper {
    public static final String GREY_COL = "\u001b[38;5;245m";
    public static final String RESET_COL = "\u001b[0m";

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
    public static void dumpHexBytes(byte[] bytes) {
        int blank_bytes_count = (bytes.length % 16 == 0) ? 0 : 16 - bytes.length % 16;
        String row_output = "";
        String row_in_string = "";
        for (int i = 0; i < bytes.length + blank_bytes_count; i++) {
            // for each byte, add to output and string output
            if (i < bytes.length) {
                row_output += String.format("%02x ", bytes[i]).toUpperCase();
                row_in_string += (bytes[i] == 0) ? GREY_COL + "." + RESET_COL : (char)bytes[i];
            } else {
                row_output += GREY_COL + "XX " + RESET_COL;
                row_in_string += " ";
            }
            // add divider
            if ((i + 1) % 8 == 0) row_output += "| ";
            if ((i + 1) % 8 == 0 && (i + 1) % 16 != 0) row_in_string += " | ";
            // print row
            if ((i + 1) % 16 == 0) {
                System.out.println(row_output + row_in_string);
                row_output = "";
                row_in_string = "";
            }
        }
    }
}
