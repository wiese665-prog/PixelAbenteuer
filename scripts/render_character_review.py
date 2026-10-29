"""Compose the actual equipped layers, hands and reaction states for visual QA."""
from pathlib import Path
from PIL import Image,ImageDraw
import json
ROOT=Path(__file__).resolve().parents[1];A=ROOT/'app/src/main/assets';C=A/'sprites/ManaSeedCharacter';OUT=ROOT/'art/reviews';OUT.mkdir(exist_ok=True)
grips=json.loads((A/'class_items/grips.json').read_text());cache={}
def cell(page,layer,name,row,col=0,variant=1):
 key=(page,layer,name,variant)
 if key not in cache:cache[key]=Image.open(C/f'char_a_{page}'/('' if layer=='0bas' else layer)/f'char_a_{page}_{layer}_{name}_v{variant:02d}.png').convert('RGBA')
 return cache[key].crop((col*64,row*64,col*64+64,row*64+64))
def pose(role,facing,action,frame):
 attack=action=='attack';moving=action=='walk';page=('pBOW' if role==1 else 'pCAST') if attack else 'p1';row=facing+4 if moving else facing;col=frame if moving or attack else 1 if action=='death' and frame==1 else 0
 hp='pONE2' if attack else page;hr=facing if attack else row;hc=0 if attack else col
 im=Image.new('RGBA',(64,64));weapon='bow1' if role==1 else 'staf'
 def add(p,l,n,r=row,c=col,v=1):im.alpha_composite(cell(p,l,n,r,c,v))
 if role==1 and facing!=1:add(page,'6tla','quvr')
 if facing==1:add(page,'6tla',weapon)
 if facing!=1:add(hp,'4har','longback',hr,hc,11)
 add(page,'0bas','humn');add(page,'1out','rngr' if role==1 else 'robe')
 if role==1 and facing==1:add(page,'6tla','quvr')
 if facing==1:add(hp,'4har','longback',hr,hc,11)
 add(hp,'4har','long',hr,hc,11);add(page,'5hat','hood' if role==1 else 'mgHt')
 if facing!=1:
  add(page,'6tla',weapon);gx,gy=grips[page][row*8+col];body=cell(page,'0bas','humn',row,col)
  im.alpha_composite(body.crop((gx-1,gy-1,gx+2,gy+2)),(gx-1,gy-1))
 result=Image.new('RGBA',(96,96));result.alpha_composite(im,(16+(1 if frame%2 else -1) if action=='hurt' else 16,16+(min(3,frame) if action=='death' else 0)))
 if action=='death' and frame>=2:result=result.rotate(90 if facing==3 else -90,Image.Resampling.NEAREST,center=(48,60))
 return result
frames=[]
for tick in range(24):
 out=Image.new('RGB',(1024,1536),'#455160');d=ImageDraw.Draw(out)
 for role in (1,2):
  for ai,action in enumerate(('walk','attack','hurt','death')):
   frame=tick%6 if action=='walk' else tick%4 if action in ('attack','hurt') else min(3,tick//4)
   y=(role-1)*768+ai*192
   d.text((4,y+6),f'{"Bogen" if role==1 else "Magie"} {action}',fill='white')
   for facing in range(4):
    im=pose(role,facing,action,frame).resize((192,192),Image.Resampling.NEAREST)
    out.paste(im,(180+facing*200,y),im)
 frames.append(out)
frames[0].save(OUT/'character-animations.gif',save_all=True,append_images=frames[1:],duration=125,loop=0,optimize=True)
# Left shield comparison uses native left arm overlay and each real weapon.
out=Image.new('RGB',(768,384),'#455160')
for ri,shield in enumerate(('shP1','shP3')):
 for ci,weapon in enumerate(('sw01','ax01','mc01')):
  im=Image.new('RGBA',(64,64));im.alpha_composite(cell('pONE2','4har','longback',3));im.alpha_composite(cell('pONE2','0bas','humn',3));im.alpha_composite(cell('pONE2','1out','boxr',3));im.alpha_composite(cell('pONE2','4har','long',3));im.alpha_composite(cell('pONE2','7tlb',shield,3))
  im.alpha_composite(cell('pONE2','0bas','humn',3).crop((23,29,26,32)),(23,29));im.alpha_composite(cell('pONE2','6tla',weapon,3))
  im=im.resize((192,192),Image.Resampling.NEAREST);out.paste(im,(ci*256,ri*192),im)
out.save(OUT/'left-shields.png')
print('Actual layer review: four directions, walk/attack/hit/fall, grip overlays and six left shield/weapon combinations.')
