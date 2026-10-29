"""Check imported sprite bounds and guild walking/stair connectivity; render a review sheet."""
from PIL import Image,ImageDraw
from pathlib import Path
import re
from collections import deque
ROOT=Path(__file__).resolve().parents[1]
s=(ROOT/'app/src/main/java/de/pixelabenteuer/game/MainActivity.java').read_text();base=ROOT/'app/src/main/assets/village';objects=[]
for args in re.findall(r'guildFurniture\((\d+,"[^"]+",[^;]+)\);',s):
 vals=args.split(',');f=int(vals[0]);name=vals[1].strip('"');v=[float(x.rstrip('f')) for x in vals[2:]];objects.append((f,name,v))
for floor in range(7):
 im=Image.open(base/'guild'/f'floor{floor}.png').convert('RGBA').resize((672,352),Image.Resampling.NEAREST)
 for x in ([544] if floor==0 else [32] if floor==2 else [32,544] if floor==1 else []):
  stairs=Image.open(base/'guild/Interior.png').crop((0,0,48,64)).resize((96,128),Image.Resampling.NEAREST);im.alpha_composite(stairs,(x,88))
 for f,n,v in sorted(objects,key=lambda q:q[2][5]):
  if f!=floor:continue
  sx,sy,w,h,x,y,scale,fw,fh=v;sheet=Image.open(base/'guild'/(n+'.png')).convert('RGBA');assert sx>=0 and sy>=0 and sx+w<=sheet.width and sy+h<=sheet.height,(n,v)
  art=sheet.crop((int(sx),int(sy),int(sx+w),int(sy+h))).resize((round(w*scale),round(h*scale)),Image.Resampling.NEAREST)
  im.alpha_composite(art,(round(x-w*scale/2),round(y-h*scale)))
 for a in re.findall(r'guildPose\('+str(floor)+r',"([^"]+)","([^"]+)",(\d+),(\d+),(\d+),(\d+),([\d.]+),([\d.]+),([\d.]+)f?\)',s):
  n,label,cw,ch,col,foot,x,y,scale=a;cw,ch,col,foot=map(int,[cw,ch,col,foot]);x,y,scale=map(float,[x,y,scale]);sheet=Image.open(base/'guild'/(n+'.png')).convert('RGBA');assert cw*col<=sheet.width and ch<=sheet.height,n
  art=sheet.crop((0,0,cw,ch)).resize((round(cw*scale),round(ch*scale)),Image.Resampling.NEAREST);im.alpha_composite(art,(round(x-cw*scale/2),round(y-foot*scale)))
 def blocked(x,y):
  if x<14 or x>658 or y<108 or y>332:return True
  return any(f==floor and v[7]>0 and v[4]-v[7]/2-6<x<v[4]+v[7]/2+6 and v[5]-v[8]-6<y<v[5]+6 for f,n,v in objects)
 seen={(336,312)};q=deque(seen)
 while q:
  x,y=q.popleft()
  for xy in [(x-4,y),(x+4,y),(x,y-4),(x,y+4)]:
   if xy not in seen and not blocked(*xy):seen.add(xy);q.append(xy)
 for x,y in [(336,312),(336,328)]+([(592,216),(592,256)] if floor<2 else [])+([(80,216),(80,256)] if 0<floor<3 else []):assert (x,y) in seen,(floor,x,y)
 for route in re.findall(r'guildWalker\('+str(floor)+r',[^;]+new float\[\]\[\]\{([^;]+)\}\);',s):
  pts=[tuple(map(float,m)) for m in re.findall(r'\{([\d.]+),([\d.]+)\}',route)]
  for i,(x,y) in enumerate(pts):
   tx,ty=pts[(i+1)%len(pts)]
   for k in range(101):assert not blocked(x+(tx-x)*k/100,y+(ty-y)*k/100),(floor,i,x,y,tx,ty,k)
  x,y=pts[0];a=Image.open(base/'guild/Citizen1_Idle_without_shadow.png').crop((0,0,32,32)).resize((51,51),Image.Resampling.NEAREST);im.alpha_composite(a,(round(x-25.6),round(y-48)))
 im.save(ROOT.parent/f'guild-floor-{floor}.png')
 print('Floor',floor,': exits/landings reachable; all roaming segments collision-free;',len(seen),'reachable foot-grid positions')
o=Image.new('RGB',(1344,1536),'#18141a');d=ImageDraw.Draw(o)
for f in range(7):
 x=(f%2)*672;y=(f//2)*384;d.text((x+8,y+4),['Gilde: Empfang / Training','Gilde: Bibliothek','Gilde: Gildenleitung / Magie','Kraeuterhaus','Gasthaus','Wohnhaus','Baeckerei'][f],fill='white');o.paste(Image.open(ROOT.parent/f'guild-floor-{f}.png'),(x,y+24))
o.save(ROOT.parent/'guild-preview.png')
