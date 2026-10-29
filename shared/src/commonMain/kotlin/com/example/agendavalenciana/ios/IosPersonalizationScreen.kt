package com.example.agendavalenciana.ios

import com.example.agendavalenciana.model.DiscoveryLevel
import com.example.agendavalenciana.model.PersonalizationOptions
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

private val personalizationText = Color(0xFF191919)
private val selectedBackground = Color(0xFFEBDFFB)
private val rowBackground = Color(0xFFF8F8F8)
private val mutedText = Color(0xFF666666)
private val tabs = listOf("Cats", "Artistes", "Llocs", "Bloq.", "Prefs")
private val tabSymbols = listOf("◎", "♫", "♧", "⊘", "☷")

/** La mateixa pantalla de 5 seccions que Android, en lloc d'un formulari dins del feed.
 * Canvis locals en prémer enrere; els follows i les campanetes s'envien per API des de la pantalla pare.
 */
@Composable
internal fun IosPersonalizationScreen(
    categories: Set<String>, towns: Set<String>, hiddenEventIds: Set<Int>,
    hiddenArtists: Set<String>, hiddenCategories: Set<String>,
    events: List<AgendaEventSummary>, townOptions: List<String>,
    subcategories: Set<String>, comarques: Set<String>, radii: Map<String, Int>,
    suggestions: Boolean, discovery: DiscoveryLevel, valencian: Boolean,
    artistFollows: Set<String>, categoryFollows: Set<String>,
    entityImages: Map<String,String>,
    alertedArtists: Set<String>, accountSignedIn: Boolean,
    notificationsEnabled: Boolean, onNotificationsEnabled: (Boolean) -> Unit,
    onFollowArtist: (String) -> Unit,
    onArtistAlerts: (String, Boolean) -> Unit,
    onResetLearning: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (Set<String>, Set<String>, Set<Int>, Set<String>, Set<String>,
             Set<String>, Set<String>, Map<String, Int>, Boolean, DiscoveryLevel, Boolean) -> Unit
) {
    val taxonomy = IosPersonalizationTaxonomy
    val allowedCats = taxonomy.categories.map { it.code }.toSet() + "CHILDREN"
    val allowedSubs = taxonomy.subcategories.map { it.code }.toSet()
    val initialKeys = (categories + subcategories + categoryFollows)
        .map { taxonomy.canonicalFollowKey(it) }.toSet()
    var selectedKeys by remember { mutableStateOf(initialKeys.filter { it in allowedCats || it in allowedSubs }.toSet()) }
    val extraStyles = remember(subcategories) { subcategories.filterNot { taxonomy.canonicalFollowKey(it) in allowedSubs }.toSet() }
    var selectedTowns by remember { mutableStateOf(towns) }
    var selectedComarques by remember { mutableStateOf(comarques) }
    var selectedRadii by remember { mutableStateOf(radii) }
    var hiddenEventsDraft by remember { mutableStateOf(hiddenEventIds) }
    var hiddenArtistsDraft by remember { mutableStateOf(hiddenArtists) }
    var hiddenCategoriesDraft by remember { mutableStateOf(hiddenCategories) }
    var suggestionsDraft by remember { mutableStateOf(suggestions) }
    var valencianDraft by remember { mutableStateOf(valencian) }
    var discoveryDraft by remember { mutableStateOf(discovery) }
    var localArtistFollows by remember(artistFollows) { mutableStateOf(artistFollows) }
    var localArtistAlerts by remember(alertedArtists) { mutableStateOf(alertedArtists) }
    var section by remember { mutableIntStateOf(0) }
    var search by remember { mutableStateOf("") }
    var locationSearch by remember { mutableStateOf("") }
    var expandedCategory by remember { mutableStateOf("") }
    var menuFor by remember { mutableStateOf("") }
    var radiusMenuFor by remember { mutableStateOf("") }
    var moreLimit by remember { mutableIntStateOf(20) }
    var confirmBlock by remember { mutableStateOf<Pair<String, String>?>(null) }
    var confirmReset by remember { mutableStateOf(false) }
    val scroll = rememberLazyListState()
    LaunchedEffect(section) { scroll.scrollToItem(0); search = ""; moreLimit = 20 }
    val allArtists = remember(events, artistFollows, hiddenArtists) {
        (events.flatMap { it.artists + it.schedule.map { s -> s.artist } } + artistFollows + hiddenArtists)
            .map(String::trim).filter(String::isNotBlank).distinct().sorted()
    }
    fun save() {
        onSave(selectedKeys.intersect(allowedCats), selectedTowns, hiddenEventsDraft,
            hiddenArtistsDraft, hiddenCategoriesDraft,
            selectedKeys.intersect(allowedSubs) + extraStyles,
            selectedComarques, selectedRadii.filterKeys { it in selectedTowns },
            suggestionsDraft, discoveryDraft, valencianDraft)
    }
    fun close() { save(); onDismiss() }

    Dialog(onDismissRequest = ::close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = Color.White, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 17.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = ::close, contentPadding = PaddingValues(4.dp)) { Text("‹", color = personalizationText, fontSize = 30.sp) }
                    Text("Personalització", fontSize = 23.sp, fontWeight = FontWeight.Black, color = personalizationText,
                        modifier = Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    tabs.forEachIndexed { index, label ->
                        Surface(modifier = Modifier.weight(1f).height(66.dp).clickable { section = index },
                            shape = RoundedCornerShape(9.dp),
                            color = if (section == index) selectedBackground else Color(0xFFFAFAFA),
                            border = if (section == index) BorderStroke(1.dp, Color(0xFF8C819D)) else null) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Text(tabSymbols[index], fontSize = 23.sp, color = if (section == index) Color(0xFF27134D) else Color.DarkGray)
                                Text(label, fontSize = 12.sp, fontWeight = if (section == index) FontWeight.Bold else FontWeight.Medium)
                            }
                        }
                    }
                }
                LazyColumn(state = scroll, modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 13.dp, end = 13.dp, top = 17.dp, bottom = 26.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    when (section) {
                        0 -> {
                            item { PersonalSectionTitle("Categories culturals", "✓ Seguint · ⋮ Opcions")
                                PersonalSearch(search, { search = it }, "Busca una categoria") }
                            val filtered = taxonomy.categories.filter { it.label.contains(search, true) ||
                                taxonomy.childrenOf(it.code).any { sub -> sub.label.contains(search, true) } }
                            items(filtered, key = { it.code }) { cat ->
                                val children = taxonomy.childrenOf(cat.code)
                                val expanded = expandedCategory == cat.code ||
                                    (search.isNotBlank() && children.any { it.label.contains(search, true) })
                                val full = cat.code in selectedKeys
                                val selectedCount = children.count { taxonomy.isSelected(selectedKeys,cat.code,it.code) }
                                val blocked = cat.label in hiddenCategoriesDraft || cat.code in hiddenCategoriesDraft
                                PersonalCard {
                                    Row(Modifier.fillMaxWidth().heightIn(min = 58.dp).padding(horizontal = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically) {
                                        PersonalAvatar(cat.label, IosPersonalizationImages.photo(entityImages,"CATEGORY",cat.label).ifBlank { IosPersonalizationImages.photo(entityImages,"CATEGORY",cat.code) })
                                        Spacer(Modifier.width(9.dp))
                                        Column(Modifier.weight(1f).clickable { expandedCategory = if (expandedCategory == cat.code) "" else cat.code }) {
                                            Text(cat.label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text(when { blocked -> "Bloquejada"; full -> "Tota la categoria"
                                                selectedCount > 0 -> "$selectedCount de ${children.size} subcategories"
                                                else -> "Desplega per triar" }, fontSize = 11.sp, color = mutedText)
                                        }
                                        TextButton(onClick = {
                                            if (blocked) hiddenCategoriesDraft = hiddenCategoriesDraft - cat.label - cat.code
                                            selectedKeys = taxonomy.toggleCategory(selectedKeys,cat.code)
                                        }, contentPadding = PaddingValues(2.dp), modifier = Modifier.width(36.dp)) {
                                            Text(if (full) "✓" else "+", fontSize = 25.sp, color = personalizationText)
                                        }
                                        TextButton(onClick = { expandedCategory = if (expandedCategory == cat.code) "" else cat.code },
                                            contentPadding = PaddingValues(2.dp), modifier = Modifier.width(29.dp)) {
                                            Text(if (expanded) "⌃" else "⌄", fontSize = 22.sp, color = personalizationText)
                                        }
                                        Box {
                                            TextButton(onClick = { menuFor = cat.code }, contentPadding = PaddingValues(1.dp),
                                                modifier = Modifier.width(26.dp)) { Text("⋮", fontSize = 23.sp, color = personalizationText) }
                                            DropdownMenu(expanded = menuFor == cat.code, onDismissRequest = { menuFor = "" }) {
                                                DropdownMenuItem(text = { Text(if (blocked) "Tornar a mostrar" else "No mostrar categoria") }, onClick = {
                                                    menuFor = ""
                                                    if (blocked) hiddenCategoriesDraft = hiddenCategoriesDraft - cat.label - cat.code
                                                    else confirmBlock = "category" to cat.label
                                                })
                                            }
                                        }
                                    }
                                    if (expanded) children.filter { search.isBlank() || cat.label.contains(search,true) || it.label.contains(search,true) }
                                        .forEach { sub ->
                                            val checked = taxonomy.isSelected(selectedKeys,cat.code,sub.code)
                                            Row(Modifier.fillMaxWidth().heightIn(min = 39.dp).padding(start = 46.dp, end = 9.dp),
                                                verticalAlignment = Alignment.CenterVertically) {
                                                Text(sub.label, modifier = Modifier.weight(1f).clickable {
                                                    hiddenCategoriesDraft = hiddenCategoriesDraft - sub.code
                                                    selectedKeys = taxonomy.toggleSubcategory(selectedKeys, sub.code)
                                                }, fontSize = 12.sp)
                                                Checkbox(checked, onCheckedChange = {
                                                    hiddenCategoriesDraft = hiddenCategoriesDraft - sub.code
                                                    selectedKeys = taxonomy.toggleSubcategory(selectedKeys, sub.code)
                                                }, modifier = Modifier.size(34.dp))
                                                TextButton(onClick = {
                                                    if (sub.code in hiddenCategoriesDraft) {
                                                        hiddenCategoriesDraft = hiddenCategoriesDraft - sub.code
                                                    } else {
                                                        if (taxonomy.isSelected(selectedKeys,sub.categoryCode,sub.code))
                                                            selectedKeys = taxonomy.toggleSubcategory(selectedKeys,sub.code)
                                                        hiddenCategoriesDraft = hiddenCategoriesDraft + sub.code
                                                    }
                                                }, modifier = Modifier.width(30.dp), contentPadding = PaddingValues(0.dp)) {
                                                    Text(if (sub.code in hiddenCategoriesDraft) "↺" else "⊘", fontSize = 17.sp)
                                                }
                                            }
                                        }
                                }
                            }
                            if (search.isBlank() || "infantil".contains(search,true)) item {
                                PersonalCard {
                                    Row(Modifier.fillMaxWidth().height(55.dp).clickable {
                                        selectedKeys = taxonomy.toggleCategory(selectedKeys,"CHILDREN")
                                    }.padding(horizontal = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                        PersonalAvatar("Infantil", "")
                                        Spacer(Modifier.width(9.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text("Infantil", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                            Text("Transversal · totes les categories", fontSize = 11.sp, color = mutedText)
                                        }
                                        Checkbox("CHILDREN" in selectedKeys, onCheckedChange = {
                                            selectedKeys = taxonomy.toggleCategory(selectedKeys,"CHILDREN")
                                        })
                                    }
                                }
                            }
                        }
                        1 -> {
                            item { PersonalSectionTitle("Artistes i grups", "✓ Seguint · ⋮ Opcions")
                                PersonalSearch(search, { search = it }, "Busca un artista o grup") }
                            val visible = allArtists.filter { it.contains(search,true) && it !in hiddenArtistsDraft }
                                .sortedWith(compareByDescending<String> { it in localArtistFollows }.thenBy { it })
                            items(visible.take(moreLimit), key = { "artist_$it" }) { name ->
                                val following = name in localArtistFollows
                                PersonalCard {
                                    Row(Modifier.fillMaxWidth().height(51.dp).padding(horizontal = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically) {
                                        PersonalAvatar(name, IosPersonalizationImages.photo(entityImages,"ARTIST",name))
                                        Spacer(Modifier.width(9.dp))
                                        Text(name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                                            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                        if (name in localArtistAlerts) Text("♧", fontSize = 12.sp)
                                        TextButton(onClick = {
                                            if (accountSignedIn) {
                                                localArtistFollows = if (following) localArtistFollows - name else localArtistFollows + name
                                                onFollowArtist(name)
                                            }
                                        }, modifier = Modifier.width(42.dp), contentPadding = PaddingValues(0.dp)) {
                                            Text(if (following) "✓" else "+", color = personalizationText, fontSize = 25.sp)
                                        }
                                        Box {
                                            TextButton(onClick = { menuFor = name }, modifier = Modifier.width(28.dp),
                                                contentPadding = PaddingValues(0.dp)) { Text("⋮", fontSize = 23.sp, color = personalizationText) }
                                            DropdownMenu(expanded = menuFor == name, onDismissRequest = { menuFor = "" }) {
                                                DropdownMenuItem(enabled = following && accountSignedIn,
                                                    text = { Text(if (name in localArtistAlerts) "Desactivar avisos" else "Activar avisos") },
                                                    onClick = {
                                                        menuFor = ""
                                                        val next = name !in localArtistAlerts
                                                        localArtistAlerts = if (next) localArtistAlerts + name else localArtistAlerts - name
                                                        onArtistAlerts(name,next)
                                                    })
                                                DropdownMenuItem(text = { Text("No mostrar artista") },
                                                    onClick = { menuFor = ""; confirmBlock = "artist" to name })
                                            }
                                        }
                                    }
                                }
                            }
                            if (!accountSignedIn) item { Text("Inicia sessió per seguir artistes i activar avisos.", fontSize = 12.sp, color = mutedText) }
                            if (visible.size > moreLimit) item { OutlinedButton(onClick = { moreLimit += 20 }, modifier = Modifier.fillMaxWidth()) { Text("Mostrar 20 artistes més") } }
                        }
                        2 -> {
                            item { Text("Les meues localitzacions", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                                if (!accountSignedIn) Text("Els llocs es conserven en este dispositiu. Inicia sessió per sincronitzar-los.", fontSize = 12.sp, color = mutedText) }
                            items(selectedTowns.sorted(), key = { "town_$it" }) { town ->
                                PersonalCard {
                                    Row(Modifier.fillMaxWidth().height(51.dp).padding(horizontal = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                        PersonalAvatar(town, IosPersonalizationImages.photo(entityImages,"TOWN",town))
                                        Spacer(Modifier.width(8.dp))
                                        Column(Modifier.weight(1f)) { Text(town, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Poble", fontSize = 11.sp, color = mutedText) }
                                        Box {
                                            TextButton(onClick = { radiusMenuFor = town }, contentPadding = PaddingValues(horizontal = 3.dp)) {
                                                Text("${selectedRadii[town] ?: PersonalizationOptions.defaultRadiusKm} km ⌄", fontSize = 12.sp, color = personalizationText)
                                            }
                                            DropdownMenu(expanded = radiusMenuFor == town, onDismissRequest = { radiusMenuFor = "" }) {
                                                PersonalizationOptions.locationRadiiKm.forEach { r ->
                                                    DropdownMenuItem(text = { Text("$r km") }, onClick = {
                                                        selectedRadii = selectedRadii + (town to r); radiusMenuFor = ""
                                                    })
                                                }
                                            }
                                        }
                                        TextButton(onClick = { selectedTowns = selectedTowns - town; selectedRadii = selectedRadii - town },
                                            contentPadding = PaddingValues(2.dp)) { Text("×", fontSize = 23.sp, color = personalizationText) }
                                    }
                                }
                            }
                            items(selectedComarques.sorted(), key = { "comarca_$it" }) { comarca ->
                                PersonalCard {
                                    Row(Modifier.fillMaxWidth().height(51.dp).padding(horizontal = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                        PersonalAvatar(comarca, IosPersonalizationImages.photo(entityImages,"TOWN",comarca))
                                        Spacer(Modifier.width(8.dp))
                                        Column(Modifier.weight(1f)) { Text(comarca, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Comarca", fontSize = 11.sp, color = mutedText) }
                                        TextButton(onClick = { selectedComarques = selectedComarques - comarca },
                                            contentPadding = PaddingValues(2.dp)) { Text("×", fontSize = 23.sp, color = personalizationText) }
                                    }
                                }
                            }
                            item {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Box(Modifier.weight(1f)) { PersonalSearch(locationSearch, { locationSearch = it }, "Poble o comarca") }
                                    Button(onClick = {
                                        val term = locationSearch.trim()
                                        val place = IosGeoPlaces.place(term)
                                        val region = IosGeoPlaces.allComarques().firstOrNull { it.equals(term,true) }
                                        when { place?.municipality == true -> selectedTowns = selectedTowns + place.name
                                            region != null -> selectedComarques = selectedComarques + region }
                                        locationSearch = ""
                                    }, enabled = IosGeoPlaces.place(locationSearch)?.municipality == true ||
                                        IosGeoPlaces.allComarques().any { it.equals(locationSearch.trim(),true) }) { Text("Afegir") }
                                }
                            }
                            if (locationSearch.length >= 2) {
                                val townsToShow = (townOptions.filter { it.contains(locationSearch,true) } + IosGeoPlaces.searchTowns(locationSearch))
                                    .distinct().filterNot { it in selectedTowns }.take(7)
                                items(townsToShow, key = { "town_suggestion_$it" }) { name ->
                                    PersonalSuggestionRow(name,"Poble") { selectedTowns = selectedTowns + name; locationSearch = "" }
                                }
                                val comarquesToShow = IosGeoPlaces.allComarques().filter { it.contains(locationSearch,true) && it !in selectedComarques }.take(7)
                                items(comarquesToShow, key = { "comarca_suggestion_$it" }) { name ->
                                    PersonalSuggestionRow(name,"Comarca") { selectedComarques = selectedComarques + name; locationSearch = "" }
                                }
                            }
                        }
                        3 -> {
                            item { Text("Contingut bloquejat", fontWeight = FontWeight.Bold, fontSize = 23.sp) }
                            val names = hiddenArtistsDraft.sorted()
                            if (names.isNotEmpty()) item { Text("Artistes i grups", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                            items(names, key = { "hidden_artist_$it" }) { name ->
                                PersonalCard { Row(Modifier.fillMaxWidth().height(55.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    PersonalAvatar(name, IosPersonalizationImages.photo(entityImages,"ARTIST",name)); Spacer(Modifier.width(9.dp))
                                    Text(name, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                                    TextButton(onClick = { hiddenArtistsDraft = hiddenArtistsDraft - name }) { Text("Tornar a mostrar", fontSize = 12.sp, color = personalizationText) }
                                } }
                            }
                            val cats = hiddenCategoriesDraft.sorted()
                            if (cats.isNotEmpty()) item { Text("Categories", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                            items(cats, key = { "hidden_category_$it" }) { name ->
                                PersonalCard { Row(Modifier.fillMaxWidth().height(55.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    PersonalAvatar(name, IosPersonalizationImages.photo(entityImages,"CATEGORY",name)); Spacer(Modifier.width(9.dp))
                                    Text(taxonomy.categoryLabel(name).let { if (it == name) taxonomy.subcategoryLabel(name) else it },
                                        modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                                    TextButton(onClick = { hiddenCategoriesDraft = hiddenCategoriesDraft - name }) { Text("Tornar a mostrar", fontSize = 12.sp, color = personalizationText) }
                                } }
                            }
                            if (hiddenEventIds.isNotEmpty()) item { Text("Esdeveniments", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                            items(hiddenEventsDraft.sorted(), key = { "hidden_event_$it" }) { id ->
                                PersonalCard { Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(events.firstOrNull { it.id == id }?.title ?: "Esdeveniment #$id", modifier = Modifier.weight(1f))
                                    TextButton(onClick = { hiddenEventsDraft = hiddenEventsDraft - id }) { Text("Tornar a mostrar", fontSize = 12.sp) }
                                } }
                            }
                            if (hiddenArtistsDraft.isEmpty() && hiddenCategoriesDraft.isEmpty() && hiddenEventsDraft.isEmpty()) item {
                                Text("No tens contingut bloquejat.", fontSize = 13.sp, color = mutedText)
                            }
                        }
                        else -> {
                            item {
                                PersonalSwitch("Activar notificacions iOS", "Permet avisos del sistema. Modifica la freqüència i les campanetes en Notificacions.",
                                    notificationsEnabled,onNotificationsEnabled)
                                Spacer(Modifier.height(13.dp))
                                Text("Preferències", fontSize = 23.sp, fontWeight = FontWeight.Bold)
                                Text("Decidix com es personalitza l'Agenda.", fontSize = 13.sp, color = mutedText)
                                Spacer(Modifier.height(12.dp))
                                PersonalSwitch("Prioritzar valencià", "Dona més pes als esdeveniments en valencià, sense excloure altres idiomes.",
                                    valencianDraft, { valencianDraft = it })
                                PersonalSwitch("Mostrar suggerències", "Mostra recomanacions a «Per a tu» i a la pestanya de suggerències de Notificacions.",
                                    suggestionsDraft, { suggestionsDraft = it })
                                if (suggestionsDraft) {
                                    Spacer(Modifier.height(10.dp))
                                    Text("Nivell de descoberta", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Tria si preferixes els teus gustos o més propostes diferents.", fontSize = 12.sp, color = mutedText)
                                    DiscoveryLevel.entries.forEach { level ->
                                        Row(Modifier.fillMaxWidth().clickable { discoveryDraft = level }, verticalAlignment = Alignment.CenterVertically) {
                                            RadioButton(discoveryDraft == level, onClick = { discoveryDraft = level })
                                            Column { Text(level.label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                Text(level.description, fontSize = 12.sp, color = mutedText) }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(20.dp))
                                Text("Gustos apresos automàticament", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                                Text("Les cerques i les fitxes que obris poden influir en «Per a tu» i en els suggeriments. Pots reiniciar només eixe aprenentatge, sense perdre cap preferència que hages triat.",
                                    fontSize = 13.sp, color = mutedText)
                                Spacer(Modifier.height(11.dp))
                                OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) {
                                    Text("↻  Reiniciar afinitats de cerques i fitxes")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    confirmBlock?.let { (kind, name) ->
        AlertDialog(onDismissRequest = { confirmBlock = null },
            title = { Text("No mostrar $name?") },
            text = { Text("Podràs revertir este bloqueig a Personalització → Bloq. Les campanyes pagades i les entrades internes es mantenen visibles.") },
            confirmButton = { TextButton(onClick = {
                if (kind == "category") {
                    hiddenCategoriesDraft = hiddenCategoriesDraft + name
                    taxonomy.categories.firstOrNull { it.label == name }?.let { cat ->
                        selectedKeys = selectedKeys - cat.code - taxonomy.childrenOf(cat.code).map { it.code }.toSet()
                    }
                } else {
                    hiddenArtistsDraft = hiddenArtistsDraft + name
                    if (name in localArtistFollows && accountSignedIn) {
                        localArtistFollows = localArtistFollows - name
                        onFollowArtist(name)
                    }
                    if (name in localArtistAlerts && accountSignedIn) {
                        localArtistAlerts = localArtistAlerts - name
                        onArtistAlerts(name,false)
                    }
                }
                confirmBlock = null
            }) { Text("Bloquejar") } },
            dismissButton = { TextButton(onClick = { confirmBlock = null }) { Text("Cancel·lar") } })
    }
    if (confirmReset) AlertDialog(onDismissRequest = { confirmReset = false },
        title = { Text("Reiniciar els gustos apresos?") },
        text = { Text("S'elimina l'aprenentatge de cerques i fitxes, sense esborrar seguiments, localitats, guardats ni bloquejos.") },
        confirmButton = { TextButton(onClick = { onResetLearning(); confirmReset = false }) { Text("Reiniciar") } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel·lar") } })
}

@Composable private fun PersonalSectionTitle(title: String, side: String = "") {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
        if (side.isNotBlank()) Text(side, fontSize = 11.sp, color = mutedText)
    }
}
@Composable private fun PersonalSearch(query: String, onChange: (String) -> Unit, placeholder: String) {
    OutlinedTextField(value = query, onValueChange = onChange, singleLine = true,
        modifier = Modifier.fillMaxWidth(), placeholder = { Text(placeholder) },
        leadingIcon = { Text("⌕", fontSize = 27.sp, color = mutedText) })
}
@Composable private fun PersonalCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(color = rowBackground, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) { Column(content = content) }
}
@Composable private fun PersonalAvatar(label: String, url: String) {
    Box(Modifier.size(40.dp).clip(RoundedCornerShape(9.dp)).background(Color(0xFFDDD8E7)), contentAlignment = Alignment.Center) {
        if (url.isNotBlank()) AsyncImage(model = url, contentDescription = null,
            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else Text(label.take(1).uppercase(), fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}
@Composable private fun PersonalSuggestionRow(name: String, type: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 42.dp).padding(horizontal = 7.dp),
        verticalAlignment = Alignment.CenterVertically) {
        PersonalAvatar(name, ""); Spacer(Modifier.width(9.dp)); Text(name, modifier = Modifier.weight(1f), fontSize = 13.sp)
        Text(type, fontSize = 11.sp, color = mutedText); Text("  +", fontSize = 20.sp)
    }
}
@Composable private fun PersonalSwitch(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, fontSize = 13.sp, color = mutedText)
        }
        Spacer(Modifier.width(8.dp)); Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
