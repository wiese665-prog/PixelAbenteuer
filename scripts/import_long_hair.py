"""Redrawn long haircut: per-pose native head anchors, continuous locks, 14 palettes.

The bob reference supplies head coverage/pose only, never rendered hair pixels.
All shading, bangs, side locks and animated tapered ends are drawn anew.
"""
from pathlib import Path
from PIL import Image
from collections import Counter
ROOT=Path(__file__).resolve().parents[1]
A=ROOT/'app/src/main/assets/sprites/ManaSeedCharacter'

def palette(variant):
 im=Image.open(A/f'char_a_p1/4har/char_a_p1_4har_bob1_v{variant:02d}.png').convert('RGBA')
 colors=Counter(p[:3] for p in im.get_flattened_data() if p[3])
 return sorted([rgb for rgb,_ in colors.most_common(6)],key=lambda p:sum(a*b for a,b in zip(p,(.2126,.7152,.0722))))

def pose(cap,facing,phase):
 """Draw a fresh connected haircut, anchored to the exact native pose mask."""
 front=Image.new('L',(64,64),255);back=Image.new('L',(64,64),255)
 x0,y0,x1,y1=cap.getbbox();width=x1-x0;mid=(x0+x1-1)/2
 # A continuous, softly shaded curtain; no old short-hair bottom outline.
 def shade(x,y):
  u=(x-x0)*14//width;v=y-y0
  # Curved, broken highlight clusters: no full-height striped cylinder.
  s=[1,2,3,4,4,3,2,3,4,4,3,3,2,1][u]
  if v<2:s=2
  elif v<4:s=min(4,s+1) if 3<=u<=10 else min(s,2)
  elif v<7 and u in (3,4,8,9):s=5
  elif 8<=v<=11 and u in (3,8):s=4
  elif v>=12:
   s=[1,2,3,3,2,2,2,3,4,3,2,3,2,1][u]
   if v>19:s=max(1,s-1)
  return s
 def put(im,x,y,s):
  if 0<=x<64 and 0<=y<64:im.putpixel((x,y),s)
 # Exact opaque head coverage follows movement, lean and combat pose.
 # Old bob colors/contours are discarded, including its black lower hem.
 for y in range(y0,y1):
  for x in range(x0,x1):
   if cap.getpixel((x,y))[3]>=128:put(front,x,y,shade(x,y))
 # New parting and curved crown highlights stay attached to the head.
 for x in range(x0+2,x1-2):
  ys=[y for y in range(y0,y1) if cap.getpixel((x,y))[3]>=128]
  if ys:
   yy=ys[0]
   put(front,x,yy,2)
   if yy+1<y1 and cap.getpixel((x,yy+1))[3]>=128:put(front,x,yy+1,3)
 # Draw lengths as one connected silhouette, not stretched columns of bob pixels.
 # The rear curtain goes behind the body; foreground locks cover neck seams.
 for x in range(x0,x1):
  u=(x-x0)*14//width
  if facing==0:allowed=u<=3 or u>=10
  elif facing==1:allowed=True
  elif facing==2:allowed=u<=6
  else:allowed=u>=7
  if not allowed:continue
  outer=min(u,13-u)
  end=y0+24-(4 if outer==0 else 2 if outer==1 else 0)-(1 if u in (3,8,11) else 0)
  if facing in (2,3):end-=1
  start=y0+7
  for y in range(start,end):
   sway=phase if y>=y0+18 else 0
   # Taper the final three pixels so the hem cannot become a straight block.
   if y>=end-2 and ((u+facing)%3==0):continue
   s=shade(x,y)
   if y>=end-1:s=max(1,s-1)
   put(back,x+sway,y,s)
   # Front strands descend beside the face; profile locks cover the rear neck
   # but never obscure nose, eyes or the weapon-side forearm.
   foreground=(facing==0 and (u<=2 or u>=11) and y<y0+19)
   foreground|=(facing==2 and u<=4 and y<y0+18)
   foreground|=(facing==3 and u>=9 and y<y0+18)
   if foreground:put(front,x,y,s)
 # Back-facing crown/curtain are one surface, with no bob seam at the neck.
 if facing==1:
  for y in range(y0+7,y1):
   for x in range(x0,x1):
    if front.getpixel((x,y))!=255:put(front,x,y,shade(x,y))
 return front,back

for page in ['p1','pONE2','pONE3']:
 native=Image.open(A/f'char_a_{page}/4har/char_a_{page}_4har_bob1_v01.png').convert('RGBA')
 layers=[Image.new('L',(512,512),255),Image.new('L',(512,512),255)]
 for row in range(8):
  for col in range(8):
   cap=native.crop((col*64,row*64,col*64+64,row*64+64))
   if cap.getbbox() is None:cap=native.crop((0,row%4*64,64,row%4*64+64))
   phase=([0,1,1,0,-1,-1,0,0][col] if (page!='p1' or row>=4) else 0)
   for dst,src in zip(layers,pose(cap,row%4,phase)):dst.paste(src,(col*64,row*64))
 for variant in range(14):
  ramp=palette(variant)
  for kind,indices in zip(['long','longback'],layers):
   out=Image.new('RGBA',(512,512))
   out.putdata([(0,0,0,0) if i==255 else (*ramp[i],255) for i in indices.get_flattened_data()])
   out.save(A/f'char_a_{page}/4har/char_a_{page}_4har_{kind}_v{variant:02d}.png')
print('Redrawn long hair: 84 sheets, 14 native palettes, fitted per-pose crowns and layered animated locks.')
