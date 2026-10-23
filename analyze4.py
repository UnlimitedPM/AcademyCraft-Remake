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

def sub(a,b): return tuple(x-y for x,y in zip(a,b))
def cross(a,b): return (a[1]*b[2]-a[2]*b[1], a[2]*b[0]-a[0]*b[2], a[0]*b[1]-a[1]*b[0])
def norm(a):
    l=math.sqrt(sum(x*x for x in a))
    return tuple(x/l for x in a) if l>1e-12 else (0,0,0)
def ang(a,b):
    d=sum(x*y for x,y in zip(a,b))
    c=max(-1,min(1,d))
    return math.degrees(math.acos(c))

# hub = vertices 1..26
HUB=set(range(1,27))
hubfaces=[(i,t) for i,t in enumerate(F,1) if all(v in HUB for v,_ in t)]
print('faces entirely in hub:',len(hubfaces),'of',len(F))
print('first hub face idx:',hubfaces[0][0],'last:',hubfaces[-1][0])

print()
print('=== geometric vs assigned normal per hub face ===')
bad=0
for idx,tri in hubfaces:
    pts=[V[v-1] for v,_ in tri]
    gn=norm(cross(sub(pts[1],pts[0]), sub(pts[2],pts[0])))
    ns=[VN[n-1] for _,n in tri if n]
    # assigned normals in this face
    uniq=set(tuple(round(x,4) for x in n) for n in ns)
    devs=[ang(gn,n) for n in ns]
    flag='' if max(devs)<2 else '  <-- MISMATCH'
    if max(devs)>=2: bad+=1
    print(f'f{idx:3d} v={[v for v,_ in tri]} n={[n for _,n in tri]} geo={tuple(round(x,3) for x in gn)} dev={[round(d,1) for d in devs]}{flag}')
print('faces whose assigned normal deviates >2deg from geometry:',bad)
