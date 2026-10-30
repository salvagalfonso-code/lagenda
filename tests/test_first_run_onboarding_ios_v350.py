"""Static contract checks for the iOS v350 anonymous onboarding wiring.
These do not substitute a Kotlin/Native build or real server integration test.
"""
from pathlib import Path
import re
ROOT=Path(__file__).resolve().parents[1]/'ANDROID_STUDIO'/'shared'/'src'/'commonMain'/'kotlin'/'com'/'example'/'agendavalenciana'/'ios'
ui=(ROOT/'IosFirstRunOnboarding.kt').read_text()
app=(ROOT/'AgendaIosScreen.kt').read_text()
checks={
 'Eight Android onboarding disciplines': '"MUSIC", "THEATRE", "CINEMA", "TALKS", "DANCE"' in ui and '"EXHIBITIONS", "LITERATURE", "POPULAR_CULTURE"' in ui,
 'Music artist step is optional': 'if ("MUSIC" in categories) 6 else 5' in ui,
 'Follow category without explicit subtype': 'none { it.code in valid }' in ui,
 'Guest selection saved immediately': 'LaunchedEffect(categories, subcategories, locations, artists)' in ui,
 'Dedicated one-time intro state': 'ios_first_run_state_v350' in app and '"offered"' in app and '"done"' in app,
 'No introductory modal for signed in': 'if (firstRunOpen && account == null)' in app,
 'Guest artist follows persisted': 'IosFollow("ARTIST",it,it)' in app and 'guestFollowTransfer.setGuest(nextFollows)' in app,
 'Guest category follows persisted': 'IosFirstRunPolicy.followKeys(cats, styles)' in app and 'IosFollow("CATEGORY",key,label)' in app,
 'Five shared radius values': 'PersonalizationOptions.locationRadiiKm.forEach' in ui,
 'Place choices saved': 'iosScopedCacheKey("pref_radii", null)' in app and 'chosenTowns = locations.keys' in app,
 'Places moved to account with journal': 'intro_pending_towns' in app and 'stageGuestFirstRunPlaces' in app,
 'Guest place migration in password signup/login, social and restore': app.count('stageGuestFirstRunPlaces(') >= 5,
 'Local draft is transferred when signing in': 'onDraftChanged(categories, subcategories, locations, artists)' in ui and 'onLogin = { completeFirstRun(); loginOpen = true }' in app,
 'No mandatory login': 'Continuar sense usuari' in ui and 'onContinueGuest = ::completeFirstRun' in app,
 'Server confirmed follows trigger new feed calculation': 'if (account?.id == accountId && updatedOnServer)' in app and 'feedPrefsServerRevision += 1' in app,
 'Canonical entity photographs': 'IosPersonalizationImages.photo(entityImages' in ui and 'IosPersonalizationImages.ingest(api.entityStyles())' in app,
 'Personalization can still be edited': 'if (prefOpen) IosPersonalizationScreen(' in app,
 'Separate account categories and guest categories': 'iosScopedCacheKey("pref_categories", null)' in app and 'iosScopedCacheKey("pref_categories",accountId)' in app,
 'First-run no accidental reset of server record': 'if (account != null) return' in app and 'guestPending.isNotEmpty()' in app,
}
for label,ok in checks.items():
 print(('OK' if ok else 'FAIL'),label)
assert all(checks.values()),f'{sum(checks.values())}/{len(checks)} passed'
print(f'PASS {len(checks)} static contract checks')
