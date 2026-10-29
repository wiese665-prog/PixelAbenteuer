# In-game video follow-up

The video showed a flat hood, oversized and poorly separated quiver/bow, an overly angular mage silhouette, staff crystal merging with the brim, flat hair highlights and a frontal left shield. The production sheets now use the original crown's clustered shading for hood folds and hair, a compact rear-mounted quiver, smaller bow profiles, a bent faceted hat with foreshortened side brim and a separate outward-bent staff crystal. Native palm overlays remain in front of grips. All 14 hair palettes and 11 skin tones remain supported.

Four new left-shield sheets preserve each original pose center/height while narrowing the shield surface to about 55% width. Creator and melee combat use those cells rather than repurposing the frontal shield. The visible forearm stays in front.

Buildings keep approved silhouettes and sharp cutouts. A shared native material pass reduces roof-color noise, mutes masonry and aligns blue glass with original exterior colors. The tower approach now runs continuously from y334 to y470.

The inn now has a native-palette bar with complete bottle crops, two paired seating groups and a rear-left sleeping alcove separated by shelves. Edda stands behind the bar at (156,208); a regression test checks that the free approach at (156,255) can still reach her. Room path tests and the resident simulation use the updated furniture footprints.

`render_character_review.py` composes actual production layers, including the native hand crops, for four facings and walk, shoot/cast, hurt and fall states. Falling now includes the native crouched intermediate pose before the pixel-aligned quarter-turn. Reviews are saved under art/reviews. This is asset-level visual inspection, not an Android emulator or device playtest. The APK is built and the projectile/resident/navigation regressions run in CI.

The tower uses a shallow base footprint in foot coordinates instead of a large circle centered on its doorway. Player, resident, projectile and mob-path checks use the corresponding coordinates, allowing approach without walking through the stone base. The resident fixture tests the free, paved approach and solid base.
