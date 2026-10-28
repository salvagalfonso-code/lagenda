#!/usr/bin/env python3
"""Auditoria estatica reproduible; NO substitueix xcodebuild a macOS."""
from pathlib import Path
import json, re, sys, xml.etree.ElementTree as ET, subprocess, plistlib
try: import yaml
except ImportError: yaml=None
R = Path(__file__).resolve().parent.parent
errors, warnings, validations = [], [], []
def ok(test, name, detail=''):
    (validations if test else errors).append(name + ((': '+detail) if detail else ''))
for rel in ('gradlew','settings.gradle.kts','shared/build.gradle.kts',
    'iosApp/AgendaIOS.xcodeproj/project.pbxproj',
    'iosApp/AgendaIOS.xcodeproj/xcshareddata/xcschemes/AgendaIOS.xcscheme',
    'iosApp/AgendaIOS/Info.plist',
    'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosForYouSnapshot.kt',
    'shared/src/iosMain/kotlin/com/example/agendavalenciana/ios/Clock.ios.kt',
    'codemagic.yaml'):
    ok((R/rel).is_file(), 'Arxiu essencial '+rel)
for p in R.rglob('*'):
    if not p.is_file() or '_qa' in p.parts or p.name == Path(__file__).name: continue
    name=p.relative_to(R).as_posix()
    if p.suffix.lower() in ('.kt','.kts','.swift','.yaml','.yml','.sh','.pbxproj','.json','.xml','.plist','.md'):
        try: t=p.read_text('utf8')
        except UnicodeError: errors.append('UTF8 invàlid: '+name);continue
        if re.search(r'^(?:<{7}(?: HEAD| head)?|={7}|>{7})',t,re.M): errors.append('Conflicte Git sense resoldre: '+name)
    if p.suffix.lower()=='.json':
        try: json.loads(p.read_text('utf8'))
        except Exception as exc:errors.append('JSON invàlid '+name+': '+str(exc))
    if p.suffix.lower()=='.xml':
        try:ET.parse(p)
        except Exception as exc:errors.append('XML invàlid '+name+': '+str(exc))
    if p.suffix.lower()=='.plist':
        try:plistlib.loads(p.read_bytes())
        except Exception as exc:errors.append('Plist invàlid '+name+': '+str(exc))
    if p.suffix.lower() in ('.sh',) or p.name=='gradlew':
        run=subprocess.run(['bash','-n',str(p)],text=True,capture_output=True)
        if run.returncode:errors.append('Shell invàlid '+name+': '+run.stderr.strip())
if yaml:
    for name in ('codemagic.yaml','iosApp/project.yml'):
        try:obj=yaml.safe_load((R/name).read_text())
        except Exception as exc:errors.append('YAML invàlid '+name+': '+str(exc));continue
        if name=='codemagic.yaml':
            ok('workflows' in obj and bool(obj['workflows']), 'Workflows de Codemagic definits')
            for wid,w in (obj.get('workflows') or {}).items():
                ok(bool(w.get('artifacts')), f'Workflow {wid}: artifacts exportats')
                for s in w.get('scripts',[]):
                    if isinstance(s,dict) and s.get('script'):
                        import tempfile
                        with tempfile.NamedTemporaryFile('w',suffix='.sh') as f:
                            f.write(s['script']);f.flush()
                            q=subprocess.run(['bash','-n',f.name],capture_output=True,text=True)
                            if q.returncode:errors.append('Bash dins YAML '+str(s.get('name'))+': '+q.stderr)
        validations.append('YAML correcte: '+name)
else:warnings.append('No disponible PyYAML')
proj=(R/'iosApp/AgendaIOS.xcodeproj/project.pbxproj').read_text()
scheme=(R/'iosApp/AgendaIOS.xcodeproj/xcshareddata/xcschemes/AgendaIOS.xcscheme').read_text()
for swift in (R/'iosApp/AgendaIOS').glob('*.swift'):
    ok(swift.name in proj,'Swift al target: '+swift.name)
for expected in ('AgendaIOS.app','AgendaIOS','C8A8118B2E99D4370C77A44B'):
    ok(expected in scheme and expected in proj,'Esquema coherent '+expected)
for api in ('loadToken','saveToken','clearToken','readCache','writeCache','scheduleLocalReminderConfirmed','cancelLocalReminder','showTicketQr','showTicketScanner','showNotificationSettings'):
    ok(re.search(r'\bfunc '+api+r'\(', (R/'iosApp/AgendaIOS/AgendaKeychain.swift').read_text()) is not None, 'Bridge Swift: '+api)
snapshot=(R/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosForYouSnapshot.kt').read_text()
clock=(R/'shared/src/iosMain/kotlin/com/example/agendavalenciana/ios/Clock.ios.kt').read_text()
ok('toSortedMap()' not in snapshot, 'No s\'utilitza toSortedMap a IosForYouSnapshot')
ok('NSTimeZone(name' not in clock and 'timeZoneWithName' not in clock,'Clock iOS sense constructors NSTimeZone invàlids')
ok('NSDate().timeIntervalSince1970' not in clock,'Clock iOS sense membre NSDate invàlid')
script=subprocess.run(['swiftc','-frontend','-parse',*[str(p) for p in (R/'iosApp/AgendaIOS').glob('*.swift')]],capture_output=True,text=True) if subprocess.run(['which','swiftc'],capture_output=True).returncode==0 else None
if script:
    if script.returncode: errors.append('Swift -parse: '+script.stderr[:1200])
    else: validations.append('Sintaxi Swift analitzada (sense SDK Apple)')
else: warnings.append('swiftc no disponible')
count=sum(1 for x in R.rglob('*') if x.is_file() and '_qa' not in x.parts)
report={'inspected_files':count,'checks_passed':len(validations),'failures':errors,'warnings':warnings, 'limitations':['Sense Mac/Xcode no es pot compilar Kotlin/Native ni verificar APIs iOS enllaçades','No es pot verificar si GitHub conte exactament esta versio sense descarregar el repositori actual']}
(R/'COMPROVACIONS/resultat_auditoria.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
print(json.dumps(report,ensure_ascii=False,indent=2))
sys.exit(1 if errors else 0)
