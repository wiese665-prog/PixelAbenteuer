# Watchtower

The approved `watchtower-approved.png` supplies the silhouette and architecture. The follow-up correction keeps that silhouette and the visible aspect ratio while matching every opaque pixel to the original Exterior_house, exterior and Exterior(1) material palette. Nearly black outer-edge pixels borrow material color from the adjacent roof, timber or masonry before palette matching. No black sticker outline is retained. A subsequent cutout pass removes dark fringe pixels and single-neighbor spurs only from the original outermost pixel layer. It reads an immutable mask so the trim cannot propagate inward.

The sprite uses nearest-neighbor sampling at 88x204 on the existing 160x208 canvas with binary transparency. Tests verify that trimming is limited to the original one-pixel perimeter, preserves all interior pixels and that every opaque color belongs to the original native palette.

A shared native material pass now uses a restrained roof ramp and muted original masonry colors. The geometry/cutout stays unchanged. The approach is continuous down to the road.
