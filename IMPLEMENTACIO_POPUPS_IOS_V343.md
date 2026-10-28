# L’Agenda iOS v343 — Tancament funcional de popups (precompilació)

**Base:** `LAGENDA_V343_KMP_IOS_PUBLICITAT_ACUMULADA.zip`. No s’ha modificat `app/src` d’Android ni el servidor.

## Funcions implementades en codi en esta passada

- Popup **esdeveniment**: quatre pestanyes fixes Info, Entrades, Ubicació i Comunitat; accions de Guardar, Recordatori, seguir sala/organitzador/categoria, consultar entrades, informar d’un problema i obrir Mapes externament en tocar el botó.
- Popup **festival**: Horaris, Info, Ubicació i Comunitat; Entrades com a acció separada de la capçalera. Sense selector de dia quan només existix una jornada real.
- Horaris: consulta no bloquejant del mateix endpoint `special-events/public.php?event_id=` que utilitza Android; recupera artistes, dates, hores i escenaris; no inventa dies del rang. Si la consulta falla, manté l’horari local del feed.
- Cerca d’artista en festival: obri Horaris, selecciona la jornada del resultat i col·loca destacada l’actuació coincident, sense text «Actuació buscada».
- Horaris individuals: desar i retirar actuacions concretes del festival, per esdeveniment i compte, persistint en la caché local d’iOS. **No es sincronitzen entre dispositius**.
- Popup **artista**: disciplina en la cantonada superior dreta de la imatge, seguir just davall, enllaços de cerca a plataformes musicals en format compacte, concerts i activitats i al final la secció Productes.
- Les plataformes musicals obrin **cerques** per artista; no es presenten com a perfils oficials.
- El botó «Cartell» en la imatge té fons semitransparent i presenta el cartell ampliat.
- Evita superposar diàlegs del concert amb el popup d’artista o la compra integrada.

## Comprovacions locals

- 91 fitxers `ANDROID_STUDIO/app/src` comparats amb l’entrada: 91 idèntics.
- Sintaxi dels fitxers Swift comprovada amb `swiftc -frontend -parse`.
- Claus i parèntesis dels fitxers Kotlin modificats comprovats estructuralment.
- Afegits tests Kotlin de desament d’horaris, dies reals i contracte `special-events/public.php`.
- Integritat de ZIP verificada.

**No s’ha completat una compilació KMP/Xcode ni s’han executat els tests Gradle en este entorn.**

## Què continua pendent dins dels popups

1. Imatges pròpies d’artista, biografia i dades professionals verificades: falta traslladar les mateixes consultes de perfils i entitats d’Android.
2. Productes reals de cada artista: la secció no inventa cap producte perquè iOS encara no consulta el catàleg de marxandatge del servidor.
3. Campaneta d’artista amb freqüències i lliurament real de notificacions: falta la persistència de freqüències i APNs; no s’ha afegit un botó que fingisca programar un push.
4. Entrades de pagament dins d’iOS: continua pendent la integració i confirmació segura amb Redsys o Stripe.
5. Guardar actuacions del festival amb sincronització entre dispositius: requerix una API de servidor real; ara és local.
6. Paritat gràfica exacta, gestos, accessibilitat i verificació de la navegació en simulador/terminal iPhone.

## Compilació

Descomprimix el projecte i executa des de macOS dins `ANDROID_STUDIO`:

```bash
sh COMPROVACIONS/compilar_ios_mac.sh
```
