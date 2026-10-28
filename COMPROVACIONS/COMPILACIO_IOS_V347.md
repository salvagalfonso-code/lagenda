# V347 — Correccions del registre Codemagic v346

Origen: v346 (inclou canvis visuals v344 i configuració de Codemagic v345).

## Canvis
- `IosAgendaApi.tickets()`: API que faltava en els 3 punts de `AgendaIosScreen.kt`; GET autenticat `tickets/mine.php`, igual que Android (`ServerApiRepository.fetchMyTickets`). Conserva els tickets del servidor i els seus codis QR; no genera valors falsos.
- `IosAgendaApi.batecs()`: substitueix `HashMap.putIfAbsent` per `containsKey` + assignació, amb preservació del primer batec per `storyKey`.
- `Clock.ios.kt`: suprimeix el constructor invàlid `NSTimeZone(name=...)`; `NSDateFormatter` interpreta la data segons la zona horària local del dispositiu.

## Validació
- Inspecció estàtica de referències, sintaxi i integritat del ZIP; comparació de fitxers Android amb v346.
- No s'ha compilat Xcode/Kotlin-Native ací. La verificació definitiva s'ha de fer amb Codemagic.
- No s'ha provat en iPhone ni s'ha certificat la paritat visual d'Android.

## Script
No cal canviar l'script Bash de compilació de Codemagic.
