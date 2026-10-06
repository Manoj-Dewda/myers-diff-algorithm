import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A file read as raw bytes and split into lines on the byte '\n'.
 * No Strings are made: line i is data[start[i] .. end[i]) and never includes the '\n'.
 * A '\r' stays inside the line, so "a\r\n" and "a\n" are different lines.
 */
final class LineFile {
    final byte[] data;
    final int[] start;
    final int[] end;
    final int count;

    private LineFile(byte[] data, int[] start, int[] end, int count) {
        this.data = data;
        this.start = start;
        this.end = end;
        this.count = count;
    }

    static LineFile read(String path) throws IOException {
        byte[] data = Files.readAllBytes(Path.of(path));

        // Every '\n' ends one line. Bytes after the last '\n' form one more line;
        // if there are none (the file ends with '\n', or is empty) that piece is empty and dropped.
        int newlines = 0;
        for (byte x : data) {
            if (x == '\n') newlines++;
        }
        boolean lastPieceNotEmpty = data.length > 0 && data[data.length - 1] != '\n';
        int count = newlines + (lastPieceNotEmpty ? 1 : 0);

        int[] start = new int[count];
        int[] end = new int[count];
        int line = 0;
        int lineStart = 0;
        for (int i = 0; i < data.length; i++) {
            if (data[i] == '\n') {
                start[line] = lineStart;
                end[line] = i;
                line++;
                lineStart = i + 1;
            }
        }
        if (lastPieceNotEmpty) {
            start[line] = lineStart;
            end[line] = data.length;
        }
        return new LineFile(data, start, end, count);
    }
}
