/** For big pairs: edit count with the trace method vs with the split forced everywhere must match. */
public class BigCheck {
    public static void main(String[] args) throws Exception {
        LineFile a = LineFile.read(args[0]), b = LineFile.read(args[1]);
        LineIds ids = LineIds.assign(a, b);
        Myers.traceBudget = 8_000_000;
        long t0 = System.nanoTime();
        int e1 = edits(Diff.compute(ids.a, ids.b, ids.distinct));
        long t1 = System.nanoTime();
        Myers.traceBudget = 0;
        int e2 = edits(Diff.compute(ids.a, ids.b, ids.distinct));
        long t2 = System.nanoTime();
        System.out.printf("%s: edits normal=%d (%d ms) split-only=%d (%d ms) %s%n", args[0], e1,
            (t1 - t0) / 1000000, e2, (t2 - t1) / 1000000, e1 == e2 ? "MATCH" : "MISMATCH");
    }
    static int edits(byte[] ops) { int e = 0; for (byte o : ops) if (o != Diff.KEEP) e++; return e; }
}
