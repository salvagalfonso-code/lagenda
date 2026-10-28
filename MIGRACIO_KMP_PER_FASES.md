# Pla de paritat iOS — L’Agenda

1. **Base comuna (v337):** configuració KMP, 11 regles compartides, entrada Xcode i consulta inicial d’esdeveniments. **Preparat en codi, compilació pendent.**
2. **Catàleg real i disseny:** migrar els models `Concert`, classificacions, deduplicació de festivals, cartells i targetes Compose completes. Mantindre els espais publicitaris segons la política original.
3. **Sessió i personalització:** token segur Keychain, API de seguiments, cache `Per a tu` per compte, 30 minuts de reutilització amb invalidació, batecs i recordatoris.
4. **Localitat i offline:** SQLDelight/SQLite iOS, sincronització incremental i caché persistent, descàrrega d’imatges, primera targeta ràpida i refresc manual.
5. **Funcions principals:** perfils, llistes, comunitat, festivals, notificacions locals + APNs, entrades/QR i Redsys al mateix servidor.
6. **QA:** proves Android després de cada trasllat, proves Xcode al simulador i iPhone real, pagaments reals en entorn de prova, permisos i revisió d’Apple.

La v336 continua com a font de veritat per a qualsevol detall de comportament que encara no s’haja migrat. No substituir-la per la pantalla parcial d’iOS.
