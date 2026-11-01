#!/usr/bin/env python3
"""Pas 27: artist popup, real public endpoints, view controls and unchanged Android."""
from pathlib import Path
import hashlib,zipfile
root=Path(__file__).resolve().parents[1]
i=root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios'
ui=(i/'AgendaIosScreen.kt').read_text()
api=(i/'IosAgendaApi.kt').read_text()
policy=(i/'IosArtistPagePolicy.kt').read_text()
assert 'private fun ArtistDialog(' in ui
assert 'publicPage: IosArtistPublicPage?' in ui and 'onRetryPage:' in ui
assert 'IosArtistPagePolicy.imageUrl' in api and 'IosArtistPagePolicy.sameArtist' in api
assert all(x in api for x in ('professional-profiles/catalog.php?type=GROUP&q=$encoded', 'entities/images.php', 'artist-store/catalog.php?artist=$encoded'))
assert 'val matched = profilesResult.getOrDefault(emptyList()).firstOrNull' in api
assert 'val products = if (catalog != null && IosArtistPagePolicy.sameArtist(officialName, wanted))' in api
assert 'if (profilesResult.isFailure && entitiesResult.isFailure && result.isFailure)' in api
assert 'request("notifications/artist-bell.php"' in api
assert 'val requestUser = account?.id ?: guestReadyUserId' in ui
assert 'if ((account?.id ?: guestReadyUserId) == requestUser)' in ui
assert 'AsyncImage(model = if (photo.startsWith("/"))' in ui
assert 'No hi ha productes publicats per a este artista.' in ui
assert 'Fotografia de $name' in ui and 'Productes' in ui and 'onFollow' in ui and 'onBell' in ui
assert 'if (pageLoading) LinearProgressIndicator' in ui
assert 'event.posterUrl' not in ui[ui.index('private fun ArtistDialog('):ui.index('/** One independent artist bell')]
assert 'ArtistDialog(value, user.follows(value)' in ui
assert 'EventCard(ev, isSaved = false, onClick = { onEvent(ev) }' in ui
assert 'fun money' in policy and 'fun discipline' in policy and 'fun activityHeading' in policy
assert (root/'shared/src/commonTest/kotlin/com/example/agendavalenciana/ios/IosArtistPagePolicyTest.kt').exists()
assert (root/'COMPROVACIONS/provar_pas_27.kt').exists()
# Match the v336 immutable Android reference AND the embedded Android of original v349.
for zipname, folder, strip in [
 ('LAGENDA_V336_ANDROID_STUDIO_v336_COMPILACIO_CORREGIDA(3).zip',Path('/mnt/data/ANDROID_V336_REFERENCIA'),False),
 ('LAGENDA_V349_IOS_PANTALLA_COMPLETA_FEED_SCROLL (1)(2).zip',root,True)]:
    total=0
    with zipfile.ZipFile('/mnt/data/'+zipname) as z:
        for n in z.namelist():
            if n.startswith('ANDROID_STUDIO/app/') and not n.endswith('/'):
                dst=folder/(n.removeprefix('ANDROID_STUDIO/') if strip else n)
                assert dst.is_file() and hashlib.sha256(dst.read_bytes()).digest()==hashlib.sha256(z.read(n)).digest(),n
                total+=1
    assert total>90
print('PAS27_STATIC_PASS: artist identity, profiles/photos, products, guest bells, UI and immutable Android')
