#!/usr/bin/env python3
"""Pas 39 regression: preserve base, guards, server QR authority and no fake offline state."""
from pathlib import Path
import hashlib,re,zipfile,plistlib
root=Path(__file__).resolve().parents[1]
work=root.parent
baseline=Path('/mnt/data/LAGENDA_IOS_V349_PASSOS_01_AL_36_ACUMULATIU(1).zip')
assert baseline.is_file()
allowed={
    'ESTAT_TREBALL_PASSOS.md',
    'ANDROID_STUDIO/shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/AgendaIosScreen.kt',
    'ANDROID_STUDIO/shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosAgendaApi.kt',
    'ANDROID_STUDIO/iosApp/AgendaIOS/AgendaKeychain.swift',
    'ANDROID_STUDIO/iosApp/AgendaIOS/AgendaProfileNative.swift',
    'ANDROID_STUDIO/iosApp/AgendaIOS/AgendaIOSApp.swift',
    'ANDROID_STUDIO/iosApp/AgendaIOS/Info.plist',
    'ANDROID_STUDIO/iosApp/AgendaIOS.xcodeproj/project.pbxproj',
    'ANDROID_STUDIO/iosApp/project.yml',
}
unchanged=0
with zipfile.ZipFile(baseline) as z:
    for name in z.namelist():
        if name.endswith('/'): continue
        p=work/name
        assert p.is_file(), f'Original file missing: {name}'
        if name not in allowed:
            assert hashlib.sha256(p.read_bytes()).digest()==hashlib.sha256(z.read(name)).digest(),f'Baseline changed: {name}'
            unchanged+=1
        if name.startswith(('ANDROID_STUDIO/app/', 'SERVIDOR_')):
            assert p.read_bytes()==z.read(name),f'Android/server changed: {name}'
api=(root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosAgendaApi.kt').read_text()
ui=(root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/AgendaIosScreen.kt').read_text()
policy=(root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosOfflineTicketPolicy.kt').read_text()
swift=(root/'iosApp/AgendaIOS/AgendaKeychain.swift').read_text()
profile=(root/'iosApp/AgendaIOS/AgendaProfileNative.swift').read_text()
for line in [
    'tickets/mine.php','tickets/purchase.php','payments/status.php',
    'tickets/validate.php','storeIssuedTicketsOffline(start.userId, issued, replace = false)',
    'storeIssuedTicketsOffline(owner, issued, replace = false)',
    'storeIssuedTicketsOffline(owner, serverTickets, replace = true)',
    'saveOfflineTickets(owner, snapshot)', 'loadOfflineTicketsForCurrentSession()',
    'IosOfflineTicketPolicy.requireValid(serverTickets)',
    'put("owner_id", owner)', 'put("session_date", t.sessionDate)',
    'put("status", t.status)', 'put("code", t.code)',
    'if (IosPaidTicketPolicy.isPaid(answer.status))'
]:
    assert line in api, line
for line in [
    'reloadMyTickets()', 'ticketsOffline = true', 'Còpia xifrada per a ús sense connexió',
    'IosOfflineTicketPolicy.displayStatus(ticket.status)',
    'IosOfflineTicketPolicy.details(ticket)', 'showIssuedTicketQr(ticket.code',
    'onPurchased(true)', 'onPurchased(false)', 'ProfessionalValidationDialog',
    'showTicketScanner(it)', 'api.validateTicket(eventId.toInt(), code)'
]:
    assert line in ui, line
assert all(x in policy for x in ('ACTIVE', 'USED', 'CANCELLED','requireValid','ticket.code.none'))
for line in ['AES.GCM.seal(data, using: key)', 'AES.GCM.open(sealed, using: key)',
             'offlineTicketFile(ownerId: uid)', 'ticketBearerFingerprint(credential.token)',
             'root["owner_id"] as? Int == uid', 'func loadOfflineTicketsForCurrentSession()',
             'try bytes.write(to: file, options: [.atomic, .completeFileProtectionUnlessOpen])']:
    assert line in swift, line
assert 'clearOfflineTickets(ownerId: userId)' in profile
assert 'outcome == "VALIDATED"' in swift
assert 'AgendaNativeApi.post("tickets/validate.php"' in swift
assert 'status == "paid"' not in policy
plist=plistlib.loads((root/'iosApp/AgendaIOS/Info.plist').read_bytes())
assert plist.get('NSCameraUsageDescription')
print(f'PAS39_STATIC_PASS preserved={unchanged} unchanged baseline files, Android/server unchanged, ticket cache encrypted/identity bound, QR server validation only')
