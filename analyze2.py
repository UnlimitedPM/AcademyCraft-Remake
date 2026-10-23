import math, collections
f='src/main/resources/assets/academy/models/windgen_fan.obj'
V=[];VN=[];F=[]
for line in open(f):
    t=line.strip()
    if not t or t.startswith('#'): continue
    p=t.split()
    if p[0]=='v': V.append(tuple(float(x) for x in p[1:4]))
    elif p[0]=='vn': VN.append(tuple(float(x) for x in p[1:4]))
    elif p[0]=='f':
        tri=[]
        for tok in p[1:]:
            q=tok.split('/')
            tri.append((int(q[0]), int(q[2]) if len(q)>2 and q[2] else None))
        F.append(tri)

v2n=collections.defaultdict(set)
for tri in F:
    for vi,ni in tri:
        v2n[vi].add(ni)

def ang(a,b):
    d=sum(x*y for x,y in zip(a,b))
    la=math.sqrt(sum(x*x for x in a)); lb=math.sqrt(sum(x*x for x in b))
    if la<1e-9 or lb<1e-9: return 999
    c=max(-1,min(1,d/(la*lb)))
    return math.degrees(math.acos(c))

print('=== per-vertex normal spread (deg) ===')
rows=[]
for vi in sorted(v2n):
    ns=[VN[n-1] for n in v2n[vi] if n]
    if len(ns)<2: spread=0.0
    else:
        spread=max(ang(a,b) for i,a in enumerate(ns) for b in ns[i+1:])
    rows.append((vi,len(ns),spread))
for vi,c,s in rows:
    print(f'v{vi:3d} normals={c} spread={s:7.2f}  pos={V[vi-1]}')
