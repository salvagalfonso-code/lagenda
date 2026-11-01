from pathlib import Path
import hashlib, zipfile
ROOT=Path(__file__).resolve().parents[2]
BASE=Path('/mnt/data/LAGENDA_IOS_V349_PASSOS_01_AL_45_ACUMULATIU_ZIP_15_DE_16.zip')
IOS=ROOT/'ANDROID_STUDIO/shared/src/commonMain/kotlin/com/example/agendavalenciana/ios'
api=(IOS/'IosAgendaApi.kt').read_text()
ui=(IOS/'IosSpecialCommercePanel.kt').read_text()
screen=(IOS/'AgendaIosScreen.kt').read_text()
publisher=(IOS/'IosPublisherPanel.kt').read_text()
roles=(IOS/'IosPublisherRoles.kt').read_text()
swift=(ROOT/'ANDROID_STUDIO/iosApp/AgendaIOS/AgendaKeychain.swift').read_text()
assert BASE.is_file()
with zipfile.ZipFile(BASE) as z:
    base={x.filename:hashlib.sha256(z.read(x.filename)).digest() for x in z.infolist() if not x.is_dir()}
modified=[]; absent=[]
for name, digest in base.items():
    dest=ROOT/name
    if not dest.is_file(): absent.append(name)
    elif hashlib.sha256(dest.read_bytes()).digest()!=digest: modified.append(name)
assert not absent, absent
assert all(x.startswith('ANDROID_STUDIO/shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/') or x=='ANDROID_STUDIO/iosApp/AgendaIOS/AgendaKeychain.swift' or x=='ESTAT_TREBALL_PASSOS.md' for x in modified), modified
assert len(base)==481
paths=['artist-store/donations/config.php','artist-store/donations/save-config.php',
       'artist-store/donations/payment-create.php','artist-store/donations/payment-status.php',
       'special-events/public.php','special-events/manage/my-events.php',
       'special-events/manage/config.php','special-events/manage/save-service.php',
       'special-events/manage/delete-service.php','special-events/manage/save-product.php',
       'special-events/manage/delete-product.php','payments/service-create.php',
       'payments/service-status.php','special-events/order-inspect.php','special-events/order-redeem.php']
assert all(x in api for x in paths)
assert all(x in ui for x in ['IosArtistDonationManager','IosArtistDonationCheckout','IosSpecialServicesDialog',
                              'IosSpecialServicesManager','IosSpecialServicesRedeem','showSpecialServiceQr',
                              'scanSpecialServiceQr','createSpecialPayment','inspectSpecialOrder','redeemSpecialOrder'])
assert 'IosArtistDonationCheckout' in screen and 'IosSpecialServicesDialog' in screen
assert 'IosArtistDonationManager' in publisher and 'IosSpecialServicesManager' in publisher
assert 'SPECIAL_SERVICES' in publisher and 'SPECIAL_SERVICES' in roles
assert 'func showSpecialServiceQr' in swift and 'func scanSpecialServiceQr' in swift
assert 'payments/service-create.php' not in swift  # only Kotlin PHP client owns service create
print('PASS: 481 previous files present;',len(modified),'allowed iOS changes; all 15 API routes and UI/native wiring.')
print('Modified:', ', '.join(modified))
