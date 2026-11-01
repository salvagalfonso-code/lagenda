#!/usr/bin/env python3
"""Pas 7: source/contract checks, not a Kotlin/Native build or live API test."""
from pathlib import Path
import re, subprocess, hashlib, zipfile
root=Path(__file__).resolve().parents[1]
project=root.parent.parent
ref=project/'ANDROID_V336_REFERENCIA/ANDROID_STUDIO/app/src/main/java/com/example/agendavalenciana/model'
ios=root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios'
tax=(ios/'IosCategoryTaxonomy.kt').read_text()
android=(ref/'CategoryTaxonomy.kt').read_text()
model=(ref/'Models.kt').read_text()
ref_categories=re.findall(r'^\s*([A-Z_]+)\("([^"\n]+)"\)',model[model.index('enum class CulturalCategory'):model.index('data class TicketOption')],re.M)
ref_subcats=re.findall(r'CulturalSubcategory\(CulturalCategory\.([A-Z_]+),\s*(\d+),\s*"([A-Z_]+__[A-Z_]+)",\s*"([^"\n]+)"',android)
ios_categories=re.findall(r'IosCategoryChoice\("([A-Z_]+)", "([^"\n]+)"\)',tax)
ios_subcats=re.findall(r'IosCulturalSubcategory\("([A-Z_]+)", (\d+), "([A-Z_]+__[A-Z_]+)", "([^"\n]+)"',tax)
assert len(ref_categories)==len(ios_categories)==12 and ref_categories==ios_categories
assert len(ref_subcats)==len(ios_subcats)==55 and ref_subcats==ios_subcats
assert '"${categoryKey}__SUB_$index"' in tax
assert 'fun fromServer(follows: List<IosFollow>)' in tax and 'mainKey(it.key)' in tax
assert 'subcategoryCode(it.key)' in tax and 'fun mutations(existing: List<IosFollow>' in tax
ui=(ios/'AgendaIosScreen.kt').read_text()
feed=(ios/'IosPersonalFeedPolicy.kt').read_text()
api=(ios/'IosAgendaApi.kt').read_text()
assert 'IosCategoryTaxonomy.fromServer(loadedUser.follows)' in ui
assert 'IosCategoryTaxonomy.fromServer(newUser.follows)' in ui
assert 'IosCategoryTaxonomy.mutations(user.follows, desiredCategoryFollows)' in ui
assert 'api.followEntity("CATEGORY", change.key, change.label, change.following)' in ui
assert '"ios_pref_subcategories_v1" to IosPersonalEncoded' not in ui
assert 'loaded.personalSubcategories()?.let' not in ui
assert 'IosCategoryTaxonomy.categories.map { it.key }' in ui
assert 'IosCategoryTaxonomy.subcategories' in ui
assert 'IosCategoryTaxonomy.matchesEvent(e.category, e.styles, selectedTaxonomy)' in feed
assert '"user-state/follow.php"' in api and '"user-state/mine.php"' in api
assert 'queuedWrite(id, "post"' in api
assert (root/'shared/src/commonTest/kotlin/com/example/agendavalenciana/ios/IosCategoryTaxonomyTest.kt').exists()
# Compare the exact original v336 reference files with the ZIP, not the mutable iOS work tree.
original=project/'LAGENDA_V336_ANDROID_STUDIO_v336_COMPILACIO_CORREGIDA(3).zip'
with zipfile.ZipFile(original) as archive:
    checked=0
    for name in archive.namelist():
        if name.startswith('ANDROID_STUDIO/') and name.endswith('.kt') and '/app/' in name:
            current=project/'ANDROID_V336_REFERENCIA'/name
            assert current.exists() and hashlib.sha256(current.read_bytes()).digest() == hashlib.sha256(archive.read(name)).digest(),name
            checked+=1
assert checked>=75,checked
print('PASS 07: taxonomy 12+55 exact, server category IDs, UI+feed, offline follow queue and',checked,'immutable Android Kotlin sources')
