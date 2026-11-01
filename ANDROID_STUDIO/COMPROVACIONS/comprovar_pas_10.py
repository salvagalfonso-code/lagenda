#!/usr/bin/env python3
"""Pas 10: grounded static verification, not live device/API integration."""
from pathlib import Path
import hashlib, zipfile, re
root=Path(__file__).resolve().parents[1]
p=root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios'
api=(p/'IosAgendaApi.kt').read_text(); ui=(p/'AgendaIosScreen.kt').read_text()
q=(p/'IosPendingServerQueue.kt').read_text();mut=(p/'IosFollowMutation.kt').read_text()
server=Path('/mnt/data/audit_ios_port/s/api/user-state/follow.php').read_text()
server_state=Path('/mnt/data/audit_ios_port/s/api/user-state/mine.php').read_text()
assert all(x in server for x in ['entity_type','entity_key','entity_label','following','user_follows','send_json'])
assert 'current_user(true)' in server and 'current_user(true)' in server_state
assert 'user-state/mine.php' in api and 'user-state/follow.php' in api
assert 'guestSession.current()?.token' in api
assert 'request("user-state/mine.php", authenticated = true)' in api
assert 'pendingFollowActions(expectedId)' in api and 'pendingFollowActions(ownerId: Int)' in q
assert 'response.text("entity_type").uppercase() == expectedType' in api
assert 'response["following"]?.jsonPrimitive?.booleanOrNull == expectedFollowing' in api
assert 'IosFollowMutation.canonicalKey(response.text("entity_key")) == expectedKey' in api
assert '"user-profiles/catalog.php"' in api and 'keys.distinct().singleOrNull()' in api
assert 'label' not in mut[mut.index('val retained'):mut.index('return if (follow)')]
assert 'user.followsType(type, resolvedFollowKey(type, name))' in ui
assert 'IosCategoryTaxonomy.mainKey(raw)' in ui
assert 'val expectedId = account?.id ?: guestReadyUserId' in ui
assert 'if (account == null) { loginOpen = true; return }' not in ui[ui.index('fun toggleFollowEntity'):ui.index('fun toggleFollow(value')]
assert 'followEditRevision == expectedFollowRevision' in ui
assert 'followRevisionBeforeRead == followEditRevision' in ui
assert 'reconcilePendingUserState(api.decodeUserState(it), expectedId)' in ui
assert 'catch (_: IosQueuedForRetryException)' in ui
assert 'Seguiment pendent de sincronitzar' in ui
assert 'if (api.activePersonalId() != null) runCatching { api.userState() }' in ui
assert 'if ((account != null || guestReadyUserId != null) && user.follows.isNotEmpty())' in ui
# Reference snapshot and the bundled v349 Android sources remain unchanged.
with zipfile.ZipFile('/mnt/data/LAGENDA_V336_ANDROID_STUDIO_v336_COMPILACIO_CORREGIDA(3).zip') as z:
    count=0
    for name in z.namelist():
        if name.startswith('ANDROID_STUDIO/app/') and name.endswith('.kt'):
            cur=Path('/mnt/data/ANDROID_V336_REFERENCIA')/name
            assert hashlib.sha256(cur.read_bytes()).digest()==hashlib.sha256(z.read(name)).digest(),name
            count+=1
    assert count>75
with zipfile.ZipFile('/mnt/data/LAGENDA_V349_IOS_PANTALLA_COMPLETA_FEED_SCROLL (1)(2).zip') as z:
    count2=0
    for name in z.namelist():
        if name.startswith('ANDROID_STUDIO/app/') and not name.endswith('/'):
            cur=root/name.removeprefix('ANDROID_STUDIO/')
            assert cur.read_bytes() == z.read(name),name
            count2 += 1
    assert count2>75
assert (root/'shared/src/commonTest/kotlin/com/example/agendavalenciana/ios/IosFollowCompatibilityTest.kt').is_file()
print(f'PAS_10_PASS: mateix PHP, guest+registered, tokens per id, receipts, cua, IDs, GET obsolets, Android immòbil ({count}+{count2} fitxers)')
