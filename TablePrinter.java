import java.util.*;
import java.util.regex.*;

public class TablePrinter {
    LinkedList<String[]> rows;

    public TablePrinter() {
        rows = new LinkedList<String[]>();
    }

    public void add_row(String[] row) {
        rows.add(row);
    }

    public void print_table() {
        int cols = rows.getFirst().length;
        int[] col_lengths = new int[cols];
        for (String[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                if (col_lengths[i] < string_length(row[i])) col_lengths[i] = string_length(row[i]);
            }
        }
        for (String[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                System.out.print(row[i]);
                for (int j = 0; j < col_lengths[i] - string_length(row[i]) + 1; j++) {
                    System.out.print(" ");
                }
            }
            System.out.print("\n");
        }
    }

    public int string_length(String string) {
        int length = string.length();
        boolean color = false;
        for (int i = 0; i < string.length(); i++) {
            if (string.charAt(i) == (char)0x1b) {
                color = true;
            }

            if (color) {
                if (string.charAt(i) == 'm') {
                    color = false;
                }
                length--;
            }
        }
        return length;
    }
}
