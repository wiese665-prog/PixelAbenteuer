"""Shared native building materials: restrained roof ramps and muted masonry."""
from pathlib import Path
from PIL import Image
from collections import Counter
ROOT=Path(__file__).resolve().parents[1];A=ROOT/'app/src/main/assets'
def polish_materials(im):
 allowed=Counter()
 for path in [A/'sprites/Exterior_house.png',A/'village/exterior.png',A/'village/guild/Exterior(1).png']:
  allowed.update(p[:3] for p in Image.open(path).convert('RGBA').get_flattened_data() if p[3] and sum(c*k for c,k in zip(p[:3],(.2126,.7152,.0722)))>=38)
 roof=[(74,30,46),(91,36,50),(117,44,56),(135,49,58),(154,54,61)]
 masonry=[c for c,n in allowed.most_common() if c[2]>c[0] and c[2]-c[0]<45 and abs(c[1]-c[0])<20][:10]
 palette=list(allowed);cache={};out=im.copy()
 for y in range(im.height):
  for x in range(im.width):
   p=im.getpixel((x,y))
   if not p[3]:continue
   c=p[:3]
   if c not in cache:
    r,g,b=c;pool=palette;target=c
    if r>g*1.4 and r>b*1.3 and b>=g*.9:pool=roof
    elif b>r and b-r<45 and abs(g-r)<20:pool=masonry;target=(r-5,g-7,b-10)
    elif b>r*1.35 and g>r*1.1:target=(r,g-10,b-5)
    cache[c]=min(pool,key=lambda q:sum((a-b)**2 for a,b in zip(target,q)))
   out.putpixel((x,y),(*cache[c],255))
 # Reuse the actual original 16px roof textures, preserving silhouette,
 # eaves, valleys and raised ridge pixels from the approved geometry.
 reference=Image.open(A/'sprites/Exterior_house.png').convert('RGBA')
 leftRoof=reference.crop((16,56,32,72));rightRoof=reference.crop((120,60,136,76))
 raw=out.copy()
 def red(c):return c[3]>0 and c[0]>c[1]*1.4 and c[0]>c[2]*1.3 and c[2]>=c[1]*.9
 def luminance(c):return sum(a*b for a,b in zip(c[:3],(.2126,.7152,.0722)))
 for y in range(1,out.height-1):
  for x in range(1,out.width-1):
   c=raw.getpixel((x,y))
   if not red(c):continue
   near=[raw.getpixel((x+dx,y+dy)) for dx,dy in [(-1,0),(1,0),(0,-1),(0,1)]]
   if not all(red(p) for p in near):continue
   ridges=[out.width*.5]+([out.width*.25,out.width*.73] if out.width==200 else [])
   if any(abs(x-ridge)<1.1 for ridge in ridges):continue
   around=[raw.getpixel((xx,yy)) for yy in range(max(0,y-3),min(out.height,y+4)) for xx in range(max(0,x-3),min(out.width,x+4)) if red(raw.getpixel((xx,yy)))]
   texture=leftRoof if sum(luminance(p) for p in around)/len(around)>55 else rightRoof
   native=texture.getpixel((x%16,y%16))
   if not red(native):
    candidates=[texture.getpixel((xx,yy)) for yy in range(16) for xx in range(16) if red(texture.getpixel((xx,yy)))]
    native=min(candidates,key=lambda p:abs(luminance(p)-luminance(c)))
   out.putpixel((x,y),(*min(roof,key=lambda q:sum((a-b)**2 for a,b in zip(native[:3],q))),255))
 glass={p[:3] for p in reference.crop((47,100,62,115)).get_flattened_data() if p[3] and p[2]-p[0]>45}
 if glass:
  for y in range(out.height):
   for x in range(out.width):
    c=out.getpixel((x,y))
    if c[3] and c[2]-c[0]>45:out.putpixel((x,y),(*min(glass,key=lambda q:sum((a-b)**2 for a,b in zip(c[:3],q))),255))
 return out
