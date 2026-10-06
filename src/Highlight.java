import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/**
 * Part B: for one paired old line and new line, find the changed characters.
 *
 * Characters are Unicode code points (an emoji is one character, even though Java
 * stores it as two chars). We run the same minimal diff on the two code point
 * sequences: deleted code points are the old ranges, inserted ones are the new ranges.
 *
 * Output: "? <old ranges> | <new ranges>\n", ranges as start-end (end not included),
 * comma-separated, touching ranges merged, "." when a side has no change.
 */
final class Highlight {

    static byte[] line(LineFile a, int lineA, LineFile b, int lineB) {
        int[] oldChars = codePoints(a, lineA);
        int[] newChars = codePoints(b, lineB);

        // Give each distinct code point of this pair a small id, as Diff expects.
        HashMap<Integer, Integer> idOf = new HashMap<>();
        int[] oldIds = toIds(oldChars, idOf);
        int[] newIds = toIds(newChars, idOf);
        byte[] ops = Diff.compute(oldIds, newIds, idOf.size());

        Ranges oldRanges = new Ranges();
        Ranges newRanges = new Ranges();
        int i = 0;                              // position in the old line
        int j = 0;                              // position in the new line
        for (byte op : ops) {
            if (op == Diff.KEEP) {
                i++;
                j++;
            } else if (op == Diff.DELETE) {
                oldRanges.add(i);
                i++;
            } else {
                newRanges.add(j);
                j++;
            }
        }
        String text = "? " + oldRanges + " | " + newRanges + "\n";
        return text.getBytes(StandardCharsets.US_ASCII);
    }

    private static int[] codePoints(LineFile file, int line) {
        String s = new String(file.data, file.start[line], file.end[line] - file.start[line],
                StandardCharsets.UTF_8);
        return s.codePoints().toArray();
    }

    private static int[] toIds(int[] chars, HashMap<Integer, Integer> idOf) {
        int[] ids = new int[chars.length];
        for (int i = 0; i < chars.length; i++) {
            Integer id = idOf.get(chars[i]);
            if (id == null) {
                id = idOf.size();
                idOf.put(chars[i], id);
            }
            ids[i] = id;
        }
        return ids;
    }

    /** Collects increasing positions and merges touching ones into start-end ranges. */
    private static final class Ranges {
        private final StringBuilder text = new StringBuilder();
        private int start = -1;                 // current open range [start, end)
        private int end = -1;

        void add(int position) {
            if (position == end) {              // touches the open range: extend it
                end++;
                return;
            }
            flush();
            start = position;
            end = position + 1;
        }

        private void flush() {
            if (start < 0) return;
            if (text.length() > 0) text.append(',');
            text.append(start).append('-').append(end);
            start = -1;
            end = -1;
        }

        @Override
        public String toString() {
            flush();
            return text.length() == 0 ? "." : text.toString();
        }
    }
}
