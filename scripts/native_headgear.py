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
 box=cap.getbbox();cx=round((box[0]+box[2]-1)/2);top=box[1]
 im=Image.new('RGBA',(64,64));d=ImageDraw.Draw(im)
 # Work in a local head basis; profiles are drawn separately then mirrored.
 def poly(pts,color):d.polygon([(cx+x,top+y) for x,y in pts],fill=color)
 def line(pts,color):d.line([(cx+x,top+y) for x,y in pts],fill=color)
 def rect(x0,y0,x1,y1,color):d.rectangle((cx+x0,top+y0,cx+x1,top+y1),fill=color)
 profile=facing in (2,3)
 if name in ('hood','mgHt','wdHl','leHl'):
  return native_form(name,cap,facing)
 elif name=='irHl':
  # Ridged steel bowl, projecting brow and separate cheek plates.
  poly([(-7,4),(-5,0),(-2,-2),(2,-2),(5,0),(7,4),(7,7),(-7,7)],O)
  poly([(-6,4),(-4,1),(-1,-1),(1,-1),(4,1),(6,4),(6,6),(-6,6)],STEEL[0])
  poly([(-5,4),(-3,1),(-1,0),(0,4),(-2,5),(-5,5)],STEEL[2]);line([(-3,2),(-2,0)],STEEL[3])
  line([(1,0),(2,5)],STEEL[1]);line([(-7,6),(7,6)],STEEL[1]);line([(-5,6),(0,6)],STEEL[3])
  if facing==1:
   poly([(-6,8),(6,8),(5,13),(3,15),(-3,15),(-5,13)],O)
   poly([(-5,8),(5,8),(4,12),(2,14),(-2,14),(-4,12)],STEEL[0]);line([(-4,9),(3,9)],STEEL[1])
  elif profile:
   poly([(-6,7),(-2,8),(-2,12),(-4,14),(-6,12)],O)
   poly([(-5,8),(-3,9),(-3,11),(-4,12),(-5,11)],STEEL[1]);line([(-5,8),(-4,9)],STEEL[2])
   # Short forward brim gives a markedly different silhouette from the wood cap.
   line([(5,6),(8,6),(8,7),(6,7)],STEEL[0])
  else:
   for sign in (-1,1):
    poly([(sign*6,8),(sign*4,8),(sign*4,11),(sign*5,13),(sign*7,12)],O)
    line([(sign*5,8),(sign*5,11)],STEEL[1])
   line([(0,7),(0,10)],STEEL[0]);d.point((cx,top+7),fill=STEEL[2])
 elif name=='wdHl':
  # Flat plank crown, visible vertical grain, leather brow strap and open face.
  poly([(-7,1),(-5,-1),(5,-1),(7,1),(7,7),(-7,7)],O)
  rect(-6,1,6,5,WOOD[0]);rect(-5,0,5,1,WOOD[1])
  for x in (-4,0,4):
   line([(x,1),(x,5)],WOOD[1]);line([(x+1,2),(x+1,4)],WOOD[2])
  line([(-6,6),(6,6)],LEATHER[0]);d.point((cx-4,top+6),fill=BRASS[1]);d.point((cx+4,top+6),fill=BRASS[1])
  if facing==1:
   rect(-6,8,6,12,WOOD[0]);line([(-6,13),(6,13)],O)
   for x in (-4,0,4):line([(x,8),(x,11)],WOOD[1])
  elif profile:
   poly([(-7,7),(-4,7),(-4,11),(-5,13),(-7,12)],O);line([(-6,8),(-6,11)],LEATHER[2])
  else:
   line([(-6,8),(-6,11)],LEATHER[0]);line([(6,8),(6,11)],LEATHER[0])
 elif name=='leHl':
  # Short leather coif with open ears: distinct from the new wood/steel crowns.
  poly([(-6,3),(-4,0),(2,0),(5,2),(6,5),(5,7),(-6,7)],O)
  poly([(-5,3),(-3,1),(1,1),(4,3),(5,5),(4,6),(-5,6)],LEATHER[1])
  poly([(-4,3),(-2,1),(0,2),(1,4),(-3,4)],LEATHER[2]);line([(-2,2),(0,2)],LEATHER[3])
  line([(-5,6),(5,6)],LEATHER[0])
  if facing==1:
   rect(-5,8,4,12,LEATHER[0]);line([(-4,8),(-3,10),(2,11)],LEATHER[1])
  elif profile:line([(-5,7),(-5,11),(-3,12)],LEATHER[0])
 # Small material clusters: bevels, seams and folds rather than large flat fills.
 if name=='mgHt':
  poly([(-3,0),(-2,-3),(0,-5),(0,-2),(2,1),(1,3)],BLUE[1])
  line([(-1,-4),(0,-3)],BLUE[2])
  line([(2,-1),(3,2),(3,3)],BLUE[0])
  line([(-5,5),(-2,5),(2,5)],BRASS[0])
  rect(1,4,2,5,BRASS[2]);d.point((cx+2,top+4),fill=BRASS[0])
  line([(-6,7),(-3,7),(0,8)],BLUE[0])
  if facing==1:line([(-2,-2),(0,0),(1,3)],BLUE[2])
 elif name=='irHl':
  line([(-4,2),(-5,4),(-3,5)],STEEL[2])
  line([(3,1),(4,3),(5,5)],STEEL[1])
  line([(0,-1),(0,3),(1,5)],STEEL[2])
  for x in (-5,4):d.point((cx+x,top+6),fill=STEEL[3])
  if facing==1:
   line([(-4,10),(3,10)],STEEL[2]);line([(-3,12),(2,12)],STEEL[1])
  elif profile:
   line([(-5,9),(-4,10),(-4,11)],STEEL[2]);d.point((cx-4,top+8),fill=STEEL[3])
 elif name=='wdHl':
  # Staggered plank ends and thin bevels interrupt the former box-like crown.
  rect(-5,-1,-3,0,WOOD[0]);rect(2,-1,4,0,WOOD[1])
  for x in (-5,-1,3):
   line([(x,2),(x,4)],WOOD[0]);d.point((cx+x+1,top+1),fill=WOOD[2])
  line([(-5,5),(5,5)],LEATHER[0])
  for x in (-5,0,5):d.point((cx+x,top+6),fill=BRASS[1])
  if facing==1:
   line([(-5,9),(-4,11)],WOOD[2]);line([(0,8),(1,10)],WOOD[1]);line([(4,9),(4,12)],WOOD[1])
 elif name=='leHl':
  # Stitched panels and a raised edge with subdued leather highlights.
  line([(-2,1),(-1,3),(0,5)],LEATHER[0]);line([(1,1),(2,3),(3,5)],LEATHER[2])
  for x in (-4,-1,2,4):d.point((cx+x,top+6),fill=LEATHER[3])
  if facing==1:
   line([(-3,8),(-2,10),(1,11)],LEATHER[2]);d.point((cx+2,top+9),fill=LEATHER[3])
 if name=='irHl':
  native=source('pfht',2 if profile else facing);ref=source('bob1',2 if profile else facing).getbbox()
  ox=cx-round((ref[0]+ref[2]-1)/2);oy=top-ref[1]
  shades={(107,50,53):STEEL[0],(169,73,76):STEEL[1],(234,114,104):STEEL[2],(252,176,153):STEEL[3]}
  for y in range(top,top+6):
   for x in range(cx-6,cx+7):
    if abs(x-cx)<=1 or im.getpixel((x,y)) in (O,(0,0,0,0)):continue
    q=native.getpixel((x-ox,y-oy))
    if q[:3] in shades:im.putpixel((x,y),shades[q[:3]])
 if facing==3:
  # Mirror about the exact source head center, not the 64px cell center.
  mirrored=Image.new('RGBA',(64,64))
  for y in range(64):
   for x in range(64):
    xx=2*cx-x
    if 0<=xx<64:mirrored.putpixel((xx,y),im.getpixel((x,y)))
  im=mirrored
 return im

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
 return out
