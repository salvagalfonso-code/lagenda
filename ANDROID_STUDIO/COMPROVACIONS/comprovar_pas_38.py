#!/usr/bin/env python3
"""Pas 38: baseline integrity, Stripe native linkage, server contract, no fake paid state."""
from pathlib import Path
import hashlib,zipfile,plistlib,re
root=Path(__file__).resolve().parents[1]
work=root.parent
baseline=Path('/mnt/data/LAGENDA_IOS_V349_PASSOS_01_AL_36_ACUMULATIU(1).zip')
allowed={
    'ESTAT_TREBALL_PASSOS.md',
    'ANDROID_STUDIO/shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/AgendaIosScreen.kt',
    'ANDROID_STUDIO/shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosAgendaApi.kt',
    'ANDROID_STUDIO/iosApp/AgendaIOS/AgendaKeychain.swift',
    'ANDROID_STUDIO/iosApp/AgendaIOS/AgendaIOSApp.swift',
    'ANDROID_STUDIO/iosApp/AgendaIOS/Info.plist',
    'ANDROID_STUDIO/iosApp/AgendaIOS.xcodeproj/project.pbxproj',
    'ANDROID_STUDIO/iosApp/project.yml',
}
assert baseline.is_file()
untouched=0
with zipfile.ZipFile(baseline) as z:
    for name in z.namelist():
        if name.endswith('/'): continue
        p=work/name
        assert p.is_file(), 'Arxiu original desaparegut: '+name
        if name not in allowed:
            assert hashlib.sha256(p.read_bytes()).digest()==hashlib.sha256(z.read(name)).digest(), 'Regressió original: '+name
            untouched+=1
        if name.startswith(('ANDROID_STUDIO/app/','SERVIDOR_')):
            assert p.read_bytes()==z.read(name), 'Android o servidor alterat: '+name
src=root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios'
api=(src/'IosAgendaApi.kt').read_text()
ui=(src/'AgendaIosScreen.kt').read_text()
policy=(src/'IosPaidTicketPolicy.kt').read_text()
swift=(root/'iosApp/AgendaIOS/AgendaKeychain.swift').read_text()
app=(root/'iosApp/AgendaIOS/AgendaIOSApp.swift').read_text()
pbx=(root/'iosApp/AgendaIOS.xcodeproj/project.pbxproj').read_text()
proj=(root/'iosApp/project.yml').read_text()
for item in ['payments/create.php','payments/status.php','mail/ticket-receipt-now.php',
             'personalBearer(owner)','ensurePaymentSession(owner, bearer)',
             'IosPaidTicketPolicy.requirePurchasable','IosPaidTicketPolicy.requireConsistentResult',
             'put("ticket_option_id",optionId)', 'put("quantity",quantity)']:
    assert item in api,item
for item in ['IosTicketPaymentCallback','presentTicketPayment','IosPaidTicketPolicy.isPaid',
             'api.ticketPaymentStatus(start)', 'api.requestTicketReceipt(start)',
             'checkPaidOrder(start)', 'paymentStart == null', 'nativePaymentOpen',
             'Consultar estat de la comanda', 'Pagar amb Stripe']:
    assert item in ui,item
assert 'onPurchased(true)' in ui and 'onPurchased(false)' in ui
assert 'IosTicketPaymentConfirmation' in policy and 'requireValidStart' in policy
assert 'payment_pending' in ui and 'payment_failed' in policy
assert 'StripeAPI.defaultPublishableKey' in swift
assert 'PaymentSheet(paymentIntentClientSecret:' in swift
assert 'configuration.returnURL = "agendavalenciana://stripe-redirect"' in swift
assert 'callback.onComplete(result: "COMPLETED"' in swift
assert 'StripeAPI.handleURLCallback(with: url)' in app
plist=plistlib.loads((root/'iosApp/AgendaIOS/Info.plist').read_bytes())
assert 'agendavalenciana' in [s for e in plist['CFBundleURLTypes'] for s in e['CFBundleURLSchemes']]
assert 'StripePaymentSheet' in pbx and 'stripe-ios-spm' in pbx
assert 'StripePaymentSheet' in proj and '25.17.0' in proj
assert 'purchase.php' not in api[api.index('suspend fun createTicketPayment'):api.index('private suspend fun ensurePaymentSession')]
print(f'PAS38_STATIC_PASS preserved={untouched} original files, original Android/server unchanged, native/backend payment wired')
