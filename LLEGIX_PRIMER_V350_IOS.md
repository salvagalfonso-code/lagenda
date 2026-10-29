# L’Agenda v350 — revisió iOS del 29/10/2026

Base: `LAGENDA_V349_IOS_PANTALLA_COMPLETA_FEED_SCROLL (1).zip`.

**Este ZIP és una revisió de codi i una candidata per a compilació, no una IPA provada ni una versió certificada per a l'App Store.** Android (`app/`) es manté byte a byte com a v349. L'informe de 98 punts està en `COMPROVACIONS/AUDITORIA_FUNCIONAL_IOS_V350.md`.

Canvis iOS: rutes d'avís cap a la fitxa en tocar una notificació, actualització del dia en segon pla mentre l'app està oberta, correcció de sessions concurrents en guardar/refrescar, neteja dels avisos locals en tancar sessió, aprofitament del PHP existent de gestió d'usuaris, APNs per entorn Debug/Release, permisos de notificacions contextuals.

Dos PHP nous estan en **un paquet separat** i requerixen desplegament/pruebes per a funcionar. Els nou endpoints restants encara no apareixen en els fitxers del servidor rebuts. No s'ha canviat la MariaDB de producció i la venda d'entrades de pagament no està implementada a iOS.

## Proves locals

`bash COMPROVACIONS/verificar_v350.sh` (Swift parser, Kotlin JVM 6 tests, estructura XML i pont de 17 mètodes). No comprova Kotlin/Native o tipatge Swift amb SDK Apple.

## Compilació real

Puixar la carpeta `ANDROID_STUDIO` a Codemagic, workflow `ios_simulator` en màquina Apple Silicon, o executar en Mac:

```sh
cd ANDROID_STUDIO
sh COMPROVACIONS/compilar_ios_mac.sh
```

En cas d'error, recuperar el primer bloc de `artifacts/xcodebuild.log`, incloent fitxer i línia, i corregir-lo abans de dir que la versió compila. Una build de simulador no equival a una IPA firmada per a dispositius. Per a distribució cal equip Apple Developer, signing i TestFlight.
