#!/usr/bin/env python3
"""PAS45 — comprovació d'immutabilitat i contractes. No substitueix KMP/Xcode/Stripe/MariaDB."""
from pathlib import Path
import zipfile, hashlib
root = Path(__file__).resolve().parents[1]
work = root.parent
base = Path('/mnt/data/LAGENDA_IOS_V349_PASSOS_01_AL_42_ACUMULATIU_ZIP_14_DE_16.zip')
allowed_modified = {
    'ESTAT_TREBALL_PASSOS.md',
    'ANDROID_STUDIO/shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosAgendaApi.kt',
    'ANDROID_STUDIO/shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosPublisherPanel.kt',
    'ANDROID_STUDIO/shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/AgendaIosScreen.kt',
    'ANDROID_STUDIO/shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosArtistPagePolicy.kt',
}
with zipfile.ZipFile(base) as z:
    names = [n for n in z.namelist() if not n.endswith('/')]
    assert len(names) == 465, len(names)
    for name in names:
        dest = work/name
        assert dest.is_file(), f'Original absent: {name}'
        if name not in allowed_modified:
            assert hashlib.sha256(dest.read_bytes()).digest() == hashlib.sha256(z.read(name)).digest(), f'Original no autoritzat modificat: {name}'
        if name.startswith(('ANDROID_STUDIO/app/', 'ANDROID_STUDIO/iosApp/', 'SERVIDOR_')):
            assert dest.read_bytes() == z.read(name), f'Android, Swift o servidor modificat: {name}'
for name in ('IosArtistStorePolicy.kt','IosArtistStorePanel.kt','IosAdvertisingPanel.kt','IosAdvertisingPolicy.kt'):
    assert (root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios'/name).is_file(), name
api=(root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosAgendaApi.kt').read_text()
for route in ('artist-store/catalog.php','artist-store/save-product.php','artist-store/delete-product.php',
              'artist-store/payment-create.php','artist-store/payment-status.php',
              'artist-store/manage/orders.php','artist-store/manage/mark-shipped.php',
              'professional-profiles/mine.php'):
    assert route in api, route
artist=(root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/AgendaIosScreen.kt').read_text()
pub=(root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosPublisherPanel.kt').read_text()
ui=(root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosArtistStorePanel.kt').read_text()
assert 'IosArtistStoreCheckout(api, product' in artist
assert 'IosArtistStoreManager(api, expectedOwnerId, onNotice)' in pub
assert 'IosPublishAction.DONATIONS -> page = "donations"' in pub
for key in ('presentTicketPayment', 'artistStoreStatus', 'artistStoreShip', 'artistStoreCreatePayment',
            'artistStoreDelete', 'artistStoreSave', 'artistStoreOrders', 'managedArtistStoreNames'):
    assert key in ui, key
assert 'writeCache(' not in ui and 'UserDefaults' not in ui
print(f'PAS45_STATIC_PASS originals={len(names)}, intactes={len(names)-len(allowed_modified)}, Android/Swift/servidor intactes')
