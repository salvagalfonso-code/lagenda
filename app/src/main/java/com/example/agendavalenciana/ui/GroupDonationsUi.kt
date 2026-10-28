package com.example.agendavalenciana.ui

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agendavalenciana.data.ServerApiRepository
import com.example.agendavalenciana.state.AgendaState
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun donationMoney(cents: Long): String = String.format(java.util.Locale.forLanguageTag("ca-ES"), "%.2f €", cents / 100.0)

@Composable
internal fun GroupDonationSection(state: AgendaState, artist: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var config by remember(artist) { mutableStateOf<ServerApiRepository.ArtistDonationConfig?>(null) }
    var loading by remember(artist) { mutableStateOf(true) }
    var editorOpen by remember { mutableStateOf(false) }
    var donateOpen by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val canManage = state.canManageArtist(artist)

    fun refresh() {
        loading = true
        scope.launch {
            val r = withContext(Dispatchers.IO) { ServerApiRepository.fetchArtistDonationConfig(context, artist) }
            loading = false
            r.onSuccess { config = it }.onFailure { message = it.message ?: "No s'han pogut carregar les donacions." }
        }
    }
    LaunchedEffect(artist) { refresh() }
    if (loading) return
    val c = config ?: return
    if (!c.active && !canManage) return

    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF111318)
    ) {
        Box {
            val image = rememberImageBitmapFromUri(c.imageUrl)
            if (image != null) {
                Image(bitmap = image, contentDescription = c.title, modifier = Modifier.fillMaxWidth().height(190.dp), contentScale = ContentScale.Crop)
                Box(Modifier.fillMaxWidth().height(190.dp).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha=.12f), Color.Black.copy(alpha=.86f)))))
            }
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(c.title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 21.sp)
                        if (c.description.isNotBlank()) Text(c.description, color = Color.White.copy(alpha=.82f), fontSize = 13.sp, modifier = Modifier.padding(top=5.dp))
                    }
                    if (canManage) OutlinedButton(onClick = { editorOpen = true }) { Text("Configurar") }
                }
                Spacer(Modifier.height(14.dp))
                if (c.active) Button(onClick = { donateOpen = true }, modifier = Modifier.fillMaxWidth()) { Text("Fer una donació") }
                else Text("Donacions desactivades", color = Color.White.copy(alpha=.65f), fontSize=12.sp)
            }
        }
    }
    message?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top=6.dp)) }
    if (editorOpen) DonationConfigDialog(artist, c, onDismiss={editorOpen=false}, onSaved={ message=it; editorOpen=false; refresh() })
    if (donateOpen) DonationCheckoutDialog(artist, c, onDismiss={donateOpen=false}, onPaid={ message=it; donateOpen=false })
}

@Composable
private fun DonationConfigDialog(artist: String, current: ServerApiRepository.ArtistDonationConfig, onDismiss:()->Unit, onSaved:(String)->Unit) {
    val context=LocalContext.current; val scope=rememberCoroutineScope()
    var active by remember { mutableStateOf(current.active) }; var title by remember { mutableStateOf(current.title) }; var desc by remember { mutableStateOf(current.description) }
    var image by remember { mutableStateOf(current.imageUrl.orEmpty()) }; var amounts by remember { mutableStateOf(current.suggestedAmountsCents.joinToString(",") { (it/100.0).toString() }) }
    var busy by remember { mutableStateOf(false) }; var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest={if(!busy)onDismiss()}, title={Text("Donacions · $artist",fontWeight=FontWeight.Black)}, text={
        LazyColumn(verticalArrangement=Arrangement.spacedBy(9.dp)) {
            item { Row(verticalAlignment=Alignment.CenterVertically){ Text("Activar donacions",Modifier.weight(1f)); Switch(active,onCheckedChange={active=it}) } }
            item { OutlinedTextField(title,{title=it},label={Text("Títol")},modifier=Modifier.fillMaxWidth()) }
            item { OutlinedTextField(desc,{desc=it},label={Text("Text explicatiu")},modifier=Modifier.fillMaxWidth(),minLines=3) }
            item { OutlinedTextField(image,{image=it},label={Text("URL de la imatge (opcional)")},modifier=Modifier.fillMaxWidth()) }
            item { OutlinedTextField(amounts,{amounts=it},label={Text("Imports suggerits € (ex: 3,5,10,20)")},modifier=Modifier.fillMaxWidth()) }
            error?.let { item { Text(it,color=MaterialTheme.colorScheme.error,fontSize=12.sp) } }
        }
    }, confirmButton={ Button(enabled=!busy&&title.isNotBlank(),onClick={
        val cents=amounts.split(',', ';').mapNotNull { it.trim().replace(',','.').toDoubleOrNull()?.let { n -> (n*100).toLong() } }.filter{it>=100}
        busy=true; scope.launch { val r=withContext(Dispatchers.IO){ ServerApiRepository.saveArtistDonationConfig(context,artist,ServerApiRepository.ArtistDonationConfig(active,title.trim(),desc.trim(),image.trim().ifBlank{null},if(cents.isEmpty()) listOf(500,1000,2000) else cents,100,100000))}; busy=false; r.onSuccess{onSaved(if(active)"Donacions activades." else "Donacions desactivades.")}.onFailure{error=it.message?:"No s'ha pogut guardar."} }
    }) { Text(if(busy)"Guardant…" else "Guardar") } }, dismissButton={TextButton(enabled=!busy,onClick=onDismiss){Text("Tancar")}})
}

@Composable
private fun DonationCheckoutDialog(artist: String, config: ServerApiRepository.ArtistDonationConfig, onDismiss:()->Unit, onPaid:(String)->Unit) {
    val context=LocalContext.current; val scope=rememberCoroutineScope()
    var selected by remember { mutableStateOf(config.suggestedAmountsCents.firstOrNull() ?: 500L) }; var custom by remember { mutableStateOf("") }
    var donorName by remember { mutableStateOf("") }; var donorMessage by remember { mutableStateOf("") }; var anonymous by remember { mutableStateOf(false) }; var pendingId by remember { mutableStateOf<Long?>(null) }
    var busy by remember { mutableStateOf(false) }; var error by remember { mutableStateOf<String?>(null) }
    val builder=remember { PaymentSheet.Builder { result -> when(result){
        is PaymentSheetResult.Completed -> { val id=pendingId; if(id==null){busy=false;error="Falta l'identificador de la donació."} else scope.launch{ error="Pagament fet. Verificant…"; val r=withContext(Dispatchers.IO){ServerApiRepository.fetchArtistDonationStatus(context,id)}; busy=false; r.onSuccess{if(it.status=="paid")onPaid("Donació confirmada · ${donationMoney(it.amountCents)}") else error="El pagament encara no està confirmat (${it.status})."}.onFailure{error=it.message?:"No s'ha pogut verificar."} } }
        is PaymentSheetResult.Canceled -> {busy=false;error=null}
        is PaymentSheetResult.Failed -> {busy=false;error=result.error.localizedMessage?:"Stripe ha fallat.";Log.e("ArtistDonationStripe","Payment failed",result.error)}
    } } }
    val sheet=builder.build()
    val customCents=custom.replace(',','.').toDoubleOrNull()?.let{(it*100).toLong()}; val amount=customCents?:selected
    AlertDialog(onDismissRequest={if(!busy)onDismiss()},title={Text("Donació a $artist",fontWeight=FontWeight.Black)},text={LazyColumn(verticalArrangement=Arrangement.spacedBy(9.dp)){
        item { Text("Tria l'import",fontWeight=FontWeight.Bold) }
        item { Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){ config.suggestedAmountsCents.take(4).forEach { a -> FilterChip(selected=custom.isBlank()&&selected==a,onClick={custom="";selected=a},label={Text(donationMoney(a))}) } } }
        item { OutlinedTextField(custom,{custom=it},label={Text("Altre import €")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),modifier=Modifier.fillMaxWidth()) }
        item { Row(verticalAlignment=Alignment.CenterVertically){ Text("Donació anònima",Modifier.weight(1f),fontWeight=FontWeight.SemiBold); Switch(checked=anonymous,onCheckedChange={anonymous=it;if(it)donorName=""}) } }
        item { if(!anonymous) OutlinedTextField(donorName,{donorName=it},label={Text("Nom (opcional)")},modifier=Modifier.fillMaxWidth()) else Text("El grup veurà esta donació com a Anònim.",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant) }
        item { OutlinedTextField(donorMessage,{donorMessage=it.take(500)},label={Text("Missatge al grup (opcional)")},modifier=Modifier.fillMaxWidth(),minLines=2) }
        item { Text("Total: ${donationMoney(amount)}",fontWeight=FontWeight.Black,fontSize=18.sp) }
        error?.let { item { Text(it,color=MaterialTheme.colorScheme.error,fontSize=12.sp) } }
    }},confirmButton={Button(enabled=!busy&&amount in config.minAmountCents..config.maxAmountCents,onClick={busy=true;error=null;scope.launch{val r=withContext(Dispatchers.IO){ServerApiRepository.createArtistDonationPayment(context,artist,amount,donorName.trim(),donorMessage.trim(),anonymous)};r.onFailure{busy=false;error=it.message?:"No s'ha pogut iniciar Stripe."};r.onSuccess{start->pendingId=start.donationId;try{PaymentConfiguration.init(context,start.publishableKey);sheet.presentWithPaymentIntent(start.clientSecret,PaymentSheet.Configuration.Builder(start.merchantName.ifBlank{"l'Agenda"}).build())}catch(t:Throwable){busy=false;error=t.localizedMessage?:"No s'ha pogut obrir Stripe.";Log.e("ArtistDonationStripe","Cannot present",t)}}}}){Text(if(busy)"Connectant…" else "Donar ${donationMoney(amount)}")}},dismissButton={TextButton(enabled=!busy,onClick=onDismiss){Text("Tancar")}})
}
