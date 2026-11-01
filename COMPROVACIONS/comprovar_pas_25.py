#!/usr/bin/env python3
"""Pas 25: contracte de pestanyes i API, regles de convidats i Android immutable."""
from pathlib import Path
import hashlib, zipfile
root=Path(__file__).resolve().parents[1]
src=root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios'
ui=(src/'AgendaIosScreen.kt').read_text()
contract=(src/'IosEventDetailContract.kt').read_text()
api=(src/'IosAgendaApi.kt').read_text()
original=(Path('/mnt/data')/'LAGENDA_V336_ANDROID_STUDIO_v336_COMPILACIO_CORREGIDA(3).zip')
android=Path('/mnt/data/ANDROID_V336_REFERENCIA')
with zipfile.ZipFile(original) as z:
    checked=0
    for name in z.namelist():
        if name.startswith('ANDROID_STUDIO/') and not name.endswith('/'):
            dst=android/name
            assert dst.exists() and hashlib.sha256(dst.read_bytes()).digest()==hashlib.sha256(z.read(name)).digest(),name
            checked+=1
    assert checked>90
assert 'listOf("Info", "Entrades", "Ubicació", "Comunitat")' in contract
assert 'listOf("Horaris", "Info", "Entrades", "Ubicació", "Comunitat")' in contract
assert 'val tabs = IosEventDetailContract.tabs(ev.isFestival)' in ui
assert 'if (ev.artists.isNotEmpty() && !ev.isFestival)' in ui
assert 'onOpenCommunityGroup: (IosCommunityGroup) -> Unit' in ui
assert 'onOpenCommunityGroup(group)' in ui
assert 'communityLoading' in ui and 'communityError' in ui and 'communityReload++' in ui
assert 'batecApi.community(ev.id)' in ui and 'group.eventId == ev.id' in ui
assert 'eventGroups.forEach' in ui and 'communityTypeLabel(group.type)' in ui
assert 'userId == null' in ui and 'guestReadyUserId' in ui
assert 'account?.id ?: guestReadyUserId ?: 0' in ui
assert 'api.community(ev.id); tab = "Comunitat"' in ui
assert 'community/groups.php' in api
assert 'tickets/options.php?event_id=$eventId' in api
assert 'batecApi.ticketOptions(ev.id)' in ui
assert 'IosEventDetailContract.isSafeExternalTicketUrl' in ui
assert 'IosEventDetailContract.placeQuery' in ui
assert 'https://maps.apple.com/?q=${dest.encodeURLParameter()}' in ui and 'mapDestination = search' in ui
assert 'onReminder = ' in ui and 'onSave = ' in ui and 'onReport = ' in ui
assert (root/'shared/src/commonTest/kotlin/com/example/agendavalenciana/ios/IosEventDetailContractTest.kt').exists()
assert not any(t in contract for t in ('UserDefaults', 'SharedPreferences'))
print('PAS25_STATIC_PASS: 4 tabs + festival, tickets, maps, in-popup community guest & APIs, Android unchanged')
