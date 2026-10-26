import pathlib,struct,zlib
from PIL import Image
root=pathlib.Path("app/src/main/assets/sprites")
out=pathlib.Path("app/src/main/assets/generated");out.mkdir(parents=True,exist_ok=True)
def u16(b,o):return struct.unpack_from("<H",b,o)[0]
def u32(b,o):return struct.unpack_from("<I",b,o)[0]
def convert(path):
 b=path.read_bytes();w,h=u16(b,8),u16(b,10);depth=u16(b,12)
 if depth!=32:print("Skipping non-RGBA",path);return
 count=u16(b,6);pos=128;frames=[];layers=[]
 for f in range(count):
  size=u32(b,pos);chunks=u16(b,pos+6);p=pos+16;frame=Image.new("RGBA",(w,h))
  for _ in range(chunks):
   length=u32(b,p);kind=u16(b,p+4);q=p+6
   if kind==0x2004:
    flags=u16(b,q);layers.append(bool(flags&1))
   elif kind==0x2005:
    layer=u16(b,q);x,y=struct.unpack_from("<hh",b,q+2);opacity=b[q+6];typ=u16(b,q+7)
    if typ in (0,2):
     cw,ch=u16(b,q+16),u16(b,q+18);raw=b[q+20:p+length]
     if typ==2:raw=zlib.decompress(raw)
     if len(raw)==cw*ch*4 and (layer>=len(layers) or layers[layer]):
      cel=Image.frombytes("RGBA",(cw,ch),raw)
      if opacity<255:cel.putalpha(cel.getchannel("A").point(lambda a:a*opacity//255))
      frame.alpha_composite(cel,(x,y))
   p+=length
  frames.append(frame);pos+=size
 if not frames:return
 sheet=Image.new("RGBA",(w*len(frames),h))
 for i,im in enumerate(frames):sheet.paste(im,(i*w,0))
 name=path.stem.lower()
 parts=path.parts
 animal=next((v for v in ("boar","deer","fox","black_grouse","orc1","orc2","orc3") if v in name),None)
 if name.startswith("sword_"):animal="player_sword"
 if name.startswith("unarmed_"):animal="player_unarmed"
 if not animal:return
 action=next((a for a in ("run_attack","walk_attack","idle","walk","run","flight","attack","hurt","death") if a in name),None)
 direction=next((d for d in ("front","back","left","right") if d in name),None)
 if action and direction:
  sheet.save(out/(animal+"_"+action+"_"+direction+".png"))
  print(animal,action,direction,len(frames))
for path in sorted(root.rglob("*.aseprite")):
 try:convert(path)
 except Exception as e:print("Could not convert",path,e)
