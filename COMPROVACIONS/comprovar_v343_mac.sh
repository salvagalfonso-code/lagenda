#!/bin/sh
# Comprovacions prèvies a compilar iOS; no equivalen a una compilació conjunta.
set -eu
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
cd "$ROOT"
command -v swiftc >/dev/null 2>&1 || { echo 'Falta swiftc'; exit 2; }
command -v plutil >/dev/null 2>&1 || { echo 'Falta plutil'; exit 2; }
command -v shasum >/dev/null 2>&1 || { echo 'Falta shasum'; exit 2; }
echo '1/4: Fonts d’Android originals'
shasum -a 256 -c COMPROVACIONS/android_v341_font_hashes.sha256 > /dev/null
echo '2/4: Sintaxi Swift'
swiftc -frontend -parse iosApp/AgendaIOS/*.swift
echo '3/4: Configuració Apple'
plutil -lint iosApp/AgendaIOS/Info.plist iosApp/AgendaIOS.xcodeproj/project.pbxproj
echo '4/4: Interfície de confirmació de recordatoris'
grep -q 'interface IosReminderSchedulingCallback' shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosAgendaApi.kt
grep -q 'func scheduleLocalReminderConfirmed' iosApp/AgendaIOS/AgendaKeychain.swift
printf '\nOK: comprovacions locals v343. Falta executar COMPROVACIONS/compilar_ios_mac.sh amb Xcode.\n'
