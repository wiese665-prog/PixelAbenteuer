"""Prepare the approved dimensional inn with a hard, fringe-free native cutout."""
from pathlib import Path
from PIL import Image,ImageDraw
import json
ROOT=Path(__file__).resolve().parents[1];A=ROOT/'app/src/main/assets'
source=Image.open(ROOT/'art/source/inn-approved.png').convert('RGBA')
source.putalpha(source.getchannel('A').point(lambda a:255 if a>=128 else 0))
source=source.crop(source.getbbox());w=200;h=round(source.height*w/source.width)
sprite=source.resize((w,h),Image.Resampling.NEAREST);raw=sprite.copy()
allowed=set()
for path in [A/'sprites/Exterior_house.png',A/'village/exterior.png',A/'village/guild/Exterior(1).png']:
 allowed.update(p[:3] for p in Image.open(path).convert('RGBA').get_flattened_data() if p[3] and .2126*p[0]+.7152*p[1]+.0722*p[2]>=38)
allowed=sorted(allowed);cache={}
def nearest(c):
 if c not in cache:cache[c]=min(allowed,key=lambda q:sum((a-b)**2 for a,b in zip(c,q)))
 return cache[c]
for y in range(h):
 for x in range(w):
  p=raw.getpixel((x,y))
  if not p[3]:continue
  neighbors=[(xx,yy) for xx,yy in [(x-1,y),(x+1,y),(x,y-1),(x,y+1)] if 0<=xx<w and 0<=yy<h and raw.getpixel((xx,yy))[3]]
  light=sum(c*k for c,k in zip(p[:3],(.2126,.7152,.0722)))
  if len(neighbors)<4 and (light<45 or len(neighbors)<=1):sprite.putpixel((x,y),(0,0,0,0));continue
  rgb=p[:3]
  if len(neighbors)<4 and light<65:
   candidates=[raw.getpixel((xx,yy))[:3] for xx,yy in neighbors if sum(c*k for c,k in zip(raw.getpixel((xx,yy))[:3],(.2126,.7152,.0722)))>=65]
   if candidates:rgb=candidates[0]
  sprite.putpixel((x,y),(*nearest(rgb),255))
out=Image.new('RGBA',(200,144));out.alpha_composite(sprite,(0,144-h));out=__import__('native_materials').polish_materials(out);out.save(A/'village/inn.png')
# The actual approved wooden door leaves: open them without replacing the arch.
# Six fully aligned frames: closed, opening, open, open, closing, closed.
box=(89,111,110,139);door=out.crop(box);strip=Image.new('RGBA',(200,864))
for f,opening in enumerate([0,.5,1,1,.5,0]):
 frame=out.copy()
 if opening:
  x,y,r,b=box;d=ImageDraw.Draw(frame)
  # Restrict the opening to the arched door leaf silhouette.
  mask=Image.new('L',(r-x,b-y));md=ImageDraw.Draw(mask)
  for yy in range(b-y):
   xs=[xx for xx in range(r-x) if (lambda p:p[3] and p[0]>p[1]*1.15 and p[0]>p[2]*1.2)(door.getpixel((xx,yy)))]
   if xs:md.line((min(xs),yy,max(xs),yy),fill=255)
  dark=Image.new('RGBA',door.size,(*nearest((34,30,39)),255));frame.paste(dark,(x,y),mask)
  leafw=max(2,round((r-x)/2*(1-opening*.85)))
  masked=door.copy();masked.putalpha(mask)
  left=masked.crop((0,0,11,b-y)).resize((leafw,b-y),Image.Resampling.NEAREST)
  right=masked.crop((11,0,r-x,b-y)).resize((leafw,b-y),Image.Resampling.NEAREST)
  frame.alpha_composite(left,(x,y));frame.alpha_composite(right,(r-leafw,y))
 strip.alpha_composite(frame,(0,f*144))
strip.save(A/'village/door2.png')
(ROOT/'art/source/inn-palette.json').write_text(json.dumps(sorted({p[:3] for p in out.get_flattened_data() if p[3]})))
print('Approved inn: 200x144, native palette, binary alpha, trimmed border, six aligned door frames.')
