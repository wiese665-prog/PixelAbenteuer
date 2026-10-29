"""Native asset corrections, complete animated house doors and additional room shells.
Run after import_guild_pack.py, with original sheets in SOURCE (default upload/).
"""
from pathlib import Path
from PIL import Image
import numpy as np
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'app/src/main/assets/village';PACK=OUT/'guild'
ext=Image.open(OUT/'exterior.png').convert('RGBA');hall=Image.open(PACK/'Exterior(1).png').convert('RGBA');walls=Image.open(PACK/'Walls_interior(2).png').convert('RGBA');interior=Image.open(OUT/'Interior.png').convert('RGBA');forge=Image.open(ROOT/'app/src/main/assets/sprites/Exterior_house.png').convert('RGBA')
for im in [ext,hall,walls,interior,forge]:im.putalpha(im.getchannel('A').point(lambda a:255 if a>=128 else 0))
# Remove the entire awning from the bakery, preserving its original wall pixels.
Image.open(OUT/'cottage.png').save(OUT/'bakery.png')
# Replace the cottage chimneys with the animated chimney from the supplied sheet.
for name in ['cottage','bakery']:
 im=Image.open(OUT/(name+'.png')).convert('RGBA');a=np.array(im);old=a.copy()
 for y in range(8,56):
  for x in range(28,51):
   r,g,b,alpha=map(int,old[y,x])
   if alpha>0 and b>=r*.9 and g>=r*.95:
    a[y,x]=old[y-16,x+16] if y>=16 else [0,0,0,0]
 Image.fromarray(a).save(OUT/(name+'.png'))
smoke=Image.open(OUT/'Smoke_animation.png').convert('RGBA');chimney=Image.new('RGBA',(48,288))
for f in range(6):
 a=np.array(smoke.crop((f*48,0,f*48+48,48)))
 for y in range(48):
  for x in range(48):
   r,g,b,alpha=map(int,a[y,x])
   if y>=28 and not 14<=x<32 or y>=14 and x>=32 or r>g*1.2 and r>b*1.15:a[y,x,3]=0
 chimney.alpha_composite(Image.fromarray(a),(0,f*48))
chimney.save(OUT/'chimney_animation.png')
# Distinct long-front inn: matching plaster, closed timber sides and one tiled roof.
windows=Image.open(PACK/'Windows_doors.png').convert('RGBA')
def component(im,box):
 from scipy.ndimage import label
 a=np.array(im.crop(box));lab,_=label(a[:,:,3]>0);n=np.bincount(lab.ravel());n[0]=0;a[lab!=n.argmax(),3]=0;return Image.fromarray(a)
inn=Image.new('RGBA',(176,144))
# One coherent timber-framed facade: native plaster modules with four windows.
for x in [24,56,88,120]:inn.alpha_composite(hall.crop((48,48,80,96)),(x,48))
for x in [16,152]:inn.alpha_composite(hall.crop((48,48,56,96)),(x,48))
for x in range(16,160,16):inn.alpha_composite(hall.crop((44,96,60,144)),(x,96))
for x in [24,136]:inn.alpha_composite(windows.crop((128,0,144,48)),(x,96))
for x in range(16,160,16):
 inn.alpha_composite(hall.crop((48,90,64,96)),(x,90))
 inn.alpha_composite(hall.crop((44,136,60,140)),(x,136))
inn.alpha_composite(hall.crop((40,96,120,144)),(48,96))
# One continuous roof, with complete native left/right caps and aligned tiles.
inn.alpha_composite(hall.crop((160,0,176,64)),(8,0))
for x in range(24,152,16):inn.alpha_composite(hall.crop((176+((x-24)//16%3)*16,0,192+((x-24)//16%3)*16,64)),(x,0))
inn.alpha_composite(hall.crop((240,0,256,64)),(152,0))
# Actual timber from the facade. Roof pixels must never become wall pillars.
beam=hall.crop((42,64,46,72))
for y in range(64,136,8):
 for x in [16,156]:inn.alpha_composite(beam,(x,y))
inn.save(OUT/'inn.png')
# Straight, uninterrupted timber boundaries on the residence. No wing fragments.
home=Image.open(OUT/'residence.png').convert('RGBA')
for y in range(64,136,8):
 for x in [16,92]:home.alpha_composite(hall.crop((42,64,46,72)),(x,y))
a=np.array(home);a[56:140,:16,3]=0;a[56:140,96:,3]=0;Image.fromarray(a).save(OUT/'residence.png')
Image.open(ROOT/'art/source/watchtower-native.png').save(OUT/'tower.png')
# Original directional grass: dark downward blades along a road's north edge,
# light upward blades at its south edge. Keep native pixels and transparency.
edges=Image.new('RGBA',(64,16))
for i,box in enumerate([(16,208,32,224),(16,176,32,192),(32,192,48,208),(0,192,16,208)]):
 tile=ext.crop(box);a=np.array(tile);flat=(a[:,:,:3]==[122,173,85]).all(2);a[flat,:3]=[117,158,83];edges.alpha_composite(Image.fromarray(a),(i*16,0))
edges.save(OUT/'grass_edges.png')
# Bake frame-aligned building sequences. The original moving door leaves are
# copied only where the source animation differs from its closed frame.
source=Image.open(ROOT/'app/src/main/assets/sprites/Doors_windows_animations.png').convert('RGBA')
closed=np.array(source.crop((7,6,25,28)))
leaves=[]
for f in range(6):
 leaf=np.array(source.crop((7,6+f*96,25,28+f*96)));mask=(leaf!=closed).any(2);leaf[:,:,3]=np.where(mask,255,0);leaves.append(Image.fromarray(leaf))
base=[forge.crop((0,0,145,163))]+[Image.open(OUT/(n+'.png')).convert('RGBA') for n in ['cottage','inn','residence','bakery','guildhall']]
rects=[(63,135,80,156),(102,99,121,124),(80,113,97,136),(48,113,65,136),(102,99,121,124),(72,113,89,136)]
for index,(im,box) in enumerate(zip(base,rects)):
 strip=Image.new('RGBA',(im.width,im.height*6));x,y,r,b=box
 for f in range(6):
  frame=im.copy()
  if f not in [0,5]:frame.alpha_composite(leaves[f].resize((r-x,b-y),Image.Resampling.NEAREST),(x,y))
  strip.alpha_composite(frame,(0,f*im.height))
 strip.save(OUT/f'door{index}.png')
# Four distinct native room backgrounds. Foreground furniture is depth sorted
# separately in Java so residents and the player can pass behind objects.
for room in range(3,7):
 im=Image.new('RGBA',(336,176))
 for y in range(0,176,16):
  for x in range(0,336,16):
   sx=288+((x//16+room)%3)*16;sy=80+(y//16%3)*16
   im.alpha_composite(walls.crop((sx,sy,sx+16,sy+16)),(x,y))
 for x in range(0,336,16):im.alpha_composite(walls.crop((16,80,32,128)),(x,0))
 for x in [64,144,224]:im.alpha_composite(walls.crop((80,80,112,128)),(x,0))
 for y in range(48,176,16):
  for x in [0,332]:im.alpha_composite(walls.crop((96,0,100,16)),(x,y))
 for x in range(0,336,16):
  if not 144<=x<192:im.alpha_composite(walls.crop((16,120,32,128)),(x,168))
 if room==3:im.alpha_composite(interior.crop((12,280,102,359)),(123,76))
 if room==4:im.alpha_composite(interior.crop((32,240,112,272)),(38,74))
 if room==5:im.alpha_composite(interior.crop((126,297,178,359)),(142,76))
 im.save(PACK/f'floor{room}.png')
print('Imported tower, straight residence, unadorned bakery, four grass edges, 6 animated houses and 4 room shells.')

import shutil
shutil.copyfile(ROOT/'app/src/main/assets/sprites/Forge.png',PACK/'Forge.png')

# Use only an opaque paving material block. No wall/transition cells can enter roads.
street=Image.open(PACK/'Walls_street(1).png').convert('RGBA')
street.crop((0,192,48,240)).save(OUT/'path.png')
# Small complete bushes and full grass/flower cells. Explicit native rectangles
# keep disconnected blossoms with their stems and cannot spill into other cells.
boxes=[(224,208,240,224),(224,225,240,239),(213,245,238,267),
       (128,768,144,800),(144,768,160,800),(160,768,176,800),
       (176,768,208,800),(208,768,224,800),(224,768,240,800)]
details=Image.new('RGBA',(32*len(boxes),32))
for n,box in enumerate(boxes):
 im=ext.crop(box);assert im.width<=32 and im.height<=32
 details.alpha_composite(im,(n*32+(32-im.width)//2,32-im.height))
details.save(OUT/'grass_details.png')

# Keep the approved replacement and its own aligned door strip on regeneration.
if (ROOT/"art/source/inn-approved.png").exists():
 import runpy
 runpy.run_path(str(ROOT/"scripts/build_approved_inn.py"))
