import java.util.Random;

/** Fast random test of Myers.keptPairs and Diff.compute against an O(N*M) LCS table.
 *  Usage: java MyersTest [traceBudget] [maxLen] [alphabet] [cases]; traceBudget 0 forces the split path. */
public class MyersTest {
    static int LEN = 30;
    public static void main(String[] args) {
        if (args.length > 1) LEN = Integer.parseInt(args[1]);
        if (args.length > 0) Myers.traceBudget = Integer.parseInt(args[0]);
        Random rng = new Random(1);
        int cases = args.length > 3 ? Integer.parseInt(args[3]) : 20000;
        for (int c = 0; c < cases; c++) {
            int alpha = 1 + rng.nextInt(args.length > 2 ? Integer.parseInt(args[2]) : 5);
            int[] a = rand(rng, rng.nextInt(LEN), alpha), b = rand(rng, rng.nextInt(LEN), alpha);
            int best = lcs(a, b);
            int[][] p = Myers.keptPairs(a, b);
            if (p[0].length != best) fail("Myers LCS " + p[0].length + " != " + best, a, b);
            for (int t = 0; t < p[0].length; t++) {
                if (a[p[0][t]] != b[p[1][t]]) fail("kept pair not equal", a, b);
                if (t > 0 && (p[0][t] <= p[0][t-1] || p[1][t] <= p[1][t-1])) fail("pairs not increasing", a, b);
            }
            byte[] ops = Diff.compute(a, b, alpha);
            int i = 0, j = 0, edits = 0;
            for (byte op : ops) {
                if (op == Diff.KEEP) { if (a[i] != b[j]) fail("bad keep", a, b); i++; j++; }
                else if (op == Diff.DELETE) { i++; edits++; } else { j++; edits++; }
            }
            if (i != a.length || j != b.length) fail("script does not cover inputs", a, b);
            if (edits != a.length + b.length - 2 * best) fail("Diff not minimal", a, b);
        }
        System.out.println("all " + cases + " Myers/Diff cases passed");
    }
    static int[] rand(Random r, int n, int alpha) { int[] x = new int[n]; for (int i = 0; i < n; i++) x[i] = r.nextInt(alpha); return x; }
    static int lcs(int[] a, int[] b) {
        int[][] t = new int[a.length + 1][b.length + 1];
        for (int i = 1; i <= a.length; i++) for (int j = 1; j <= b.length; j++)
            t[i][j] = a[i-1] == b[j-1] ? t[i-1][j-1] + 1 : Math.max(t[i-1][j], t[i][j-1]);
        return t[a.length][b.length];
    }
    static void fail(String why, int[] a, int[] b) {
        throw new AssertionError(why + "\na=" + java.util.Arrays.toString(a) + "\nb=" + java.util.Arrays.toString(b));
    }
}
