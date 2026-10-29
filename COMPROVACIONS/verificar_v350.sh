#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
from xml.etree import ElementTree as ET
p = Path('iosApp/AgendaIOS/Info.plist')
assert '<string>350</string>' in p.read_text()
for x in (p, Path('iosApp/AgendaIOS/AgendaIOS.entitlements')): ET.parse(x)
a = Path('shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosAgendaApi.kt').read_text()
s = Path('iosApp/AgendaIOS/AgendaKeychain.swift').read_text() + Path('iosApp/AgendaIOS/AgendaProfileNative.swift').read_text()
import re
interface = a.split('interface IosSessionBridge',1)[1].split('interface IosSocialAuthCallback',1)[0]
methods = re.findall(r'\bfun\s+(\w+)\(',interface)
missing = [m for m in methods if not re.search(r'\bfunc\s+'+re.escape(m)+r'\(',s)]
assert not missing, 'Missing native bridge methods: '+str(missing)
assert 'admin/user-update.php' in a and 'admin/user-access-ios.php' not in a
assert 'cancelAllLocalReminders(it)' in Path('shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/AgendaIosScreen.kt').read_text()
print(f'OK: {len(methods)} native bridge methods, plist XML, admin route, cleanup, build 350')
PY
if command -v swiftc >/dev/null; then
  swiftc -frontend -parse iosApp/AgendaIOS/*.swift
  echo 'OK: Swift frontend parse (no typecheck or Xcode link)'
fi
if command -v kotlinc >/dev/null && command -v java >/dev/null; then
  TEMP_JAR="$(mktemp -t agenda_v350_tests_XXXXXX.jar)"
  trap 'rm -f "$TEMP_JAR"' EXIT
  kotlinc \
    shared/src/commonMain/kotlin/com/example/agendavalenciana/model/IosAccountCachePolicy.kt \
    shared/src/commonMain/kotlin/com/example/agendavalenciana/model/IosSavedAgendaPolicy.kt \
    shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosBatecNavigationPolicy.kt \
    shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosArtistBellPolicy.kt \
    COMPROVACIONS/PolicySmokeV350.kt -include-runtime -d "$TEMP_JAR"
  java -jar "$TEMP_JAR"
fi
