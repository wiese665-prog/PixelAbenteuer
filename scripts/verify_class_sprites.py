"""Validate complete class animation layers, native materials and head attachment."""
from pathlib import Path
from PIL import Image,ImageChops
import json
ROOT=Path(__file__).resolve().parents[1];A=ROOT/'app/src/main/assets';C=A/'sprites/ManaSeedCharacter'
def sheet(p,l,n,v=1):return Image.open(C/f'char_a_{p}'/('' if l=='0bas' else l)/f'char_a_{p}_{l}_{n}_v{v:02d}.png').convert('RGBA')
def frame(im,r,c):return im.crop((c*64,r*64,c*64+64,r*64+64))
allowed=set()
for path in [A/'village/grass_details.png',A/'village/guild/Mage1.png',A/'village/guild/Fighter2_Walk_without_shadow.png',C/'char_a_p1/1out/char_a_p1_1out_fstr_v01.png']:
 allowed.update(p[:3] for p in Image.open(path).convert('RGBA').get_flattened_data() if p[3])
for page in ('p1','pONE2','pONE3'):
 for native in ('fstr','pfpn'):
  for variant in range(1,6):
   allowed.update(p[:3] for p in sheet(page,'1out',native,variant).get_flattened_data() if p[3])
count=0
for page in ('p1','pONE2','pONE3','pBOW','pCAST'):
 for layer,names in [('1out',['rngr','robe']),('5hat',['hood','mgHt']),('6tla',['bow1','staf','quvr'])]:
  for name in names:
   im=sheet(page,layer,name);assert im.size==(512,512)
   assert set(im.getchannel('A').get_flattened_data())<={0,255}
   assert {p[:3] for p in im.get_flattened_data() if p[3]}<=allowed,(page,name,'foreign color')
   for row in range(8):
    for col in range(8):
     assert frame(im,row,col).getbbox(),(page,name,row,col,'missing frame');count+=1
# Garments must use character-equipment palettes, not grass or scenery colors.
armor_colors=set()
for variant in range(1,6):
 armor_colors.update(px[:3] for px in sheet('p1','1out','pfpn',variant).get_flattened_data() if px[3])
for page in ('p1','pONE2','pONE3','pBOW','pCAST'):
 for layer,name in [('1out','rngr'),('1out','robe'),('5hat','hood'),('5hat','mgHt')]:
  actual={px[:3] for px in sheet(page,layer,name).get_flattened_data() if px[3]}
  assert actual<=armor_colors,(page,name,'not an original equipment color')
  assert (24,24,24) in actual,(page,name,'native outline missing')
for page in ('pBOW','pCAST'):
 for variant in range(11):
  body=sheet(page,'0bas','humn',variant);original=sheet('pONE2','0bas','humn',variant)
  for facing in range(4):
   cap=frame(sheet('pONE2','4har','bob1'),facing,0);x,y,r,b=cap.getbbox()
   for col in range(4):
    actual=frame(body,facing,col);native=frame(original,facing,0)
    for yy in range(y,y+12):
     for xx in range(x,r):
      a=actual.getpixel((xx,yy));b=native.getpixel((xx,yy))
      assert a[3]==b[3] and (not b[3] or a==b),('head moved',page,variant,facing,col,xx,yy)
   assert len({frame(body,facing,c).tobytes() for c in range(4)})>=3,('attack not animated',page,facing)
grips=json.loads((A/'class_items/grips.json').read_text())
for page in ('p1','pBOW','pCAST'):
 body=sheet(page,'0bas','humn')
 assert len(grips[page])==64
 rows=range(8) if page=='p1' else range(4)
 for row in rows:
  for col in range(6 if page=='p1' and row>=4 else 4 if page!='p1' else 1):
   x,y=grips[page][row*8+col]
   assert 1<=x<62 and 1<=y<62
   assert frame(body,row,col).crop((x-1,y-1,x+2,y+2)).getbbox(),('grip detached from hand',page,row,col)
for name in ['Jagdbogen','Magierstab','Waldlaeuferwams','Waldlaeuferkapuze','Magierrobe','Magierhut']:
 im=Image.open(A/'class_items'/f'{name}.png').convert('RGBA');assert im.size==(32,32) and im.getbbox()
# The left profile keeps its native attachment while actually narrowing the face.
for page in ('pONE2','pONE3'):
 for original,profile in [('sh01','shP1'),('sh03','shP3')]:
  old=sheet(page,'7tlb',original);new=sheet(page,'7tlb',profile)
  for row in (2,3,6,7):
   for col in range(8):
    a=frame(old,row,col);b=frame(new,row,col)
    if not a.getbbox():continue
    x,y,r,bb=a.getbbox();xx,yy,rr,bbb=b.getbbox()
    assert rr-xx<r-x and yy==y and bbb==bb,('frontal shield reused',page,row,col)
    assert abs((xx+rr)-(x+r))<=1,('shield grip shifted',page,row,col)
    assert {p[:3] for p in b.get_flattened_data() if p[3]}<={p[:3] for p in a.get_flattened_data() if p[3]}
bar=Image.open(A/'village/guild/inn_bar.png').convert('RGBA');assert bar.size==(96,44)
# The approved inn must have a sharp alpha edge and only original native colors.
inn=Image.open(A/'village/inn.png').convert('RGBA');assert inn.size==(200,144)
assert set(inn.getchannel('A').get_flattened_data())<={0,255}
palette={tuple(p) for p in json.loads((ROOT/'art/source/inn-palette.json').read_text())}
assert {p[:3] for p in inn.get_flattened_data() if p[3]}==palette
for y in range(inn.height):
 for x in range(inn.width):
  p=inn.getpixel((x,y))
  if p[3]:assert sum(c*k for c,k in zip(p[:3],(.2126,.7152,.0722)))>=38,('black fringe',x,y)
doors=Image.open(A/'village/door2.png').convert('RGBA');assert doors.size==(200,864)
assert doors.crop((0,0,200,144)).tobytes()==inn.tobytes()==doors.crop((0,720,200,864)).tobytes()
# Opening must affect only the actual entrance, never window, arch or foundation.
opened=doors.crop((0,432,200,576));changed=ImageChops.difference(inn,opened).convert('RGB').getbbox()
assert changed and changed[0]>=89 and changed[1]>=111 and changed[2]<=110 and changed[3]<=139,changed
print(f'Class assets: {count} equipped poses; 11 skin tones, head attachment, distinct attack frames, native palettes, icons and approved inn/door cutout passed.')
