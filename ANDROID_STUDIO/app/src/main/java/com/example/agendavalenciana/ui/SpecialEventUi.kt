
package com.example.agendavalenciana.ui

import com.example.agendavalenciana.model.AgendaDateFormat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agendavalenciana.model.Concert
import com.example.agendavalenciana.data.ServerApiRepository
import com.example.agendavalenciana.state.AgendaState
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

internal data class SpecialEventConfig(
    val displayTitle: String,
    val heroImageUri: String? = null,
    val accessTitle: String,
    val subtitle: String,
    val dateLabel: String,
    val venueLabel: String,
    val styles: List<String>,
    val infoGeneral: List<Pair<String, String>>,
    val schedule: List<SpecialScheduleItem>,
    val services: List<SpecialServiceItem>,
    val channels: List<SpecialCommunityChannel>,
    /** Etiqueta humana opcional de cada jornada: p. ex. CAPLA Kids / CAPLA Festival. */
    val dayLabels: Map<String, String> = emptyMap(),
    /** Modalitats informatives de venda externa per al popup de festival. */
    val festivalTickets: List<FestivalTicketInfo> = emptyList()
)

internal data class FestivalTicketInfo(
    val name: String,
    val priceLabel: String,
    val details: String = "",
    val date: String = "",
    val url: String = ""
)

internal data class SpecialScheduleItem(
    val time: String,
    val title: String,
    val location: String,
    val details: String,
    val artistName: String? = null,
    val imageUri: String? = null,
    val date: String = "",
    val endTime: String? = null,
    val sortOrder: Int = 0,
    val groupName: String = "",
    val itemType: String = "performance"
)

internal data class SpecialServiceProduct(
    val name: String,
    val description: String,
    val price: Double,
    val reserveOnly: Boolean = false,
    val id: Long = 0L
)

internal data class SpecialServiceItem(
    val name: String,
    val description: String,
    val accent: Color,
    val products: List<SpecialServiceProduct>,
    val imageUri: String? = null,
    val id: Long = 0L
)

internal data class SpecialServicePurchaseItem(
    val productName: String,
    val quantity: Int,
    val unitPrice: Double
) {
    val lineTotal: Double get() = quantity * unitPrice
}

internal data class SpecialServicePurchase(
    val id: String,
    val serviceName: String,
    val items: List<SpecialServicePurchaseItem>,
    val totalPrice: Double,
    val qrPayload: String
)

internal data class SpecialCommunityChannel(
    val name: String,
    val description: String,
    val stats: String
)

private enum class SpecialTab(val label: String) {
    INFO("Info"),
    HORARIS("Horaris"),
    SERVEIS("Serveis"),
    COMUNITAT("Comunitat")
}


internal fun demoMediterraniSpecialConcert(): Concert = Concert(
    // Mateix ID que l'esdeveniment real migrat a MySQL: permet carregar serveis/productes reals.
    id = 946,
    artist = "Mediterrani Skate Fest",
    title = "Mediterrani Skate Fest",
    lineup = listOf("No Fun At All", "CRIM", "Romàntic Dimoni", "Kill the President", "Kids of Rage", "Vacants", "PÖLS", "Apologia", "Ruxe Ruxe", "Zetkin", "Adur", "L'Home Brut", "Morgue"),
    posterUri = "asset://special_events/mediterrani_skate_fest.jpg",
    date = "2027-04-03",
    time = "10:00",
    town = "Vila-real",
    comarca = "La Plana Baixa",
    venue = "Skate Park de Vila-real",
    styles = listOf("Skate", "Punk", "Cultura urbana"),
    price = 0,
    language = "Valencià",
    description = "Espai especial de prova per al Mediterrani Skate Fest.",
    ticketPrice = 0,
    ticketAvailable = true
)

internal fun specialEventConfigFor(concert: Concert): SpecialEventConfig? {
    // Exemple complet i fallback local del sistema general de festivals.
    // Quan el servidor ja tinga la programació sincronitzada, remoteSpecialConfig()
    // prevaldrà automàticament sobre esta configuració local.
    if (concert.id == 1159) {
        val ticketUrl = "https://caplafestival.com/comprar-entradas/"
        return SpecialEventConfig(
            displayTitle = "Capla Festival 2026",
            heroImageUri = "https://caplafestival.com/wp-content/uploads/2026/09/Cartel-inicio-web-082.jpg",
            accessTitle = "Capla Festival 2026",
            subtitle = "Dos jornades a Castelló: Capla Kids divendres i el festival principal dissabte.",
            dateLabel = "02/10/2026 – 03/10/2026",
            venueLabel = "Recinte de Fires i Mercats de Castelló · Castelló de la Plana",
            styles = listOf("Rock", "Pop", "Indie", "Electrònica", "DJ"),
            infoGeneral = listOf(
                "02/10/2026 · Capla Kids" to "De 17:00 a 23:00. Jornada familiar amb entrada gratuïta fins a completar aforament, 26 inflables i atraccions, Resilientes a les 19:00, Moron 5 a les 21:00, sessions de DJ entre actuacions i food trucks.",
                "03/10/2026 · Capla Festival" to "Obertura d'accés des de les 15:30 i programació musical en dos escenaris fins a la matinada.",
                "Menors" to "El dissabte no poden accedir menors de 16 anys. Les persones de 16 i 17 anys han de portar l'autorització i la documentació exigida per l'organització.",
                "Gastronomia" to "Food trucks amb oferta variada i opcions veganes i sense gluten.",
                "Transport públic" to "Autobusos CVCS-105-4, L3 i L17, i TRAM T1, amb parades pròximes al Recinte Ferial.",
                "Aparcament" to "Antic Mercat (150 m), aparcament gratuït dels Jutjats (400 m) i APK2 Avinguda del Mar (550 m)."
            ),
            schedule = listOf(
                SpecialScheduleItem("17:00", "Obertura Capla Kids", "Accés", "Entrada gratuïta fins a completar aforament.", date = "2026-10-02", endTime = "23:00", sortOrder = 10),
                SpecialScheduleItem("17:00", "Inflables gegants", "Zona familiar", "Activitat de 17:00 a 23:00. 1 € per viatge o polsera il·limitada de 10 €.", date = "2026-10-02", endTime = "23:00", sortOrder = 20),
                SpecialScheduleItem("19:00", "Resilientes", "Escenari Capla Kids", "Concert. Hi haurà sessions de DJ entre actuacions.", "Resilientes", date = "2026-10-02", sortOrder = 30),
                SpecialScheduleItem("21:00", "Moron 5", "Escenari Capla Kids", "Concert. Hi haurà sessions de DJ entre actuacions.", "Moron 5", date = "2026-10-02", sortOrder = 40),
                SpecialScheduleItem("17:00", "Food trucks", "Zona gastronòmica", "Oferta variada amb opcions veganes i sense gluten.", date = "2026-10-02", endTime = "23:00", sortOrder = 50),

                SpecialScheduleItem("15:30", "Obertura de portes", "Accés", "Accés al recinte del festival principal.", date = "2026-10-03", sortOrder = 100),
                SpecialScheduleItem("15:45", "Marc Barceló", "Escenari Fraguel Rock", "Sessió DJ", "Marc Barceló", date = "2026-10-03", sortOrder = 110),
                SpecialScheduleItem("16:30", "Provi Morcillo", "Escenari Estrella Damm", "Concert", "Provi Morcillo", date = "2026-10-03", sortOrder = 120),
                SpecialScheduleItem("17:40", "Los Romeos", "Escenari Estrella Damm", "Concert", "Los Romeos", date = "2026-10-03", sortOrder = 130),
                SpecialScheduleItem("17:45", "DJ Paco Roca", "Escenari Fraguel Rock", "Sessió DJ", "DJ Paco Roca", date = "2026-10-03", sortOrder = 140),
                SpecialScheduleItem("19:35", "Los Indi.os", "Escenari Estrella Damm", "Concert", "Los Indi.os", date = "2026-10-03", sortOrder = 150),
                SpecialScheduleItem("19:45", "DJ Rafa Sánchez", "Escenari Fraguel Rock", "Sessió DJ", "DJ Rafa Sánchez", date = "2026-10-03", sortOrder = 160),
                SpecialScheduleItem("21:05", "Miss Caffeina", "Escenari Estrella Damm", "Concert", "Miss Caffeina", date = "2026-10-03", sortOrder = 170),
                SpecialScheduleItem("21:45", "Don Fluor", "Escenari Fraguel Rock", "Sessió DJ", "Don Fluor", date = "2026-10-03", sortOrder = 180),
                SpecialScheduleItem("23:15", "Loquillo", "Escenari Estrella Damm", "Concert", "Loquillo", date = "2026-10-03", sortOrder = 190),
                SpecialScheduleItem("23:45", "Toxicosmos", "Escenari Fraguel Rock", "Sessió DJ", "Toxicosmos", date = "2026-10-03", sortOrder = 200),
                SpecialScheduleItem("01:15", "OBK", "Escenari Estrella Damm", "Concert", "OBK", date = "2026-10-03", sortOrder = 210),
                SpecialScheduleItem("01:45", "DJ Pepe Roca", "Escenari Fraguel Rock", "Sessió DJ", "DJ Pepe Roca", date = "2026-10-03", sortOrder = 220),
                SpecialScheduleItem("04:00", "Tancament", "Recinte", "Fi de la programació publicada.", date = "2026-10-03", sortOrder = 230)
            ),
            services = emptyList(),
            channels = emptyList(),
            dayLabels = mapOf(
                "2026-10-02" to "Capla Kids",
                "2026-10-03" to "Capla Festival"
            ),
            festivalTickets = listOf(
                FestivalTicketInfo("Capla Kids", "Gratuït", "Entrada gratuïta fins a completar aforament.", "2026-10-02"),
                FestivalTicketInfo("Entrada general", "42 € + g.d.", "Accés a tots els concerts i reaccés il·limitat. Majors de 16 anys.", "2026-10-03", ticketUrl),
                FestivalTicketInfo("Entrada VIP", "85 € + g.d.", "Carril preferent, zona VIP/Chill Out, banys i barra VIP.", "2026-10-03", ticketUrl),
                FestivalTicketInfo("VIP + hotel + desplaçaments", "124 €/persona", "Habitació doble per a 2 persones, hotel del 03/10/2026 al 04/10/2026 i transport d'anada i tornada.", "2026-10-03", ticketUrl),
                FestivalTicketInfo("VIP + hotel + desplaçaments", "163 €", "Habitació individual, hotel del 03/10/2026 al 04/10/2026 i transport d'anada i tornada.", "2026-10-03", ticketUrl)
            )
        )
    }

    // Panell local de prova existent. La resta només apareixen quan
    // s'activen manualment des de la configuració especial del servidor.
    if (concert.id != 946) return null
    return SpecialEventConfig(
        displayTitle = "Mediterrani Skate Fest",
        accessTitle = "Mediterrani Skate Fest",
        subtitle = "Accedix als horaris, el mapa, els serveis, la info general i la comunitat del festival.",
        dateLabel = if (concert.date.isNotBlank()) AgendaDateFormat.date(concert.date) else "03/04/2027",
        venueLabel = if (concert.venue.isNotBlank()) concert.venue + " · " + concert.town else "Skate Park de Vila-real",
        styles = listOf("Skate", "Punk", "Food Trucks", "Cultura urbana"),
        infoGeneral = listOf(
            "Accés" to "Obertura del recinte a les 10:00. Main stage a partir de les 16:00.",
            "Polsera / entrada" to "Mostra el QR de l’entrada en l’accés principal i et posaran la polsera del festival.",
            "Pagaments" to "Barra, merch i alguns serveis es poden reservar des de l’app i recollir al recinte.",
            "Menors" to "Les persones menors d’edat han d’anar amb l’autorització corresponent.",
            "Recinte" to "Hi haurà zona de descans, WC, primers auxilis, food trucks i punt d’informació."
        ),
        schedule = listOf(
            SpecialScheduleItem("10:00", "Obertura Skate Park", "Skate Park", "Recepció, escalfament i pràctica lliure"),
            SpecialScheduleItem("11:00", "Competició de skate", "Skate Park", "Categories street i bowl amb premis"),
            SpecialScheduleItem("14:00", "Food trucks + tallers", "Zona food & cultura", "Menjar, tallers artesanals i fanzins"),
            SpecialScheduleItem("16:00", "Morgue", "Main Stage · Parking Matilde Salvador", "Concert", "Morgue"),
            SpecialScheduleItem("16:35", "Adur", "Main Stage · Parking Matilde Salvador", "Concert", "Adur"),
            SpecialScheduleItem("17:10", "L'Home Brut", "Main Stage · Parking Matilde Salvador", "Concert", "L'Home Brut"),
            SpecialScheduleItem("17:45", "PÖLS", "Main Stage · Parking Matilde Salvador", "Concert", "PÖLS"),
            SpecialScheduleItem("18:20", "Zetkin", "Main Stage · Parking Matilde Salvador", "Concert", "Zetkin"),
            SpecialScheduleItem("18:55", "Apologia", "Main Stage · Parking Matilde Salvador", "Concert", "Apologia"),
            SpecialScheduleItem("19:30", "Vacants", "Main Stage · Parking Matilde Salvador", "Concert", "Vacants"),
            SpecialScheduleItem("20:05", "Kids of Rage", "Main Stage · Parking Matilde Salvador", "Concert", "Kids of Rage"),
            SpecialScheduleItem("20:45", "Ruxe Ruxe", "Main Stage · Parking Matilde Salvador", "Concert", "Ruxe Ruxe"),
            SpecialScheduleItem("21:25", "Kill the President", "Main Stage · Parking Matilde Salvador", "Concert", "Kill the President"),
            SpecialScheduleItem("22:10", "Romàntic Dimoni", "Main Stage · Parking Matilde Salvador", "Concert", "Romàntic Dimoni"),
            SpecialScheduleItem("23:00", "CRIM", "Main Stage · Parking Matilde Salvador", "Concert", "CRIM"),
            SpecialScheduleItem("00:00", "No Fun At All", "Main Stage · Parking Matilde Salvador", "Concert", "No Fun At All")
        ),
        services = listOf(
            SpecialServiceItem(
                "Barra",
                "Compra abans i arreplega directament en barra mostrant el QR.",
                Color(0xFFFFE082),
                listOf(
                    SpecialServiceProduct("Aigua", "Botella 50 cl", 2.0),
                    SpecialServiceProduct("Refresc", "Llanda o botella", 3.0),
                    SpecialServiceProduct("Cervesa", "Vas retornable", 3.5),
                    SpecialServiceProduct("Entrepà vegetal", "Entrepà preparat al moment", 5.0)
                ),
                imageUri = "asset://special_events/mediterrani_skate_fest.jpg"
            ),
            SpecialServiceItem(
                "Merch",
                "Reserva merch oficial i arreplega'l sense cues al punt de venda.",
                Color(0xFFFFCDD2),
                listOf(
                    SpecialServiceProduct("Samarreta oficial", "Talles S · M · L · XL", 18.0),
                    SpecialServiceProduct("Tote bag", "Bossa Mediterrani Skate Fest", 8.0),
                    SpecialServiceProduct("Gorra", "Gorra oficial del festival", 15.0),
                    SpecialServiceProduct("Pòster", "Cartell oficial imprés", 5.0)
                ),
                imageUri = "asset://special_events/mediterrani_skate_fest.jpg"
            ),
            SpecialServiceItem(
                "Food Trucks",
                "Demana menjar des de l'app i mostra el QR quan estiga preparat.",
                Color(0xFFB3E5FC),
                listOf(
                    SpecialServiceProduct("Burger vegana + creïlles", "Menú complet", 10.0),
                    SpecialServiceProduct("Pizza", "Ració gran", 9.0),
                    SpecialServiceProduct("Creïlles", "Ració individual", 4.0),
                    SpecialServiceProduct("Café", "Café o tallat", 2.0)
                ),
                imageUri = "asset://special_events/mediterrani_skate_fest.jpg"
            ),
            SpecialServiceItem(
                "SkatePark",
                "Reserva activitats i serveis del SkatePark amb plaça limitada.",
                Color(0xFFC8E6C9),
                listOf(
                    SpecialServiceProduct("Sessió Bowl", "Reserva de franja de 30 minuts", 3.0, reserveOnly = true),
                    SpecialServiceProduct("Best Trick", "Inscripció a la sessió oberta", 5.0, reserveOnly = true),
                    SpecialServiceProduct("Taller iniciació", "Taller guiat de 45 minuts", 8.0, reserveOnly = true),
                    SpecialServiceProduct("Guarda-taula", "Custòdia de skate durant el festival", 2.0, reserveOnly = true)
                ),
                imageUri = "asset://special_events/mediterrani_skate_fest.jpg"
            )
        ),
        channels = listOf(
            SpecialCommunityChannel("# General", "Tot sobre l’esdeveniment i avisos útils.", "532 missatges · fa 5 min"),
            SpecialCommunityChannel("# Transport", "Com arribar, compartir cotxe i tornada.", "128 missatges · fa 12 min"),
            SpecialCommunityChannel("# Allotjament", "Hotels, apartaments i càmping.", "76 missatges · fa 1 h"),
            SpecialCommunityChannel("# Quedades", "Troba companyia per anar junts o conéixer gent.", "94 missatges · fa 2 h")
        )
    )
}

internal fun remoteSpecialConfig(remote: ServerApiRepository.RemoteSpecialEvent, concert: Concert): SpecialEventConfig {
    val info = remote.generalInfo.lines().mapNotNull { raw ->
        val line = raw.trim()
        if (line.isBlank()) null else {
            val parts = line.split("|", limit = 2)
            if (parts.size == 2) parts[0].trim() to parts[1].trim() else "Informació" to line
        }
    }.ifEmpty { listOf("Informació" to "Consulta ací tota la informació general de l’esdeveniment.") }
    return SpecialEventConfig(
        displayTitle = remote.displayTitle.ifBlank { concert.title },
        heroImageUri = remote.heroImageUrl,
        accessTitle = remote.displayTitle.ifBlank { concert.title },
        subtitle = "Horaris, serveis, informació i comunitat de l’esdeveniment.",
        dateLabel = run {
            val start = remote.startAt?.take(10)?.takeIf(String::isNotBlank) ?: concert.date.take(10)
            val end = concert.scheduleDates.lastOrNull()?.take(10)
                ?: concert.endDate.take(10).takeIf(String::isNotBlank)
            if (!end.isNullOrBlank() && end != start) "${AgendaDateFormat.date(start)} – ${AgendaDateFormat.date(end)}" else AgendaDateFormat.date(start)
        },
        venueLabel = remote.venueLabel.ifBlank { listOf(concert.venue,concert.town).filter(String::isNotBlank).joinToString(" · ") },
        styles = emptyList(),
        infoGeneral = info,
        schedule = remote.schedule.map { s ->
            SpecialScheduleItem(
                time = s.startTime?.take(5).orEmpty(),
                title = s.title,
                location = s.stage.ifBlank { s.location },
                details = s.details,
                artistName = s.artistName,
                imageUri = s.imageUrl,
                date = s.scheduleDate.ifBlank { s.startAt.take(10) },
                endTime = s.endTime?.take(5),
                sortOrder = s.sortOrder,
                groupName = s.groupName,
                itemType = s.itemType
            )
        },
        services = remote.services.map { s ->
            val accent = runCatching { Color(android.graphics.Color.parseColor(s.overlayColor)) }.getOrDefault(Color(0xFFFFE082))
            SpecialServiceItem(
                name = s.name,
                description = s.description,
                accent = accent,
                products = s.products.map { p -> SpecialServiceProduct(p.name,p.description,p.priceCents/100.0,p.reserveOnly,p.id) },
                imageUri = s.imageUrl,
                id = s.id
            )
        },
        channels = emptyList()
    )
}

private fun specialEventCountdownLabel(concert: Concert): String? {
    val eventDate = runCatching { LocalDate.parse(concert.date) }.getOrNull() ?: LocalDate.of(2027, 4, 3)
    val today = LocalDate.now()
    val days = ChronoUnit.DAYS.between(today, eventDate)
    return when {
        days > 1 -> "Falten $days dies"
        days == 1L -> "Falta 1 dia"
        days == 0L -> "Hui"
        else -> null
    }
}

private fun specialEventDateTimeLabel(concert: Concert): String {
    val rawDate = concert.feedDate.ifBlank { concert.date }
    val eventDate = runCatching { LocalDate.parse(rawDate.take(10)) }.getOrNull()
    val dateLabel = eventDate?.let { AgendaDateFormat.cardDate(it.toString()) } ?: AgendaDateFormat.cardDate(rawDate)
    val time = concert.time.trim()
    return if (time.isBlank()) dateLabel else "$dateLabel · $time"
}

@Composable
internal fun SpecialEventAccessCard(
    state: AgendaState,
    concert: Concert,
    onOpen: () -> Unit
) {
    val config = state.specialEventRemote(concert.id)?.let { remoteSpecialConfig(it, concert) }
        ?: specialEventConfigFor(concert) ?: return
    val image = rememberImageBitmapFromUris(listOf(config.heroImageUri) + concert.imageCandidates())
    val countdownLabel = specialEventCountdownLabel(concert)
    val dateTimeLabel = specialEventDateTimeLabel(concert)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101216))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
            ) {
                if (image != null) {
                    Image(
                        bitmap = image,
                        contentDescription = config.displayTitle,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Surface(shape = RoundedCornerShape(999.dp), color = Color(0xFFFFD54F)) {
                        Text(
                            "Ja tens entrada",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color.Black
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(config.accessTitle, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(3.dp))
                    Text(dateTimeLabel, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(config.venueLabel, color = Color.White.copy(alpha = 0.80f), fontSize = 12.sp)
                }
            }
            if (countdownLabel != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 14.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFD54F)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⏳", fontSize = 15.sp)
                        Spacer(Modifier.width(7.dp))
                        Text(
                            "$countdownLabel per a l’esdeveniment",
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Entrar a l’espai de l’esdeveniment",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text("→", color = Color(0xFFFFD54F), fontWeight = FontWeight.Black, fontSize = 24.sp)
            }
        }
    }
}

@Composable
internal fun SpecialEventEntryCard(
    title: String,
    subtitle: String,
    onOpen: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF111318)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, color = Color.White.copy(alpha = 0.86f), fontSize = 13.sp, lineHeight = 18.sp)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Info · Horaris · Serveis · Comunitat", color = Color(0xFFFFD54F), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("Obrir →", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun SpecialEventSpaceScreen(
    state: AgendaState,
    concert: Concert,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onOpenArtist: (String) -> Unit
) {
    LaunchedEffect(concert.id) { state.refreshSpecialEvent(concert.id) }
    val config = state.specialEventRemote(concert.id)?.let { remoteSpecialConfig(it, concert) }
        ?: specialEventConfigFor(concert) ?: return
    var selectedTab by remember(concert.id, concert.feedDate) {
        mutableStateOf(if (concert.isMultiday && concert.selectedDate?.isNotBlank() == true) SpecialTab.HORARIS else SpecialTab.INFO)
    }
    val servicePurchases = remember(concert.id) { mutableStateListOf<SpecialServicePurchase>() }
    val poster = rememberImageBitmapFromUris(concert.imageCandidates(state.profileForArtist(concert.artist)?.photoUri), eventId = concert.id, knownResolvedUri = concert.resolvedCardImageUri)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Tornar")
            }
            Column(Modifier.weight(1f)) {
                Text(config.displayTitle, fontWeight = FontWeight.Black, fontSize = 20.sp)
                Text("Espai especial de l’esdeveniment", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SpecialTab.values().forEach { tab ->
                AssistChip(
                    onClick = { selectedTab = tab },
                    label = { Text(tab.label) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        when (selectedTab) {
            SpecialTab.INFO -> SpecialSummaryTab(config = config, concert = concert, posterAvailable = poster != null, onShowTab = { selectedTab = it }, posterBitmapPresent = poster != null)
            SpecialTab.HORARIS -> SpecialScheduleTab(state = state, concert = concert, config = config)
            SpecialTab.SERVEIS -> SpecialServicesTab(state = state, concert = concert, config = config, purchases = servicePurchases)
            SpecialTab.COMUNITAT -> SpecialCommunityTab(state = state, concert = concert)
        }
    }
}

@Composable
private fun SpecialSummaryTab(
    config: SpecialEventConfig,
    concert: Concert,
    posterAvailable: Boolean,
    posterBitmapPresent: Boolean,
    onShowTab: (SpecialTab) -> Unit
) {
    val image = rememberImageBitmapFromUris(listOf(config.heroImageUri) + concert.imageCandidates())
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(shape = RoundedCornerShape(24.dp)) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .background(Color(0xFF12161C))
                    ) {
                        if (image != null) {
                            Image(
                                bitmap = image,
                                contentDescription = config.displayTitle,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))))
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                        ) {
                            Text(config.displayTitle, color = Color.White, fontWeight = FontWeight.Black, fontSize = 28.sp)
                            Spacer(Modifier.height(6.dp))
                            Text("${AgendaDateFormat.dateRangeLabel(config.dateLabel)} · ${config.venueLabel}", color = Color.White.copy(alpha = 0.92f), fontSize = 13.sp)
                        }
                    }
                    Column(Modifier.padding(16.dp)) {
                        EventQuickActions(onShowTab = onShowTab)
                    }
                }
            }
        }
        item {
            Text("Info general", fontSize = 19.sp, fontWeight = FontWeight.Black)
        }
        items(config.infoGeneral) { row ->
            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)) {
                Column(Modifier.padding(14.dp)) {
                    Text(row.first, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(row.second, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), fontSize = 13.sp, lineHeight = 18.sp)
                }
            }
        }
        item {
            Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFF101216)) {
                Column(Modifier.padding(16.dp)) {
                    Text("El meu esdeveniment", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Spacer(Modifier.height(12.dp))
                    SummaryListRow("Entrada reservada", "General · 1 persona")
                    SummaryListRow("No Fun At All", "Recordatori activat")
                }
            }
        }
    }
}

@Composable
private fun EventQuickActions(onShowTab: (SpecialTab) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickActionTile("Info", Color(0xFFF3F5F7), Icons.Filled.Info, Modifier.weight(1f)) { onShowTab(SpecialTab.INFO) }
            QuickActionTile("Horaris", Color(0xFFFFECB3), Icons.Outlined.CalendarMonth, Modifier.weight(1f)) { onShowTab(SpecialTab.HORARIS) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickActionTile("Serveis", Color(0xFFFFCDD2), Icons.Outlined.Storefront, Modifier.weight(1f)) { onShowTab(SpecialTab.SERVEIS) }
            QuickActionTile("Comunitat", Color(0xFF101216), Icons.Outlined.ChatBubbleOutline, Modifier.weight(1f), dark = true) { onShowTab(SpecialTab.COMUNITAT) }
        }
    }
}

@Composable
private fun QuickActionTile(
    label: String,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    dark: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(88.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = color
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(icon, contentDescription = label, tint = if (dark) Color.White else Color.Black)
            Text(label, color = if (dark) Color.White else Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SummaryListRow(title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.1f)))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color.White.copy(alpha = 0.74f), fontSize = 12.sp)
        }
        Text("›", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
    }
}

private fun formatScheduleDay(raw: String): String = AgendaDateFormat.date(raw)

@Composable
private fun SpecialScheduleTab(
    state: AgendaState,
    concert: Concert,
    config: SpecialEventConfig
) {
    val posterFallback = concert.posterUri?.takeIf { it.isNotBlank() }
        ?: "asset://special_events/mediterrani_skate_fest.jpg"
    val dates = config.schedule.asSequence().map { it.date.take(10) }.filter(String::isNotBlank).distinct().sorted().toList()
    val preferredDate = concert.selectedDate?.takeIf { it in dates }
        ?: concert.feedOccurrenceDate?.takeIf { it in dates }
        ?: concert.feedDate.takeIf { it in dates }
        ?: dates.firstOrNull().orEmpty()
    var selectedDate by remember(concert.id, concert.feedDate, dates) { mutableStateOf(preferredDate) }
    val daySchedule = if (dates.isEmpty()) config.schedule else config.schedule.filter { it.date.take(10) == selectedDate }
    val stages = daySchedule.groupBy { it.location.ifBlank { "Programació" } }
        .toList().sortedBy { (_, rows) -> rows.minOfOrNull { it.sortOrder } ?: Int.MAX_VALUE }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (dates.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(dates, key = { it }) { day ->
                        val active = day == selectedDate
                        if (active) {
                            Button(onClick = { selectedDate = day }) { Text(formatScheduleDay(day)) }
                        } else {
                            OutlinedButton(onClick = { selectedDate = day }) { Text(formatScheduleDay(day)) }
                        }
                    }
                }
            }
            item {
                Text(formatScheduleDay(selectedDate), fontWeight = FontWeight.Black, fontSize = 18.sp)
            }
        }
        if (daySchedule.isEmpty()) {
            item { Text("No hi ha programació publicada per a esta jornada.") }
        }
        stages.forEach { (stage, rows) ->
            item(key = "stage_${selectedDate}_$stage") {
                Text(stage.uppercase(Locale("ca", "ES")), fontWeight = FontWeight.Black, fontSize = 15.sp)
            }
            items(
                rows.sortedWith(compareBy<SpecialScheduleItem> { it.time.isBlank() }.thenBy { it.time }.thenBy { it.sortOrder }),
                key = { "${it.date}_${it.location}_${it.time}_${it.title}_${it.sortOrder}" }
            ) { item ->
                val artistPhoto = item.artistName?.let { state.publicPhotoForArtist(it) }
                val scheduleImage = rememberImageBitmapFromUris(listOf(item.imageUri, artistPhoto, posterFallback))
                Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(width = 86.dp, height = 74.dp)
                                .clip(RoundedCornerShape(15.dp)).background(Color(0xFFE9EDF1))
                        ) {
                            if (scheduleImage != null) {
                                Image(bitmap = scheduleImage, contentDescription = item.artistName ?: item.title,
                                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            val timeLabel = if (item.time.isBlank()) "Sense hora" else buildString {
                                append(item.time); item.endTime?.takeIf(String::isNotBlank)?.let { append(" – ").append(it) }
                            }
                            Text(timeLabel, fontWeight = FontWeight.Black, fontSize = 18.sp)
                            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            item.artistName?.takeIf { it.isNotBlank() && !it.equals(item.title, true) }?.let {
                                Text(it, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                            item.details.takeIf(String::isNotBlank)?.let {
                                Text(it, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f), fontSize = 12.sp, lineHeight = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecialServicesTab(
    state: AgendaState,
    concert: Concert,
    config: SpecialEventConfig,
    purchases: MutableList<SpecialServicePurchase>
) {
    val context = LocalContext.current
    var selectedService by remember { mutableStateOf<SpecialServiceItem?>(null) }
    var qrPurchase by remember { mutableStateOf<SpecialServicePurchase?>(null) }
    var pendingOrderId by remember { mutableStateOf<Long?>(null) }
    var paymentError by remember { mutableStateOf<String?>(null) }
    var paymentBusy by remember { mutableStateOf(false) }
    var paymentSheetOpen by remember { mutableStateOf(false) }

    // Mateix patró que la compra d'entrades. No es reconstrueix un flux Stripe paral·lel.
    val paymentSheet = PaymentSheet.Builder { result ->
        paymentSheetOpen = false
        when (result) {
            is PaymentSheetResult.Completed -> {
                val orderId = pendingOrderId
                if (orderId == null) {
                    paymentError = "Stripe ha finalitzat però falta l'identificador de la comanda."
                    paymentBusy = false
                } else {
                    paymentError = "Pagament fet. Verificant la comanda…"
                    state.finalizeSpecialServicePayment(orderId) { paidOrder, error ->
                        paymentBusy = false
                        if (paidOrder == null) {
                            paymentError = error ?: "No s'ha pogut verificar la compra."
                        } else {
                            val purchase = SpecialServicePurchase(
                                id = paidOrder.orderId.toString(),
                                serviceName = paidOrder.serviceName,
                                items = paidOrder.items.map {
                                    SpecialServicePurchaseItem(
                                        productName = it.productName,
                                        quantity = it.quantity,
                                        unitPrice = it.unitPriceCents / 100.0
                                    )
                                },
                                totalPrice = paidOrder.totalCents / 100.0,
                                qrPayload = paidOrder.qrPayload
                            )
                            purchases.add(0, purchase)
                            qrPurchase = purchase
                            selectedService = null
                            paymentError = null
                        }
                    }
                }
            }
            is PaymentSheetResult.Canceled -> {
                paymentBusy = false
                paymentError = null
            }
            is PaymentSheetResult.Failed -> {
                paymentBusy = false
                val detail = result.error.localizedMessage ?: result.error.message ?: "sense missatge"
                paymentError = "${result.error::class.java.simpleName}: $detail"
                android.util.Log.e("SpecialStripe", "PaymentSheetResult.Failed", result.error)
            }
        }
    }.build()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Serveis", fontSize = 26.sp, fontWeight = FontWeight.Black)
            Text(
                "Entra a un espai, prepara la comanda i paga. Un únic QR pot contindre tots els productes de la comanda.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
        paymentError?.let { message ->
            item {
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.errorContainer) {
                    Text(
                        message,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 12.sp
                    )
                }
            }
        }
        if (purchases.isNotEmpty()) {
            item { Text("Les meues compres", fontSize = 18.sp, fontWeight = FontWeight.Black) }
            items(purchases) { purchase ->
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { qrPurchase = purchase },
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF101216)
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(purchase.serviceName, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(
                                purchase.items.joinToString(" · ") { "${it.quantity}× ${it.productName}" },
                                color = Color.White.copy(alpha = .72f),
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(formatSpecialPrice(purchase.totalPrice), color = Color(0xFFFFD54F), fontWeight = FontWeight.Black)
                        }
                        Text("QR →", color = Color(0xFFFFD54F), fontWeight = FontWeight.Black)
                    }
                }
            }
        }
        item { Text("Espais disponibles", fontSize = 18.sp, fontWeight = FontWeight.Black) }
        items(config.services) { service ->
            val serviceImage = rememberImageBitmapFromUri(service.imageUri)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(152.dp)
                    .clickable { selectedService = service },
                shape = RoundedCornerShape(22.dp),
                color = service.accent
            ) {
                Box(Modifier.fillMaxSize()) {
                    if (serviceImage != null) {
                        Image(
                            bitmap = serviceImage,
                            contentDescription = service.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Box(Modifier.fillMaxSize().background(service.accent.copy(alpha = .72f)))
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Text(service.name, fontWeight = FontWeight.Black, fontSize = 24.sp, color = Color.Black)
                        Text(service.description, fontSize = 12.sp, lineHeight = 16.sp, color = Color.Black.copy(alpha = .82f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(7.dp))
                        Surface(shape = RoundedCornerShape(999.dp), color = Color.White.copy(alpha = .82f)) {
                            Text("Entrar →", modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    selectedService?.let { service ->
        val quantities = remember(service.name) { mutableStateMapOf<String, Int>() }
        val selectedItems = service.products.mapNotNull { product ->
            val qty = quantities[product.name] ?: 0
            if (qty > 0) SpecialServicePurchaseItem(product.name, qty, product.price) else null
        }
        val baseTotal = selectedItems.sumOf { it.lineTotal }
        val baseTotalCents = kotlin.math.round(baseTotal * 100.0).toLong()
        val chargedTotalCents = generalPurchaseChargeCents(baseTotalCents)
        val managementFeeCents = generalPurchaseFeeCents(baseTotalCents)
        val total = chargedTotalCents / 100.0
        AlertDialog(
            onDismissRequest = { selectedService = null },
            title = { Text(service.name, fontWeight = FontWeight.Black) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { Text(service.description, fontSize = 13.sp) }
                    items(service.products) { product ->
                        val qty = quantities[product.name] ?: 0
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f)
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(product.name, fontWeight = FontWeight.Bold)
                                    Text(product.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(formatSpecialPrice(product.price), fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 3.dp))
                                }
                                OutlinedButton(
                                    onClick = { if (qty > 0) quantities[product.name] = qty - 1 },
                                    enabled = qty > 0,
                                    contentPadding = PaddingValues(horizontal = 11.dp)
                                ) { Text("−") }
                                Text("$qty", modifier = Modifier.padding(horizontal = 10.dp), fontWeight = FontWeight.Black, fontSize = 17.sp)
                                OutlinedButton(
                                    onClick = { quantities[product.name] = qty + 1 },
                                    contentPadding = PaddingValues(horizontal = 11.dp)
                                ) { Text("+") }
                            }
                        }
                    }
                    if (selectedItems.isNotEmpty()) {
                        item {
                            Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFF101216)) {
                                Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text("Total amb gestió", color = Color.White, fontWeight = FontWeight.Bold)
                                        Text("Base ${formatSpecialPrice(baseTotal)} · gestió ${formatSpecialPrice(managementFeeCents / 100.0)}", color = Color.White.copy(alpha = .72f), fontSize = 10.sp)
                                    }
                                    Text(formatSpecialPrice(total), color = Color(0xFFFFD54F), fontWeight = FontWeight.Black, fontSize = 20.sp)
                                }
                            }
                        }
                    }
                    paymentError?.let { message ->
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text("Error de Stripe", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onErrorContainer)
                                    Spacer(Modifier.height(4.dp))
                                    Text(message, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onErrorContainer)
                                    Spacer(Modifier.height(4.dp))
                                    Text("Copia este missatge si continua fallant.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = .78f))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = selectedItems.isNotEmpty() && !paymentBusy && !paymentSheetOpen,
                    onClick = {
                        paymentError = null
                        paymentBusy = true
                        val selectedForPayment = service.products.mapNotNull { product ->
                            val qty = quantities[product.name] ?: 0
                            if (qty > 0) Triple(product.id, product.name, qty) else null
                        }
                        state.createSpecialServicePayment(
                            concertId = concert.id,
                            serviceId = service.id,
                            serviceName = service.name,
                            items = selectedForPayment.map { Triple(it.first, it.second, it.third) }
                        ) { start, error ->
                            if (start == null) {
                                paymentBusy = false
                                paymentError = error ?: "No s'ha pogut iniciar Stripe."
                            } else {
                                runCatching {
                                    PaymentConfiguration.init(context, start.publishableKey)
                                    pendingOrderId = start.orderId
                                    paymentSheetOpen = true
                                    paymentSheet.presentWithPaymentIntent(
                                        start.clientSecret,
                                        PaymentSheet.Configuration.Builder(start.merchantName).build()
                                    )
                                }.onFailure { ex ->
                                    paymentSheetOpen = false
                                    paymentBusy = false
                                    paymentError = ex.localizedMessage ?: "No s'ha pogut obrir Stripe."
                                }
                            }
                        }
                    }
                ) {
                    Text(if (paymentBusy) "Connectant amb Stripe…" else "Pagar amb Stripe · ${formatSpecialPrice(total)}")
                }
            },
            dismissButton = { TextButton(onClick = { selectedService = null }) { Text("Tancar") } }
        )
    }

    qrPurchase?.let { purchase ->
        val qr = remember(purchase.qrPayload) { createSpecialServiceQrBitmap(purchase.qrPayload).asImageBitmap() }
        AlertDialog(
            onDismissRequest = { qrPurchase = null },
            title = { Text("QR · ${purchase.serviceName}", fontWeight = FontWeight.Black) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Image(bitmap = qr, contentDescription = "QR de la comanda", modifier = Modifier.size(220.dp))
                    Spacer(Modifier.height(10.dp))
                    purchase.items.forEach { item ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${item.quantity}× ${item.productName}", fontSize = 12.sp)
                            Text(formatSpecialPrice(item.lineTotal), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(7.dp))
                    Text("Total: ${formatSpecialPrice(purchase.totalPrice)}", fontWeight = FontWeight.Black)
                    Text("Mostra este QR al punt de servei. El lector mostrarà tota la comanda abans de confirmar l’entrega.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                    Text(purchase.id, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                }
            },
            confirmButton = { Button(onClick = { qrPurchase = null }) { Text("Fet") } }
        )
    }
}

private fun formatSpecialPrice(price: Double): String =
    if (price % 1.0 == 0.0) "${price.toInt()} €" else String.format(Locale("ca", "ES"), "%.2f €", price)

private fun createSpecialServiceQrBitmap(value: String, size: Int = 512): Bitmap {
    val matrix = MultiFormatWriter().encode(value, BarcodeFormat.QR_CODE, size, size)
    val pixels = IntArray(size * size)
    for (y in 0 until size) {
        val offset = y * size
        for (x in 0 until size) {
            pixels[offset + x] = if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
        }
    }
    return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, size, 0, 0, size, size)
    }
}

@Composable
private fun SpecialCommunityTab(
    state: AgendaState,
    concert: Concert
) {
    val groups = state.groupsForConcert(concert.id)
    var selectedGroup by remember { mutableStateOf<com.example.agendavalenciana.model.CommunityGroup?>(null) }
    var showCreateGroup by remember { mutableStateOf(false) }
    var createType by remember(concert.id) { mutableStateOf(com.example.agendavalenciana.model.CommunityGroupType.CAR) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(shape = RoundedCornerShape(24.dp), color = Color(0xFF1A1D24)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .background(Brush.horizontalGradient(listOf(Color(0xFF2C1A10), Color(0xFF4C2E1B), Color(0xFF101216))))
                ) {
                    Column(modifier = Modifier.align(Alignment.BottomStart).padding(18.dp)) {
                        Text("Comunitat", color = Color.White, fontWeight = FontWeight.Black, fontSize = 34.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Taulons d’anuncis creats des d’este esdeveniment, amb xat opcional i contacte amb el creador.",
                            color = Color.White.copy(alpha = 0.9f),
                            lineHeight = 19.sp
                        )
                    }
                }
            }
        }
        item {
            CommunityBoardSections(
                state = state,
                concertId = concert.id,
                groups = groups,
                onCreate = { selected ->
                    createType = selected
                    showCreateGroup = true
                },
                onOpen = { selectedGroup = it }
            )
        }
    }

    if (showCreateGroup) {
        CreateCommunityGroupDialog(
            state = state,
            concert = concert,
            onDismiss = { showCreateGroup = false },
            onCreated = { showCreateGroup = false; state.refreshCommunityGroups(concert.id) },
            initialType = createType
        )
    }
    selectedGroup?.let { group ->
        CommunityChatDialog(
            state = state,
            groupId = group.id,
            onDismiss = { selectedGroup = null }
        )
    }
}
