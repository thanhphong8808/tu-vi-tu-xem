"""Kiểm tra mọi tham chiếu từ classes.dex sang lớp hệ thống (java.*, android.*, …) đều tồn tại trong android.jar.
Dùng: python3 check_api.py classes.dex android.jar [android.jar khác…]
Hàm/trường không tìm thấy (kể cả khi tra ngược lên lớp cha, interface) → in ra và thoát mã 1."""
import re, subprocess, struct, sys, zipfile

dex, jars = sys.argv[1], sys.argv[2:]
out = subprocess.run(['dexdump', '-d', dex], capture_output=True, text=True, errors='replace').stdout
defined = set(re.findall(r"Class descriptor\s+: '(L[^']+;)'", out))
meth_re = re.compile(r"\|[0-9a-f]{4}: invoke-[a-z/-]+ \{[^}\n]*\}, (L[^;\n]+;)\.([^:\n]+):(\([^ \n]*)")
field_re = re.compile(r"\|[0-9a-f]{4}: [is](?:get|put)[a-z-]* [^,\n]+(?:, [^,\n]+)?, (L[^;\n]+;)\.([^:\n]+):(\S+)")
refs = set()
for m in meth_re.finditer(out): refs.add(('M',) + m.groups())
for m in field_re.finditer(out): refs.add(('F',) + m.groups())
ext = {r for r in refs if r[1] not in defined}

def parse(b):
    cp = [None]; i = 10; n = struct.unpack('>H', b[8:10])[0]; k = 1
    while k < n:
        t = b[i]
        if t == 1: l = struct.unpack('>H', b[i+1:i+3])[0]; cp.append(('U', b[i+3:i+3+l].decode('utf-8', 'replace'))); i += 3 + l
        elif t in (3, 4): cp.append(None); i += 5
        elif t in (5, 6): cp += [None, None]; i += 9; k += 1
        elif t == 7: cp.append(('C', struct.unpack('>H', b[i+1:i+3])[0])); i += 3
        elif t in (8, 16, 19, 20): cp.append(None); i += 3
        elif t in (9, 10, 11, 12, 17, 18): cp.append(None); i += 5
        elif t == 15: cp.append(None); i += 4
        else: raise ValueError(t)
        k += 1
    u = lambda j: cp[j][1]
    cname = lambda j: u(cp[j][1]) if j else None
    i += 2; this = cname(struct.unpack('>H', b[i:i+2])[0]); sup = cname(struct.unpack('>H', b[i+2:i+4])[0]); i += 4
    ni = struct.unpack('>H', b[i:i+2])[0]; i += 2
    itf = [cname(struct.unpack('>H', b[i+2*j:i+2*j+2])[0]) for j in range(ni)]; i += 2 * ni
    members = {}
    for kind in ('F', 'M'):
        cnt = struct.unpack('>H', b[i:i+2])[0]; i += 2
        for _ in range(cnt):
            _, ni_, di, ac = struct.unpack('>HHHH', b[i:i+8]); i += 8
            members.setdefault(kind, set()).add((u(ni_), u(di)))
            for _ in range(ac):
                l = struct.unpack('>I', b[i+2:i+6])[0]; i += 6 + l
    return sup, itf, members

classes = {}
for j in jars:
    z = zipfile.ZipFile(j)
    for n in z.namelist():
        if n.endswith('.class'):
            classes.setdefault(n[:-6], (z, n))
cache = {}
def info(c):
    if c not in cache:
        e = classes.get(c)
        cache[c] = parse(e[0].read(e[1])) if e else None
    return cache[c]
def has(c, kind, name, desc, seen=None):
    seen = seen or set()
    if c is None or c in seen: return False
    seen.add(c)
    i = info(c)
    if i is None: return None
    sup, itf, mem = i
    if kind == 'M':
        if any(n == name and d.startswith(desc.split(')')[0] + ')') and d == desc for n, d in mem.get('M', ())): return True
    elif any(n == name and d == desc for n, d in mem.get('F', ())): return True
    return any(has(x, kind, name, desc, seen) for x in [sup] + itf if x)
missing, unknown = [], set()
for kind, cls, name, desc in sorted(ext):
    c = cls[1:-1]
    r = has(c, kind, name, desc)
    if r is None: unknown.add(c)
    elif not r: missing.append(f'{kind} {c}.{name}{desc if kind=="M" else ":"+desc}')
sysunknown = sorted(u for u in unknown if re.match(r'(java|javax|android|dalvik|org/json|org/w3c|org/xml)/', u))
print(f'Tham chiếu ra ngoài DEX: {len(ext)} · không thấy hàm/trường: {len(missing)} · lớp hệ thống không có: {len(sysunknown)}')
for m in missing: print('  THIẾU', m)
for u in sysunknown: print('  THIẾU LỚP', u)
others = sorted(u for u in unknown if u not in sysunknown)
if others: print('  (lớp ngoài hệ thống, không có trong DEX — chỉ lỗi nếu được gọi):', len(others), others[:15])
sys.exit(1 if missing or sysunknown else 0)
