"""Step 19 static contract and frozen Android check; does NOT certify runtime or live API."""
from pathlib import Path
import zipfile,hashlib
root=Path(__file__).resolve().parents[1]
src=root/'shared/src/commonMain/kotlin/com/example/agendavalenciana'
api=(src/'ios/IosAgendaApi.kt').read_text()
ui=(src/'ios/AgendaIosScreen.kt').read_text()
projection=(src/'ios/IosBatecFeedContract.kt').read_text()
policy=(src/'model/IosBatecArtistGroups.kt').read_text()
server=Path('/mnt/data/audit_lagenda/web/api/api/batecs')
assert '"batecs/index.php?offset=$offset&limit=50"' in api
assert '"batecs/recent-events.php"' in api
assert 'currentLocalDateIso()' in api
assert 'current_user(false)' in (server/'index.php').read_text()
assert 'current_user(true)' in (server/'recent-events.php').read_text()
assert 'BATEC_LIFETIME_HOURS = 48' in (server/'_batecs.php').read_text()
assert 'if (eligible.size <= 6) return emptyList()' in projection
assert 'if (followed.size <= 6) return emptyList()' in policy
assert '.take(6)' in policy
assert 'FOLLOWED_PLACEHOLDER' in projection and 'EVENT_FALLBACK' in projection
assert 'event.artists + event.schedule.map' in projection
assert 'manual:' in projection and 'event:' in projection
assert 'IosBatecFeedContract.project' in api and 'LaunchedEffect(events, user.follows, excludedArtists, account?.id, guestReadyUserId, batecsRefreshEpoch)' in ui
assert 'IosBatecArtistGroups.canonicalSeenKey(key)' in ui
assert 'put("story_key", key)' in api and 'IosBatecOperations.canonicalKey' in api
assert 'preg_match(\'/^(manual|event):\\d+$/\'' in (server/'seen.php').read_text()
assert 'IosBatecStep19Test.kt' in [p.name for p in (root/'shared/src/commonTest/kotlin/com/example/agendavalenciana/ios').iterdir()]
with zipfile.ZipFile('/mnt/data/LAGENDA_V336_ANDROID_STUDIO_v336_COMPILACIO_CORREGIDA(3).zip') as z:
    count=sum(1 for n in z.namelist() if n.startswith('ANDROID_STUDIO/app/') and n.endswith('.kt')
        and (Path('/mnt/data/ANDROID_V336_REFERENCIA')/n).read_bytes()==z.read(n))
    assert count >= 78
with zipfile.ZipFile('/mnt/data/LAGENDA_V349_IOS_PANTALLA_COMPLETA_FEED_SCROLL (1)(2).zip') as z:
    count2=0
    for n in z.namelist():
        if n.startswith('ANDROID_STUDIO/app/') and not n.endswith('/'):
            assert (root/n.removeprefix('ANDROID_STUDIO/')).read_bytes()==z.read(n),n
            count2+=1
    assert count2>=90
print('PAS_19_PASS: 7 seguidors, 6 cercles, PHP Batecs, invitats, bloquejos, dates, clau vistos PHP, Android intacte')
