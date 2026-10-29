"""Native equipment variants: preserve original masks, poses and color identities."""
from pathlib import Path
from PIL import Image,ImageDraw
import json
ROOT=Path(__file__).resolve().parents[1];A=ROOT/'app/src/main/assets';C=A/'sprites/ManaSeedCharacter'
OUTLINE=(24,24,24)
METAL=[(82,82,95),(124,137,137),(167,181,167),(220,220,203)]
LEATHER=[(61,18,0),(99,46,17),(141,99,64),(218,185,136)]
WOOD=[(88,56,40),(152,96,56),(240,152,72)]
SOURCE=[OUTLINE,(113,101,88),(166,152,120),(199,196,162),(91,24,57),(141,34,70),(190,86,103),(30,43,65),(60,85,117)]
MATERIALS={
 'wdAr':[OUTLINE,*WOOD,*LEATHER[:3],(113,101,88),(166,152,120)],
 'ltAr':[OUTLINE,*LEATHER[:3],(54,74,63),(91,127,106),(168,200,152),(88,56,40),(152,96,56)],
 'irAr':[OUTLINE,*METAL[:3],*LEATHER[:3],(46,64,72),(45,131,109)]}
HATS={'leHl':LEATHER,'irHl':METAL,'stHl':[(46,64,72),(82,82,95),(124,137,137),(167,181,167)]}
ITEMS={'Wooden Armor':('1out','wdAr'),'Leather Armor':('1out','ltAr'),'Iron Armor':('1out','irAr'),'Leather Helmet':('5hat','leHl'),'Iron Helmet':('5hat','irHl'),'Helm':('5hat','stHl'),'Leather Boot':('8feet','leBt'),'Iron Boot':('8feet','irBt'),'Belt':('9belt','belt'),'Bag':('9belt','bag')}
def path(page,layer,name):return C/f'char_a_{page}'/('' if layer=='0bas' else layer)/f'char_a_{page}_{layer}_{name}_v01.png'
def read(page,layer,name):return Image.open(path(page,layer,name)).convert('RGBA')
def frame(im,r,c):return im.crop((c*64,r*64,c*64+64,r*64+64))
def recolor(im,mapping):
 out=im.copy();out.putdata([(*mapping.get(px[:3],px[:3]),255) if px[3] else (0,0,0,0) for px in im.get_flattened_data()]);return out
for page in ('p1','pONE2','pONE3','pBOW','pCAST'):
 ranged=page in ('pBOW','pCAST');sourcepage='pONE2' if ranged else page
 cloth=read(sourcepage,'1out','fstr');hat=read(sourcepage,'5hat','pfht');heads=read(sourcepage,'4har','bob1');body=read(page,'0bas','humn')
 sheets={name:Image.new('RGBA',(512,512)) for name in list(MATERIALS)+list(HATS)+['leBt','irBt','belt','bag']}
 for row in range(8):
  for col in range(8):
   r,c=(row%4,0) if ranged else (row,col);native=frame(cloth,r,c);helmet=frame(hat,r,c);head=frame(heads,r,c);hb=head.getbbox()
   if not hb:hb=frame(read('pONE2','4har','bob1'),row%4,0).getbbox()
   cx=round((hb[0]+hb[2]-1)/2);b=frame(body,row,col);bb=b.getbbox()
   for name,colors in MATERIALS.items():
    q=recolor(native,dict(zip(SOURCE,colors)))
    if ranged:
     for y in range(hb[1]+12,min(64,hb[1]+26)):
      for x in range(64):
       if abs(x-cx)>4:q.putpixel((x,y),(0,0,0,0))
    sheets[name].alpha_composite(q,(col*64,row*64))
   for name,ramp in HATS.items():
    src=[(107,50,53),(169,73,76),(234,114,104),(252,176,153)]
    q=recolor(helmet,dict(zip(src,ramp)));sheets[name].alpha_composite(q,(col*64,row*64))
   if bb:
    foot=bb[3]
    for name,ramp in [('leBt',LEATHER),('irBt',METAL)]:
     q=Image.new('RGBA',(64,64));mapping=dict(zip(SOURCE,[OUTLINE,*ramp[:3],*ramp[:3],ramp[0],ramp[1]]))
     for y in range(max(bb[1],foot-4),foot):
      for x in range(64):
       px=native.getpixel((x,y))
       if px[3]:q.putpixel((x,y),(*mapping[px[:3]],255))
     sheets[name].alpha_composite(q,(col*64,row*64))
    waist=min(foot-7,hb[1]+23)
    for name in ('belt','bag'):
     q=Image.new('RGBA',(64,64));d=ImageDraw.Draw(q)
     if name=='belt':
      for x in range(max(0,cx-5),min(64,cx+6)):
       if native.getpixel((x,waist))[3]:d.point((x,waist),fill=(*LEATHER[0],255))
      if row%4!=1:d.point((cx,waist),fill=(178,145,73,255))
     else:
      side=-1 if row%4==3 else 1;bx=cx+side*4
      d.rectangle((bx-1,waist,bx+1,waist+3),fill=(*LEATHER[0],255));d.line((bx,waist+1,bx,waist+2),fill=(*LEATHER[2],255))
     sheets[name].alpha_composite(q,(col*64,row*64))
 for name,im in sheets.items():
  layer='1out' if name in MATERIALS else '5hat' if name in HATS else '8feet' if name.endswith('Bt') else '9belt';p=path(page,layer,name);p.parent.mkdir(parents=True,exist_ok=True);im.save(p)
# Match inventory objects to the equipped production pixels, at integer scale only.
icons=A/'equipment_items';icons.mkdir(exist_ok=True)
for item,(layer,name) in ITEMS.items():
 im=frame(read('p1',layer,name),0,0);im=im.crop(im.getbbox());scale=min(2,28//max(im.size));im=im.resize((im.width*scale,im.height*scale),Image.Resampling.NEAREST)
 icon=Image.new('RGBA',(32,32));icon.alpha_composite(im,((32-im.width)//2,(32-im.height)//2));icon.save(icons/f'{item}.png')
print('50 native equipment sheets and 10 matching icons built from original animation masks and material colors.')
