# L’Agenda v343 — Migració iOS acumulada sobre v342

## Funcions implementades ara

1. **Gest de refresc de «Per a tu» i «Sense filtre»**: des de la part alta de la llista, estirar cap avall força `refresh(true)` (no espera el TTL de 30 minuts). No tapa els esdeveniments existents ni mostra roda de càrrega; una línia de progrés fina indica l'actualització. Els intents simultanis es descarten mitjançant `updating`.
2. **Festivals en cerques**: si el text buscat coincideix amb un artista del cartell horari, la fitxa del festival s'obri en «Horaris», selecciona el dia de l'actuació i la destaca al principi. No hi ha text «Actuació buscada». Si hi ha una sola jornada, no es mostra selector redundant. Conserva Info, Entrades, Ubicació i Comunitat.
3. **Recordatoris iOS verificats**: `scheduleLocalReminderConfirmed` comunica el resultat del `UNUserNotificationCenter.add` a Kotlin amb un callback, i només després s'activa l'avís al servidor i s'actualitza la interfície. Es tracta l'error o la denegació del permís sense confirmar un recordatori que no existix. Si el servidor falla, es cancel·la la notificació local. Els identificadors pendents són específics del compte, i s'elimina la notificació amb identificador antic de v342 quan es programa de nou.

## Què no s’ha canviat

- `app/src` Android: els **91 fitxers originals** són idèntics als de v342.
- No s'ha canviat cap endpoint ni la base de dades en producció.
- El sistema de compra de pagament, APNs i les funcions avançades d'AgendaPRO **continuen pendents**. Consulteu `PARITAT_IOS_V343.csv`.

## Comprovacions passades ací

- Kotlin pur: 7 proves de separació de comptes i 4 de batecs.
- `swiftc -frontend -parse` als tres fitxers Swift.
- `plutil -lint` del projecte Xcode i `Info.plist`.
- `bash -n` i comparació SHA256 dels fonts Android amb la v342.
- Validació de rutes del pont Kotlin–Swift, sense compilació de framework.

## IMPORTANT — comprovacions encara pendents

**No s'ha executat una compilació completa Gradle, Kotlin/Native ni Xcode.** La sintaxi de Swift no comprova el tipatge del nou callback exportat per KMP. Fins que passe el build en un Mac, esta versió no és apta per a instal·lació ni per a App Store.

### Primera compilació en Mac

Des de la carpeta `ANDROID_STUDIO`:

```sh
sh COMPROVACIONS/comprovar_v343_mac.sh
sh COMPROVACIONS/compilar_ios_mac.sh
```

Si falla, conserveu el **primer error** de Gradle/Xcode (amb fitxer i línia) i corregirem sobre la mateixa v343.

### Pruebes del simulador

- Obrir Inici i fer **pull-to-refresh** de dos a tres vegades seguides: una petició alhora i llista visible.
- Buscar un artista present només en un horari d’un festival multidia; comprovar que s’obri la jornada correcta. Provar festival d’un dia.
- Denegar el permís de notificacions: l’avís **no** ha de quedar actiu per haver tocat Guardar.
- Acceptar el permís i programar un avís futur: apareix confirmació després de la resposta de iOS; tornar a obrir l’app i desactivar-lo.
- Entrar amb dos comptes diferents i provar que les notificacions locals noves usen identificadors per compte.
