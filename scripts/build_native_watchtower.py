"""Keep approved tower geometry, match native palette and remove black outer contour."""
from PIL import Image
from pathlib import Path
import json
root=Path(__file__).resolve().parents[1];A=root/'app/src/main/assets'
source=Image.open(root/'art/source/watchtower-approved.png').convert('RGBA')
source.putalpha(source.getchannel('A').point(lambda a:255 if a>=128 else 0))
source=source.crop(source.getbbox());h=204;w=round(source.width*h/source.height)
sprite=source.resize((w,h),Image.Resampling.NEAREST)
# Native source material palette, excluding almost-black outline colors.
allowed=set()
for path in [A/'sprites/Exterior_house.png',A/'village/exterior.png',A/'village/guild/Exterior(1).png']:
 for r,g,b,a in Image.open(path).convert('RGBA').get_flattened_data():
  if a>=128 and .2126*r+.7152*g+.0722*b>=38:allowed.add((r,g,b))
allowed=sorted(allowed);original=sprite.copy();cache={}
def nearest(rgb):
 if rgb not in cache:cache[rgb]=min(allowed,key=lambda c:sum((a-b)**2 for a,b in zip(rgb,c)))
 return cache[rgb]
for y in range(h):
 for x in range(w):
  r,g,b,a=original.getpixel((x,y))
  if not a:continue
  boundary=any(nx<0 or ny<0 or nx>=w or ny>=h or original.getpixel((nx,ny))[3]==0 for nx,ny in [(x-1,y),(x+1,y),(x,y-1),(x,y+1)])
  if boundary and .2126*r+.7152*g+.0722*b<60:
   inside=[]
   for yy in range(max(0,y-3),min(h,y+4)):
    for xx in range(max(0,x-3),min(w,x+4)):
     p=original.getpixel((xx,yy))
     if p[3] and .2126*p[0]+.7152*p[1]+.0722*p[2]>=65:inside.append(((xx-x)**2+(yy-y)**2,p[:3]))
   if inside:r,g,b=min(inside,key=lambda p:p[0])[1]
  sprite.putpixel((x,y),(*nearest((r,g,b)),255))
out=Image.new('RGBA',(160,208));out.alpha_composite(sprite,((160-w)//2,208-h))
out.save(root/'art/source/watchtower-native.png');out.save(A/'village/tower.png')
colors=sorted({p[:3] for p in out.get_flattened_data() if p[3]})
(root/'art/source/watchtower-palette.json').write_text(json.dumps(colors,separators=(',',':'))+'\n')
print('Tower: approved geometry, original game palette, hard pixel edges, material-colored outer contour.')
