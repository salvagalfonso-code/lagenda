package com.example.agendavalenciana.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agendavalenciana.data.ServerApiRepository
import com.example.agendavalenciana.state.AgendaState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * V320: la venda sempre comença DESPRÉS de publicar. No s'exigix sessió en
 * entrar a L'Agenda; esta pantalla només s'obri després de prémer vendre.
 * Els camps fiscals i les acceptacions es guarden únicament al servidor.
 */
@Composable
internal fun SellerBillingOnboardingScreen(
    state: AgendaState,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onActivated: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val eventId = state.pendingInternalSaleEventId
    var status by remember { mutableStateOf<JSONObject?>(null) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var kind by remember { mutableStateOf("PERSON") }
    var name by remember { mutableStateOf("") }
    var taxId by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var postalCode by remember { mutableStateOf("") }
    var town by remember { mutableStateOf("") }
    var privacyRead by remember { mutableStateOf(false) }
    var salesRead by remember { mutableStateOf(false) }
    var privacyAccepted by remember { mutableStateOf(false) }
    var salesAccepted by remember { mutableStateOf(false) }
    val companies = status?.optJSONArray("companies")
    val existingCompany = companies?.optJSONObject(0)
    val hasBilling = status?.optBoolean("has_billing",false) == true && existingCompany != null
    val noticesCurrent = existingCompany?.optBoolean("notices_current",false) == true
    val privacyUrl = status?.optString("privacy_url", "").orEmpty()
    val salesUrl = status?.optString("sales_terms_url", "").orEmpty()
    val policiesReady = status?.optBoolean("policies_ready",false) == true

    LaunchedEffect(state.currentAccount?.serverUserId) {
        loading = true
        error = null
        val result = withContext(Dispatchers.IO) {
            ServerApiRepository.getAuthenticatedJson(context.applicationContext,"seller-onboarding/index.php")
        }
        result.onSuccess { status = it }.onFailure { error = it.message ?: "No s'han pogut consultar les dades de facturació." }
        loading = false
    }

    fun completeRegistration(companyId: Int?) {
        if (saving) return
        if (eventId <= 0) { error = "No hi ha un esdeveniment vinculat. Torna a l'edició i prem Iniciar venda."; return }
        if (!policiesReady) { error = "Falten les polítiques legals definitives al servidor. L'esdeveniment continua publicat, però la venda no es pot activar."; return }
        if (!privacyAccepted || !salesAccepted || !privacyRead || !salesRead) {
            error = "Cal llegir la política de privacitat i acceptar les condicions de venda.";return
        }
        if (companyId == null && (name.isBlank() || taxId.isBlank() || address.isBlank() || postalCode.length != 5 || town.isBlank())) {
            error = "Completa el nom fiscal, DNI/NIE/CIF, adreça, codi postal i població."; return
        }
        saving = true; error = null
        scope.launch {
            val payload = JSONObject()
                .put("event_id",eventId)
                .put("accept_privacy_notice",privacyAccepted)
                .put("accept_sales_terms",salesAccepted)
            if (companyId != null) payload.put("company_id",companyId)
            else payload.put("kind",kind).put("display_name",name.trim())
                .put("tax_id",taxId.trim().uppercase()).put("address",address.trim())
                .put("postal_code",postalCode.trim()).put("town",town.trim()).put("province","")
            val result = withContext(Dispatchers.IO) {
                ServerApiRepository.postAuthenticatedJson(context.applicationContext,"seller-onboarding/index.php",payload)
            }
            saving = false
            result.onSuccess { onActivated() }
                .onFailure { error = it.message ?: "No s'ha pogut guardar la facturació." }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        item { TextButton(onClick = onBack) { Text("‹ Tornar") } }
        item { Text("Activar venda d'entrades", fontSize = 24.sp, fontWeight = FontWeight.Bold) }
        item { Text("L'esdeveniment ja està publicat. Per a vendre entrades des de L'Agenda cal identificar el titular fiscal i acceptar les condicions. No cal omplir de nou les dades d'accés si ja tens un compte.", fontSize = 13.sp) }
        if (loading) item { CircularProgressIndicator() }
        if (!loading && hasBilling) item {
            Text("Dades de facturació ja registrades: ${existingCompany?.optString("display_name").orEmpty()}", fontWeight = FontWeight.SemiBold)
        }
        if (!loading && !hasBilling) {
            item { Text("Dades de facturació", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = kind=="PERSON", onClick={kind="PERSON"}, label={Text("Persona / autònom")})
                FilterChip(selected = kind=="COMPANY", onClick={kind="COMPANY"}, label={Text("Empresa")})
            } }
            item { OutlinedTextField(name,{name=it},modifier=Modifier.fillMaxWidth(),singleLine=true,label={Text(if(kind=="PERSON") "Nom i cognoms" else "Nom de l'empresa")}) }
            item { OutlinedTextField(taxId,{taxId=it.uppercase().filter { c->c.isLetterOrDigit() || c=='-' }.take(24)},modifier=Modifier.fillMaxWidth(),singleLine=true,label={Text("DNI / NIE / CIF")}) }
            item { OutlinedTextField(address,{address=it},modifier=Modifier.fillMaxWidth(),singleLine=true,label={Text("Adreça fiscal")}) }
            item { OutlinedTextField(postalCode,{postalCode=it.filter(Char::isDigit).take(5)},modifier=Modifier.fillMaxWidth(),singleLine=true,label={Text("Codi postal")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number)) }
            item { OutlinedTextField(town,{town=it},modifier=Modifier.fillMaxWidth(),singleLine=true,label={Text("Població")}) }
        }
        if (!loading && policiesReady && !noticesCurrent || !loading && policiesReady && !hasBilling) {
            item { Text("Documents i acceptacions",fontSize=18.sp,fontWeight=FontWeight.SemiBold) }
            item {
                TextButton(onClick={ if(privacyUrl.startsWith("https://")) { uriHandler.openUri(privacyUrl); privacyRead=true } }) {
                    Text("Llegir la política de privacitat")
                }
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Checkbox(checked=privacyAccepted,onCheckedChange={privacyAccepted=it},enabled=privacyRead)
                    Text("He llegit la política de privacitat",fontSize=13.sp)
                }
            }
            item {
                TextButton(onClick={ if(salesUrl.startsWith("https://")) { uriHandler.openUri(salesUrl); salesRead=true } }) {
                    Text("Llegir les condicions de venda d'entrades")
                }
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Checkbox(checked=salesAccepted,onCheckedChange={salesAccepted=it},enabled=salesRead)
                    Text("Accepte les condicions de venda d'entrades de L'Agenda",fontSize=13.sp)
                }
            }
        }
        if (!loading && !policiesReady) item {
            Text("Les condicions legals definitives encara no estan configurades al servidor. Pots continuar utilitzant L'Agenda, però la venda no s'activarà fins que estiguen publicades.",color=MaterialTheme.colorScheme.error,fontSize=13.sp)
        }
        error?.let { message-> item { Text(message,color=MaterialTheme.colorScheme.error,fontSize=13.sp) } }
        if (!loading) item {
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !saving && policiesReady &&
                    (noticesCurrent || (privacyAccepted && salesAccepted && privacyRead && salesRead)),
                onClick = {
                    if (hasBilling && noticesCurrent) {
                        // Les acceptacions ja consten en el servidor en la versió vigent.
                        privacyRead = true; salesRead = true; privacyAccepted = true; salesAccepted = true
                    }
                    completeRegistration(if(hasBilling) existingCompany?.optInt("id") else null)
                }
            ) { Text(if (saving) "Guardant…" else "Continuar i configurar entrades") }
        }
        item { TextButton(onClick=onBack,modifier=Modifier.fillMaxWidth()) { Text("Ara no") } }
    }
}
