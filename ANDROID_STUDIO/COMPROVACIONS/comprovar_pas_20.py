"""Step 20 invariants; static inspection is not a compiled iPhone or live MariaDB test."""
from pathlib import Path
import zipfile
root=Path(__file__).resolve().parents[1]
ios=root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios'
api=(ios/'IosAgendaApi.kt').read_text(); ui=(ios/'AgendaIosScreen.kt').read_text()
queue=(ios/'IosPendingServerQueue.kt').read_text(); contract=(ios/'IosBatecOperations.kt').read_text()
server=Path('/mnt/data/audit_lagenda/web/api/api/batecs')
for ep in ['batecs/seen.php','batecs/index.php','batecs/eligibility.php','batecs/create.php','batecs/update.php','batecs/delete.php']:
    assert ep in api, ep
assert 'queuedWrite(owner, "post", buildJsonObject { put("story_key", key) }, "batecs/seen.php")' in api
assert '"batecs/seen.php"' in queue and 'pendingBatecSeenActions' in queue
assert 'IosBatecOperations.verifySeenReceipt' in api and 'IosBatecOperations.seenKeys(remote)' in api
assert 'registeredBatecOwner()' in api and 'checkSameRegisteredBatecOwner(owner)' in api
assert 'IosBatecOperations.verifyMutation' in api and 'IosBatecOperations.verifyDelete' in api
assert 'No es retornaran resultats parcials' not in api or 'no es retornaran resultats parcials' in api.lower()
assert 'can_edit' in contract and 'creator_user_id' in contract
assert 'BatecManagementDialog(' in ui and 'onBatecsChanged' in ui and 'onBatecSignIn' in ui
assert 'if (item.canEdit && canPublish)' in ui
assert 'api.batecEligibility' in ui and 'api.createBatec' in ui and 'api.updateBatec' in ui and 'api.deleteBatec' in ui
assert '(account?.id ?: guestReadyUserId) != expectedId' in ui
assert 'LaunchedEffect(events, user.follows, excludedArtists, account?.id, guestReadyUserId, batecsRefreshEpoch)' in ui
assert 'if (currentId == 0) api.createBatec' in ui
assert 'current_user(true)' in (server/'seen.php').read_text()
assert 'user_is_event_organizer' in (server/'_batecs.php').read_text()
assert 'user[\'role\']' in (server/'update.php').read_text()
assert 'current_user(true)' in (server/'create.php').read_text()
assert "clean_string($data['title'] ?? '', 120)" in (server/'create.php').read_text()
assert "clean_string($data['body'] ?? '', 1000)" in (server/'create.php').read_text()
assert 'IosBatecOperationsTest.kt' in [x.name for x in (root/'shared/src/commonTest/kotlin/com/example/agendavalenciana/ios').iterdir()]
with zipfile.ZipFile('/mnt/data/LAGENDA_V336_ANDROID_STUDIO_v336_COMPILACIO_CORREGIDA(3).zip') as z:
    matches=sum(1 for n in z.namelist() if n.startswith('ANDROID_STUDIO/app/') and n.endswith('.kt')
      and (Path('/mnt/data/ANDROID_V336_REFERENCIA')/n).read_bytes()==z.read(n))
    assert matches>=78, matches
with zipfile.ZipFile('/mnt/data/LAGENDA_V349_IOS_PANTALLA_COMPLETA_FEED_SCROLL (1)(2).zip') as z:
    count=0
    for n in z.namelist():
        if n.startswith('ANDROID_STUDIO/app/') and not n.endswith('/'):
            assert (root/n.removeprefix('ANDROID_STUDIO/')).read_bytes()==z.read(n),n
            count+=1
    assert count>=90
print('PAS_20_PASS: seen queued and verified, auth, create/update/delete and UI, Android immutable')
