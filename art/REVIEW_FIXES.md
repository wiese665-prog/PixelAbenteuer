# Follow-up to the second in-game review

Scope: points 1–6, building details/spacing and subsequently authorized player visibility behind objects. Enemy names, health bars, damage numbers, alerts and NPC label formatting are unchanged.

## Feet and visibility

`CharacterPose` is the common frame resolver for actual rendering and body-foot lookup. The selected native body's opaque bottom row is cached per frame and skin. A reusable 128x128 player layer is aligned to the existing logical foot position, py+24 outdoors, and the ground projection is inverted about that foot rather than the old sprite center. Shadow, depth ordering, collision and actor interactions therefore share the foot anchor. Furnished interiors and the smithy use the same body-foot calculation.

The layer is cached by appearance, selected frame and reaction stage; camera movement alone does not redraw the sprite. The layer is also used as a faint gold silhouette when an object drawn in front covers opaque body pixels. `VisibilityProbe` uses at most 15 alpha probes per overlapping object, checking both real player pixels and source object transparency. It supports trees, noticeboard, building roofs, decorations and furnished-room furniture. Transparent atlas margins do not trigger the effect. Bitmaps, alpha accessors and foot bounds are reused; there is no full-screen readback, per-frame sprite scan or growing particle list.

## Village safety and spacing

The tower moved from x1010 to x1110, leaving a clear gap beside the inn. Door approach and shallow foot collision follow the shared tower coordinates. Overlapping border trees are omitted around the new footprint. `VillageSafety` includes the eastern public paths/tower wing, a 24px protected margin and an 80px wildlife exclusion margin. The clearance exceeds melee attack reach. Spawn, movement, chase paths and old-save relocation use the same exclusion, so old animals cannot remain beside an entrance. Decorative birds are placed farther into the forest.

## Sprite corrections

The hood now has a separate sewn-cloth silhouette, broad fabric folds, an actual transparent face opening and neck closure. It no longer recolors bob hair. The bowstring has clear space from the wooden curve. The robe's golden hem is continuous instead of being interrupted by blue endpoint pixels. Profile hair follows a curved tapered tail behind the shoulder; only the nape seam and tie draw in front, preventing the former rectangular strip across the arm.

Side shields use fresh `shP1`/`shP3` sheets in both native side directions and both attack groups. `CharacterPose.shield` is called by creator and combat, and tested directly. The creator restores only a small palm crop above the shield instead of an entire forearm. Old profile files remain historical resources but have no active rendering reference.

The accepted building shapes/cutouts are preserved. Roof surfaces use actual original 16px roof texture samples, with retained roof edges/ridges and original exterior glass colors. Palette, alpha and door-frame checks remain active.

## Verification

CI resolves frames against actual body PNGs using ImageIO and verifies projected feet against the shadow anchor. Further tests cover side shield selection, village safety/attack clearance, transparent versus opaque occlusion, furnished-room routes, one-hour resident simulation, bounded mob navigation, intermittent ambience and projectile collision/damage. Native sprite palette/head/attachment checks and the Android APK build also run. Asset composition reviews are in art/reviews. No Android emulator or device playtest is available in this environment; these are not device screenshots.
