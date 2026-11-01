# Pas 21/48 — Batecs: visor, gestos i paritat visual amb Android v336

## Implementació en codi, acumulada sobre passos 1–20

- **Ordre estable del visor:** en obrir un cercle es captura en RAM la llista dels sis grups, les publicacions i l'ID d'usuari. La recepció asíncrona d'un `seen` pot reordenar el carrusel en el feed però NO canviar el següent artista dins del visor. En canviar de compte es tanca i s'esborra la captura.
- **Navegació d'Android:** mitat esquerra de la pantalla → anterior; mitat dreta → següent; lliscar cap a l'esquerra/dreta amb llindar de 56dp; tocar anterior al primer grup reinicia la barra de 15s; avançar després de l'últim tanca. Els concerts accepten desplaçament vertical.
- **Retorn al principi:** tancar el visor incrementa `batecsReturnEpoch`; tant el carrusel com el feed tornen a l'índex zero. El carrusel continua ancorat al principi davant d'una actualització de vistos fins que l'usuari el torna a tocar amb el dit, com `StoriesRow` d'Android v336.
- **Cercles:** 54dp, ample de 64dp, anell multicolor `sweepGradient` d'Android v336, foto retallada, filtre gris de saturació zero en vistos, paleta gris i text apagat. Un cercle per artista seguit, comptador si té diverses publicacions, distintiu blau de seguit i inicials quan no hi ha foto. Les fotos NO es guarden en preferències personals locals.
- **Visor:** relació 9:16, paleta de fons determinista per artista d'Android v336, imatge si existix, capa negra semitransparent, una barra de 15s, pròximes actuacions i obertura de la fitxa original. Botó de seguir/deixar de seguir reutilitza el sistema de seguiments servidor del pas 10.
- **Vistos:** es mantenen les crides del pas 20 a `batecs/seen.php` per a convidats i registrats i la cua xifrada; tancar el visor no buida la cua.
- **Sense modificar Android ni servidor.** Només `AgendaIosScreen.kt`, `IosBatecNavigationPolicy.kt`, test i comprovacions/documentació d'iOS.

## Comprovacions executades

1. **16 casos Kotlin/JVM reals** sobre `IosBatecNavigationPolicy.kt` (navegació, primer/últim, tocs, llindars de gest i indexació). PASS.
2. **21 scripts de regressió estàtica** `comprovar_pas_01.py` a `comprovar_pas_21.py`. PASS; inclou comprovació byte a byte de l'Android v336 i de la còpia Android encapsulada en v349.
3. Validació del ZIP complet 1–21 (integritat ZIP, fitxers obligatoris i Android intacte). PASS quan s'executa l'empaquetador.
4. **Compilació completa Kotlin/Native i simulador iPhone:** NO realitzada. El wrapper del projecte necessita descarregar Gradle i l'entorn no resol `services.gradle.org`; no hi ha Xcode/macOS en el contenidor.
5. **Proves tàctils reals d'iPhone i PHP/MariaDB:** NO realitzades. Les proves estàtiques i l'execució Kotlin/JVM no equivalen a una compilació ni a paritat visual certificada en dispositiu.

## Limitacions ja existents, no resoltes en este pas

- El servidor de vistos accepta `event:ID` i no distingix els diversos artistes participants en el mateix esdeveniment.
- Android v336 encara té les seleccions individuals de festivals en `SharedPreferences`, no persistides en servidor. No es garanteix la paritat d'eixes seleccions mentre Android estiga congelat.
- La paritat del motor «Per a tu» no és absoluta quan no existix snapshot signat d'Android i s'utilitza el motor alternatiu PHP.

Pròxim pas: **22/48 — cercador de grups, activitats i actuacions dins de festivals**. Pròxim ZIP: pas 24.
