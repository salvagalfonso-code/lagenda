# Personalització iOS — registre de canvis acumulats v350

Referència: les cinc captures d'Android v338 aportades el 29/10/2026 i `app/src/main/java/com/example/agendavalenciana/ui/AgendaApp.kt` (funció de personalització i estructura de categories).

## Implementat en codi iOS

- Reemplaçat l'antic formulari únic d'iOS per una pantalla modal completa, amb capçalera, retorn i cinc pestanyes fixes: Cats, Artistes, Llocs, Bloq. i Prefs.
- Categories: 11 categories principals, 55 subcategories oficials (amb els codis i àlies exactes d'Android), infantil transversal; cercador, desplegable, seguiment complet o parcial, menú de bloqueig i restauració.
- Artistes: cercador, seguits primer, càrrega incremental de 20, seguiment i opció de campaneta, menú per bloquejar; les fotos vénen de l'API `entities/images.php` i no de cartells aleatoris.
- Llocs: pobles i comarques, suggeriments, afegir/eliminar, radi individual 5/15/30/50/100 km i sincronització amb `preferences/locations.php` quan hi ha sessió.
- Bloquejos: categories (incloses subcategories), artistes, esdeveniments, restauració i sincronització amb `visibility/index.php`; comercialment garantits mai no s'oculten. En festivals amb diversos artistes un artista bloquejat no oculta tot el festival.
- Preferències: prioritzar valencià, suggerències ON/OFF, nivell de descoberta condicional, reinici d'afinitats; en iOS sol·licitud de permís de notificacions a través del pont natiu.
- Seguiments de categories amb `user-state/follow.php`, com Android; persistència local per compte. Seguiments d'artistes amb `user-state/follow.php`; campanetes amb `notifications/artist-bell.php`.
- Millorada la selecció del feed: les categories com MUSIC corresponen a etiquetes com Música i les subcategories reconeixen els àlies editorials exactes.
- L'actualització dels llocs protegix contra respostes antigues de servidor; els canvis de visibilitat es guarden localment quan la xarxa falla i es reintenten.

## Verificat localment

- 13 comprovacions de la taxonomia i semàntica de seguiment (Kotlin JVM, amb dades reals).
- 8 comprovacions del filtratge per categoria i subcategoria (Kotlin JVM, utilitzant stubs només per a les dependències de xarxa/geografia).
- 6 comprovacions addicionals de les coincidències exactes d'àlies.
- 93/93 fitxers originals de `ANDROID_STUDIO/app/` idèntics a la v350 inicial (SHA256).

## No verificat / bloquejos

- No s'ha compilat el projecte complet ni s'ha provat a Xcode, Codemagic o iPhone. No declarar equivalència visual pixel a pixel fins a veure una captura d'iOS.
- La sincronització de categories, visibilitat, localitats i campanetes depén de rutes del servidor Android; la resposta real i els permisos no s'han provat.
- `affinities/reset-learning.php`: sol·licitud integrada, però el servidor no ha confirmat que la ruta funcione.
- Desactivar les notificacions del sistema requerix els ajustos d'iOS; la preferència local no garantix el bloqueig de notificacions remotes fins que el servidor gestione l'opció.
- Les fotos de categoria/artista/poble depenen del catàleg canònic remot; quan falten es mostren inicials.
- Preferències de suggerències / valencià / descoberta es guarden per compte al dispositiu, però encara falta verificar la seua sincronització entre Android i iOS.

Nom de versió preservat: **v350**. Cap ZIP nou generat en este pas.
