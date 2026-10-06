import java.io.BufferedOutputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.InvalidPathException;

/**
 * Command-line entry point.
 *
 *   java Main lines A B       Part A: minimal line diff of file A to file B
 *   java Main highlight A B   Part B: the same diff plus changed-character ranges
 */
public class Main {

    public static void main(String[] args) throws IOException {
        if (args.length != 3 || !(args[0].equals("lines") || args[0].equals("highlight"))) {
            System.err.println("usage: Main lines|highlight FILE_A FILE_B");
            System.exit(2);
        }
        boolean highlight = args[0].equals("highlight");

        // Read BOTH files before printing anything, so a read error leaves stdout empty.
        LineFile a;
        LineFile b;
        try {
            a = LineFile.read(args[1]);
            b = LineFile.read(args[2]);
        } catch (IOException | InvalidPathException e) {
            System.err.println("error: cannot read file: " + e.getMessage());
            System.exit(2);
            return; // never reached; tells the compiler a and b are set below
        }

        // Turn every line into an int id. Equal bytes give the same id.
        LineIds ids = LineIds.assign(a, b);

        // The edit script: one KEEP, DELETE or INSERT per output line.
        byte[] ops = Diff.compute(ids.a, ids.b, ids.distinct);

        // One big buffer over raw stdout: we write bytes, never Strings.
        OutputStream out = new BufferedOutputStream(new FileOutputStream(FileDescriptor.out), 1 << 16);
        DiffPrinter.print(ops, a, b, highlight, out);
        out.flush();
    }
}
