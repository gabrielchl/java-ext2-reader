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
        int blank_bytes_count = (bytes.length % 16 == 0) ? 0 : 16 - bytes.length % 16;
        String row_output = "";
        String row_in_string = "";
        for (int i = 0; i < bytes.length + blank_bytes_count; i++) {
            if (i < bytes.length) {
                row_output += Integer.toHexString(bytes[i]) + " ";
                row_in_string += (char)bytes[i];
            } else {
                row_output += "\u001b[38;5;245m" + "XX " + "\u001b[0m";
                row_in_string += "\u001b[38;5;245m" + "." + "\u001b[0m";
            }
            if ((i + 1) % 8 == 0) row_output += "| ";
            if ((i + 1) % 8 == 0 && (i + 1) % 16 != 0) row_in_string += " | ";
            if ((i + 1) % 16 == 0) {
                System.out.println(row_output + row_in_string);
                row_output = "";
                row_in_string = "";
            }
        }
    }
}
