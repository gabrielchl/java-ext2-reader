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
                System.out.print(String.format("%-" + (col_lengths[i] + 1) + "s", row[i]));
            }
            System.out.print("\n");
        }
    }

    public int string_length(String string) {
        Pattern pattern = Pattern.compile("\\.*?u.*?m");
        Matcher matcher = pattern.matcher(string);
        int length = string.length();
        /**while (matcher.find()) {
            String s = matcher.group(1);
            System.out.println("match");
            length -= s.length();
        }**/
        return length;
    }
}
