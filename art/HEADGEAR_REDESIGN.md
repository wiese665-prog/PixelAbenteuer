# Neue Kopfbedeckungen – 29. Oktober 2026

Die vorige Umsetzung unterschied mehrere Helme nur über ihre Farben. Kapuze und Magierhut hatten außerdem zu runde beziehungsweise unpassende Konturen. Grundlage der Neuzeichnung sind die nativen 64×64-Frames, die ursprünglichen Materialpaletten und die tatsächlichen Kopfpositionen aus der Rüstungsanalyse.

## Eigene Formen

- **Holzhelm (`Wooden Helmet`, `wdHl`)**: flache Krone aus Holzsegmenten mit senkrechter Maserung, Lederstirnband und offener Gesichtsseite. Keine Umfärbung des Eisenhelms.
- **Eisenhelm (`irHl`)**: gewölbte Schale mit Mittelgrat, vorstehendem Stirnrand, kurzen Wangenplatten und frontalem Nasenschutz. Der Nacken erhält einen eigenen Schutz statt einer Stofffalte.
- **Lederhelm (`leHl`)**: kleinere, offene Lederhaube mit niedriger Krone, freier Gesichtspartie und kurzem Nackenteil. Ebenfalls neu gezeichnet, damit die bisher beim Holzset gezeigte braune Kopfbedeckung nicht unverändert bleibt.
- **Waldläuferkapuze (`hood`)**: engere asymmetrische Stoffkontur, klare Gesichtsaussparung, kurze Kinn-/Nackenpartie und wenige zusammenhängende Stofffalten. Der breite runde Rahmen entfällt.
- **Magierhut (`mgHt`)**: kompakter Kegel mit kurzer umgelegter Spitze, schmalerem Profilrand, wenigen blauen Lichtflächen und gedämpftem Messingband. Die lange seitliche schwarze Spitze entfällt.

Einen eigenständigen Holzhelm gab es vorher nicht. Die Vergleichstafel kombinierte Holzrüstung mit Lederhelm. Daher ist der neue Holzhelm ein eigener ausrüstbarer Kopfgegenstand; vorhandene Lederhelme bleiben Lederhelme. Maela führt den Holzhelm auch in bestehenden Spielständen. Er unterstützt dieselben Seltenheits-/Instanzsuffixe wie die anderen Ausrüstungsgegenstände.

## Pixelmaß, Farben und Animation

Alle Formen werden direkt in nativen Pixeln gezeichnet: eine Pixelreihe für Kanten, kleine klare Farbcluster statt weicher Übergänge, binäres Alpha und ausschließlich originale Charakterfarben. Holz, Stahl, Leder und Stoff behalten getrennte Schatten-/Mittel-/Lichttöne. Körper, Rüstungen und Waffen werden nicht skaliert, verschoben oder umgefärbt.

`native_headgear.py` ist die gemeinsame Quelle für beide bestehenden Spritegeneratoren. Jede Kopfbedeckung folgt der tatsächlichen Kopfposition des jeweiligen Frames, einschließlich der verschobenen Köpfe beim Gehen, Schlagen, Bogen-/Zauberangriff und bei Reaktionsposen. Vorder-, Rück- und Profilansichten haben eigene sichtbare Flächen. Links wird um den jeweiligen nativen Kopfmittelpunkt gespiegelt, nicht um die Zellmitte.

25 neu gezeichnete Blätter = fünf Kopfbedeckungen × fünf Animationsseiten. Dazu die passenden Inventarsymbole. Wizard Hat bleibt derselbe sichtbare Gegenstand wie Magierhut.

## Prüfung

- `verify_headgear.py`: 1.600 neu gezeichnete Kopfbedeckungsframes; exakte Kopfanbindung, native Farben, binäres Alpha und keine angeschnittenen Zellränder.
- Fünf tatsächlich unterschiedliche Alphasilhouetten je Pose. Holz-/Eisenkonturen müssen deutlich voneinander abweichen; verschiedene RGB-Werte allein bestehen die Prüfung nicht.
- Bestehende Klassen-/Materialprüfungen bleiben aktiv. Originalkonturgleichheit wird weiterhin für die nicht neu entworfenen Brustmaterialien und den unveränderten Standardhelm geprüft.
- `CharacterEquipmentTest`: 16 Gegenstände, drei Namens-/Seltenheitsvarianten, fünf Animationsseiten, vier Richtungen und leere Slots.
- [Aktuelle Vergleichstafel](reviews/all-equipment-comparison.png) und [Bewegungsübersicht](reviews/all-equipment-animation.gif) sind Kompositionen der tatsächlichen Produktionslayer, keine Geräteaufnahmen.
- Android-Build über GitHub Actions; neuer Gerätetest und FPS-Messung weiterhin offen.

## Detailüberarbeitung

Die Kapuze erhält eine neue Kontur mit flacher Stirn, lockerem hinterem Stoffvolumen und überlappendem Kragen. Holz bekommt versetzte Segmentenden, Maserung und Befestigungen; Leder abgesetzte Nähte und Panellichter; Eisen feinere Grat-, Wangen- und Nackenkanten. Beim Magierhut trennen zusätzliche native Farbcluster die gefaltete Kegelfläche, das Band mit Schnalle und die Krempe. Die Details sind in Vorlagenfarben auf dem unveränderten nativen Raster ausgeführt. Alle 1.600 Kopf-Frames und die bestehenden Klassen-/Ausrüstungsprüfungen wurden erneut geprüft.


## Aktuelle Neufassung nach Vorlagen

Holz, Leder, Kapuze und Magierhut verwenden neu aufgebaute Formen mit den ursprünglichen Farbclustern aus `pfht` beziehungsweise `pnty`. Die Original-Kopfbewegungen bestimmen ihre Position. Holz ist gewölbt, Leder eine offene Haube, die Kapuze hat einen breiteren Stoffkragen. Der Magierhut ist jetzt der geknickten, breiteren Vorlagenform nachempfunden. Eisen behält den Mittelgrat und die Wangenplatten, erhält aber die weich abgestuften nativen Metallflächen.

Der doppelte Gegenstand `Helm` und seine `stHl`-Blätter entfallen. Alte Spielstände migrieren ihn in Inventar, Ausrüstung, Schnellleiste, Händlerangeboten und Bodenobjekten zu `Iron Helmet`; Seltenheits-/Instanzsuffixe bleiben erhalten. Die Vergleichstafel zeigt nur noch fünf Ausrüstungen.

## Korrektur vom 30. Oktober 2026

Eisen hat eine abgerundete Schale mit kurzen Pixelstufen und symmetrischen Frontflächen einschließlich Wangenplatten. Holz erhält eine flachere segmentierte Schale mit gebogenem Bindungsrand. Leder wird als weiche, vernähte Haube mit Ohrenschutz neu gezeichnet. Die Kapuze erhält eine asymmetrische Stoffkrone, freie Gesichtspartie und eine hintere Falte.

Beim freigegebenen Magierhut bleibt die Form erhalten: Nur der goldene Bogen der Vorderansicht wird innerhalb seines bisherigen Bereichs nach unten gewölbt. Die anderen drei Ansichten und der Alphakanal bleiben unverändert.

Alle fünf Animationsseiten und die passenden Inventarsymbole werden aus derselben Kopfverankerung erzeugt. Die Prüfung kontrolliert zusätzlich die pixelgenaue RGB- und Alpha-Symmetrie aller Eisen-Frontframes. Vergleichsbilder sind Kompositionen der Produktionssprites; ein Gerätetest steht noch aus.
