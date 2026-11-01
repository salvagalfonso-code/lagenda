#!/bin/sh
# Diagnòstic previ, no desplega res ni requereix compte Apple Developer.
set -eu
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
cd "$ROOT"
echo '== Entorn iOS =='
command -v xcodebuild >/dev/null 2>&1 || { echo 'ERROR: Xcode complet no trobat'; exit 2; }
command -v java >/dev/null 2>&1 || { echo 'ERROR: JDK 17+ no trobat'; exit 2; }
command -v swiftc >/dev/null 2>&1 || { echo 'ERROR: Swift no trobat'; exit 2; }
command -v plutil >/dev/null 2>&1 || { echo 'ERROR: plutil no trobat'; exit 2; }
xcodebuild -version
java -version
xcrun --sdk iphonesimulator --show-sdk-path >/dev/null
plutil -lint iosApp/AgendaIOS/Info.plist iosApp/AgendaIOS.xcodeproj/project.pbxproj
swiftc -frontend -parse iosApp/AgendaIOS/*.swift
echo 'OK: sintaxi Swift, plist, Xcode i SDK disponibles'
echo 'La comprovació NO valida el tipatge de Swift ni la vinculació del framework Kotlin.'
