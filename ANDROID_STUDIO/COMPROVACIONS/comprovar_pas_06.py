#!/usr/bin/env python3
"""Source checks only, not Kotlin/Native build or live MariaDB tests."""
from pathlib import Path
from subprocess import run
import hashlib, zipfile
r=Path(__file__).resolve().parents[1]
a=r/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios'
q=(a/'IosPendingServerQueue.kt').read_text()
api=(a/'IosAgendaApi.kt').read_text()
sw=(r/'iosApp/AgendaIOS/AgendaKeychain.swift').read_text()
ui=(a/'AgendaIosScreen.kt').read_text()
linker=(a/'IosGuestAccountLinker.kt').read_text()
assert 'AES.GCM.seal' in sw and 'AES.GCM.open' in sw
assert 'pending_offline_aes256_key_v1' in sw and 'kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly' in sw
assert '.completeFileProtectionUnlessOpen' in sw and 'isExcludedFromBackup = true' in sw
assert 'loadEncryptedPendingOperations()' in sw and 'saveEncryptedPendingOperations(json: String)' in sw
assert 'UserDefaults.standard.set' not in sw[sw.index('// Pas 6:'):sw.index('/// Store large catalog JSON')]
assert 'queue.append(item)' in api and 'queue.flush(ownerId, bearer)' in api
assert 'if (queue.contains(item.id)) throw IosQueuedForRetryException()' in api
assert 'deliverAndVerify(action, bearer)' in q and 'persist(remaining)' in q
assert q.index('persist(current) // write-ahead') < q.index('suspend fun flush(')
assert 'if (action.ownerId != ownerId) continue' in q
assert 'ackIds == expectedIds' in api and 'APPLIED' in api and 'ALREADY_APPLIED' in api
assert 'check(activePersonalId() == action.ownerId)' in api
assert 'onConfirmedBeforeDiscard' in linker and 'transferPendingAfterVerifiedClaim' in api
assert 'LaunchedEffect(account?.id, guestReadyUserId, tab)' in ui and 'delay(60_000L)' in ui
assert all(f'"{name}"' in api for name in ('state','locations','feed','post'))
assert (r/'shared/src/commonTest/kotlin/com/example/agendavalenciana/ios/IosPendingServerQueueTest.kt').exists()
for zname, prefix, dest in [
    ('LAGENDA_V336_ANDROID_STUDIO_v336_COMPILACIO_CORREGIDA(3).zip','ANDROID_STUDIO/',r.parent.parent/'ANDROID_V336_REFERENCIA'),
    ('LAGENDA_V349_IOS_PANTALLA_COMPLETA_FEED_SCROLL (1)(2).zip','ANDROID_STUDIO/app/',r.parent)]:
    with zipfile.ZipFile(r.parent.parent/zname) as z:
        n=0
        for name in z.namelist():
            if not name.startswith(prefix) or name.endswith('/'): continue
            assert hashlib.sha256(z.read(name)).digest()==hashlib.sha256((dest/name).read_bytes()).digest(),name
            n+=1
        print('IMMUTABLE',zname,n,'files')
run(['swiftc','-frontend','-parse',str(r/'iosApp/AgendaIOS/AgendaKeychain.swift')],check=True)
print('PASS 06: encrypted AES-GCM write-ahead, per-user replay, confirmed removal, guest claim, Swift syntax and immutable Android')
