#!/usr/bin/env python3
"""Step 36: role, +, publishing, publisher permissions, seller API and frozen Android."""
from pathlib import Path
import zipfile,hashlib
r=Path(__file__).resolve().parents[1]
base=r.parent.parent
src=r/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios'
a=(src/'IosAgendaApi.kt').read_text()
u=(src/'AgendaIosScreen.kt').read_text()
p=(src/'IosPublisherRoles.kt').read_text()
ui=(src/'IosPublisherPanel.kt').read_text()
with zipfile.ZipFile(base/'web.zip') as z:
    for endpoint in ['account/publisher-access.php','account/publication-trust.php',
        'events/my-published-v312.php','events/community-submit.php',
        'events/community-update.php','professional-profiles/request.php',
        'seller-onboarding/index.php']:
        assert 'api/'+endpoint in z.namelist(), endpoint
        assert endpoint in a, endpoint
for marker in ['activePersonalId() == identity.first','checkPublisherIdentity(identity)',
               'publisherSubmit','publisherEdit','publisherSnapshot','sellerActivate',
               'requestProfessionalClaim','ownedPublishedFor(identity)',
               'draft.validate()', 'ticket_setup_ready', 'notices_current']:
    assert marker in a,marker
for marker in ['visibleActions','canPublishDirectly','serverConfirmed','professionalVerified',
               'CREATE_AD','EDIT_EVENTS','SUPER_ADMIN','IosSellerActivation']:
    assert marker in p,marker
for marker in ['IosPublisherPanel','publisherMenuOpen = true', 'serverPublisherAccess',
               'IosPublisherRoles::isProfessional','IosPublisherRoles.isSuperAdmin']:
    assert marker in u,marker
for marker in ['IosPublisherEventEditor','IosProfessionalClaimForm','IosSellerActivationForm',
               'IosPublisherRoles.allows','api.sellerActivate','api.publisherSubmit','api.publisherEdit',
               'api.requestProfessionalClaim','api.sellerOnboardingStatus']:
    assert marker in ui,marker
assert 'tab = "Perfil"' not in u[u.find('AgendaTopAction("plus"'):u.find('AgendaTopAction("sliders"')]
assert (r/'shared/src/commonTest/kotlin/com/example/agendavalenciana/ios/IosPublisherRolesTest.kt').is_file()
assert (r/'COMPROVACIONS/PAS_36_ROLS_MENU_PROFESSIONAL.md').is_file()
with zipfile.ZipFile(base/'LAGENDA_V336_ANDROID_STUDIO_v336_COMPILACIO_CORREGIDA(3).zip') as z:
    count=0
    for name in z.namelist():
        if name.startswith('ANDROID_STUDIO/') and not name.endswith('/'):
            dest=base/'ANDROID_V336_REFERENCIA'/name
            assert dest.is_file() and hashlib.sha256(dest.read_bytes()).digest() == hashlib.sha256(z.read(name)).digest(),name
            count+=1
    assert count>=100
with zipfile.ZipFile(base/'LAGENDA_V349_IOS_PANTALLA_COMPLETA_FEED_SCROLL (1)(2).zip') as z:
    for name in z.namelist():
        if name.startswith(('ANDROID_STUDIO/app/','SERVIDOR_')) and not name.endswith('/'):
            dest=base/'LAGENDA_IOS_TREBALL_ACUMULATIU'/name
            assert dest.is_file() and dest.read_bytes()==z.read(name),name
print('PAS36_STATIC_PASS roles/publisher/ticket-own/edit/claim/identity/Android', count)
