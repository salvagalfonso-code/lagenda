from pathlib import Path
s=Path(__file__).resolve().parents[1].joinpath('ANDROID_STUDIO/shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/AgendaIosScreen.kt').read_text()
checks={
 'No cancel·lació de preferències si falla userState': 'async { runCatching { api.userState() }.getOrNull() }' in s,
 'Categories aplicades quan arriba userState': 'applyAccountCategoryFollows(expectedAccount, user.follows)' in s,
 'Categories aplicades en refresc': 'applyAccountCategoryFollows(requestedAccountId, user.follows)' in s,
 'Edicions locals de categories no se sobreescriuen': 'if (platformReadPublicCache(iosScopedCacheKey("pref_categories_dirty",accountId)) == "true") return' in s,
 'Edicions en cua després del guardat': 'platformWritePublicCache(iosScopedCacheKey("pref_categories_dirty",accountId),"true")' in s,
 'POSTs CATEGORY al mateix endpoint': 'api.followEntity("CATEGORY",key,label,following)' in s,
 'Cua de categories restaurada en iniciar sessió': 'synchronizePendingCategories(expectedAccount)' in s,
 'Cua de categories restaurada en refresh': 'synchronizePendingCategories(requestedAccountId)' in s,
 'Confirmació remota abans de netejar cua': 'val confirmed = runCatching { api.userState() }.getOrNull()' in s and 'platformWritePublicCache(dirtyKey,"false")' in s,
 'Revisió de feed invalidada en confirmar categories': 'feedPrefsServerRevision += 1' in s,
 'Fallada recuperació feed preferències visible': "No s'han pogut recuperar Suggerències, Descoberta i Idioma" in s,
 'Fallada recuperació llocs visible': "No s'han pogut recuperar els pobles, comarques i radis" in s,
 'Cap doble sincronització antiga de categories en onSave': 'if (accountId != null && nextKeys != serverKeys) work {' not in s,
 'Declaració abans d’ús dins de refresh': s.index('suspend fun synchronizePendingCategories(')<s.index('fun refresh('),
}
for name,passed in checks.items():
 print(('OK' if passed else 'FAIL') + ': ' + name)
assert all(checks.values())
print('PASS',len(checks),'comprovacions estructurals; no substituïx una prova de xarxa o iPhone')
