#!/usr/bin/env python3
"""Pas 35: access credentials, account deletion, guest continuity, immutable v336."""
from pathlib import Path
import hashlib,zipfile,subprocess
r=Path(__file__).resolve().parents[1]
base=r.parent.parent
k=r/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios'
a=(k/'IosAgendaApi.kt').read_text()
u=(k/'AgendaIosScreen.kt').read_text()
q=(k/'IosPendingServerQueue.kt').read_text()
p=(k/'IosAccountLifecyclePolicy.kt').read_text()
sw=(r/'iosApp/AgendaIOS/AgendaProfileNative.swift').read_text()
ent=(r/'iosApp/AgendaIOS/AgendaIOS.entitlements').read_text()
php=base/'LAGENDA_IOS_TREBALL_ACUMULATIU/SERVIDOR_PERFIL_IOS'
repo=(base/'ANDROID_V336_REFERENCIA/ANDROID_STUDIO/app/src/main/java/com/example/agendavalenciana/data/ServerApiRepository.kt').read_text()
for end in ['auth/login.php','auth/register.php','auth/me.php','auth/logout.php','guest/claim.php']:
    assert end in a and (end in repo or end in {'auth/me.php', 'guest/claim.php'}),end
for end in ['auth/google-ios.php','auth/apple-ios.php','account/delete-ios.php']:
    assert end in a and (php/end).is_file(),end
assert 'auth/google.php' in repo and 'guest_token' in repo
for stmt in ['accountTransitionMutex.withLock','persistVerifiedSession','bridge.loadToken() == bearer',
             'IosAccountLifecyclePolicy.requireSocialCredential','IosAccountLifecyclePolicy.requireDeleteReceipt',
             'root.bool("deleted")','discardDeletedAccount(current.id)','registeredAccount = persistVerifiedSession(response)',
             'guestLink.claimInto','onConfirmedBeforeDiscard', 'guestRegistrationNotifications']:
    assert stmt in a,stmt
assert 'if (!loginOpen) { api.signOutSecurely(); return@work }' in u
assert 'api.claimPendingGuestIdentity(expectedUserId)' in u
assert 'sessionBridge?.eraseLocalAccountData(leavingAccountId' in u
assert 'IosAccountLifecyclePolicy' in p and 'DELETED' in p and 'ANONYMIZED_LEGAL_RETENTION' in p
assert 'suspend fun discardDeletedAccount(ownerId: Int)' in q
for stmt in ['ASAuthorizationAppleIDProvider()', 'ASAuthorizationController', 'SHA256.hash',
             'ASWebAuthenticationSession', 'code_challenge_method', 'code_verifier',
             'returned.queryItems?.first(where: { $0.name == "state" })']:
    assert stmt in sw,stmt
assert 'com.apple.developer.applesignin' in ent
assert 'AgendaGoogleIOSClientID' in (r/'iosApp/AgendaIOS/Info.plist').read_text()
for name in ['auth/google-ios.php','auth/apple-ios.php','account/delete-ios.php']:
    ret=subprocess.run(['php','-l',str(php/name)],capture_output=True,text=True)
    assert ret.returncode==0,(name,ret.stderr)
assert (r/'shared/src/commonTest/kotlin/com/example/agendavalenciana/ios/IosAccountLifecyclePolicyTest.kt').is_file()
assert (r/'COMPROVACIONS/PAS_35_ACCES_VINCULACIO_BAIXA.md').is_file()
with zipfile.ZipFile(base/'LAGENDA_V336_ANDROID_STUDIO_v336_COMPILACIO_CORREGIDA(3).zip') as z:
    checked=0
    for name in z.namelist():
        if name.startswith('ANDROID_STUDIO/') and not name.endswith('/'):
            path=base/'ANDROID_V336_REFERENCIA'/name
            assert path.is_file() and hashlib.sha256(path.read_bytes()).digest()==hashlib.sha256(z.read(name)).digest(),name
            checked+=1
    assert checked>=100
with zipfile.ZipFile(base/'LAGENDA_V349_IOS_PANTALLA_COMPLETA_FEED_SCROLL (1)(2).zip') as z:
    for name in z.namelist():
        if name.startswith(('ANDROID_STUDIO/app/','SERVIDOR_')) and not name.endswith('/'):
            path=base/'LAGENDA_IOS_TREBALL_ACUMULATIU'/name
            assert path.is_file() and hashlib.sha256(path.read_bytes()).digest()==hashlib.sha256(z.read(name)).digest(),name
print(f'PAS35_STATIC_PASS Apple/Google/guest/auth/deletion, PHP syntax, immutable Android {checked} files')
