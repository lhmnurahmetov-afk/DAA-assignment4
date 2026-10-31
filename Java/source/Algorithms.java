import java.util.*;

public final class Algorithms {

    private static List<Integer> boxed(int[] a) {
        List<Integer> r = new ArrayList<>(a.length);
        for (int v : a) r.add(v);
        return r;
    }

    private static long asLong(Object o) { return ((Number) o).longValue(); }

    static Map<String, Object> lcs(Map<String, Object> input) {
        String a = (String) input.get("a"), b = (String) input.get("b");
        int n = a.length(), m = b.length();
        int[][] dp = new int[n + 1][m + 1]; // row 0 / column 0 = base case 0
        for (int i = 1; i <= n; i++) {
            char ca = a.charAt(i - 1);
            for (int j = 1; j <= m; j++) {
                if (ca == b.charAt(j - 1)) dp[i][j] = dp[i - 1][j - 1] + 1;
                else dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
            }
        }
        StringBuilder sb = new StringBuilder();
        int i = n, j = m;
        while (i > 0 && j > 0) {
            if (a.charAt(i - 1) == b.charAt(j - 1)) { sb.append(a.charAt(i - 1)); i--; j--; }
            else if (dp[i - 1][j] >= dp[i][j - 1]) i--;
            else j--;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("length", dp[n][m]);
        out.put("sequence", sb.reverse().toString());
        return out;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> merge(Map<String, Object> input) {
        List<Object> sz = (List<Object>) input.get("sizes");
        int n = sz.size();
        Map<String, Object> out = new LinkedHashMap<>();
        if (n <= 1) {
            out.put("cost", 0L);
            out.put("plan", new ArrayList<Object>());
            return out;
        }
        long[] pre = new long[n + 1];
        for (int i = 0; i < n; i++) pre[i + 1] = pre[i] + asLong(sz.get(i));
        long[][] dp = new long[n][n];
        int[][] split = new int[n][n];
        for (int len = 2; len <= n; len++) {
            for (int l = 0; l + len - 1 < n; l++) {
                int r = l + len - 1;
                long best = Long.MAX_VALUE;
                int bk = l;
                long total = pre[r + 1] - pre[l];
                for (int k = l; k < r; k++) {
                    long c = dp[l][k] + dp[k + 1][r] + total;
                    if (c < best) { best = c; bk = k; }
                }
                dp[l][r] = best;
                split[l][r] = bk;
            }
        }
        
        List<Object> plan = new ArrayList<>();
        Deque<int[]> st = new ArrayDeque<>();
        st.push(new int[]{0, n - 1, 0});
        while (!st.isEmpty()) {
            int[] f = st.peek();
            int l = f[0], r = f[1];
            if (l == r) { st.pop(); continue; }
            int k = split[l][r];
            if (f[2] == 0) { f[2] = 1; st.push(new int[]{l, k, 0}); }
            else if (f[2] == 1) { f[2] = 2; st.push(new int[]{k + 1, r, 0}); }
            else { plan.add(new int[]{l, k, r}); st.pop(); }
        }
        out.put("cost", dp[0][n - 1]);
        out.put("plan", plan);
        return out;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> tree(Map<String, Object> input) {
        int n = (int) asLong(input.get("n"));
        Map<String, Object> out = new LinkedHashMap<>();
        if (n == 0) {
            out.put("weight", 0L);
            out.put("selected", new ArrayList<Object>());
            return out;
        }
        List<Object> w = (List<Object>) input.get("weights");
        List<Object> edges = (List<Object>) input.get("edges");
        long[] wt = new long[n];
        for (int i = 0; i < n; i++) wt[i] = asLong(w.get(i));
        int e = edges.size();
        int[] head = new int[n], nxt = new int[2 * e], to = new int[2 * e];
        Arrays.fill(head, -1);
        int c = 0;
        for (Object eo : edges) {
            List<Object> p = (List<Object>) eo;
            int u = (int) asLong(p.get(0)), v = (int) asLong(p.get(1));
            to[c] = v; nxt[c] = head[u]; head[u] = c++;
            to[c] = u; nxt[c] = head[v]; head[v] = c++;
        }
        int[] parent = new int[n], order = new int[n];
        parent[0] = -1;
        int qh = 0, qt = 0;
        order[qt++] = 0;
        while (qh < qt) {
            int v = order[qh++];
            for (int x = head[v]; x != -1; x = nxt[x]) {
                int u = to[x];
                if (u != parent[v]) { parent[u] = v; order[qt++] = u; }
            }
        }
        long[] take = new long[n], skip = new long[n];
        for (int i = 0; i < n; i++) take[i] = wt[i];
        for (int i = n - 1; i >= 1; i--) { // children processed before parents
            int v = order[i], p = parent[v];
            take[p] += skip[v];
            skip[p] += Math.max(take[v], skip[v]);
        }
        boolean[] chosen = new boolean[n];
        chosen[0] = take[0] > skip[0];
        for (int i = 1; i < n; i++) {
            int v = order[i];
            chosen[v] = !chosen[parent[v]] && take[v] > skip[v];
        }
        List<Integer> sel = new ArrayList<>();
        for (int i = 0; i < n; i++) if (chosen[i]) sel.add(i);
        out.put("weight", Math.max(take[0], skip[0]));
        out.put("selected", sel);
        return out;
    }

    static Map<String, Object> naive(Map<String, Object> input) {
        String t = (String) input.get("text"), p = (String) input.get("pattern");
        int n = t.length(), m = p.length();
        List<Integer> pos = new ArrayList<>();
        if (m == 0) { for (int i = 0; i <= n; i++) pos.add(i); }
        else {
            for (int s = 0; s + m <= n; s++) {
                int j = 0;
                while (j < m && t.charAt(s + j) == p.charAt(j)) j++;
                if (j == m) pos.add(s);
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("positions", pos);
        return out;
    }

    static Map<String, Object> kmp(Map<String, Object> input) {
        String t = (String) input.get("text"), p = (String) input.get("pattern");
        int n = t.length(), m = p.length();
        List<Integer> pos = new ArrayList<>();
        if (m == 0) { for (int i = 0; i <= n; i++) pos.add(i); }
        else {
            int[] pi = new int[m];
            for (int i = 1, k = 0; i < m; i++) {
                while (k > 0 && p.charAt(i) != p.charAt(k)) k = pi[k - 1];
                if (p.charAt(i) == p.charAt(k)) k++;
                pi[i] = k;
            }
            for (int i = 0, k = 0; i < n; i++) {
                while (k > 0 && t.charAt(i) != p.charAt(k)) k = pi[k - 1];
                if (t.charAt(i) == p.charAt(k)) k++;
                if (k == m) { pos.add(i - m + 1); k = pi[k - 1]; } // fallback keeps overlaps
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("positions", pos);
        return out;
    }

    static Map<String, Object> rk(Map<String, Object> input) {
        final int B = 31, MOD = 101;
        String t = (String) input.get("text"), p = (String) input.get("pattern");
        int n = t.length(), m = p.length();
        List<Integer> pos = new ArrayList<>();
        if (m == 0) { for (int i = 0; i <= n; i++) pos.add(i); }
        else if (m <= n) {
            long high = 1; // B^(m-1) mod MOD
            for (int i = 1; i < m; i++) high = (high * B) % MOD;
            long hp = 0, ht = 0;
            for (int i = 0; i < m; i++) {
                hp = (hp * B + p.charAt(i)) % MOD;
                ht = (ht * B + t.charAt(i)) % MOD;
            }
            for (int s = 0; ; s++) {
                if (hp == ht) { // candidate only: compare the actual characters
                    int j = 0;
                    while (j < m && t.charAt(s + j) == p.charAt(j)) j++;
                    if (j == m) pos.add(s);
                }
                if (s + m >= n) break;
                ht = ((ht - t.charAt(s) * high) % MOD + MOD) % MOD; // drop leftmost, normalize
                ht = (ht * B + t.charAt(s + m)) % MOD;              // add next character
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("positions", pos);
        return out;
    }

    static Map<String, Object> suffix(Map<String, Object> input) {
        String t = (String) input.get("text");
        int n = t.length();
        Map<String, Object> out = new LinkedHashMap<>();
        if (n == 0) {
            out.put("sa", new ArrayList<Object>());
            out.put("lcp", new ArrayList<Object>());
            out.put("repeated", "");
            return out;
        }
        int m = Math.max(n, 128) + 1;
        int[] sa = new int[n], sa2 = new int[n], rnk = new int[n], tmp = new int[n];
        int[] cnt = new int[m];
        for (int i = 0; i < n; i++) { rnk[i] = t.charAt(i); cnt[rnk[i]]++; }
        for (int i = 1; i < m; i++) cnt[i] += cnt[i - 1];
        for (int i = n - 1; i >= 0; i--) sa[--cnt[rnk[i]]] = i;
        for (int k = 1; ; k <<= 1) {
            int p = 0;
            for (int i = Math.max(0, n - k); i < n; i++) sa2[p++] = i; // empty second key first
            for (int j = 0; j < n; j++) if (sa[j] >= k) sa2[p++] = sa[j] - k;
            Arrays.fill(cnt, 0);
            for (int i = 0; i < n; i++) cnt[rnk[i]]++;
            for (int i = 1; i < m; i++) cnt[i] += cnt[i - 1];
            for (int j = n - 1; j >= 0; j--) sa[--cnt[rnk[sa2[j]]]] = sa2[j];
            tmp[sa[0]] = 0;
            int classes = 0;
            for (int j = 1; j < n; j++) {
                int a = sa[j - 1], b = sa[j];
                int ra = a + k < n ? rnk[a + k] : -1, rb = b + k < n ? rnk[b + k] : -1;
                if (rnk[a] != rnk[b] || ra != rb) classes++;
                tmp[b] = classes;
            }
            int[] sw = rnk; rnk = tmp; tmp = sw;
            if (classes == n - 1) break;
        }
        int[] rank = new int[n];
        for (int i = 0; i < n; i++) rank[sa[i]] = i;
        int[] lcp = new int[n];
        int h = 0, best = 0, bestPos = 0;
        for (int i = 0; i < n; i++) { // Kasai: lcp drops by at most 1 from i to i+1
            if (rank[i] > 0) {
                int j = sa[rank[i] - 1];
                while (i + h < n && j + h < n && t.charAt(i + h) == t.charAt(j + h)) h++;
                lcp[rank[i]] = h;
                if (h > best) { best = h; bestPos = i; }
                if (h > 0) h--;
            } else h = 0;
        }
        out.put("sa", boxed(sa));
        out.put("lcp", boxed(lcp));
        out.put("repeated", best == 0 ? "" : t.substring(bestPos, bestPos + best));
        return out;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> aho(Map<String, Object> input) {
        List<Object> pats = (List<Object>) input.get("patterns");
        List<Object> texts = (List<Object>) input.get("texts");
        final int A = 95; // printable ASCII 32..126
        int p = pats.size(), L = 0;
        for (Object o : pats) L += ((String) o).length();
        int[] go = new int[(L + 1) * A]; // 0 = absent (root is never a child)
        int states = 1;
        int[] term = new int[p];
        for (int i = 0; i < p; i++) {
            String s = (String) pats.get(i);
            int v = 0;
            for (int j = 0; j < s.length(); j++) {
                int c = s.charAt(j) - 32;
                if (go[v * A + c] == 0) go[v * A + c] = states++;
                v = go[v * A + c];
            }
            term[i] = v; // duplicates share the same terminal state
        }
        int[] fail = new int[states], bfs = new int[states];
        int qh = 0, qt = 0;
        for (int c = 0; c < A; c++) {
            int u = go[c];
            if (u != 0) { fail[u] = 0; bfs[qt++] = u; }
        }
        while (qh < qt) {
            int v = bfs[qh++];
            for (int c = 0; c < A; c++) {
                int u = go[v * A + c];
                if (u != 0) { fail[u] = go[fail[v] * A + c]; bfs[qt++] = u; }
                else go[v * A + c] = go[fail[v] * A + c]; // complete the automaton
            }
        }
        List<Object> rows = new ArrayList<>();
        long[] visits = new long[states];
        for (Object to : texts) {
            String t = (String) to;
            Arrays.fill(visits, 0);
            int v = 0;
            for (int i = 0; i < t.length(); i++) {
                v = go[v * A + (t.charAt(i) - 32)];
                visits[v]++;
            }
            for (int i = qt - 1; i >= 0; i--) { // deeper states first: push counts up fail links
                int s = bfs[i];
                visits[fail[s]] += visits[s];
            }
            long[] row = new long[p];
            for (int i = 0; i < p; i++) row[i] = visits[term[i]];
            rows.add(row);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("counts", rows);
        return out;
    }
}
