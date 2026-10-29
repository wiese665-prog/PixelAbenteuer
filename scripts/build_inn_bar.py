"""Small tavern counter on the original pixel grid and original furniture palette."""
from PIL import Image,ImageDraw
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1];A=ROOT/'app/src/main/assets/village/guild'
source=Image.open(A/'Interior.png').convert('RGBA')
outline=(55,44,41);dark=(74,44,44);shadow=(102,65,56);wood=(120,78,63);mid=(141,95,69);light=(157,109,71)
im=Image.new('RGBA',(96,40));d=ImageDraw.Draw(im)
def c(rgb):return (*rgb,255)
d.polygon([(3,7),(90,7),(95,13),(95,20),(1,20),(1,13)],fill=c(outline))
d.polygon([(5,8),(89,8),(93,13),(3,13)],fill=c(light));d.rectangle((3,14,93,17),fill=c(mid));d.line((3,18,93,18),fill=c(dark))
d.rectangle((5,19,91,36),fill=c(wood))
for x in range(8,90,14):
 d.rectangle((x,21,x+10,33),fill=c(mid));d.line((x+10,21,x+10,34),fill=c(shadow));d.point((x+2,24),fill=c(light))
d.line((5,35,91,35),fill=c(dark),width=2);d.rectangle((5,36,12,39),fill=c(outline));d.rectangle((84,36,91,39),fill=c(outline))
counter=im;im=Image.new('RGBA',(96,44));im.alpha_composite(counter,(0,4))
# Exact complete bottle components, excluding neighboring caps and fragments.
for box,xy in [((180,241,189,254),(20,0)),((178,256,189,271),(66,0))]:
 bottle=source.crop(box);bbox=bottle.getbbox()
 if bbox:im.alpha_composite(bottle.crop(bbox),(xy[0],15-bbox[3]+bbox[1]))
im.save(A/'inn_bar.png')
print('Native tavern counter with original wood colors and complete bottle sprites.')

# Place the existing native rug inside the sleeping alcove, not in the empty hall.
floor=Image.open(A/'floor4.png').convert('RGBA')
walls=Image.open(A/'Walls_interior(2).png').convert('RGBA')
for y in range(64,112,16):
 for x in range(128,208,16):
  sx=288+((x//16+4)%3)*16;sy=80+(y//16%3)*16
  floor.paste(walls.crop((sx,sy,sx+16,sy+16)),(x,y))
original=Image.open(A.parent/'Interior.png').convert('RGBA')
floor.alpha_composite(original.crop((32,240,112,272)),(38,74))
floor.save(A/'floor4.png')
