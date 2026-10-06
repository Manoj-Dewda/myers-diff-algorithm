"""Generate big test pairs into tools/big/."""
import os, random
rng = random.Random(42)
os.makedirs('tools/big', exist_ok=True)

def src_line(i):
    k = rng.random()
    if k < 0.08: return ''
    if k < 0.15: return '    }'
    return f'    int v{rng.randint(0, 10**9)} = compute({i}, "{rng.random():.6f}");'

def write(name, lines):
    with open(f'tools/big/{name}', 'w', newline='\n') as f:
        f.write('\n'.join(lines) + ('\n' if lines else ''))

def mutate(lines, changes, new_line):
    out = list(lines)
    for _ in range(changes):
        p = rng.randrange(len(out))
        r = rng.random()
        if r < 0.4: out[p] = new_line(p)
        elif r < 0.7: del out[p]
        else: out.insert(p, new_line(p))
    return out

base = [src_line(i) for i in range(500_000)]
write('A1', base); write('B1', mutate(base, 50, src_line))            # few changes
write('A2', base); write('B2', mutate(base, 3000, src_line))          # many scattered changes
write('A3', base); write('B3', base[:100_000] + base[110_000:400_000] + base[100_000:110_000] + base[400_000:])
write('A7', base); write('B7', mutate(base, 20000, src_line))         # 20k changes, mostly unique lines

small = ['{', '}', '', 'return x;', 'x++;', 'if (x) {', 'else', 'break;', '// note', 'y = 0;']
pick = lambda p: rng.choice(small)
s = [pick(0) for _ in range(200_000)]
write('A4', s); write('B4', mutate(s, 1000, pick))                     # only 10 distinct lines
write('A5', []); write('B5', base)                                      # empty vs big
write('A6', [src_line(i) for i in range(20_000)]); write('B6', [src_line(i) for i in range(20_000)])  # all different
s2 = [pick(0) for _ in range(500_000)]
write('A8', s2); write('B8', mutate(s2, 3000, pick))                   # 500k lines, 10 distinct, 3000 changes
print('done')
