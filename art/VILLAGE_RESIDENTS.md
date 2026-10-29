# Named village residents

Every inhabitant has one persistent identity and one current location. Mobile residents use the same actor object outdoors, during door and stair transitions, and inside houses. Combined talking-pair sprites are not spawned alongside their individual characters. Training dummies are solid props, not duplicate residents.

| Name | Title | Home / activity |
| --- | --- | --- |
| Borin | Schmied | Original smithy; weapon shop |
| Maela | Händlerin | Original smithy shop |
| Tarek | Abenteurer | Original smithy; existing quest chain |
| Lina | Kräuterkundlerin | Village, herb house, guild library, bakery |
| Jorin | Bäcker | Village, bakery, inn, guild reception |
| Rovan | Dorfwache | Village patrol, guild training/council, inn, residence |
| Grom | Händler | Village, inn, guild reception, herb house; existing mixed shop |
| Aldric | Gildenmeister | Guild council chamber |
| Kael | Waffenmeister | Guild training area |
| Varek | Feuermagier | Guild council/magic room |
| Elian | Zauberlehrling | Guild council/magic room |
| Orin | Alchemist | Herb-house potion table |
| Lyra | Magierin | Guild council/magic room |
| Alwin | Chronist | Guild library |
| Mira | Gelehrte | Guild library |
| Neris | Reisende | Inn |
| Edda | Wirtin | Inn |

Names and purple profession titles are drawn separately. Every resident has several personal dialogue lines; E near a resident starts a conversation or opens their existing shop. Tarek keeps the existing quest rewards and cooldowns. Mobile characters stop while speaking, respect other actors/player feet, and use navigable paths around house and furniture bases. At most two mobile inhabitants visit indoors at once, keeping the village populated. Entrances, exits and guild stairs remain automatic and have no E icons.

Resident state is saved by ID. Loading updates existing identities rather than constructing new actors. The village melody is also selected for the main, pause and settings menus, including when paused indoors or near enemies.

The last village/video corrections remain in place: northern herb house, new inn/tower, original chimney animations, removed market armor stand/scattered signs/awning, right-side information board, completed forest borders, native grass details and edges, centered spawn pad, corrected night overlay, additional interiors and animated doors. No sprite or map asset was reverted for the resident update.

The APK workflow runs the actual portable Java resident model against exported map/furniture geometry. It checks uniqueness, a simulated hour of village/house/stair visits, stationary characters, actor/player collision, dialogue and restoration before compiling Android. BlueStacks play testing is still required for final visual assessment.

## Shared labels

All 17 residents, including the three original smithy actors, use the same Android cursive typeface and 13 logical pixel size for names and 11.5 for straight purple titles. Outdoor labels cancel the 1.10 world zoom, so their text and line spacing have the same screen size as indoor labels. All building names, including the watchtower and interior headings, share one serif italic drawing routine at 13 logical pixels and the same gold color.

## Character and wildlife corrections

Long hair uses the unshifted native bob crown masks with all six native color indices, and separate animated length sheets. Lengths render behind the torso in front/side views and over the back when facing away. This avoids scalp patches, arbitrary head-box fitting and incorrect shoulder occlusion.

The left creator shield uses the original oblique shield frame. Its attachment center is aligned to the opaque bounds of the original left-facing shield pose, calculated and cached separately for wooden and metal shields. The native hand is redrawn over the rim; the weapon remains in front.

Wildlife no longer starts A* searches for ordinary roaming, flock following or fleeing. Hostile pursuit shares one search slot every 120ms and a 240-node expansion cap. Static navigation cells and fine terrain collision results are cached independently from residents.

## Ambient activity and map density

Trees and birds use deterministic independent animation phases with long idle periods. Idle wild grouse also pause their animation; movement/escape animations remain active. Population limits are ten animals and six orcs, groups at most two, enforced both at spawn and when loading older saves.

Ruin placements are removed. Rock placements are reduced from 85 to 18 and every decoration footprint, including a 12px border, is checked against paths. The noticeboard is moved to (900,535). The well has an independent ground-foot collider centered at (610,510); player checks use py+24 and resident checks use their native foot coordinates.
