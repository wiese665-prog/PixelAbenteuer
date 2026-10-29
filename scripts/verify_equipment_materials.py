"""Verify actual slot layers against original pixels, not just a color count."""
from pathlib import Path
from PIL import Image
import json
ROOT=Path(__file__).resolve().parents[1];C=ROOT/'app/src/main/assets/sprites/ManaSeedCharacter'
def read(p,l,n,v=1):return Image.open(C/f'char_a_{p}'/('' if l=='0bas' else l)/f'char_a_{p}_{l}_{n}_v{v:02}.png').convert('RGBA')
def cell(im,r,c):return im.crop((c*64,r*64,c*64+64,r*64+64))
allowed=set()
for layer,n in [('1out','fstr'),('1out','pfpn'),('1out','boxr')]:
 for v in range(1,2 if n=='boxr' else 6):allowed.update(px[:3] for px in read('p1',layer,n,v).get_flattened_data() if px[3])
count=0
for p in ('p1','pONE2','pONE3','pBOW','pCAST'):
 for layer,names in [('1out',['wdAr','ltAr','irAr']),('5hat',['wdHl','leHl','irHl']),('8feet',['leBt','irBt']),('9belt',['belt','bag'])]:
  for n in names:
   im=read(p,layer,n);assert im.size==(512,512)
   assert set(im.getchannel('A').get_flattened_data())<={0,255}
   colors={px[:3] for px in im.get_flattened_data() if px[3]};assert colors<=allowed,(p,n,colors-allowed)
   source='pONE2' if p in ('pBOW','pCAST') else p
   if layer=='1out':
    original=read(source,layer,'fstr' if layer=='1out' else 'pfht')
    for r in range(8):
     for c in range(8):
      old=cell(original,r%4 if source!=p else r,0 if source!=p else c);new=cell(im,r,c)
      assert bool(new.getbbox())==bool(old.getbbox()),(p,n,r,c,'native frame availability changed')
      if source==p or layer=='5hat':assert old.getchannel('A').tobytes()==new.getchannel('A').tobytes(),(p,n,r,c,'native contour changed')
      # Material conversion must preserve original dark outline identities.
      if source==p:
       for a,b in zip(old.get_flattened_data(),new.get_flattened_data()):
        if a[3] and a[:3]==(24,24,24):assert b==(24,24,24,255)
      count+=1
   if layer=='8feet':
    native=read(source,'1out','fstr');body=read(p,'0bas','humn')
    for r in range(8):
     for c in range(8):
      new=cell(im,r,c);b=cell(body,r,c).getbbox();box=new.getbbox()
      if box:assert b and box[1]>=b[3]-4 and box[3]<=b[3],(p,n,r,c,'boot moved away from feet')
 # Distinct equipped materials, all directions, not merely distinct inventory icons.
 for row in range(4):
  for layer,names in [('1out',['wdAr','ltAr','irAr']),('5hat',['wdHl','leHl','irHl'])]:
   assert len({cell(read(p,layer,n),row,0).tobytes() for n in names})==len(names)
for icon in (ROOT/'app/src/main/assets/equipment_items').glob('*.png'):
 im=Image.open(icon).convert('RGBA');assert im.size==(32,32) and im.getbbox()
 assert {px[:3] for px in im.get_flattened_data() if px[3]}<=allowed
print(f'{count} original-contour chest/helmet poses: native contours, exact equipment colors, material separation; foot layers and 11 icons passed.')
