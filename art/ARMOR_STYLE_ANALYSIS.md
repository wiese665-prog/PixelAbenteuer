# Rüstungsanalyse und Anpassung – 29. Oktober 2026

## Umfang und Belege

Analysiert wurden alle 66 ursprünglichen Kleidungs- und Kopfbedeckungsblätter in `p1`, `pONE2` und `pONE3`: je Seite fünf `fstr`- und fünf `pfpn`-Varianten, zwei Unterbekleidungen und zehn Hüte. Die vollständige RGB-Häufigkeit, Alphawerte, belegten Frames und Größenbereiche stehen in [armor-style-census.json](reviews/armor-style-census.json). Zusätzlich geprüft: die acht vorhandenen Inventarbilder für Rüstungen, Helme, Stiefel und Zauberhut, die tatsächliche Ausrüstungszuordnung im Spiel und das neue Video `20261029-2012-42.9929199(1).mp4`.

Die Vergleichstafel zeigt echte Produktionslayer mit Körper, ohne Haare und Waffen, damit die Kleidung sichtbar bleibt. Die Animation zeigt Lauf- und Angriffsposen, ebenfalls ohne Waffen. Beides sind Spritekompositionen, keine neuen Geräteaufnahmen.

## Pixelgröße und Formen

- Originale Animationsblätter: 512 × 512 Pixel, acht mal acht Zellen à 64 × 64. Der transparente Zellrand gehört nicht zur Figurengröße.
- `fstr` im Stand: vorne/hinten etwa 14 × 18–19 belegte Pixel, Profile etwa 11 × 18. Über alle p1-Posen: 11–20 Pixel breit und 15–29 hoch.
- Ein sichtbares Detail wird mit einem nativen Pixel gezeichnet. Konturen und Nähte sind überwiegend einen Pixel stark; zusammenhängende Highlights umfassen wenige Pixel.
- Rundungen entstehen durch bewusst abgestufte Konturen. Keine geglätteten Zwischenpixel, keine halbtransparenten Ränder, keine skalierten Konzeptbildausschnitte.
- Seitenansichten sind eigens schmaler; sie zeigen andere Flächen als Front und Rücken. Kleidung folgt dem jeweiligen Körperframe.
- Im Spiel werden diese Pixel vergrößert. Die Projektion und der Zoom verändern die Bildschirmgröße; daraus lässt sich keine neue native Pixelgröße ableiten.

## Originale Farblogik

`fstr` nutzt exakt neun deckende RGB-Farben pro Variante, `pfpn` sieben. Die ursprünglichen Unterbekleidungen nutzen fünf. Die vollständigen Tabellen aller Hüte und Seiten stehen im JSON.

`fstr v01` trennt seine Materialien bereits im Quellbild:

| Funktion | Schatten | Mittelton | Licht |
| --- | --- | --- | --- |
| Kontur | `#181818` | – | – |
| Hauptmaterial | `#716558` | `#A69878` | `#C7C4A2` |
| Ärmel/Akzent | `#5B1839` | `#8D2246` | `#BE5667` |
| Dunkle Einsätze | `#1E2B41` | `#3C5575` | – |

Die Farben sind nicht bloß ein Helligkeitsverlauf. Sie kennzeichnen unterschiedliche Materialien. Eine Einteilung nach durchschnittlicher RGB-Helligkeit zerstört diese Trennung und kann Konturen in Stoff verwandeln. Genau das war eine Schwäche des bisherigen neuen Generators.

Die verwendeten neuen Materialfarben stammen jetzt direkt aus den ursprünglichen `pfpn`-Varianten:

| Material | Dunkel/Kontur | Schatten | Mittelton | Licht |
| --- | --- | --- | --- | --- |
| Alle Konturen | `#181818` | – | – | – |
| Waldläuferstoff, aus v02 | – | `#364A3F` | `#5B7F6A` | `#A8C898` |
| Leder, aus v02 | – | `#583828` | `#986038` | `#F09848` |
| Magierstoff, aus v01 | – | `#193364` | `#3A6797` | `#5999BD` |
| Messingbesatz, aus v01 | – | `#815C21` | `#B29149` | `#E0D28E` |

## Inventarbilder sind eine andere Darstellungsebene

Die vorhandenen Inventarbilder sind jeweils 32 × 32 Pixel, enthalten aber viel mehr Farben als die Körperlayer: Wooden Armor 494, Leather Armor 518, Iron Armor 439, Leather Helmet 468, Iron Helmet 556, Leather Boot 379, Iron Boot 422 und Wizard Hat 342 sichtbare RGB-Farben. Sie sind daher keine geeignete Vorlage für eine siebenfarbige Körperanimation. Deren Farbmischung auf die Spielfigur zu übertragen würde erneut einen anderen Stil erzeugen.

Die neuen Inventarsymbole bleiben auf 32 × 32 und werden aus den tatsächlich ausrüstbaren Produktionslayern erzeugt. Bestehende Inventarbilder wurden nicht umgefärbt.

## Sichtbare Probleme im Video

Die ersten Aufnahmen der Charaktererstellung zeigen den Vergleich besonders deutlich: Der grüne Kopfumriss ist rund und großflächig, der Körper hat eine andere Konturwirkung als die ursprünglichen Figuren. Das Magierset hat sehr auffällige goldorange Linien. Spätere Szenen im Dorf und Gasthaus zeigen dieselben Sets klein im Umfeld der Original-NPCs; dort zählt eine klare Silhouette stärker als zusätzliche Einzelpixel. Die Videoansicht erlaubt eine visuelle Einschätzung, aber keine zuverlässige Messung nativer RGB-Werte oder Bildrate. Dafür wurden die Quell-PNGs ausgewertet.

## Durchgeführte Änderungen

1. Die ursprüngliche `fstr`-Materialzuordnung wird pro RGB-Farbe übersetzt, anstatt alle Materialien anhand der Helligkeit zusammenzuwerfen. Originale Konturpixel bleiben Konturpixel.
2. Waldläuferwams und Kapuze verwenden ausschließlich die originalen grünen Stoff- und Lederfarben. Die Kapuze ist schmaler und hat eine klar abgegrenzte Gesichtsöffnung.
3. Magierrobe und Hut verwenden ausschließlich die originale Blau-/Messingpalette. Der grelle orange Zickzacksaum entfällt.
4. Der Robenrock bekommt einen zusammenhängenden Saum, wenige Stofffalten und eine schmalere Profilform. Die Saumbewegung folgt den Laufphasen; der Rücken hat keine frontale Öffnung.
5. Alle fünf Seiten `p1`, `pONE2`, `pONE3`, `pBOW`, `pCAST` und vier Richtungen wurden erneuert; die passenden vier Inventarsymbole ebenfalls.
6. Körper, Waffen, Spielwerte, Klassenfunktionen und bestehende Rüstungen bleiben erhalten. Die Änderungen betreffen die neu angelegten Kleidungs- und Hutlayer.

Die alten Rüstungsgegenstände Wooden/Leather/Iron Armor verwenden derzeit alle `fstr v01` am Körper; unterschiedliche Inventarbilder bedeuten also noch keine unterschiedlichen getragenen Rüstungsmodelle. Das ist ein bestehender Befund, keine in diesem Auftrag heimlich geänderte Zuordnung. Die neuen Sets haben bereits eigene Outfitnamen.

## Prüfung und Grenzen

- `python scripts/analyze_armor_style.py`: reproduzierbare Bestandsaufnahme, Vergleichstafel und Bewegungsübersicht.
- `python scripts/verify_class_sprites.py`: 2.240 vorhandene Ausrüstungspose-Layer, binäres Alpha, gültige Frames, ursprüngliche Charakterfarben, Kopf-/Handanbindung, elf Hautfarben und verschiedene Angriffsframes. Für neue Kleidungsstücke gilt zusätzlich: ausschließlich originale Charakterrüstungsfarben und vorhandene native Konturfarbe.
- Sichtprüfung der vergrößerten Vier-Richtungs-Tafel und der tatsächlichen PNGs. Keine weich interpolierten Pixel.
- Android-Build und weitere automatische Simulationen laufen über GitHub Actions. Ein neuer Gerätetest und eine FPS-Messung wurden hier nicht durchgeführt.
