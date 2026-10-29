"""Audit original equipment at native resolution; generate palette census and comparison."""
from pathlib import Path
from collections import Counter
from PIL import Image,ImageDraw
import json
ROOT=Path(__file__).resolve().parents[1];C=ROOT/'app/src/main/assets/sprites/ManaSeedCharacter';OUT=ROOT/'art/reviews';OUT.mkdir(exist_ok=True)
records=[]
for page in ('p1','pONE2','pONE3'):
 for layer in ('1out','5hat'):
  for p in sorted((C/f'char_a_{page}'/layer).glob('*.png')):
   if any(f'_{n}_' in p.name for n in ('rngr','robe','hood','mgHt')):continue
   im=Image.open(p).convert('RGBA');counts=Counter(px[:3] for px in im.get_flattened_data() if px[3]);boxes=[];valid=0
   for row in range(im.height//64):
    for col in range(im.width//64):
     b=im.crop((col*64,row*64,col*64+64,row*64+64)).getbbox()
     if b:valid+=1;boxes.append([b[2]-b[0],b[3]-b[1]])
   records.append(dict(page=page,layer=layer,name=p.stem.split(f'_{layer}_')[1],sheet=list(im.size),frame=[64,64],frames=valid,alpha=sorted(set(im.getchannel('A').get_flattened_data())),bounds_range=[min(x[0] for x in boxes),max(x[0] for x in boxes),min(x[1] for x in boxes),max(x[1] for x in boxes)],palette=[dict(hex='#%02x%02x%02x'%rgb,pixels=n) for rgb,n in counts.most_common()]))
(OUT/'armor-style-census.json').write_text(json.dumps(records,indent=2)+'\n')
# Four standing directions at native size and 6x nearest-neighbour; actual body layers.
names=[('fstr',1,None),('fstr',3,None),('pfpn',1,None),('pfpn',2,None),('rngr',1,'hood'),('robe',1,'mgHt')]
out=Image.new('RGB',(1050,len(names)*230),(53,61,68));d=ImageDraw.Draw(out)
for i,(name,v,hat) in enumerate(names):
 d.text((8,i*230+8),f'{name} v{v:02}',fill='white')
 for row in range(4):
  im=Image.new('RGBA',(64,64))
  for layer,n,var in [('0bas','humn',1),('1out',name,v)]+([('5hat',hat,1)] if hat else []):
   p=C/'char_a_p1'/('' if layer=='0bas' else layer)/f'char_a_p1_{layer}_{n}_v{var:02}.png';q=Image.open(p).convert('RGBA').crop((0,row*64,64,row*64+64));im.alpha_composite(q)
  crop=im.crop((16,8,48,45)).resize((192,222),Image.Resampling.NEAREST);out.paste(crop,(200+row*205,i*230),crop)
out.save(OUT/'armor-style-comparison.png')
print(f'{len(records)} original outfit/headgear sheets audited; exact RGB counts, alpha and native frame bounds exported.')
# Show all four directions in motion using the exact generated equipment/body frames.
frames=[]
for tick in range(24):
 canvas=Image.new('RGB',(960,560),(53,61,68));draw=ImageDraw.Draw(canvas)
 for role,(outfit,hat,page) in enumerate([('rngr','hood','pBOW'),('robe','mgHt','pCAST')]):
  for facing in range(4):
   pg='p1' if tick<12 else page;row=facing+4 if tick<12 else facing;col=tick%6 if tick<12 else tick%4
   im=Image.new('RGBA',(64,64))
   for layer,n in [('0bas','humn'),('1out',outfit),('5hat',hat)]:
    path=C/f'char_a_{pg}'/('' if layer=='0bas' else layer)/f'char_a_{pg}_{layer}_{n}_v01.png';sheet=Image.open(path).convert('RGBA');im.alpha_composite(sheet.crop((col*64,row*64,col*64+64,row*64+64)))
   crop=im.crop((14,4,50,48)).resize((216,264),Image.Resampling.NEAREST);canvas.paste(crop,(facing*240,role*280),crop)
 draw.text((8,4),'Walk' if tick<12 else 'Attack pose',fill='white');frames.append(canvas)
frames[0].save(OUT/'armor-animation-review.gif',save_all=True,append_images=frames[1:],duration=140,loop=0,optimize=True)
