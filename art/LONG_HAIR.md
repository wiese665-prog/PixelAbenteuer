# Redrawn long hair

`import_long_hair.py` builds 84 replacement sprite sheets: foreground and rear layers for p1, pONE2 and pONE3, in all 14 native hair palettes. It draws new crown shading, continuous side locks and tapered animated lengths. It does not copy bob hair colors or extend the old bob outline. The native hair alpha masks supply exact per-frame head coverage and position, including leaning combat poses.

Foreground side locks cover the rear neck in profile; the rear curtain sits behind the body except in the back view. A single palette and shading function prevents mismatched scalp patches and short-hair seams. Walking and combat ends sway one native pixel while the crown remains anchored.

`verify_character_views.py` checks all 2688 frame/color combinations, binary transparency, native palettes, exact opaque coverage of native heads, profile rear-scalp coverage and animated lengths. These asset checks do not replace a device playtest.
