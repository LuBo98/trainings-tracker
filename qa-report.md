QA-BERICHTE - Training Tracker
==============================

1. AUTHENTIFIZIERUNG
-------------------
[OK] Login mit validen Credentials (max/max123) - funktioniert
[OK] Logout - funktioniert (POST zu /auth/logout)
[OK] Registration - funktioniert (mit confirmPassword)
[OK] Forgot Password Seite existiert
[BUG] Logout-Button auf Dashboard: Der POST-Logout-Button funktioniert nicht über den Browser (bekanntes Browser-Tool-Problem auf Windows, funktioniert aber in echtem Browser)
[BUG] Login war nach DB-Reset nicht sofort verfügbar - alte DB hatte veraltetes Passwort. Nach Volume-Reset + neuem Build OK.

2. KATEGORIEN CRUD
------------------
[OK] Kategorie erstellen (z.B. "Unterkoerper")
[OK] Kategorie bearbeiten (z.B. "Oberkoerper" -> "Oberkörper")
[BUG] Kategorie löschen: Delete funktioniert nicht über POST (405 Method Not Allowed). DELETE-Endpunkt fehlt oder ist nicht konfiguriert.
[OK] Kategorien werden korrekt gelistet

3. UEBUNGEN CRUD
----------------
[OK] Übung erstellen (z.B. "Bankdruecken2")
[OK] Übung bearbeiten
[OK] Übung löschen
[OK] Übungen werden pro Kategorie angezeigt

4. WORKOUTS ERSTELLEN
---------------------
[OK] Einzelnes Workout eintragen (Übung + Sets/Wdh/Gewicht/Difficulty)
[OK] Bulk Workout (mehrere Übungen auf einmal)
[OK] Workout editieren
[BUG] Workout löschen: DELETE als GET funktioniert, als POST gibt es 405. Die SecurityConfig erlaubt nur GET für /workouts/delete, nicht POST.

5. TRAININGSHISTORIE
--------------------
[OK] Workouts werden nach Datum gruppiert angezeigt
[OK] Workout-Einträge zeigen: Name, Sets x Wdh, Gewicht, Gesamtvolume, Difficulty-Badge
[OK] Expand/Collapse für Workouts vorhanden (Pfeil-Icons)
[OK] Workout-Edit und Workout-Delete Links pro Datum
[OK] Kategorien werden auf der Workouts-Seite gruppiert angezeigt

6. STATISTIK
------------
[OK] 4 Charts: Volume, Difficulty, Gewicht, Sets/Reps
[OK] Stat-Cards: Anzahl Workouts, Gesamt-Sets, Gesamt-Übungen, Gesamtvolume, Durchschnittsgewicht, etc.
[OK] Filter: Datumsbereich, Kategorie
[BUG] Filter-Button "Alle Uebungen" ist vorhanden, aber Filter nach Kategorie scheint nicht zu funktionieren (nur ein Filter-Button sichtbar)

7. MOBILE RESPONSIVENESS
------------------------
[OK] Viewport Meta-Tag vorhanden auf allen Seiten
[OK] Hamburger-Menü auf Mobile (<768px)
[OK] Mobile CSS vorhanden mit Breakpoint bei 768px
[OK] Form-Controls haben min-height: 44px auf Mobile
[OK] Dashboard-Grid wird single-column auf Mobile
[OK] Navbar-Navigation wird full-width auf Mobile mit min-height: 44px
[WARN] Input-Felder auf Desktop sind nur ~32px hoch (unterhalb 44px)
[WARN] Checkbox "Angemeldet bleiben" ist zu klein für Touch (~13x13px)
[WARN] Difficulty-Badges sind sehr klein auf Mobile (0.75rem)

8. WORKFLOW-ANALYSE
-------------------

MENÜPUNKTE:
- Dashboard (Home)
- Kategorien
- Workouts
- Bulk Workout
- Statistik

ANALYSE:
- "Bulk Workout" ist ein separater Menüpunkt. Die meisten User werden wahrscheinlich nur einzelne Workouts eintragen. Bulk Workout ist eher ein Power-User-Feature.
- "Workouts" und "Bulk Workout" sind redundant - beide führen zum gleichen Ziel (Workout eintragen).
- Auf dem Dashboard gibt es Quick-Access-Buttons: "Neue Kategorie anlegen" und "Statistik ansehen". Aber keine Quick-Access für "Workout eintragen" - das ist der häufigste Use-Case!

KLICK-ZAHLEN:

Szenario: Workout eintragen
- Aktuell: Dashboard -> Workouts -> Kategorie -> Workout eintragen (4 Klicks)
- Oder: Dashboard -> Bulk Workout -> Kategorie -> Workout eintragen (4 Klicks)
- Besser: Direkt von Dashboard einen Button "Workout eintragen" (3 Klicks)

Szenario: Neue Kategorie erstellen
- Aktuell: Dashboard -> Kategorien -> Neue Kategorie (3 Klicks)
- Besser: Dashboard hat bereits einen Button "Neue Kategorie anlegen" (1 Klick)

Szenario: Trainingshistorie ansehen
- Aktuell: Dashboard -> Workouts (2 Klicks)
- OK

Szenario: Statistik
- Aktuell: Dashboard -> Statistik (2 Klicks)
- Besser: Dashboard hat bereits einen Button "Statistik ansehen" (1 Klick)

9. FEHLER/ZUSÄTZLICHE PROBLEME
------------------------------
[BUG] Logout-Button: Der POST-Logout-Formular-Button auf dem Dashboard funktioniert nicht korrekt. Der Logout-Link gibt einen 404 zurück (GET nicht erlaubt).
[BUG] Kategorie löschen: POST auf /categories/delete gibt 405. DELETE-Endpunkt fehlt.
[BUG] Workout löschen: POST auf /workouts/delete gibt 405. Nur GET erlaubt.
[WARN] CSRF-Token erscheint 4x auf der Bulk Workout Seite (doppelte CSRF-Inputs).
[WARN] Auf der Bulk Workout Seite gibt es 3 CSRF-Token-Inputs - das ist unnötig und könnte zu Fehlern führen.
[WARN] Die stat-card auf dem Dashboard zeigt nur Icons (K, W, S) statt vollständiger Texte.

10. EMPFEHLUNGEN
----------------

HIGH PRIORITY:
1. Logout-Button fixen - entweder GET erlauben oder POST korrekt konfigurieren
2. Kategorie-Delete fixen - DELETE-Endpunkt hinzufügen
3. Workout-Delete fixen - POST oder DELETE erlauben
4. Dashboard-Quick-Access für "Workout eintragen" hinzufügen (häufigster Use-Case!)

MEDIUM PRIORITY:
5. "Bulk Workout" aus Hauptmenü entfernen und stattdessen als Button auf der Workouts-Seite anzeigen
6. CSRF-Token Duplikate auf Bulk Workout Seite entfernen
7. Difficulty-Badges auf Mobile vergrößern
8. Checkbox "Angemeldet bleiben" vergrößern für Touch
9. Stat-Cards auf Dashboard mit vollständigen Texten statt nur Icons

LOW PRIORITY:
10. Filter auf Stats-Seite nach Kategorie verbessern
11. Forgot Password Funktionalität implementieren (Seite existiert aber kein Backend)
12. Logout-Link im Hauptmenü statt nur im Dropdown
