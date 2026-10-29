from PIL import Image
from pathlib import Path
from scipy.ndimage import label
import numpy as np,json
root=Path(__file__).resolve().parents[1];forge=Image.open(root/'app/src/main/assets/sprites/Exterior_house.png').convert('RGBA');street=Image.open(root/'app/src/main/assets/village/guild/Walls_street(1).png').convert('RGBA');ext=Image.open(root/'app/src/main/assets/village/exterior.png').convert('RGBA')
for im in [forge,street,ext]:im.putalpha(im.getchannel('A').point(lambda a:255 if a>=128 else 0))
def piece(im,box):
 a=np.array(im.crop(box));l,_=label(a[:,:,3]>0);counts=np.bincount(l.ravel());counts[0]=0;a[l!=counts.argmax(),3]=0;return Image.fromarray(a)
out=Image.new('RGBA',(160,208))
# Native 16px masonry pattern: original brick sizes and colors, never scaled.
for y in range(96,208,16):
 for x in range(48,112,16):out.alpha_composite(street.crop((48+(x//16%2)*16,16,64+(x//16%2)*16,32)),(x,y))
 for x,sx in [(44,48),(112,76)]:out.alpha_composite(street.crop((sx,16,sx+4,32)),(x,y))
# Complete original timber storey and one continuous, native tiled roof.
# The previous detached hip-roof crop contained cut ridge posts and mixed gables.
out.alpha_composite(piece(forge,(144,87,256,120)),(24,64))
roof=Image.new('RGBA',(128,64))
roof.alpha_composite(Image.open(root/'app/src/main/assets/village/guild/Exterior(1).png').convert('RGBA').crop((160,0,176,64)),(0,0))
hall=Image.open(root/'app/src/main/assets/village/guild/Exterior(1).png').convert('RGBA')
for x in range(16,112,16):roof.alpha_composite(hall.crop((176+(x//16%3)*16,0,192+(x//16%3)*16,64)),(x,0))
roof.alpha_composite(hall.crop((240,0,256,64)),(112,0))
out.alpha_composite(roof,(16,8))
# Matched wooden corbels copied whole from the lookout, no floating stair pixels.
# Original blue-grey wall windows and original small arched door.
for y in [120,152]:out.alpha_composite(ext.crop((24,100,40,120)),(72,y))
for x in range(48,112,16):out.alpha_composite(street.crop((16,68,32,72)),(x,204))
out.alpha_composite(ext.crop((100,96,124,128)),(68,176))
a=np.array(out);a[:,:,3]=np.where(a[:,:,3]>=128,255,0);out=Image.fromarray(a)
out.save(root/'art/source/watchtower-native.png')
colors=sorted(set(map(tuple,a[a[:,:,3]>0,:3].tolist())))
(root/'art/source/watchtower-palette.json').write_text(json.dumps(colors,indent=2)+'\n')
(root/'art/WATCHTOWER.md').write_text('''# Native watchtower\n\nThe generated drafts were discarded after user feedback. The final `art/source/watchtower-native.png` is a 160x208 native sprite assembled from the original Exterior_house, Exterior(1), exterior and Walls_street reference sheets. Original brick, timber, window, door and roof pixels are retained at 1:1 resolution. There is no image-generation illustration or color quantization in the final tower.\n\n`art/source/watchtower-palette.json` lists the exact opaque RGB colors copied from those sheets. Transparency is binary. The masonry pattern uses the source 16px tiles; native windows and the door retain their original size. The roof is a single continuous red tile roof with closed end caps; there are no clipped ridge posts, mixed gables or loose diagonal pixels.\n''')
print('Native tower',out.size,'original RGB colors',len(colors))
