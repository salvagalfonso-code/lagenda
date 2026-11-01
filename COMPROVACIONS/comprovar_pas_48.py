#!/usr/bin/env python3
"""Auditoria reproducible d'integritat i cablejat (NO compilació Xcode ni prova MariaDB)."""
import hashlib
import json
import re
import subprocess
import zipfile
from pathlib import Path

root = Path(__file__).resolve().parents[2]
project = root / 'ANDROID_STUDIO'
src = project / 'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios'
base_folder = Path('/mnt/data')
zips = [
    'LAGENDA_IOS_V349_PASSOS_01_AL_36_ACUMULATIU(1).zip',
    'LAGENDA_IOS_V349_PASSOS_01_AL_39_ACUMULATIU_ZIP_13_DE_16.zip',
    'LAGENDA_IOS_V349_PASSOS_01_AL_42_ACUMULATIU_ZIP_14_DE_16.zip',
    'LAGENDA_IOS_V349_PASSOS_01_AL_45_ACUMULATIU_ZIP_15_DE_16.zip',
]
results = {'pass':48,'kind':'static_and_isolated_policy_checks','base_zips':[], 'warnings':[]}
for name in zips:
    file = base_folder / name
    assert file.is_file(), f'Falta base {name}'
    with zipfile.ZipFile(file) as z:
        archive_files = {x.filename: z.read(x.filename) for x in z.infolist() if not x.is_dir()}
        missing = [n for n in archive_files if not (root/n).is_file()]
        assert not missing, f'Fitxers perduts de {name}: {missing[:10]}'
        modified = [n for n,data in archive_files.items() if (root/n).read_bytes() != data]
        unauthorized = [n for n in modified if n.startswith(('ANDROID_STUDIO/app/',
           'ANDROID_STUDIO/shared/src/androidMain/', 'SERVIDOR_'))]
        assert not unauthorized, f'Android/backend modificat respecte a {name}: {unauthorized}'
        if name == zips[-1]:
            permitted = ('ANDROID_STUDIO/shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/',
                         'ANDROID_STUDIO/iosApp/AgendaIOS/', 'ESTAT_TREBALL_PASSOS.md')
            disallowed = [n for n in modified if not n.startswith(permitted)]
            assert not disallowed, f'Canvis inesperats respecte ZIP15: {disallowed}'
            results['changed_since_zip15'] = modified
            results['source_zip15_entries'] = len(archive_files)
        results['base_zips'].append({'zip':name,'files_present':len(archive_files),
                                     'files_modified':len(modified), 'no_android_server_changes':True})
# Wiring (not an API integration test): are the shipped modules and routes referenced?
api = (src/'IosAgendaApi.kt').read_text()
screen = (src/'AgendaIosScreen.kt').read_text()
swift = '\n'.join(p.read_text() for p in (project/'iosApp/AgendaIOS').glob('*.swift'))
keychain = (project/'iosApp/AgendaIOS/AgendaKeychain.swift').read_text()
assert 'val guestId = guestSession.ensure().guestId' in api
assert 'platformWritePublicCache("ios_report_guest_id"' not in api
assert 'platformReadPublicCache("ios_report_guest_id"' not in api
ios_kotlin = '\n'.join(p.read_text() for p in src.glob('*.kt'))
for route in ('guest/create.php','sync/state.php','events/index.php', 'events/saved.php',
  'tickets/options.php','payments/create.php','payments/status.php','events/community-submit.php',
  'events/community-update.php','events/community-poster.php','seller-onboarding/index.php',
  'professional-seller/dashboard.php','professional-seller/options.php',
  'ads/create-payment.php','ads/status.php','ads/mine.php',
  'artist-store/catalog.php','artist-store/payment-create.php','artist-store/payment-status.php',
  'artist-store/donations/payment-create.php','payments/service-create.php',
  'special-events/order-redeem.php','admin/user-access-ios.php'):
    assert route in ios_kotlin, f'Ruta sense referència: {route}'
for module in ('IosForYouServerSelection','IosUnfilteredAgenda','IosSavedAgendaScreen',
 'IosCommunityAdvanced','IosNotificationCenterSettings','IosTicketCatalogPolicy',
 'IosPaidTicketPolicy','IosOfflineTicketPolicy','IosPublisherPanel',
 'IosProfessionalDashboardPanel','IosAdvertisingPanel','IosArtistStorePanel',
 'IosSpecialCommercePanel','IosSuperAdminConsole'):
    assert (src/(module+'.kt')).is_file(), module
bridge = (src/'IosAgendaApi.kt').read_text().split('interface IosSessionBridge {',1)[1].split('\n}',1)[0]
methods = re.findall(r'\bfun\s+(\w+)\s*\(',bridge)
missing_bridges = [m for m in methods if not re.search(r'\bfunc\s+'+re.escape(m)+r'\s*\(',swift)]
assert not missing_bridges, f'El pont Swift no exposa {missing_bridges}'
assert 'StripeAPI.handleURLCallback' in swift and 'paymentIntentClientSecret' in swift
assert 'AES.GCM' in keychain and 'saveOfflineTickets' in swift and 'showTicketScanner' in swift
assert 'platformWritePublicCache("ad_popup_last_day", today)' in screen
results['wiring'] = {'ios_modules':14,'api_routes_checked':24,'native_bridge_methods':len(methods),
 'reported_guest_id_now_server_issued':True,'ios_local_ad_popup_state_still_present':True}
# Syntax only, cannot type-check UIKit/Stripe dependencies in Linux.
sp = subprocess.run(['swiftc','-frontend','-parse',*[str(x) for x in (project/'iosApp/AgendaIOS').glob('*.swift')]],
                    capture_output=True,text=True)
assert sp.returncode == 0, sp.stderr
results['swift_parse'] = 'PASS (syntax only)'
# PHP helper has not been deployed, but must be valid on disk.
php = root/'SERVIDOR_SUPERADMIN_IOS/admin/user-access-ios.php'
p = subprocess.run(['php','-l',str(php)],capture_output=True,text=True)
assert p.returncode == 0, p.stderr
results['aux_admin_php_lint'] = 'PASS (not deployed)'
results['warnings'] = [
 'No hi ha compilació Kotlin/Native ni Xcode/iPhone; Gradle requereix descàrrega externa inaccessible.',
 'Cap endpoint PHP/MariaDB ni Stripe/APNs s’ha provat en producció.',
 'Els horaris guardats de festival d’Android v336 romanen en SharedPreferences: paritat servidor no acreditada.',
 'Recuperació de PaymentIntent després de tancament total pendent de contracte backend.',
 'Canvi de rols superadmin necessita migració i endpoint PHP auxiliars no desplegats.',
 'Estat publicitari de popup diari encara en cache de dispositiu; requereix decisió de backend.',
 'La barrera de Batecs en codi és 7 seguits, no 6; discrepància amb especificació.',
 'Visual i rendiment no han pogut ser verificats executant iOS.'
]
(root/'ANDROID_STUDIO/COMPROVACIONS/PAS_48_RESULTATS.json').write_text(
 json.dumps(results,ensure_ascii=False,indent=2)+'\n')
print('PAS48_AUDITORIA_ESTATICA_PASS:',results['source_zip15_entries'], 'fitxers ZIP15 preservats;',
      len(results['changed_since_zip15']),'modificats dins del perímetre iOS/estat')
print('PAS48_BRIDGE_PASS:',len(methods),'mètodes amb implementació Swift')
print('PAS48_SWIFT_PARSE_PASS; PHP_LINT_PASS; NO BUILD COMPLET NI PROVES DE PRODUCCIÓ')
