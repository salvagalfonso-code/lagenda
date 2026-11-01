package com.example.agendavalenciana.ui

import com.example.agendavalenciana.model.AgendaDateFormat
import com.example.agendavalenciana.model.BellFollowedFrequency

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agendavalenciana.R
import com.example.agendavalenciana.model.AccountProfile
import com.example.agendavalenciana.model.AccountRole
import com.example.agendavalenciana.model.Concert
import com.example.agendavalenciana.model.CulturalCategory
import com.example.agendavalenciana.state.AgendaState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class MusicPlatformLink(
    val name: String,
    val iconRes: Int,
    val color: Color,
    val url: String
)

@Composable
fun GroupProfileScreen(
    state: AgendaState,
    artist: String,
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    onBack: () -> Unit,
    onOpenConcert: (Concert) -> Unit
) {
    val profile = state.publicArtistProfile(artist)
    val artistAccount = state.accountForArtist(artist)
        ?: state.currentAccount?.takeIf { it.canManageArtist(artist) || it.role == AccountRole.SUPER_ADMIN }
    val accountProfile = artistAccount?.let { state.profileFor(it.username) } ?: state.profileForArtist(artist)
    val canEditProfessional = state.currentAccount?.let { current ->
        current.role == AccountRole.SUPER_ADMIN || current.canManageArtist(artist) || current.username == artistAccount?.username
    } == true
    var showProfessionalEditor by remember(artist) { mutableStateOf(false) }
    val followed = artist in state.followedArtists
    val alertsEnabled = artist in state.artistAlerts
    var showArtistBellDialog by remember(artist) { mutableStateOf(false) }
    var selectedArtistFrequency by remember(artist) { mutableStateOf(state.artistAlertFrequency(artist)) }
    val openArtistBell: () -> Unit = {
        selectedArtistFrequency = state.artistAlertFrequency(artist)
        showArtistBellDialog = true
    }
    val management = state.managementForArtist(artist)
    val context = LocalContext.current
    val encodedArtist = Uri.encode(artist)
    val musicPlatforms = listOf(
        MusicPlatformLink("Spotify", R.drawable.ic_music_spotify, Color(0xFF1DB954), "https://open.spotify.com/search/$encodedArtist"),
        MusicPlatformLink("YouTube Music", R.drawable.ic_music_youtube, Color(0xFFFF0033), "https://music.youtube.com/search?q=$encodedArtist"),
        MusicPlatformLink("Apple Music", R.drawable.ic_music_apple, Color(0xFFFA243C), "https://music.apple.com/es/search?term=$encodedArtist"),
        MusicPlatformLink("Deezer", R.drawable.ic_music_deezer, Color(0xFFA238FF), "https://www.deezer.com/search/$encodedArtist"),
        MusicPlatformLink("TIDAL", R.drawable.ic_music_tidal, Color(0xFF111111), "https://listen.tidal.com/search?q=$encodedArtist"),
        MusicPlatformLink("Bandcamp", R.drawable.ic_music_bandcamp, Color(0xFF629AA9), "https://bandcamp.com/search?q=$encodedArtist")
    )
    val concerts = state.concertsForArtistProfile(artist)
    val inferredCategory = concerts
        .groupingBy { it.category }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key ?: CulturalCategory.MUSIC
    val profileCategory = accountProfile?.discipline
        ?.takeIf(String::isNotBlank)
        ?.let { runCatching { CulturalCategory.valueOf(it) }.getOrNull() }
        ?: inferredCategory
    val isMusicProfile = profileCategory == CulturalCategory.MUSIC
    val profileAccent = professionalProfileAccent(profileCategory, concerts)
    val activityLabel = when (profileCategory) {
        CulturalCategory.MUSIC -> "Concerts"
        CulturalCategory.THEATRE -> "Espectacles"
        CulturalCategory.CINEMA -> "Projeccions i activitats"
        CulturalCategory.TALKS -> "Xarrades i activitats"
        CulturalCategory.DANCE -> "Espectacles"
        CulturalCategory.EXHIBITIONS -> "Exposicions i activitats"
        CulturalCategory.LITERATURE -> "Presentacions i activitats"
        CulturalCategory.WORKSHOPS -> "Tallers i activitats"
        CulturalCategory.CHILDREN -> "Activitats"
        CulturalCategory.POPULAR_CULTURE -> "Activitats"
        CulturalCategory.HERITAGE -> "Visites i activitats"
        CulturalCategory.OTHER -> "Activitats"
    }
    val image = rememberImageBitmapFromUri(state.publicPhotoForArtist(artist))

    if (showArtistBellDialog) {
        AlertDialog(
            onDismissRequest = { showArtistBellDialog = false },
            title = { Text("Quan vols avisos de $artist?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Només t'avisarem de noves activitats d'este artista o grup si actives la campaneta.")
                    BellFollowedFrequency.entries.forEach { frequency ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { selectedArtistFrequency = frequency },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedArtistFrequency == frequency, onClick = { selectedArtistFrequency = frequency })
                            Text(frequency.label)
                        }
                    }
                    Text(
                        "Els avisos Android fora de l'app requerixen el servei de notificacions push del servidor; " +
                            "ací es guarda la teua preferència.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    state.configureArtistAlert(artist, true, selectedArtistFrequency)
                    showArtistBellDialog = false
                    Toast.makeText(context, "Avisos de $artist guardats · ${selectedArtistFrequency.label}", Toast.LENGTH_SHORT).show()
                }) { Text(if (alertsEnabled) "Guardar freqüència" else "Activar campaneta") }
            },
            dismissButton = {
                Row {
                    if (alertsEnabled) {
                        TextButton(onClick = {
                            state.configureArtistAlert(artist, false)
                            showArtistBellDialog = false
                            Toast.makeText(context, "Avisos de $artist desactivats.", Toast.LENGTH_SHORT).show()
                        }) { Text("Desactivar") }
                    }
                    TextButton(onClick = { showArtistBellDialog = false }) { Text("Cancel·lar") }
                }
            }
        )
    }

    LaunchedEffect(artist) {
        state.recordArtistView(artist)
        state.ensureFullAgendaLoaded("artist_profile")
        state.reconcileCatalogForLookup()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 28.dp
        )
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Tornar")
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .height(230.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.5.dp, profileAccent.copy(alpha = .88f), RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (image != null) {
                    Image(
                        bitmap = image,
                        contentDescription = artist,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        artist.take(2).uppercase(),
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .45f)
                    )
                }
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = .62f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .25f))
                ) {
                    Text(profileCategory.label,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(onClick = { state.toggleFollowArtist(artist) }, modifier = Modifier.weight(1f)) {
                    Text(if (followed) "Seguint" else "+ Seguir")
                }
                OutlinedButton(onClick = openArtistBell, modifier = Modifier.weight(1f)) {
                    Icon(if (alertsEnabled) Icons.Outlined.NotificationsActive else Icons.Outlined.Notifications,
                        contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (alertsEnabled) "Avisos actius" else "Notificacions")
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(artist, fontSize = 27.sp, fontWeight = FontWeight.Black)
                        management?.let { agency ->
                            Text(
                                "Management · ${agency.displayName}",
                                modifier = Modifier.padding(top = 3.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .72f)
                            )
                        }
                        if (!isMusicProfile && profile.hometown.isNotBlank()) {
                            Text(
                                profile.hometown,
                                modifier = Modifier.padding(top = if (management != null) 2.dp else 0.dp),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .58f)
                            )
                        }
                    }

                }

                if (isMusicProfile) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        items(musicPlatforms) { platform ->
                            Surface(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .clickable { openExternalUrl(context, platform.url) },
                                shape = CircleShape,
                                color = platform.color
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(platform.iconRes),
                                        contentDescription = platform.name,
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                val bio = accountProfile?.bio?.takeIf { it.isNotBlank() } ?: profile.bio
                if (bio.isNotBlank()) {
                    Text(bio, modifier = Modifier.padding(top = 14.dp), fontSize = 14.sp)
                }

                accountProfile?.let { ProfessionalDisciplineInfo(it, profileCategory) }

                if (canEditProfessional && accountProfile != null) {
                    OutlinedButton(
                        onClick = { showProfessionalEditor = true },
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Editar dades professionals")
                    }
                }

                Text(
                    activityLabel,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp
                )
            }
        }

        if (concerts.isEmpty()) {
            item {
                Text(
                    "No hi ha ${activityLabel.lowercase(Locale.getDefault())} publicades.",
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(concerts, key = { it.id }) { concert ->
                CompactConcertItem(
                    state = state,
                    concert = concert,
                    onOpenArtist = {},
                    onOpenConcert = { onOpenConcert(concert) }
                )
            }
        }
        if (isMusicProfile) {
            item {
                Text("Productes", modifier = Modifier.padding(start = 18.dp, top = 22.dp, bottom = 8.dp),
                    fontSize = 20.sp, fontWeight = FontWeight.Black)
                GroupMerchSection(state = state, artist = artist)
            }
        }
    }

    if (showProfessionalEditor && accountProfile != null && artistAccount != null) {
        ProfessionalDisciplineEditor(
            state = state,
            username = artistAccount.username,
            profile = accountProfile,
            inferredCategory = profileCategory,
            onDismiss = { showProfessionalEditor = false }
        )
    }
}

@Composable
private fun ProfessionalDisciplineInfo(profile: AccountProfile, category: CulturalCategory) {
    val rows = when (category) {
        CulturalCategory.THEATRE, CulturalCategory.DANCE -> listOf(
            "Companyia / projecte" to profile.companyName,
            "Repartiment / equip" to profile.castText,
            "Espectacles / obres" to profile.worksText
        )
        CulturalCategory.LITERATURE -> listOf(
            "Editorial" to profile.publisherName,
            "Llibres / publicacions" to profile.booksText,
            "Projecte" to profile.companyName
        )
        CulturalCategory.EXHIBITIONS -> listOf(
            "Obra / exposicions" to profile.worksText,
            "Espais / galeries" to profile.spacesText,
            "Projecte / col·lectiu" to profile.companyName
        )
        CulturalCategory.CINEMA -> listOf(
            "Productora / projecte" to profile.companyName,
            "Equip / repartiment" to profile.castText,
            "Obres / projeccions" to profile.worksText
        )
        CulturalCategory.MUSIC -> listOf(
            "Projecte" to profile.companyName,
            "Discografia / llançaments" to profile.worksText
        )
        else -> listOf(
            "Projecte / entitat" to profile.companyName,
            "Activitats / obra" to profile.worksText,
            "Espais" to profile.spacesText
        )
    }.filter { it.second.isNotBlank() }
    if (rows.isEmpty()) return
    Column(Modifier.fillMaxWidth().padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { (label, value) ->
            Column {
                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, fontSize = 13.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfessionalDisciplineEditor(
    state: AgendaState,
    username: String,
    profile: AccountProfile,
    inferredCategory: CulturalCategory,
    onDismiss: () -> Unit
) {
    var category by remember(profile.discipline, inferredCategory) {
        mutableStateOf(runCatching { CulturalCategory.valueOf(profile.discipline) }.getOrNull() ?: inferredCategory)
    }
    var company by remember(profile.companyName) { mutableStateOf(profile.companyName) }
    var cast by remember(profile.castText) { mutableStateOf(profile.castText) }
    var publisher by remember(profile.publisherName) { mutableStateOf(profile.publisherName) }
    var books by remember(profile.booksText) { mutableStateOf(profile.booksText) }
    var works by remember(profile.worksText) { mutableStateOf(profile.worksText) }
    var spaces by remember(profile.spacesText) { mutableStateOf(profile.spacesText) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = { if (!saving) onDismiss() }) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 34.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("Perfil professional", fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text("La disciplina defineix els camps i seccions que es mostren públicament.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(CulturalCategory.entries) { option ->
                        val categoryPhoto = rememberImageBitmapFromUri(
                            state.publicPhotoForCategory(option),
                            maxDimension = 96
                        )
                        FilterChip(
                            selected = category == option,
                            onClick = { category = option },
                            label = { Text(option.label) },
                            leadingIcon = if (categoryPhoto != null) {
                                {
                                    Image(
                                        bitmap = categoryPhoto,
                                        contentDescription = option.label,
                                        modifier = Modifier.size(26.dp).clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            } else null
                        )
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = company,
                    onValueChange = { company = it },
                    label = { Text(when (category) {
                        CulturalCategory.THEATRE, CulturalCategory.DANCE -> "Companyia / projecte"
                        CulturalCategory.LITERATURE -> "Projecte / autor/a"
                        CulturalCategory.EXHIBITIONS -> "Projecte / col·lectiu"
                        CulturalCategory.CINEMA -> "Productora / projecte"
                        else -> "Projecte / entitat"
                    }) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (category in setOf(CulturalCategory.THEATRE, CulturalCategory.DANCE, CulturalCategory.CINEMA)) {
                item { OutlinedTextField(value = cast, onValueChange = { cast = it }, label = { Text("Repartiment / equip") }, modifier = Modifier.fillMaxWidth(), minLines = 2) }
            }
            if (category == CulturalCategory.LITERATURE) {
                item { OutlinedTextField(value = publisher, onValueChange = { publisher = it }, label = { Text("Editorial") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = books, onValueChange = { books = it }, label = { Text("Llibres / publicacions") }, modifier = Modifier.fillMaxWidth(), minLines = 2) }
            }
            item {
                OutlinedTextField(
                    value = works,
                    onValueChange = { works = it },
                    label = { Text(when (category) {
                        CulturalCategory.MUSIC -> "Discografia / llançaments"
                        CulturalCategory.EXHIBITIONS -> "Obra / exposicions"
                        CulturalCategory.THEATRE, CulturalCategory.DANCE -> "Espectacles / obres"
                        CulturalCategory.CINEMA -> "Obres / projeccions"
                        else -> "Activitats / obra"
                    }) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
            if (category == CulturalCategory.EXHIBITIONS || category == CulturalCategory.OTHER) {
                item { OutlinedTextField(value = spaces, onValueChange = { spaces = it }, label = { Text("Espais / galeries") }, modifier = Modifier.fillMaxWidth(), minLines = 2) }
            }
            error?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) } }
            item {
                Button(
                    enabled = !saving,
                    onClick = {
                        saving = true
                        error = null
                        state.updateAccountProfileRemote(
                            username = username,
                            displayName = profile.displayName,
                            bio = profile.bio,
                            photoUri = profile.photoUri,
                            discipline = category.name,
                            companyName = company,
                            castText = cast,
                            publisherName = publisher,
                            booksText = books,
                            worksText = works,
                            spacesText = spaces
                        ) { ok, message, _ ->
                            saving = false
                            if (ok) onDismiss() else error = message
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (saving) "Guardant…" else "Guardar perfil professional") }
            }
        }
    }
}

private fun profileDisciplineAccent(category: CulturalCategory): Color = when (category) {
    CulturalCategory.MUSIC -> Color(0xFF7357B4)
    CulturalCategory.THEATRE -> Color(0xFF9A4E56)
    CulturalCategory.CINEMA -> Color(0xFF4E6178)
    CulturalCategory.TALKS -> Color(0xFF4E756C)
    CulturalCategory.DANCE -> Color(0xFF8A5B75)
    CulturalCategory.EXHIBITIONS -> Color(0xFF987444)
    CulturalCategory.LITERATURE -> Color(0xFF5E704D)
    CulturalCategory.WORKSHOPS -> Color(0xFF577983)
    CulturalCategory.CHILDREN -> Color(0xFF94784B)
    CulturalCategory.POPULAR_CULTURE -> Color(0xFF8B6545)
    CulturalCategory.HERITAGE -> Color(0xFF66745A)
    CulturalCategory.OTHER -> Color(0xFF62666C)
}

private fun professionalProfileAccent(category: CulturalCategory, concerts: List<Concert>): Color {
    if (category != CulturalCategory.MUSIC) return profileDisciplineAccent(category)
    val style = concerts.asSequence().flatMap { it.styles.asSequence() }.joinToString(" ").lowercase(Locale.getDefault())
    return when {
        listOf("cambra", "chamber").any { style.contains(it) } -> Color(0xFF4F7D78)
        listOf("clàss", "class", "simf", "òpera", "opera", "líric", "barroc", "orquestr").any { style.contains(it) } -> Color(0xFF4A628C)
        listOf("jazz", "blues", "soul", "gospel", "swing").any { style.contains(it) } -> Color(0xFF8A6846)
        listOf("folk", "tradicional", "tradició", "arrel", "mediterr", "world music").any { style.contains(it) } -> Color(0xFF6C7A48)
        listOf("rock", "punk", "metal", "hardcore", "grunge").any { style.contains(it) } -> Color(0xFF8B4A4A)
        listOf("rap", "hip hop", "hip-hop", "trap", "urbana", "reggaeton", "reggaetón").any { style.contains(it) } -> Color(0xFF5F4D79)
        listOf("electr", "techno", "house", "ambient", "dance", "dj").any { style.contains(it) } -> Color(0xFF437989)
        listOf("pop", "indie", "cantautor", "singer-songwriter").any { style.contains(it) } -> Color(0xFF9A5C7E)
        else -> profileDisciplineAccent(CulturalCategory.MUSIC)
    }
}


private fun openExternalUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}

@Composable
private fun PublicArtistAvatar(label: String, photoUri: String?, size: androidx.compose.ui.unit.Dp) {
    val bitmap = rememberImageBitmapFromUri(photoUri)
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(
            Brush.linearGradient(listOf(Color(0xFF833AB4), Color(0xFFFD1D1D), Color(0xFFFCAF45)))
        ),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(bitmap = bitmap, contentDescription = label, modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.Crop)
        } else {
            Text(label.take(2).uppercase(), color = Color.White, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun ProfileStat(value: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), fontWeight = FontWeight.Black, fontSize = 18.sp)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f))
    }
}

@Composable
fun ArtistAnalyticsSection(state: AgendaState, artist: String) {
    if (artist.isBlank() || !state.canManageArtist(artist)) return
    val analytics = state.artistAnalytics(artist)

    Text("Analítica de $artist", modifier = Modifier.padding(top = 24.dp), fontWeight = FontWeight.Black, fontSize = 18.sp)
    Text("Dades agregades; mai mostren perfils particulars.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .58f))
    LazyRow(modifier = Modifier.padding(top = 9.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item { MetricCard("Guardats", analytics.saved) }
        item { MetricCard("M'agrada", analytics.likes) }
        item { MetricCard("Vull anar", analytics.wantToGo) }
        item { MetricCard("Aniré", analytics.going) }
        item { MetricCard("Converses", analytics.conversations) }
    }
    Text("Procedència agregada", modifier = Modifier.padding(top = 12.dp), fontWeight = FontWeight.Bold)
    Text(analytics.topTowns.joinToString(" · ") { "${it.first} ${it.second}%" }, fontSize = 12.sp)
    Text("Estils compartits", modifier = Modifier.padding(top = 8.dp), fontWeight = FontWeight.Bold)
    Text(analytics.sharedStyles.joinToString(" · ") { "${it.first} ${it.second}%" }, fontSize = 12.sp)
}

@Composable
private fun MetricCard(label: String, value: Int) {
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.width(105.dp)) {
        Column(Modifier.padding(12.dp)) {
            Icon(Icons.Outlined.Insights, null, modifier = Modifier.size(18.dp))
            Text(value.toString(), fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 5.dp))
            Text(label, fontSize = 11.sp)
        }
    }
}

private fun prettyDateSocial(iso: String): String = AgendaDateFormat.date(iso)
