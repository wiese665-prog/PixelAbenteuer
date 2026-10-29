"""Native-grid equipment and articulated bow/casting attacks for two classes.
Design reference: art/source/ranged-classes-design.png. No rescaled AI pixels:
all production sprites are drawn at original 64px frame scale with native colors.
"""
from pathlib import Path
from PIL import Image,ImageDraw
from collections import Counter
import json
ROOT=Path(__file__).resolve().parents[1];A=ROOT/'app/src/main/assets';C=A/'sprites/ManaSeedCharacter'
GREEN=[(32,71,44),(78,130,73),(86,140,77),(135,183,88)]
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
for page in ('pBOW','pCAST'):
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
   # Rebuild materials on the exact moving outfit mask, preserving silhouettes.
   for name,ramp in [('rngr',GREEN),('robe',BLUE)]:
    q=poses[name]
    for yy in range(64):
     for xx in range(64):
      p=cloth.getpixel((xx,yy))
      if not p[3]:continue
      if ranged and abs(xx-cx)>4 and yy<y0+26:continue
      lum=sum(p[:3])/3;idx=0 if lum<45 else 1 if lum<110 else 2 if lum<160 else 3
      # Pants and boots retain native leather for the archer.
      rgb=WOOD[min(3,idx)] if name=='rngr' and yy>y0+24 else ramp[idx]
      q.putpixel((xx,yy),opaque(rgb))
    d=ImageDraw.Draw(q);waist=min(foot-7,y0+23);hem=foot-3
    if name=='robe':
     sway=[0,1,0,-1,0,1,0,-1][col] if row>=4 or ranged else 0
     d.polygon([(cx-4,waist-2),(cx+4,waist-2),(cx+6+sway,hem),(cx+1,hem),(cx,hem-5),(cx-2,hem),(cx-6+sway,hem)],fill=opaque(BLUE[1]))
     d.line([(cx-3,waist),(cx-4+sway,hem-1)],fill=opaque(BLUE[2]));d.line([(cx+3,waist),(cx+4+sway,hem-1)],fill=opaque(BLUE[0]))
     if facing!=1:d.line([(cx,waist),(cx+1,hem-5)],fill=opaque(GOLD))
     if facing==1:
      d.polygon([(cx-4,waist),(cx+4,waist),(cx+5+sway,hem-1),(cx+2,hem),(cx-2,hem),(cx-5+sway,hem-1)],fill=opaque(BLUE[1]))
      d.line([(cx-4+sway,hem-1),(cx-2,hem),(cx+2,hem),(cx+4+sway,hem-1)],fill=opaque(GOLD))
     else:d.line([(cx-5+sway,hem-1),(cx-2,hem),(cx,hem-4),(cx+2,hem),(cx+5+sway,hem-1)],fill=opaque(GOLD))
    else:
     d.line((cx-4,waist-3,cx+4,waist-3),fill=opaque(WOOD[0]),width=2)
    if name=='robe':
     d.line([(cx-3,y0+14),(cx-1,y0+18),(cx,waist-4)],fill=opaque(BLUE[2]))
     d.line([(cx+3,y0+14),(cx+1,y0+18)],fill=opaque(BLUE[0]))
     d.line([(cx-3,waist+2),(cx-2,hem-3)],fill=opaque(BLUE[2]))
     d.line([(cx+2,waist+2),(cx+3,hem-2)],fill=opaque(BLUE[0]))
     # Short broken gold stitching avoids a rigid rectangular outline.

    else:
     d.line([(cx-3,y0+14),(cx-2,y0+18),(cx-3,waist-5)],fill=opaque(GREEN[0]))
     d.line([(cx+1,y0+15),(cx+3,y0+19)],fill=opaque(GREEN[3]))
    d.rectangle((cx-1,waist-3,cx+1,waist-2),fill=opaque(GOLD))
   # A sewn cloth hood: broad fabric folds, face cutout and neck closure.
   # Its silhouette is independent of every hairstyle beneath it.
   q=poses['hood'];d=ImageDraw.Draw(q);rx=max(8,(x1-x0+1)//2+1)
   d.polygon([(cx-rx,y0+5),(cx-rx+2,y0),(cx-1,y0-2),(cx+rx-3,y0),(cx+rx,y0+5),(cx+rx-1,y0+12),(cx+3,y0+16),(cx-3,y0+16),(cx-rx+1,y0+12)],fill=opaque(GREEN[0]))
   d.polygon([(cx-rx+2,y0+5),(cx-4,y0+1),(cx+2,y0),(cx+rx-2,y0+5),(cx+rx-2,y0+10),(cx+2,y0+14),(cx-3,y0+14),(cx-rx+2,y0+10)],fill=opaque(GREEN[1]))
   d.polygon([(cx-5,y0+3),(cx-1,y0+1),(cx+2,y0+3),(cx-3,y0+5)],fill=opaque(GREEN[2]))
   if facing==0:
    opening=[(cx-4,y0+7),(cx+3,y0+7),(cx+4,y0+10),(cx+2,y0+13),(cx-2,y0+13),(cx-4,y0+10)]
    d.polygon(opening,fill=(0,0,0,0));d.line(opening+[opening[0]],fill=opaque(GREEN[0]))
   elif facing in (2,3):
    signface=1 if facing==2 else -1
    opening=[(cx+signface,y0+7),(cx+signface*(rx-1),y0+6),(cx+signface*rx,y0+12),(cx+signface*3,y0+14),(cx+signface,y0+11)]
    d.polygon(opening,fill=(0,0,0,0));d.line([opening[-1],opening[0],opening[1]],fill=opaque(GREEN[0]))
   else:
    d.line([(cx-4,y0+7),(cx,y0+12),(cx+4,y0+7)],fill=opaque(GREEN[0]))
    d.line([(cx-3,y0+6),(cx,y0+9)],fill=opaque(GREEN[2]))
   d.polygon([(cx-3,y0+14),(cx,y0+16),(cx+3,y0+14),(cx+2,y0+17),(cx-2,y0+17)],fill=opaque(GREEN[0]))
   d.point((cx,y0+16),fill=opaque(GREEN[2]))
   # Compact bent crown, clustered cloth highlights and a foreshortened brim.
   q=poses['mgHt'];d=ImageDraw.Draw(q);mirror=-1 if facing in (1,3) else 1
   def hp(points):return [(cx+mirror*x,y0+y) for x,y in points]
   d.polygon(hp([(-6,5),(-4,0),(-3,-4),(-6,-5),(-5,-7),(-1,-6),(2,-4),(4,0),(6,5)]),fill=opaque(BLUE[0]))
   d.polygon(hp([(-4,4),(-2,0),(-2,-4),(0,-4),(2,-1),(3,4)]),fill=opaque(BLUE[1]))
   d.polygon(hp([(-3,3),(-2,-2),(0,-3),(1,-1),(0,2)]),fill=opaque(BLUE[2]))
   d.line(hp([(-4,4),(0,5),(4,4)]),fill=opaque(GOLD))
   brim=7 if facing in (2,3) else 9
   d.polygon([(cx-brim,y0+6),(cx-3,y0+5),(cx+brim-1,y0+6),(cx+brim,y0+7),(cx+3,y0+9),(cx-4,y0+8),(cx-brim,y0+7)],fill=opaque(BLUE[0]))
   d.line([(cx-brim+2,y0+6),(cx-2,y0+6),(cx+brim-2,y0+7)],fill=opaque(BLUE[2]))
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
(ROOT/'art/source/class-palette.json').write_text(json.dumps(sorted(set(GREEN+BLUE+WOOD+[GOLD,ICE,LIGHT]))))
print('Two native class outfits, hats, bow, staff and quiver; five animation pages, 11 skin tones, icons.')
