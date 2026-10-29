"""Verify native colors, complete door/chimney frames, paving and sparse NPC rows."""
from pathlib import Path
from PIL import Image,ImageChops
import json
ROOT=Path(__file__).resolve().parents[1];assets=ROOT/'app/src/main/assets';v=assets/'village'
def rgba(p):return Image.open(p).convert('RGBA')
# The user explicitly approved this tower image. Verify faithful proportional
# nearest-neighbor preparation, rather than imposing the discarded native draft.
approved=rgba(ROOT/'art/source/watchtower-approved.png')
approved.putalpha(approved.getchannel('A').point(lambda a:255 if a>=128 else 0))
approved=approved.crop(approved.getbbox());height=204;width=round(approved.width*height/approved.height)
expected=Image.new('RGBA',(160,208));expected.alpha_composite(approved.resize((width,height),Image.Resampling.NEAREST),((160-width)//2,208-height))
tower=rgba(v/'tower.png');assert tower.tobytes()==expected.tobytes()
assert set(tower.getchannel('A').get_flattened_data())<={0,255}
assert tower.getbbox()[3]==208
# All six houses have actual opening states and a matching closed final pose.
for i in range(6):
 im=rgba(v/f'door{i}.png');assert im.height%6==0;h=im.height//6
 frames=[im.crop((0,f*h,im.width,(f+1)*h)) for f in range(6)]
 assert ImageChops.difference(frames[0],frames[5]).getbbox() is None,i
 assert frames[0].tobytes()!=frames[3].tobytes(),i
 assert all(frame.getbbox() is not None for frame in frames)
# The road material contains no transparent transition / object cells.
path=rgba(v/'path.png');assert path.size==(48,48)
assert path.getchannel('A').getextrema()==(255,255)
assert rgba(v/'grass_edges.png').size==(64,16)
plant=rgba(v/'grass_details.png');assert plant.size==(288,32)
assert all(plant.crop((i*32,0,(i+1)*32,32)).getbbox() for i in range(9))
# Retain the complete 18-pixel chimney cap/body from every original frame.
source=rgba(v/'Smoke_animation.png');chimney=rgba(v/'chimney_animation.png');assert chimney.size==(48,288)
for f in range(6):
 original=source.crop((f*48+14,32,f*48+32,46))
 result=chimney.crop((14,f*48+32,32,f*48+46))
 assert original.tobytes()==result.tobytes(),('chimney clipped',f)
# These sheets intentionally contain six empty rear-idle cells. Document that
# regression fixture and verify that every direction still has valid poses.
for who in ['Citizen1','Citizen2','Fighter2']:
 for action,cols in [('Idle',12),('Walk',6)]:
  im=rgba(v/'guild'/f'{who}_{action}_without_shadow.png');assert im.size==(cols*32,128)
  counts=[]
  for row in range(4):
   count=sum(im.crop((col*32,row*32,(col+1)*32,(row+1)*32)).getbbox() is not None for col in range(cols));counts.append(count)
  assert min(counts)>0
  if action=='Idle':assert counts[3]==6
  else:assert counts==[6,6,6,6]
print('Approved tower preparation, six animated doors, opaque paving, complete chimneys, 9 complete grass-detail variants and sparse NPC rows: passed.')
