"""All equipped materials in four directions, using real production masks."""
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
C=ROOT/'app/src/main/assets/sprites/ManaSeedCharacter'
def read(page,layer,name):return Image.open(C/f'char_a_{page}'/('' if layer=='0bas' else layer)/f'char_a_{page}_{layer}_{name}_v01.png').convert('RGBA')
def frame(im,r,c):return im.crop((c*64,r*64,c*64+64,r*64+64))
from PIL import Image,ImageDraw
OUT=ROOT/'art/reviews';OUT.mkdir(exist_ok=True)
sets=[('Holz','wdAr','wdHl','leBt','belt'),('Leder','ltAr','leHl','leBt','bag'),('Eisen','irAr','irHl','irBt','belt'),('Helm / Eisen','irAr','stHl','irBt',''),('Waldlaeufer','rngr','hood','leBt',''),('Magier','robe','mgHt','irBt','')]
def pose(page,row,col,outfit,hat,boots,belt):
 im=Image.new('RGBA',(64,64))
 for l,n in [('0bas','humn'),('1out',outfit),('8feet',boots),('9belt',belt),('5hat',hat)]:
  if n:im.alpha_composite(frame(read(page,l,n),row,col))
 return im
out=Image.new('RGB',(1040,len(sets)*244),(53,61,68));d=ImageDraw.Draw(out)
for i,(label,*gear) in enumerate(sets):
 d.text((8,i*244+8),label,fill='white')
 for r in range(4):
  q=pose('p1',r,0,*gear).crop((14,4,50,44)).resize((216,240),Image.Resampling.NEAREST);out.paste(q,(160+r*220,i*244),q)
out.save(OUT/'all-equipment-comparison.png')
frames=[]
for tick in range(12):
 out=Image.new('RGB',(1024,len(sets)*200),(53,61,68));d=ImageDraw.Draw(out)
 page='p1' if tick<6 else 'pONE3'
 for i,(label,*gear) in enumerate(sets):
  d.text((8,i*200+8),label,fill='white')
  for r in range(4):
   row=r+4 if tick<6 else r;col=tick%6
   q=pose(page,row,col,*gear).resize((192,192),Image.Resampling.NEAREST);out.paste(q,(180+r*208,i*200),q)
 frames.append(out)
frames[0].save(OUT/'all-equipment-animation.gif',save_all=True,append_images=frames[1:],duration=140,loop=0,optimize=True)
print('Six complete equipment combinations: all directions, walk and native melee poses.')
