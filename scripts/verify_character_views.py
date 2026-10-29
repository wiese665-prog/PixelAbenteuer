"""Check native equipment cells and rear scalp coverage in all long-hair poses."""
from pathlib import Path
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'app/src/main/assets/sprites/ManaSeedCharacter'
def sheet(page,layer,name,variant=1):
 folder='' if layer=='0bas' else layer+'/'
 return Image.open(ASSETS/f'char_a_{page}/{folder}char_a_{page}_{layer}_{name}_v{variant:02d}.png').convert('RGBA')
def frame(im,row,col=0):
 return im.crop((col*64,row*64,(col+1)*64,(row+1)*64))
def rear_covered(native,hair,facing):
 x0,y0,x1,y1=native.getbbox();middle=(x0+x1)//2
 for y in range(y0,y1):
  for x in range(x0,x1):
   rear=x<middle if facing==2 else x>=middle
   if rear and native.getpixel((x,y))[3]>=128:
    assert hair.getpixel((x,y))[3]==255,('visible rear scalp',facing,x,y)

# Every weapon, shield and body view comes from the native armed-idle page.
for layer,name in [('0bas','humn'),('1out','boxr'),('1out','undi'),('6tla','sw01'),('6tla','ax01'),('6tla','mc01'),('7tlb','sh01'),('7tlb','sh03')]:
 im=sheet('pONE2',layer,name);assert im.size==(512,512)
 assert all(frame(im,r).getbbox() for r in range(4)),(layer,name)

poses=0
for page in ['p1','pONE2','pONE3']:
 native=sheet(page,'4har','bob1')
 for variant in range(14):
  hair=sheet(page,'4har','long',variant)
  back=sheet(page,'4har','longback',variant)
  assert hair.size==back.size==(512,512)
  assert set(back.getchannel('A').get_flattened_data())<={0,255}
  assert set(hair.getchannel('A').get_flattened_data())<={0,255}
  palette={p[:3] for p in sheet('p1','4har','bob1',variant).get_flattened_data() if p[3]}
  assert {p[:3] for p in hair.get_flattened_data() if p[3]}<=palette
  for row in range(8):
   for col in range(8):
    h=frame(hair,row,col);assert h.getbbox() and frame(back,row,col).getbbox();poses+=1
    if variant==1:
     original=frame(native,row,col)
     if original.getbbox() is None:original=frame(native,row%4)
     assert h.getchannel('A').tobytes()==original.getchannel('A').tobytes(),('crown shifted',page,row,col)
     x0,y0,x1,y1=original.getbbox()
     for yy in range(y0,max(y0,y1-3)):
      for xx in range(x0,x1):
       if original.getpixel((xx,yy))[3]:assert h.getpixel((xx,yy))==original.getpixel((xx,yy)),('crown recolored',page,row,col,xx,yy)
    if row%4 in (2,3):
     scalp=frame(native,row,col)
     if scalp.getbbox() is None:scalp=frame(native,row%4)
     rear_covered(scalp,h,row%4)
  if page=='p1':
   for row in range(4,8):
    shapes={frame(back,row,c).crop(frame(back,row,c).getbbox()).tobytes() for c in range(6)}
    assert len(shapes)>1,('long hair does not animate',variant,row)
  if page=='pONE2':
   for facing in (2,3):rear_covered(frame(native,facing),frame(hair,facing),facing)
print(f'Armed-idle equipment: all four views populated. Long hair: {poses} poses, native palettes, rear scalp covered in side stand/walk/combat and creator views.')
