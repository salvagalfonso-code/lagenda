#!/usr/bin/env python3
"""PAS 17: checks source contracts, no iPhone/MariaDB claims."""
from pathlib import Path
import zipfile
root=Path(__file__).resolve().parents[1]
ios=root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios'
a=(ios/'IosAgendaApi.kt').read_text()
c=(ios/'IosSavedListContract.kt').read_text()
u=(ios/'IosSavedAgendaScreen.kt').read_text()
main=(ios/'AgendaIosScreen.kt').read_text()
assert 'collections/index.php?action=list' in a
assert 'IosServerConnection.json("collections/index.php", buildJsonObject' in a
assert 'guestSession' in a and 'private suspend fun collectionIdentity()' in a
assert 'savedListWriteMutex.withLock' in a
assert 'activePersonalId() == owner && personalBearer(owner) == bearer' in a
assert 'IosSavedListContract.preflight(before, command)' in a
assert 'IosSavedListContract.verify(before, after, command, response)' in a
assert 'suspend fun revokeSavedListSharing(id: Int)' in a
for action in ['create_list','rename_list','delete_list','list_event','share_list','join_list','leave_list','revoke_list_share']:
 assert action in a and action in c
assert 'private val savedListWriteMutex = Mutex()' in a
assert 're-POSTing it after an ambiguous timeout could duplicate' in a
assert 'current.shareCode == code && current.inviteUrl == url' in c
assert 'preflight' in c and 'check(list.owner)' in c
assert 'onListsChanged(api.savedLists())' in u
assert 'api.revokeSavedListSharing(id)' in u
assert 'IosNativeBridge.current?.shareText' in u
assert 'savedListsRevision' in main and 'listsBefore == savedListsRevision' in main
assert (root/'shared/src/commonTest/kotlin/com/example/agendavalenciana/ios/IosSavedListContractTest.kt').is_file()
with zipfile.ZipFile('/mnt/data/LAGENDA_V336_ANDROID_STUDIO_v336_COMPILACIO_CORREGIDA(3).zip') as z:
 ref=Path('/mnt/data/ANDROID_V336_REFERENCIA')
 f=[p for p in z.namelist() if p.startswith('ANDROID_STUDIO/') and not p.endswith('/')]
 assert all((ref/p).read_bytes()==z.read(p) for p in f),'Android v336 changed'
with zipfile.ZipFile('/mnt/data/LAGENDA_V349_IOS_PANTALLA_COMPLETA_FEED_SCROLL (1)(2).zip') as z:
 f=[p for p in z.namelist() if p.startswith('ANDROID_STUDIO/app/') and not p.endswith('/')]
 assert all((root/p.removeprefix('ANDROID_STUDIO/')).read_bytes()==z.read(p) for p in f),'embedded Android changed'
print('PAS_17_STATIC_PASS: authenticated guests, server GET verification, ownership, sharing/revocation, Android unchanged')
