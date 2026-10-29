"""Long hair with native fitted crowns and separately depth-sorted animated lengths."""
from pathlib import Path
from PIL import Image
from collections import Counter
ROOT=Path(__file__).resolve().parents[1];A=ROOT/'app/src/main/assets/sprites/ManaSeedCharacter'
REFERENCE=[(24,24,24),(82,82,95),(124,137,137),(167,181,167),(220,220,203),(248,240,224)]
def index(rgb):return min(range(6),key=lambda i:sum((a-b)**2 for a,b in zip(rgb,REFERENCE[i])))
for page in ['p1','pONE2','pONE3']:
 native=Image.open(A/f'char_a_{page}/4har/char_a_{page}_4har_bob1_v01.png').convert('RGBA')
 fronts=Image.new('L',(512,512),255);backs=Image.new('L',(512,512),255)
 for row in range(8):
  facing=row%4
  for col in range(8):
   cap=native.crop((col*64,row*64,(col+1)*64,(row+1)*64))
   if cap.getbbox() is None:cap=native.crop((0,facing*64,64,(facing+1)*64))
   x0,y0,x1,y1=cap.getbbox();mid=(x0+x1)//2
   # Native crown pixels and palette indices are kept exactly. No arbitrary
   # fitted rectangles or patches of differently shaded short hair are inserted.
   for y in range(y0,y1):
    for x in range(x0,x1):
     c=cap.getpixel((x,y))
     if c[3]>=128:fronts.putpixel((col*64+x,row*64+y),index(c[:3]))
   # Extend existing native strands downwards, using each column's own shading.
   for x in range(x0,x1):
    if facing==0 and mid-3<=x<=mid+2:continue # keep front face/body opening
    if facing==2 and x>=mid:continue
    if facing==3 and x<mid:continue
    column=[(y,cap.getpixel((x,y))) for y in range(y0,y1) if cap.getpixel((x,y))[3]>=128]
    if len(column)<3:continue
    start=column[-1][0]-2;shade=max(2,index(column[-3][1][:3]))
    # Side lengths stay behind the torso. Back view covers the shoulders.
    end=min(62,y0+23-max(0,(abs(x-mid)-2)//2))
    phase=[0,0,1,1,0,-1,0,0][col] if row>=4 or page!='p1' else 0
    for y in range(start,end):
     sway=phase if y>=end-4 else 0;xx=x+sway
     if not 0<=xx<64:continue
     color=shade
     if y==end-1:color=max(1,shade-1)
     backs.putpixel((col*64+xx,row*64+y),color)
   # Remove only the old short haircut's bottom contour where it would
   # otherwise draw a black horizontal seam across the new longer strands.
   for y in range(max(y0,y1-3),y1):
    for x in range(x0,x1):
     at=(col*64+x,row*64+y)
     if fronts.getpixel(at)==0 and backs.getpixel(at)!=255:
      fronts.putpixel(at,backs.getpixel(at))
 for variant in range(14):
  palette=Image.open(A/f'char_a_p1/4har/char_a_p1_4har_bob1_v{variant:02d}.png').convert('RGBA')
  counts=Counter(p[:3] for p in palette.get_flattened_data() if p[3]==255)
  ramp=sorted([rgb for rgb,_ in counts.most_common(6)],key=lambda p:sum(a*b for a,b in zip(p,(.2126,.7152,.0722))))
  for kind,indices in [('long',fronts),('longback',backs)]:
   out=Image.new('RGBA',(512,512));out.putdata([(0,0,0,0) if i==255 else (*ramp[i],255) for i in indices.get_flattened_data()])
   out.save(A/f'char_a_{page}/4har/char_a_{page}_4har_{kind}_v{variant:02d}.png')
print('Long hair: native crowns plus animated depth-sorted lengths; 84 sheets / 14 original palettes.')
