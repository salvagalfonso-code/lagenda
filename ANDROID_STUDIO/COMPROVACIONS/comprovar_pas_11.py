#!/usr/bin/env python3
"""Pas 11: source/contract checks only; no live API, real database or iOS compilation."""
from pathlib import Path
import hashlib, zipfile
root=Path(__file__).resolve().parents[1]
ios=root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios'
ui=(ios/'AgendaIosScreen.kt').read_text()
api=(ios/'IosAgendaApi.kt').read_text()
queue=(ios/'IosPendingServerQueue.kt').read_text()
center=(ios/'IosCentralState.kt').read_text()
visibility=(ios/'IosVisibilityAffinityContract.kt').read_text()
policy=(root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/model/IosFeedExclusionPolicy.kt').read_text()
php_visibility=Path('/mnt/data/audit_lagenda/web/api/visibility/index.php').read_text()
php_affinity=Path('/mnt/data/audit_lagenda/web/api/affinities/record.php').read_text()
php_state=Path('/mnt/data/audit_lagenda/web/api/user-state/mine.php').read_text()
assert 'agenda_visibility_exclusions' in php_visibility and "'events'=>[]" in php_visibility
assert 'action' in php_visibility and 'kind' in php_visibility and 'target' in php_visibility
assert all(x in visibility for x in ('class IosVisibilitySnapshot','fun mutations(', 'fun apply(', 'fun confirm('))
assert 'visibility/index.php' in api and 'affinities/record.php' in api and 'user-state/mine.php' in api
assert 'pendingVisibilityActions' in queue and 'pendingVisibilityActions(owner)' in api
assert 'check(IosVisibilityAffinityContract.confirm(persisted, command))' in api
assert 'IosPersonalMutationId.make()' in api and 'dedupe_key' in visibility
assert 'entity_type' in php_affinity and 'affinity_process_signal' in php_affinity
assert 'affinities' in php_state and 'effective_score' in php_state
assert 'affinities = root.items("affinities")' in api
assert 'IosAffinitySignals.styleWeights(user.affinities)' in (ios/'IosForYouSnapshot.kt').read_text()
assert 'IosRecentInterest.hydrate' not in ui
assert all(k not in ui for k in ('"ios_hidden_events_v1"','"ios_hidden_artists_v1"','"ios_hidden_categories_v1"','"ios_recent_styles_v1"'))
assert 'api.updateVisibility' in ui and 'api.recordOpenAffinity' in ui and 'api.recordSearchAffinity' in ui
assert 'loaded.visibility?.let' in ui and 'expectedVisibilityRevision' in ui
assert 'IosVisibilityAffinityContract' in api and 'IosVisibilitySnapshot' in center
assert 'mixed lineup' in policy or 'mixed lineup' in (root/'shared/src/commonTest/kotlin/com/example/agendavalenciana/ios/IosVisibilityAffinityCompatibilityTest.kt').read_text()
assert 'commerciallyGuaranteed' in policy
assert 'personalizationOwnerId = account?.id ?: guestReadyUserId' in ui
# Immutable Android: compare all source files against user-supplied source ZIP.
ref=Path('/mnt/data/LAGENDA_V336_ANDROID_STUDIO_v336_COMPILACIO_CORREGIDA(3).zip')
with zipfile.ZipFile(ref) as z:
    files=[n for n in z.namelist() if n.startswith('ANDROID_STUDIO/app/') and not n.endswith('/')]
    for n in files:
        actual=Path('/mnt/data/ANDROID_V336_REFERENCIA')/n
        assert actual.exists() and hashlib.sha256(actual.read_bytes()).digest()==hashlib.sha256(z.read(n)).digest(),n
with zipfile.ZipFile('/mnt/data/LAGENDA_V349_IOS_PANTALLA_COMPLETA_FEED_SCROLL (1)(2).zip') as z:
    files2=[n for n in z.namelist() if n.startswith('ANDROID_STUDIO/app/') and not n.endswith('/')]
    for n in files2:
        actual=root/n.removeprefix('ANDROID_STUDIO/')
        assert actual.exists() and actual.read_bytes()==z.read(n),n
print(f'PASS 11: server exclusion/affinity parity, identity, pending write queue, PHP contracts, immutable Android ({len(files)}+{len(files2)})')
