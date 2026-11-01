#!/usr/bin/env python3
"""Pas 43, únicament integritat i presència de contractes: no és prova d'iPhone/API real."""
from pathlib import Path
import hashlib
import zipfile

root = Path(__file__).resolve().parents[1]
work = root.parent
base = Path('/mnt/data/LAGENDA_IOS_V349_PASSOS_01_AL_42_ACUMULATIU_ZIP_14_DE_16.zip')
changed = {
    'ANDROID_STUDIO/shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosAgendaApi.kt',
    'ANDROID_STUDIO/shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosPublisherPanel.kt',
    'ESTAT_TREBALL_PASSOS.md'
}
new = [
    root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosProfessionalSellerContract.kt',
    root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosProfessionalDashboardPanel.kt',
    root/'COMPROVACIONS/pas43_policy_check.kt',
    root/'COMPROVACIONS/comprovar_pas_43.py',
    root/'COMPROVACIONS/PAS_43_PANELL_PROFESSIONAL.md'
]
with zipfile.ZipFile(base) as z:
    saved = [x for x in z.namelist() if not x.endswith('/')]
    assert len(saved) == 465, len(saved)
    for name in saved:
        file = work/name
        assert file.is_file(), f'Falta fitxer anterior: {name}'
        if name not in changed:
            assert hashlib.sha256(file.read_bytes()).digest() == hashlib.sha256(z.read(name)).digest(), f'Canvi inesperat: {name}'
        if name.startswith(('ANDROID_STUDIO/app/', 'ANDROID_STUDIO/iosApp/', 'SERVIDOR_')):
            assert file.read_bytes() == z.read(name), f'Android, servidor o Swift tocat: {name}'
for file in new:
    assert file.is_file(), f'Falta fitxer nou: {file}'
api=(root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosAgendaApi.kt').read_text()
ui=(root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosProfessionalDashboardPanel.kt').read_text()
panel=(root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosPublisherPanel.kt').read_text()
policy=(root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosProfessionalSellerContract.kt').read_text()
for term in ['professional-seller/dashboard.php', 'professional-seller/options.php', 'professional-seller/assign-company.php',
             'professional-seller/invoices.php', 'events/ticket-drafts.php', 'seller-onboarding/index.php',
             'proIdentity()', 'checkPublisherIdentity(identity)', 'sellerActivationMutex.withLock',
             'professionalSetSaleEnabled', 'professionalSaveTicketOption', 'professionalAssignCompany',
             'professionalRegisterExternalInvoice', 'verifiedUpsert', 'confirmedAssignment', 'confirmedInvoice']:
    assert term in api, f'Falta API {term}'
for term in ['"Resum"', '"Vendes"', '"Esdeveniments"', '"Factures"', '"Empreses"',
             'IosProfessionalTicketOptionForm', 'IosProfessionalInvoiceForm',
             'Compartir vendes CSV', 'companyFilter', 'period', 'currentLocalDateIso()', 'uncertain',
             'IosNativeBridge.current?.shareText']:
    assert term in ui, f'Falta UI {term}'
for term in ['IosPublishAction.TICKETS -> { page = "dashboard" }', 'page == "dashboard"', 'IosProfessionalDashboardPanel']:
    assert term in panel, f'Falta integració {term}'
for term in ['fun validSummary', 'fun validOption', 'fun verifiedUpsert', 'fun confirmedInvoice',
             'fun reportCsv', 'fun canAssign', 'fun cents']:
    assert term in policy, f'Falta política {term}'
print(f'PAS43_STATIC_PASS base={len(saved)}, intactes={len(saved)-len(changed)}, modificats_documentacio_iOS={len(changed)}, nous={len(new)}; Android/Swift/servidor originals intactes')
