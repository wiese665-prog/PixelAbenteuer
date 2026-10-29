# Approved inn integration

`source/inn-approved.png` is the final user-approved wider/deeper inn with the dormer extended to the back roof ridge and straight lower walls. `build_approved_inn.py` crops to alpha, thresholds transparency, samples nearest-neighbor into a 200x144 canvas and maps to original exterior material colors. It removes one native pixel of dark outside fringe and edge spurs; interior dark colors remain material shades, never opaque black. No resampling blur or semi-transparent matte is retained.

The front door remains on the original house center and threshold anchor. Its own six-frame opening sequence is derived from the actual wooden leaves, including their arched top; window, stone arch and foundation pixels stay unchanged. The wider footprint has matching player/resident navigation collision. The regeneration script calls this approved builder after the older generic house pipeline so the accepted asset cannot be overwritten by the discarded inn.
