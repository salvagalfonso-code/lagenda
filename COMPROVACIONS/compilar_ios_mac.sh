#!/bin/sh
# Executar des de Terminal en un Mac amb Xcode complet instal·lat.
set -eu
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
cd "$ROOT"
command -v java >/dev/null 2>&1 || { echo 'ERROR: cal JDK 17 o superior'; exit 2; }
command -v xcodebuild >/dev/null 2>&1 || { echo 'ERROR: cal Xcode (no només Command Line Tools)'; exit 2; }
sh ./COMPROVACIONS/preflight_ios_v341.sh
if [ "$(uname -m)" != "arm64" ]; then
  echo 'ERROR: aquesta configuració del simulador necessita un Mac Apple Silicon (arm64).' >&2
  exit 2
fi
TARGET='linkDebugFrameworkIosSimulatorArm64'
printf '\n1/2: Preparant framework Kotlin (%s)\n' "$TARGET"
sh ./gradlew ":shared:$TARGET" --stacktrace
printf '\n2/2: Compilant aplicació per al simulador iOS\n'
xcodebuild -project iosApp/AgendaIOS.xcodeproj -scheme AgendaIOS \
  -configuration Debug -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' \
  CODE_SIGNING_ALLOWED=NO build
printf '\nCompilació iOS per al simulador completada.\n'
