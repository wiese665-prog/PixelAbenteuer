"""Check the redesigned silhouettes, native palettes and per-pose head anchoring."""
from PIL import Image
from pathlib import Path
from native_headgear import headgear
ROOT=Path(__file__).resolve().parents[1];C=ROOT/'app/src/main/assets/sprites/ManaSeedCharacter'
def read(page,layer,name):return Image.open(C/f'char_a_{page}'/layer/f'char_a_{page}_{layer}_{name}_v01.png').convert('RGBA')
def frame(im,r,c):return im.crop((c*64,r*64,c*64+64,r*64+64))
allowed=set()
for n in ['fstr','pfpn','boxr']:
 for v in range(1,2 if n=='boxr' else 6):
  p=C/'char_a_p1/1out'/f'char_a_p1_1out_{n}_v{v:02}.png';allowed.update(q[:3] for q in Image.open(p).convert('RGBA').get_flattened_data() if q[3])
count=0
for page in ('p1','pONE2','pONE3','pBOW','pCAST'):
 source='pONE2' if page in ('pBOW','pCAST') else page;heads=read(source,'4har','bob1');fallback=read('pONE2','4har','bob1')
 sheets={n:read(page,'5hat',n) for n in ['wdHl','leHl','irHl','hood','mgHt']}
 for r in range(8):
  for c in range(8):
   cap=frame(heads,r%4 if source!=page else r,0 if source!=page else c)
   if not cap.getbbox():cap=frame(fallback,r%4,0)
   hb=cap.getbbox();cx=round((hb[0]+hb[2]-1)/2)
   masks={}
   for n,im in sheets.items():
    actual=frame(im,r,c);b=actual.getbbox();assert b and 0<b[0]<b[2]<64 and 0<b[1]<b[3]<64,(page,n,r,c,'clipped edge')
    assert hb[1]-8<=b[1]<=hb[1]+1 and b[3]<=hb[1]+17,(page,n,r,c,'head anchor moved')
    assert b[0]>=cx-10 and b[2]<=cx+11,(page,n,r,c,'oversized')
    assert actual.tobytes()==headgear(n,cap,r%4).tobytes(),(page,n,r,c,'wrong head frame')
    assert set(actual.getchannel('A').get_flattened_data())<={0,255}
    assert {px[:3] for px in actual.get_flattened_data() if px[3]}<=allowed,(page,n,'foreign palette')
    if n=='irHl' and r%4==0:
     for yy in range(64):
      for off in range(1,10):assert actual.getpixel((cx-off,yy))==actual.getpixel((cx+off,yy)),(page,r,c,'asymmetric iron front')
    masks[n]=set((x,y) for y in range(64) for x in range(64) if actual.getpixel((x,y))[3]);count+=1
   # Differentiation must be in silhouette, not just RGB recoloring.
   assert len({frozenset(m) for m in masks.values()})==5,(page,r,c,'recolored duplicate')
   a,b=masks['wdHl'],masks['irHl'];assert len(a&b)/len(a|b)<.85,(page,r,c,'wood/iron too similar')
for n in ('Wooden Helmet','Leather Helmet','Iron Helmet'):
 im=Image.open(ROOT/'app/src/main/assets/equipment_items'/f'{n}.png');assert im.size==(32,32)
print(f'{count} rebuilt headgear frames: five unique silhouettes, exact head anchors, original colors and no frame-edge clipping.')
