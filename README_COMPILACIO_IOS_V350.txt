L’AGENDA — V350 ACUMULADA — CODI FONT PER A PROVA DE COMPILACIO IOS
Data: 29/10/2026

CONTINGUT
- ANDROID_STUDIO/: projecte Kotlin Multiplatform amb Android original i canvis iOS acumulats.
- codemagic.yaml: workflow de compilacio ios_simulator.
- SERVIDOR_*: fitxers complementaris de servidor, NO desplegats per este ZIP.
- INFORME_PAS_* i ANDROID_STUDIO/COMPROVACIONS/: auditories i comprovacions locals.

COMPILACIO CODEMAGIC
1. Puja el ZIP a un repositori compatible amb Codemagic (descomprimint-ne el contingut a l'arrel).
2. Selecciona el workflow 'ios_simulator' definit a codemagic.yaml.
3. Revisa el registre 'artifacts/xcodebuild.log' si la compilacio falla.
4. Si compila, el resultat sera 'artifacts/lagenda-ios-simulator.zip'. Es una APP de simulador, no un IPA instal·lable a un iPhone fisic.

ESTAT DE VERIFICACIO
- Comprovacions locals superades: bridge Kotlin/Swift (17 metodes), sintaxi Swift, XML del plist i 6 proves smoke de politiques.
- Els 93 fitxers Android originals del ZIP v349 tenen SHA256 identic.
- NO verificat: compilacio Xcode/Kotlin Native completa, instal·lacio/arrancada al simulador ni proves en iPhone real.
- Alguns fluxos professionals, pagaments, autenticacio social i notificacions APNs continuen incomplets o necessiten proves i/o servidor desplegat.
- Els ajustos de perfil del servidor complementari requereixen desplegament independent i validacio contra el servidor real.

VERSIO
- Es manté la numeracio v350, sense increment.
