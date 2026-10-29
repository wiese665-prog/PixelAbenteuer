"""New native-grid headgear silhouettes. Coordinates follow each original head frame."""
from PIL import Image,ImageDraw
O=(24,24,24,255)
GREEN=[(54,74,63,255),(91,127,106,255),(168,200,152,255)]
BLUE=[(25,51,100,255),(58,103,151,255),(89,153,189,255)]
STEEL=[(82,82,95,255),(124,137,137,255),(167,181,167,255),(220,220,203,255)]
WOOD=[(88,56,40,255),(152,96,56,255),(240,152,72,255)]
LEATHER=[(61,18,0,255),(99,46,17,255),(141,99,64,255),(218,185,136,255)]
BRASS=[(129,92,33,255),(178,145,73,255),(224,210,142,255)]

def headgear(name,cap,facing):
 box=cap.getbbox();cx=round((box[0]+box[2]-1)/2);top=box[1]
 im=Image.new('RGBA',(64,64));d=ImageDraw.Draw(im)
 # Work in a local head basis; profiles are drawn separately then mirrored.
 def poly(pts,color):d.polygon([(cx+x,top+y) for x,y in pts],fill=color)
 def line(pts,color):d.line([(cx+x,top+y) for x,y in pts],fill=color)
 def rect(x0,y0,x1,y1,color):d.rectangle((cx+x0,top+y0,cx+x1,top+y1),fill=color)
 profile=facing in (2,3)
 if name=='hood':
  if profile:
   poly([(-7,3),(-5,0),(-1,-1),(3,1),(6,4),(5,6),(2,7),(2,11),(4,13),(1,15),(-3,14),(-6,11),(-7,7)],O)
   poly([(-6,4),(-4,1),(-1,0),(2,2),(4,4),(4,5),(1,6),(1,11),(2,13),(0,14),(-3,13),(-5,10),(-6,7)],GREEN[0])
   poly([(-5,4),(-3,1),(-1,1),(1,3),(0,5),(-3,5)],GREEN[1])
   line([(-4,9),(-3,11),(-1,12)],GREEN[1]);line([(0,5),(2,5),(3,4)],GREEN[2])
  elif facing==1:
   poly([(-7,4),(-5,0),(-1,-1),(4,1),(7,5),(6,11),(3,15),(-3,15),(-6,12)],O)
   poly([(-6,4),(-4,1),(-1,0),(3,2),(6,5),(5,10),(2,14),(-2,14),(-5,11)],GREEN[0])
   poly([(-4,3),(-2,1),(1,2),(2,4),(-1,6),(-4,5)],GREEN[1]);line([(-3,8),(-1,11),(1,12)],GREEN[1])
  else:
   poly([(-7,4),(-5,1),(-1,-1),(3,0),(6,3),(7,7),(6,12),(3,15),(-3,15),(-6,12),(-7,8)],O)
   poly([(-6,4),(-4,2),(-1,0),(2,1),(5,4),(6,7),(5,11),(2,14),(-2,14),(-5,11),(-6,8)],GREEN[0])
   poly([(-4,3),(-1,1),(1,2),(2,4),(-2,4)],GREEN[1])
   # Wide face opening follows the original brow/cheeks, without a circular ring.
   poly([(-4,6),(3,6),(4,8),(3,12),(1,13),(-2,12),(-4,10)],(0,0,0,0))
   line([(-5,6),(-3,5),(2,5),(4,6)],GREEN[1]);line([(-4,5),(-2,4),(0,4)],GREEN[2])
   line([(-4,12),(-2,13),(1,14),(3,12)],GREEN[1])
 elif name=='mgHt':
  # Tapered cloth cone; short folded tip, no horizontal black antenna.
  poly([(-6,4),(-4,0),(-3,-4),(-1,-7),(2,-8),(3,-7),(1,-5),(3,-2),(5,3),(6,5)],O)
  poly([(-4,3),(-3,0),(-2,-4),(0,-6),(1,-6),(0,-4),(2,-1),(4,3)],BLUE[0])
  poly([(-3,2),(-2,-1),(-1,-4),(0,-4),(1,-1),(1,2)],BLUE[1])
  line([(-2,-1),(-1,-3)],BLUE[2]);line([(-4,4),(3,4)],BRASS[0]);line([(-3,4),(1,4)],BRASS[1])
  width=7 if profile else 9
  poly([(-width,6),(-width+2,5),(-4,5),(4,5),(width-1,6),(width,7),(4,8),(-3,8),(-width,7)],O)
  line([(-width+2,6),(-3,6),(3,6),(width-2,7)],BLUE[1]);line([(-4,6),(-1,6)],BLUE[2])
 elif name=='irHl':
  # Ridged steel bowl, projecting brow and separate cheek plates.
  poly([(-7,4),(-5,0),(-2,-2),(2,-2),(5,0),(7,4),(7,7),(-7,7)],O)
  poly([(-6,4),(-4,1),(-1,-1),(1,-1),(4,1),(6,4),(6,6),(-6,6)],STEEL[0])
  poly([(-5,4),(-3,1),(-1,0),(0,4),(-2,5),(-5,5)],STEEL[2]);line([(-3,2),(-2,0)],STEEL[3])
  line([(1,0),(2,5)],STEEL[1]);line([(-7,6),(7,6)],STEEL[1]);line([(-5,6),(0,6)],STEEL[3])
  if facing==1:
   poly([(-6,8),(6,8),(5,13),(3,15),(-3,15),(-5,13)],O)
   poly([(-5,8),(5,8),(4,12),(2,14),(-2,14),(-4,12)],STEEL[0]);line([(-4,9),(3,9)],STEEL[1])
  elif profile:
   poly([(-6,7),(-2,8),(-2,12),(-4,14),(-6,12)],O)
   poly([(-5,8),(-3,9),(-3,11),(-4,12),(-5,11)],STEEL[1]);line([(-5,8),(-4,9)],STEEL[2])
   # Short forward brim gives a markedly different silhouette from the wood cap.
   line([(5,6),(8,6),(8,7),(6,7)],STEEL[0])
  else:
   for sign in (-1,1):
    poly([(sign*6,8),(sign*4,8),(sign*4,11),(sign*5,13),(sign*7,12)],O)
    line([(sign*5,8),(sign*5,11)],STEEL[1])
   line([(0,7),(0,10)],STEEL[0]);d.point((cx,top+7),fill=STEEL[2])
 elif name=='wdHl':
  # Flat plank crown, visible vertical grain, leather brow strap and open face.
  poly([(-7,1),(-5,-1),(5,-1),(7,1),(7,7),(-7,7)],O)
  rect(-6,1,6,5,WOOD[0]);rect(-5,0,5,1,WOOD[1])
  for x in (-4,0,4):
   line([(x,1),(x,5)],WOOD[1]);line([(x+1,2),(x+1,4)],WOOD[2])
  line([(-6,6),(6,6)],LEATHER[0]);d.point((cx-4,top+6),fill=BRASS[1]);d.point((cx+4,top+6),fill=BRASS[1])
  if facing==1:
   rect(-6,8,6,12,WOOD[0]);line([(-6,13),(6,13)],O)
   for x in (-4,0,4):line([(x,8),(x,11)],WOOD[1])
  elif profile:
   poly([(-7,7),(-4,7),(-4,11),(-5,13),(-7,12)],O);line([(-6,8),(-6,11)],LEATHER[2])
  else:
   line([(-6,8),(-6,11)],LEATHER[0]);line([(6,8),(6,11)],LEATHER[0])
 elif name=='leHl':
  # Short leather coif with open ears: distinct from the new wood/steel crowns.
  poly([(-6,3),(-4,0),(2,0),(5,2),(6,5),(5,7),(-6,7)],O)
  poly([(-5,3),(-3,1),(1,1),(4,3),(5,5),(4,6),(-5,6)],LEATHER[1])
  poly([(-4,3),(-2,1),(0,2),(1,4),(-3,4)],LEATHER[2]);line([(-2,2),(0,2)],LEATHER[3])
  line([(-5,6),(5,6)],LEATHER[0])
  if facing==1:
   rect(-5,8,4,12,LEATHER[0]);line([(-4,8),(-3,10),(2,11)],LEATHER[1])
  elif profile:line([(-5,7),(-5,11),(-3,12)],LEATHER[0])
 if facing==3:
  # Mirror about the exact source head center, not the 64px cell center.
  mirrored=Image.new('RGBA',(64,64))
  for y in range(64):
   for x in range(64):
    xx=2*cx-x
    if 0<=xx<64:mirrored.putpixel((xx,y),im.getpixel((x,y)))
  im=mirrored
 return im
