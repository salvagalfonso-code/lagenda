"""Pas 22: contracte cercador iOS i invariància d'Android. No prova un iPhone."""
from pathlib import Path
import hashlib,zipfile
root=Path(__file__).resolve().parents[1]
sources=root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios'
search=(sources/'IosFestivalArtistSearch.kt').read_text()
advanced=(sources/'IosAdvancedSearch.kt').read_text()
ui=(sources/'AgendaIosScreen.kt').read_text()
checks={
 'normalització d’artista': 'IosAdvancedSearch.norm' in search,
 'coincidència inequívoca': 'singleOrNull()' in search,
 'respecta suggeriment de població': 'suggestion != null && key(suggestion.value)' in search,
 'real published schedule': 'special-events/public.php' in search and 'api.festivalSchedule(id)' in ui,
 'no inventa horari': 'Horari pendent de confirmar' in ui,
 'una fitxa festival': 'seenFestivals.add(it.id)' in search,
 'ordre real': 'feedOccurrenceDate = slot.date.take(10)' in search,
 'no date if none': 'event.copy(feedOccurrenceDate = "")' in search,
 'exact artist for popup': 'IosFestivalArtistSearch.firstVerified(ev, matchedArtist, festivalProgramme)' in ui,
 'prioritza dia correcte': 'IosFestivalPopupPolicy.initialDay(festivalDays, targetSlot, selectedDay)' in ui,
 'gestiona estils i activitats': 'IosAdvancedSearch.matches(' in ui and 'IosSearchType.EVENT -> same(event.title,term)' in advanced,
 'paginació resultat search': 'val visibleFeed = remember(personalizedFeed, limit)' in ui,
 'no resultat fals amb publicitat': 'if (searchInProgress && searchedArtist != null && it.isFestival) "festival:${it.id}"' in ui,
 'cap UserDefaults de cerca personal nova': 'UserDefaults' not in search,
 'tests persistents': (root/'shared/src/commonTest/kotlin/com/example/agendavalenciana/ios/IosFestivalArtistSearchTest.kt').exists(),
}
for label,ok in checks.items(): assert ok,label
for source in ['/mnt/data/LAGENDA_V336_ANDROID_STUDIO_v336_COMPILACIO_CORREGIDA(3).zip',
               '/mnt/data/LAGENDA_V349_IOS_PANTALLA_COMPLETA_FEED_SCROLL (1)(2).zip']:
    verified=0
    with zipfile.ZipFile(source) as z:
        for name in z.namelist():
            if name.startswith('ANDROID_STUDIO/app/') and not name.endswith('/'):
                target=(root/name.removeprefix('ANDROID_STUDIO/')) if 'V349' in source else (Path('/mnt/data/ANDROID_V336_REFERENCIA')/name)
                assert target.is_file() and hashlib.sha256(target.read_bytes()).digest()==hashlib.sha256(z.read(name)).digest(),name
                verified+=1
    assert verified>=90
print('PAS22_STATIC_PASS: search, artist schedule, popup focus, no invented dates, Android immutable')
