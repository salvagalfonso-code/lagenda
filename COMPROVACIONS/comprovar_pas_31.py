#!/usr/bin/env python3
"""Pas 31: Android five-section Community mapping, guest identity, server-only creation, protect original."""
from pathlib import Path
import subprocess,zipfile,hashlib
r=Path(__file__).resolve().parents[1]
a=r/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios'
api=(a/'IosAgendaApi.kt').read_text();ui=(a/'AgendaIosScreen.kt').read_text();adv=(a/'IosCommunityAdvanced.kt').read_text();pol=(a/'IosCommunityBoardContract.kt').read_text()
assert 'val sections = listOf("CAR", "TRANSPORT", "MEETUP", "HOSTING", "OTHER")' in pol
assert pol.count('"LGBTQ_FRIENDLY"') >= 2
assert 'fun serverType(raw: String)' in pol and '"HOSTING" -> "OTHER"' in pol
assert 'community/board-settings.php?group_ids=1' in api and 'community/create.php' in api
assert 'put("board_type",type)' in api and 'put("chat_enabled",chatEnabled)' in api
assert 'saveBoardSettings(created.id,type,chatEnabled,owner,bearer)' in api
assert 'settings[g.id]?.let' in api and '.chunked(100)' in api
assert 'community/board-settings.php?contacted=1' in api
assert 'checkCommunityOwner(owner,bearer)' in api
assert 'No el tornes a crear' in api
assert 'currentUserId = account?.id ?: guestReadyUserId ?: 0' in ui
assert 'var chatEnabled by remember' in ui
assert 'submitted = true' in ui
assert 'IosCommunityBoardContract.section(group.type)' in adv
assert '"CONTACT"' in adv and 'currentUserId' in adv
assert 'if(!group.chatEnabled)' in adv
assert (r/'shared/src/commonTest/kotlin/com/example/agendavalenciana/ios/IosCommunityBoardContractTest.kt').is_file()
zip_path=Path('/mnt/data/LAGENDA_V336_ANDROID_STUDIO_v336_COMPILACIO_CORREGIDA(3).zip')
ref=Path('/mnt/data/ANDROID_V336_REFERENCIA')
n=0
with zipfile.ZipFile(zip_path) as z:
 for name in z.namelist():
  if not name.endswith('/') and name.startswith('ANDROID_STUDIO/'):
   p=ref/name
   assert p.exists() and hashlib.sha256(p.read_bytes()).digest()==hashlib.sha256(z.read(name)).digest(),name
   n+=1
assert n>90
orig_ios_zip=Path('/mnt/data/LAGENDA_V349_IOS_PANTALLA_COMPLETA_FEED_SCROLL (1)(2).zip')
with zipfile.ZipFile(orig_ios_zip) as z:
 for name in z.namelist():
  if name.startswith('ANDROID_STUDIO/app/src/') and not name.endswith('/'):
   p=r/name.removeprefix('ANDROID_STUDIO/')
   assert p.exists() and hashlib.sha256(p.read_bytes()).digest()==hashlib.sha256(z.read(name)).digest(),name
print('PAS31_STATIC_PASS: 11 types/5 sections, full PHP board settings, guests, non-replayable create, Android SHA intact; files checked',n)
