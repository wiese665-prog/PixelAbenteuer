"""Review-only buildings, composed at original pixel scale; no game assets overwritten."""
from pathlib import Path
from PIL import Image,ImageDraw
ROOT=Path(__file__).resolve().parents[2];A=ROOT/'app/src/main/assets';OUT=Path(__file__).resolve().parent
forge=Image.open(A/'sprites/Exterior_house.png').convert('RGBA')
ext=Image.open(A/'village/exterior.png').convert('RGBA')
hall=Image.open(A/'village/guild/Exterior(1).png').convert('RGBA')
for im in [forge,ext,hall]:im.putalpha(im.getchannel('A').point(lambda a:255 if a>=128 else 0))
# Inn: complete sloping cottage roof, a timber middle storey, stone ground floor.
inn=Image.new('RGBA',(176,176))
inn.alpha_composite(forge.crop((144,87,256,160)),(32,88))
inn.alpha_composite(ext.crop((0,0,145,96)),(16,0))
# A tiny purpose-drawn mug sign uses only colors sampled from the original art.
wood=[p for p in forge.crop((150,90,160,110)).get_flattened_data() if p[3]]
brown=forge.getpixel((150,94));light=hall.getpixel((60,70));dark=min(wood,key=lambda p:sum(p[:3]));gold=max(wood,key=lambda p:sum(p[:3]))
d=ImageDraw.Draw(inn);d.rectangle((23,109,34,110),fill=dark);d.rectangle((24,110,25,117),fill=dark);d.rectangle((19,117,31,132),fill=dark);d.rectangle((20,118,30,131),fill=brown);d.rectangle((21,119,29,120),fill=gold);d.rectangle((23,122,26,128),fill=light);d.rectangle((27,123,28,126),fill=light);d.point((27,125),fill=brown)
# Watchtower: narrow masonry shaft, single-window lookout, complete native gable.
tower=Image.new('RGBA',(128,224))
stone=forge.crop((150,126,166,150))
assert stone.getchannel('A').getextrema()==(255,255)
for y in range(88,216,24):
 for x in range(40,88,16):
  h=min(24,216-y);tower.alpha_composite(stone.crop((0,0,16,h)),(x,y))
for x in range(36,92,8):tower.alpha_composite(forge.crop((150,152,158,158)),(x,216))
for y in [115,150]:tower.alpha_composite(forge.crop((168,126,184,144)),(56,y))
tower.alpha_composite(forge.crop((188,128,212,157)),(52,187))
# Native left/right diagonal framing and one complete central window module.
tower.alpha_composite(forge.crop((144,87,156,120)),(36,64))
tower.alpha_composite(forge.crop((162,87,194,120)),(48,64))
tower.alpha_composite(forge.crop((244,87,256,120)),(80,64))
tower.alpha_composite(forge.crop((152,24,216,74)),(32,17))
allowed={p for im in [forge,ext,hall] for p in im.get_flattened_data() if p[3]}
for name,im in [('inn-native-review',inn),('tower-native-review',tower)]:
 assert {p for p in im.get_flattened_data() if p[3]}<=allowed
 assert set(im.getchannel('A').get_flattened_data())<={0,255}
 im.save(OUT/(name+'.png'))
 canvas=Image.new('RGBA',im.size,'#759e53');canvas.alpha_composite(im)
 canvas.resize((im.width*4,im.height*4),Image.Resampling.NEAREST).save(OUT/(name+'-4x.png'))
print('Two proposals: native pixel scale, binary alpha, 100% source palette. Review only.')
