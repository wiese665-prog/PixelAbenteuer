# Loose half-up long hairstyle

The previous straight long haircut is replaced with a loose half-up ponytail, a side-swept fringe and short face-framing locks. A narrow tied tail replaces the old full-width curtain. The tail sways one native pixel in walking/attack frames while the crown follows each native head pose exactly. There are 84 replacement sheets: foreground and rear layers for p1, pONE2 and pONE3, in all 14 original hair palettes.

`import_long_hair.py` draws on the native 64px cells. Native crown shading provides the original small highlight clusters and exact head coverage. The nape tie and smoothly tapered tail are newly drawn, with a consistent shadow/light strand instead of scattered patches. Profile foreground locks cover rear neck seams. The ranged attack pages share the exact pONE2 frame-zero head anchor, so every hairstyle follows the new bow/casting body without a floating head offset.

`verify_character_views.py` checks all 2688 frame/color combinations, binary transparency, native palettes, opaque coverage of native heads, profile scalp coverage and animated lengths. Device testing remains separate.

Second-review correction: the profile tail curves behind the shoulder and tapers in width. Only the nape seam and compact tie are foreground pixels; lower locks cannot cover the forearm with a straight rectangular strip.
