#!/usr/bin/env python3
"""Pas 3: proves de contracte (sense accés de producció ni compilació iPhone)."""
from pathlib import Path
import hashlib, zipfile, re

project = Path(__file__).resolve().parents[1]
base = project.parent.parent
code = project/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios'
api = (code/'IosAgendaApi.kt').read_text()
session = (code/'IosGuestSession.kt').read_text()
link = (code/'IosGuestAccountLinker.kt').read_text()
screen = (code/'AgendaIosScreen.kt').read_text()
swift = (project/'iosApp/AgendaIOS/AgendaKeychain.swift').read_text()
php = (base/'audit_lagenda/web/api/guest/claim.php').read_text()
register = (base/'audit_lagenda/web/api/auth/register.php').read_text()
tests = (project/'shared/src/commonTest/kotlin/com/example/agendavalenciana/ios/IosGuestAccountLinkerTest.kt').read_text()

assert 'guest/claim.php' in api and 'put("guest_token", guestToken)' in api
assert 'guestBeforeRegistration?.let { put("guest_token", it.token) }' in api
assert 'response.bool("guest_upgraded_in_place")' in api
assert 'claimPendingGuestIdentity(expectedAccountId: Int)' in api
assert 'suspend fun claimInto(registeredUserId: Int, registeredBearer: String)' in link
assert 'response.text("merged_guest_user_id").toIntOrNull() == guest.userId' in link
assert 'response.text("into_user_id").toIntOrNull() == registeredUserId' in link
assert link.index('val response = claimOnServer(') < link.rindex('guestSession.discardVerified(guest)')
assert 'check(clearSecure())' in session and 'cached = null' in session
assert 'func clearGuestIdentity() -> Bool' in swift and 'SecItemDelete(guestQuery as CFDictionary)' in swift
assert 'if (outcome.isFailure' in screen and 'delay(60_000L)' in screen
assert 'loadAccountPages()' in screen and 'LaunchedEffect(account?.id)' in screen
assert 'guest_token' in register and 'guest_upgraded_in_place' in register
assert 'claimed_into_user_id' in php and 'already_claimed' in php and '$p->beginTransaction()' in php
assert 'agenda_user_client_state' in php and 'user_follows' in php and 'user_event_states' in php
assert 'community_members' in php and 'agenda_saved_lists' in php and 'agenda_push_devices' in php
for method in ('mergesExistingAccountOnlyAfterMatchingServerConfirmation', 'failedOrMismatchedServerResultPreservesGuest', 'alreadyClaimedIsIdempotentAndAllowsCleanup', 'inPlaceRegistrationKeepsUserIdAndSkipsClaimEndpoint', 'guestTokenIsRetainedIfKeychainCannotErase'):
    assert method in tests
assert 'UserDefaults.standard.set(json' not in swift

# Existing Android v336 and the server supplementary folders in v349 are hash-identical.
with zipfile.ZipFile(base/'LAGENDA_V336_ANDROID_STUDIO_v336_COMPILACIO_CORREGIDA(3).zip') as z:
    members = [n for n in z.namelist() if n.startswith('ANDROID_STUDIO/') and not n.endswith('/')]
    for n in members:
        assert hashlib.sha256(z.read(n)).digest() == hashlib.sha256((base/'ANDROID_V336_REFERENCIA'/n).read_bytes()).digest(), n
print(f'PASS: Android v336 unchanged ({len(members)} files)')
with zipfile.ZipFile(base/'LAGENDA_V349_IOS_PANTALLA_COMPLETA_FEED_SCROLL (1)(2).zip') as z:
    members = [n for n in z.namelist() if n.startswith(('ANDROID_STUDIO/app/', 'SERVIDOR_')) and not n.endswith('/')]
    for n in members:
        assert hashlib.sha256(z.read(n)).digest() == hashlib.sha256((base/'LAGENDA_IOS_TREBALL_ACUMULATIU'/n).read_bytes()).digest(), n
print(f'PASS: embedded Android/server unchanged ({len(members)} files)')
print('PASS: guest claim via bearer, server confirmation, secure cleanup, retry and in-place registration')
