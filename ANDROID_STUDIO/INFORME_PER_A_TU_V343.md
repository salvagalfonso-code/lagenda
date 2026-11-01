# L’Agenda v343 — continuació de «Per a tu» a iPhone

## Implementat en el codi en esta passada

1. **Catàleg d'estils idèntic al d'Android**: còpia generada del fitxer original `style_affinity_catalog_v3.json`, amb 792 àlies, equivalències i relacions de proximitat. No modifica les fonts d'Android.
2. **Metadades de les entitats d'artistes**: lectura del contracte existent `entities/images.php`. L'app pren primer els estils/subcategories declarats en l'entitat seguida; únicament fa servir els estils dels esdeveniments si falta metadata d'aquell artista.
3. **Caché de metadades**: dades públiques persistents, 30 minuts; una errada de xarxa no elimina l'última resposta vàlida.
4. **Senyals de cerques**: quan es confirma la cerca —botó Cercar o acció Cerca del teclat—, els resultats coincidents per nom d'artista o títol poden afegir senyals d'estil. No s'aprén en teclejar ni d'una cerca només per població.
5. **Pes i vigència de suggerències**: les coincidències repetides guanyen pes, fins a 120 registres locals, per compte, amb un termini de 15 dies. Només intervenen quan les suggerències estan activades.
6. **Càlcul sense bloquejar Compose**: selecció personalitzada en `Dispatchers.Default`, en segon pla i amb conservació de la selecció del mateix compte mentre arriba la nova.
7. **Reutilització de caché de 30 minuts**: continuen les invalidacions per canvis importants de dades, seguiments, preferències i metadades; un m'agrada o guardar una fitxa no obliga a recalcular.
8. **Publicitat protegida**: es conserva el comportament ja existent de mostrar continguts comercials amb campanya activa o venda interna, fins i tot amb exclusions, sense duplicats.

## Comprovacions efectuades

- Compilació local de la selecció pura Kotlin, el mòdul d'interessos i el catàleg d'estils contra simuladors de les dependències (no és una compilació Kotlin Multiplatform completa).
- Casos de prova: prioritat de l'artista seguit, metadades vs fallback, garantia comercial, exclusió de cerques només de lloc, reiteració de cerques, separació per comptes, expiració de 15 dies.
- Comparació exacta de l'asset d'Android i el catàleg iOS generat.
- Anàlisi de sintaxi Swift de les fonts de l'app.
- Verificació de l'integritat del ZIP i de la preservació del codi Android.

**No s'ha executat una compilació completa amb Gradle i Xcode ni s'ha provat en un iPhone.** Tampoc s'ha fet cap canvi al servidor de producció.

## Mancances específiques que queden a «Per a tu»

- Paritat comprovada dels punts i de les regles d'afinitat de l'Android per a tots els tipus d'entitats (incloses coedicions i els mapatges canònics de taxonomia).
- Sincronització real de preferències i historial d'interessos entre dispositius.
- Verificació dels camps i metadades d'estils contra respostes **reals** de la instal·lació de producció.
- Comprovació en iPhone de la caché de 30 minuts, flux offline i temps fins a la primera targeta.
- Reproducció exacta de les posicions de publicitat, la rotació i els blocs Premium/HEADER.

## Altres mancances generals d'iPhone (fora del feed)

- «Sense filtre»: paritat visual i de festivals, optimització completa i proves reals.
- Cercador avançat: suggeriments de pobles/comarques, radis, filtres i cerca global d'artistes.
- Batecs: paritat de gestos, carrusel, vistos i navegació de retorn.
- Popups d'esdeveniment, artista i festival: totes les pestanyes, horaris, accions, productes i modalitats.
- Guardats: paritat d'interfície, llistes, invitacions i recordatoris complets.
- Entrades: compres de pagament segures amb Redsys o Stripe, devolucions i conciliació; proves de reserva i QR reals.
- Comunitat: bloquejos d'usuaris, taulons avançats, moderació i notificacions de missatges.
- Notificacions: sis categories amb dades d'origen reals, APNs, registres de dispositius, freqüències i enviaments externs.
- Perfil i identitat: fotografia, inici de sessió amb Apple/Google, baixa de compte i configuració de privacitat.
- AgendaPRO i Superadmin: gestió professional, publicació, taquilles, estadístiques, campanyes push i permisos.
- Accessibilitat, persistència, fluxos offline, seguretat, Xcode, simulador, iPhone físic, TestFlight i App Store.

## Estat honest

- **Implementat en codi en esta passada**: les huit millores anteriors.
- **Implementació parcial**: el bloc complet «Per a tu» encara no té paritat verificada amb Android.
- **Funcionalitats verificades en un iPhone**: 0, perquè encara no hem fet la compilació i prova.
