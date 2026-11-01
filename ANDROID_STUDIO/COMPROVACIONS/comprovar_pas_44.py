#!/usr/bin/env python3
"""PAS44: audit static / hash de fitxers existents. No simula API, MySQL, ni Xcode."""
from pathlib import Path
import hashlib, zipfile
root=Path(__file__).resolve().parents[1]
work=root.parent
reference=Path('/mnt/data/LAGENDA_IOS_V349_PASSOS_01_AL_42_ACUMULATIU_ZIP_14_DE_16.zip')
changed={
    'ANDROID_STUDIO/shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosAgendaApi.kt',
    'ANDROID_STUDIO/shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosPublisherPanel.kt',
    'ESTAT_TREBALL_PASSOS.md'
}
new=[root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios'/x for x in
     ['IosAdvertisingPolicy.kt','IosAdvertisingPanel.kt']]
new+= [root/'COMPROVACIONS'/x for x in
        ['provar_pas_44.kt','comprovar_pas_44.py','PAS_44_PUBLICITAT_CAMPANYES.md']]
with zipfile.ZipFile(reference) as z:
    files=[n for n in z.namelist() if not n.endswith('/')]
    assert len(files)==465,len(files)
    for name in files:
        local=work/name
        assert local.is_file(),f'Original desaparegut: {name}'
        if name not in changed:
            assert hashlib.sha256(z.read(name)).digest()==hashlib.sha256(local.read_bytes()).digest(),f'Original canviat: {name}'
        if name.startswith(('ANDROID_STUDIO/app/', 'ANDROID_STUDIO/iosApp/', 'SERVIDOR_')):
            assert z.read(name)==local.read_bytes(),f'Android, Swift o backend canviat: {name}'
for item in new: assert item.is_file(),item
api=(root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosAgendaApi.kt').read_text()
ui=(root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosAdvertisingPanel.kt').read_text()
panel=(root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosPublisherPanel.kt').read_text()
for key in ('ads/create-payment.php','ads/status.php','ads/mine.php','ads/update.php','ads/visual.php',
            'images/upload.php','ads/visual-map.php','campaignsFor','updatedTarget','updatedVisual',
            'activeConfirmed','pendingCampaignPayment','sellerActivationMutex'):
    assert key in api or key in (root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosAdvertisingPolicy.kt').read_text(),key
for key in ('presentTicketPayment','IosAdvertisingPanel','Preparar pagament al servidor',
            'Reprendre el mateix pagament','Pujar nova creativitat','Guardar segmentació',
            'Comprovar pagament','serverPending','api.professionalDashboard()'):
    assert key in ui,key
assert 'IosAdvertisingPanel(api, expectedOwnerId, registered, own, onAuth, onNotice)' in panel
assert 'La creació d\'anuncis requereix la configuració' not in panel
print(f'PAS44_STATIC_PASS base={len(files)}, intactes={len(files)-len(changed)}; modificacions acotades a iOS; Android/Swift/servidor intactes')
