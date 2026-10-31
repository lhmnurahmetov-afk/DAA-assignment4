import java.util.*;

public class Bench {
    static final int WARM = 300, INNER = 50, RUNS = 3;

    static String random(Random r, int n, int alpha) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append((char) ('a' + r.nextInt(alpha)));
        return sb.toString();
    }

    public static void main(String[] args) {
        int[][] sizes = {{300, 10}, {1000, 100}, {3000, 1000}};
        String[] algos = {"naive", "kmp", "rk"};
        System.out.println("|Case|n|m|Seed/generator|Algorithm|Run 1 (us)|Run 2 (us)|Run 3 (us)|Median (us)|Correct outputs checked|");
        System.out.println("|---|---:|---:|---|---|---:|---:|---:|---:|---|");
        for (String kind : new String[]{"random", "repetitive"}) {
            for (int[] sz : sizes) {
                int n = sz[0], m = sz[1];
                Random rnd = new Random(42L + n);
                String text, pat, gen;
                if (kind.equals("random")) {
                    text = random(rnd, n, 26);
                    int st = rnd.nextInt(n - m + 1);
                    pat = text.substring(st, st + m);
                    gen = "seed " + (42L + n) + ", uniform a-z, pattern = random substring of text";
                } else {
                    StringBuilder t = new StringBuilder(), p = new StringBuilder();
                    for (int i = 0; i < n; i++) t.append('a');
                    for (int i = 0; i < m - 1; i++) p.append('a');
                    p.append('b');
                    text = t.toString(); pat = p.toString();
                    gen = "text a^n, pattern a^(m-1)b (no match, worst case for naive)";
                }
                Map<String, Object> req = new HashMap<>();
                req.put("text", text); req.put("pattern", pat);
                Object ref = Algorithms.naive(req).get("positions");
                boolean agree = ref.equals(Algorithms.kmp(req).get("positions"))
                        && ref.equals(Algorithms.rk(req).get("positions"));
                for (String a : algos) {
                    for (int i = 0; i < WARM; i++) call(a, req);
                    double[] runs = new double[RUNS];
                    for (int r = 0; r < RUNS; r++) {
                        long t0 = System.nanoTime();
                        for (int i = 0; i < INNER; i++) call(a, req);
                        runs[r] = (System.nanoTime() - t0) / 1000.0 / INNER;
                    }
                    double[] s = runs.clone(); Arrays.sort(s);
                    System.out.printf("|%s|%d|%d|%s|%s|%.1f|%.1f|%.1f|%.1f|%s (%d starts)|%n", kind, n, m, gen, a,
                            runs[0], runs[1], runs[2], s[1], agree ? "naive=KMP=RK" : "MISMATCH",
                            ((List<?>) ref).size());
                }
            }
        }
    }

    static Object call(String a, Map<String, Object> req) {
        switch (a) {
            case "naive": return Algorithms.naive(req);
            case "kmp": return Algorithms.kmp(req);
            default: return Algorithms.rk(req);
        }
    }
}
