#!/usr/bin/env python3
"""Pas 34: Android public account/profile contracts and verified server state."""
from pathlib import Path
import subprocess,hashlib,zipfile
r=Path(__file__).resolve().parents[1]
src=r/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios'
a=(src/'IosAgendaApi.kt').read_text(); u=(src/'AgendaIosScreen.kt').read_text()
p=(src/'IosProfileParity.kt').read_text()
android=Path('/mnt/data/LAGENDA_ANDROID_REFERENCIA_336/ANDROID_STUDIO/app/src/main/java/com/example/agendavalenciana/data/ServerApiRepository.kt').read_text()
for endpoint in ['account/update.php','user-profiles/update.php','user-profiles/catalog.php','images/upload.php','images/owner.php','account/profile-settings.php']:
    assert endpoint in android and endpoint in a, endpoint
for field in ['username','email','home_town','display_name','bio','discipline','company_name','cast_text','publisher_name','books_text','works_text','spaces_text','remove_image']:
    assert f'put("{field}"' in a, field
for marker in ['profileWriteMutex.withLock','registeredProfileIdentity()','checkRegisteredProfileIdentity(expected)',
    'profileSnapshot(expected)','IosProfileParity.sameProfessionalFields','guestProfileIdentity()',
    'check(resolveProfileImage(owner.text("image_url")) == url)','val updated = account!!.copy']:
    assert marker in a, marker
for marker in ['AccountIdentityIosDialog','ProfileEditIosDialog','Convidat · ID de servidor',
    'Preferències i personalització','Notificacions i recordatoris',
    'Editor' if False else 'companyName = company','onSave = { updated, remove ->']:
    assert marker in u,marker
assert 'user-profiles/update.php' in android and 'images/upload.php' in android
assert 'validateIdentity' in p and 'validateProfile' in p
assert (r/'shared/src/commonTest/kotlin/com/example/agendavalenciana/ios/IosProfileParityTest.kt').is_file()
with zipfile.ZipFile('/mnt/data/LAGENDA_V336_ANDROID_STUDIO_v336_COMPILACIO_CORREGIDA(3).zip') as z:
    n=0
    for name in z.namelist():
        if not name.startswith('ANDROID_STUDIO/') or name.endswith('/'):continue
        f=Path('/mnt/data/LAGENDA_ANDROID_REFERENCIA_336')/name
        assert f.is_file() and hashlib.sha256(f.read_bytes()).digest()==hashlib.sha256(z.read(name)).digest(),name
        n+=1
    assert n>90
with zipfile.ZipFile('/mnt/data/LAGENDA_V349_IOS_PANTALLA_COMPLETA_FEED_SCROLL (1)(2).zip') as z:
    for name in z.namelist():
        if name.startswith('ANDROID_STUDIO/app/src/') and not name.endswith('/'):
            f=r/name.removeprefix('ANDROID_STUDIO/')
            assert f.is_file() and hashlib.sha256(f.read_bytes()).digest()==hashlib.sha256(z.read(name)).digest(),name
print(f'PAS34_STATIC_PASS: Android APIs, per-user profile, image, professional fields, guests; Android frozen ({n} files)')
