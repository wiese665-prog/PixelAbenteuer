"""New native-grid headgear silhouettes. Coordinates follow each original head frame."""
from PIL import Image,ImageDraw
from pathlib import Path
from functools import lru_cache
O=(24,24,24,255)
GREEN=[(54,74,63,255),(91,127,106,255),(168,200,152,255)]
BLUE=[(25,51,100,255),(58,103,151,255),(89,153,189,255)]
STEEL=[(82,82,95,255),(124,137,137,255),(167,181,167,255),(220,220,203,255)]
WOOD=[(88,56,40,255),(152,96,56,255),(240,152,72,255)]
LEATHER=[(61,18,0,255),(99,46,17,255),(141,99,64,255),(218,185,136,255)]
BRASS=[(129,92,33,255),(178,145,73,255),(224,210,142,255)]

def headgear(name,cap,facing):
 return native_form(name,cap,facing) if name=='mgHt' else tailored_form(name,cap,facing)

@lru_cache(None)
def source(kind,facing):
 root=Path(__file__).resolve().parents[1]/'app/src/main/assets/sprites/ManaSeedCharacter/char_a_p1'
 layer='4har' if kind=='bob1' else '5hat'
 sheet=Image.open(root/layer/f'char_a_p1_{layer}_{kind}_v01.png').convert('RGBA')
 return sheet.crop((0,facing*64,64,facing*64+64))

def native_form(name,cap,facing):
 # Original pixel clusters and lighting, fitted to the current native head pose.
 reference=source('bob1',facing).getbbox();target=cap.getbbox()
 sx=round((reference[0]+reference[2]-1)/2);cx=round((target[0]+target[2]-1)/2)
 dx=cx-sx;dy=target[1]-reference[1];top=target[1]
 kind='pnty' if name=='mgHt' else 'pfht';original=source(kind,facing)
 colors=sorted({p[:3] for p in original.get_flattened_data() if p[3]},key=lambda p:sum(p))
 if name=='mgHt':
  mapping={(24,24,24):O,(33,19,33):O,(30,43,65):BLUE[0],(60,85,117):BLUE[1],(98,147,173):BLUE[2],(153,214,227):(144,160,176,255),(91,24,57):BRASS[0],(141,34,70):BRASS[1],(190,86,103):BRASS[2]}
 else:
  ramp={'hood':[O,GREEN[0],GREEN[1],GREEN[1],GREEN[2]],'wdHl':[O,LEATHER[0],LEATHER[1],LEATHER[2],LEATHER[3]],'leHl':[O,*LEATHER]}[name]
  mapping=dict(zip(colors,ramp))
 out=Image.new('RGBA',(64,64))
 for y in range(64):
  for x in range(64):
   px=original.getpixel((x,y));xx=x+dx;yy=y+dy
   if px[3] and 0<=xx<64 and 0<=yy<64:
    # Round wooden skullcap; leather has open ears and a short rear flap.
    if name=='wdHl' and yy>top+(11 if facing==1 else 8):continue
    if name=='leHl' and yy>top+(13 if facing==1 else 7):continue
    out.putpixel((xx,yy),mapping[px[:3]])
 d=ImageDraw.Draw(out)
 def line(pts,color):d.line([(cx+x,top+y) for x,y in pts],fill=color)
 def poly(pts,color):d.polygon([(cx+x,top+y) for x,y in pts],fill=color)
 if name=='hood':
  # Wide folded opening and shoulder drape replace the tight round face ring.
  if facing==0:
   poly([(-5,11),(-3,13),(1,14),(5,11),(7,14),(3,16),(-4,16),(-7,14)],GREEN[0])
   line([(-5,13),(-2,14),(2,15),(5,13)],GREEN[1]);line([(-3,14),(0,15)],GREEN[2])
  elif facing==1:
   poly([(-5,11),(5,11),(7,15),(2,16),(-4,16),(-7,14)],GREEN[0])
   line([(-4,12),(-1,14),(2,15),(5,13)],GREEN[1])
  else:
   sign=1 if facing==2 else -1
   poly([(sign*-5,10),(sign*0,12),(sign*4,14),(sign*2,16),(sign*-5,15),(sign*-7,13)],GREEN[0])
   line([(sign*-5,12),(sign*-2,13),(sign*1,15)],GREEN[1])
 elif name=='wdHl':
  # Curved slats follow the dome, with a narrow leather fastening strap.
  for off in (-3,2):
   for y in range(top+2,top+7):
    x=cx+off
    if out.getpixel((x,y))[3]:d.point((x,y),fill=LEATHER[0])
  line([(-5,7),(0,8),(5,7)],LEATHER[0]);d.point((cx-4,top+7),fill=BRASS[1])
 elif name=='leHl':
  # Padded travelling cap: cloth-like panels and an offset leather fastening.
  line([(-3,2),(-2,4),(0,6)],LEATHER[1])
  for x,y in [(-3,3),(-1,5),(2,6)]:d.point((cx+x,top+y),fill=LEATHER[3])
  if facing!=1:
   side=-1 if facing==3 else 1
   line([(side*5,7),(side*5,10),(side*3,12)],LEATHER[0])
 if name=='mgHt' and facing==0:
  # Reverse only the existing gold arc; retain the approved hat alpha and other views.
  gold={(x,y):out.getpixel((x,y)) for y in range(64) for x in range(64) if out.getpixel((x,y)) in BRASS}
  if gold:
   low=min(y for x,y in gold);high=max(y for x,y in gold)
   for x,y in gold:out.putpixel((x,y),BLUE[1])
   for (x,y),color in gold.items():
    yy=low+high-y
    if out.getpixel((x,yy))[3]:out.putpixel((x,yy),color)
 return out

def tailored_form(name,cap,facing):
 box=cap.getbbox();cx=round((box[0]+box[2]-1)/2);top=box[1]
 out=Image.new('RGBA',(64,64));d=ImageDraw.Draw(out)
 def p(pts,col):d.polygon([(cx+x,top+y) for x,y in pts],fill=col)
 def l(pts,col):d.line([(cx+x,top+y) for x,y in pts],fill=col)
 profile=facing in (2,3)
 if name=='irHl':
  # A rounded bowl: short one-pixel steps instead of the former pointed ridge.
  p([(-7,4),(-6,1),(-4,-1),(-2,-2),(2,-2),(4,-1),(6,1),(7,4),(7,7),(-7,7)],STEEL[0])
  p([(-6,4),(-5,1),(-3,0),(3,0),(5,1),(6,4),(6,5),(-6,5)],STEEL[1])
  p([(-4,2),(-2,0),(2,0),(4,2),(4,4),(-4,4)],STEEL[2])
  l([(-2,0),(2,0)],STEEL[3]);l([(-5,5),(5,5)],STEEL[2])
  l([(-7,7),(7,7)],O)
  if facing==1:
   p([(-6,8),(6,8),(5,12),(3,14),(-3,14),(-5,12)],STEEL[0])
   p([(-5,8),(5,8),(4,11),(2,12),(-2,12),(-4,11)],STEEL[1]);l([(-4,9),(4,9)],STEEL[2])
  elif profile:
   p([(-6,7),(-2,8),(-2,11),(-4,13),(-6,11)],STEEL[0]);l([(-5,8),(-4,9),(-4,11)],STEEL[2])
   l([(6,5),(8,6),(7,7)],STEEL[1])
  else:
   for sign in (-1,1):
    p([(sign*6,7),(sign*4,8),(sign*4,11),(sign*5,12),(sign*7,10)],STEEL[0])
    l([(sign*5,8),(sign*5,10)],STEEL[1])
   l([(0,7),(0,10)],STEEL[1]);l([(0,7),(0,8)],STEEL[2])
   for y in range(64):
    for off in range(1,10):out.putpixel((cx+off,y),out.getpixel((cx-off,y)))
 elif name=='wdHl':
  # Shallow segmented shell, with curved bindings; no leather-coif silhouette.
  p([(-8,5),(-6,2),(-3,0),(2,0),(5,1),(7,3),(8,6),(6,8),(-6,8)],WOOD[0])
  p([(-6,4),(-3,1),(2,1),(5,3),(6,5),(5,6),(-6,6)],WOOD[1])
  l([(-5,3),(-3,2),(-2,2)],WOOD[2]);l([(0,1),(2,2),(3,4)],WOOD[2])
  l([(-3,3),(-3,6)],WOOD[0]);l([(2,3),(2,6)],WOOD[0])
  l([(-7,6),(-4,7),(4,7),(7,6)],LEATHER[0]);l([(-5,8),(5,8)],O)
  if facing==1:
   p([(-6,8),(6,8),(5,10),(2,11),(-3,11),(-6,10)],WOOD[0]);l([(-4,9),(3,9)],WOOD[1])
  elif profile:
   l([(-6,8),(-5,10),(-3,10)],LEATHER[0]);l([(-5,8),(-4,9)],WOOD[1])
 elif name=='leHl':
  # A soft fitted cap with sewn panels and ear guards, not a recolored wood shell.
  p([(-6,3),(-5,0),(-2,-1),(2,-1),(5,1),(6,4),(5,7),(-6,7)],LEATHER[0])
  p([(-5,3),(-4,1),(-2,0),(2,0),(4,2),(4,5),(-5,5)],LEATHER[1])
  p([(-3,1),(0,0),(2,2),(2,4),(-2,4)],LEATHER[2])
  l([(-2,0),(-2,2),(-1,4)],LEATHER[0]);l([(-5,6),(4,6)],LEATHER[2])
  for x in (-4,-1,2):l([(x,6),(x,6)],LEATHER[3])
  if facing==1:
   p([(-6,7),(5,7),(5,11),(3,14),(-3,14),(-6,12)],LEATHER[0])
   p([(-5,8),(4,8),(3,12),(-2,13),(-4,11)],LEATHER[1]);l([(-2,8),(-1,11),(1,12)],LEATHER[2])
  elif profile:
   p([(-6,6),(-2,7),(-2,10),(-4,13),(-6,11)],LEATHER[0]);l([(-5,8),(-4,10),(-4,11)],LEATHER[2]);l([(-6,4),(-6,7)],O)
  else:
   for sign in (-1,1):
    p([(sign*6,6),(sign*4,7),(sign*4,10),(sign*5,12),(sign*6,10)],LEATHER[0]);l([(sign*5,8),(sign*5,10)],LEATHER[1])
   l([(-6,4),(-6,6)],O);l([(6,4),(6,6)],O)
 elif name=='hood':
  # Cloth overhang with an open face and a loose rear fold.
  p([(-8,5),(-7,1),(-4,-2),(0,-3),(4,-1),(6,2),(7,6),(6,12),(3,15),(-3,15),(-7,12)],GREEN[0])
  p([(-6,3),(-4,0),(0,-1),(3,0),(5,3),(4,6),(-5,6)],GREEN[1])
  l([(-4,1),(-1,0),(1,0),(3,2)],GREEN[2]);l([(-8,5),(-8,7)],O)
  if facing==1:
   p([(-5,5),(4,4),(5,9),(2,13),(-3,12),(-5,9)],GREEN[1]);l([(-3,6),(-2,9),(1,11)],GREEN[2]);l([(3,7),(2,10),(0,12)],GREEN[0])
  elif profile:
   p([(2,4),(6,5),(7,7),(6,11),(3,12),(1,9)],(0,0,0,0))
   l([(1,4),(4,4),(6,5)],GREEN[2]);l([(-5,5),(-5,9),(-2,12),(2,14)],GREEN[1]);l([(-4,11),(-1,13)],GREEN[2])
  else:
   p([(-3,4),(2,4),(4,6),(4,10),(2,12),(-2,12),(-4,10),(-4,7)],(0,0,0,0))
   l([(-3,3),(1,3),(3,4)],GREEN[2]);l([(-6,7),(-5,11),(-2,13),(2,14)],GREEN[1]);l([(-4,12),(-2,13)],GREEN[2])
 if facing==3:
  mirrored=Image.new('RGBA',(64,64))
  for y in range(64):
   for x in range(64):
    if 0<=2*cx-x<64:mirrored.putpixel((2*cx-x,y),out.getpixel((x,y)))
  return mirrored
 return out
