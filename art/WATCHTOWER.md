# User-approved watchtower

The user selected `art/source/watchtower-approved.png` on 29 October 2026 and explicitly authorized this exact version for the game. It replaces the prior native-parts draft. No further window, roof, silhouette or material redesign is applied.

`build_native_watchtower.py` removes transparent padding, preserves the visible aspect ratio, prepares the sprite at 88x204 using nearest-neighbor sampling, and centers it on the existing 160x208 game canvas. Alpha is binary. The village prop position, collision and camera treatment are retained. The asset check compares every output pixel to this approved-source transformation.
