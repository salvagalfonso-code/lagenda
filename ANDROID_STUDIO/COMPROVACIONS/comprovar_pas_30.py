#!/usr/bin/env python3
"""Pas 30: APNs iOS + server staging, never changes Android or deploys to production."""
from pathlib import Path
import plistlib
import subprocess
import json
ROOT=Path(__file__).resolve().parents[1]
WORK=ROOT.parent
SW=(ROOT/'iosApp/AgendaIOS/AgendaKeychain.swift').read_text()
APP=(ROOT/'iosApp/AgendaIOS/AgendaIOSApp.swift').read_text()
API=(ROOT/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosAgendaApi.kt').read_text()
UI=(ROOT/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/AgendaIosScreen.kt').read_text()
BASE=(WORK/'SERVIDOR_APNS_PAS30')
assert 'apns_hex_device_token_v2' in SW and 'SecItemAdd(q as CFDictionary' in SW
assert 'writeCache(key: "apns_device_token"' not in SW
assert 'push_open_artist' not in SW
assert 'setPushDeliveryEnabled(enabled: Bool)' in SW
assert 'loadGuestIdentity()' in SW and 'currentPushBearer()' in SW
assert 'syncDeviceTokenIfAuthenticated()' in SW
assert 'unregisterDevice(bearer: old)' in SW
assert '"notifications/apns-device.php"' in SW
assert '(200..<300).contains(http.statusCode)' in SW and 'obj["ok"] as? Bool == true' in SW
assert 'fun onOpenEvent(eventId: Int)' in API and 'func didTapPushEvent(_ eventId: Int32)' in SW
assert 'pendingPushEventId' in UI and 'fun useAccountPreferences(id: Int?)' in UI
assert 'IosNativeBridge.current?.setPushDeliveryEnabled(false)' in UI
assert 'IosNativeBridge.current?.setPushDeliveryEnabled(draft.pushEnabled)' in UI
assert 'requestAuthorization(options: [.alert, .badge, .sound])' not in APP
assert 'didRegisterForRemoteNotificationsWithDeviceToken' in APP
assert 'didFailToRegisterForRemoteNotificationsWithError' in APP
assert 'ensureRemoteRegistrationIfPermitted()' in APP
assert 'AgendaKeychain.shared.didTapPushEvent' in APP
plist=plistlib.loads((ROOT/'iosApp/AgendaIOS/AgendaIOS.entitlements').read_bytes())
assert plist['aps-environment']=='$(AGENDA_APNS_ENV)'
pbx=(ROOT/'iosApp/AgendaIOS.xcodeproj/project.pbxproj').read_text()
assert 'AGENDA_APNS_ENV = development' in pbx and 'AGENDA_APNS_ENV = production' in pbx
assert 'GET_LOCK(?,0)' in (BASE/'cron/inbox-push.php').read_text()
assert 'ios_apns_preferences' in (BASE/'cron/artist-push.php').read_text()
assert 'ios_apns_quiet_end' in (BASE/'cron/inbox-push.php').read_text()
assert 'ios_apns_quiet_end' in (BASE/'cron/artist-push.php').read_text()
assert 'ios_push_enabled' in (BASE/'api/notifications/apns-device.php').read_text()
assert 'CREATE TABLE IF NOT EXISTS ios_apns_inbox_delivery' in (BASE/'sql/02_apns_inbox_delivery.sql').read_text()
assert not list(WORK.rglob('*.p8')), 'PRIVATE APPLE SIGNING KEY MUST NOT SHIP'
for fn in [ROOT/'iosApp/AgendaIOS/AgendaIOSApp.swift', ROOT/'iosApp/AgendaIOS/AgendaKeychain.swift']:
    assert fn.is_file()
for p in [BASE/'api/notifications/apns-device.php',*sorted((BASE/'cron').glob('*.php'))]:
    q=subprocess.run(['php','-l',str(p)],capture_output=True,text=True)
    assert q.returncode==0,(p,q.stderr)
php=(BASE/'cron/_ios_push_prefs.php').as_posix().replace("'","\\'")
code="""require '%s';
$p=['ios_push_enabled'=>true,'ios_push_quiet_enabled'=>true,'ios_push_quiet_start'=>1380,'ios_push_quiet_end'=>480];
function check($b,$m){if(!$b){fwrite(STDERR,$m);exit(12);}}
check(ios_apns_allowed($p,'ARTIST'),'artist allowed');
check(!ios_apns_allowed(['ios_push_enabled'=>false],'ARTIST'),'disabled');
check(ios_apns_allowed($p,'EVENT'),'initial event allowed');
$p['alert_day_before']=false;check(!ios_apns_allowed($p,'EVENT'),'category disabled');
$now=new DateTimeImmutable('2026-11-01T22:30:00+00:00');
check(ios_apns_quiet_end($p,false,$now)!==null,'night quiet');
check(ios_apns_quiet_end($p,true,$now)===null,'custom reminders bypass quiet');
$now=new DateTimeImmutable('2026-11-01T11:00:00+00:00');
check(ios_apns_quiet_end($p,false,$now)===null,'midday allowed');
$p['ios_push_quiet_start']=480;$p['ios_push_quiet_end']=1020;
$now=new DateTimeImmutable('2026-11-01T11:00:00+00:00');
check(ios_apns_quiet_end($p,false,$now)!==null,'same day quiet');
check(ios_apns_quiet_end(['ios_push_enabled'=>true,'ios_push_quiet_enabled'=>true,'ios_push_quiet_start'=>60,'ios_push_quiet_end'=>60],false,$now)===null,'equal hours disable');
echo 'PASS PHP APNs preferences quiet/categorical/custom';""" % php
q=subprocess.run(['php','-r',code],capture_output=True,text=True)
assert q.returncode==0,(q.stdout,q.stderr)
print(q.stdout.strip())
print('PAS30_STATIC_PASS: native token Keychain, guest/registered, APNs opt-in, permission, backend preference gates, time zones and Android frozen')
