# L’Agenda iOS — continuació sobre v343 (precompilació)

Base utilitzada: `LAGENDA_V343_KMP_IOS_INTEGRACIO_FUNCIONAL_PRECOMPILACIO.zip`. La numeració de projecte es manté en v343.

## Nova implementació incorporada al codi

### Menú de tres punts del feed

- **Informar:** diàleg amb motius admesos pel PHP `events/report.php`, camp de detalls (fins a 1.500 caràcters) i enviament real a l'API. El report sols es dona per enviat si el servidor retorna `ok=true`; els errors continuen sent visibles.
- **No mostrar esdeveniment:** exclusió d'un ID del feed orgànic.
- **No mostrar artista:** exclusió del primer artista de la fitxa o dels horaris del festival quan no n'hi ha en la capçalera.
- **No mostrar categoria:** exclusió de la categoria de la fitxa.
- **Personalització:** llista per revisar i desfer exclusions d'esdeveniments, artistes i categories.
- **Persistència:** exclusions per usuari amb l'emmagatzematge local existent. Convidat i comptes separats. Els canvis s'apliquen al llistat de manera immediata sense refer l'instantània de 30 minuts de «Per a tu».
- **Garantia comercial:** si l'esdeveniment té campanya activa, consta com a prioritari o ven entrades internament, la regla d'exclusió no el filtra. En estos casos el menú indica per què no es pot ocultar.

### Integració de reports

- `IosAgendaApi.reportEvent` usa `events/report.php` (JSON amb `event_id`, `event_title`, `event_town`, `event_date`, `reason`, `details`, `guest_id`, etc.) segons la còpia local del servidor aportada anteriorment.
- El `guest_id` de report es genera una vegada i es guarda en la caché local. No és una credencial d'autenticació.
- La funcionalitat no esborra automàticament un esdeveniment ni envia notificacions push.
- No s'han fet reports reals de prova a producció.

## Comprovacions executades

1. Compilació i execució amb `kotlinc` de la política pura d'exclusions: **6 casos PASS** (ocultació per ID/artista/categoria; respecte a campanya comercial; cas normal).
2. Sintaxi Swift amb `swiftc -frontend -parse`: **PASS**.
3. Configuració `Info.plist` i `project.pbxproj` amb `plutil -lint`: **PASS**.
4. Comparació byte a byte amb el ZIP base: només han canviat `AgendaIosScreen.kt` i `IosAgendaApi.kt`, més els fitxers nous; el codi d'Android és idèntic.
5. Validació del contracte de report per inspecció local de `events/report.php`: codis de motius, `guest_id` i camps d'esdeveniment. No verifica la resposta real del servidor de producció.
6. Integritat CRC del nou ZIP: indicada després de la generació.

## Llista de completitud

| Flux | Situació |
| --- | --- |
| Ocultar esdeveniment/artista/categoria al feed i recuperar-ho | **Implementat en codi**, pendent de compilació i prova iOS |
| Respectar la publicitat obligatòria en les exclusions | **Implementat en codi** i regla pura provada; pendent de prova amb campanyes reals |
| Informar d'un esdeveniment des del feed | **Implementat en codi** i contracte PHP contrastat; pendent de prova de xarxa real |
| Llistes de Guardats / invitacions i gestió de perfil professional | Ja implementats en passades anteriors; pendents de compilació i prova iOS |
| Notificacions locals, batecs i cercador | Parcialment implementats; pendents de paritat i de proves |
| Pagaments de pagament, APNs, eliminació de compte, Sign in with Apple, AgendaPRO integral | **Pendents** de backend, credencials, desenvolupament i/o validació |
| Compilació completa d'iOS amb Xcode | **No verificada** |

**Funcionalitats completades i verificades en un iPhone: 0** (encara no s'ha executat Xcode). No s'ha modificat el servidor ni cap base de dades.

## Com compilar

En un Mac amb Xcode, descomprimix el projecte, entra a `ANDROID_STUDIO` i executa:

```sh
sh COMPROVACIONS/compilar_ios_mac.sh
```

La prova completa amb Xcode és imprescindible per validar compatibilitat Kotlin/Native i les interaccions Compose.
