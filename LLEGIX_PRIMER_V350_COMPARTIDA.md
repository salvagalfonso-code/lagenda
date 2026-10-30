# L’Agenda iOS/Android v350 — paquet acumulat · 30/10/2026

El ZIP porta `shared/`, `iosApp/`, `app/`, `gradlew`, `gradle/` i `codemagic.yaml` a l’arrel. Substituïx el contingut del repositori que utilitza Codemagic: no l’afegisques dins d’una carpeta `ANDROID_STUDIO/` addicional.

Inclou el motor PHP **nou** (`SERVIDOR_PER_A_TU/api/for-you/feed.php` i `_engine.php`), les rutes de preferències, els SQL i els tests. El backend NO queda desplegat per pujar el codi a Codemagic: ha d’instal·lar-se al servidor existent i verificar-se l’esquema SQL i l’autenticació.

IMPORTANT: `api/for-you/index.php` de producció continua sent la caché de versions antigues; no s’ha de substituir ni eliminar.

Les correccions de recuperació de preferències en iniciar sessió i de visualització de Personalització estan integrades directament en `shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/AgendaIosScreen.kt`.

Proves verificades: 13 comprovacions estàtiques de preferències, 19 del formulari, 14 de la sincronització, 8 casos del motor PHP, verificació de 18 mètodes pont Swift, validació sintàctica PHP/Swift, 6 smoke tests Kotlin/JVM. Aquestes proves NO equivalen a compilació nativa de Kotlin/Native, funcionament real en iPhone, compatibilitat amb MariaDB de producció ni paritat completa Android/iOS.

Configuració de Codemagic: workflow `ios_simulator` (compila per a simulador, no iPhone físic).

Per desplegar el backend: copiar només els fitxers de `SERVIDOR_PER_A_TU/api/` a les rutes equivalents dins del servidor (respectant l'API existent), i aplicar `SERVIDOR_PER_A_TU/sql/*.sql` després de còpia de seguretat i comprovació d'esquema.
