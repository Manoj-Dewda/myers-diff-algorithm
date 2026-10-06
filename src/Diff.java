/**
 * Minimal edit script of two int sequences whose values are in [0, alphabet).
 * Used for lines (values = line ids) and for characters (values = code point ids).
 *
 * Three steps make Myers fast without changing the result:
 *   1. Equal values at the start and at the end of both sides are always kept.
 *   2. A value that never appears on the other side can never be kept,
 *      so it is removed before Myers and simply deleted or inserted afterwards.
 *   3. Myers runs on what is left and returns the kept pairs.
 * Then the script is rebuilt: between two kept pairs come all deletes, then all inserts,
 * which is exactly the delete-first rule.
 */
final class Diff {
    static final byte KEEP = 0;
    static final byte DELETE = 1;
    static final byte INSERT = 2;

    static byte[] compute(int[] a, int[] b, int alphabet) {
        int n = a.length;
        int m = b.length;

        // 1. Common prefix and suffix.
        int prefix = 0;
        while (prefix < n && prefix < m && a[prefix] == b[prefix]) prefix++;
        int suffix = 0;
        while (suffix < n - prefix && suffix < m - prefix && a[n - 1 - suffix] == b[m - 1 - suffix]) suffix++;
        int aEnd = n - suffix;
        int bEnd = m - suffix;

        // 2. Which values appear in the middle part of each side?
        boolean[] inA = new boolean[alphabet];
        boolean[] inB = new boolean[alphabet];
        for (int i = prefix; i < aEnd; i++) inA[a[i]] = true;
        for (int j = prefix; j < bEnd; j++) inB[b[j]] = true;

        // Keep only positions whose value also appears on the other side.
        int[] posA = new int[aEnd - prefix];
        int lenA = 0;
        for (int i = prefix; i < aEnd; i++) if (inB[a[i]]) posA[lenA++] = i;
        int[] posB = new int[bEnd - prefix];
        int lenB = 0;
        for (int j = prefix; j < bEnd; j++) if (inA[b[j]]) posB[lenB++] = j;

        int[] smallA = new int[lenA];
        for (int i = 0; i < lenA; i++) smallA[i] = a[posA[i]];
        int[] smallB = new int[lenB];
        for (int j = 0; j < lenB; j++) smallB[j] = b[posB[j]];

        // 3. Myers on the reduced sequences.
        int[][] pairs = Myers.keptPairs(smallA, smallB);
        int middleKept = pairs[0].length;

        // Rebuild the full script. Its length is N + M - (number of keeps).
        int keeps = prefix + middleKept + suffix;
        byte[] ops = new byte[n + m - keeps];
        int o = 0;
        for (int t = 0; t < prefix; t++) ops[o++] = KEEP;
        int i = prefix;
        int j = prefix;
        for (int t = 0; t < middleKept; t++) {
            int keepI = posA[pairs[0][t]];       // map back to positions in the full sequences
            int keepJ = posB[pairs[1][t]];
            while (i < keepI) { ops[o++] = DELETE; i++; }
            while (j < keepJ) { ops[o++] = INSERT; j++; }
            ops[o++] = KEEP;
            i++;
            j++;
        }
        while (i < aEnd) { ops[o++] = DELETE; i++; }
        while (j < bEnd) { ops[o++] = INSERT; j++; }
        for (int t = 0; t < suffix; t++) ops[o++] = KEEP;
        return ops;
    }
}
