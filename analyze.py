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
print('v',len(V),'vn',len(VN),'f',len(F))
# vertex -> set of normals
v2n=collections.defaultdict(set)
for tri in F:
    for vi,ni in tri:
        v2n[vi].add(ni)
multi=[vi for vi,s in v2n.items() if len(s)>1]
print('vertices with >1 normal:',len(multi),'of',len(v2n))
print('first 40 such:',sorted(multi)[:40])
# normal -> set of vertices
n2v=collections.defaultdict(set)
for tri in F:
    for vi,ni in tri:
        n2v[ni].add(vi)
print('normals used by >1 vertex:',sum(1 for s in n2v.values() if len(s)>1),'of',len(n2v))
# hub vertices 1..24 : how many normals each
print('--- hub v1..24 normal counts ---')
for vi in range(1,25):
    print(vi, V[vi-1], sorted(v2n[vi]))
