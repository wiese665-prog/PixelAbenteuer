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

All 17 residents, including the three original smithy actors, use the same Android cursive typeface and 13 logical pixel size for both name and purple title. Outdoor labels cancel the 1.10 world zoom, so their text and line spacing have the same screen size as indoor labels. All building names, including the watchtower and interior headings, share one serif italic drawing routine at 13 logical pixels and the same gold color.
