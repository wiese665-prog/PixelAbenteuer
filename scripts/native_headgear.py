"""New native-grid headgear silhouettes. Coordinates follow each original head frame."""
from PIL import Image,ImageDraw
from pathlib import Path
from functools import lru_cache
O=(24,24,24,255)
GREEN=[(54,74,63,255),(91,127,106,255),(168,200,152,255)]
BLUE=[(25,51,100,255),(58,103,151,255),(89,153,189,255)]
STEEL=[(82,82,95,255),(124,137,137,255),(167,181,167,255),(220,220,203,255)]
WOOD=[(88,56,40,255),(152,96,56,255),(240,152,72,255)]
LEATHER=[(61,18,0,255),(99,46,17,255),(141,99,64,255),(218,185,136,255)]
BRASS=[(129,92,33,255),(178,145,73,255),(224,210,142,255)]

def headgear(name,cap,facing):
 return native_form(name,cap,facing) if name=='mgHt' else tailored_form(name,cap,facing)

@lru_cache(None)
def source(kind,facing):
 root=Path(__file__).resolve().parents[1]/'app/src/main/assets/sprites/ManaSeedCharacter/char_a_p1'
 layer='4har' if kind=='bob1' else '5hat'
 sheet=Image.open(root/layer/f'char_a_p1_{layer}_{kind}_v01.png').convert('RGBA')
 return sheet.crop((0,facing*64,64,facing*64+64))

def native_form(name,cap,facing):
 # Original pixel clusters and lighting, fitted to the current native head pose.
 reference=source('bob1',facing).getbbox();target=cap.getbbox()
 sx=round((reference[0]+reference[2]-1)/2);cx=round((target[0]+target[2]-1)/2)
 dx=cx-sx;dy=target[1]-reference[1];top=target[1]
 kind='pnty' if name=='mgHt' else 'pfht';original=source(kind,facing)
 colors=sorted({p[:3] for p in original.get_flattened_data() if p[3]},key=lambda p:sum(p))
 if name=='mgHt':
  mapping={(24,24,24):O,(33,19,33):O,(30,43,65):BLUE[0],(60,85,117):BLUE[1],(98,147,173):BLUE[2],(153,214,227):(144,160,176,255),(91,24,57):BRASS[0],(141,34,70):BRASS[1],(190,86,103):BRASS[2]}
 else:
  ramp={'hood':[O,GREEN[0],GREEN[1],GREEN[1],GREEN[2]],'wdHl':[O,LEATHER[0],LEATHER[1],LEATHER[2],LEATHER[3]],'leHl':[O,*LEATHER]}[name]
  mapping=dict(zip(colors,ramp))
 out=Image.new('RGBA',(64,64))
 for y in range(64):
  for x in range(64):
   px=original.getpixel((x,y));xx=x+dx;yy=y+dy
   if px[3] and 0<=xx<64 and 0<=yy<64:
    # Round wooden skullcap; leather has open ears and a short rear flap.
    if name=='wdHl' and yy>top+(11 if facing==1 else 8):continue
    if name=='leHl' and yy>top+(13 if facing==1 else 7):continue
    out.putpixel((xx,yy),mapping[px[:3]])
 d=ImageDraw.Draw(out)
 def line(pts,color):d.line([(cx+x,top+y) for x,y in pts],fill=color)
 def poly(pts,color):d.polygon([(cx+x,top+y) for x,y in pts],fill=color)
 if name=='hood':
  # Wide folded opening and shoulder drape replace the tight round face ring.
  if facing==0:
   poly([(-5,11),(-3,13),(1,14),(5,11),(7,14),(3,16),(-4,16),(-7,14)],GREEN[0])
   line([(-5,13),(-2,14),(2,15),(5,13)],GREEN[1]);line([(-3,14),(0,15)],GREEN[2])
  elif facing==1:
   poly([(-5,11),(5,11),(7,15),(2,16),(-4,16),(-7,14)],GREEN[0])
   line([(-4,12),(-1,14),(2,15),(5,13)],GREEN[1])
  else:
   sign=1 if facing==2 else -1
   poly([(sign*-5,10),(sign*0,12),(sign*4,14),(sign*2,16),(sign*-5,15),(sign*-7,13)],GREEN[0])
   line([(sign*-5,12),(sign*-2,13),(sign*1,15)],GREEN[1])
 elif name=='wdHl':
  # Curved slats follow the dome, with a narrow leather fastening strap.
  for off in (-3,2):
   for y in range(top+2,top+7):
    x=cx+off
    if out.getpixel((x,y))[3]:d.point((x,y),fill=LEATHER[0])
  line([(-5,7),(0,8),(5,7)],LEATHER[0]);d.point((cx-4,top+7),fill=BRASS[1])
 elif name=='leHl':
  # Padded travelling cap: cloth-like panels and an offset leather fastening.
  line([(-3,2),(-2,4),(0,6)],LEATHER[1])
  for x,y in [(-3,3),(-1,5),(2,6)]:d.point((cx+x,top+y),fill=LEATHER[3])
  if facing!=1:
   side=-1 if facing==3 else 1
   line([(side*5,7),(side*5,10),(side*3,12)],LEATHER[0])
 if name=='mgHt' and facing==0:
  # Reverse only the existing gold arc; retain the approved hat alpha and other views.
  gold={(x,y):out.getpixel((x,y)) for y in range(64) for x in range(64) if out.getpixel((x,y)) in BRASS}
  if gold:
   low=min(y for x,y in gold);high=max(y for x,y in gold)
   for x,y in gold:out.putpixel((x,y),BLUE[1])
   for (x,y),color in gold.items():
    yy=low+high-y
    if out.getpixel((x,yy))[3]:out.putpixel((x,yy),color)
 return out

# Hand-placed native pixels. Every character below is one production pixel;
# blanks expose the underlying face. Front, back and profile have separate forms.
# Crown and light-cluster steps follow the original pfht's 14px head scale.
FORMS={
 'irHl':[
  ['....sssss....','..ssmmhmmss..','.smhhhhhhmms.','.smhmmmmmhms.','smmmmmhmmmmms','smmmmmhmmmmms','ssmmmmhmmmmss','.sssssssssss.','.sm..sss..ms.','.sm...s...ms.','.sm.......ms.','..s.......s..'],
  ['....sssss....','..ssmmhmmss..','.smhhhhmmmms.','.smhhmmmmmmms','smmmhhmmmmmms','smmmmmmmmmmms','ssmmmmmmmmmss','.ssmmmmmmmss.','..smmmmmmms..','..ssmmmmmss..','...sssssss...'],
  ['....sssss....','..ssmmhmmss..','.smhhhmmmmss.','.smhhmmmmmms.','smmhmmmmmmms.','smmmmmmmmmms.','ssmmmmmmmsss.','.ssmmmssss...','.smmms.......','.smhms.......','..smms.......','..sss........']
 ],
 'wdHl':[
  ['....sssss....','..ssmmmmmss..','.smhmmsmmhms.','.smhmmsmmhms.','smhmmmsmmmhms','smmmmmsmmmmms','ssmmmmmmmmmss','smmmsssssmmms','sm....s....ms','sm....m....ms','ssm...s...mss','.sm.......ms.','.smm.....mms.','..smmmmmmms..','...sssssss...'],
  ['....sssss....','..ssmmmmmss..','.smhmmsmmmms.','.smhmmsmmhmss','smmmmmsmmmhms','smmmmmsmmmmms','ssmmmmmmmmmss','.smmmsmmmms..','.smhmsmhmmss.','.smmmsmmmmms.','..smmsmmmms..','..smmsmmmms..','..smmsmmmms..','...smsssms...','...sssssss...'],
  ['....sssss....','..ssmmmmmss..','.smhmsmmmmss.','.smhmsmmhmms.','smmmmsmmmmms.','smmmmsmmmmms.','ssmmmmmmmmss.','.smmmmmmmms..','.smms........','.smhms.......','.smmms.......','..smms.......','..smmssssss..','...smmmmmms..','....ssssss...']
 ],
 'leHl':[
  ['....sssss....','..ssmmmmmss..','.smmhhhmmmms.','.smmhhmmmmmss','smmmmmmmmmmms','smmmmmmmmmmms','ssmmhmmmhmmss','.sssmmmmmsss.','.sm.......ms.','.sm.......ms.','.sh.......hs.','.sm.......ms.','..ss.....ss..'],
  ['....sssss....','..ssmmmmmss..','.smmhhhmmmms.','.smmhhmmmmmss','smmmmsmmmmmms','smmmmsmmmmmms','ssmmmmmmmmmss','.smhmmmhmmms.','.smmmsmmmmms.','..smmsmmmms..','..ssmmmmmss..','...sssssss...'],
  ['....sssss....','..ssmmmmmss..','.smmhhhmmmss.','.smmhhmmmmms.','smmmmsmmmmms.','smmmmsmmmmms.','ssmmmmmmmmss.','.smmhhmmsss..','.smmmmmms....','.smmhmms.....','.smmmms......','..smms.......','...ss........']
 ],
 'hood':[
  ['.....sss.....','...ssmmmss...','..smhhmmmms..','.smhhmmmmmms.','.smmmmmmmmms.','smmms...smmms','smms.....smms','sms.......sms','sms.......sms','sms.......sms','.sms.....sms.','.sms.....sms.','..sms...sms..','...ss...ss...'],
  ['.....sss.....','...ssmmmss...','..smhhmmmms..','.smhhmmmmmms.','.smmmmmmmmms.','smmmmmmmmmmms','smmmmmmmmmmms','smmmmmmmmmms.','.smmmmsmmmms.','.smmmsmmmms..','..smsmmmms...','..ssmmmms....','...smmms.....','....sss......'],
  ['.....sss.....','...ssmmmss...','..smhhmmmms..','.smhhmmmmmms.','.smmmmmmmsss.','smmmmmmms....','smmmmmmmss...','smmmmmmms....','.smmmmms.....','.smmmms......','..smmms......','..smms.......','...smss......','....ssss.....']
 ]}

def tailored_form(name,cap,facing):
 box=cap.getbbox();cx=round((box[0]+box[2]-1)/2);top=box[1]-1
 palette={'irHl':{'s':STEEL[0],'m':STEEL[1],'h':STEEL[2]},
          'wdHl':{'s':WOOD[0],'m':WOOD[1],'h':WOOD[2]},
          'leHl':{'s':LEATHER[0],'m':LEATHER[1],'h':LEATHER[2]},
          'hood':{'s':GREEN[0],'m':GREEN[1],'h':GREEN[2]}}[name]
 out=Image.new('RGBA',(64,64));rows=FORMS[name][min(facing,2)]
 for y,row in enumerate(rows):
  assert len(row)==13,(name,facing,y,len(row))
  for x,key in enumerate(row):
   if key!='.':out.putpixel((cx+(6-x if facing==3 else x-6),top+y),palette[key])
 # Keep the deepest recesses dark, not a heavy enclosing black outline.
 if name=='hood':
  for x,y in ([(0,7),(0,8)] if facing==1 else [(-5,8),(-5,9)] if facing==0 else [(-4,9),(-4,10)]):
   xx=cx+(-x if facing==3 else x)
   if out.getpixel((xx,top+y))[3]:out.putpixel((xx,top+y),O)
 if name=='irHl' and facing==0:
  for y in range(64):
   for off in range(1,7):out.putpixel((cx+off,y),out.getpixel((cx-off,y)))
 return out
