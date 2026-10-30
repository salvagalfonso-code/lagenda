package com.example.agendavalenciana.ios

import com.example.agendavalenciana.model.PersonalizationOptions
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage

private val introInk = Color(0xFF171717)
private val introGold = Color(0xFFD7B768)

/** Exact eight disciplines and step ordering used by Android FirstRunOnboardingPolicy.
 * Draft saves are delegated to the same guest personalization/follows used by Per a tu.
 */
internal object IosFirstRunPolicy {
    val disciplines = listOf("MUSIC", "THEATRE", "CINEMA", "TALKS", "DANCE",
        "EXHIBITIONS", "LITERATURE", "POPULAR_CULTURE")
    fun finalStep(categories: Set<String>) = if ("MUSIC" in categories) 6 else 5
    fun followKeys(categories: Set<String>, subcategories: Set<String>): Set<String> {
        val valid = subcategories.filter { sub ->
            IosPersonalizationTaxonomy.subcategories.any { it.code == sub && it.categoryCode in categories }
        }.toSet()
        val full = categories.filter { category ->
            IosPersonalizationTaxonomy.childrenOf(category).none { it.code in valid }
        }.toSet()
        return full + valid
    }
}

@Composable
internal fun IosFirstRunOnboardingDialog(
    initialCategories: Set<String>, initialSubcategories: Set<String>,
    initialLocations: Map<String, Int>, initialArtists: Set<String>,
    events: List<AgendaEventSummary>, entityImages: Map<String,String>,
    onDraftChanged: (Set<String>, Set<String>, Map<String, Int>, Set<String>) -> Unit,
    onSkip: () -> Unit, onLogin: () -> Unit, onRegister: () -> Unit,
    onContinueGuest: () -> Unit
) {
    val taxonomy = IosPersonalizationTaxonomy
    var step by remember { mutableIntStateOf(platformReadPublicCache("ios_intro_step_v350").toIntOrNull()?.coerceIn(0,6) ?: 0) }
    var categories by remember { mutableStateOf(initialCategories) }
    var subcategories by remember { mutableStateOf(initialSubcategories) }
    var locations by remember { mutableStateOf(initialLocations) }
    var artists by remember { mutableStateOf(initialArtists) }
    var townQuery by remember { mutableStateOf("") }
    var artistQuery by remember { mutableStateOf("") }
    var radiusMenu by remember { mutableStateOf("") }
    val finalStep = IosFirstRunPolicy.finalStep(categories)
    if (step > finalStep) step = finalStep
    val scroll = rememberLazyListState()
    LaunchedEffect(step) {
        platformWritePublicCache("ios_intro_step_v350", step.toString())
        scroll.scrollToItem(0)
    }
    // Each draft edit is persisted immediately, even when the app closes halfway through.
    LaunchedEffect(categories, subcategories, locations, artists) {
        onDraftChanged(categories, subcategories, locations, artists)
    }
    val matchingTowns = remember(townQuery) {
        if (townQuery.trim().length < 2) emptyList()
        else IosGeoPlaces.searchTowns(townQuery.trim(), 12).filterNot { it in locations }
    }
    val rankedArtists = remember(events, subcategories, artistQuery, artists) {
        val wanted = subcategories.filter { it.startsWith("MUSIC__") }.toSet()
        val musicEvents = events.filter { event ->
            event.category.equals("MUSIC", true) || event.category.equals("Música", true) ||
                event.styles.any { style -> wanted.any { code ->
                    code == style || IosPersonalizationTaxonomy.matchesSubcategory(code, "MUSIC", event.styles)
                } }
        }
        val tally = mutableMapOf<String, MutableSet<Int>>()
        musicEvents.forEach { event ->
            (event.artists + event.schedule.map { it.artist }).map(String::trim)
                .filter(String::isNotBlank).distinct().forEach { name ->
                    tally.getOrPut(name) { mutableSetOf() }.add(event.id)
                }
        }
        (tally.keys + artists).distinct().filter { artist ->
            artistQuery.isBlank() || artist.contains(artistQuery.trim(), true)
        }.sortedWith(compareByDescending<String> { tally[it]?.size ?: 0 }.thenBy { it })
            .take(if (artistQuery.isBlank()) 8 else 50)
    }
    fun finish(action: () -> Unit) {
        onDraftChanged(categories, subcategories, locations, artists)
        platformWritePublicCache("ios_intro_step_v350", "0")
        action()
    }
    Dialog(onDismissRequest = { finish(onSkip) },
        properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth(.94f).fillMaxHeight(.90f),
            shape = RoundedCornerShape(22.dp), color = Color.White, shadowElevation = 10.dp) {
            Column(Modifier.fillMaxSize().padding(horizontal = 15.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("L’AGENDA", fontSize = 17.sp, fontWeight = FontWeight.Black,
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = { finish(onSkip) }) { Text("Ometre", fontSize = 12.sp) }
                    TextButton(onClick = { finish(onLogin) }) { Text("Iniciar sessió", fontSize = 12.sp) }
                }
                LinearProgressIndicator(progress = { (step + 1f) / (finalStep + 1f) },
                    modifier = Modifier.fillMaxWidth().height(3.dp), color = introGold)
                Spacer(Modifier.height(14.dp))
                Text(when (step) {
                    0 -> "Benvinguts a L’Agenda"
                    1 -> "Què t’agrada? · 1/2"
                    2 -> "Què t’agrada? · 2/2"
                    3 -> "Quins estils t’agraden?"
                    4 -> "Quines poblacions t’interessen?"
                    5 -> if ("MUSIC" in categories) "Grups · Música" else "L’Agenda ja és teua"
                    else -> "L’Agenda ja és teua"
                }, fontSize = 21.sp, fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Text(when (step) {
                    0 -> "Descobrix activitats culturals adaptades als teus gustos. És ràpid, gratuït i pots ometre-ho quan vulgues."
                    1, 2 -> "Tria tantes disciplines com vulgues."
                    3 -> "Tria els tipus d’activitats que més t’interessen."
                    4 -> "Afig tantes poblacions com vulgues i tria el radi de cadascuna."
                    5 -> if ("MUSIC" in categories) "Els huit grups amb més concerts. Pots buscar-ne més pel nom."
                         else "Ja pots entrar a L’Agenda amb un compte o com a convidat."
                    else -> "Ja pots entrar a L’Agenda amb un compte o com a convidat."
                }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 13.dp),
                    textAlign = TextAlign.Center, fontSize = 13.sp, color = Color.DarkGray)
                LazyColumn(Modifier.weight(1f), state = scroll,
                    contentPadding = PaddingValues(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (step) {
                        0 -> item {
                            Column(Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Un espai per trobar música, teatre, cinema i tota la cultura que tens a prop.",
                                    textAlign = TextAlign.Center, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                Spacer(Modifier.height(18.dp))
                                Text("Ens diràs què t’agrada i quines poblacions t’interessen. Amb això prepararem ‘Per a tu’. Pots canviar-ho després en Personalització.",
                                    textAlign = TextAlign.Center, fontSize = 13.sp, color = Color.DarkGray)
                            }
                        }
                        1, 2 -> {
                            val page = IosFirstRunPolicy.disciplines.drop(if (step == 1) 0 else 4).take(4)
                            items(page.chunked(2), key = { "category_" + it.first() }) { row ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                    row.forEach { cat ->
                                        IosIntroPhotoCard(taxonomy.categoryLabel(cat),
                                            IosPersonalizationImages.photo(entityImages,"CATEGORY",taxonomy.categoryLabel(cat)),
                                            cat in categories, Modifier.weight(1f), 138,
                                            onClick = {
                                                categories = if (cat in categories) categories - cat else categories + cat
                                                subcategories = subcategories.filter { sub ->
                                                    taxonomy.subcategories.any { it.code == sub && it.categoryCode in categories }
                                                }.toSet()
                                            })
                                    }
                                }
                            }
                        }
                        3 -> {
                            if (categories.isEmpty()) item { Text("No has marcat cap disciplina. Pots continuar o tornar arrere.") }
                            IosFirstRunPolicy.disciplines.filter { it in categories }.forEach { cat ->
                                item(key = "title_$cat") { Text(taxonomy.categoryLabel(cat),
                                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) }
                                items(taxonomy.childrenOf(cat).chunked(2), key = { "style_" + it.first().code }) { row ->
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                        row.forEach { sub ->
                                            val image = IosPersonalizationImages.photo(entityImages,"CATEGORY",sub.label)
                                                .ifBlank { IosPersonalizationImages.photo(entityImages,"CATEGORY",taxonomy.categoryLabel(cat)) }
                                            IosIntroPhotoCard(sub.label, image, sub.code in subcategories,
                                                Modifier.weight(1f), 125, onClick = {
                                                    subcategories = if (sub.code in subcategories) subcategories - sub.code
                                                        else subcategories + sub.code
                                                })
                                        }
                                        if (row.size == 1) Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                        4 -> {
                            item { OutlinedTextField(townQuery, { townQuery = it },
                                label = { Text("Busca una població") }, singleLine = true,
                                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) }
                            items(matchingTowns, key = { "town_$it" }) { town ->
                                val place = IosGeoPlaces.place(town)
                                Row(Modifier.fillMaxWidth().clickable {
                                    locations = locations + (town to PersonalizationOptions.defaultRadiusKm)
                                    townQuery = ""
                                }.padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
                                    IosIntroAvatar(town, IosPersonalizationImages.photo(entityImages,"TOWN",town))
                                    Spacer(Modifier.width(9.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(town, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                                        Text(place?.comarca.orEmpty(), fontSize = 12.sp, color = Color.Gray)
                                    }
                                    Text("+ Afegir", fontSize = 12.sp)
                                }
                            }
                            if (locations.isNotEmpty()) {
                                item { Text("Localitzacions (${locations.size})", fontWeight = FontWeight.Bold) }
                            }
                            items(locations.entries.sortedBy { it.key }, key = { "chosen_${it.key}" }) { entry ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(entry.key, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Box {
                                        TextButton(onClick = { radiusMenu = entry.key }) { Text("${entry.value} km ▾") }
                                        DropdownMenu(expanded = radiusMenu == entry.key, onDismissRequest = { radiusMenu = "" }) {
                                            PersonalizationOptions.locationRadiiKm.forEach { radius ->
                                                DropdownMenuItem(text = { Text("$radius km") }, onClick = {
                                                    locations = locations + (entry.key to radius)
                                                    radiusMenu = ""
                                                })
                                            }
                                        }
                                    }
                                    TextButton(onClick = { locations = locations - entry.key }) { Text("×", fontSize = 22.sp) }
                                }
                            }
                        }
                        5 -> if ("MUSIC" in categories) {
                            item { OutlinedTextField(artistQuery, { artistQuery = it },
                                label = { Text("Busca grups de música") }, singleLine = true,
                                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) }
                            if (rankedArtists.isEmpty()) item { Text("Cap grup trobat. Prova amb un altre nom.") }
                            items(rankedArtists.chunked(2), key = { "artist_" + it.first() }) { row ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                    row.forEach { artist ->
                                        IosIntroPhotoCard(artist,
                                            IosPersonalizationImages.photo(entityImages,"ARTIST",artist),
                                            artist in artists, Modifier.weight(1f), 148,
                                            onClick = { artists = if (artist in artists) artists - artist else artists + artist })
                                    }
                                    if (row.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                            if (artists.isNotEmpty()) item { Text("${artists.size} grups seleccionats", fontSize = 12.sp) }
                        } else item { IosIntroLastStep(onContinueGuest = { finish(onContinueGuest) },
                            onRegister = { finish(onRegister) }, onLogin = { finish(onLogin) }) }
                        else -> item { IosIntroLastStep(onContinueGuest = { finish(onContinueGuest) },
                            onRegister = { finish(onRegister) }, onLogin = { finish(onLogin) }) }
                    }
                }
                if (step < finalStep) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        if (step > 0) TextButton(onClick = { step--; artistQuery = "" }) { Text("Enrere") }
                        Spacer(Modifier.weight(1f))
                        Button(onClick = { step++; artistQuery = "" },
                            colors = ButtonDefaults.buttonColors(containerColor = introInk),
                            shape = RoundedCornerShape(12.dp)) {
                            Text(if (step == 0) "Començar" else if (step == finalStep - 1) "Finalitzar" else "Continuar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IosIntroPhotoCard(title: String, imageUrl: String, selected: Boolean,
    modifier: Modifier, height: Int, onClick: () -> Unit) {
    Surface(modifier.height(height.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(15.dp),
        color = introInk,
        border = BorderStroke(if (selected) 2.5.dp else 1.dp,
            if (selected) introGold else Color(0xFFBBBBBB))) {
        Box(Modifier.fillMaxSize()) {
            if (imageUrl.isNotBlank()) {
                AsyncImage(model = imageUrl, contentDescription = title,
                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Box(Modifier.fillMaxSize().background(Brush.linearGradient(
                    listOf(Color(0xFF333333), Color(0xFF6A6358)))))
                Text(title.take(1).uppercase(), modifier = Modifier.align(Alignment.Center),
                    color = Color.White.copy(alpha = .25f), fontSize = 55.sp)
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                listOf(Color.Transparent, Color(0xC9000000), Color(0xEE000000)), startY = 40f)))
            Text(title, modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(8.dp),
                textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = Color.White,
                fontSize = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            if (selected) Text("✓", modifier = Modifier.align(Alignment.TopEnd).padding(7.dp)
                .background(introGold, RoundedCornerShape(30.dp)).padding(horizontal = 7.dp, vertical = 2.dp),
                color = introInk, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun IosIntroAvatar(label: String, imageUrl: String) {
    Box(Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFE9E9E9)),
        contentAlignment = Alignment.Center) {
        if (imageUrl.isNotBlank()) AsyncImage(model = imageUrl, contentDescription = label,
            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else Text(label.take(1).uppercase(), fontSize = 21.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun IosIntroLastStep(onContinueGuest: () -> Unit, onRegister: () -> Unit, onLogin: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Les teues eleccions ja estan en Personalització.", fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center)
        Text("Sense compte pots consultar l’agenda i utilitzar ‘Per a tu’. Els gustos i llocs es guarden només en este dispositiu.",
            textAlign = TextAlign.Center, fontSize = 13.sp)
        Button(onClick = onContinueGuest, modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = introInk)) { Text("Continuar sense usuari") }
        Text("Amb un compte, les preferències i seguiments es guarden al servidor i es recuperen en un altre dispositiu.",
            textAlign = TextAlign.Center, fontSize = 13.sp)
        Text("Pots proposar esdeveniments sense compte. Per a comprar o vendre entrades dins de L’Agenda i participar en comunitats, cal registrar-se.",
            textAlign = TextAlign.Center, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Button(onClick = onRegister, modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = introInk)) { Text("Crear un usuari") }
        OutlinedButton(onClick = onLogin, modifier = Modifier.fillMaxWidth()) { Text("Iniciar sessió") }
        Text("Pots modificar-ho en Personalització quan vulgues.",
            fontSize = 12.sp, textAlign = TextAlign.Center, color = Color.Gray)
    }
}
