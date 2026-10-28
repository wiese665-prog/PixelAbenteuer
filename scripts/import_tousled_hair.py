"""Import the approved hair image into Mana Seed's 64px animation cells.

Run from the repository root with Pillow installed. Existing dap1 sheets supply
only head bounds and color ramps; the hairstyle pixels come from the source PNG.
"""
from collections import Counter
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'app/src/main/assets/sprites/ManaSeedCharacter'
SOURCE = ROOT / 'art/source/tousled-hair-approved.png'
BROWN = [(80, 51, 37), (133, 80, 52), (185, 120, 72), (223, 166, 107)]


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
            pixels = [(r, g, b, 255) if a >= 224 and r > g > b and r - g >= 10
                      else (0, 0, 0, 0) for r, g, b, a in image.getdata()]
            image.putdata(pixels)
            bounds = image.getbbox()
            if bounds is None:
                raise ValueError(f'Empty source frame {row}:{column}')
            line.append(image.crop(bounds))
        frames.append(line)
    return frames


def palette(variant):
    if variant == 11:
        return BROWN  # Preserve the approved brown appearance.
    path = ASSETS / 'char_a_p1/4har' / f'char_a_p1_4har_dap1_v{variant:02d}.png'
    original = Image.open(path).convert('RGBA')
    common = Counter(p[:3] for p in original.getdata() if p[3] == 255)
    return sorted([rgb for rgb, _ in common.most_common(4)],
                  key=lambda rgb: sum(a * b for a, b in zip(rgb, (0.2126, 0.7152, 0.0722))))


def build():
    frames = load_frames()
    for page in ('p1', 'pONE3'):
        template_path = ASSETS / f'char_a_{page}/4har/char_a_{page}_4har_dap1_v01.png'
        template = Image.open(template_path).convert('RGBA')
        indexed = []
        for row in range(8):
            for column in range(8):
                bounds = cell(template, row, column).getbbox()
                if bounds is None:
                    raise ValueError(f'Empty anchor {page}:{row}:{column}')
                x0, y0, x1, y1 = bounds
                # Combat uses its own head anchors. Use the corresponding facing
                # and motion frame rather than reusing outdoor sheet coordinates.
                source_row = row if page == 'p1' else row % 4
                hair = frames[source_row][column].resize((x1 - x0, y1 - y0), Image.Resampling.NEAREST)
                indices = []
                for r, g, b, a in hair.getdata():
                    indices.append(-1 if a == 0 else min(range(4), key=lambda i:
                                   sum((v - w) ** 2 for v, w in zip((r, g, b), BROWN[i]))))
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
            assert len(set(result.getdata()) - {(0, 0, 0, 0)}) <= 4
    print('Imported 28 sheets: 64 frames each, binary alpha, four hair colors.')


if __name__ == '__main__':
    build()
