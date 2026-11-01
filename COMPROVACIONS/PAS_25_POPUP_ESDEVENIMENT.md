# Pas 25/48 · Popup d'esdeveniment · iOS v349

**Referència:** Android v336, intacte. **Base:** iOS v349 acumulat passos 1–24.

## Canvis aplicats a iOS

- Fitxes no festival: quatre pestanyes **Info, Entrades, Ubicació, Comunitat** en este ordre; inicialment Info. Els artistes i seguiments es mostren dins Info en lloc d'ocupar una pestanya addicional.
- Festivals: es conserva **Horaris**, amb les pestanyes Info, Entrades, Ubicació i Comunitat (la paritat per dies/horaris es continua en pas 26). Barra horitzontal si no caben.
- Capçalera de la fitxa: cartell, títol, data, lloc, accions de guardar, campaneta, compartir, visor de cartell, denúncia, artistes i gestió de Batecs existents conservades.
- **Entrades:** lectura de `tickets/options.php?event_id=...` exclusivament si la venda és interna; mostra tipus, preu, disponibilitat i sessió retornats pel servidor. Les entrades externes només s'obrin amb URL HTTPS; no es simulen passarel·les de pagament. El botó continua obrint el diàleg existent de selecció o l'enllaç extern.
- **Ubicació:** mostra sala, població i comarca si hi són; seguiment de sala mitjançant el flux de `user-state/follow.php` existent i itinerari via la cerca explícita de Mapes d'Apple. No inventa coordenades.
- **Comunitat:** `community/groups.php?event_id=...` carregat dins del popup, amb els cinc tipus de tauló, estat obert/aprovació, descripció i membres. Obrir un tauló mostra el xat real; crear tauló utilitza `community/create.php`. Convidats amb `guestReadyUserId` tenen les mateixes operacions autoritzades pel PHP (el servidor decideix els permisos). Lectures, errors, reintents i dades en RAM separades per esdeveniment/usuari.
- El hub general de Comunitat deixa de bloquejar els convidats que tenen identitat de servidor, sense modificar autenticació PHP ni Android.
- Nou mòdul de comportament `IosEventDetailContract.kt` i prova comuna `IosEventDetailContractTest.kt`.

## Validacions disponibles

- 25 de 25 scripts estàtics acumulats PASS (`COMPROVACIONS/comprovar_pas_01.py` ... `comprovar_pas_25.py`).
- Mòdul `IosEventDetailContract.kt` compilat amb Kotlin/JVM i 13 comprovacions de comportament PASS.
- `swiftc -frontend -parse` de Swift PASS.
- Original Android v336 i fitxers d'Android embeguts en v349: comparació SHA-256 amb ZIP font PASS, sense canvis.

## No certificat

- **No** compilació completa Kotlin Multiplatform / Xcode ni verificació de la pantalla sobre iPhone; l'entorn no té el wrapper Gradle executable / Xcode operatiu.
- **No** prova amb servidor PHP i MariaDB en viu. Els endpoints es comparen amb els fitxers PHP entregats.
- Pagaments d'entrades de preu i funcionalitat avançada de Comunitat no es consideren tancats en este pas; tenen passos específics del pla.
- La referència Android v336 utilitza un panell desplegable amb botons Artistes/Entrades/Info/Comunitat; l'especificació del projecte per a la fitxa no festival exigeix Info/Entrades/Ubicació/Comunitat. S'han conservat artistes dins Info i s'ha afegit la pestanya Ubicació, sense modificar Android.

**No s'ha generat cap ZIP:** lliurament acumulatiu següent en completar pas 27.
