"""Import original guild pack at native pixel resolution; no resampling of features.
Run with the supplied PNG sheets in SOURCE (default upload/).
"""
from pathlib import Path
from PIL import Image
import json, shutil, sys
SOURCE=Path(sys.argv[1] if len(sys.argv)>1 else 'upload')
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'app/src/main/assets/village'
PACK=OUT/'guild';PACK.mkdir(parents=True,exist_ok=True)
def read(n):return Image.open(SOURCE/n).convert('RGBA')
def save(im,n):im.save(OUT/(n+'.png'))
def paste(dst,src,box,pos):dst.alpha_composite(src.crop(box),pos)
ext=read('exterior.png');hall=read('Exterior(1).png');objects=read('Interior_objects(2).png');interior=read('Interior.png');walls=read('Walls_interior(2).png');street=read('Walls_street(1).png')
forge=Image.open(ROOT/'app/src/main/assets/sprites/Exterior_house.png').convert('RGBA')
# The supplied hall needs its separate roof layered over the bare upper facade.
guild=hall.crop((0,0,160,144));paste(guild,hall,(160,0,256,64),(32,0));paste(guild,hall,(168,64,248,96),(40,84));save(guild,'guildhall')
# All houses use original, unscaled roof, facade, door and window pixels.
cottage=ext.crop((0,0,144,128));save(cottage,'cottage')
residence=Image.new('RGBA',(112,144));paste(residence,hall,(40,32,120,140),(16,32));paste(residence,hall,(160,0,256,64),(8,0));save(residence,'residence')
# Isolate a crop's main component to exclude adjacent atlas fragments.
def component(src,box):
 import numpy as np
 from scipy.ndimage import label
 crop=src.crop(box);a=np.array(crop);labels,count=label(a[:,:,3]>0)
 sizes=np.bincount(labels.ravel());sizes[0]=0
 a[labels!=sizes.argmax(),3]=0
 return Image.fromarray(a)
inn=Image.new('RGBA',(152,144))
inn.alpha_composite(component(forge,(144,86,256,160)),(20,64))
inn.alpha_composite(component(forge,(264,88,400,184)),(8,0))
inn.alpha_composite(component(forge,(152,24,216,74)),(44,14))
paste(inn,hall,(40,32,64,36),(10,102));paste(inn,hall,(40,32,44,48),(10,104))
inn.alpha_composite(component(forge,(320,168,344,200)),(0,104));save(inn,'inn')
bakery=cottage.copy();bakery.alpha_composite(component(forge,(267,15,316,77)),(80,64));save(bakery,'bakery')
# Large watchtower built from the same blue-grey masonry as the supplied walls.
tower=Image.new('RGBA',(80,160))
for y in range(32,160,16):
 for x in range(16,64,16):paste(tower,street,(48,16,64,32),(x,y))
for y in range(32,160,16):
 paste(tower,street,(48,16,52,32),(12,y));paste(tower,street,(76,16,80,32),(64,y))
for x in range(8,72,16):paste(tower,street,(16,0,32,16),(x,16))
# Retain original arched opening and original windows at native dimensions.
paste(tower,ext,(100,96,124,128),(28,128))
window=ext.crop((24,100,40,120))
paste(tower,window,(0,0,16,20),(32,52));paste(tower,window,(0,0,16,20),(32,88));save(tower,'tower')
# Replace every generated prop with native components from the reference sheets.
component(objects,(48,160,96,224)).save(OUT/'quests.png')
seal=objects.crop((48,8,128,56))
# This tiny corner belongs to the neighbouring stair sprite, not the seal.
seal.paste((0,0,0,0),(0,39,16,48));save(seal,'shrine')
market=Image.new('RGBA',(80,80))
market.alpha_composite(component(objects,(312,168,364,224)),(16,0))
market.alpha_composite(component(objects,(296,224,360,264)),(8,40));save(market,'market')
sign=Image.new('RGBA',(32,48));paste(sign,ext,(176,0,192,48),(8,0))
sign.alpha_composite(component(forge,(320,168,344,200)),(4,0));save(sign,'sign')
# A complete original banner replaces the unrelated generated lamp design.
flags=read('Flags_animation.png');flags.crop((0,0,48,48)).save(OUT/'lantern.png')
# Preserve the full 240x96 road material so neighbouring cells keep their pattern.
street.crop((0,192,240,288)).save(OUT/'path.png')
# Imported originals, including the separate shadow sheets, remain available.
files=['Exterior(1).png','Interior_objects(2).png','Walls_interior(2).png','Walls_street(1).png','Windows_doors.png','Decorative_cracks(1).png','Fire.png','Flags_animation.png','Interior.png','Guildmaster.png','Reader1.png','Reader2.png','Talking_people.png','Talking_people_without_shadow.png','Talking_people_shadow.png','Talking_person1.png','Talking_person1_without_shadow.png','Talking_person2.png','Talking_person2_without_shadow.png','Different_characters_shadow.png','Shadow_fighter.png','shadow_manequin.png','Mages_shadow.png','Fighter_sword_with_shadow.png','Fighter_sword_without_shadow.png']
for k in ['Citizen1','Citizen2','Fighter2']:
 for a in ['Idle','Walk']:
  files.extend([f'{k}_{a}.png',f'{k}_{a}_without_shadow.png'])
for k in range(1,5):files.extend([f'Mage{k}.png',f'Mage{k}_without_shadow.png'])
for k in range(1,4):files.extend([f'Attacked_Manequin{k}_with_shadow.png',f'Attacked_Manequin{k}_without_shadow.png'])
for name in files:shutil.copyfile(SOURCE/name,PACK/name)
component(objects,(192,2,384,88)).save(PACK/'dragon_trophy.png')
# Native room shells: 336x176 native pixels, rendered at exactly 2x.
for floor in range(3):
 room=Image.new('RGBA',(336,176))
 for y in range(0,176,16):
  for x in range(0,336,16):
   sx=288+((x//16+floor)%3)*16;sy=80+(y//16%3)*16
   paste(room,walls,(sx,sy,sx+16,sy+16),(x,y))
 # Tall back wall and side trims.
 for x in range(0,336,16):paste(room,walls,(16,80,32,128),(x,0))
 for x in [64,144,224]:paste(room,walls,(80,80,112,128),(x,0))
 for y in range(48,176,16):
  paste(room,walls,(96,0,100,16),(0,y));paste(room,walls,(96,0,100,16),(332,y))
 for x in range(0,336,16):
  if floor==0 and 144<=x<192:continue
  paste(room,walls,(16,120,32,128),(x,168))
 # Native carpets are floor decorations and stay passable.
 if floor==0:paste(room,interior,(16,240,80,272),(136,60))
 elif floor==1:paste(room,interior,(0,272,112,368),(112,57))
 else:paste(room,Image.open(OUT/'shrine.png').convert('RGBA'),(0,0,80,48),(128,92))
 room.save(PACK/f'floor{floor}.png')
print('Imported',len(files),'original sheets, three floor shells and native exterior replacements.')

# Composed native sprites use hard transparency; retain original RGB palette.
for name in ['cottage','residence','inn','bakery','guildhall','tower','market','quests','shrine','sign','lantern']:
 im=Image.open(OUT/(name+'.png')).convert('RGBA');im.putalpha(im.getchannel('A').point(lambda a:255 if a>=128 else 0));im.save(OUT/(name+'.png'))
