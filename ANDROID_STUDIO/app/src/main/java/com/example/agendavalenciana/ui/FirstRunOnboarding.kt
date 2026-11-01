package com.example.agendavalenciana.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.unit.Dp
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import org.json.JSONObject
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.agendavalenciana.data.ServerApiRepository
import com.example.agendavalenciana.model.CategoryTaxonomy
import com.example.agendavalenciana.model.CulturalCategory
import com.example.agendavalenciana.model.PersonalizationOptions
import com.example.agendavalenciana.model.FirstRunOnboardingPolicy
import com.example.agendavalenciana.state.AgendaState
import java.util.Locale

private val IntroGold = Color(0xFFFEAE06)
private val IntroBlack = Color(0xFF171717)
private val IntroCategories = FirstRunOnboardingPolicy.disciplines

/** Disciplines, subcategories, llocs i un únic pas de grups si s'ha triat Música.
 * Cap compte obligatori. Totes les eleccions entren en les preferències
 * existents (follows CATEGORY/ARTIST i llocs); en autenticació se sincronitzen amb MySQL.
 */
@Composable
internal fun FirstRunOnboardingDialog(
    state: AgendaState,
    onSkip: () -> Unit,
    onLogin: () -> Unit,
    onCreateAccount: () -> Unit,
    onContinueGuest: () -> Unit
) {
    val context = LocalContext.current
    val introPrefs = remember(context) {
        context.getSharedPreferences("agenda_valenciana", Context.MODE_PRIVATE)
    }
    var step by remember { mutableStateOf(0) }
    var selectedCategories by remember { mutableStateOf(
        introPrefs.getStringSet("v294_intro_categories", emptySet()).orEmpty()
            .mapNotNull { name -> CulturalCategory.entries.firstOrNull { it.name == name } }.toSet()
    ) }
    var selectedSubcategories by remember { mutableStateOf(
        introPrefs.getStringSet("v294_intro_subcategories", emptySet()).orEmpty().toSet()
    ) }
    var selectedLocations by remember { mutableStateOf(runCatching {
        val obj = JSONObject(introPrefs.getString("v294_intro_locations", "{}") ?: "{}")
        obj.keys().asSequence().associateWith { key ->
            PersonalizationOptions.validRadius(obj.optInt(key, PersonalizationOptions.defaultRadiusKm))
        }
    }.getOrDefault(emptyMap<String, Int>())) }
    var selectedArtists by remember { mutableStateOf(
        introPrefs.getStringSet("v294_intro_artists", emptySet()).orEmpty().toSet()
    ) }
    var townQuery by remember { mutableStateOf("") }
    var artistQuery by remember { mutableStateOf("") }
    val artistCategories = remember(selectedCategories) {
        FirstRunOnboardingPolicy.artistCategories(selectedCategories)
    }
    val finalStep = FirstRunOnboardingPolicy.finalStep(selectedCategories)
    val activeArtistCategory = FirstRunOnboardingPolicy.artistCategoryAtStep(selectedCategories, step)
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    LaunchedEffect(Unit) { state.refreshOnboardingRankings() }
    // Guardat immediat: també sobreviu si es tanca l'aplicació a mitjan assistent.
    // Les mateixes dades queden en Personalització i migren al compte en autenticar-se.
    LaunchedEffect(selectedCategories, selectedSubcategories, selectedLocations, selectedArtists) {
        introPrefs.edit()
            .putStringSet("v294_intro_categories", selectedCategories.map { it.name }.toSet())
            .putStringSet("v294_intro_subcategories", selectedSubcategories.toSet())
            .putString("v294_intro_locations", JSONObject().apply {
                selectedLocations.forEach { (name, radius) -> put(name, radius) }
            }.toString())
            .putStringSet("v294_intro_artists", selectedArtists.toSet())
            .commit()
        state.applyFirstRunChoices(selectedCategories, selectedSubcategories, selectedLocations, selectedArtists)
    }

    val wantedSubcategories = remember(selectedCategories) {
        IntroCategories.filter { it in selectedCategories }
            .flatMap(CategoryTaxonomy::childrenOf)
    }
    // Només hi ha un pas de grups si s'ha triat Música. Els huit grups amb
    // més concerts diferents apareixen primer; el cercador és exclusiu de música.
    val matchingArtists = remember(activeArtistCategory, selectedSubcategories, artistQuery,
        state.onboardingRankings, state.concerts, state.entityImageCatalogRevision) {
        activeArtistCategory?.let { category ->
            if (category == CulturalCategory.MUSIC) state.onboardingMusicGroupChoices(
                selectedSubcategoryCodes = selectedSubcategories,
                query = artistQuery.trim(),
                limit = if (artistQuery.isBlank()) 8 else 50
            ) else emptyList()
        }.orEmpty()
    }
    // El primer recurs de cada subcategoria és la FOTO D'UN ARTISTA que tinga
    // aquell codi en ENTITATS o que aparega en concerts d'eixe estil. Si no
    // hi ha cap retrat real, fem servir la fotografia de la disciplina.
    val photoBySubcategory = remember(state.concerts, state.entityImageCatalogRevision) {
        state.onboardingSubcategoryArtistPhotos()
    }
    val nearbyTowns = remember(townQuery, state.municipalities, state.onboardingRankings) {
        if (townQuery.trim().length < 2) emptyList()
        else state.onboardingTownChoices(townQuery.trim(), limit = 12)
    }

    // Altura ajustada a les files reals, sense un quadre buit gegant.
    // Les pantalles llargues continuen amb scroll, amb les accions visibles.
    val visibleTownMatches = nearbyTowns.count { it.name !in selectedLocations }
    val desiredBodyHeight = when {
        step == 0 -> 235.dp
        step in 1..2 -> 304.dp
        step == 3 && wantedSubcategories.isEmpty() -> 50.dp
        step == 3 -> (IntroCategories.filter { it in selectedCategories }.sumOf { category ->
            39 + ((CategoryTaxonomy.childrenOf(category).size + 1) / 2) * 130
        } + 20).dp
        step == 4 -> (85 + visibleTownMatches * 69 + selectedLocations.size * 54 +
            (if (selectedLocations.isNotEmpty()) 30 else 0)).dp
        activeArtistCategory != null -> (88 + ((matchingArtists.size + 1) / 2) * 155 +
            (if (selectedArtists.isNotEmpty()) 27 else 0)).dp
        else -> 315.dp
    }
    val maximumBodyHeight = minOf(505.dp, (screenHeight - 218.dp).coerceAtLeast(115.dp))
    val bodyHeight = minOf(desiredBodyHeight, maximumBodyHeight)
    val listState = rememberLazyListState()
    LaunchedEffect(step) { listState.scrollToItem(0) }

    fun saveSelections() {
        state.applyFirstRunChoices(
            disciplines = selectedCategories,
            subcategoryCodes = selectedSubcategories,
            locationRadii = selectedLocations,
            artists = selectedArtists
        )
    }
    fun exit(action: () -> Unit) {
        saveSelections()
        action()
    }

    Dialog(
        onDismissRequest = { exit(onSkip) },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(.94f).wrapContentHeight()
                .heightIn(max = screenHeight * .95f),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            shadowElevation = 9.dp
        ) {
            Column(Modifier.padding(horizontal = 17.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("L’AGENDA", fontSize = 17.sp, fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = { exit(onSkip) }) { Text("Ometre", color = MaterialTheme.colorScheme.onSurface) }
                    TextButton(onClick = { exit(onLogin) }) { Text("Iniciar sessió", color = MaterialTheme.colorScheme.onSurface) }
                }
                Box(
                    Modifier.fillMaxWidth().height(3.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(3.dp))
                ) {
                    Box(Modifier.fillMaxWidth((step + 1f) / (finalStep + 1f)).height(3.dp)
                        .background(IntroGold, RoundedCornerShape(3.dp)))
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    when (step) {
                        0 -> "Benvinguts a L’Agenda"
                        1 -> "Què t’agrada? · 1/2"
                        2 -> "Què t’agrada? · 2/2"
                        3 -> "Quins estils t’agraden?"
                        4 -> "Quines poblacions t’interessen?"
                        else -> if (activeArtistCategory != null)
                            "Grups · ${activeArtistCategory.label}"
                        else "L’Agenda ja és teua"
                    },
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Text(
                    when (step) {
                        0 -> "Descobrix activitats culturals adaptades als teus gustos, pas a pas. És ràpid, gratuït i pots ometre-ho quan vulgues."
                        1 -> "Tria tantes disciplines com vulgues."
                        2 -> "Pots marcar també alguna de les quatre restants."
                        3 -> "Tria els tipus d’activitats que més t’interessen."
                        4 -> "Afig tantes poblacions com vulgues i tria el radi de cadascuna."
                        else -> if (activeArtistCategory != null)
                            "Els huit grups amb més concerts a L’Agenda. Pots buscar-ne més pel nom."
                        else "Ja pots entrar a L’Agenda. Tria si vols continuar com a convidat o guardar les preferències amb un compte."
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 13.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(bodyHeight),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    contentPadding = PaddingValues(bottom = 10.dp)
                ) {
                    when (step) {
                        0 -> {
                            item {
                                Text("Un espai per trobar música, teatre, cinema i tota la cultura que tens a prop.",
                                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 11.dp),
                                    textAlign = TextAlign.Center, fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium)
                                Text("Ens diràs què t’agrada i quines poblacions t’interessen. Amb això prepararem ‘Per a tu’. Pots canviar-ho després en Personalització.",
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            }
                        }
                        1, 2 -> {
                            val categories = IntroCategories.drop(if (step == 1) 0 else 4).take(4)
                            items(categories.chunked(2), key = { row -> "disc_${row.first().name}" }) { row ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterHorizontally)) {
                                    row.forEach { category ->
                                        IntroPhotoCard(
                                            title = category.label,
                                            imageUrl = state.publicPhotoForCategory(category),
                                            selected = category in selectedCategories,
                                            modifier = if (row.size == 1) Modifier.fillMaxWidth(.48f) else Modifier.weight(1f),
                                            height = 142.dp,
                                            onClick = {
                                                selectedCategories = if (category in selectedCategories)
                                                    selectedCategories - category else selectedCategories + category
                                                selectedSubcategories = selectedSubcategories.filterTo(mutableSetOf()) {
                                                    CategoryTaxonomy.findByCode(it)?.category in selectedCategories
                                                }
                                            }
                                        )
                                    }
                                    // La fila incompleta queda centrada, sense una casella buida.
                                }
                            }
                        }
                        3 -> {
                            if (wantedSubcategories.isEmpty()) {
                                item { Text("No has marcat cap disciplina. Pots continuar o tornar arrere.") }
                            } else {
                                IntroCategories.filter { it in selectedCategories }.forEach { category ->
                                    item(key = "category_${category.name}") {
                                        Text(category.label, fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth().padding(top = 9.dp, bottom = 5.dp))
                                    }
                                    items(CategoryTaxonomy.childrenOf(category).chunked(2),
                                        key = { row -> "sub_${row.first().code}" }) { row ->
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterHorizontally)) {
                                            row.forEach { sub ->
                                                IntroPhotoCard(
                                                    title = sub.label,
                                                    imageUrl = photoBySubcategory[sub.code]
                                                        ?: state.publicPhotoForCategory(category),
                                                    selected = sub.code in selectedSubcategories,
                                                    modifier = if (row.size == 1) Modifier.fillMaxWidth(.48f) else Modifier.weight(1f),
                                                    height = 123.dp,
                                                    onClick = {
                                                        selectedSubcategories = if (sub.code in selectedSubcategories)
                                                            selectedSubcategories - sub.code else selectedSubcategories + sub.code
                                                    }
                                                )
                                            }
                                            // Centre la targeta solta.
                                        }
                                    }
                                }
                            }
                        }
                        4 -> {
                            item {
                                OutlinedTextField(
                                    value = townQuery,
                                    onValueChange = { townQuery = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Busca una població") },
                                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                            items(nearbyTowns, key = { it.code }) { town ->
                                if (town.name !in selectedLocations) {
                                    IntroTownPhotoRow(
                                        name = town.name,
                                        subtitle = town.comarca,
                                        imageUrl = state.publicPhotoForTown(town.name),
                                        onClick = {
                                            selectedLocations = selectedLocations + (town.name to PersonalizationOptions.defaultRadiusKm)
                                            townQuery = ""
                                        }
                                    )
                                }
                            }
                            if (selectedLocations.isNotEmpty()) {
                                item { Text("Localitzacions (${selectedLocations.size})", fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 9.dp, bottom = 4.dp)) }
                            }
                            items(selectedLocations.entries.sortedBy { it.key }, key = { "location_${it.key}" }) { entry ->
                                var radiusExpanded by remember(entry.key) { mutableStateOf(false) }
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(entry.key, modifier = Modifier.weight(1f), maxLines = 1,
                                        overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                                    Box {
                                        TextButton(onClick = { radiusExpanded = true }) {
                                            Text("${entry.value} km ▾")
                                        }
                                        DropdownMenu(expanded = radiusExpanded, onDismissRequest = { radiusExpanded = false }) {
                                            PersonalizationOptions.locationRadiiKm.forEach { radius ->
                                                DropdownMenuItem(text = { Text("$radius km") }, onClick = {
                                                    selectedLocations = selectedLocations + (entry.key to radius)
                                                    radiusExpanded = false
                                                })
                                            }
                                        }
                                    }
                                    IconButton(onClick = { selectedLocations = selectedLocations - entry.key }) {
                                        Icon(Icons.Outlined.Close, contentDescription = "Llevar ${entry.key}")
                                    }
                                }
                            }
                        }
                        in 5 until finalStep -> {
                            item {
                                OutlinedTextField(
                                    value = artistQuery,
                                    onValueChange = { artistQuery = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Busca grups de música") },
                                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                            if (matchingArtists.isEmpty()) {
                                item { Text("Cap grup trobat. Prova amb un altre nom.") }
                            }
                            items(matchingArtists.chunked(2), key = { row ->
                                "artist_${row.first().lowercase(Locale.ROOT)}"
                            }) { row ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterHorizontally)) {
                                    row.forEach { artist ->
                                        IntroPhotoCard(
                                            title = artist,
                                            imageUrl = state.publicPhotoForArtist(artist),
                                            selected = artist in selectedArtists,
                                            modifier = if (row.size == 1) Modifier.fillMaxWidth(.48f) else Modifier.weight(1f),
                                            height = 148.dp,
                                            onClick = {
                                                selectedArtists = if (artist in selectedArtists)
                                                    selectedArtists - artist else selectedArtists + artist
                                            }
                                        )
                                    }
                                    // Centre la targeta solta.
                                }
                            }
                            if (selectedArtists.isNotEmpty()) {
                                item { Text("${selectedArtists.size} grups seleccionats",
                                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                        }
                        else -> {
                            item {
                                Text("Les teues eleccions ja estan en Personalització.",
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
                                Text("Sense compte pots consultar l’agenda i utilitzar ‘Per a tu’. Els gustos i llocs es guarden només en este dispositiu: no es recuperen si canvies de mòbil o esborres l’app.",
                                    textAlign = TextAlign.Center, fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(12.dp))
                                IntroAction("Continuar sense usuari", onClick = { exit(onContinueGuest) })
                                Spacer(Modifier.height(12.dp))
                                Text("Amb un compte, les teues preferències i seguiments es guarden al servidor i es recuperen quan inicies sessió en un altre dispositiu.",
                                    textAlign = TextAlign.Center, fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(8.dp))
                                Text("Pots proposar esdeveniments sense compte. Per a comprar o vendre entrades dins de L’Agenda i participar en comunitats, cal registrar-se.",
                                    textAlign = TextAlign.Center, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(12.dp))
                                IntroAction("Crear un usuari", onClick = { exit(onCreateAccount) })
                                Spacer(Modifier.height(8.dp))
                                IntroAction("Iniciar sessió", onClick = { exit(onLogin) })
                                Spacer(Modifier.height(10.dp))
                                Text("Pots modificar-ho en Personalització quan vulgues.",
                                    textAlign = TextAlign.Center, fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                if (step < finalStep) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (step > 0) TextButton(onClick = {
                            saveSelections()
                            step--
                            artistQuery = ""
                        }) { Text("Enrere") }
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = {
                                saveSelections()
                                step++
                                artistQuery = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IntroBlack),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text(if (step == 0) "Començar" else if (step == finalStep - 1) "Finalitzar" else "Continuar") }
                    }
                }
            }
        }
    }
}

/** Targeta fotogràfica seleccionable, reutilitzant les imatges públiques de L'Agenda.
 * Una fotografia absent es representa de manera neutra; mai es descarrega una
 * foto inventada ni es mostra una foto d'una entitat diferent.
 */
@Composable
private fun IntroPhotoCard(
    title: String,
    imageUrl: String?,
    selected: Boolean,
    modifier: Modifier = Modifier,
    height: Dp = 138.dp,
    onClick: () -> Unit
) {
    val photo = rememberImageBitmapFromUri(imageUrl, maxDimension = 256)
    Surface(
        modifier = modifier.height(height).clickable(onClick = onClick),
        shape = RoundedCornerShape(15.dp),
        color = IntroBlack,
        border = BorderStroke(if (selected) 2.5.dp else 1.dp,
            if (selected) IntroGold else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(Modifier.fillMaxSize()) {
            if (photo != null) {
                Image(bitmap = photo, contentDescription = title,
                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Box(Modifier.fillMaxSize().background(
                    Brush.linearGradient(listOf(Color(0xFF333333), Color(0xFF6A6358))))
                )
                Text(title.take(1).uppercase(Locale.ROOT), color = Color.White.copy(alpha = .20f),
                    fontWeight = FontWeight.Black, fontSize = 67.sp,
                    modifier = Modifier.align(Alignment.Center))
            }
            Box(Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Transparent, Color(0xBB000000), Color(0xF3000000)),
                    startY = 58f)))
            Text(title, modifier = Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth().padding(start = 9.dp, end = 9.dp, bottom = 11.dp),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold, color = Color.White,
                fontSize = 14.sp, maxLines = 3, overflow = TextOverflow.Ellipsis,
                lineHeight = 17.sp)
            Box(
                Modifier.align(Alignment.TopEnd).padding(8.dp)
                    .size(27.dp).clip(CircleShape)
                    .background(if (selected) IntroGold else Color.Black.copy(alpha = .55f))
                    .border(1.dp, if (selected) IntroGold else Color.White.copy(alpha = .80f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (selected) Icon(Icons.Outlined.Check, contentDescription = "Seleccionat",
                    tint = IntroBlack, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun IntroTownPhotoRow(name: String, subtitle: String, imageUrl: String?, onClick: () -> Unit) {
    val photo = rememberImageBitmapFromUri(imageUrl, maxDimension = 128)
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(13.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.fillMaxWidth().padding(5.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(57.dp).clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center) {
                if (photo != null) Image(photo, contentDescription = name,
                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else Text(name.take(1).uppercase(Locale.ROOT), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f).padding(horizontal = 11.dp)) {
                Text(name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text("+ Afegir", fontSize = 12.sp, color = IntroBlack, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 9.dp))
        }
    }
}

@Composable
private fun IntroAction(title: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = IntroBlack),
        shape = RoundedCornerShape(12.dp)) { Text(title) }
}
