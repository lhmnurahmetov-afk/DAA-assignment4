# Assignment 4 — analysis

## Environment and commands

- Language/runtime: Java, OpenJDK 21.0.12 (compiled by the launcher with `javac --release 17`).
- Machine used for all results below: Linux 6.18 (x86-64), Intel Xeon @ 2.80 GHz, 1 CPU available, about 4 GB RAM.
- Setup/build: `python tools/run.py --language Java --setup`
- Public checks (44 cases + repeated mixed batch): `python tools/run.py --language Java --check` — all 44 public cases passed and the state-reset batch passed.
- Own tests: `python tests/student/oracle_tests.py` — printed `ALL OWN TESTS PASSED`.
- Search benchmark: `javac -cp Java/build -d /tmp/bench tests/student/Bench.java` then `java -Xmx384m -cp Java/build:/tmp/bench Bench`.
- The final commit URL (with the full 40-character SHA) is submitted in Moodle.

## Algorithm explanations

Conventions: n, m are input sizes as defined in the part; all indices are zero-based; all sums that can exceed 32 bits use `long`.

### lcs
- **State.** `dp[i][j]` = length of an LCS of the prefixes `a[0..i)` and `b[0..j)`.
- **Transition.** If `a[i-1]==b[j-1]`, `dp[i][j]=dp[i-1][j-1]+1`; otherwise `dp[i][j]=max(dp[i-1][j], dp[i][j-1])`.
- **Base.** `dp[0][*]=dp[*][0]=0` (an empty prefix has no common characters). This also covers empty inputs: length 0, sequence `""`.
- **Order.** Rows increase, columns increase; every dependency (up, left, diagonal) is already filled.
- **Correctness.** On a match, taking the matched character is never worse than skipping it (exchange argument); on a mismatch at least one of the two last characters is unused, so the optimum is the better of dropping either.
- **Reconstruction.** Start at `(n,m)`: on equal characters emit it and move diagonally; otherwise move to the neighbour with the larger value (ties: up). Reverse the emitted characters. Any optimal sequence is accepted, so ties need no special handling.
- **Cost.** Time O((n+1)(m+1)); memory O((n+1)(m+1)) ints for the table plus O(min-length) output.
- **Boundary test.** `a="tea_time", b="team"` expects length 4 and `team`: it detects an implementation that computes the longest common *substring* (that would give 3).

### merge
- **State.** `dp[l][r]` = minimum total cost to merge original blocks `l..r` into one block; `split[l][r]` = the `k` of the last merge `[l,k]+[k+1,r]`.
- **Transition.** `dp[l][r] = min over l<=k<r of dp[l][k] + dp[k+1][r] + (prefix[r+1]-prefix[l])`; the last merge pays the combined size of the whole interval.
- **Base.** `dp[i][i]=0`. For n<=1, cost 0 and plan `[]`.
- **Order.** Intervals by increasing length, so both sub-intervals (shorter) are done.
- **Correctness.** Some merge is last in any plan; it joins two adjacent intervals that were built independently, so the optimum is the best split with optimal children (optimal substructure). The order of merges inside different children does not affect the cost.
- **Reconstruction.** A post-order traversal (explicit stack) of the split tree emits the left child, then the right child, then `[l,k,r]`. Children are therefore listed before their parent, which makes the plan chronological and valid; exactly n-1 merges are produced. Ties: the first minimal `k`.
- **Cost.** Time O(n^3), auxiliary memory O(n^2) (`dp`, `split`); O(1)-size output per merge, O(n) plan.
- **Boundary test.** `sizes=[2,3,4]` expects cost 14 (merging 3 and 4 first gives 16): it detects a wrong order or a greedy choice. 200 blocks of 10^9 give a cost above 2^31, which detects `int` overflow.

### tree
- **State.** For vertex v: `take[v]` = best weight in the subtree of v with v selected; `skip[v]` = best weight with v not selected.
- **Transition.** `take[v]=w[v]+sum(skip[c])`, `skip[v]=sum(max(take[c],skip[c]))` over children c.
- **Base.** A leaf has `take=w`, `skip=0`. n=0 returns weight 0 and `[]`.
- **Order.** A BFS from vertex 0 gives an order in which parents precede children; processing it backwards is a postorder, so children are complete before their parent. This is iterative: a 100000-vertex chain does not overflow the stack.
- **Correctness.** Selecting v forces every child out; not selecting v leaves each child free, so subtrees are independent given v's decision. The answer is `max(take[root],skip[root])`; since `skip>=0`, the empty set is allowed and all-negative inputs return 0.
- **Reconstruction.** Go in BFS order: the root is chosen if `take>skip`; a child is chosen only if its parent is not chosen and `take[c]>skip[c]`. Ties prefer "skip", which is a valid optimum.
- **Cost.** Time O(n); memory O(n) (adjacency arrays, `take`, `skip`, order, parent).
- **Boundary test.** Weights `[-1,-2,-3]` on a chain expect weight 0 and `[]`: it detects forcing at least one vertex. A star with 10^9 weights checks 64-bit sums and that the centre is not taken together with a leaf.

### naive
- **Invariant.** For start s, compare `text[s+j]` with `pattern[j]` for j=0,1,… until a mismatch or j=m.
- **Starts.** s runs from 0 to n-m (the last valid start); overlaps are reported because s always advances by one.
- **Empty pattern.** Returns every boundary 0..n (n+1 positions); pattern longer than the text returns `[]`.
- **Cost.** Time O(n*m + n + 1) including output; O(1) auxiliary memory; output memory O(z+1).
- **Boundary test.** `text="aaaaa", pattern="aaa"` expects `[0,1,2]`; `text="", pattern=""` expects `[0]`.

### kmp
- **Definition.** `pi[i]` = length of the longest proper prefix of `p[0..i]` that is also a suffix of it.
- **Construction.** For i≥1 start with `k=pi[i-1]`; while `k>0` and `p[i]!=p[k]`, set `k=pi[k-1]`; if `p[i]==p[k]`, `k++`; `pi[i]=k`.
- **Search.** `k` is the length of the current matched prefix. On a mismatch fall back `k=pi[k-1]` without moving in the text; on a match advance k. After a full match (`k==m`) record `i-m+1` and set `k=pi[m-1]`, which keeps the overlapping candidates.
- **Correctness.** The matched prefix is always the longest prefix of the pattern that is a suffix of the text read so far; falling back through `pi` visits exactly the shorter borders, so no occurrence is skipped.
- **Cost.** Each step either advances i or decreases k, and k grows at most once per i: time O(n+m+z+1); auxiliary memory O(m+1); the empty pattern is handled before any pattern indexing.
- **Boundary test.** `text="abababa", pattern="aba"` expects `[0,2,4]`; a version that resets k to 0 after a match misses start 2 and 4.

### rk
- **Hash.** `h(s)=sum s[i]*31^(m-1-i) mod 101` with ASCII codes; the leftmost character has the highest power.
- **Rolling update.** `h' = ((h - t[s]*31^(m-1)) mod 101 + 101) mod 101`, then `h'=(h'*31 + t[s+m]) mod 101`. The `+101` normalises the negative remainder. 64-bit arithmetic is used.
- **Verification.** Equal hashes are candidates only: the code compares all m characters before reporting a start.
- **Edge cases.** m=0 returns 0..n before any window is formed; m>n returns `[]`.
- **Cost.** Time O(n+m+c*m), where c is the number of equal-hash windows; with a modulus of only 101 collisions are frequent, so the safe bound for all permitted inputs is O(n*m+n+m+1). No deterministic linear worst case is claimed. Auxiliary memory O(1) (rolling window); output O(z+1).
- **Boundary test.** `ab` and `dj` both hash to 75 (base 31, mod 101). `text="abdj", pattern="ab"` expects `[0]` and `text="dj", pattern="ab"` expects `[]`: a version that trusts equal hashes would report a false match.

### suffix
- **Prefix doubling.** `rank_k[i]` is the class of the first k characters of suffix i (a missing character after the end ranks as -1, so there is no sentinel in the output). Round k sorts the pairs `(rank_k[i], rank_k[i+k])` to obtain `rank_2k`. Each round is a radix sort by the second key and then a stable counting sort by the first key, so a round costs O(n+128); the loop stops when all n ranks differ (at most ceil(log2 n)+1 rounds): O(n log n).
- **Kasai.** With `rank[sa[i]]=i`, for suffix i compare with the suffix preceding it in `sa`, starting from the previously known length h. Moving from suffix i to i+1 can reduce the LCP by at most 1 (dropping the first character), so h is decremented once per step: total O(n). `lcp[0]=0`, `lcp[r]` compares `sa[r-1]` and `sa[r]`.
- **Longest repeat.** Any repeated substring is a common prefix of two suffixes; the best pair is adjacent in sorted order, so the maximum adjacent LCP is the length of a longest repeat (overlaps allowed), and the repeat is `text[i..i+max)` for the suffix i where it occurs. If max is 0, `repeated=""`.
- **Empty text.** `sa=[]`, `lcp=[]`, `repeated=""` in O(1).
- **Cost.** Time O(n log n) (radix) + O(n) (Kasai); memory O(n+1) (several int arrays of length n plus a counting array of max(n,128)+1). Suffix strings are never materialised.
- **Boundary test.** `banana` expects `sa=[5,3,1,0,4,2]`, `lcp=[0,1,3,0,0,2]`, `repeated="ana"` (overlapping occurrences); a 50000-character string of equal letters checks the worst number of doubling rounds and `repeated` of length 49999.

### aho
- **Trie.** One node per prefix of any pattern over the 95-character alphabet (codes 32..126), stored in a flat `int[(L+1)*95]`. Duplicate patterns end at the same terminal state, and `term[i]` keeps the state of dictionary position i, so duplicates receive equal counts.
- **Failure links and automaton.** BFS from the root: `fail[u]=go[fail[v]][c]` for a child u of v on c; a missing transition is filled with `go[fail[v]][c]`, so the matching loop never backtracks.
- **Counting.** For each text, reset `visits` (independent texts), run the automaton and do `visits[state]++` per character; then process states in reverse BFS order (deeper states first) doing `visits[fail[s]] += visits[s]`. Now `visits[s]` is the number of times the pattern string of state s (and every pattern that is a suffix of it, via failure links) occurred. `counts[i]=visits[term[i]]`.
- **Why it handles suffix patterns and overlaps.** In `sheshe` the state of `she` propagates to the state of `he` through its failure link, so `he` is counted too; overlaps are counted because the automaton continues from the current state.
- **Empty cases.** No patterns gives an empty row per text; no texts gives `[]`; an empty text gives a zero per dictionary position.
- **Cost.** Build O(L+95*S+p) time and O(95*S+p) memory; per text O(|text|+S+p). Output memory O(q*(p+1)+1). No per-match records are created.
- **Boundary test.** Patterns `["he","she","he"]`, texts `["sheshe","sheshe",""]` expect `[[2,2,2],[2,2,2],[0,0,0]]`: it checks the suffix pattern, the duplicate and the reset between texts. A dense case (patterns `a, aa, …, a^399` plus duplicates of `a`, 100 texts of 2000 `a`) checks that counting does not enumerate individual matches.

## Own tests

All tests are in `tests/student/`. `oracle_tests.py` sends one mixed JSON Lines batch (shuffled tasks, one process) and compares each answer with an independent brute-force oracle written in Python (subset enumeration for LCS and the tree, memoised recursion for merge, `str.startswith` scanning for search/aho, sorting of suffix slices for suffix). Run: `python tests/student/oracle_tests.py` (result: `ALL OWN TESTS PASSED`, no failures).

|Command|Input or fixture path|Expected result|Actual result / log|What it checks|
|---|---|---|---|---|
|lcs|150 random pairs, length 0–9, alphabet abc (`oracle_tests.py`)|length equals brute-force optimum; sequence is a subsequence of both|passed|empty inputs, repeated characters, wrong DP transitions|
|lcs|`a="tea_time", b="team"`|length 4, `team`|passed|subsequence versus substring|
|merge|100 random size arrays (0–8 blocks, values include 0 and 10^9)|cost equals memoised optimum; plan valid, chronological, n-1 merges, cost recomputed from plan|passed|ordering of child merges, adjacency, zeros|
|merge|`[2,3,4]`, `[]`, `[7]`, 200×10^9|14; 0 with `[]`; 0 with `[]`; cost > 2^31 and 199 merges|passed|greedy order, n≤1, 32-bit overflow|
|tree|150 random trees (n 0–11, weights −6..9, shuffled edges)|weight equals subset-enumeration optimum; selection has no repeats, no adjacent pair, sums to the weight|passed|take/skip transitions, reconstruction, ties|
|tree|all negative on a chain; n=0|weight 0, `[]`|passed|empty set allowed|
|tree|chain n=100000 (weights 1); star n=100000 (weights 10^9)|weight 50000; weight 99999·10^9 and centre not selected|passed|deep chain without recursion failure, 64-bit sums|
|naive/kmp/rk|3×200 random (text ≤14, pattern ≤4, alphabet `ab `), mixed in one batch|positions equal all starts incl. overlaps|passed|overlaps, empty pattern (n+1 starts), empty text, pattern longer, spaces|
|rk|`abdj`/`ab` and `dj`/`ab`|`[0]` and `[]`|passed|hash collision (both hash to 75) is verified by characters|
|kmp|`aaaaa`/`aaa`; `a`×200000 / `a`×100000|`[0,1,2]`; 100001 starts|passed|fallback after a full match, dense overlaps at the size limit|
|suffix|150 random texts (length 0–14, alphabets a/ab/abc)|sa and lcp equal the sorted-slices oracle; repeated has the maximum LCP length and occurs twice|passed|doubling, Kasai, empty text, no sentinel|
|suffix|`banana`, `abc`, `a`×50000|`ana`; `""`; sa descending, repeated length 49999|passed|overlapping repeat, no repeat, worst doubling|
|aho|100 random dictionaries/texts (0–5 patterns, 0–4 texts)|counts equal the per-position `startswith` oracle|passed|empty dictionary/texts/text, duplicates, suffix patterns, overlaps|
|aho|`he, she, he` / `sheshe, sheshe, ""`; empty dictionary; empty texts|`[[2,2,2],[2,2,2],[0,0,0]]`; `[[],[]]`; `[]`|passed|reset between texts, empty rows|
|aho|399 prefix-patterns of `a` + 5 duplicates, 100 texts of 2000 `a`|first count 2000; last text, pattern a^399 count 1602|passed|dense matches stay fast (reverse-failure aggregation)|

## Search comparison

Inputs are identical for naive, KMP and RK. Generators: *random* = uniform letters a–z with `java.util.Random(42+n)`, the pattern is a random substring of the text (so there is at least one match); *repetitive* = text `a^n`, pattern `a^(m-1)b`, which has no match and is the worst case for naive. Correctness was verified first: for every row the three algorithms returned identical position lists (column "Correct outputs checked"; all rows agree).

Timing scope: only the call `Algorithms.<task>(Map)` for the already-parsed input (no JSON, no JVM start, no compilation). Policy: 300 warm-up calls per algorithm and case (JIT warm-up), then 3 runs; one run is the mean of 50 consecutive calls, in microseconds. Command: see above (`Bench`). Machine: see Environment (1 CPU, no isolation from other processes).

|Case|n|m|Seed/generator|Algorithm|Run 1|Run 2|Run 3|Median|Correct outputs checked|
|---|---:|---:|---|---|---:|---:|---:|---:|---|
|random|300|10|seed 342, uniform a-z, pattern = random substring of text|naive|7.5|6.3|6.6|6.6|naive=KMP=RK (1 starts)|
|random|300|10|seed 342, uniform a-z, pattern = random substring of text|kmp|11.9|12.7|12.7|12.7|naive=KMP=RK (1 starts)|
|random|300|10|seed 342, uniform a-z, pattern = random substring of text|rk|14.6|91.9|14.1|14.6|naive=KMP=RK (1 starts)|
|random|1000|100|seed 1042, uniform a-z, pattern = random substring of text|naive|24.6|21.7|105.4|24.6|naive=KMP=RK (1 starts)|
|random|1000|100|seed 1042, uniform a-z, pattern = random substring of text|kmp|114.5|28.5|29.7|29.7|naive=KMP=RK (1 starts)|
|random|1000|100|seed 1042, uniform a-z, pattern = random substring of text|rk|24.1|24.0|23.7|24.0|naive=KMP=RK (1 starts)|
|random|3000|1000|seed 3042, uniform a-z, pattern = random substring of text|naive|6.7|88.1|7.7|7.7|naive=KMP=RK (1 starts)|
|random|3000|1000|seed 3042, uniform a-z, pattern = random substring of text|kmp|197.1|245.9|11.5|197.1|naive=KMP=RK (1 starts)|
|random|3000|1000|seed 3042, uniform a-z, pattern = random substring of text|rk|115.0|55.4|46.7|55.4|naive=KMP=RK (1 starts)|
|repetitive|300|10|text a^n, pattern a^(m-1)b (no match, worst case for naive)|naive|6.1|5.5|5.3|5.5|naive=KMP=RK (0 starts)|
|repetitive|300|10|text a^n, pattern a^(m-1)b (no match, worst case for naive)|kmp|1.8|1.7|1.8|1.8|naive=KMP=RK (0 starts)|
|repetitive|300|10|text a^n, pattern a^(m-1)b (no match, worst case for naive)|rk|5.0|4.8|8.7|5.0|naive=KMP=RK (0 starts)|
|repetitive|1000|100|text a^n, pattern a^(m-1)b (no match, worst case for naive)|naive|302.7|302.1|305.6|302.7|naive=KMP=RK (0 starts)|
|repetitive|1000|100|text a^n, pattern a^(m-1)b (no match, worst case for naive)|kmp|16.9|10.9|11.0|11.0|naive=KMP=RK (0 starts)|
|repetitive|1000|100|text a^n, pattern a^(m-1)b (no match, worst case for naive)|rk|18.0|18.3|17.1|18.0|naive=KMP=RK (0 starts)|
|repetitive|3000|1000|text a^n, pattern a^(m-1)b (no match, worst case for naive)|naive|3878.3|3685.0|3914.9|3878.3|naive=KMP=RK (0 starts)|
|repetitive|3000|1000|text a^n, pattern a^(m-1)b (no match, worst case for naive)|kmp|20.3|20.2|20.0|20.2|naive=KMP=RK (0 starts)|
|repetitive|3000|1000|text a^n, pattern a^(m-1)b (no match, worst case for naive)|rk|43.6|43.7|43.0|43.6|naive=KMP=RK (0 starts)|

**Observations (not a proof of asymptotic complexity).**
- On the repetitive worst case for naive, naive's time grows roughly with n·m (about 5.5 µs, 303 µs, 3878 µs for the three sizes) while KMP stays near the size of the text (about 1.8, 11, 20 µs). This is consistent with O(n·m) versus O(n+m).
- On the random case almost every window of the naive search fails at the first character, so naive does about n comparisons and is as fast as or faster than KMP, which pays for building `pi`. These random-case measurements are noisy: single runs such as 245.9 µs for KMP at (3000, 1000) are far above its neighbouring runs (11.5 µs), which I attribute to JIT compilation and GC on a shared single CPU, but I did not isolate the cause. Because the median of only three runs is sensitive to such outliers, I do not draw conclusions from small differences between algorithms in the random rows.
- RK in the repetitive case did not hit its quadratic behaviour: the pattern `a^(m-1)b` differs from every window `a^m` by exactly 1 modulo 101, so there are never any equal-hash candidates (c=0) and RK ran in about n steps. A case that makes RK quadratic would need many windows whose hash equals the pattern's hash; I did not benchmark such an input, so the O(n·m) worst-case bound for RK is stated from the analysis, not measured.
- The deliberately small modulus 101 means only 101 distinct fingerprints, so on long random text roughly 1 in 101 windows is expected to be a false candidate and must be verified character by character (up to m comparisons). This is specific to this assignment's parameters and should not be generalised to hash functions with a large modulus. The collision `ab`/`dj` (both 75) is included as a correctness test.

## Sources and AI assistance

- **Tools and sources:** The course materials in this repository (assignment specification, contracts, lecture notes and the interactive lesson) and the standard algorithms taught in the course.
- **AI contribution:** I used AI to help me write a well-structured ANALYSIS.md file, and I also asked how to add an instructor to a private assignment, since I had forgotten how to do that.
- **My own verification:** I executed the commands on my computer and studied each algorithm.