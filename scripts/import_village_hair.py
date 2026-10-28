"""Import distinct ponytail and curly hair using the native palettes and anchors.

Run with Pillow from any directory. Pixel art is sampled with nearest-neighbor;
all exported frames use binary alpha. Native head motion drives every pose.
"""
from pathlib import Path
from collections import Counter
from collections import deque
from PIL import Image, ImageOps

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'app/src/main/assets/sprites/ManaSeedCharacter'
REFERENCE = [(24,24,24),(82,82,95),(124,137,137),(167,181,167),(220,220,203)]


def closest(rgb):
    return min(range(5), key=lambda i: sum((a-b)**2 for a,b in zip(rgb,REFERENCE[i])))


def isolate_hair(frame):
    # Generated sheets can include a neighboring row fragment or stray pixels.
    # Keep only the main connected hair silhouette before measuring its bounds.
    mask=frame.getchannel('A');seen=set();largest=[]
    for y in range(frame.height):
        for x in range(frame.width):
            if (x,y) in seen or mask.getpixel((x,y))<128:
                continue
            pending=deque([(x,y)]);seen.add((x,y));component=[]
            while pending:
                px,py=pending.popleft();component.append((px,py))
                for nx,ny in ((px-1,py),(px+1,py),(px,py-1),(px,py+1)):
                    if 0<=nx<frame.width and 0<=ny<frame.height and (nx,ny) not in seen and mask.getpixel((nx,ny))>=128:
                        seen.add((nx,ny));pending.append((nx,ny))
            if len(component)>len(largest):largest=component
    out=Image.new('RGBA',frame.size)
    for pos in largest:out.putpixel(pos,frame.getpixel(pos))
    return out.crop(out.getbbox())


def build():
    for kind in ('pony','curl'):
        source = Image.open(ROOT / f'art/source/{kind}-hair.png').convert('RGBA')
        directions = []
        for facing in range(4):
            animation=[]
            for column in range(8):
                frame=source.crop((round(column*source.width/8),round(facing*source.height/8),round((column+1)*source.width/8),round((facing+1)*source.height/8)))
                frame.putdata([p if p[3]>=240 and max(p[:3])-min(p[:3])<70 else (0,0,0,0) for p in frame.getdata()])
                frame=isolate_hair(frame)
                if kind=='pony' and facing>=2:frame=ImageOps.mirror(frame)
                animation.append(frame)
            directions.append(animation)
        for page in ('p1','pONE3'):
            template = Image.open(ASSETS / f'char_a_{page}/4har/char_a_{page}_4har_dap1_v01.png').convert('RGBA')
            indexed = Image.new('L',(512,512),255)
            for row in range(8):
                facing = row%4
                for col in range(8):
                    native = template.crop((col*64,row*64,col*64+64,row*64+64))
                    x0,y0,x1,y1 = native.getbbox()
                    if kind == 'pony':
                        w,h = ((16,15) if facing==0 else (16,20) if facing==1 else (20,18))
                        left = round((x0+x1-w)/2) + (-2 if facing==2 else 2 if facing==3 else 0)
                        if facing==0:left+=1
                        left += 1
                        top = y0-1
                    else:
                        w,h = x1-x0+4,y1-y0+3
                        left,top = x0-2,y0-2
                    # Respect crouching/falling head compression from the native pose.
                    if page=='pONE3' and facing==0 and row==4 and col in (1,2,3):
                        h=max(10,h-2)
                    hair = directions[facing][col if row>=4 or page=='pONE3' else 0].resize((w,h),Image.Resampling.NEAREST)
                    for y in range(h):
                        for x in range(w):
                            p=hair.getpixel((x,y))
                            if p[3]>=128:
                                indexed.putpixel((col*64+left+x,row*64+top+y),closest(p[:3]))
            for variant in range(14):
                palette_image=Image.open(ASSETS / f'char_a_p1/4har/char_a_p1_4har_dap1_v{variant:02d}.png').convert('RGBA')
                colors=Counter(p[:3] for p in palette_image.getdata() if p[3]==255)
                ramp=sorted([rgb for rgb,_ in colors.most_common(5)],key=lambda p:sum(a*b for a,b in zip(p,(.2126,.7152,.0722))))
                out=Image.new('RGBA',(512,512))
                out.putdata([(0,0,0,0) if i==255 else (*ramp[i],255) for i in indexed.getdata()])
                path=ASSETS / f'char_a_{page}/4har/char_a_{page}_4har_{kind}_v{variant:02d}.png'
                out.save(path)
                assert set(out.getchannel('A').getdata())<={0,255}
    print('Exported 56 sheets for ponytail and curls, including walking and combat poses.')


if __name__ == '__main__':
    build()
