"""Export the actual house/furniture geometry for the portable resident simulation."""
from pathlib import Path
from PIL import Image,ImageDraw,ImageFilter
import re,struct,sys
ROOT=Path(__file__).resolve().parents[1];s=(ROOT/'app/src/main/java/de/pixelabenteuer/game/MainActivity.java').read_text();out=Path(sys.argv[1] if len(sys.argv)>1 else '/tmp/pixel-resident-world.bin')
houses=[tuple(map(float,p)) for p in re.findall(r'\{([\d.]+),([\d.]+)\}',re.search(r'villageHouses=\{(.*?)};',s).group(1))];offset=[0,64,0,0,64,0]
objects=[]
for args in re.findall(r'guildFurniture\((\d+,"[^"]+",[^;]+)\);',s):
 a=args.split(',');objects.append((int(a[0]),[float(v.rstrip('f')) for v in a[2:]]))
world=Image.new('L',(1600,1000));d=ImageDraw.Draw(world)
for i,(x,y) in enumerate(houses[1:],1):
 d.rectangle((x-111,y+48,x+111,y+116),fill=1);d.rectangle((x+offset[i]-19,y+95,x+offset[i]+19,y+116),fill=0)
forge=Image.open(ROOT/'app/src/main/assets/sprites/Exterior_house.png').convert('RGBA').crop((0,0,145,163));alpha=forge.getchannel('A').resize((232,261),Image.Resampling.NEAREST).point(lambda a:1 if a>48 else 0).filter(ImageFilter.MaxFilter(15));world.paste(alpha,(434,160),alpha.point(lambda a:255 if a else 0));d=ImageDraw.Draw(world);d.rectangle((527,379,573,450),fill=0)
for (sx,sy,sw,sh),(x,y,w,h) in [((70,162,32,38),(433,413,50,52)),((116,160,34,34),(647,366,55,55))]:
 im=Image.open(ROOT/'app/src/main/assets/sprites/Exterior_house.png').convert('RGBA').crop((sx,sy,sx+sw,sy+sh)).getchannel('A').resize((w,h),Image.Resampling.NEAREST).point(lambda a:1 if a>48 else 0).filter(ImageFilter.MaxFilter(15));world.paste(im,(x,y),im.point(lambda a:255 if a else 0))
d=ImageDraw.Draw(world)
def circle(x,y,r):d.ellipse((x-r,y-r,x+r,y+r),fill=1)
for x,y in [(110,310),(104,630),(106,820),(335,150),(690,150),(1000,220),(1000,575),(1010,800),(150,935),(360,945),(600,944),(840,947)]:circle(x,y,17)
for side in range(4):
 for lane in range(2):
  for i in range(15):
   x,y=(40+lane*55 if side==0 else 1090+lane*120,90+i*62) if side<2 else (70+i*100,62+lane*34 if side==2 else 965+lane*30)
   if side<2 and abs(y-470)<88 or side==2 and abs(x-410)<82 or x>1560 or y>995:continue
   circle(x,y,17)
circle(610,525,24);circle(1010,350,47);circle(730,530,17);circle(982,562,15);circle(984,612,15)
for x,y in houses[1:]:circle(x-100,y+132,19);circle(x+101,y+125,16)
roads=Image.new('L',(1600,1000));rd=ImageDraw.Draw(roads)
for r in [(160,195,450,235),(386,195,434,932),(666,450,714,932),(180,450,1210,490),(180,820,930,860),(430,442,666,570),(0,446,190,494),(920,446,1599,494),(386,0,434,220),(992,385,1028,470)]:rd.rectangle(r,fill=1)
for (x,y),dx in zip(houses,offset):rd.rectangle((x+dx-20,y+100,x+dx+20,840 if y>500 else 470),fill=1)
rd.ellipse((385,199,435,231),fill=0)
entries=[]
def sample(room,im,minx,miny,maxx,maxy,road=None):
 step=2;w=(maxx-minx)//step+1;h=(maxy-miny)//step+1;pix=im.load();buf=bytes(1 if pix[minx+x*step,miny+y*step] else 0 for y in range(h) for x in range(w));entries.append((room,minx,miny,step,w,h,buf))
 if road is not None:
  p=road.load();entries.append((-100,minx,miny,step,w,h,bytes(1 if p[minx+x*step,miny+y*step] else 0 for y in range(h) for x in range(w))))
sample(-1,world,100,110,1020,900,roads)
for room in range(7):
 im=Image.new('L',(673,353),1);draw=ImageDraw.Draw(im);draw.rectangle((14,108,658,332),fill=0)
 for f,v in objects:
  if f!=room:continue
  sx,sy,w,h,x,y,scale,fw,fh=v
  if fw>0:draw.rectangle((x-fw/2-6,y-fh-6,x+fw/2+6,y+6),fill=1)
 sample(room,im,0,0,672,352)
with out.open('wb') as f:
 f.write(struct.pack('>i',len(entries)))
 for room,x,y,step,w,h,buf in entries:f.write(struct.pack('>6i',room,x,y,step,w,h));f.write(buf)
print('Exported resident navigation fixtures from',len(houses),'houses and',len(objects),'furniture bases to',out)
