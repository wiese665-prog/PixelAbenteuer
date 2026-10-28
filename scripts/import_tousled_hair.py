"""Fit the restyled hair to Mana Seed's native head footprints and palettes.

Run from the repository root with Pillow installed. Existing dap1 sheets supply
head bounds, compact silhouettes, selective contours and all five palette colors.
The source PNG supplies the new strand pattern.
"""
from collections import Counter
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'app/src/main/assets/sprites/ManaSeedCharacter'
SOURCE = ROOT / 'art/source/tousled-hair-approved.png'
REFERENCE = [(24, 24, 24), (82, 82, 95), (124, 137, 137),
             (167, 181, 167), (220, 220, 203)]


def cell(sheet, row, column):
    return sheet.crop((column * 64, row * 64, column * 64 + 64, row * 64 + 64))


def load_frames():
    source = Image.open(SOURCE).convert('RGBA')
    frames = []
    for row in range(8):
        line = []
        for column in range(8):
            box = (round(column * source.width / 8), round(row * source.height / 8),
                   round((column + 1) * source.width / 8), round((row + 1) * source.height / 8))
            image = source.crop(box)
            # Ignore the generator's nearly transparent fringe and color specks.
            pixels = [(r, g, b, 255) if a >= 224
                      else (0, 0, 0, 0) for r, g, b, a in image.getdata()]
            image.putdata(pixels)
            bounds = image.getbbox()
            if bounds is None:
                raise ValueError(f'Empty source frame {row}:{column}')
            line.append(image.crop(bounds))
        frames.append(line)
    return frames


def palette(variant):
    path = ASSETS / 'char_a_p1/4har' / f'char_a_p1_4har_dap1_v{variant:02d}.png'
    original = Image.open(path).convert('RGBA')
    common = Counter(p[:3] for p in original.getdata() if p[3] == 255)
    return sorted([rgb for rgb, _ in common.most_common(5)],
                  key=lambda rgb: sum(a * b for a, b in zip(rgb, (0.2126, 0.7152, 0.0722))))


def build():
    frames = load_frames()
    for page in ('p1', 'pONE3'):
        template_path = ASSETS / f'char_a_{page}/4har/char_a_{page}_4har_dap1_v01.png'
        template = Image.open(template_path).convert('RGBA')
        indexed = []
        for row in range(8):
            for column in range(8):
                native = cell(template, row, column)
                bounds = native.getbbox()
                if bounds is None:
                    raise ValueError(f'Empty anchor {page}:{row}:{column}')
                x0, y0, x1, y1 = bounds
                # Keep strand highlights stable through the animation. The native
                # footprint and anchor follow each walking and combat head pose.
                hair = frames[row % 4][0].resize((x1 - x0, y1 - y0), Image.Resampling.NEAREST)
                indices = []
                for y in range(y0, y1):
                    for x in range(x0, x1):
                        pixel = native.getpixel((x, y))
                        if pixel[3] < 128:
                            indices.append(-1)
                            continue
                        edge = any(nx < 0 or nx >= 64 or ny < 0 or ny >= 64
                                   or native.getpixel((nx, ny))[3] < 128
                                   for nx, ny in ((x-1,y),(x+1,y),(x,y-1),(x,y+1)))
                        strand = hair.getpixel((x-x0, y-y0))
                        # The native selective contour is one actual pixel wide;
                        # clipping to the footprint prevents halos and tall tufts.
                        color = pixel[:3] if edge or strand[3] < 128 else strand[:3]
                        indices.append(min(range(5), key=lambda i:
                                       sum((v-w)**2 for v,w in zip(color,REFERENCE[i]))))
                indexed.append((row, column, bounds, indices))
        for variant in range(14):
            ramp = palette(variant)
            result = Image.new('RGBA', (512, 512))
            for row, column, (x0, y0, x1, y1), indices in indexed:
                hair = Image.new('RGBA', (x1 - x0, y1 - y0))
                hair.putdata([(0, 0, 0, 0) if i < 0 else (*ramp[i], 255) for i in indices])
                result.paste(hair, (column * 64 + x0, row * 64 + y0))
            target = ASSETS / f'char_a_{page}/4har/char_a_{page}_4har_tous_v{variant:02d}.png'
            target.parent.mkdir(parents=True, exist_ok=True)
            result.save(target)
            assert set(result.getchannel('A').getdata()) <= {0, 255}
            assert all(cell(result, r, c).getbbox() for r in range(8) for c in range(8))
            assert len(set(result.getdata()) - {(0, 0, 0, 0)}) <= 5
    print('Restyled 28 sheets: native head footprints, five original colors, binary alpha.')


if __name__ == '__main__':
    build()
