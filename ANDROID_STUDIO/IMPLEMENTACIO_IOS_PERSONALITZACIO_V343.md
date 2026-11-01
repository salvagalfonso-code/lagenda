# L’Agenda · v343 – Passada de personalització de «Per a tu» per a iOS

## Implementat en codi (pendent de compilació conjunta en Xcode)

- Catàleg geogràfic integrat de 6.013 llocs amb coordenades vàlides, extret de l'actiu territorial original Android v277 (font original conté 6.016 registres).
- Afinitat separada: actuacions d'artistes/organitzadors/sales seguits arreu; estils derivats dels esdeveniments dels artistes seguits dins del radi preferit; preferències culturals i geogràfiques fins a 100 km.
- Selecció de comarques, poblacions i radi per població de 5/15/30/50/100 km.
- Selecció d'estils/subcategories d'activitats presents al catàleg, prioritat del valencià i suggerències opcionals basades en activitats obertes els últims 15 dies. Les cerques de pobles no compten com a gustos.
- Manté la caché ordenada de 30 minuts, també quan la selecció orgànica està buida; canviar preferències força la invalidació.
- Prioritat comercial garantida en «Per a tu», «Sense filtre» i cerques, inclosos anuncis i venda interna. Una única fitxa per esdeveniment/jornada.
- Preferències i historial local separats per usuari. Android es manté intacte.

## Comprovat localment

- Compilació independent del motor Kotlin de geografia i personalització amb `kotlinc` i execució de 11 casos sobre municipis reals.
- Compilació independent del codi de caché i historial amb dobles de l'API, i 8 casos d'invalidació/reutilització.
- **No** s'ha compilat el mòdul Compose ni el binari iOS. Les proves no acrediten execució en iPhone.

## Encara no completat

- Paritat 1:1 de subcategories amb tota la taxonomia i les metadades d'entitats d'Android: iOS calcula l'estil de seguits a partir dels actes presents en el catàleg, mentre Android també consulta metadades d'entitats.
- Sincronització entre dispositius de preferències: els valors nous d'iOS es guarden per compte només al dispositiu.
- Aprenentatge de cerques i altres interaccions: esta passada utilitza obertures explícites d'esdeveniments; no afirma tindre el model complet Android.
- Desplegament o integració de pagaments reals, APNs, eliminació de comptes i resta de blocs incomplets segons l'auditoria anterior.
- Compilació Xcode, simulador, iPhone real i verificació d'API en producció.

## Com compilar

Des de macOS: `cd ANDROID_STUDIO && sh COMPROVACIONS/compilar_ios_mac.sh`.
La versió de projecte es manté com **v343**.
