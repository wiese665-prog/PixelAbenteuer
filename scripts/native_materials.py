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
 return out
