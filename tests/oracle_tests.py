import itertools, json, pathlib, random, subprocess, sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
CMD = ['java', '-Xmx384m', '-cp', str(ROOT / 'Java' / 'build'), 'Main']
random.seed(12345)

def run(reqs):
    data = ''.join(json.dumps(r) + '\n' for r in reqs)
    res = subprocess.run(CMD, input=data, capture_output=True, text=True, timeout=120)
    assert res.returncode == 0, res.stderr
    out = [json.loads(l) for l in res.stdout.splitlines()]
    assert len(out) == len(reqs)
    return out

def rs(n, alpha='ab'):
    return ''.join(random.choice(alpha) for _ in range(n))

def is_subseq(s, t):
    it = iter(t)
    return all(c in it for c in s)


def lcs_len(a, b): 
    if len(a) > len(b): a, b = b, a
    best = 0
    for mask in range(1 << len(a)):
        s = ''.join(a[i] for i in range(len(a)) if mask >> i & 1)
        if len(s) > best and is_subseq(s, b): best = len(s)
    return best

def merge_cost(sz):
    from functools import lru_cache
    @lru_cache(None)
    def f(l, r):
        if l == r: return 0
        return sum(sz[l:r+1]) + min(f(l, k) + f(k+1, r) for k in range(l, r))
    return f(0, len(sz)-1) if sz else 0

def tree_best(n, w, edges): 
    best = 0
    for mask in range(1 << n):
        if any(mask >> u & 1 and mask >> v & 1 for u, v in edges): continue
        best = max(best, sum(w[i] for i in range(n) if mask >> i & 1))
    return best

def occ(t, p):
    return [i for i in range(len(t)+1) if t.startswith(p, i)] 

def suffix_oracle(t):
    sa = sorted(range(len(t)), key=lambda i: t[i:])
    if not t:
        return [], []
    lcp = [0] + [len(list(itertools.takewhile(lambda x: x[0] == x[1], zip(t[sa[i-1]:], t[sa[i]:])))) for i in range(1, len(t))]
    return sa, lcp

def aho_oracle(pats, texts):
    return [[sum(1 for i in range(len(t)) if t.startswith(p, i)) for p in pats] for t in texts]

fails = 0
def expect(cond, msg):
    global fails
    if not cond:
        fails += 1
        print('FAIL', msg)


reqs, meta = [], []
for _ in range(150):
    a, b = rs(random.randint(0, 9), 'abc'), rs(random.randint(0, 9), 'abc')
    reqs.append({'task': 'lcs', 'a': a, 'b': b}); meta.append(('lcs', a, b))
for _ in range(100):
    sz = [random.choice([0, 1, 2, 5, 10**9]) for _ in range(random.randint(0, 8))]
    reqs.append({'task': 'merge', 'sizes': sz}); meta.append(('merge', sz))
for _ in range(150):
    n = random.randint(0, 11)
    w = [random.randint(-6, 9) for _ in range(n)]
    edges = [[random.randrange(i), i] for i in range(1, n)]
    random.shuffle(edges)
    reqs.append({'task': 'tree', 'n': n, 'weights': w, 'edges': edges}); meta.append(('tree', n, w, edges))
for task in ('naive', 'kmp', 'rk'):
    for _ in range(200):
        t, p = rs(random.randint(0, 14), 'ab '), rs(random.randint(0, 4), 'ab ')
        reqs.append({'task': task, 'text': t, 'pattern': p}); meta.append(('search', t, p))
for _ in range(150):
    t = rs(random.randint(0, 14), random.choice(['a', 'ab', 'abc']))
    reqs.append({'task': 'suffix', 'text': t}); meta.append(('suffix', t))
for _ in range(100):
    pats = [rs(random.randint(1, 3), 'ab') for _ in range(random.randint(0, 5))]
    texts = [rs(random.randint(0, 10), 'ab') for _ in range(random.randint(0, 4))]
    reqs.append({'task': 'aho', 'patterns': pats, 'texts': texts}); meta.append(('aho', pats, texts))

order = list(range(len(reqs)))
random.shuffle(order) 
reqs = [reqs[i] for i in order]; meta = [meta[i] for i in order]
answers = run(reqs)

for r, m, ans in zip(reqs, meta, answers):
    k = m[0]
    if k == 'lcs':
        _, a, b = m
        s = ans['sequence']
        expect(ans['length'] == len(s) == lcs_len(a, b) and is_subseq(s, a) and is_subseq(s, b), f'lcs {a!r} {b!r} -> {ans}')
    elif k == 'merge':
        sz = m[1]
        ok = ans['cost'] == merge_cost(tuple(sz)) and len(ans['plan']) == max(0, len(sz) - 1)
        if ok:
            blocks = [(i, i) for i in range(len(sz))]; c = 0
            for l, kk, rr in ans['plan']:
                j = blocks.index((l, kk)); ok &= blocks[j+1] == (kk+1, rr)
                c += sum(sz[l:rr+1]); blocks[j:j+2] = [(l, rr)]
            ok &= c == ans['cost']
        expect(ok, f'merge {sz} -> {ans}')
    elif k == 'tree':
        _, n, w, edges = m
        sel = ans['selected']
        ok = (ans['weight'] == tree_best(n, w, edges) and len(set(sel)) == len(sel)
              and sum(w[i] for i in sel) == ans['weight']
              and not any(u in sel and v in sel for u, v in edges))
        expect(ok, f'tree {n} {w} {edges} -> {ans}')
    elif k == 'search':
        _, t, p = m
        expect(ans['positions'] == occ(t, p), f'{r["task"]} {t!r} {p!r} -> {ans}')
    elif k == 'suffix':
        t = m[1]
        sa, lcp = suffix_oracle(t)
        rep = ans['repeated']
        best = max(lcp, default=0)
        ok = ans['sa'] == sa and ans['lcp'] == lcp and len(rep) == best and (best == 0 or t.count(rep) >= 1 and len(occ(t, rep)) >= 2)
        expect(ok, f'suffix {t!r} -> {ans}')
    elif k == 'aho':
        expect(ans['counts'] == aho_oracle(m[1], m[2]), f'aho {m[1]} {m[2]} -> {ans}')


fixed = [
    ({'task': 'rk', 'text': 'abdj', 'pattern': 'ab'}, {'positions': [0]}),           
    ({'task': 'rk', 'text': 'dj', 'pattern': 'ab'}, {'positions': []}),               
    ({'task': 'naive', 'text': '', 'pattern': ''}, {'positions': [0]}),
    ({'task': 'kmp', 'text': 'abc', 'pattern': ''}, {'positions': [0, 1, 2, 3]}),
    ({'task': 'rk', 'text': 'ab', 'pattern': 'abc'}, {'positions': []}),
    ({'task': 'kmp', 'text': 'aaaaa', 'pattern': 'aaa'}, {'positions': [0, 1, 2]}),
    ({'task': 'suffix', 'text': 'banana'}, {'sa': [5, 3, 1, 0, 4, 2], 'lcp': [0, 1, 3, 0, 0, 2], 'repeated': 'ana'}),
    ({'task': 'suffix', 'text': 'abc'}, {'repeated': ''}),
    ({'task': 'aho', 'patterns': ['he', 'she', 'he'], 'texts': ['sheshe', 'sheshe', '']}, {'counts': [[2, 2, 2], [2, 2, 2], [0, 0, 0]]}),
    ({'task': 'aho', 'patterns': [], 'texts': ['abc', '']}, {'counts': [[], []]}),
    ({'task': 'aho', 'patterns': ['a'], 'texts': []}, {'counts': []}),
    ({'task': 'tree', 'n': 3, 'weights': [-1, -2, -3], 'edges': [[0, 1], [1, 2]]}, {'weight': 0, 'selected': []}),
    ({'task': 'tree', 'n': 0, 'weights': [], 'edges': []}, {'weight': 0, 'selected': []}),
    ({'task': 'merge', 'sizes': [2, 3, 4]}, {'cost': 14}),
    ({'task': 'merge', 'sizes': []}, {'cost': 0, 'plan': []}),
    ({'task': 'merge', 'sizes': [7]}, {'cost': 0, 'plan': []}),
    ({'task': 'lcs', 'a': '', 'b': 'abc'}, {'length': 0, 'sequence': ''}),
    ({'task': 'lcs', 'a': 'tea_time', 'b': 'team'}, {'length': 4, 'sequence': 'team'}),
]
for (req, exp), ans in zip(fixed, run([f[0] for f in fixed])):
    expect(all(ans[k] == v for k, v in exp.items()), f'fixed {req} -> {ans}')


n = 100000
chain = {'task': 'tree', 'n': n, 'weights': [1] * n, 'edges': [[i, i+1] for i in range(n-1)]}
star = {'task': 'tree', 'n': n, 'weights': [1000000000] * n, 'edges': [[0, i] for i in range(1, n)]}
big_sum = {'task': 'merge', 'sizes': [10**9] * 200}
long_a = {'task': 'kmp', 'text': 'a' * 200000, 'pattern': 'a' * 100000}
sa_big = {'task': 'suffix', 'text': 'a' * 50000}
aho_dense = {'task': 'aho', 'patterns': ['a' * i for i in range(1, 400)] + ['a'] * 5, 'texts': ['a' * 2000] * 100}
c, s, b, k, sf, ad = run([chain, star, big_sum, long_a, sa_big, aho_dense])
expect(c['weight'] == (n + 1) // 2 and len(c['selected']) == (n + 1) // 2, 'deep chain')
expect(s['weight'] == (n - 1) * 10**9 and 0 not in s['selected'], 'star: leaves beat centre')
expect(b['cost'] > 2**31 and len(b['plan']) == 199, 'merge: costs exceed 32 bits')
expect(len(k['positions']) == 100001, 'kmp dense overlaps')
expect(sf['sa'] == list(range(49999, -1, -1)) and sf['repeated'] == 'a' * 49999, 'suffix all equal')
expect(ad['counts'][0][0] == 2000 and ad['counts'][99][398] == 2000 - 399 + 1, 'aho dense')

print('ALL OWN TESTS PASSED' if not fails else f'{fails} FAILURES')
sys.exit(1 if fails else 0)
