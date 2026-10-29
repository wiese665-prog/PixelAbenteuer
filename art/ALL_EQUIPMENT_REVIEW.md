# Vollständige Ausrüstungsprüfung – 29. Oktober 2026

Grundlage: [Rüstungsanalyse](ARMOR_STYLE_ANALYSIS.md), 66 ursprüngliche Outfit-/Hutblätter, alle definierten tragbaren Rüstungsgegenstände und die tatsächlichen Renderpfade in Charaktererstellung, Innenräumen und Außenwelt.

Die anschließende vollständige Neuzeichnung der Kopfbedeckungen ist in [HEADGEAR_REDESIGN.md](HEADGEAR_REDESIGN.md) dokumentiert. Die dort beschriebenen eigenen Silhouetten ersetzen die anfänglichen Helm-Umfärbungen.

## Befunde und Umsetzung

| Gegenstand | Bisheriger Befund | Überarbeitung |
| --- | --- | --- |
| Wooden Armor | Gleiche getragene Grafik wie Leder und Eisen | Eigener Holz-/Leinenfarbsatz auf nativer Rüstungsform |
| Leather Armor | Gleiche getragene Grafik wie Holz und Eisen | Braunes Leder, grüne Stoffeinsätze, klare Kontur |
| Iron Armor | Beige Rüstung statt erkennbar metallischer Fläche | Originale graugrüne Stahlfarben, Lederärmel und dunkle Einsätze |
| Leather Helmet | Roter Standardhelm | Brauner Lederhelm mit originalen Licht-/Schattenflächen |
| Iron Helmet | Derselbe rote Standardhelm | Vier native Metallfarbstufen |
| Helm | Derselbe rote Standardhelm | Eigene dunklere Stahlfarbstufen |
| Leather Boot | Kein eigener sichtbarer Ausrüstungslayer | Lederfarbene native Schuhpixel je Pose |
| Iron Boot | Kein eigener sichtbarer Ausrüstungslayer | Metallfarbene native Schuhpixel je Pose |
| Belt | Nur Spielwerte/Inventar, kein sichtbarer Layer | Schmaler Ledergürtel mit kleiner Schnalle |
| Bag | Nur Spielwerte/Inventar, kein sichtbarer Layer | Kleine seitliche Ledertasche |
| Waldlaeuferwams / Waldlaeuferkapuze | Bereits an Originalpalette angepasst | Erneut mit allen Seiten und Mischkombinationen geprüft |
| Magierrobe / Magierhut | Bereits an Originalpalette angepasst | Erneut mit allen Seiten und Mischkombinationen geprüft |
| Wizard Hat | Alias des Magierhuts | Einheitliche Grafik und passendes Inventarbild beibehalten |
| Unterbekleidung | Native Originale mit fünf Farben | Geprüft; bei leerem Brustslot weiterhin geschlechtsabhängig korrekt |

Holz-, Leder- und Eisenmaterialien bleiben auch bei Seltenheits- oder Instanzsuffixen korrekt zugeordnet. Neue Kleidung verändert keine Rüstungswerte, Kampfwerte, Klassen oder gespeicherten Gegenstands-IDs.

## Pixelarbeit

- Die 66 ursprünglichen Quellen bleiben erhalten. Ihre bereits passenden Konturen und Animationen werden nicht pauschal neu gezeichnet.
- 50 abgeleitete Ausrüstungsblätter: drei Brustmaterialien, drei Helme, zwei Stiefel und zwei Gürtelgegenstände für jeweils fünf Animationsseiten.
- Die Materialumsetzung erhält die RGB-Identität der ursprünglichen Farbflächen. Dunkle Konturen bleiben dunkel; kein globaler Helligkeitsfilter.
- Ausschließlich tatsächlich vorhandene Charakterfarben, 64 × 64 Pixel pro Frame, binäres Alpha, keine geglätteten Ränder.
- Die ursprünglichen Brust- und Helmkonturen bleiben in den drei nativen Seiten exakt erhalten. Bei Bogen-/Zauberposen werden die Ärmelbereiche entsprechend den vorhandenen ausgestreckten Armen freigestellt.
- Stiefel sind auf die unteren vier Pixelreihen der jeweiligen Körperpose begrenzt. Die Rüstung wird nicht am Bildschirm nachträglich verschoben.
- Zehn bisherige Inventarbilder werden durch scharfe, ganzzahlig skalierte Ausschnitte aus den passenden getragenen Layern ersetzt. Alte Quelldateien bleiben erhalten.

## Integration

`CharacterEquipment.java` löst alle Ausrüstungsslots und Varianten zentral auf. Alle drei Zeichenpfade benutzen dieselbe Zuordnung. Der Player-Layer-Cache berücksichtigt jetzt auch Fuß- und Gürtelausrüstung; ein Wechsel erscheint sofort, auch während die Figur stillsteht. Die Layer werden wie die bestehenden Charakterblätter geladen und wiederverwendet; keine Erzeugung von PNGs oder Farbscans während eines Frames.

Die neuen Materialsets existieren auch für `pBOW` und `pCAST`. Damit fallen alte Brustmaterialien beim Wechsel auf Bogen oder Stab nicht mehr auf eine falsch gefärbte Standardrüstung zurück.

## Validierung

- `verify_equipment_materials.py`: 1.920 Brust-/Helmposen, ursprüngliche Alphakonturen und Konturfarbe, native Palette, unterschiedliche Materialien, Fußgrenzen und zehn Inventarsymbole.
- `CharacterEquipmentTest.java`: tatsächliche Auflösung aller 15 Gegenstandszuordnungen mit drei Namens-/Seltenheitsvarianten und fünf Seiten; 225 Blattauflösungen, jeweils vier sichtbare Richtungen sowie leere Slots.
- Bestehende Klassenprüfung: 2.240 Ausrüstungspose-Layer, Hautfarben, Kopf-/Handanbindung und Angriffsframes.
- Vergleichstafel und Bewegungsübersicht aus echten Produktionslayern: [alle Sets](reviews/all-equipment-comparison.png), [Animationen](reviews/all-equipment-animation.gif).
- Android-Build und bestehende Welt-/Kampfsimulationen über GitHub Actions. Neuer Gerätetest und Bildratenmessung stehen weiterhin aus.
