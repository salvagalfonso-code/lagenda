# L’Agenda v342 — Integració acumulada Android + iOS

## Canvis verificats sobre v341

1. **Batecs**: només artistes seguits; mínim de 6, ordre del servidor, eliminació de duplicats, 6 cercles inicials. En obrir una fitxa, s’actualitza l’estat «vist» contra `batecs/seen.php` i el cercle passa a gris. La xarxa no bloqueja l’obertura de la fitxa. Encara no és el visor de batecs complet d’Android.
2. **Dades per compte**: preferències locals de categories i pobles i hora dels recordatoris amb claus específiques del compte. En canviar de compte, no es reutilitza la configuració de l’anterior. Les peticions lentes no poden sobreescriure Guardats, Entrades, Batecs o Comunitat si s’ha canviat de compte.
3. **Festivals i recordatoris vigents**: Guardats i Notificacions no oculten festivals perquè la primera jornada siga anterior a hui, sempre que la data final siga vigent.
4. **Proves**: proves unitàries Kotlin pures per a filtres i claus de comptes. No s’han alterat els fonts de l’app Android (`app/src`).
5. **iOS build number**: 342 (0.3.1), sense canviar el bundle ID.

## Comprovacions realitzades en este entorn

- Compilació Kotlin independent i execució de 4 proves de batecs.
- Compilació Kotlin independent i execució de 7 proves de separació de comptes i preferències.
- Sintaxi Swift amb `swiftc -frontend -parse` (NO comprovació del framework Apple enllaçat).
- `plutil -lint` del projecte Xcode i `Info.plist`.
- `bash -n` de scripts i validació de l’estructura i integritat del ZIP.
- Comparació SHA256 de `app/src` v341 i v342 per assegurar Android intacte.

## Estat de la conversió

**No és encara una conversió acabada.** Encara no s’ha compilat iOS amb Xcode; Android iOS no tenen paritat en feeds, popups, entrades pagades, APNs, llistes i AgendaPRO. Vegeu `PARITAT_IOS_V342.csv` (39 apartats) per a la llista detallada.

## Com compilar al Mac

1. Descomprimix el ZIP; obri Terminal en `ANDROID_STUDIO`.
2. Instal·la Xcode complet i JDK 17+; descarrega almenys un simulador iOS.
3. Executa `sh COMPROVACIONS/preflight_ios_v341.sh`.
4. Executa `sh COMPROVACIONS/compilar_ios_mac.sh`.
5. Si falla, copia **el primer error** del log i el fitxer/línia. No dona per validada una compilació per passar proves de sintaxi.

No es modifica cap servidor ni base de dades real; l’API `batecs/seen.php` ja existix en el codi de servidor examinat.
