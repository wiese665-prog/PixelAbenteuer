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
 # Original-scale clustered highlights follow the round head volume.
 ref=[(24,24,24),(82,82,95),(124,137,137),(167,181,167),(220,220,203),(248,240,224)]
 for y in range(y0,y1):
  for x in range(x0,x1):
   pixel=cap.getpixel((x,y))
   if not pixel[3]:continue
   shade=min(range(6),key=lambda i:sum((a-b)**2 for a,b in zip(pixel[:3],ref[i])))
   if y>=y1-3 and shade==0:shade=2
   put(front,x,y,shade)
 # A smooth tapered lock, with a visible bound nape and no sawtooth hem.
 tx=cx if facing in (0,1) else x0+1 if facing==2 else x1-2
 for v in range(11,27):
  center=tx+(phase if v>=19 else 0)
  half=(3 if v<19 else 2 if v<24 else 1 if v<26 else 0) if facing in (0,1) else (2 if v<23 else 1 if v<26 else 0)
  for dx in range(-half,half+1):
   shade=1 if abs(dx)==half else 4 if dx==-1 and v<22 else 3 if dx<=0 else 2
   put(back,center+dx,y0+v,shade)
   if facing in (2,3) and v<18:
    put(front,center+dx,y0+v,shade)
 # A compact dark tie with a tiny knot remains visible over the crown layer.
 if facing==1:
  for dx in range(-3,4):put(front,cx+dx,y0+12,1)
  put(front,cx,y0+11,4);put(front,cx+1,y0+12,3)
 if facing==0:
  for side in (-1,1):
   for v in range(10,17):
    x=cx+side*(6 if v<14 else 5)
    put(front,x,y0+v,2);put(front,x-side,y0+v,3)
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
