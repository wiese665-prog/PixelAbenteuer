"""Prepare the exact user-approved tower for the existing native game canvas."""
from PIL import Image
from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]
source=Image.open(root/'art/source/watchtower-approved.png').convert('RGBA')
source.putalpha(source.getchannel('A').point(lambda a:255 if a>=128 else 0))
box=source.getbbox();sprite=source.crop(box);h=204;w=round(sprite.width*h/sprite.height)
sprite=sprite.resize((w,h),Image.Resampling.NEAREST)
out=Image.new('RGBA',(160,208));out.alpha_composite(sprite,((160-w)//2,208-h))
out.save(root/'art/source/watchtower-native.png');out.save(root/'app/src/main/assets/village/tower.png')
colors=sorted({p[:3] for p in out.get_flattened_data() if p[3]})
(root/'art/source/watchtower-palette.json').write_text(json.dumps(colors,separators=(',',':'))+'\n')
print('User-approved tower:',sprite.size,'inside 160x208 canvas; original aspect, nearest-neighbor, binary alpha.')
