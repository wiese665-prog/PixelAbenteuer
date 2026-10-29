"""Build the new long haircut on native head anchors for movement, creator and combat."""
from pathlib import Path
from PIL import Image
from collections import Counter
from import_village_hair import isolate_hair,closest
ROOT=Path(__file__).resolve().parents[1];A=ROOT/'app/src/main/assets/sprites/ManaSeedCharacter'
source=Image.open(ROOT/'art/source/long-hair-v2.png').convert('RGBA')
frames=[]
for facing,(top,bottom) in enumerate([(170,350),(416,608),(664,849),(909,1098)]):
 row=[]
 for col in range(8):
  im=source.crop((col*160,top,(col+1)*160,bottom))
  im.putdata([p if p[3]>=224 and max(p[:3])-min(p[:3])<70 else (0,0,0,0) for p in im.get_flattened_data()]);row.append(isolate_hair(im))
 frames.append(row)
for page in ['p1','pONE2','pONE3']:
 native=Image.open(A/f'char_a_{page}/4har/char_a_{page}_4har_dap1_v01.png').convert('RGBA');indexed=Image.new('L',(512,512),255)
 for row in range(8):
  facing=row%4
  for col in range(8):
   scalp=native.crop((col*64,row*64,(col+1)*64,(row+1)*64))
   if scalp.getbbox() is None:scalp=native.crop((0,facing*64,64,(facing+1)*64))
   x0,y0,x1,y1=scalp.getbbox()
   width,height=(17,23) if facing==1 else (16,22)
   if page=='pONE3' and row==4 and col in (1,2,3):height-=2
   left,top=round((x0+x1-width)/2),y0-1
   # Native head motion plus a stable crown; ends use sequential sway poses.
   frame=frames[facing][col if row>=4 or page!='p1' else 0].resize((width,height),Image.Resampling.NEAREST)
   for y in range(height):
    for x in range(width):
     p=frame.getpixel((x,y))
     if p[3]>=128:indexed.putpixel((col*64+left+x,row*64+top+y),closest(p[:3]))
   middle=(x0+x1)//2
   for y in range(y0,y1):
    for x in range(x0,x1):
     p=scalp.getpixel((x,y));rear=(facing==2 and x<middle) or (facing==3 and x>=middle)
     if p[3]>=128 and (rear or y<y0+2):
      pos=(col*64+x,row*64+y)
      if indexed.getpixel(pos)==255:indexed.putpixel(pos,closest(p[:3]))
 for variant in range(14):
  palette=Image.open(A/f'char_a_p1/4har/char_a_p1_4har_dap1_v{variant:02d}.png').convert('RGBA')
  colors=Counter(p[:3] for p in palette.get_flattened_data() if p[3]==255)
  ramp=sorted([rgb for rgb,_ in colors.most_common(5)],key=lambda p:sum(a*b for a,b in zip(p,(.2126,.7152,.0722))))
  out=Image.new('RGBA',(512,512));out.putdata([(0,0,0,0) if i==255 else (*ramp[i],255) for i in indexed.get_flattened_data()])
  out.save(A/f'char_a_{page}/4har/char_a_{page}_4har_long_v{variant:02d}.png')
print('New long haircut: 42 sheets, 14 native palettes, stand/walk/armed idle/combat, four directions.')
