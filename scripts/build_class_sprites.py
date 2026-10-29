"""Native-grid equipment and articulated bow/casting attacks for two classes.
Design reference: art/source/ranged-classes-design.png. No rescaled AI pixels:
all production sprites are drawn at original 64px frame scale with native colors.
"""
from pathlib import Path
from PIL import Image,ImageDraw
from collections import Counter
import json,sys
from native_headgear import headgear
ROOT=Path(__file__).resolve().parents[1];A=ROOT/'app/src/main/assets';C=A/'sprites/ManaSeedCharacter'
GREEN=[(24,24,24),(54,74,63),(91,127,106),(168,200,152)]
CLOTHBLUE=[(24,24,24),(25,51,100),(58,103,151),(89,153,189)]
TRIM=[(129,92,33),(178,145,73),(224,210,142)]
BLUE=[(26,28,40),(30,43,65),(60,85,117),(105,130,166)]
WOOD=[(85,45,36),(113,101,88),(190,134,95),(225,178,110)]
GOLD=(243,161,74);ICE=(148,193,223);LIGHT=(199,196,162)

def path(p,l,n,v=1):return C/f'char_a_{p}'/('' if l=='0bas' else l)/f'char_a_{p}_{l}_{n}_v{v:02d}.png'
def read(p,l,n,v=1):return Image.open(path(p,l,n,v)).convert('RGBA')
def cell(im,r,c):return im.crop((c*64,r*64,c*64+64,r*64+64))
def save(im,p,l,n,v=1):
 q=path(p,l,n,v);q.parent.mkdir(parents=True,exist_ok=True);im.save(q)
def opaque(c):return (*c,255)
def head(p,r,c):
 im=cell(read('pONE2' if p in ('pBOW','pCAST') else p,'4har','bob1'),r%4 if p in ('pBOW','pCAST') else r,0 if p in ('pBOW','pCAST') else c)
 if im.getbbox() is None:im=cell(read('pONE2','4har','bob1'),r%4,0)
 return im

def arm_pose(original,cap,facing,col,kind):
 out=original.copy();x0,y0,x1,y1=cap.getbbox();cx=round((x0+x1-1)/2);shoulder=y0+14
 # The native head/legs remain untouched. Replace only the old side arms.
 for y in range(shoulder-1,min(63,shoulder+12)):
  for x in range(64):
   if abs(x-cx)>4:out.putpixel((x,y),(0,0,0,0))
 colors=Counter(p[:3] for p in original.crop((x0+3,y0+5,x1-3,y0+12)).get_flattened_data() if p[3])
 skin=sorted(colors,key=lambda p:sum(p));mid=skin[len(skin)//2];light=skin[-1];outline=skin[0]
 d=ImageDraw.Draw(out)
 sign=-1 if facing in (1,3) else 1
 grip=(cx+sign*9,shoulder+4)
 pull=(cx+sign*([5,2,6,4][col%4]),shoulder+(3 if facing in (2,3) else 5))
 if kind=='pCAST':grip=(cx+sign*8,shoulder+3);pull=(cx-sign*5,shoulder+([6,0,-2,4][col%4]))
 for start,end in [((cx+sign*4,shoulder),grip),((cx-sign*4,shoulder+1),pull)]:
  d.line([start,end],fill=opaque(outline),width=4);d.line([start,end],fill=opaque(mid),width=2);d.point(end,fill=opaque(light))
 return out

# Dedicated four-frame draw/release and channel/release poses, all 11 skin tones.
for page in (() if '--equipment-only' in sys.argv else ('pBOW','pCAST')):
 for variant in range(11):
  base=read('pONE2','0bas','humn',variant);sheet=Image.new('RGBA',(512,512))
  for r in range(8):
   for c in range(8):sheet.alpha_composite(arm_pose(cell(base,r%4,0),head(page,r,c),r%4,c%4,page),(c*64,r*64))
  save(sheet,page,'0bas','humn',variant)

grips={}
for page in ('p1','pONE2','pONE3','pBOW','pCAST'):
 grips[page]=[]
 ranged=page in ('pBOW','pCAST');base=read(page,'0bas','humn');native=read('pONE2' if ranged else page,'1out','fstr')
 sheets={name:Image.new('RGBA',(512,512)) for name in ('rngr','robe','hood','mgHt','bow1','staf','quvr')}
 for row in range(8):
  facing=row%4
  for col in range(8):
   cap=head(page,row,col);x0,y0,x1,y1=cap.getbbox();cx=round((x0+x1-1)/2)
   b=cell(base,row,col);bbox=b.getbbox()
   if bbox is None:b=cell(read('pONE2','0bas','humn'),facing,0);bbox=b.getbbox()
   foot=bbox[3];cloth=cell(native,facing if ranged else row,0 if ranged else col)
   if cloth.getbbox() is None:cloth=cell(read('pONE2','1out','fstr'),facing,0)
   poses={name:Image.new('RGBA',(64,64)) for name in sheets}
   # Preserve native material identities and original outline pixels exactly.
   source=[(24,24,24),(113,101,88),(166,152,120),(199,196,162),(91,24,57),(141,34,70),(190,86,103),(30,43,65),(60,85,117)]
   archer=[GREEN[0],(88,56,40),(152,96,56),(240,152,72),GREEN[1],GREEN[2],GREEN[3],GREEN[1],GREEN[2]]
   mage=[CLOTHBLUE[0],CLOTHBLUE[1],CLOTHBLUE[2],CLOTHBLUE[3],TRIM[0],TRIM[1],TRIM[2],CLOTHBLUE[1],CLOTHBLUE[2]]
   for name,material in [('rngr',archer),('robe',mage)]:
    q=poses[name];mapping=dict(zip(source,material))
    for yy in range(64):
     for xx in range(64):
      px=cloth.getpixel((xx,yy))
      if not px[3]:continue
      if ranged and abs(xx-cx)>4 and yy<y0+26:continue
      q.putpixel((xx,yy),opaque(mapping[px[:3]]))
    if name=='robe':
     # Direction-specific draped skirt; compact native outlines, no gold zigzag.
     d=ImageDraw.Draw(q);waist=min(foot-7,y0+23);hem=foot-3
     sway=[0,1,0,-1,0,1,0,-1][col] if row>=4 or ranged else 0
     half=5 if facing<2 else 3
     pts=[(cx-half+1,waist-1),(cx+half-1,waist-1),(cx+half+sway,hem-1),(cx+half-1+sway,hem),(cx-half+1+sway,hem),(cx-half+sway,hem-1)]
     d.polygon(pts,fill=opaque(CLOTHBLUE[1]));d.line(pts[1:]+[pts[0]],fill=opaque(CLOTHBLUE[0]))
     d.line([(cx-half+1,waist+1),(cx-half+1+sway,hem-2)],fill=opaque(CLOTHBLUE[2]))
     if facing<2:d.line([(cx-1,waist+1),(cx-1+sway,hem-2)],fill=opaque(CLOTHBLUE[2]))
     d.line((cx-half+1,waist-1,cx+half-1,waist-1),fill=opaque(TRIM[0]))
     if facing!=1:d.point((cx,waist-1),fill=opaque(TRIM[2]))
   poses['hood']=headgear('hood',cap,facing)
   poses['mgHt']=headgear('mgHt',cap,facing)
   # Short shoulder-mounted quiver sits behind the torso, away from the bow.
   q=poses['quvr'];d=ImageDraw.Draw(q)
   qx=cx+(-4 if facing==2 else 4 if facing==3 else 3);qy=y0+14
   d.polygon([(qx-2,qy),(qx+2,qy-1),(qx+1,qy+8),(qx-2,qy+9)],fill=opaque(WOOD[0]))
   d.line((qx-1,qy+1,qx-1,qy+7),fill=opaque(WOOD[2]))
   d.line((qx-2,qy,qx+2,qy-1),fill=opaque(WOOD[3]))
   for off in (-1,1):
    d.line((qx+off,qy,qx+off+1,qy-4),fill=opaque(WOOD[2]))
    d.line((qx+off,qy-4,qx+off+1,qy-3),fill=opaque(LIGHT))
   # Find the actual hand in each native pose; no shared arbitrary sprite offset.
   sign=-1 if facing in (1,3) else 1
   tx=cx+sign*(9 if ranged else 7);ty=y0+(18 if ranged else 19)
   if not ranged:
    skincolors={p[:3] for p in b.crop((x0+3,y0+5,x1-3,y0+12)).get_flattened_data() if p[3] and sum(p[:3])>140}
    hands=[(xx,yy) for yy in range(y0+13,min(foot-4,y0+25)) for xx in range(max(0,cx-13),min(64,cx+14)) if b.getpixel((xx,yy))[3] and b.getpixel((xx,yy))[:3] in skincolors]
    if hands:tx,ty=min(hands,key=lambda p:(p[0]-tx)**2+(p[1]-ty)**2)
   grips[page].append([tx,ty])
   q=poses['bow1'];d=ImageDraw.Draw(q)
   if facing in (2,3):
    points=[(tx-sign*4,ty-8),(tx-sign,ty-5),(tx,ty),(tx-sign,ty+5),(tx-sign*4,ty+8)]
    pull=tx-sign*(7 if ranged and col%4==1 else 4)
    d.line(points,fill=opaque(WOOD[0]),width=3);d.line(points,fill=opaque(WOOD[2]),width=1)
    d.line([(tx-sign*4,ty-8),(pull,ty),(tx-sign*4,ty+8)],fill=opaque(LIGHT))
    if ranged and col%4 in (0,1):
     d.line((pull,ty,tx+sign*7,ty),fill=opaque(WOOD[3]));d.point((tx+sign*8,ty),fill=opaque(ICE))
   else:
    points=[(tx-sign*4,ty-8),(tx-sign,ty-4),(tx,ty),(tx-sign,ty+4),(tx-sign*4,ty+8)]
    d.line(points,fill=opaque(WOOD[0]),width=2);d.line(points,fill=opaque(WOOD[2]))
    pull=tx-sign*(7 if ranged and col%4==1 else 4)
    d.line([(tx-sign*4,ty-8),(pull,ty),(tx-sign*4,ty+8)],fill=opaque(LIGHT))
   q=poses['staf'];d=ImageDraw.Draw(q);sx=tx;sy=ty-14;crystalx=sx+sign*4
   d.line([(sx,ty+8),(sx,sy+6),(crystalx,sy+2)],fill=opaque(WOOD[0]),width=3)
   d.line([(sx-1,ty+7),(sx-1,sy+6),(crystalx-1,sy+2)],fill=opaque(WOOD[2]))
   d.polygon([(crystalx,sy-2),(crystalx+2,sy),(crystalx+1,sy+3),(crystalx-1,sy+3),(crystalx-2,sy)],fill=opaque(BLUE[2]))
   d.line((crystalx-1,sy,crystalx,sy+2),fill=opaque(ICE))
   d.point((crystalx,sy-1),fill=opaque(LIGHT))
   if ranged and col%4 in (1,2):
    d.point((crystalx-sign*3,sy-3),fill=opaque(ICE));d.point((crystalx+sign*3,sy+2),fill=opaque(ICE))
   for name,im in poses.items():sheets[name].alpha_composite(im,(col*64,row*64))
 for name,im in sheets.items():save(im,page,'1out' if name in ('rngr','robe') else '5hat' if name in ('hood','mgHt') else '6tla',name)
# Native-scale, transparent inventory icons rendered from actual production layers.
icons=A/'class_items';icons.mkdir(exist_ok=True)
(icons/'grips.json').write_text(json.dumps(grips,separators=(',',':')))
for item,page,layer,name,row in [('Jagdbogen','p1','6tla','bow1',2),('Magierstab','p1','6tla','staf',0),('Magierrobe','p1','1out','robe',0),('Magierhut','p1','5hat','mgHt',0),('Waldlaeuferwams','p1','1out','rngr',0),('Waldlaeuferkapuze','p1','5hat','hood',0)]:
 im=cell(read(page,layer,name),row,0);im=im.crop(im.getbbox());icon=Image.new('RGBA',(32,32));icon.alpha_composite(im,((32-im.width)//2,(32-im.height)//2));icon.save(icons/(item+'.png'))
(ROOT/'art/source/class-palette.json').write_text(json.dumps(sorted(set(GREEN+CLOTHBLUE+TRIM+BLUE+WOOD+[(88,56,40),(152,96,56),(240,152,72),GOLD,ICE,LIGHT]))))
print('Two native class outfits, hats, bow, staff and quiver; five animation pages, 11 skin tones, icons.')
