# L’Agenda iOS v343 — Correspondència visual de Guardats

## Punt de partida
Projecte acumulat `LAGENDA_V343_KMP_IOS_CERCADOR_AVANCAT_ACUMULADA.zip`.
Canvis exclusivament en `shared` i el pont Swift de `iosApp`; `app/src` Android conservat byte a byte.
No hi ha canvis a la base de dades ni desplegament del servidor.

## Implementat en codi en aquesta passada

1. `Guardats` replica les **dues pestanyes** d'Android: `Guardats` i `Recordatoris` (la gestió de llistes ja no és una tercera pestanya).
2. A `Guardats`, selector **General / llistes pròpies / llistes compartides** i botó `+ Nova llista`.
3. `General` agrupa **tots** els esdeveniments guardats i els de les llistes, sense filtres del feed ni finestra de set dies.
4. **Targetes compactes** amb cartell, títol, data, hora i població, ordenades cronològicament.
5. Inclou **actuacions individuals guardades de festivals** (caché local per compte), intercalades per data amb les activitats; permet obrir l'horari enfocant l'artista i llevar una actuació amb confirmació.
6. `Recordatoris`: només esdeveniments amb fitxa disponible i data vigent, cartell, data/hora configurada i campaneta per desactivar (confirmació ja existent a la pantalla principal).
7. Gestió integrada de llistes: crear, unir-se amb codi o enllaç, seleccionar, canviar nom, afegir i llevar activitats, compartir, eliminar pròpies i abandonar compartides. Respecta el rol de propietari que retorna l'API.
8. **Compartició amb full natiu d'iOS** (`UIActivityViewController`), sense copiar manualment l'enllaç.
9. Cap spinner nou: la llista es dibuixa a partir de dades ja disponibles i les mutacions es fan en segon pla.

## Proves realitzades
- Regles Kotlin `IosSavedAgendaPolicy` compilades independentment; sis casos de selecció, vigència i descodificació d'actuacions han passat.
- Anàlisi sintàctica Swift amb `swiftc -frontend -parse`: correcta.
- Integritat ZIP i preservació dels **91 fitxers originals d’Android**: correcta.
- La pantalla antiga `SavedListsPanel` continua definida per compatibilitat, però ja no s'invoca.

## Limitacions i verificació pendent
- No s'ha pogut executar **Gradle complet ni Xcode** en este entorn; la interfície encara no està validada en simulador o iPhone.
- Els horaris individuals de festivals són **locals al dispositiu**: l'API actual no sincronitza eixa selecció entre iPhone/Android.
- Les fitxes que falten al catàleg de l'iPhone s'oculten temporalment; no s'esborren els guardats ni els avisos del servidor. Cal provar recuperació de fitxes antigues fora del catàleg descarregat.
- Les proporcions, tipus de lletra i marges s'han aproximat al codi Android, però cal revisió visual en simulador per confirmar equivalència exacta.

## Instruccions de compilació
Des d'un Mac amb Xcode, en `ANDROID_STUDIO`:

```sh
sh COMPROVACIONS/compilar_ios_mac.sh
```

Inspeccionar `IosSavedAgendaScreen.kt`, `IosSavedAgendaPolicy.kt`, `AgendaIosScreen.kt` i `AgendaKeychain.swift` en cas d'error; adjuntar el missatge exacte de compilació per corregir-lo.
