"""End-to-end checker for the Java diff. Run: python3 tools/check.py [cases]"""
import os, random, re, subprocess, sys, tempfile

RANGE = re.compile(r'^(\.|(0|[1-9][0-9]*)-(0|[1-9][0-9]*)(,(0|[1-9][0-9]*)-(0|[1-9][0-9]*))*)$')


def split_lines(data: bytes):
    parts = data.split(b'\n')
    if parts and parts[-1] == b'':
        parts.pop()
    return parts


def lcs(x, y):
    prev = [0] * (len(y) + 1)
    for xi in x:
        cur = [0]
        for j, yj in enumerate(y):
            cur.append(prev[j] + 1 if xi == yj else max(prev[j + 1], cur[j]))
        prev = cur
    return prev[-1]


def parse_ranges(s):
    assert RANGE.match(s), f'bad range syntax {s!r}'
    if s == '.':
        return []
    out = []
    for part in s.split(','):
        a, b = map(int, part.split('-'))
        assert a < b, f'empty range {part}'
        out.append((a, b))
    for (a1, b1), (a2, b2) in zip(out, out[1:]):
        assert b1 < a2, f'ranges overlap or touch: {s}'
    return out


def remove(chars, ranges):
    drop = set()
    for a, b in ranges:
        assert b <= len(chars), f'range {a}-{b} past end of line of {len(chars)}'
        drop.update(range(a, b))
    return [c for i, c in enumerate(chars) if i not in drop], len(drop)


def check(A: bytes, B: bytes, out: bytes, highlight: bool):
    la, lb = split_lines(A), split_lines(B)
    lines = out.split(b'\n')
    assert lines[-1] == b'', 'output must end with newline (or be empty)'
    lines.pop()
    ra, rb, edits = [], [], 0
    block_d, block_i, state = [], [], 'keep'
    i = 0
    while i < len(lines):
        ln = lines[i]
        p, body = ln[:1], ln[1:]
        if p == b' ':
            ra.append(body); rb.append(body); block_d, block_i, state = [], [], 'keep'
        elif p == b'-':
            assert state != 'ins', 'delete after insert inside a block'
            ra.append(body); edits += 1; block_d.append(body); state = 'del'
        elif p == b'+':
            rb.append(body); edits += 1; block_i.append(body); state = 'ins'
            k = len(block_i) - 1
            if highlight and k < len(block_d):
                i += 1
                q = lines[i].decode()
                assert q.startswith('? ') and ' | ' in q, f'bad ? line {q!r}'
                left, right = q[2:].split(' | ')
                old = list(block_d[k].decode()); new = list(body.decode())
                r1, r2 = parse_ranges(left), parse_ranges(right)
                o2, c1 = remove(old, r1); n2, c2 = remove(new, r2)
                assert o2 == n2, f'not equal after removing: {q}'
                assert c1 + c2 == len(old) + len(new) - 2 * lcs(old, new), f'not minimal highlight {q}'
        else:
            raise AssertionError(f'bad prefix in {ln!r}')
        i += 1
    assert ra == la, 'output does not rebuild A'
    assert rb == lb, 'output does not rebuild B'
    assert edits == len(la) + len(lb) - 2 * lcs(la, lb), f'not minimal: {edits} edits'


def run(mode, pa, pb):
    r = subprocess.run(['java', '-cp', 'out', 'Main', mode, pa, pb], capture_output=True)
    assert r.returncode == 0, r.stderr
    return r.stdout


def random_file(rng, pool):
    n = rng.randint(0, 25)
    data = b'\n'.join(rng.choice(pool) for _ in range(n))
    if n and rng.random() < 0.7:
        data += b'\n'
    return data


def main():
    cases = int(sys.argv[1]) if len(sys.argv) > 1 else 200
    rng = random.Random(7)
    words = ['a', 'b', 'c', 'x = 1', 'x = 10', 'port = 8000', 'port = 8080', 'héllo', 'hi 😀', 'hi 😃', '', 'a\r', '}']
    tmp = tempfile.mkdtemp()
    pa, pb = os.path.join(tmp, 'A'), os.path.join(tmp, 'B')
    for c in range(cases):
        pool = [w.encode() for w in rng.sample(words, rng.randint(2, 6))]
        if c % 5 == 0:
            pool.append(b'\xff\xfe bad')            # invalid UTF-8, lines tests only
        A, B = random_file(rng, pool), random_file(rng, pool)
        open(pa, 'wb').write(A); open(pb, 'wb').write(B)
        modes = ['lines'] if c % 5 == 0 else ['lines', 'highlight']
        for mode in modes:
            out = run(mode, pa, pb)
            try:
                check(A, B, out, mode == 'highlight')
            except AssertionError as e:
                print('FAIL', mode, e, '\nA=', A, '\nB=', B, '\nOUT=', out)
                sys.exit(1)
    print(f'all {cases} random cases passed')


if __name__ == '__main__':
    main()
