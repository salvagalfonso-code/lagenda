#!/usr/bin/env python3
from pathlib import Path
import hashlib, zipfile
root=Path(__file__).resolve().parents[1]
work=root.parent
base=Path('/mnt/data/LAGENDA_IOS_V349_PASSOS_01_AL_39_ACUMULATIU_ZIP_13_DE_16.zip')
assert base.exists()
allowed={
    'ESTAT_TREBALL_PASSOS.md',
    'ANDROID_STUDIO/shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosAgendaApi.kt',
    'ANDROID_STUDIO/shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosPublisherPanel.kt',
    'ANDROID_STUDIO/shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/AgendaIosScreen.kt',
}
preserved=0
with zipfile.ZipFile(base) as z:
    for name in z.namelist():
        if name.endswith('/'): continue
        f=work/name
        assert f.is_file(),f'Missing {name}'
        if name not in allowed:
            assert hashlib.sha256(f.read_bytes()).digest()==hashlib.sha256(z.read(name)).digest(),f'Unexpected change to {name}'
            preserved+=1
        if name.startswith(('ANDROID_STUDIO/app/','SERVIDOR_')):
            assert f.read_bytes()==z.read(name),f'Android/server changed: {name}'
api=(root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosAgendaApi.kt').read_text()
panel=(root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosPublisherPanel.kt').read_text()
screen=(root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/AgendaIosScreen.kt').read_text()
policy=(root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosPublicationReviewPolicy.kt').read_text()
for snippet in ['events/community-submit.php','IosPublicationReviewPolicy.fromServer(',
                'publisherSubmit(draft: IosPublishDraft): IosPublicationReceipt',
                'if (identity.third.isEmpty())', '!snapshot.suspended', 'checkPublisherIdentity(identity)',
                'reply.text("review_status")', 'NON-idempotent']:
    assert snippet in api,snippet
for snippet in ['"submitted"', 'publicationReceipt = receipt', 'if (receipt.published) onPublished(receipt.eventId)',
                'Crear compte o iniciar sessió','Ara no', 'submissionUncertain = true',
                'Reintentar manualment (risc de duplicat)','Comprovar les meues publicacions',
                'La publicació està suspesa per moderació']:
    assert snippet in panel,snippet
assert 'publisherMenuOpen = false\n                            status = "Esdeveniment $id publicat' not in screen
for snippet in ['published && reviewConfirmed','published ->', 'Encara no és públic', 'reviewQueued ?:']:
    assert snippet in policy,snippet
print(f'PAS40_STATIC_PASS existing={preserved} unchanged files, Android/server immutable, guest publication receipt and no duplicate auto-retry')
