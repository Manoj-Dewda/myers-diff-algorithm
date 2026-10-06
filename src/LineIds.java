import java.util.Arrays;

/**
 * Gives every distinct line (compared as exact bytes) a small int id, shared by both files.
 * After this, file A is just int[] a and file B is int[] b, and the diff compares ints.
 *
 * The table uses open addressing over plain int arrays, so there is no object per line.
 */
final class LineIds {
    final int[] a;
    final int[] b;
    final int distinct;

    private LineIds(int[] a, int[] b, int distinct) {
        this.a = a;
        this.b = b;
        this.distinct = distinct;
    }

    static LineIds assign(LineFile fileA, LineFile fileB) {
        int total = fileA.count + fileB.count;
        int capacity = 2;
        while (capacity < 2 * total) capacity <<= 1;   // keep the table at most half full
        int mask = capacity - 1;

        int[] slots = new int[capacity];               // slot -> id, or -1 if empty
        Arrays.fill(slots, -1);
        int[] idHash = new int[Math.max(total, 1)];    // id -> hash of its line
        LineFile[] idFile = new LineFile[Math.max(total, 1)]; // id -> a file holding the line
        int[] idLine = new int[Math.max(total, 1)];    // id -> that line's index
        int distinct = 0;

        LineFile[] files = {fileA, fileB};
        int[][] result = {new int[fileA.count], new int[fileB.count]};
        for (int f = 0; f < 2; f++) {
            LineFile file = files[f];
            for (int i = 0; i < file.count; i++) {
                int h = hash(file.data, file.start[i], file.end[i]);
                int slot = h & mask;
                while (true) {
                    int id = slots[slot];
                    if (id == -1) {                     // a line not seen before: new id
                        id = distinct++;
                        slots[slot] = id;
                        idHash[id] = h;
                        idFile[id] = file;
                        idLine[id] = i;
                        result[f][i] = id;
                        break;
                    }
                    LineFile g = idFile[id];
                    int j = idLine[id];
                    if (idHash[id] == h && Arrays.equals(
                            file.data, file.start[i], file.end[i], g.data, g.start[j], g.end[j])) {
                        result[f][i] = id;              // same bytes as an earlier line
                        break;
                    }
                    slot = (slot + 1) & mask;           // collision: try the next slot
                }
            }
        }
        return new LineIds(result[0], result[1], distinct);
    }

    /** FNV-1a hash of data[from..to), with a final mix so the low bits spread well. */
    private static int hash(byte[] data, int from, int to) {
        int h = 0x811C9DC5;
        for (int i = from; i < to; i++) {
            h ^= data[i];
            h *= 0x01000193;
        }
        h ^= h >>> 16;
        h *= 0x85EBCA6B;
        h ^= h >>> 13;
        return h;
    }
}
