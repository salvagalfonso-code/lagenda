#!/usr/bin/env python3
"""Static/integrity check: not an iPhone build or a PHP/MariaDB integration test."""
from pathlib import Path
import hashlib, zipfile
p=Path(__file__).resolve().parents[2]
source=p/'ANDROID_STUDIO'/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios'
api=(source/'IosAgendaApi.kt').read_text()
ui=(source/'IosSuperAdminConsole.kt').read_text()
policy=(source/'IosAdminActionPolicy.kt').read_text()
base=Path('/mnt/data/LAGENDA_IOS_V349_PASSOS_01_AL_45_ACUMULATIU_ZIP_15_DE_16.zip')
assert base.exists(), 'Missing source ZIP 15/16'
with zipfile.ZipFile(base) as z:
    entries=[n for n in z.namelist() if not n.endswith('/')]
    missing=[n for n in entries if not (p/n).is_file()]
    assert not missing, f'Files lost: {missing[:5]}'
    changes=[n for n in entries if hashlib.sha256(z.read(n)).digest()!=hashlib.sha256((p/n).read_bytes()).digest()]
    unauthorized=[n for n in changes if not (n.startswith('ANDROID_STUDIO/shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/')
        or n.startswith('ANDROID_STUDIO/iosApp/') or n=='ESTAT_TREBALL_PASSOS.md')]
    assert not unauthorized, f'Android/Server files changed: {unauthorized}'
required=(
    'private fun adminIdentity()', 'private fun checkAdminIdentity(', 'private suspend fun adminRead(',
    'private suspend fun adminWrite(', 'private val adminMutationMutex = Mutex()',
    'checkAdminIdentity(who)', 'fun adminSetUserAccess(', 'fun adminDeleteUser(',
    'fun adminReviewCommunity(', 'fun adminModerateEvent(', 'fun adminPreviewTargets(',
    'fun adminSendTargets(', 'fun adminPendingProfessionalClaims(',
    'fun adminReviewProfessionalClaim(', 'fun adminFindEvents(', 'fun adminEventReports(',
)
for part in required:
    assert part in api, f'Missing API action or guard: {part}'
for endpoint in (
    'admin/dashboard.php', 'admin/users.php', 'admin/user-detail.php', 'admin/user-access-ios.php',
    'admin/user-delete.php', 'admin/events.php', 'admin/reports.php',
    'events/community-pending.php', 'events/community-review.php', 'moderation/review.php',
    'professional-profiles/pending-requests.php', 'professional-profiles/review-request.php',
    'admin/notifications-preview.php', 'admin/notifications-send.php'
):
    assert endpoint in api, f'Missing reference route: {endpoint}'
assert 'remember(actorId)' in ui, 'Sensitive view state must be identity-bound'
assert 'validProfessionalClaim' in ui and 'He comprovat les proves' in ui
assert 'Acció de moderació executada al servidor' not in ui, 'Do not claim full moderation confirmation'
assert 'validCampaign' in policy and 'validReview' in policy
assert 'adminUsers().firstOrNull' in api and 'adminCommunityPending().none' in api
assert (source/'IosAdminActionPolicy.kt').is_file()
print(f'PASS 47: {len(entries)} base ZIP entries retained, {len(changes)} permitted changed entries, no Android/server changes')
print('PASS 47: admin guard + 14 named server routes + review/readback + UI confirmation checks')
print('Differing entries from ZIP15:', ', '.join(changes))
