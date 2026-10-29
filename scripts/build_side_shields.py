"""Oblique native-color left shields with the original pose attachment center."""
from pathlib import Path
from PIL import Image
ROOT=Path(__file__).resolve().parents[1];A=ROOT/'app/src/main/assets/sprites/ManaSeedCharacter'
for page in ('pONE2','pONE3'):
 for name,outname in [('sh01','shL1'),('sh03','shL3')]:
  source=Image.open(A/f'char_a_{page}/7tlb/char_a_{page}_7tlb_{name}_v01.png').convert('RGBA');out=source.copy()
  for row in (3,7):
   for col in range(8):
    frame=source.crop((col*64,row*64,col*64+64,row*64+64));bbox=frame.getbbox()
    if not bbox:continue
    x,y,r,b=bbox;profile=frame.crop(bbox).resize((max(4,round((r-x)*.55)),b-y),Image.Resampling.NEAREST)
    target=Image.new('RGBA',(64,64));left=round((x+r-profile.width)/2)
    target.alpha_composite(profile,(left,y));out.paste(target,(col*64,row*64))
  out.save(A/f'char_a_{page}/7tlb/char_a_{page}_7tlb_{outname}_v01.png')
print('Four fitted left-profile shield sheets generated without frontal-cell reuse.')
