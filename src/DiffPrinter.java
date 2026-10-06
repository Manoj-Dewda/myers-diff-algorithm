import java.io.IOException;
import java.io.OutputStream;

/**
 * Prints the edit script.
 *   ' ' + line   keep   (line is in both files)
 *   '-' + line   delete (line only in A)
 *   '+' + line   insert (line only in B)
 * Each line is printed as its exact bytes, then '\n'.
 *
 * A change block is a run of deletes and inserts with no keep between them.
 * Its deleted lines are A[i .. i+dels) and its inserted lines are B[j .. j+ins),
 * so we print all deletes, then all inserts (the delete-first rule).
 * In highlight mode the k-th '-' pairs with the k-th '+', and a '?' line follows that '+'.
 */
final class DiffPrinter {

    static void print(byte[] ops, LineFile a, LineFile b, boolean highlight, OutputStream out)
            throws IOException {
        int i = 0;                  // next line of A
        int j = 0;                  // next line of B
        int t = 0;                  // next op
        while (t < ops.length) {
            if (ops[t] == Diff.KEEP) {
                writeLine(out, ' ', a, i);
                i++;
                j++;
                t++;
                continue;
            }
            // Count the whole change block first.
            int dels = 0;
            int ins = 0;
            while (t < ops.length && ops[t] != Diff.KEEP) {
                if (ops[t] == Diff.DELETE) dels++; else ins++;
                t++;
            }
            for (int d = 0; d < dels; d++) writeLine(out, '-', a, i + d);
            for (int k = 0; k < ins; k++) {
                writeLine(out, '+', b, j + k);
                if (highlight && k < dels) {           // paired line: k-th delete with k-th insert
                    out.write(Highlight.line(a, i + k, b, j + k));
                }
            }
            i += dels;
            j += ins;
        }
    }

    private static void writeLine(OutputStream out, char prefix, LineFile file, int line)
            throws IOException {
        out.write(prefix);
        out.write(file.data, file.start[line], file.end[line] - file.start[line]);
        out.write('\n');
    }
}
