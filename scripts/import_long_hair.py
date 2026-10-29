"""Half-up long ponytail: per-pose native head anchors, continuous locks, 14 palettes.

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
 """Loose half-up ponytail, side-swept fringe and fitted crown in every pose."""
 front=Image.new('L',(64,64),255);back=Image.new('L',(64,64),255)
 x0,y0,x1,y1=cap.getbbox();cx=(x0+x1-1)//2;width=x1-x0
 def put(im,x,y,v):
  if 0<=x<64 and 0<=y<64:im.putpixel((x,y),v)
 # Follow native head coverage but redraw a diagonal side part and curved locks.
 for y in range(y0,y1):
  for x in range(x0,x1):
   if cap.getpixel((x,y))[3]<128:continue
   u=(x-x0)*14//width;v=y-y0
   shade=2 if u<2 or u>11 else 3
   if 2<=u<=6 and 2<=v<=6:shade=4
   if 3<=u<=5 and 3<=v<=4:shade=5
   if v>=6 and (u+v//2)%6 in (1,2):shade=4
   if v>=10:shade=max(1,shade-1)
   # Side-swept fringe crosses the forehead diagonally, without filling the face.
   if facing!=1 and 4<=u<=10 and v==5+(u-4)//2:shade=2
   put(front,x,y,shade)
 # Narrow loose tail tied at nape; the old full-width straight curtain is gone.
 tx=cx if facing in (0,1) else x0+1 if facing==2 else x1-2
 for v in range(9,26):
  bend=(0 if v<15 else phase)+(1 if 16<=v<=20 else 0)
  center=tx+bend
  half=2 if v<13 else 4 if v<19 else 3 if v<23 else 2 if v<25 else 0
  for dx in range(-half,half+1):
   shade=1 if abs(dx)==half else 4 if dx in (-2,-1) and v<22 else 3 if dx<2 else 2
   if v in (10,11):shade=1 if dx%2 else 2 # hair tie, native hair ramp
   put(back,center+dx,y0+v,shade)
   # In profile the upper rear lock must cover the neck/scalp seam above torso.
   if facing in (2,3) and v<=17 and ((facing==2 and dx<=0) or (facing==3 and dx>=0)):
    put(front,center+dx,y0+v,shade)
 # Two loose front face-framing locks stop at the shoulder, not the waist.
 if facing==0:
  for side in (-1,1):
   for v in range(9,18):
    x=cx+side*(6 if v<14 else 5)
    put(front,x,y0+v,2);put(front,x-side,y0+v,3 if v<16 else 2)
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
