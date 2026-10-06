import sys
A = open(sys.argv[1], 'rb').read().split(b'\n'); A = A[:-1] if A and A[-1] == b'' else A
B = open(sys.argv[2], 'rb').read().split(b'\n'); B = B[:-1] if B and B[-1] == b'' else B
out = open(sys.argv[3], 'rb').read().split(b'\n'); assert out[-1] == b''; out.pop()
ra, rb, state, edits = [], [], ' ', 0
for ln in out:
    p, body = ln[:1], ln[1:]
    if p == b'?': continue
    if p == b' ': ra.append(body); rb.append(body); state = ' '
    elif p == b'-': assert state != '+', 'delete after insert'; ra.append(body); state = '-'; edits += 1
    elif p == b'+': rb.append(body); state = '+'; edits += 1
    else: raise SystemExit('bad prefix')
assert ra == A, 'does not rebuild A'; assert rb == B, 'does not rebuild B'
print('valid', sys.argv[3].split('/')[-1], 'edits', edits)
