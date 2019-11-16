import java.util.*;

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
                if (col_lengths[i] < row[i].length()) col_lengths[i] = row[i].length();
            }
        }
        for (String[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                System.out.print(String.format("%-" + (col_lengths[i] + 1) + "s", row[i]));
            }
            System.out.print("\n");
        }
    }
}
