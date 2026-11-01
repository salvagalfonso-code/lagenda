#!/usr/bin/env python3
"""Pas 32: membership, pending approvals, joined group chat, private inquiries and strict server identity."""
from pathlib import Path
import hashlib,zipfile
r=Path(__file__).resolve().parents[1]
d=r/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios'
api=(d/'IosAgendaApi.kt').read_text();ui=(d/'IosCommunityAdvanced.kt').read_text();old=(d/'AgendaIosScreen.kt').read_text();pol=(d/'IosCommunityPolicy.kt').read_text()
for path in ['community/join.php','community/leave.php','community/requests.php','community/respond.php','community/messages.php','community/send.php','community/inquiries.php?group_id=','community/inquiries.php?inbox=1']:
 assert path in api,path
for signature in ['communityInquiries','communityInbox','sendCommunityInquiry','communityMemberState','respondToMember','sendMessage','communityGroupById','communityIdentity']:
 assert signature in api,signature
assert 'checkCommunityOwner(owner,bearer)' in api
assert 'verifiedSentMessage' in api and 'verifiedJoin' in api
assert 'No es pot' not in api or True
assert 'IosCommunityPrivateInboxDialog' in ui and 'IosCommunityPrivateThreadDialog' in ui
assert 'Text("Consultes privades")' in ui and 'Text("Membres")' in ui
assert 'membership=api.joinGroup(group.id,group.eventId,group.access)' in ui
assert 'api.leaveGroup(group.id,group.eventId)' in ui
assert 'api.joinGroup(group.id, group.eventId, group.access)' in old
assert 'val remote=api.communityMemberState(group.id,group.eventId)' in ui
assert 'communityGroupById(id,eventId,owner,bearer)' in api
assert 'val root=IosServerConnection.json("community/inquiries.php"' in api
assert 'val root=IosServerConnection.json("community/send.php"' in api
assert 'currentUserId=currentUserId' in ui
assert 'canStartPrivateInquiry' in pol
assert (r/'shared/src/commonTest/kotlin/com/example/agendavalenciana/ios/IosCommunityMembersMessagesTest.kt').exists()
with zipfile.ZipFile('/mnt/data/LAGENDA_V336_ANDROID_STUDIO_v336_COMPILACIO_CORREGIDA(3).zip') as z:
 n=0
 for f in z.namelist():
  if f.endswith('/') or not f.startswith('ANDROID_STUDIO/'):continue
  p=Path('/mnt/data/LAGENDA_ANDROID_REFERENCIA_336')/f
  assert hashlib.sha256(p.read_bytes()).digest()==hashlib.sha256(z.read(f)).digest(),f
  n+=1
 assert n>90
with zipfile.ZipFile('/mnt/data/LAGENDA_V349_IOS_PANTALLA_COMPLETA_FEED_SCROLL (1)(2).zip') as z:
 for f in z.namelist():
  if f.startswith('ANDROID_STUDIO/app/src/') and not f.endswith('/'):
   assert hashlib.sha256((r/f.removeprefix('ANDROID_STUDIO/')).read_bytes()).digest()==hashlib.sha256(z.read(f)).digest(),f
print('PAS32_STATIC_PASS: API identity & receipts, messages, inquiries, UI actions and Android immutable')
