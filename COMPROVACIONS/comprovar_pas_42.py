#!/usr/bin/env python3
from pathlib import Path
import hashlib, zipfile
root=Path(__file__).resolve().parents[1]
work=root.parent
base=Path('/mnt/data/LAGENDA_IOS_V349_PASSOS_01_AL_39_ACUMULATIU_ZIP_13_DE_16.zip')
assert base.exists()
changes={
    'ANDROID_STUDIO/shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/AgendaIosScreen.kt',
    'ANDROID_STUDIO/shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosPublisherPanel.kt',
    'ANDROID_STUDIO/shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosAgendaApi.kt',
    'ANDROID_STUDIO/shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosPublisherRoles.kt',
    'ESTAT_TREBALL_PASSOS.md',
}
original=0
with zipfile.ZipFile(base) as z:
    for name in z.namelist():
        if name.endswith('/'): continue
        file=work/name
        assert file.is_file(),f'Missing original: {name}'
        if name not in changes:
            assert hashlib.sha256(file.read_bytes()).digest() == hashlib.sha256(z.read(name)).digest(), f'Unexpected modification: {name}'
            original+=1
        if name.startswith(('ANDROID_STUDIO/app/', 'SERVIDOR_','ANDROID_STUDIO/iosApp/')):
            assert file.read_bytes() == z.read(name), f'Android/Swift/server modified: {name}'

p=(root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosPublisherPanel.kt').read_text()
a=(root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosAgendaApi.kt').read_text()
l=(root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosSellerLegalPolicy.kt').read_text()
for term in ['IosSellerActivationForm','privacyViewed','salesViewed','uriHandler.openUri(state.privacyUrl)',
             'uriHandler.openUri(state.salesTermsUrl)','sellerUncertain = true', 'page == "sale"', 'Activar venda',
             'Consultar titulars, no repetir l\'alta','Confirmar titular fiscal i continuar',
             'Crear compte o iniciar sessió']:
    assert term in p,term
for term in ['seller-onboarding/index.php','sellerActivationMutex.withLock','checkPublisherIdentity(identity)',
             'IosSellerLegalPolicy.canSubmit','IosSellerLegalPolicy.postAcknowledged',
             'IosSellerLegalPolicy.getConfirmed','ownedPublishedFor(identity)', 'professional-seller/options.php', 'sellerSetSaleEnabled', 'IosSellerSalePolicy.verified']:
    assert term in a,term
for term in ['fun canSubmit','fun getConfirmed','fun postAcknowledged','validLegalUrl','noticesCurrent', 'IosSellerSalePolicy', 'fun canEnable']:
    assert term in l,term
print(f'PAS42_STATIC_PASS {original} existing files unchanged; Android, Swift and server unchanged; legal/owner/confirmation guards present')
