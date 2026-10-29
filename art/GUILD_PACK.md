# Guild and village reference pack

The user supplied the original guild exterior, furniture, architecture, paving and character animation PNGs. These originals are stored in `app/src/main/assets/village/guild/`. Their RGB palettes and native feature dimensions are retained. No generated illustration is used by the replacement village houses and props.

The hall combines the supplied facade, separate roof and GUILD HALL sign. It replaces the former southeast warehouse. Enter from its south door by walking north or pressing E nearby. Three floors have reception/training, a library and a council/magic room. Stairs work by walking north onto their lower landing or pressing E; lower-left stairs go down, upper-right stairs go up. The current floor is saved.

Moving citizens and guards use 32x32 cells: 12 idle / 6 walk frames, with four directions. Guildmaster uses six 48x32 cells. Readers use eleven 32x48 cells and two 16x32 cells respectively. Mage front casts use seven 48x64 cells; the other rows have different layouts and are not mistaken for a regular 7x4 grid. Fighter sword fronts use six 64x64 cells. Foot anchors stay fixed throughout each animation.

Furniture is depth sorted with residents and the player. Only small base rectangles collide. Stairs, floor carpets and magic seals remain passable. The smithy interior and its collision mask remain unchanged. Wildlife continues to be excluded from the village.

`python scripts/import_guild_pack.py /path/to/original/sheets` imports/composes native assets. It requires Pillow, NumPy and SciPy. `python scripts/verify_guild_layout.py` checks frame bounds, traversable stairs and every resident route segment and writes a visual review sheet outside the checkout.

Validation does not replace an in-game movement and visual test in BlueStacks.

## Village and additional interiors

Kraeuterhaus, Gasthaus, Wohnhaus and Baeckerei have separate furnished interiors (room IDs 3..6). All six house entrances use a timed six-frame native door animation and automatic entry/exit. Guild stairs and house transitions have no E-key icons. The inn uses a distinct long, twin-gabled yellow facade; the herb house is moved north to clear the road. Bakery and herb-house chimneys use the complete original smoke/chimney animation.

Roads sample a verified opaque 48x48 material block, with dark north / light south grass edges from the original atlas. Small native plants are placed only on clear grass, away from roads, houses, beds and the spawn pad. Decorative forest extends beyond all playable map edges while future passage corridors stay open. The former market armor stand and scattered shield/fence signs are no longer placed.

Resident animation selects only populated direction cells, including the six-frame rear idle rows. Night tint is drawn over the full viewport after camera transforms, before UI. New characters start in daylight. The spawn pad and its paving opening share one center.

Rebuild the final tower with `python scripts/build_native_watchtower.py`, then apply the village assets with `python scripts/import_village_refinement.py` after the base pack importer. The APK workflow verifies room routes and native sprite integrity before compiling.
