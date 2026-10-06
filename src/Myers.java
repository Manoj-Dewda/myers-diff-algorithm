import java.util.ArrayList;
import java.util.Arrays;

/**
 * Eugene Myers' O(ND) greedy diff (1986), on two int sequences.
 *
 * Edit graph: x walks along a, y along b.
 *   right    (x+1)      = delete a[x]                    cost 1
 *   down     (y+1)      = insert b[y]                    cost 1
 *   diagonal (x+1, y+1) = keep, only if a[x] == b[y],    free
 * Diagonal k = x - y. V[k] = furthest x reached on diagonal k so far.
 *
 * Result: the kept pairs of a shortest edit script, keepA[t] / keepB[t] (increasing):
 * a[keepA[t]] is kept as b[keepB[t]]. Everything else is deleted or inserted.
 *
 * Normal case: the greedy forward search with a saved trace, then backtracking.
 * Safety net: the trace needs about D*D/2 ints. If that would pass traceBudget, we find a
 * point on an optimal path with the bidirectional "middle snake" search (paper, section 4b),
 * split the problem there, and solve each half the same way.
 */
final class Myers {

    /** Most ints the saved trace may hold (8 million ints = 32 MB). */
    static int traceBudget = 8_000_000;

    static int[][] keptPairs(int[] a, int[] b) {
        Pairs out = new Pairs(Math.min(a.length, b.length));
        solve(a, 0, a.length, b, 0, b.length, out);
        return new int[][] {Arrays.copyOf(out.a, out.size), Arrays.copyOf(out.b, out.size)};
    }

    /** Appends, in order, the kept pairs of a shortest script for a[aLo..aHi) vs b[bLo..bHi). */
    private static void solve(int[] a, int aLo, int aHi, int[] b, int bLo, int bHi, Pairs out) {
        // Equal values at the start and end are always kept.
        while (aLo < aHi && bLo < bHi && a[aLo] == b[bLo]) {
            out.add(aLo, bLo);
            aLo++;
            bLo++;
        }
        int suffix = 0;
        while (aLo < aHi - suffix && bLo < bHi - suffix
                && a[aHi - 1 - suffix] == b[bHi - 1 - suffix]) {
            suffix++;
        }
        aHi -= suffix;
        bHi -= suffix;

        if (aLo < aHi && bLo < bHi && !greedy(a, aLo, aHi, b, bLo, bHi, out, traceBudget)) {
            int[] split = middleSplit(a, aLo, aHi, b, bLo, bHi);
            if (split == null) {
                // No value of a appears in b: nothing to keep.
            } else if ((split[0] == aLo && split[1] == bLo) || (split[0] == aHi && split[1] == bHi)) {
                greedy(a, aLo, aHi, b, bLo, bHi, out, Long.MAX_VALUE);   // never happens in practice
            } else {
                solve(a, aLo, split[0], b, bLo, split[1], out);
                solve(a, split[0], aHi, b, split[1], bHi, out);
            }
        }
        for (int t = 0; t < suffix; t++) out.add(aHi + t, bHi + t);
    }

    /**
     * The greedy algorithm from the paper. Returns false, having added nothing,
     * if the saved trace would need more than budget ints.
     */
    private static boolean greedy(int[] a, int aLo, int aHi, int[] b, int bLo, int bHi,
                                  Pairs out, long budget) {
        int n = aHi - aLo;
        int m = bHi - bLo;
        int max = n + m;
        int offset = max + 1;                 // V[k] lives at v[offset + k]; k may be negative
        int[] v = new int[2 * max + 3];       // v[offset + 1] = 0, so round 0 starts at (0, 0)

        // trace.get(d) = V after round d, only for k = -d, -d+2, ..., d, at index (k + d) / 2.
        ArrayList<int[]> trace = new ArrayList<>();
        long traceSize = 0;
        int finalD = -1;

        search:
        for (int d = 0; d <= max; d++) {
            for (int k = -d; k <= d; k += 2) {
                int x;
                if (k == -d || (k != d && v[offset + k - 1] < v[offset + k + 1])) {
                    x = v[offset + k + 1];        // came from diagonal k+1 by moving down (insert)
                } else {
                    x = v[offset + k - 1] + 1;    // came from diagonal k-1 by moving right (delete)
                }
                int y = x - k;
                while (x < n && y < m && a[aLo + x] == b[bLo + y]) {   // follow the snake
                    x++;
                    y++;
                }
                v[offset + k] = x;
                if (x >= n && y >= m) {           // reached (N, M): d is the minimum
                    finalD = d;
                    break search;
                }
            }
            // Save only the d+1 entries of this round, never the whole V array:
            // copying all of V every round would make the time quadratic.
            traceSize += d + 1;
            if (traceSize > budget) return false;
            int[] round = new int[d + 1];
            for (int i = 0; i <= d; i++) round[i] = v[offset - d + 2 * i];
            trace.add(round);
        }

        // Number of kept pairs = (N + M - D) / 2. Fill them from the back while walking back.
        int kept = (n + m - finalD) / 2;
        int[] keepA = new int[kept];
        int[] keepB = new int[kept];
        int pos = kept;

        // Backtrack from (N, M). At round d, redo the down-or-right choice with the
        // V saved after round d-1, which is exactly what round d saw going forward.
        int x = n;
        int y = m;
        for (int d = finalD; d > 0; d--) {
            int[] prev = trace.get(d - 1);       // k' of round d-1 is at index (k' + d - 1) / 2
            int k = x - y;
            int iDown = (k + d) / 2;              // index of k+1 in prev
            int iRight = iDown - 1;               // index of k-1 in prev
            boolean down = k == -d || (k != d && prev[iRight] < prev[iDown]);
            int prevK = down ? k + 1 : k - 1;
            int prevX = down ? prev[iDown] : prev[iRight];
            int prevY = prevX - prevK;

            while (x > prevX && y > prevY) {      // the snake, walked backwards: keeps
                x--;
                y--;
                pos--;
                keepA[pos] = aLo + x;
                keepB[pos] = bLo + y;
            }
            if (down) y--; else x--;              // undo round d's one edit
        }
        while (x > 0 && y > 0) {                  // round 0's snake from (0, 0)
            x--;
            y--;
            pos--;
            keepA[pos] = aLo + x;
            keepB[pos] = bLo + y;
        }
        for (int t = 0; t < kept; t++) out.add(keepA[t], keepB[t]);
        return true;
    }

    /**
     * Bidirectional search (paper, section 4b): run the greedy search forward from (0, 0)
     * and backward from (N, M) at the same time. Where the two meet on a diagonal, that
     * point lies on an optimal path. Returns {x, y} in absolute positions,
     * or null if a and b have nothing in common.
     */
    private static int[] middleSplit(int[] a, int aLo, int aHi, int[] b, int bLo, int bHi) {
        int n = aHi - aLo;
        int m = bHi - bLo;
        int maxD = (n + m + 1) / 2;
        int offset = maxD;
        int length = 2 * maxD + 2;
        int[] vf = new int[length];               // forward:  furthest x on diagonal k
        int[] vb = new int[length];               // backward: furthest x counted from the end
        Arrays.fill(vf, -1);
        Arrays.fill(vb, -1);
        vf[offset + 1] = 0;
        vb[offset + 1] = 0;
        int delta = n - m;
        boolean front = (delta & 1) != 0;         // odd delta: the paths meet on a forward step
        int kfStart = 0, kfEnd = 0, kbStart = 0, kbEnd = 0;   // diagonals that left the grid

        for (int d = 0; d < maxD; d++) {
            for (int k = -d + kfStart; k <= d - kfEnd; k += 2) {
                int i = offset + k;
                int x = (k == -d || (k != d && vf[i - 1] < vf[i + 1])) ? vf[i + 1] : vf[i - 1] + 1;
                int y = x - k;
                while (x < n && y < m && a[aLo + x] == b[bLo + y]) {
                    x++;
                    y++;
                }
                vf[i] = x;
                if (x > n) {
                    kfEnd += 2;                   // ran off the right edge
                } else if (y > m) {
                    kfStart += 2;                 // ran off the bottom edge
                } else if (front) {
                    int j = offset + delta - k;   // the same diagonal, seen from the end
                    if (j >= 0 && j < length && vb[j] != -1 && x >= n - vb[j]) {
                        return new int[] {aLo + x, bLo + y};
                    }
                }
            }
            for (int k = -d + kbStart; k <= d - kbEnd; k += 2) {
                int i = offset + k;
                int x = (k == -d || (k != d && vb[i - 1] < vb[i + 1])) ? vb[i + 1] : vb[i - 1] + 1;
                int y = x - k;
                while (x < n && y < m && a[aHi - 1 - x] == b[bHi - 1 - y]) {
                    x++;
                    y++;
                }
                vb[i] = x;
                if (x > n) {
                    kbEnd += 2;
                } else if (y > m) {
                    kbStart += 2;
                } else if (!front) {
                    int j = offset + delta - k;
                    if (j >= 0 && j < length && vf[j] != -1) {
                        int fx = vf[j];
                        int fy = fx - (delta - k);
                        if (fx >= n - x) return new int[] {aLo + fx, bLo + fy};
                    }
                }
            }
        }
        return null;
    }

    /** A growing list of kept pairs, in order. */
    private static final class Pairs {
        int[] a;
        int[] b;
        int size;

        Pairs(int capacity) {
            a = new int[Math.max(capacity, 1)];
            b = new int[Math.max(capacity, 1)];
        }

        void add(int i, int j) {
            if (size == a.length) {
                a = Arrays.copyOf(a, size * 2);
                b = Arrays.copyOf(b, size * 2);
            }
            a[size] = i;
            b[size] = j;
            size++;
        }
    }
}
