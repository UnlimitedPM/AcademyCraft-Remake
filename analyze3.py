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

def fmt(n): return '(' + ', '.join(f'{x:+.4f}' for x in n) + ')'
def ang(a,b):
    d=sum(x*y for x,y in zip(a,b))
    la=math.sqrt(sum(x*x for x in a)); lb=math.sqrt(sum(x*x for x in b))
    c=max(-1,min(1,d/(la*lb)))
    return math.degrees(math.acos(c))

print('=== HUB ring normals ===')
for vi in [1,2,9,10,17,18,25,26]:
    print(f'v{vi} pos={V[vi-1]}')
    for n in sorted(v2n[vi]):
        print(f'   n{n} {fmt(VN[n-1])}')

print()
print('=== BLADE 1 sample normals ===')
for vi in [27,28,29,30,31,32,35,36,39,40,41,44,45,47,51,53]:
    print(f'v{vi} pos={V[vi-1]}')
    for n in sorted(v2n[vi]):
        print(f'   n{n} {fmt(VN[n-1])}')

print()
print('=== duplicate normal directions (same vector, different index) ===')
buckets=collections.defaultdict(list)
for i,n in enumerate(VN,1):
    key=tuple(round(x,4) for x in n)
    buckets[key].append(i)
dups={k:v for k,v in buckets.items() if len(v)>1}
print('distinct directions:',len(buckets),'of',len(VN),'indices;',len(dups),'directions duplicated')
for k,v in list(dups.items())[:20]:
    print('  ',k,'->',v)
