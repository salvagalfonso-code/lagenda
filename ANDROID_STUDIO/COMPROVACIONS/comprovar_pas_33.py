#!/usr/bin/env python3
"""Pas 33: bloquejos, denúncies, expulsions i permisos sobre el backend existent."""
from pathlib import Path
import hashlib,zipfile
r=Path(__file__).resolve().parents[1]
src=r/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios'
api=(src/'IosAgendaApi.kt').read_text();ui=(src/'IosCommunityAdvanced.kt').read_text()
policy=(src/'IosCommunityModerationPolicy.kt').read_text()
backend=Path('/mnt/data/LAGENDA_IOS_TREBALL_ACUMULATIU/SERVIDOR_COMUNITAT/api/community')
for route in ['community/blocks.php','community/report.php','community/manage.php','community/admin-reports.php']:
    assert route in api,route
for f in ['blocks.php','manage.php','report.php','admin-reports.php']:
    assert (backend/f).is_file()
for value in ['communityBlockedSnapshot(owner,bearer)','communityManagementSnapshot(groupId,owner,bearer)',
              'communityReportsSnapshot(owner,bearer,true)','communityIdentity()',
              'checkCommunityOwner(owner,bearer)','verifiedBlock','verifiedBan','verifiedReportStatus',
              'eligibleManagement','eligibleAdmin','require(account?.role?.uppercase()=="SUPER_ADMIN")']:
    assert value in api,value
assert 'reply.bool("ok")' in api and 'reply.text("action")==action' in api
assert 'val before=communityManagementSnapshot' in api
assert 'val actual=communityBlockedSnapshot' in api
for value in ['IosCommunityManageDialog','IosBlockedPeopleDialog','IosAdminReportsDialog',
              'IosCommunityReportDialog','api.setCommunityBlocked','api.moderateCommunityGroup',
              'api.reportCommunityMessage','api.reportCommunityGroup','remember(initial.id,currentUserId)']:
    assert value in ui,value
assert '"BLOCK" else "UNBLOCK"' in api
assert '"BAN_MEMBER","UNBAN_MEMBER"' in policy
assert '"DELETE_MESSAGE","CLOSE_GROUP","BAN_MEMBER"' in policy
assert 'action' in (backend/'manage.php').read_text()
assert 'community_require_creator' in (backend/'manage.php').read_text()
assert "'SUPER_ADMIN'" in (backend/'admin-reports.php').read_text()
assert (r/'shared/src/commonTest/kotlin/com/example/agendavalenciana/ios/IosCommunityModerationPolicyTest.kt').is_file()
with zipfile.ZipFile('/mnt/data/LAGENDA_V336_ANDROID_STUDIO_v336_COMPILACIO_CORREGIDA(3).zip') as z:
    checked=0
    for f in z.namelist():
        if not f.startswith('ANDROID_STUDIO/') or f.endswith('/'):continue
        p=Path('/mnt/data/LAGENDA_ANDROID_REFERENCIA_336')/f
        assert p.exists() and hashlib.sha256(p.read_bytes()).digest()==hashlib.sha256(z.read(f)).digest(),f
        checked+=1
    assert checked>90
with zipfile.ZipFile('/mnt/data/LAGENDA_V349_IOS_PANTALLA_COMPLETA_FEED_SCROLL (1)(2).zip') as z:
    for f in z.namelist():
        if f.startswith('ANDROID_STUDIO/app/src/') and not f.endswith('/'):
            p=r/f.removeprefix('ANDROID_STUDIO/')
            assert p.is_file() and hashlib.sha256(p.read_bytes()).digest()==hashlib.sha256(z.read(f)).digest(),f
print(f'PAS33_STATIC_PASS: API confirmations, permission guards, dialogs, no Android changes ({checked} files)')
