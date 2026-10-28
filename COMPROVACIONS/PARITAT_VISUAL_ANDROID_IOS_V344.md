# L'AGENDA v344 · paritat visual Android / iPhone

**Font de veritat visual:** captures Android facilitades per l'usuari el 28/10/2026 (incloses a `REFERENCIES_VISUALS_ANDROID/` a l'arrel del ZIP). Les captures corresponen a Inici, popup, Guardats i Notificacions. L'objectiu és reproduir el mateix disseny, no fer una reinterpretació SwiftUI d'iOS.

## IMPLEMENTAT EN CODI (NO VERIFICAT A L'IPHONE)
- Logotip horitzontal original en `composeResources`, no tipografia substitutiva.
- Capçalera simètrica amb perfil, afegir, ajustos i campaneta; icones dibuixades i no emojis.
- Cercador en primera posició, botó Opcions, carrusel publicitari de capçalera de mida proporcional a la pantalla; Batecs a continuació i modes del feed abans de les targetes.
- Targeta compacta de feed amb cartell, títol, lloc, hora, preu, data, guardat i menú; càrrega progressiva mantinguda.
- Popup d'esdeveniment de tot l'ample, imatge de capçalera amb gradient, títol sobre el cartell, campaneta/compartir/tancar, botons Guardar/Entrades, pestanyes Artistes/Info/Ubicació/Comunitat. Data visual DD/MM/YYYY.
- Guardats amb capçalera «Les meues agendes», dos selectors estil Android, selector de llistes, botó «+ Nova llista» i targetes compactes amb cartell i metadades.
- Notificacions amb diàleg quasi complet, categories horitzontals, llistes amb miniatures reals i comptadors.
- Barra inferior estil Android amb quatre entrades i pestanya seleccionada.
- Mantingudes les funcionalitats KMP de la versió 343, API, permisos, pagaments, publicitat i lògiques existents.

## NO COMPILAT NI VERIFICAT EN IPHONE
- Compilació `xcodebuild` o Codemagic: NO realitzada en aquest entorn (sense macOS/Xcode).
- Captures comparatives del mateix esdeveniment Android/iOS: NO realitzades.
- Validació de dimensions en dispositius iPhone: PENDENT.
- Fonts exactes, trencaments de línia, notch i àrees segures: PENDENT.
- Navegació, tots els gestos i clics amb API en iPhone real: PENDENT.

## LIMITACIONS CONEGUDES
- L'original Android és una UI Compose Android independent; la UI d'iPhone encara és una implementació KMP que ha de contrastar-se pantalla per pantalla.
- Alguns components no replicats píxel a píxel: dades dels Batecs/avatars, notificacions amb neteja massiva (el servidor iOS només té `markRead`), botons d'artistes amb foto pròpia quan el catàleg no la retorna.
- `Guardats` en iOS continua requerint sessió, segons la implementació existent, mentre que Android admet altres fluxos locals. No s'anuncia paritat funcional absoluta.
- El fet que hi haja codi no acredita que puga iniciar-se sense errors fins que no es faça una compilació i una prova d'obertura.

## PROVA DE RECEPCIÓ OBLIGATÒRIA
1. Compilar i instal·lar v344 en simulador iPhone (amb llibreries KMP disponibles).
2. Obrir Inici/Guardats/Notificacions/popup amb el mateix compte i dades que a Android.
3. Capturar la mateixa mida relativa i comparar posicions i dimensions respecte dels originals.
4. Corregir diferències per pantalla, i després verificar al dispositiu real.
