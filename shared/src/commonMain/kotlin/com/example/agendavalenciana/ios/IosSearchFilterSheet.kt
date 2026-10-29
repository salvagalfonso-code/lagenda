package com.example.agendavalenciana.ios

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val IosFilterOrange = Color(0xFFF0B31F)
private val IosFilterBlack = Color(0xFF101010)
private val IosFilterOutline = Color(0xFFF0F0F0)

private val androidCategoryLabels = listOf(
    "Música", "Teatre", "Cinema", "Xarrades", "Dansa", "Exposicions", "Literatura",
    "Tallers", "Infantil", "Cultura popular", "Patrimoni", "Altres"
)

/** Android-style chips: the entire choice remains accessible by horizontal scrolling. */
@Composable
private fun IosChoiceChip(label: String, selected: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(9.dp),
        color = if (selected) Color(0xFFE6E6E6) else Color.White,
        border = BorderStroke(1.dp, if (selected) Color(0xFF858585) else IosFilterOutline)
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 13.sp, color = if (enabled) IosFilterBlack else Color.LightGray,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun IosChoiceStrip(
    title: String? = null, options: List<String>, selected: Set<String>,
    onToggle: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        if (title != null) Text(title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = IosFilterBlack)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            options.forEach { option ->
                IosChoiceChip(option, option in selected, onClick = { onToggle(option) })
            }
        }
    }
}

@Composable
private fun IosFilterSection(
    title: String, icon: String, expanded: Boolean, onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(13.dp), color = Color.White,
        border = BorderStroke(1.dp, IosFilterOutline)) {
        Column {
            Row(Modifier.fillMaxWidth().clickable(onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically) {
                AgendaGlyph(icon, Modifier.size(24.dp), IosFilterOrange)
                Spacer(Modifier.width(10.dp))
                Text(title, modifier = Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(if (expanded) "⌃" else "⌄", fontSize = 20.sp, color = Color.DarkGray)
            }
            if (expanded) {
                HorizontalDivider(color = IosFilterOutline)
                Column(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 15.dp),
                    verticalArrangement = Arrangement.spacedBy(11.dp), content = content)
            }
        }
    }
}

/** All changes stay in a draft until Apply; dismissal restores the previous filters. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IosSearchFilterSheet(
    initial: IosSearchFilters,
    today: String,
    categories: List<String>,
    availablePresets: List<IosSavedSearch>,
    isSignedIn: Boolean,
    onDismiss: () -> Unit,
    onApply: (IosSearchFilters) -> Unit,
    onLoadPreset: (IosSavedSearch) -> Unit,
    onSavedPreset: (String, IosSearchFilters) -> Unit,
    onClear: () -> Unit,
    onSave: suspend (String, IosSearchFilters) -> Unit,
    onDeletePreset: suspend (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var draft by remember(initial) { mutableStateOf(initial) }
    var expanded by remember { mutableStateOf(emptySet<String>()) }
    var locationQuery by remember { mutableStateOf("") }
    var pickDate by remember { mutableStateOf<String?>(null) }
    var saveDialog by remember { mutableStateOf(false) }
    var saveName by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<String?>(null) }
    var saveError by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val allComarques = remember { IosGeoPlaces.allComarques() }
    val allTowns = remember { IosGeoPlaces.allTowns() }
    val monthChoices = remember(today) { IosFilterDates.monthOptions(today) }
    val knownCategories = remember(categories) {
        (androidCategoryLabels + categories).filter(String::isNotBlank)
            .distinctBy(IosAdvancedSearch::norm)
    }
    fun section(name: String) {
        expanded = if (name in expanded) expanded - name else expanded + name
    }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        containerColor = Color.White
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.90f).imePadding().navigationBarsPadding()) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Filtres", Modifier.weight(1f), fontSize = 24.sp, fontWeight = FontWeight.Black,
                        color = IosFilterBlack)
                    TextButton(onClick = onClear) {
                        Text("Netejar", fontSize = 15.sp, color = IosFilterBlack)
                    }
                }
                IosFilterSection("Quan", "calendar", "when" in expanded, { section("when") }) {
                    val tomorrow = IosFilterDates.addDays(today, 1)
                    val saturday = IosFilterDates.weekendSaturday(today)
                    val plus6 = IosFilterDates.addDays(today, 6)
                    val currentMonth = today.take(7)
                    val thisMonth = IosFilterDates.monthRange(currentMonth)
                    val shortcutDates = listOf(
                        "Hui" to (today to today), "Demà" to (tomorrow to tomorrow),
                        "Cap de setmana" to (saturday to IosFilterDates.addDays(saturday,1)),
                        "7 dies" to (today to plus6), "Este mes" to thisMonth
                    )
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        shortcutDates.forEach { (label, dates) ->
                            IosChoiceChip(label,
                                draft.fromDate == dates.first && draft.toDate == dates.second && draft.month.isBlank(),
                                onClick = { draft = draft.copy(fromDate = dates.first, toDate = dates.second, month = "") })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        listOf("from" to "Des de", "to" to "Fins a").forEach { (field, label) ->
                            Surface(Modifier.weight(1f).clickable { pickDate = field },
                                shape = RoundedCornerShape(10.dp), color = Color.White,
                                border = BorderStroke(1.dp, IosFilterOutline)) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(label, fontSize = 11.sp, color = Color.Gray)
                                    Spacer(Modifier.height(6.dp))
                                    Text(draft.run { if (field == "from") fromDate else toDate }
                                            .takeIf { it.isNotBlank() }?.let(IosUnfilteredFeedPolicy::fullDate)
                                            ?: "Tria data", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                    IosChoiceStrip("Mes", listOf("Tots") + monthChoices.map { it.first },
                        selected = if (draft.month.isBlank() && draft.fromDate.isBlank() && draft.toDate.isBlank()) setOf("Tots")
                            else monthChoices.filter { it.second == draft.month }.map { it.first }.toSet(),
                        onToggle = { choice ->
                            draft = draft.copy(month = monthChoices.firstOrNull { it.first == choice }?.second.orEmpty(),
                                fromDate = "", toDate = "")
                        })
                }
                IosFilterSection("On?", "pin", "where" in expanded, { section("where") }) {
                    Text("Busca per poble o comarca", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    if (draft.town.isNotBlank() || draft.comarca.isNotBlank()) {
                        Row(Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (draft.town.isNotBlank()) IosChoiceChip("Poble: ${draft.town} ×", true,
                                onClick = { draft = draft.copy(town = "", radiusKm = 0) })
                            if (draft.comarca.isNotBlank()) IosChoiceChip("Comarca: ${draft.comarca} ×", true,
                                onClick = { draft = draft.copy(comarca = "") })
                        }
                    }
                    OutlinedTextField(locationQuery, { locationQuery = it.take(120) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        placeholder = { Text("Escriu un poble o comarca", fontSize = 13.sp) },
                        leadingIcon = { AgendaGlyph("search", Modifier.size(23.dp), Color.DarkGray) })
                    if (locationQuery.isNotBlank()) {
                        val q = IosAdvancedSearch.norm(locationQuery)
                        val towns = allTowns.asSequence().filter { IosAdvancedSearch.norm(it).contains(q) }.take(8).toList()
                        val comarques = allComarques.asSequence().filter { IosAdvancedSearch.norm(it).contains(q) }.take(8).toList()
                        if (towns.isNotEmpty()) {
                            Text("Pobles", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                towns.forEach { town -> IosChoiceChip(town, draft.town == town, onClick = {
                                    draft = draft.copy(town = town, comarca = ""); locationQuery = ""
                                }) }
                            }
                        }
                        if (comarques.isNotEmpty()) {
                            Text("Comarques", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                comarques.forEach { name -> IosChoiceChip(name, draft.comarca == name, onClick = {
                                    draft = draft.copy(comarca = name, town = "", radiusKm = 0); locationQuery = ""
                                }) }
                            }
                        }
                        if (towns.isEmpty() && comarques.isEmpty()) Text(
                            "No hem trobat cap poble ni comarca amb este nom.", fontSize = 12.sp, color = Color.Gray)
                    }
                    IosChoiceStrip("Comarca (totes)", listOf("Totes") + allComarques,
                        selected = setOf(draft.comarca.ifBlank { "Totes" }), onToggle = { name ->
                            draft = draft.copy(comarca = name.takeUnless { it == "Totes" }.orEmpty(),
                                town = if (name != "Totes") "" else draft.town,
                                radiusKm = if (name != "Totes") 0 else draft.radiusKm)
                        })
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Distància", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            listOf(0,10,25,50,100).forEach { km ->
                                IosChoiceChip(if (km == 0) "Qualsevol" else "≤ $km km",
                                    draft.radiusKm == km, enabled = km == 0 || draft.town.isNotBlank() || draft.referenceTowns.isNotEmpty(),
                                    onClick = { draft = draft.copy(radiusKm = km) })
                            }
                        }
                        if (draft.town.isBlank() && draft.referenceTowns.isEmpty()) Text("Tria un poble o configura les ubicacions preferides per a calcular la distància.",
                            fontSize = 11.sp, color = Color.Gray)
                    }
                }
                IosFilterSection("Què", "sliders", "what" in expanded, { section("what") }) {
                    val selected = draft.categories.ifEmpty { listOfNotNull(draft.category.takeIf(String::isNotBlank)).toSet() }
                    IosChoiceStrip("Disciplines", knownCategories, selected, onToggle = { category ->
                        val next = if (category in selected) selected - category else selected + category
                        draft = draft.copy(categories = next, category = "")
                    })
                    if (draft.style.isNotBlank()) Text("Estil: ${draft.style} ×", Modifier.clickable {
                        draft = draft.copy(style = "")
                    }, fontSize = 11.sp)
                }
                IosFilterSection("Preu", "ticket", "price" in expanded, { section("price") }) {
                    IosChoiceStrip("Entrada", listOf("Qualsevol", "Gratuït", "≤ 10 €", "≤ 15 €", "≤ 20 €"),
                        selected = setOf(when {
                            draft.freeOnly -> "Gratuït"
                            draft.maxPrice != null -> "≤ ${draft.maxPrice} €"
                            else -> "Qualsevol"
                        }), onToggle = { item ->
                            draft = when(item) {
                                "Gratuït" -> draft.copy(freeOnly = true, maxPrice = null)
                                "≤ 10 €" -> draft.copy(freeOnly = false, maxPrice = 10)
                                "≤ 15 €" -> draft.copy(freeOnly = false, maxPrice = 15)
                                "≤ 20 €" -> draft.copy(freeOnly = false, maxPrice = 20)
                                else -> draft.copy(freeOnly = false, maxPrice = null)
                            }
                        })
                }
                if (isSignedIn && availablePresets.isNotEmpty()) {
                    Text("Filtres guardats", Modifier.padding(top = 11.dp), fontWeight = FontWeight.Bold)
                    availablePresets.forEach { preset ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = {
                                val restored = preset.filters.copy(referenceTowns = draft.referenceTowns)
                                if (restored.valid()) onLoadPreset(preset.copy(filters = restored))
                                else {
                                    draft = restored
                                    expanded = expanded + "where"
                                    saveError = "Esta cerca necessita un poble d'origen o ubicacions preferides per al radi."
                                }
                            }, modifier = Modifier.weight(1f)) {
                                Text(preset.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            TextButton(enabled = deleting == null, onClick = {
                                deleting = preset.name
                                scope.launch {
                                    runCatching { onDeletePreset(preset.name) }.onFailure { saveError = it.message.orEmpty() }
                                    deleting = null
                                }
                            }) { Text("×") }
                        }
                    }
                }
                if (saveError.isNotBlank()) Text(saveError, color = Color.Red, fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
            }
            // Pinned footer: never scrolls away when all four groups are expanded.
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { if (draft.isActive) onApply(draft) else onClear() },
                    enabled = draft.valid(), modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IosFilterBlack),
                    shape = RoundedCornerShape(10.dp)) { Text("Aplicar", fontWeight = FontWeight.Bold) }
                OutlinedButton(onClick = { saveError = ""; saveDialog = true },
                    enabled = draft.isActive && draft.valid(), modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(10.dp)) { Text("♧  Guardar", fontWeight = FontWeight.SemiBold) }
            }
        }
    }
    pickDate?.let { field ->
        val original = (if (field == "from") draft.fromDate else draft.toDate).ifBlank { today }
        val calendar = rememberDatePickerState(initialSelectedDateMillis = IosFilterDates.toUtcMillis(original))
        DatePickerDialog(onDismissRequest = { pickDate = null }, confirmButton = {
            TextButton(onClick = {
                calendar.selectedDateMillis?.let { ms ->
                    val date = IosFilterDates.fromUtcMillis(ms)
                    draft = if (field == "from") draft.copy(fromDate = date,
                        toDate = draft.toDate.takeIf { it.isNotBlank() && it >= date }.orEmpty(), month = "")
                    else draft.copy(toDate = date,
                        fromDate = draft.fromDate.takeIf { it.isNotBlank() && it <= date }.orEmpty(), month = "")
                }
                pickDate = null
            }) { Text("Acceptar") }
        }, dismissButton = { TextButton(onClick = { pickDate = null }) { Text("Cancel·lar") } }) {
            DatePicker(state = calendar)
        }
    }
    if (saveDialog) {
        AlertDialog(onDismissRequest = { if (!saving) saveDialog = false },
            title = { Text("Guardar filtres") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    OutlinedTextField(saveName, { saveName = it.take(52) },
                        label = { Text("Nom de la llista") }, singleLine = true)
                    if (!isSignedIn) Text("Inicia sessió per guardar els filtres en la base de dades.",
                        color = Color.Red, fontSize = 12.sp)
                    if (saveError.isNotBlank()) Text(saveError, color = Color.Red, fontSize = 12.sp)
                    if (availablePresets.any { it.name.equals(saveName.trim(), true) })
                        Text("Ja existeix: se substituirà la configuració guardada.", fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(enabled = !saving && isSignedIn && saveName.trim().isNotBlank() && draft.valid(),
                    onClick = {
                        val name = saveName.trim(); val chosen = draft
                        saving = true; saveError = ""
                        scope.launch {
                            runCatching { onSave(name, chosen) }.onSuccess {
                                saveDialog = false
                                onSavedPreset(name, chosen)
                            }.onFailure { saveError = it.message ?: "No s'ha pogut guardar el filtre al servidor." }
                            saving = false
                        }
                    }) { Text(if (saving) "Guardant..." else "Guardar") }
            }, dismissButton = {
                TextButton(enabled = !saving, onClick = { saveDialog = false }) { Text("Cancel·lar") }
            })
    }
}
