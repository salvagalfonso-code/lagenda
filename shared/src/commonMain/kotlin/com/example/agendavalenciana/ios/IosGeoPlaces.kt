package com.example.agendavalenciana.ios

import kotlin.math.*

/** Font: agenda_territori_v277.json (6016 localitats). Geografia comuna Android/iOS.
 * No descarrega dades de xarxa ni depén del GPS; l'usuari selecciona els llocs.
 */
data class IosGeoPlace(val name: String, val comarca: String, val territory: String,
                       val latitude: Double, val longitude: Double, val municipality: Boolean)

object IosGeoPlaces {
    private fun norm(raw: String): String = IosAdvancedSearch.norm(raw)
        .replace('à','a').replace('á','a').replace('â','a').replace('ä','a')
        .replace('è','e').replace('é','e').replace('ê','e').replace('ë','e')
        .replace('í','i').replace('ï','i').replace('ì','i')
        .replace('ò','o').replace('ó','o').replace('ô','o').replace('ö','o')
        .replace('ú','u').replace('ü','u').replace('ù','u')
        .replace('ç','c').replace('ñ','n')
        .replace(Regex("[^a-z0-9]+"), " ").trim().replace(Regex(" +"), " ")
    fun key(name: String): String = norm(name)

    private val rows: List<String> = listOf(
        """Abdet|Abdet||Marina Baixa|PV|38.695482|-0.257733|0
Abella|Abella||Ripollès|CAT|42.319358|2.314505|0
Abella|Abella||la Ribagorça|FR|42.454299|0.564013|0
Abella de la Conca|Abella de la Conca||Pallars Jussà|CAT|42.175743|1.118004|1
Abrera|Abrera||Baix Llobregat|CAT|41.516387|1.909977|1
Academia General Básica de Suboficiales|Academia General Básica de Suboficiales||Pallars Jussà|CAT|42.201108|0.873769|0
Acadèmia General Bàsica de Suboficials|Acadèmia General Bàsica de Suboficials||Pallars Jussà|CAT|42.202339|0.873722|0
Ademús|Ademuz||Racó d'Ademús|PV|40.069557|-1.229075|1
Adons|Adons||Alta Ribagorça|CAT|42.332991|0.828947|0
Ador|Ador||Safor|PV|38.923684|-0.241736|1
Adrall|Adrall||Alt Urgell|CAT|42.326701|1.400819|0
Adraén|Adraén||Alt Urgell|CAT|42.274999|1.499402|0
Adventistas|Adventistas||Camp de Morvedre|PV|39.704696|-0.28326|0
Aeròdrom Militar|Aeròdrom Militar||Mallorca|IB|39.908962|3.104975|0
Agafallops|Agafallops||Ripollès|CAT|42.188505|2.196335|0
Age|Age||Cerdanya|CAT|42.418586|1.947099|0
Agell|Agell||Maresme|CAT|41.532928|2.405124|0
Agost|Agost||Alacantí|PV|38.442319|-0.641651|1
Agramunt|Agramunt||Urgell|CAT|41.787506|1.068319|1
Agres|Agres||Comtat|PV|38.786915|-0.511931|1
Aguamarina-Sector A1|Aguamarina-Sector A1||Baix Segura|PV|37.912132|-0.736907|0
Aguilar|Aguilar||Alt Urgell|CAT|42.017445|1.278755|0
Aguilar de Segarra|Aguilar de Segarra||Bages|CAT|41.729657|1.61796|1
Aguiló|Aguiló||Conca de Barberà|CAT|41.552912|1.418462|0
Aguiró|Aguiró||Pallars Jussà|CAT|42.398727|0.943579|0
Agullana|Agullana||Alt Empordà|CAT|42.405805|2.840759|1
Agullent|Agullent||Vall d'Albaida|PV|38.834825|-0.558088|1
Agulló|Agulló||Noguera|CAT|42.006083|0.71997|0
Ahillas|Ahillas||Serrans|PV|39.819979|-0.976025|0
Aiacor|Aiacor||Costera|PV|38.97222|-0.574657|0
Aidí|Aidí||Pallars Sobirà|CAT|42.521903|1.197793|0
Aielo de Malferit|Aielo de Malferit||Vall d'Albaida|PV|38.884094|-0.606801|1
Aielo de Rugat|Aielo de Rugat||Vall d'Albaida|PV|38.878564|-0.342522|1
Aigua Xelida|Aigua Xelida||Baix Empordà|CAT|41.92342|3.209932|0
Aiguabella|Aiguabella||Pallars Jussà|CAT|42.442679|0.991997|0
Aiguablava|Aiguablava||Baix Empordà|CAT|41.935014|3.213699|0
Aiguablava|Aiguablava||Baix Empordà|CAT|41.964199|3.22627|0
Aiguafreda|Aiguafreda||Osona|CAT|41.781343|2.264489|1
Aiguamúrcia|Aiguamúrcia||Alt Camp|CAT|41.362888|1.424043|1
Aiguatèbia i Talau|Ayguatébia-Talau||el Conflent|CN|42.552759|2.18328|1
Aiguaviva|Aiguaviva||Gironès|CAT|41.928247|2.77215|1
Aiguaviva|Aiguaviva||Maresme|CAT|41.594849|2.547935|0
Aiguaviva|Aiguaviva||Baix Penedès|CAT|41.351906|1.502901|0
Aiguaviva de Bergantes|Aguaviva||el Matarranya|FR|40.817263|-0.202734|1
Aiguaviva Parc|Aiguaviva Parc||Selva|CAT|41.753694|2.810227|0
Aigües|Aigües||Alacantí|PV|38.510067|-0.363329|1
Aigües Bones|Aigües Bones||Selva|CAT|41.834531|2.821108|0
Ainet de Besan|Ainet de Besan||Pallars Sobirà|CAT|42.537834|1.296863|0
Ainet de Cardós|Ainet de Cardós||Pallars Sobirà|CAT|42.582623|1.235912|0
Aineto|Aineto||Pallars Sobirà|CAT|42.637867|1.249227|0
Aiora|Ayora||Vall de Cofrents|PV|39.01886|-1.074264|1
Airesol A-B|Airesol A-B||Vallès Occidental|CAT|41.627801|2.071725|0
Airesol C|Airesol C||Vallès Occidental|CAT|41.619143|2.081902|0
Airesol D|Airesol D||Vallès Occidental|CAT|41.620698|2.075099|0
Aitona|Aitona||Segrià|CAT|41.47491|0.47175|1
Aixirivall|Aixirivall|||AD|42.46372|1.49129|0
Aixovall|Aixovall|||AD|42.46372|1.49129|0
Aiòder|Ayódar||Alt Millars|PV|39.992037|-0.384962|1
Alacant|Alacant / Alicante||Alacantí|PV|38.382222|-0.537562|1
Alaior|Alaior||Menorca|IB|39.91863|4.135398|1
Alameda del Mar D-1|Alameda del Mar D-1||Baix Segura|PV|37.938494|-0.721998|0
Alaquàs|Alaquàs||Horta Sud|PV|39.451499|-0.470769|1
Alaró|Alaró||Mallorca|IB|39.723542|2.802406|1
Alba de Liste|Alba de Liste||Selva|CAT|41.721681|2.552255|0
Albaida|Albaida||Vall d'Albaida|PV|38.84834|-0.524578|1
Albal|Albal||Horta Sud|PV|39.392701|-0.417452|1
Albalat de la Ribera|Albalat de la Ribera||Ribera Baixa|PV|39.220217|-0.373946|1
Albalat dels Sorells|Albalat dels Sorells||Horta Nord|PV|39.550007|-0.350923|1
Albalat dels Tarongers|Albalat dels Tarongers||Camp de Morvedre|PV|39.676929|-0.357516|1
Albanyà|Albanyà||Alt Empordà|CAT|42.31823|2.662493|1
Albarca|Albarca||Priorat|CAT|41.300501|0.912305|0
Albatera|Albatera||Baix Segura|PV|38.215944|-0.891871|1
Albatàrrec|Albatàrrec||Segrià|CAT|41.566269|0.617618|1
Albelda|Albelda||la Llitera|FR|41.848646|0.493907|1
Alberic|Alberic||Ribera Alta|PV|39.118335|-0.537202|1
Alberola|Alberola||Noguera|CAT|41.935493|0.678356|0
Albesa|Albesa||Noguera|CAT|41.759206|0.670461|1
Albet|Albet||Alt Urgell|CAT|42.406459|1.324652|0
Albinyana|Albinyana||Baix Penedès|CAT|41.242096|1.49253|1
Albió|Albió||Conca de Barberà|CAT|41.564926|1.270656|0
Albocàsser|Albocàsser||Alt Maestrat|PV|40.373172|0.017126|1
Alboi|Alboi||Costera|PV|38.97415|-0.487104|0
Albons|Albons||Baix Empordà|CAT|42.107307|3.080934|1
Alborada|Alborada||Marina Alta|PV|38.745639|0.158242|0
Alboraia|Alboraia/Alboraya||Horta Nord|PV|39.500448|-0.337096|1
Alboraig|Alborache||Foia de Bunyol|PV|39.377605|-0.755449|1
Albuixec|Albuixech||Horta Nord|PV|39.542969|-0.317819|1
Alcalalí|Alcalalí||Marina Alta|PV|38.755883|-0.030991|1
Alcalfar|Alcalfar||Menorca|IB|39.835035|4.291511|0
Alcalà de la Jovada|Alcalà de la Jovada||Marina Alta|PV|38.794035|-0.253167|0
Alcalà de Xivert|Alcalà de Xivert||Baix Maestrat|PV|40.295829|0.231251|1
Alcanar|Alcanar||Montsià|CAT|40.573683|0.520309|1
Alcanó|Alcanó||Segrià|CAT|41.469681|0.625526|1
Alcarràs|Alcarràs||Segrià|CAT|41.609249|0.465928|1
Alcoi|Alcoi / Alcoy||Alcoià|PV|38.68228|-0.516581|1
Alcoleja|Alcoleja||Comtat|PV|38.664893|-0.327463|1
Alcoletge|Alcoletge||Segrià|CAT|41.648525|0.699634|1
Alconàsser|Alconàsser||Mallorca|IB|39.777053|2.666322|0
Alcossebre|Alcossebre||Baix Maestrat|PV|40.245289|0.276625|0
Alcosser|Alcosser||Comtat|PV|38.795077|-0.400636|1
Alcotas|Alcotas||Serrans|PV|39.821001|-0.917859|0
Alcover|Alcover||Alt Camp|CAT|41.265573|1.167171|1
Alcàntera de Xúquer|Alcàntera de Xúquer||Ribera Alta|PV|39.065752|-0.55791|1
Alcàsser|Alcàsser||Horta Sud|PV|39.381098|-0.447173|1
Alcúdia|Alcúdia||Mallorca|IB|39.8424|3.111833|1
Aldaia|Aldaia||Horta Sud|PV|39.459876|-0.488401|1
Aldamar|Aldamar||Foia de Bunyol|PV|39.449922|-0.631649|0
Aldea de las Cuevas|Aldea de las Cuevas||Marina Alta|PV|38.783305|-0.009351|0
Aldover|Aldover||Baix Ebre|CAT|40.866828|0.474491|1
Alella|Alella||Maresme|CAT|41.499072|2.291938|1
Alella de Mar|Alella de Mar||Maresme|CAT|41.480949|2.299471|0
Alella Park|Alella Park||Maresme|CAT|41.511561|2.290886|0
Alentorn|Alentorn||Noguera|CAT|41.92536|1.061227|0
Aleny|Aleny||Anoia|CAT|41.749313|1.513658|0
Alenyà|Alénya||el Rosselló|CN|42.640885|2.981546|1
Aler|Aler||la Ribagorça|FR|42.112551|0.425928|0
Alfafar|Alfafar||Horta Sud|PV|39.402993|-0.361571|1
Alfafara|Alfafara||Comtat|PV|38.780872|-0.563742|1
Alfara de Carles|Alfara de Carles||Baix Ebre|CAT|40.854006|0.368236|1
Alfara de la Baronia|Alfara de la Baronia||Camp de Morvedre|PV|39.771245|-0.346836|1
Alfara del Patriarca|Alfara del Patriarca||Horta Nord|PV|39.539009|-0.381021|1
Alfarb|Alfarb||Ribera Alta|PV|39.282572|-0.523108|1
Alfarrasí|Alfarrasí||Vall d'Albaida|PV|38.906441|-0.502446|1
Alfarràs|Alfarràs||Segrià|CAT|41.830118|0.565993|1
Alfauir|Alfauir||Safor|PV|38.929972|-0.246408|1
Alfinach|Alfinach||Horta Nord|PV|39.630363|-0.338637|0
Alfondeguilla|Alfondeguilla||Plana Baixa|PV|39.838554|-0.281527|1
Alforja|Alforja||Baix Camp|CAT|41.205168|0.961215|1
Alfés|Alfés||Segrià|CAT|41.513927|0.634652|1
Algaiat|Algaiat||Vinalopó Mitjà|PV|38.348015|-0.969654|0
Algaida|Algaida||Mallorca|IB|39.564877|2.903881|1
Algar de Palància|Algar de Palància||Camp de Morvedre|PV|39.785755|-0.363931|1
Algayón|Algayón||la Llitera|FR|41.832783|0.378786|0
Algemesí|Algemesí||Ribera Alta|PV|39.213782|-0.42921|1
Algerri|Algerri||Noguera|CAT|41.823476|0.635175|1
Alginet|Alginet||Ribera Alta|PV|39.259925|-0.463904|1
Algorfa|Algorfa||Baix Segura|PV|38.064056|-0.786682|1
Alguaire|Alguaire||Segrià|CAT|41.738845|0.561928|1
Algímia d'Alfara|Algímia d'Alfara||Camp de Morvedre|PV|39.744749|-0.393226|1
Algímia d'Almonesir|Algimia de Almonacid||Alt Palància|PV|39.915388|-0.420739|1
Alins|Alins||Pallars Sobirà|CAT|42.579387|1.351669|1
Alins|Alins||la Ribagorça|FR|42.434018|0.626942|0
Alins del Monte|Alins del Monte||la Llitera|FR|42.018774|0.336442|0
Alinyà|Alinyà||Alt Urgell|CAT|42.180344|1.422883|0
Alió|Alió||Alt Camp|CAT|41.297312|1.306813|1
All|All||Cerdanya|CAT|42.39772|1.837299|0
Almacelles|Almacelles||Segrià|CAT|41.731317|0.457705|1
Almardà|Almardà||Camp de Morvedre|PV|39.696284|-0.202221|0
Almassora|Almassora||Plana Alta|PV|39.95128|-0.068026|1
Almatret|Almatret||Segrià|CAT|41.320983|0.406696|1
Almeda Mercader|Almeda Mercader||Baix Llobregat|CAT|41.353197|2.083172|0
Almedíxer|Almedíjar||Alt Palància|PV|39.872342|-0.407953|1
Almenar|Almenar||Segrià|CAT|41.78437|0.539375|1
Almenara|Almenara||Plana Baixa|PV|39.751259|-0.226439|1
Almenara Alta|Almenara Alta||Urgell|CAT|41.741903|1.055448|0
Almiserà|Almiserà||Safor|PV|38.92582|-0.286817|1
Almoines|Almoines||Safor|PV|38.945251|-0.179806|1
Almoradí|Almoradí||Baix Segura|PV|38.072138|-0.791498|1
Almoster|Almoster||Baix Camp|CAT|41.199664|1.100483|1
Almudaina|Almudaina||Comtat|PV|38.764179|-0.357822|1
Almunia de San Lorenzo|Almunia de San Lorenzo||la Ribagorça|FR|42.16808|0.602049|0
Almussafes|Almussafes||Ribera Baixa|PV|39.297011|-0.411456|1
Almàssera|Almàssera||Horta Nord|PV|39.514155|-0.350376|1
Alorda Parc|Alorda Parc||Baix Penedès|CAT|41.190556|1.415317|0
Alp|Alp||Cerdanya|CAT|42.346754|1.936068|1
Alpatró|Alpatró||Marina Alta|PV|38.820137|-0.261037|0
Alpens|Alpens||Lluçanès|CAT|42.121143|2.091953|1
Alpicat|Alpicat||Segrià|CAT|41.670259|0.555183|1
Alpont|Alpuente||Serrans|PV|39.907514|-1.027397|1
Alquebla|Alquebla||Vinalopó Mitjà|PV|38.429363|-0.852119|0
Alquería de Ferrando|Alquería de Ferrando||Marina Alta|PV|38.811106|0.071768|0
Alsamora|Alsamora||Pallars Jussà|CAT|42.081191|0.729404|0
Alt Aran|Naut Aran||la Vall d'Aran|CAT|42.701168|0.924305|1
Alt de Botigueta|Alt de Botigueta||Camp de Túria|PV|39.627001|-0.565567|0
Alt Àneu|Alt Àneu||Pallars Sobirà|CAT|42.682771|1.060597|1
Alta-riba|Alta-riba||Segarra|CAT|41.711775|1.365474|0
Altadill|Altadill||Segarra|CAT|41.670221|1.388805|0
Altafulla|Altafulla||Tarragonès|CAT|41.151707|1.380316|1
Altamira|Altamira||Baix Vinalopó|PV|38.287386|-0.733273|0
Altea|Altea||Marina Baixa|PV|38.62534|-0.04724|1
Altea la Vella|Altea la Vella||Marina Baixa|PV|38.6311|-0.044247|0
Alteró de Mompoi|Alteró de Mompoi||Horta Sud|PV|39.374681|-0.526051|0
Altet|Altet||Urgell|CAT|41.683471|1.142491|0
Alto de la Torre|Alto de la Torre||Foia de Bunyol|PV|39.425253|-0.700968|0
Altron|Altron||Pallars Sobirà|CAT|42.450536|1.103648|0
Altura|Altura||Alt Palància|PV|39.814926|-0.58464|1
Altury|Altury||Ribera Alta|PV|39.328804|-0.698837|0
Alzina|Alzina||Pallars Jussà|CAT|42.083696|0.806897|0
Alzira|Alzira||Ribera Alta|PV|39.136973|-0.450772|1
Alàs|Alàs||Alt Urgell|CAT|42.351881|1.508291|0
Alàs i Cerc|Alàs i Cerc||Alt Urgell|CAT|42.322496|1.520485|1
Alòs de Balaguer|Alòs de Balaguer||Noguera|CAT|41.922546|0.944838|1
Alós d'Isil|Alós d'Isil||Pallars Sobirà|CAT|42.700883|1.099888|0
Amer|Amer||Selva|CAT|42.010456|2.601697|1
Amorós|Amorós||Segarra|CAT|41.653167|1.426727|0
Ampliacio del Poble|Ampliacio del Poble||Baix Penedès|CAT|41.253963|1.556931|0
Amposta|Amposta||Montsià|CAT|40.675863|0.639359|1
Ancinyà|Ansignan||la Fenolleda|CN|42.762623|2.520678|1
Andilla|Andilla||Serrans|PV|39.81251|-0.802188|1
Andorra la Vella|Andorra la Vella|||AD|42.50779|1.52109|0
Andratx|Andratx||Mallorca|IB|39.582054|2.40242|1
Aneto|Aneto||la Ribagorça|FR|42.554432|0.742976|0
Anglesola|Anglesola||Urgell|CAT|41.662764|1.058874|1
Anglès|Anglès||Selva|CAT|41.944077|2.637848|1
Angostrina i Vilanova de les Escaldes|Angoustrine-Villeneuve-des-Escaldes||l'Alta Cerdanya|CN|42.564597|1.950447|1
Anna|Anna||Canal de Navarrés|PV|39.0149|-0.655456|1
Annauir|Annauir||Costera|PV|38.975896|-0.565182|0
Anroig|Anroig||Baix Maestrat|PV|40.512818|0.119678|0
Ansalonga|Ansalonga|||AD|42.55623|1.53319|0
Anserall|Anserall||Alt Urgell|CAT|42.379086|1.456478|0
Ansovell|Ansovell||Alt Urgell|CAT|42.325185|1.584003|0
Antella|Antella||Ribera Alta|PV|39.096918|-0.60117|1
Antenza|Antenza||la Ribagorça|FR|42.068028|0.553572|0
Antist|Antist||Pallars Jussà|CAT|42.371076|0.948369|0
Antius|Antius||Bages|CAT|41.80619|1.760537|0
Anya|Anya||Noguera|CAT|41.923701|1.106534|0
Anyós|Anyós|||AD|42.54499|1.51483|0
Anàs|Anàs||Pallars Sobirà|CAT|42.576321|1.212515|0
Arabí|Arabí||Marina Baixa|PV|38.575616|-0.094375|0
Araia|Araia||Alcalatén|PV|40.065794|-0.27481|0
Aramunt|Aramunt||Pallars Jussà|CAT|42.201907|0.989097|0
Arans|Arans|||AD|42.55623|1.53319|0
Aransís|Aransís||Pallars Jussà|CAT|42.086441|0.955513|0
Aranyel|Arañuel||Alt Millars|PV|40.083315|-0.488504|1
Aravell|Aravell||Alt Urgell|CAT|42.360792|1.404029|0
Araós|Araós||Pallars Sobirà|CAT|42.534924|1.261188|0
Arbeca|Arbeca||Garrigues|CAT|41.544629|0.936703|1
Arbolí|Arbolí||Baix Camp|CAT|41.23843|0.956554|1
Arboçols|Arboussols||el Conflent|CN|42.666163|2.492752|1
Arbúcies|Arbúcies||Selva|CAT|41.824535|2.490877|1
Arcalís|Arcalís||Pallars Sobirà|CAT|42.353882|1.084065|0
Arcavell|Arcavell||Alt Urgell|CAT|42.430433|1.482898|0
Arcavell i la Farga de Moles|Arcavell i la Farga de Moles||Alt Urgell|CAT|42.443332|1.454653|0
Ardenya|Ardenya||Tarragonès|CAT|41.169609|1.343377|0
Ardèvol|Ardèvol||Solsonès|CAT|41.851394|1.518943|0
Area Especial CN340|Area Especial CN340||Baix Camp|CAT|41.07795|1.063741|0
Arenal d'en Castell|Arenal d'en Castell||Menorca|IB|40.021467|4.186203|0
Arenals de Mar|Arenals de Mar||Baix Empordà|CAT|41.995683|3.190401|0
Arengades-Enginyers|Arengades-Enginyers||Alt Empordà|CAT|42.256263|2.950395|0
Areny de Noguera|Arén||la Ribagorça|FR|42.289998|0.674892|1
Arenys d'Empordà|Arenys d'Empordà||Alt Empordà|CAT|42.169213|2.955652|0
Arenys de Lledó|Arens de Lledó||el Matarranya|FR|40.990041|0.24559|1
Arenys de Mar|Arenys de Mar||Maresme|CAT|41.583159|2.547395|1
Arenys de Munt|Arenys de Munt||Maresme|CAT|41.609992|2.530754|1
Arenys Residencial|Arenys Residencial||Maresme|CAT|41.597276|2.544505|0
Ares del Bosque|Ares del Bosque||Comtat|PV|38.686734|-0.326172|0
Ares del Maestrat|Ares del Maestrat||Alt Maestrat|PV|40.444848|-0.109681|1
Ares dels Oms|Aras de los Olmos||Serrans|PV|39.928837|-1.148069|1
Arestui|Arestui||Pallars Sobirà|CAT|42.507874|1.166513|0
Arfa|Arfa||Alt Urgell|CAT|42.329833|1.415451|0""",
        """Argelaguer|Argelaguer||Garrotxa|CAT|42.209658|2.630045|1
Argelers de la Marenda|Argelès-sur-Mer||el Rosselló|CN|42.535057|3.024208|1
Argeleta|Argelita||Alt Millars|PV|40.067074|-0.340956|1
Argentona|Argentona||Maresme|CAT|41.561998|2.391259|1
Argençola|Argençola||Anoia|CAT|41.595382|1.45461|1
Ariany|Ariany||Mallorca|IB|39.655884|3.132672|1
Arinsal|Arinsal|||AD|42.54499|1.51483|0
Aristot|Aristot||Alt Urgell|CAT|42.379113|1.623553|0
Arles de Tec|Arles-sur-Tech||el Vallespir|CN|42.449556|2.62598|1
Arnes|Arnes||Terra Alta|CAT|40.876407|0.27566|1
Arneva|Arneva||Baix Segura|PV|38.055915|-0.941524|0
Arrabassada i Savinosa|Arrabassada i Savinosa||Tarragonès|CAT|41.125095|1.280358|0
Arres|Arres||la Vall d'Aran|CAT|42.760549|0.707749|1
Arres de Jos|Arres de Jos||la Vall d'Aran|CAT|42.754762|0.711115|0
Arres de Sus|Arres de Sus||la Vall d'Aran|CAT|42.757158|0.712139|0
Arroyo Cerezo|Arroyo Cerezo||Racó d'Ademús|PV|40.12096|-1.419263|0
Arròs|Arròs||la Vall d'Aran|CAT|42.738529|0.761632|0
Arròs i Vila|Arròs e Vila|Arròs e Vila|Aran|CAT|42.705946|0.787125|0
Arró|Arró||Aran|CAT|42.744504|0.712633|0
Arrós|Arrós||Pallars Sobirà|CAT|42.592607|1.248333|0
Ars|Ars||Alt Urgell|CAT|42.445239|1.393687|0
Arsèguel|Arsèguel||Alt Urgell|CAT|42.35168|1.585108|1
Artaj|Artaj||Serrans|PV|39.801776|-0.790674|0
Artana|Artana||Plana Baixa|PV|39.895332|-0.246505|1
Arteas de Abajo|Arteas de Abajo||Alt Palància|PV|39.903586|-0.745892|0
Artedó|Artedó||Alt Urgell|CAT|42.335857|1.514306|0
Artesa|Artesa||Plana Baixa|PV|39.958523|-0.287758|0
Artesa de Lleida|Artesa de Lleida||Segrià|CAT|41.54383|0.698147|1
Artesa de Segre|Artesa de Segre||Noguera|CAT|41.936284|1.08144|1
Arties|Arties||la Vall d'Aran|CAT|42.699773|0.872519|0
Arties i Garòs|Arties e Garòs|Arties e Garòs|Aran|CAT|42.701168|0.924305|0
Artà|Artà||Mallorca|IB|39.714629|3.32869|1
Artés|Artés||Bages|CAT|41.796213|1.956974|1
Arànser|Arànser||Cerdanya|CAT|42.408104|1.662667|0
Ascó|Ascó||Ribera d'Ebre|CAT|41.164456|0.550204|1
Asnurri|Asnurri||Alt Urgell|CAT|42.44491|1.422724|0
Asp|Aspe||Vinalopó Mitjà|PV|38.329662|-0.785487|1
Aspa|Aspa||Segrià|CAT|41.49645|0.674954|1
Assumpció|Assumpció||Maresme|CAT|41.512851|2.315128|0
Assuévar|Azuébar||Alt Palància|PV|39.850658|-0.366371|1
Astell|Astell||Pallars Jussà|CAT|42.403167|0.965306|0
Atzeneta d'Albaida|Atzeneta d'Albaida||Vall d'Albaida|PV|38.832068|-0.486808|1
Atzeneta del Maestrat|Atzeneta del Maestrat||Alt Maestrat|PV|40.234812|-0.185751|1
Aubèrt|Aubèrt||la Vall d'Aran|CAT|42.730755|0.777183|0
Aubèrt i Betlan|Aubèrt e Betlan|Aubèrt e Betlan|Aran|CAT|42.705946|0.787125|0
Aulàs|Aulàs||Pallars Jussà|CAT|42.274136|0.801967|0
Auvinyà|Auvinyà|||AD|42.46372|1.49129|0
Avellanet|Avellanet||Alt Urgell|CAT|42.35075|1.328444|0
Avellanos|Avellanos||Pallars Jussà|CAT|42.404672|0.899497|0
Avinyonet de Puigventós|Avinyonet de Puigventós||Alt Empordà|CAT|42.25294|2.914259|1
Avinyonet del Penedès|Avinyonet del Penedès||Alt Penedès|CAT|41.352228|1.783101|1
Avinyó|Avinyó||Bages|CAT|41.860491|1.976427|1
Avinyó Nou|Avinyó Nou||Alt Penedès|CAT|41.361524|1.776314|0
Avià|Avià||Berguedà|CAT|42.065745|1.830631|1
Azanuy|Azanuy||la Llitera|FR|41.972242|0.311108|0
Aín|Aín||Plana Baixa|PV|39.893837|-0.348675|1
Badalona|Badalona||Barcelonès|CAT|41.461886|2.236594|1
Badia Blava|Badia Blava||Mallorca|IB|39.43171|2.752729|0
Badia de Palma|Badia de Palma||Mallorca|IB|39.496021|2.530044|0
Badia del Vallès|Badia del Vallès||Vallès Occidental|CAT|41.508205|2.114901|1
Badia Gran|Badia Gran||Mallorca|IB|39.42664|2.749934|0
Baells|Baélls||la Llitera|FR|41.958156|0.464609|1
Bagergue|Bagergue||Aran|CAT|42.719388|0.916614|0
Bages de Rosselló|Bages||el Rosselló|CN|42.60776|2.889096|1
Bagà|Bagà||Berguedà|CAT|42.279069|1.858179|1
Bahía de Jávea|Bahía de Jávea||Marina Alta|PV|38.788026|0.178442|0
Baiasca|Baiasca||Pallars Sobirà|CAT|42.50947|1.14261|0
Baix des Puig|Baix des Puig||Mallorca|IB|39.660816|2.734301|0
Baix Pallars|Baix Pallars||Pallars Sobirà|CAT|42.321497|1.060813|1
Baix Poble|Baix Poble||Maresme|CAT|41.563737|2.510087|0
Baixos de Calbinyà|Baixos de Calbinyà||Alt Urgell|CAT|42.362886|1.471336|0
Baixàs|Baixas||el Rosselló|CN|42.745703|2.80548|1
Balada|Balada||Montsià|CAT|40.712087|0.683075|0
Balada|Balada||Montsià|CAT|40.7123|0.683483|0
Baladrar-Punta Estrella|Baladrar-Punta Estrella||Marina Alta|PV|38.681199|0.010118|0
Balaguer|Balaguer||Noguera|CAT|41.783054|0.791767|1
Balcó de Tarragona|Balcó de Tarragona||Tarragonès|CAT|41.144379|1.277057|0
Baldomar|Baldomar||Noguera|CAT|41.9179|1.014722|0
Baldovar|Baldovar||Serrans|PV|39.893449|-1.028657|0
Balenyà|Balenyà||Osona|CAT|41.818176|2.211578|1
Ballabriga|Ballabriga||la Ribagorça|FR|42.398345|0.586993|0
Ballestar|Ballestar||Baix Maestrat|PV|40.663984|0.175969|0
Ballestera|Ballestera||Alacantí|PV|38.46595|-0.387389|0
Balneari les Masies|Balneari les Masies||Conca de Barberà|CAT|41.378664|1.091711|0
Balones|Balones||Comtat|PV|38.744141|-0.333205|1
Balonga|Balonga|||CX|38.254955|-1.051719|0
Balsareny|Balsareny||Bages|CAT|41.866152|1.865288|1
Baltarga|Baltarga||Cerdanya|CAT|42.363328|1.808408|0
Banyalbufar|Banyalbufar||Mallorca|IB|39.681316|2.527377|1
Banyeres de Mariola|Banyeres de Mariola||Alcoià|PV|38.705489|-0.677875|1
Banyeres del Penedès|Banyeres del Penedès||Baix Penedès|CAT|41.270831|1.559438|1
Banyoles|Banyoles||Pla de l'Estany|CAT|42.122096|2.771146|1
Banyuls de la Marenda|Banyuls-sur-Mer||el Rosselló|CN|42.461347|3.099502|1
Banyuls dels Aspres|Banyuls-dels-Aspres||el Rosselló|CN|42.565096|2.866585|1
Baqueira|Baqueira||la Vall d'Aran|CAT|42.699632|0.931192|0
Bar|Bar||Alt Urgell|CAT|42.358454|1.639199|0
Baramar|Baramar||Tarragonès|CAT|41.187949|1.478301|0
Barbarroja|Barbarroja||Baix Segura|PV|38.275866|-0.965109|0
Barbens|Barbens||Pla d'Urgell|CAT|41.688099|1.045124|1
Barberà de la Conca|Barberà de la Conca||Conca de Barberà|CAT|41.411146|1.229182|1
Barberà del Vallès|Barberà del Vallès||Vallès Occidental|CAT|41.518216|2.135356|1
Barcelona|Barcelona||Barcelonès|CAT|41.399993|2.151225|1
Baro|Baro||Pallars Sobirà|CAT|42.35942|1.081221|0
Baronia del Montseny i Can Bosc|Baronia del Montseny i Can Bosc||Vallès Oriental|CAT|41.659011|2.463146|0
Barranc Blanc|Barranc Blanc||Safor|PV|38.964894|-0.237239|0
Barranc Fondo|Barranc Fondo||Marina Baixa|PV|38.578304|-0.114871|0
Barranc Fondo|Barranc Fondo||Marina Baixa|PV|38.576011|-0.124927|0
Barranquet-l'Argila|Barranquet-l'Argila||Camp de Túria|PV|39.592788|-0.532416|0
Barraques|Barracas||Alt Palància|PV|39.998882|-0.696128|1
Barri Ballesta|Barri Ballesta||Vallès Oriental|CAT|41.628026|2.229309|0
Barri Can Peret Manent|Barri Can Peret Manent||Vallès Oriental|CAT|41.627854|2.241696|0
Barri Can Tabola|Barri Can Tabola||Vallès Oriental|CAT|41.560467|2.25525|0
Barri Colomer|Barri Colomer||Baix Llobregat|CAT|41.377966|2.019556|0
Barri de Can Xiol|Barri de Can Xiol||Vallès Oriental|CAT|41.58241|2.238552|0
Barri de Granada|Barri de Granada||Alacantí|PV|38.386228|-0.532598|0
Barri de l'Estació|Barri de l'Estació||Gironès|CAT|42.037438|2.918285|0
Barri de la Creu|Barri de la Creu||Berguedà|CAT|42.086785|1.829187|0
Barri de l’Estació|Barri de l’Estació||Vinalopó Mitjà|PV|38.403132|-0.774974|0
Barri de Mongons|Barri de Mongons||Tarragonès|CAT|41.133332|1.228537|0
Barri de Nolla|Barri de Nolla||Horta Nord|PV|39.521938|-0.34596|0
Barri de Porta|Barri de Porta||Horta Sud|PV|39.476435|-0.4633|0
Barri de Sant Antoni|Barri de Sant Antoni||Alt Urgell|CAT|42.362124|1.47524|0
Barri de Sant Pere|Barri de Sant Pere||Alt Urgell|CAT|42.361196|1.486821|0
Barri del Clavell|Barri del Clavell||Ribera Baixa|PV|39.152713|-0.351298|0
Barri del Remei-Castell|Barri del Remei-Castell||Maresme|CAT|41.500058|2.342135|0
Barri la Serra|Barri la Serra||Vallès Oriental|CAT|41.622482|2.220938|0
Barri Maritim de Comar-ruga|Barri Maritim de Comar-ruga||Baix Penedès|CAT|41.182299|1.522795|0
Barri Migdia|Barri Migdia||Vallès Oriental|CAT|41.603479|2.25117|0
Barri Rodes|Barri Rodes||Vallès Oriental|CAT|41.55428|2.297822|0
Barri Sant Josep Artesà|Barri Sant Josep Artesà||Horta Sud|PV|39.477254|-0.473696|0
Barri Serra de Dalt|Barri Serra de Dalt||Alt Penedès|CAT|41.389128|1.612864|0
Barriada Nova|Barriada Nova||Vallès Oriental|CAT|41.619415|2.281926|0
Barrio Alto|Barrio Alto||Baix Vinalopó|PV|38.227724|-0.60076|0
Barrio Arroyo|Barrio Arroyo||Plana d'Utiel|PV|39.516578|-1.16522|0
Barrio Bonny|Barrio Bonny||Alacantí|PV|38.438413|-0.411326|0
Barrio Carretera|Barrio Carretera||Baix Segura|PV|38.083842|-0.920553|0
Barrio Carretera|Barrio Carretera||Baix Segura|PV|38.112518|-0.83757|0
Barrio de la Ascensión|Barrio de la Ascensión||Baix Segura|PV|38.080443|-0.954158|0
Barrio de la Luz|Barrio de la Luz||Horta Sud|PV|39.466611|-0.416564|0
Barrio de la Virgen|Barrio de la Virgen||Baix Segura|PV|37.996544|-0.870014|0
Barrio de los Dolores|Barrio de los Dolores||Baix Segura|PV|38.143653|-0.815544|0
Barrio de los Dolores|Barrio de los Dolores||Horta Nord|PV|39.55656|-0.39493|0
Barrio de Mariano Cases|Barrio de Mariano Cases||Baix Segura|PV|38.067851|-0.954181|0
Barrio de San Casimiro|Barrio de San Casimiro||Plana d'Utiel|PV|39.299647|-1.143451|0
Barrio del Camino Viejo de Hurchillo|Barrio del Camino Viejo de Hurchillo||Baix Segura|PV|38.072619|-0.937535|0
Barrio del Pilar|Barrio del Pilar||Horta Nord|PV|39.56803|-0.403313|0
Barrio el Cerrito|Barrio el Cerrito||Alt Millars|PV|40.135287|-0.498048|0
Barrio Nuestra Señora de la Alegría|Barrio Nuestra Señora de la Alegría||Baix Segura|PV|38.077229|-0.954731|0
Barrio Nuevo|Barrio Nuevo||Baix Segura|PV|38.090466|-0.897049|0
Barrio Orba|Barrio Orba||Horta Sud|PV|39.417197|-0.400595|0
Barrio Roca|Barrio Roca||Horta Nord|PV|39.526403|-0.324168|0
Barrio San Blas|Barrio San Blas||Alt Millars|PV|40.183947|-0.542401|0
Barruera|Barruera||Alta Ribagorça|CAT|42.504784|0.802755|0
Barrí Colomer|Barrí Colomer||Vallès Oriental|CAT|41.527176|2.255564|0
Barrí de Sant Llorenç|Barrí de Sant Llorenç||Vallès Occidental|CAT|41.605367|2.017849|0
Bartró|Bartró||Baix Llobregat|CAT|41.326522|1.925803|0
Barx|Barx||Safor|PV|39.011312|-0.313413|1
Barxeta|Barxeta||Costera|PV|39.012968|-0.421995|1
Base Militar|Base Militar||Alt Empordà|CAT|42.372006|2.971946|0
Bassella|Bassella||Alt Urgell|CAT|42.010746|1.302697|1
Basturs|Basturs||Pallars Jussà|CAT|42.152064|1.011796|0
Batea|Batea||Terra Alta|CAT|41.104275|0.295351|1
Batet|Batet||Ripollès|CAT|42.326326|2.166297|0
Bausén|Bausen||la Vall d'Aran|CAT|42.841538|0.70016|1
Baén|Baén||Pallars Sobirà|CAT|42.326782|1.10528|0
Baños de San Antonio|Baños de San Antonio||Vall de Cofrents|PV|39.043789|-1.051925|0
Baó|Baho||el Rosselló|CN|42.705912|2.820084|1
Beders|Beders||Cerdanya|CAT|42.356206|1.806037|0
Beget|Beget||Ripollès|CAT|42.320542|2.480992|0
Begudà|Begudà||Garrotxa|CAT|42.195796|2.546118|0
Begues|Begues||Baix Llobregat|CAT|41.31852|1.901082|1
Begues Park|Begues Park||Baix Llobregat|CAT|41.340243|1.899536|0
Begur|Begur||Baix Empordà|CAT|41.947883|3.199916|1
Begís|Bejís||Alt Palància|PV|39.900921|-0.736402|1
Begós|Begós||la Vall d'Aran|CAT|42.740877|0.73509|0
Bel|Bel||Baix Maestrat|PV|40.623733|0.162874|0
Bel-Horizonte|Bel-Horizonte||Vall de Cofrents|PV|39.020086|-1.05372|0
Belianes|Belianes||Urgell|CAT|41.570218|0.992735|1
Bell-Lloc|Bell-Lloc||Baix Empordà|CAT|41.837512|2.981456|0
Bell-lloc d'Urgell|Bell-lloc d'Urgell||Pla d'Urgell|CAT|41.631857|0.773609|1
Bellaguarda|Bellaguarda||Garrigues|CAT|41.338612|0.730331|1
Bellamar|Bellamar||Baix Penedès|CAT|41.193854|1.582162|0
Bellaterra|Bellaterra||Vallès Occidental|CAT|41.499808|2.096542|0
Bellaterra|Bellaterra||Ribera Baixa|PV|39.182545|-0.231764|0
Bellavista|Bellavista||Vallès Oriental|CAT|41.623094|2.31999|0
Bellavista|Bellavista||Bages|CAT|41.720165|1.812371|0
Bellavista|Bellavista||Alt Empordà|CAT|42.30601|3.154721|0
Bellavista|Bellavista||Mallorca|IB|39.479911|2.733651|0
Bellavista|Bellavista||Alacantí|PV|38.394157|-0.419438|0
Bellcaire d'Empordà|Bellcaire d'Empordà||Baix Empordà|CAT|42.086739|3.103813|1
Bellcaire d'Urgell|Bellcaire d'Urgell||Noguera|CAT|41.759815|0.890542|1
Bellestar|Bellestar||Alt Urgell|CAT|42.36809|1.419427|0
Bellestar de la Frontera|Bélesta||la Fenolleda|CN|42.720403|2.623677|1
Bellmunt|Bellmunt||Segarra|CAT|41.593143|1.398607|0
Bellmunt d'Urgell|Bellmunt d'Urgell||Noguera|CAT|41.761764|0.946346|1
Bellmunt de Mesquí|Belmonte de San José||el Matarranya|FR|40.882937|-0.061385|1
Bellmunt del Priorat|Bellmunt del Priorat||Priorat|CAT|41.158883|0.759589|1
Bellprat|Bellprat||Anoia|CAT|41.513365|1.447643|1
Bellpuig|Bellpuig||Urgell|CAT|41.629868|1.006277|1
Bellreguard|Bellreguard||Safor|PV|38.953121|-0.152494|1
Bellresguard|Bellresguard||Mallorca|IB|39.912384|3.093745|0
Belltall|Belltall||Conca de Barberà|CAT|41.505253|1.185617|0
Bellulla|Bellulla||Vallès Oriental|CAT|41.620635|2.272814|0
Bellvei|Bellvei||Baix Penedès|CAT|41.234306|1.567646|1
Bellver|Bellver||Alt Penedès|CAT|41.394603|1.658746|0
Bellver d'Ossó|Bellver d'Ossó||Urgell|CAT|41.747492|1.172805|0
Bellver de Cerdanya|Bellver de Cerdanya||Cerdanya|CAT|42.365974|1.768801|1
Bellveí|Bellveí||Segarra|CAT|41.755281|1.291657|0
Bellvís|Bellvís||Pla d'Urgell|CAT|41.681501|0.81709|1
Bellús|Bellús||Vall d'Albaida|PV|38.945893|-0.497902|1
Benafer|Benafer||Alt Palància|PV|39.96999|-0.605744|1
Benafigos|Benafigos||Alt Maestrat|PV|40.273168|-0.195315|1
Benaguasil|Benaguasil||Camp de Túria|PV|39.594064|-0.616631|1
Benaixeve|Benagéber||Serrans|PV|39.722826|-1.124576|1
Benasau|Benasau||Comtat|PV|38.695738|-0.337883|1
Benassal|Benassal||Alt Maestrat|PV|40.386495|-0.167737|1
Benavarri|Benabarre||la Ribagorça|FR|42.0841|0.479141|1
Benavent de la Conca|Benavent de la Conca||Pallars Jussà|CAT|42.074659|1.09704|0
Benavent de Segrià|Benavent de Segrià||Segrià|CAT|41.693071|0.643925|1
Benavites|Benavites||Camp de Morvedre|PV|39.734242|-0.23679|1
Bendinat|Bendinat||Mallorca|IB|39.54635|2.577857|0
Beneixama|Beneixama||Alt Vinalopó|PV|38.722174|-0.7606|1
Beneixida|Beneixida||Ribera Alta|PV|39.070517|-0.548765|1
Benejússer|Benejúzar||Baix Segura|PV|38.073671|-0.83195|1
Benesa|Benesa||Vinalopó Mitjà|PV|38.378065|-0.885027|0
Benestar-Sa Cabana|Benestar-Sa Cabana||Mallorca|IB|39.602806|2.689496|0
Benetússer|Benetússer||Horta Sud|PV|39.422228|-0.397863|1
Benferri|Benferri||Baix Segura|PV|38.151779|-0.983139|1
Beniaia|Beniaia||Marina Alta|PV|38.776861|-0.270298|0
Benialfaquí|Benialfaquí||Comtat|PV|38.77341|-0.340596|0
Benialí|Benialí||Marina Alta|PV|38.823273|-0.220864|0
Beniarbeig|Beniarbeig||Marina Alta|PV|38.817554|-0.011582|1
Beniardà|Beniardà||Marina Baixa|PV|38.696045|-0.214281|1
Beniarjó|Beniarjó||Safor|PV|38.934393|-0.191529|1
Beniarrés|Beniarrés||Comtat|PV|38.825625|-0.363089|1
Beniatjar|Beniatjar||Vall d'Albaida|PV|38.851336|-0.422969|1
Benicarló|Benicarló||Baix Maestrat|PV|40.443732|0.402637|1
Benicolet|Benicolet||Vall d'Albaida|PV|38.919347|-0.331124|1
Benicull de Xúquer|Benicull de Xúquer||Ribera Baixa|PV|39.185071|-0.387531|1
Benicàssim|Benicàssim / Benicasim||Plana Alta|PV|40.056456|0.042189|1
Benidoleig|Benidoleig||Marina Alta|PV|38.792291|-0.027007|1
Benidorm|Benidorm||Marina Baixa|PV|38.554897|-0.128887|1
Benifairó de la Valldigna|Benifairó de la Valldigna||Safor|PV|39.062519|-0.302016|1
Benifairó de les Valls|Benifairó de les Valls||Camp de Morvedre|PV|39.724336|-0.275727|1
Benifaió|Benifaió||Ribera Alta|PV|39.284805|-0.444842|1
Benifallet|Benifallet||Baix Ebre|CAT|40.967588|0.523171|1
Benifallim|Benifallim||Alcoià|PV|38.654961|-0.411666|1
Benifaraig|Benifaraig|||PV|39.529397|-0.385639|0
Benifato|Benifato||Marina Baixa|PV|38.66028|-0.241253|1
Beniflà|Beniflá||Safor|PV|38.9268|-0.190081|1""",
        """Benifons|Benifons||la Ribagorça|FR|42.46995|0.660323|0
Benigembla|Benigembla||Marina Alta|PV|38.746867|-0.115806|1
Benigànim|Benigànim||Vall d'Albaida|PV|38.954692|-0.445824|1
Benijòfer|Benijófar||Baix Segura|PV|38.074921|-0.740866|1
Benilloba|Benilloba||Comtat|PV|38.705684|-0.395696|1
Benillup|Benillup||Comtat|PV|38.756359|-0.379962|1
Benimantell|Benimantell||Marina Baixa|PV|38.645628|-0.202625|1
Benimarco|Benimarco||Marina Alta|PV|38.70049|0.084215|0
Benimarfull|Benimarfull||Comtat|PV|38.775335|-0.391551|1
Benimassot|Benimassot||Comtat|PV|38.753848|-0.298751|1
Benimaurell|Benimaurell||Marina Alta|PV|38.775938|-0.128273|0
Benimeit|Benimeit||Marina Alta|PV|38.701011|0.108877|0
Benimeli|Benimeli||Marina Alta|PV|38.829192|-0.035784|1
Benimodo|Benimodo||Ribera Alta|PV|39.196061|-0.550521|1
Benimuslem|Benimuslem||Ribera Alta|PV|39.138338|-0.494135|1
Benimàmet-Beniferri|Benimàmet-Beniferri|||PV|39.496237|-0.405382|0
Beniparrell|Beniparrell||Horta Sud|PV|39.382927|-0.414261|1
Beniparrell|Beniparrell||Plana Baixa|PV|39.979983|-0.27494|0
Benirrama|Benirrama||Marina Alta|PV|38.830744|-0.196786|0
Benirredrà|Benirredrà||Safor|PV|38.96168|-0.195451|1
Benissa|Benissa||Marina Alta|PV|38.6923|0.037894|1
Benissanet|Benissanet||Ribera d'Ebre|CAT|41.068291|0.584171|1
Benissanó|Benissanó||Camp de Túria|PV|39.615666|-0.572294|1
Benissili|Benissili||Marina Alta|PV|38.818658|-0.277354|0
Benissivà|Benissivà||Marina Alta|PV|38.819056|-0.22532|0
Benissoda|Benissoda||Vall d'Albaida|PV|38.826358|-0.532055|1
Benissuera|Benissuera||Vall d'Albaida|PV|38.90844|-0.481414|1
Benitaia|Benitaia||Marina Alta|PV|38.818427|-0.226803|0
Benitandús|Benitandús||Plana Baixa|PV|39.929247|-0.336449|0
Benlloc|Benlloc||Plana Alta|PV|40.207784|0.057618|1
Benàmer|Benàmer||Comtat|PV|38.781327|-0.418168|0
Benós|Benós||la Vall d'Aran|CAT|42.739559|0.728452|0
Beranui|Beranuy||la Ribagorça|FR|42.381026|0.604405|1
Beranui|Beranui||Pallars Jussà|CAT|42.37421|0.967578|0
Berfull|Berfull||Ribera Alta|PV|39.055746|-0.465105|0
Berga|Berga||Berguedà|CAT|42.100633|1.847599|1
Bernui|Bernui||Pallars Sobirà|CAT|42.448272|1.082766|0
Berrós Jussà|Berrós Jussà||Pallars Sobirà|CAT|42.575204|1.153451|0
Besalú|Besalú||Garrotxa|CAT|42.201492|2.698173|1
Bescanó|Bescanó||Gironès|CAT|41.953122|2.716662|1
Bescaran|Bescaran||Alt Urgell|CAT|42.402194|1.544211|0
Beseit|Beceite||el Matarranya|FR|40.810127|0.200208|1
Beselga|Beselga||Camp de Morvedre|PV|39.710198|-0.367161|0
Betesa|Betesa||la Ribagorça|FR|42.352666|0.691406|0
Betlan|Betlan||la Vall d'Aran|CAT|42.728269|0.786514|0
Betlem|Betlem||Mallorca|IB|39.752808|3.318015|0
Betren|Betren||Aran|CAT|42.697417|0.805896|0
Betxí|Betxí||Plana Baixa|PV|39.921296|-0.196145|1
Beuda|Beuda||Garrotxa|CAT|42.239351|2.715156|1
Biar|Biar||Alt Vinalopó|PV|38.644532|-0.747947|1
Biascas de Obarra|Biascas de Obarra||la Ribagorça|FR|42.339789|0.573868|0
Bicorb|Bicorp||Canal de Navarrés|PV|39.11322|-0.848547|1
Bigastre|Bigastro||Baix Segura|PV|38.060211|-0.895408|1
Bigues|Bigues||Vallès Oriental|CAT|41.676148|2.208655|0
Bigues i Riells del Fai|Bigues i Riells del Fai||Vallès Oriental|CAT|41.681747|2.205084|1
Biniagual|Biniagual||Mallorca|IB|39.65781|2.884628|0
Biniali|Biniali||Mallorca|IB|39.641176|2.859287|0
Biniamar|Biniamar||Mallorca|IB|39.727888|2.87981|0
Biniancolla|Biniancolla||Menorca|IB|39.8135|4.261885|0
Biniancollet|Biniancollet||Menorca|IB|39.83805|4.262133|0
Biniaraix|Biniaraix||Mallorca|IB|39.772323|2.731872|0
Binibèquer Nou|Binibèquer Nou||Menorca|IB|39.818451|4.245234|0
Binibèquer Vell|Binibèquer Vell||Menorca|IB|39.823618|4.233404|0
Binidalí|Binidalí||Menorca|IB|39.83614|4.195067|0
Binissafúller|Binissafúller||Menorca|IB|39.826057|4.22593|0
Binissalem|Binissalem||Mallorca|IB|39.684515|2.856821|1
Binixíquer|Binixíquer||Menorca|IB|39.873146|4.184199|0
Biosca|Biosca||Solsonès|CAT|41.871208|1.373495|1
Biure|Biure||Alt Empordà|CAT|42.339744|2.901767|1
Biure de Gaià|Biure de Gaià||Conca de Barberà|CAT|41.490884|1.350944|0
Bixessarri|Bixessarri|||AD|42.46372|1.49129|0
Blancafort|Blancafort||Conca de Barberà|CAT|41.449554|1.150363|1
Blanes|Blanes||Selva|CAT|41.689218|2.7829|1
Boadella d'Empordà|Boadella d'Empordà||Alt Empordà|CAT|42.331588|2.856422|0
Boadella i les Escaules|Boadella i les Escaules||Alt Empordà|CAT|42.329124|2.86648|1
Bocairent|Bocairent||Vall d'Albaida|PV|38.742086|-0.608867|1
Bodega Vanacloig|Bodega Vanacloig||Serrans|PV|39.667554|-0.829577|0
Bogadella|Bogadella||Bages|CAT|41.797902|1.770663|0
Bogatell|Bogatell||Alt Camp|CAT|41.227702|1.255782|0
Boixar|Boixar||Baix Maestrat|PV|40.679938|0.120001|0
Bolbait|Bolbaite||Canal de Navarrés|PV|39.050241|-0.746021|1
Boldís Jussà|Boldís Jussà||Pallars Sobirà|CAT|42.613715|1.266554|0
Boldís Sobirà|Boldís Sobirà||Pallars Sobirà|CAT|42.613704|1.271797|0
Boldú|Boldú||Urgell|CAT|41.720034|1.005179|0
Bolquera|Bolquère||l'Alta Cerdanya|CN|42.521707|2.065001|1
Bolulla|Bolulla||Marina Baixa|PV|38.68487|-0.121263|1
Bolvir|Bolvir||Cerdanya|CAT|42.416529|1.882533|1
Bompàs|Bompas||el Rosselló|CN|42.728301|2.941742|1
Bon Any|Bon Any||Alacantí|PV|38.418681|-0.469263|0
Bon Sol|Bon Sol||Alt Camp|CAT|41.286606|1.269297|0
Bon Solei|Bon Solei||Baix Llobregat|CAT|41.334958|1.928164|0
Bon Solei II|Bon Solei II||Baix Llobregat|CAT|41.338535|1.928385|0
Bonaigua|Bonaigua||Tarragonès|CAT|41.171055|1.315119|0
Bonaire|Bonaire||Tarragonès|CAT|41.161508|1.273427|0
Bonaire|Bonaire||Mallorca|IB|39.862978|3.151666|0
Bonalba|Bonalba||Alacantí|PV|38.445549|-0.435301|0
Bonanova|Bonanova||Baix Penedès|CAT|41.200507|1.560821|0
Bonansa|Bonansa||la Ribagorça|FR|42.41166|0.691205|1
Bonany|Bonany||Alt Camp|CAT|41.393496|1.468874|0
Bonastre|Bonastre||Baix Penedès|CAT|41.22498|1.435965|1
Bonaterra I|Bonaterra I||Baix Penedès|CAT|41.241072|1.524824|0
Bonaterra II|Bonaterra II||Baix Penedès|CAT|41.245149|1.523888|0
Bonavista|Bonavista||Alt Penedès|CAT|41.416767|1.72209|0
Bonavista|Bonavista||Tarragonès|CAT|41.119061|1.193674|0
Bonavista|Bonavista||Baix Vinalopó|PV|38.298011|-0.680664|0
Bonestarre|Bonestarre||Pallars Sobirà|CAT|42.574687|1.217435|0
Bonmatí|Bonmatí||Selva|CAT|41.968011|2.670327|0
Bonmont Terres Noves|Bonmont Terres Noves||Baix Camp|CAT|41.040231|0.925183|0
Bonrepòs i Mirambell|Bonrepòs i Mirambell||Horta Nord|PV|39.519341|-0.367323|1
Bonretorn|Bonretorn||Baix Camp|CAT|41.245567|1.128806|0
Bonsol|Bonsol||Tarragonès|CAT|41.134039|1.301493|0
Bor|Bor||Cerdanya|CAT|42.345838|1.801579|0
Borbotó|Borbotó|||PV|39.515547|-0.390695|0
Bordes|Bordes||Marina Baixa|PV|38.522647|-0.232979|0
Bordes d'Envalira|Bordes d'Envalira|||AD|42.5676|1.59756|0
Bordils|Bordils||Gironès|CAT|42.047244|2.910446|1
Bordius|Bordius||la Vall d'Aran|CAT|42.820231|0.752888|0
Borgonyà|Borgonyà||Osona|CAT|42.064581|2.243404|0
Borgonyà|Borgonyà||Pla de l'Estany|CAT|42.101685|2.801514|0
Borrassà|Borrassà||Alt Empordà|CAT|42.217819|2.930564|1
Borredà|Borredà||Berguedà|CAT|42.128393|2.012554|1
Borriana|Borriana/Burriana||Plana Baixa|PV|39.890047|-0.074325|1
Borriol|Borriol||Plana Alta|PV|40.050604|-0.07023|1
Borrissola|Borrissola||Osona|CAT|41.988615|2.236749|0
Borén|Borén||Pallars Sobirà|CAT|42.659807|1.081681|0
Bosc de Tosca|Bosc de Tosca||Garrotxa|CAT|42.160327|2.468491|0
Bosc Ruscalleda|Bosc Ruscalleda||Vallès Oriental|CAT|41.561771|2.323279|0
Boscos de Can Riells|Boscos de Can Riells||Vallès Oriental|CAT|41.697044|2.203712|0
Bossost|Bossòst||la Vall d'Aran|CAT|42.778188|0.689124|1
Bot|Bot||Terra Alta|CAT|41.014923|0.372606|1
Botarell|Botarell||Baix Camp|CAT|41.146266|1.003884|1
Bovera|Bovera||Garrigues|CAT|41.317272|0.638429|1
Boí|Boí||Alta Ribagorça|CAT|42.522265|0.834368|0
Breda|Breda||Selva|CAT|41.753255|2.55652|1
Bresca|Bresca||Pallars Sobirà|CAT|42.311851|1.067859|0
Bressui|Bressui||Pallars Sobirà|CAT|42.40735|1.121089|0
Bretui|Bretui||Pallars Sobirà|CAT|42.332691|1.020704|0
Brises del Mar|Brises del Mar||Tarragonès|CAT|41.168166|1.389008|0
Bruguera|Bruguera||Ripollès|CAT|42.277054|2.185646|0
Brullà|Brouilla||el Rosselló|CN|42.575029|2.894986|1
Brunyola|Brunyola||Selva|CAT|41.9046|2.684676|0
Brunyola i Sant Martí Sapresa|Brunyola i Sant Martí Sapresa||Selva|CAT|41.912267|2.672015|1
Bràfim|Bràfim||Alt Camp|CAT|41.277168|1.341389|1
Buenos Aires|Buenos Aires||Bages|CAT|41.79305|1.777676|0
Bufali|Bufali||Vall d'Albaida|PV|38.866987|-0.504009|1
Bugarra|Bugarra||Serrans|PV|39.616476|-0.771038|1
Buixalleu|Buixalleu||Selva|CAT|41.792051|2.574072|0
Bula d'Amunt|Boule-d'Amont||el Rosselló|CN|42.587706|2.591883|1
Bulaternera|Bouleternère||el Rosselló|CN|42.644744|2.588014|1
Bulevar del Xúquer|Bulevar del Xúquer||Ribera Baixa|PV|39.160466|-0.257194|0
Bunyol|Buñol||Foia de Bunyol|PV|39.429105|-0.848508|1
Bunyola|Bunyola||Mallorca|IB|39.70757|2.712481|1
Burg|Burg||Pallars Sobirà|CAT|42.503854|1.273509|0
Burjassot|Burjassot||Horta Nord|PV|39.50809|-0.41122|1
Busot|Busot||Alacantí|PV|38.500945|-0.418476|1
Butsènit|Butsènit||Segrià|CAT|41.579523|0.573473|0
Butsènit d'Urgell|Butsènit d'Urgell||Noguera|CAT|41.800807|0.991931|0
Buvisa|Buvisa||Maresme|CAT|41.492188|2.342065|0
Buvisa|Buvisa||Maresme|CAT|41.490771|2.338971|0
Bàscara|Bàscara||Alt Empordà|CAT|42.141325|2.916111|1
Bèlgida|Bèlgida||Vall d'Albaida|PV|38.864156|-0.463619|1
Bétera|Bétera||Camp de Túria|PV|39.599819|-0.463079|1
Bítem|Bítem|Bitem|Baix Ebre|CAT|40.859804|0.523298|0
Bóixols|Bóixols||Pallars Jussà|CAT|42.170563|1.163037|0
Búger|Búger||Mallorca|IB|39.758095|2.985055|1
Ca l'Alegre|Ca l'Alegre||Vallès Oriental|CAT|41.547432|2.310087|0
Ca L'Anton|Ca L'Anton||Anoia|CAT|41.53736|1.682772|0
Ca l'Artigues|Ca l'Artigues||Vallès Oriental|CAT|41.615168|2.218682|0
Ca l'Avi|Ca l'Avi||Alt Penedès|CAT|41.409707|1.738835|0
Ca l'Esteper|Ca l'Esteper||Vallès Oriental|CAT|41.608877|2.222653|0
Ca l'Esteper|Ca l'Esteper||Vallès Oriental|CAT|41.603989|2.224684|0
Ca l'Esteve|Ca l'Esteve||Bages|CAT|41.653277|1.763361|0
Ca L'Oliva|Ca L'Oliva||Vallès Occidental|CAT|41.603153|1.899275|0
Ca n'Aguilera|Ca n'Aguilera||Anoia|CAT|41.555096|1.744984|0
Ca n'Alzina|Ca n'Alzina||Anoia|CAT|41.589967|1.6827|0
Ca n'Amat|Ca n'Amat||Baix Llobregat|CAT|41.511684|1.884712|0
Ca n'Amat|Ca n'Amat||Baix Llobregat|CAT|41.500976|1.884294|0
Ca n'Amat|Ca n'Amat||Vallès Occidental|CAT|41.532449|1.9773|0
Ca n'amell|Ca n'amell||Baix Llobregat|CAT|41.331315|1.936209|0
Ca n'Armengol|Ca n'Armengol||Baix Llobregat|CAT|41.410747|1.893428|0
Ca n'Avellaneda|Ca n'Avellaneda||Vallès Occidental|CAT|41.592661|2.06625|0
Ca n'Hoste|Ca n'Hoste||Selva|CAT|41.73808|2.546713|0
Ca n'Iglésies|Ca n'Iglésies||Baix Llobregat|CAT|41.414957|1.96989|0
Ca n'Olivella|Ca n'Olivella||Alt Penedès|CAT|41.358311|1.891646|0
Ca na Negreta|Ca na Negreta||Eivissa|IB|38.948136|1.444926|0
Cabacés|Cabacés||Priorat|CAT|41.245823|0.73043|1
Cabanabona|Cabanabona||Noguera|CAT|41.837657|1.204905|1
Cabanelles|Cabanelles||Alt Empordà|CAT|42.247984|2.785932|1
Cabanes|Cabanes||Alt Empordà|CAT|42.308164|2.968095|1
Cabanes|Cabanes||Plana Alta|PV|40.144357|0.093762|1
Cabdella|Cabdella||Pallars Jussà|CAT|42.474137|0.991932|0
Cabestany|Cabestany||el Rosselló|CN|42.679356|2.945593|1
Cabestany|Cabestany||Segarra|CAT|41.579724|1.278418|0
Cabo Cervera-Playa la Mata|Cabo Cervera-Playa la Mata||Baix Segura|PV|38.010149|-0.653992|0
Cabra del Camp|Cabra del Camp||Alt Camp|CAT|41.39536|1.299983|1
Cabrera d'Anoia|Cabrera d'Anoia||Anoia|CAT|41.488899|1.710493|1
Cabrera de Mar|Cabrera de Mar||Maresme|CAT|41.525756|2.400816|1
Cabrianes|Cabrianes||Bages|CAT|41.79434|1.904816|0
Cabrils|Cabrils||Maresme|CAT|41.529544|2.367523|1
Cabó|Cabó||Alt Urgell|CAT|42.239419|1.234996|1
Cachap|Cachap||Baix Vinalopó|PV|38.218249|-0.777334|0
Cadaqués|Cadaqués||Alt Empordà|CAT|42.290802|3.273652|1
Caimari|Caimari||Mallorca|IB|39.76981|2.908481|0
Caixàs|Caixas||el Rosselló|CN|42.600347|2.662053|1
Cajigar|Cajigar||la Ribagorça|FR|42.260563|0.597552|0
Cal Bassacs|Cal Bassacs||Berguedà|CAT|42.02117|1.883462|0
Cal Canonge|Cal Canonge||Alt Camp|CAT|41.34048|1.403359|0
Cal Claramunt|Cal Claramunt||Alt Penedès|CAT|41.303434|1.649753|0
Cal Germà|Cal Germà||Pla de l'Estany|CAT|42.118271|2.741662|0
Cal Janes|Cal Janes||Alt Penedès|CAT|41.412349|1.737505|0
Cal Joan Coix|Cal Joan Coix||Vallès Occidental|CAT|41.631778|2.065665|0
Cal Llarg|Cal Llarg||Solsonès|CAT|41.982341|1.505662|0
Cal Marçal|Cal Marçal||Berguedà|CAT|41.956952|1.879229|0
Cal Panxa|Cal Panxa||Alt Penedès|CAT|41.406778|1.799243|0
Cal Pons|Cal Pons||Berguedà|CAT|41.960436|1.884434|0
Cal Pontonet|Cal Pontonet||Alt Penedès|CAT|41.424055|1.50322|0
Cal Ramons|Cal Ramons||Berguedà|CAT|42.025985|1.890316|0
Cal Rata|Cal Rata||Bages|CAT|41.898231|1.887104|0
Cal Riera|Cal Riera||Berguedà|CAT|41.931159|1.879421|0
Cal Rosal|Cal Rosal||Berguedà|CAT|42.072048|1.867989|0
Cal Rosal|Cal Rosal||Berguedà|CAT|42.069164|1.87064|0
Cal Rossell de la Serra|Cal Rossell de la Serra||Alt Penedès|CAT|41.426513|1.74416|0
Cal Rubió|Cal Rubió||Alt Penedès|CAT|41.332405|1.650155|0
Cal Salines|Cal Salines||Alt Penedès|CAT|41.328797|1.665939|0
Cal Vidal|Cal Vidal||Berguedà|CAT|41.94292|1.880186|0
Cal Vies|Cal Vies||Baix Penedès|CAT|41.31059|1.637339|0
Cal Viudo|Cal Viudo||Baix Llobregat|CAT|41.32676|1.935769|0
Cala Anguila-Cala Mendia|Cala Anguila-Cala Mendia||Mallorca|IB|39.526294|3.319391|0
Cala Blanca|Cala Blanca||Menorca|IB|39.97204|3.8343|0
Cala Blava|Cala Blava||Mallorca|IB|39.483707|2.736014|0
Cala Bona|Cala Bona||Mallorca|IB|39.614638|3.389609|0
Cala d'Or|Cala d'Or||Mallorca|IB|39.37476|3.226862|0
Cala d'Or|Cala d'Or||Alacantí|PV|38.469134|-0.330839|0
Cala de Bou|Cala de Bou||Eivissa|IB|38.963917|1.298876|0
Cala de Sant Esteve|Cala de Sant Esteve||Menorca|IB|39.858224|4.299975|0
Cala de Sant Vicent|Cala de Sant Vicent||Eivissa|IB|39.076293|1.597751|0
Cala en Blanes|Cala en Blanes||Menorca|IB|40.000558|3.810967|0
Cala en Bosc|Cala en Bosc||Menorca|IB|39.928872|3.833695|0
Cala en Porter|Cala en Porter||Menorca|IB|39.872757|4.135423|0
Cala Ferrera|Cala Ferrera||Mallorca|IB|39.3813|3.239664|0
Cala Figuera|Cala Figuera||Mallorca|IB|39.329684|3.168446|0
Cala Galdana|Cala Galdana||Menorca|IB|39.937062|3.962064|0
Cala Gat|Cala Gat||Mallorca|IB|39.713438|3.472921|0
Cala Llevado|Cala Llevado||Selva|CAT|41.71009|2.909324|0
Cala Lliteres|Cala Lliteres||Mallorca|IB|39.718431|3.461255|0
Cala Llombards|Cala Llombards||Mallorca|IB|39.321174|3.131074|0
Cala Llonga|Cala Llonga||Menorca|IB|39.889912|4.29994|0
Cala Mesquida|Cala Mesquida||Mallorca|IB|39.74393|3.429502|0
Cala Millor|Cala Millor||Mallorca|IB|39.606356|3.384248|0
Cala Montgo|Cala Montgo||Baix Empordà|CAT|42.107375|3.16734|0""",
        """Cala Morell|Cala Morell||Menorca|IB|40.051279|3.884338|0
Cala Murada|Cala Murada||Mallorca|IB|39.451593|3.26765|0
Cala Pi|Cala Pi||Mallorca|IB|39.365251|2.839096|0
Cala Rajada|Cala Rajada||Mallorca|IB|39.711704|3.461234|0
Cala Romana|Cala Romana||Tarragonès|CAT|41.127277|1.289346|0
Cala Sant Vicenç|Cala Sant Vicenç||Mallorca|IB|39.918146|3.056214|0
Cala Santanyí|Cala Santanyí||Mallorca|IB|39.330949|3.144124|0
Cala Serena|Cala Serena||Mallorca|IB|39.379785|3.243778|0
Cala Tamarit|Cala Tamarit||Tarragonès|CAT|41.130257|1.361003|0
Cala Tirant|Cala Tirant||Menorca|IB|40.046218|4.102158|0
Cala Tuent|Cala Tuent||Mallorca|IB|39.843037|2.776298|0
Cala Vedella|Cala Vedella||Eivissa|IB|38.909856|1.223283|0
Calabacín|Calabacín||Camp de Túria|PV|39.655747|-0.662344|0
Calabuig|Calabuig||Alt Empordà|CAT|42.151702|2.92485|0
Calaceit|Calaceite||el Matarranya|FR|41.017144|0.185794|1
Caladrones|Caladrones||la Ribagorça|FR|42.073425|0.524545|0
Calaf|Calaf||Anoia|CAT|41.72156|1.506122|1
Calafat|Calafat||Baix Ebre|CAT|40.926132|0.843248|0
Calafell|Calafell||Baix Penedès|CAT|41.209039|1.589692|1
Calafell Parc|Calafell Parc||Baix Penedès|CAT|41.227776|1.593958|0
Calasanz|Calasanz||la Llitera|FR|42.021783|0.372953|0
Calbinyà|Calbinyà||Alt Urgell|CAT|42.378204|1.467881|0
Calce|Calce||el Rosselló|CN|42.748102|2.75615|1
Calders|Calders||Moianès|CAT|41.772583|1.969952|1
Calders-Escaldes|Calders-Escaldes||Alt Camp|CAT|41.269807|1.26901|0
Calderón|Calderón||Plana d'Utiel|PV|39.52978|-1.185465|0
Caldes d'Estrac|Caldes d'Estrac||Maresme|CAT|41.570734|2.523785|1
Caldes de Malavella|Caldes de Malavella||Selva|CAT|41.834061|2.808697|1
Caldes de Montbui|Caldes de Montbui||Vallès Oriental|CAT|41.644633|2.157271|1
Calella|Calella||Maresme|CAT|41.623068|2.646327|1
Calella de Palafrugell|Calella de Palafrugell||Baix Empordà|CAT|41.89056|3.183497|0
Cales de Mallorca|Cales de Mallorca||Mallorca|IB|39.463576|3.275025|0
Calescoves|Calescoves||Menorca|IB|39.878729|4.168451|0
Calespiques|Calespiques||Menorca|IB|40.00536|3.80438|0
Caleta Santandria|Caleta Santandria||Menorca|IB|39.984248|3.842414|0
Calicanto A|Calicanto A||Foia de Bunyol|PV|39.441391|-0.623777|0
Calicanto B|Calicanto B||Foia de Bunyol|PV|39.432477|-0.623244|0
Califòrnia|Califòrnia||Garraf|CAT|41.272766|1.728636|0
Calldetenes|Calldetenes||Osona|CAT|41.925732|2.29772|1
Calles|Calles||Serrans|PV|39.740775|-0.950273|1
Callosa d'en Sarrià|Callosa d'en Sarrià||Marina Baixa|PV|38.653355|-0.104285|1
Callosa de Segura|Callosa de Segura||Baix Segura|PV|38.134148|-0.871093|1
Callús|Callús||Bages|CAT|41.800205|1.784613|1
Calmella|Calmeilles||el Vallespir|CN|42.55195|2.671396|1
Calonge|Calonge||Baix Empordà|CAT|41.862288|3.073145|0
Calonge|Calonge||Mallorca|IB|39.400579|3.203159|0
Calonge de Segarra|Calonge de Segarra||Anoia|CAT|41.750115|1.485384|1
Calonge i Sant Antoni|Calonge i Sant Antoni||Baix Empordà|CAT|41.863503|3.073023|1
Calp|Calp||Marina Alta|PV|38.652636|0.044235|1
Calvera|Calvera||la Ribagorça|FR|42.387886|0.613514|0
Calvià|Calvià||Mallorca|IB|39.551011|2.508113|1
Camallera|Camallera||Alt Empordà|CAT|42.123172|2.963883|0
Camarasa|Camarasa||Noguera|CAT|41.913854|0.867405|1
Camarles|Camarles||Baix Ebre|CAT|40.779504|0.651521|1
Camaro|Camaro||Anoia|CAT|41.540707|1.677468|0
Cambrils|Cambrils||Baix Camp|CAT|41.082371|1.042031|1
Cambrils|Cambrils||Solsonès|CAT|42.138011|1.389278|0
Cambrils Badia|Cambrils Badia||Baix Camp|CAT|41.068551|1.072622|0
Cameles|Camélas||el Rosselló|CN|42.638659|2.688272|1
Camino Cañada-Alcoy|Camino Cañada-Alcoy||Alacantí|PV|38.454616|-0.573798|0
Camino de Enmedio|Camino de Enmedio||Baix Segura|PV|38.074335|-0.985266|0
Camino de la Ermita|Camino de la Ermita||Alacantí|PV|38.416533|-0.574343|0
Camino del Ventorrillo|Camino del Ventorrillo||Alacantí|PV|38.42069|-0.552445|0
Camino Paterna|Camino Paterna||Camp de Túria|PV|39.579748|-0.462445|0
Camino Viejo de Callosa|Camino Viejo de Callosa||Baix Segura|PV|38.094391|-0.926947|0
Camp de Lliria|Camp de Lliria||Camp de Túria|PV|39.668091|-0.657965|0
Camp de Turia|Camp de Turia||Camp de Túria|PV|39.564554|-0.552041|0
Camp dels Pilans|Camp dels Pilans||Alt Empordà|CAT|42.122893|3.118967|0
Campamento Militar|Campamento Militar||Camp de Túria|PV|39.618269|-0.479577|0
Campanet|Campanet||Mallorca|IB|39.79744|2.966513|1
Campclar|Campclar||Tarragonès|CAT|41.116031|1.201499|0
Campdevànol|Campdevànol||Ripollès|CAT|42.237152|2.139193|1
Campelles|Campelles||Ripollès|CAT|42.28516|2.134759|1
Campins|Campins||Vallès Oriental|CAT|41.725729|2.471699|1
Campllong|Campllong||Gironès|CAT|41.897954|2.832147|1
Campo Arcís|Campo Arcís||Plana d'Utiel|PV|39.430847|-1.16782|0
Campo de Abajo|Campo de Abajo||Serrans|PV|39.859408|-1.027289|0
Campo de Arriba|Campo de Arriba||Serrans|PV|39.866762|-1.048711|0
Campome|Campôme||el Conflent|CN|42.642718|2.384453|1
Camporrells|Camporrélls||la Llitera|FR|41.949099|0.530591|1
Camporrobles|Camporrobles||Plana d'Utiel|PV|39.636582|-1.378168|1
Campos|Campos||Mallorca|IB|39.406841|3.014613|1
Campossí|Campoussy||la Fenolleda|CN|42.695445|2.451725|1
Campredó|Campredó||Baix Ebre|CAT|40.760471|0.553643|0
Camprodon|Camprodon||Ripollès|CAT|42.310486|2.431024|1
Camps|Camps||Bages|CAT|41.777275|1.688993|0
Camí dels Muntanyans|Camí dels Muntanyans||Alt Camp|CAT|41.280924|1.187999|0
Camós|Camós||Pla de l'Estany|CAT|42.084426|2.762074|1
Can Baldic|Can Baldic||Vallès Oriental|CAT|41.653716|2.291579|0
Can Bargalló|Can Bargalló||Baix Llobregat|CAT|41.458969|1.874699|0
Can Barri|Can Barri||Vallès Oriental|CAT|41.675623|2.229464|0
Can Bartomeu|Can Bartomeu||Maresme|CAT|41.632261|2.600353|0
Can Bas|Can Bas||Alt Penedès|CAT|41.404791|1.759947|0
Can Batista|Can Batista||Alt Penedès|CAT|41.412495|1.775379|0
Can Batlle|Can Batlle||Baix Llobregat|CAT|41.384133|1.934319|0
Can Bells|Can Bells||Selva|CAT|41.89536|2.723418|0
Can Benet de la Prua|Can Benet de la Prua||Alt Penedès|CAT|41.461278|1.795659|0
Can Blanc|Can Blanc||Garrotxa|CAT|42.159815|2.510341|0
Can Bonastre|Can Bonastre||Anoia|CAT|41.504716|1.774137|0
Can Bosc|Can Bosc||Vallès Oriental|CAT|41.550375|2.318304|0
Can Bou|Can Bou||Anoia|CAT|41.528616|1.606555|0
Can Bou|Can Bou||Anoia|CAT|41.487725|1.762289|0
Can Bros|Can Bros||Baix Llobregat|CAT|41.495558|1.915813|0
Can Cabassa|Can Cabassa||Vallès Occidental|CAT|41.535196|1.964865|0
Can Cabot|Can Cabot||Maresme|CAT|41.562441|2.376443|0
Can Cadell|Can Cadell||Selva|CAT|41.742547|2.574436|0
Can Cairot|Can Cairot||Anoia|CAT|41.495128|1.78614|0
Can Canals|Can Canals||Anoia|CAT|41.46919|1.781386|0
Can Canals (Nucli Antic)|Can Canals (Nucli Antic)||Anoia|CAT|41.471247|1.780544|0
Can Candi|Can Candi||Vallès Occidental|CAT|41.604602|2.013884|0
Can Canonge|Can Canonge||Baix Llobregat|CAT|41.435493|1.958166|0
Can Canyameres i Pedra Santa|Can Canyameres i Pedra Santa||Vallès Occidental|CAT|41.582245|2.126002|0
Can Carbonell|Can Carbonell||Selva|CAT|41.806274|2.848328|0
Can Carbonell|Can Carbonell||Mallorca|IB|39.610141|2.695979|0
Can Carreras|Can Carreras||Maresme|CAT|41.636617|2.647299|0
Can Carreres|Can Carreres||Vallès Oriental|CAT|41.669069|2.194303|0
Can Cartró|Can Cartró||Alt Penedès|CAT|41.396176|1.738808|0
Can Casablanques|Can Casablanques||Vallès Occidental|CAT|41.539044|2.076868|0
Can Castany|Can Castany||Baix Llobregat|CAT|41.389545|1.96692|0
Can Castanyer|Can Castanyer||Vallès Oriental|CAT|41.69297|2.203093|0
Can Castells|Can Castells||Vallès Oriental|CAT|41.626465|2.277609|0
Can Castellví|Can Castellví||Alt Penedès|CAT|41.442438|1.622757|0
Can Catassús|Can Catassús||Alt Penedès|CAT|41.442417|1.796247|0
Can Cavaller|Can Cavaller||Bages|CAT|41.790551|1.778647|0
Can Cebrià|Can Cebrià||Vallès Oriental|CAT|41.549299|2.296416|0
Can Cerdanet|Can Cerdanet||Vallès Oriental|CAT|41.573069|2.226261|0
Can Claramunt|Can Claramunt||Anoia|CAT|41.508335|1.749396|0
Can Clavos-Ca Na Palava|Can Clavos-Ca Na Palava||Eivissa|IB|38.958443|1.444443|0
Can Coll|Can Coll||Baix Llobregat|CAT|41.431423|1.940679|0
Can Comulada|Can Comulada||Maresme|CAT|41.4969|2.282387|0
Can Coral|Can Coral||Alt Penedès|CAT|41.399861|1.574424|0
Can Corbera|Can Corbera||Vallès Occidental|CAT|41.565896|1.956661|0
Can Cornet|Can Cornet||Maresme|CAT|41.633182|2.688459|0
Can Cortès i la Pedra Llarga|Can Cortès i la Pedra Llarga||Vallès Occidental|CAT|41.57999|2.177467|0
Can Costa|Can Costa||Vallès Occidental|CAT|41.489801|1.984529|0
Can Costa|Can Costa||Vallès Oriental|CAT|41.615219|2.212277|0
Can Costa|Can Costa||Alt Penedès|CAT|41.350348|1.844619|0
Can Costa Redona|Can Costa Redona||Eivissa|IB|38.981376|1.410441|0
Can Creixell|Can Creixell||Anoia|CAT|41.496349|1.774632|0
Can Cruixent|Can Cruixent||Vallès Oriental|CAT|41.70496|2.424205|0
Can Cruset|Can Cruset||Alt Penedès|CAT|41.382371|1.585667|0
Can Cuiàs|Can Cuiàs||Vallès Occidental|CAT|41.461993|2.161709|0
Can Dalmases|Can Dalmases||Baix Llobregat|CAT|41.563778|1.803001|0
Can Derrocada|Can Derrocada||Vallès Oriental|CAT|41.684436|2.395864|0
Can Diviu|Can Diviu||Vallès Oriental|CAT|41.625976|2.265658|0
Can Domenec|Can Domenec||Maresme|CAT|41.648346|2.608399|0
Can Domènec|Can Domènec||Maresme|CAT|41.651382|2.612329|0
Can Duran|Can Duran||Vallès Oriental|CAT|41.62605|2.271572|0
Can Duran|Can Duran||Vallès Occidental|CAT|41.601336|2.030352|0
Can Falguera-Els Turons|Can Falguera-Els Turons||Vallès Occidental|CAT|41.595741|2.188904|0
Can Farell|Can Farell||Vallès Oriental|CAT|41.631315|2.216864|0
Can Febrera|Can Febrera||Vallès Oriental|CAT|41.679428|2.238303|0
Can Ferran|Can Ferran||Baix Empordà|CAT|41.937872|3.209063|0
Can Ferrer de la Cogullada|Can Ferrer de la Cogullada||Baix Penedès|CAT|41.324445|1.42067|0
Can Figueres|Can Figueres||Vallès Oriental|CAT|41.735743|2.497926|0
Can Flautes|Can Flautes||Bages|CAT|41.872094|1.714698|0
Can Font|Can Font||Vallès Occidental|CAT|41.596837|2.061435|0
Can Fornaca|Can Fornaca||Selva|CAT|41.808506|2.639815|0
Can Franc|Can Franc||Vallès Occidental|CAT|41.605568|1.869394|0
Can Franquesa|Can Franquesa||Vallès Oriental|CAT|41.601588|2.258826|0
Can Galan|Can Galan||Anoia|CAT|41.556811|1.6791|0
Can Garballer|Can Garballer||Vallès Oriental|CAT|41.709924|2.431593|0
Can Ginebre|Can Ginebre||Maresme|CAT|41.622717|2.580813|0
Can Gonteres|Can Gonteres||Vallès Occidental|CAT|41.572924|1.98151|0
Can Gordei|Can Gordei||Baix Penedès|CAT|41.264088|1.474396|0
Can Guey|Can Guey||Baix Llobregat|CAT|41.356489|1.961113|0
Can Guitard Vell|Can Guitard Vell||Baix Llobregat|CAT|41.392174|1.974053|0
Can Güell|Can Güell||Baix Llobregat|CAT|41.347004|1.97702|0
Can Jalpi|Can Jalpi||Maresme|CAT|41.597057|2.539128|0
Can Jofre|Can Jofre||Garrotxa|CAT|42.156872|2.51088|0
Can Jordi|Can Jordi||Gironès|CAT|41.904821|2.783054|0
Can Jordi|Can Jordi||Alt Empordà|CAT|42.166101|3.070385|0
Can Jornet|Can Jornet||Vallès Oriental|CAT|41.542586|2.309946|0
Can Julià|Can Julià||Baix Llobregat|CAT|41.382735|1.912284|0
Can Lledó|Can Lledó||Vallès Oriental|CAT|41.587063|2.208374|0
Can Llenes|Can Llenes||Alt Camp|CAT|41.397525|1.438253|0
Can Llong|Can Llong||Vallès Occidental|CAT|41.560145|2.083891|0
Can Llopard|Can Llopard||Baix Llobregat|CAT|41.437975|1.953885|0
Can Lloses-Can Marcer|Can Lloses-Can Marcer||Garraf|CAT|41.274552|1.796|0
Can Lluch|Can Lluch||Baix Llobregat|CAT|41.370853|2.01822|0
Can Lluís|Can Lluís||Baix Llobregat|CAT|41.415506|1.911736|0
Can Macià|Can Macià||Garraf|CAT|41.253156|1.779312|0
Can Magarola|Can Magarola||Maresme|CAT|41.505281|2.303036|0
Can Maiol|Can Maiol||Vallès Occidental|CAT|41.573306|2.184484|0
Can Margarit|Can Margarit||Baix Llobregat|CAT|41.424902|1.93832|0
Can Martí de l'Estela|Can Martí de l'Estela||Anoia|CAT|41.549729|1.753564|0
Can Mas|Can Mas||Anoia|CAT|41.517818|1.775119|0
Can Mascaró|Can Mascaró||Baix Llobregat|CAT|41.402174|1.984115|0
Can Massuet del Far|Can Massuet del Far||Maresme|CAT|41.617191|2.423668|0
Can Mata|Can Mata||Anoia|CAT|41.508937|1.730173|0
Can Maurici|Can Maurici||Maresme|CAT|41.472257|2.276047|0
Can Miret|Can Miret||Vallès Oriental|CAT|41.667467|2.376879|0
Can Miret|Can Miret||Vallès Oriental|CAT|41.670545|2.376849|0
Can Montmany de Mas Peçoles|Can Montmany de Mas Peçoles||Baix Llobregat|CAT|41.425541|1.960646|0
Can Moreno|Can Moreno||Mallorca|IB|39.614696|2.64078|0
Can Morer|Can Morer||Maresme|CAT|41.622661|2.66821|0
Can Morgades|Can Morgades||Alt Penedès|CAT|41.35223|1.853837|0
Can Nadal|Can Nadal||Vallès Oriental|CAT|41.54423|2.318985|0
Can Nadal|Can Nadal||Eivissa|IB|38.984686|1.523337|0
Can Nadal de la Bogadella|Can Nadal de la Bogadella||Alt Penedès|CAT|41.424147|1.700642|0
Can Negre|Can Negre||Eivissa|IB|38.925887|1.423828|0
Can Nicolau de Dalt|Can Nicolau de Dalt||Vallès Occidental|CAT|41.501039|1.963166|0
Can Noguera|Can Noguera||Selva|CAT|41.750151|2.595152|0
Can Palau|Can Palau||Maresme|CAT|41.637598|2.593509|0
Can Palet|Can Palet||Baix Llobregat|CAT|41.437502|1.964509|0
Can Palet|Can Palet||Vallès Occidental|CAT|41.524076|1.961707|0
Can Palet de Vista Alegre|Can Palet de Vista Alegre||Vallès Occidental|CAT|41.540477|1.984458|0
Can Pallars i Llobateres|Can Pallars i Llobateres||Vallès Occidental|CAT|41.536308|2.057074|0
Can Parellada|Can Parellada||Anoia|CAT|41.506341|1.831094|0
Can Pastilla|Can Pastilla||Mallorca|IB|39.535713|2.720987|0
Can Paulet|Can Paulet||Baix Llobregat|CAT|41.403992|1.958766|0
Can Pere de la Plana|Can Pere de la Plana||Garraf|CAT|41.285156|1.806719|0
Can Perera i Can Valls|Can Perera i Can Valls||Vallès Occidental|CAT|41.570126|2.171948|0
Can Picafort|Can Picafort||Mallorca|IB|39.762723|3.15498|0
Can Pla|Can Pla||Vallès Oriental|CAT|41.718133|2.531529|0
Can Plana|Can Plana||Selva|CAT|41.730802|2.542695|0
Can Potxa|Can Potxa||Garrotxa|CAT|42.158389|2.512236|0
Can Prat|Can Prat||Bages|CAT|41.634401|1.814697|0
Can Prat|Can Prat||Vallès Oriental|CAT|41.585915|2.215536|0
Can Prat|Can Prat||Vallès Occidental|CAT|41.609328|2.018958|0
Can Prats|Can Prats||Baix Llobregat|CAT|41.468809|1.878587|0
Can Puigdemir|Can Puigdemir||Vallès Oriental|CAT|41.654198|2.503864|0
Can Pujolet|Can Pujolet||Eivissa|IB|38.967323|1.349081|0
Can Quiseró|Can Quiseró||Anoia|CAT|41.479844|1.804071|0
Can Rabassa|Can Rabassa||Vallès Oriental|CAT|41.541496|2.300134|0
Can Raimí|Can Raimí||Maresme|CAT|41.559493|2.373709|0
Can Ram|Can Ram||Vallès Oriental|CAT|41.682023|2.421005|0
Can Ramon|Can Ramon||Eivissa|IB|38.943763|1.447124|0
Can Ramon|Can Ramon||Eivissa|IB|38.984477|1.516873|0
Can Record|Can Record||Vallès Oriental|CAT|41.707258|2.433722|0
Can Regassol|Can Regassol||Vallès Oriental|CAT|41.659831|2.180867|0
Can Reixac|Can Reixac||Vallès Oriental|CAT|41.652768|2.25872|0
Can Rial|Can Rial||Baix Llobregat|CAT|41.526766|1.854381|0
Can Ribas|Can Ribas||Vallès Oriental|CAT|41.708468|2.425691|0
Can Ribosa|Can Ribosa||Maresme|CAT|41.559414|2.378191|0
Can Riera|Can Riera||Vallès Occidental|CAT|41.579615|2.163712|0
Can Rigol|Can Rigol||Baix Llobregat|CAT|41.40672|1.908001|0
Can Robert|Can Robert||Vallès Occidental|CAT|41.629498|2.006764|0
Can Roca|Can Roca||Garraf|CAT|41.280894|1.745077|0
Can Roig|Can Roig||Baix Llobregat|CAT|41.41239|1.93066|0
Can Ros|Can Ros||Anoia|CAT|41.467786|1.720662|0
Can Rossell|Can Rossell||Alt Penedès|CAT|41.398249|1.805929|0
Can Roure|Can Roure||Vallès Oriental|CAT|41.60074|2.20483|0
Can Roure|Can Roure||Selva|CAT|41.773405|2.726719|0
Can Rovira|Can Rovira||Vallès Oriental|CAT|41.602162|2.197992|0
Can Rovira|Can Rovira||Baix Llobregat|CAT|41.385909|1.929086|0
Can Sadurni|Can Sadurni||Baix Llobregat|CAT|41.340006|1.912291|0
Can Sagrera|Can Sagrera||Maresme|CAT|41.592183|2.540817|0
Can Salgot|Can Salgot||Vallès Oriental|CAT|41.59663|2.212735|0
Can Salgot|Can Salgot||Vallès Oriental|CAT|41.595382|2.214883|0""",
        """Can Salvà|Can Salvà||Selva|CAT|41.724957|2.558193|0
Can Sant Joan|Can Sant Joan||Vallès Occidental|CAT|41.471742|2.183597|0
Can Santeugini|Can Santeugini||Vallès Occidental|CAT|41.489869|1.955268|0
Can Sebastianet|Can Sebastianet||Vallès Oriental|CAT|41.680219|2.365881|0
Can Serra|Can Serra||Vallès Occidental|CAT|41.593982|1.892838|0
Can Serra Estació|Can Serra Estació||Vallès Occidental|CAT|41.600438|1.893394|0
Can Servitge|Can Servitge||Bages|CAT|41.716062|1.76462|0
Can Singala|Can Singala||Mallorca|IB|39.911992|3.097404|0
Can Siurana|Can Siurana||Maresme|CAT|41.470765|2.266666|0
Can Socarrats i Can Moragues|Can Socarrats i Can Moragues||Baix Llobregat|CAT|41.51166|1.922202|0
Can Solà del Racó|Can Solà del Racó||Vallès Occidental|CAT|41.60564|2.034731|0
Can Sors|Can Sors||Maresme|CAT|41.483337|2.30511|0
Can Sunyer del Palau|Can Sunyer del Palau||Baix Llobregat|CAT|41.454792|1.94652|0
Can Suquet|Can Suquet||Vallès Oriental|CAT|41.671039|2.334879|0
Can Terrer|Can Terrer||Selva|CAT|41.889187|2.719682|0
Can Tià Camps|Can Tià Camps||Selva|CAT|41.745568|2.651732|0
Can Tomàs|Can Tomàs||Eivissa|IB|38.980024|1.350656|0
Can Trabal|Can Trabal||Alt Penedès|CAT|41.317677|1.754508|0
Can Traver|Can Traver||Vallès Oriental|CAT|41.67207|2.199728|0
Can Tries|Can Tries||Vallès Occidental|CAT|41.559022|1.983346|0
Can Turu|Can Turu||Vallès Occidental|CAT|41.548102|1.94987|0
Can Valls|Can Valls||Vallès Oriental|CAT|41.619865|2.192332|0
Can Valls|Can Valls||Maresme|CAT|41.608246|2.419593|0
Can Valls|Can Valls||Anoia|CAT|41.4954|1.808884|0
Can Valmanya|Can Valmanya||Maresme|CAT|41.602186|2.61565|0
Can Verdaguer|Can Verdaguer||Alt Penedès|CAT|41.473858|1.636633|0
Can Via|Can Via||Baix Llobregat|CAT|41.365127|2.009185|0
Can Vidal|Can Vidal||Baix Llobregat|CAT|41.40463|1.974408|0
Can Vila|Can Vila||Vallès Oriental|CAT|41.675364|2.409724|0
Can Vila|Can Vila||Vallès Oriental|CAT|41.680072|2.414628|0
Can Vila-Rosal|Can Vila-Rosal||Vallès Oriental|CAT|41.580383|2.207523|0
Can Vilalba|Can Vilalba||Baix Llobregat|CAT|41.52328|1.923876|0
Can Vilardebò|Can Vilardebò||Vallès Oriental|CAT|41.599174|2.231072|0
Can Vilardell|Can Vilardell||Maresme|CAT|41.567279|2.425335|0
Can Villar|Can Villar||Maresme|CAT|41.598476|2.613753|0
Can Vinardell|Can Vinardell||Pla de l'Estany|CAT|42.071544|2.803127|0
Can Vinyals|Can Vinyals||Vallès Occidental|CAT|41.622023|2.118593|0
Can Vinyers|Can Vinyers||Vallès Occidental|CAT|41.599214|2.03428|0
Can Volard|Can Volard||Vallès Oriental|CAT|41.562422|2.242553|0
Can Xacó|Can Xacó||Alt Penedès|CAT|41.347634|1.849113|0
Can Xamaio|Can Xamaio||Alt Penedès|CAT|41.472857|1.655484|0
Can Xamaio|Can Xamaio||Alt Penedès|CAT|41.46885|1.6555|0
Can Xel|Can Xel||Garrotxa|CAT|42.148304|2.531407|0
Can Xesc|Can Xesc||Bages|CAT|41.66301|1.835812|0
Can Xicota|Can Xicota||Vallès Oriental|CAT|41.614835|2.258009|0
Can Xiquet Pou|Can Xiquet Pou||Eivissa|IB|38.983847|1.529169|0
Can Xoles del Palà|Can Xoles del Palà||Vallès Occidental|CAT|41.604428|1.876662|0
Can Xombo|Can Xombo||Alt Penedès|CAT|41.483255|1.602478|0
Canadà Parc|Canadà Parc||Vallès Oriental|CAT|41.649835|2.455372|0
Canalda|Canalda||Solsonès|CAT|42.122813|1.438938|0
Canales|Canales||Alt Palància|PV|39.853379|-0.760382|0
Canaletes|Canaletes||Anoia|CAT|41.476507|1.705713|0
Canals|Canals||Costera|PV|38.949858|-0.585919|1
Canapost|Canapost||Baix Empordà|CAT|41.975111|3.071913|0
Canavelles|Canaveilles||el Conflent|CN|42.534464|2.223447|1
Canejan|Canejan||la Vall d'Aran|CAT|42.816201|0.796325|1
Canelles|Canelles||Alt Empordà|CAT|42.194766|2.882339|0
Canet d'Adri|Canet d'Adri||Gironès|CAT|42.05618|2.724461|1
Canet d'en Berenguer|Canet d'en Berenguer||Camp de Morvedre|PV|39.680706|-0.216354|1
Canet de Fals|Canet de Fals||Bages|CAT|41.759787|1.739076|0
Canet de la Tallada|Canet de la Tallada||Baix Empordà|CAT|42.052727|3.067012|0
Canet de Mar|Canet de Mar||Maresme|CAT|41.598616|2.576462|1
Canet de Rosselló|Canet-en-Roussillon||el Rosselló|CN|42.683882|3.010807|1
Canet lo Roig|Canet lo Roig||Baix Maestrat|PV|40.561591|0.233551|1
Canillo|Canillo|||AD|42.5676|1.59756|0
Canonge|Canonge||Maresme|CAT|41.48658|2.298945|0
Canovelles|Canovelles||Vallès Oriental|CAT|41.626565|2.269578|1
Cantallops|Cantallops||Alt Empordà|CAT|42.41818|2.928167|1
Cantallops|Cantallops||Alt Penedès|CAT|41.366493|1.800399|0
Cantallops|Cantallops||Vallès Oriental|CAT|41.581071|2.233373|0
Cantallops|Cantallops||Alt Penedès|CAT|41.372923|1.803327|0
Cantallops|Cantallops||Baix Empordà|CAT|41.923294|3.180527|0
Cantaperdius|Cantaperdius||Noguera|CAT|41.838346|0.821686|0
Cantonigròs|Cantonigròs||Osona|CAT|42.040722|2.404284|0
Canyada de Guaita|Canyada de Guaita||Horta Sud|PV|39.362327|-0.472301|0
Canyamars|Canyamars||Maresme|CAT|41.603956|2.437804|0
Canyamel Coves|Canyamel Coves||Mallorca|IB|39.66443|3.440019|0
Canyamel Platja|Canyamel Platja||Mallorca|IB|39.675497|3.43427|0
Canyelles|Canyelles||Garraf|CAT|41.283514|1.721001|1
Canyelles|Canyelles||Selva|CAT|41.7101|2.892294|0
Canyelles-Almadrava-puig-rom|Canyelles-Almadrava-puig-rom||Alt Empordà|CAT|42.245825|3.199936|0
Cap Blanc|Cap Blanc||Marina Alta|PV|38.678148|0.11232|0
Cap Blanc|Cap Blanc||Baix Maestrat|PV|40.341694|0.379501|0
Cap d'Artrutx|Cap d'Artrutx||Menorca|IB|39.927837|3.825131|0
Cap d'en Font|Cap d'en Font||Menorca|IB|39.829468|4.207167|0
Cap Martí|Cap Martí||Marina Alta|PV|38.750158|0.20136|0
Cap sa Sal|Cap sa Sal||Baix Empordà|CAT|41.966747|3.230771|0
Cap Salou|Cap Salou||Tarragonès|CAT|41.059852|1.167841|0
Cap-Blanc|Cap-Blanc||Ribera Baixa|PV|39.18292|-0.226869|0
Cap-Roig|Cap-Roig||Baix Ebre|CAT|40.821742|0.731023|0
Capafonts|Capafonts||Baix Camp|CAT|41.292919|1.040432|1
Capblanc|Capblanc||Marina Baixa|PV|38.586872|-0.060507|0
Capdepera|Capdepera||Mallorca|IB|39.697072|3.427357|1
Capdevila|Capdevila||Cerdanya|CAT|42.36619|1.839496|0
Capellades|Capellades||Anoia|CAT|41.529287|1.688115|1
Capicorb|Capicorb||Baix Maestrat|PV|40.206687|0.258946|0
Capmany|Capmany||Alt Empordà|CAT|42.371458|2.907674|1
Capolat|Capolat||Berguedà|CAT|42.087265|1.751402|1
Capçanes|Capçanes||Priorat|CAT|41.095034|0.79943|1
Caramany|Caramany||la Fenolleda|CN|42.736753|2.559979|1
Carcaixent|Carcaixent||Ribera Alta|PV|39.09839|-0.427594|1
Cardedeu|Cardedeu||Vallès Oriental|CAT|41.645957|2.355126|1
Cardet|Cardet||Alta Ribagorça|CAT|42.498045|0.785472|0
Cardona|Cardona||Bages|CAT|41.907039|1.660043|1
Caregue|Caregue||Pallars Sobirà|CAT|42.469726|1.104526|0
Carlet|Carlet||Ribera Alta|PV|39.234655|-0.536197|1
Carme|Carme||Anoia|CAT|41.533905|1.624537|1
Carmeniu|Carmeniu||Alt Urgell|CAT|42.368638|1.337195|0
Carpesa|Carpesa|||PV|39.516794|-0.378086|0
Carrascalet|Carrascalet||Ribera Alta|PV|39.200357|-0.441555|0
Carrer Montseny|Carrer Montseny||Selva|CAT|41.74995|2.620262|0
Carrers d'Orbeta|Carrers d'Orbeta||Marina Alta|PV|38.778956|-0.059057|0
Carretera Aeroport|Carretera Aeroport||Selva|CAT|41.893841|2.767251|0
Carretera d'Igualada|Carretera d'Igualada||Bages|CAT|41.717811|1.819224|0
Carretera de l'Albir|Carretera de l'Albir||Marina Baixa|PV|38.58048|-0.064831|0
Carretera de Rafal|Carretera de Rafal||Baix Segura|PV|38.117151|-0.868417|0
Carretera del Reguers|Carretera del Reguers||Baix Ebre|CAT|40.823564|0.473685|0
Carretera Vendrell|Carretera Vendrell||Baix Penedès|CAT|41.226836|1.539178|0
Carrícola|Carrícola||Vall d'Albaida|PV|38.843421|-0.473166|1
Cas Capità|Cas Capità||Mallorca|IB|39.611058|2.716542|0
Cas Català-Ses Illetes|Cas Català-Ses Illetes||Mallorca|IB|39.544529|2.586009|0
Cas Concos|Cas Concos||Mallorca|IB|39.419621|3.134954|0
Cas Corb|Cas Corb||Eivissa|IB|38.934484|1.418751|0
Cas Miot-Pont d'Inca Park|Cas Miot-Pont d'Inca Park||Mallorca|IB|39.607959|2.704887|0
Casa Fus|Casa Fus||Alacantí|PV|38.403753|-0.456406|0
Casa Groga|Casa Groga||Alacantí|PV|38.416657|-0.531982|0
Casa Nova|Casa Nova||Vallès Oriental|CAT|41.543535|2.290944|0
Casablanca|Casablanca||Alt Penedès|CAT|41.422845|1.835831|0
Casablanca|Casablanca||Camp de Túria|PV|39.611387|-0.542821|0
Casafabre|Casefabre||el Rosselló|CN|42.617797|2.614933|1
Casarilh|Casarilh||la Vall d'Aran|CAT|42.699483|0.831235|0
Casas de Bàrcena|Casas de Bàrcena||València|PV|39.522454|-0.357517|0
Casas de Cabanes y las Fuentes|Casas de Cabanes y las Fuentes||Alt Vinalopó|PV|38.654705|-0.884266|0
Casas de Cuadra|Casas de Cuadra||Plana d'Utiel|PV|39.45201|-1.255008|0
Casas de Eufemia|Casas de Eufemia||Plana d'Utiel|PV|39.449096|-1.211501|0
Casas de Ibáñez|Casas de Ibáñez||Vinalopó Mitjà|PV|38.406516|-1.086753|0
Casas de Jordán|Casas de Jordán||Alt Vinalopó|PV|38.61474|-0.909715|0
Casas de Madrona|Casas de Madrona||Vall de Cofrents|PV|38.977181|-1.075658|0
Casas de Moya|Casas de Moya||Plana d'Utiel|PV|39.460115|-1.392191|0
Casas de Pradas|Casas de Pradas||Plana d'Utiel|PV|39.448023|-1.339593|0
Casas de Sotos|Casas de Sotos||Plana d'Utiel|PV|39.33187|-1.064601|0
Casas de Terol|Casas de Terol||Alacantí|PV|38.40694|-0.593162|0
Casas de Vidal|Casas de Vidal||Vall d'Albaida|PV|38.78257|-0.775788|0
Casas del Rey|Casas del Rey||Plana d'Utiel|PV|39.475457|-1.381026|0
Casas del Río|Casas del Río||Plana d'Utiel|PV|39.296417|-1.133646|0
Casau|Casau||Aran|CAT|42.705914|0.785355|0
Casavells|Casavells||Baix Empordà|CAT|42.000245|3.03171|0
Cascanar|Cascanar||Mallorca|IB|39.638826|2.920269|0
Cascante|Cascante||Alt Vinalopó|PV|38.659766|-0.8533|0
Caselles Davall|Caselles Davall||Pla de l'Estany|CAT|42.144987|2.734551|0
Caseres|Caseres||Terra Alta|CAT|41.042662|0.281661|1
Caserras del Castillo|Caserras del Castillo||la Ribagorça|FR|42.034997|0.556761|0
Cases Altes|Casas Altas||Racó d'Ademús|PV|40.033204|-1.249766|1
Cases Baixes|Casas Bajas||Racó d'Ademús|PV|40.014411|-1.261126|1
Cases de Sanchis|Cases de Sanchis||Vinalopó Mitjà|PV|38.410075|-0.929342|0
Cases del Salt|Cases del Salt||Alcoià|PV|38.68805|-0.509109|0
Cases del Senyor|Cases del Senyor||Vinalopó Mitjà|PV|38.406029|-0.953448|0
Casetes de Joan Blanco|Casetes de Joan Blanco||Vinalopó Mitjà|PV|38.43969|-0.95232|0
Casetes de la Pedrera|Casetes de la Pedrera||Vall d'Albaida|PV|38.830129|-0.509236|0
Casetes de Meleja|Casetes de Meleja||Baix Vinalopó|PV|38.20549|-0.54809|0
Casetes Julià|Casetes Julià||Baix Llobregat|CAT|41.37748|1.910615|0
Casetes Muntaner|Casetes Muntaner||Baix Llobregat|CAT|41.379113|1.917752|0
Casicas|Casicas||Baix Vinalopó|PV|38.165825|-0.815818|0
Casinos|Casinos||Camp de Túria|PV|39.703488|-0.74353|1
Cassanyes|Cassagnes||la Fenolleda|CN|42.743877|2.62444|1
Casserres|Casserres||Berguedà|CAT|42.017577|1.841085|1
Cassibrós|Cassibrós||Pallars Sobirà|CAT|42.574321|1.232914|0
Cassovall|Cassovall||Alt Urgell|CAT|42.352975|1.31008|0
Cassà de la Selva|Cassà de la Selva||Gironès|CAT|41.879521|2.888076|1
Cassà de Pelràs|Cassà de Pelràs||Baix Empordà|CAT|42.008799|2.99585|0
Castalla|Castalla||Alcoià|PV|38.567969|-0.670612|1
Castanesa|Castanesa||la Ribagorça|FR|42.497804|0.661846|0
Castarné|Castarné||la Ribagorça|FR|42.441373|0.692829|0
Castell|Casteil||el Conflent|CN|42.505967|2.41959|1
Castell d'Aro|Castell d'Aro||Baix Empordà|CAT|41.814381|3.032172|0
Castell d'Aro, Platja d'Aro i s'Agaró|Castell d'Aro, Platja d'Aro i s'Agaró||Baix Empordà|CAT|41.821419|3.046532|1
Castell d'Empordà|Castell d'Empordà||Baix Empordà|CAT|41.978727|3.044019|0
Castell de Cabrera|Castell de Cabrera||Anoia|CAT|41.504546|1.695046|0
Castell de Cabres|Castell de Cabres||Baix Maestrat|PV|40.669784|0.042767|1
Castell de Castells|Castell de Castells||Marina Alta|PV|38.735025|-0.187067|1
Castell de l'Areny|Castell de l'Areny||Berguedà|CAT|42.177544|1.945091|1
Castell de Mur|Castell de Mur||Pallars Jussà|CAT|42.098265|0.850496|1
Castell Madeleine|Castell Madeleine||Baix Empordà|CAT|41.833503|3.09072|0
Castell-Montbui|Castell-Montbui||Vallès Oriental|CAT|41.665175|2.180636|0
Castelladral|Castelladral||Bages|CAT|41.89883|1.77906|0
Castellar|Castellar||Bages|CAT|41.72862|1.654029|0
Castellar d'Índies|Castellar d'Índies||Maresme|CAT|41.639412|2.607788|0
Castellar de la Ribera|Castellar de la Ribera||Solsonès|CAT|42.012209|1.404059|1
Castellar de n'Hug|Castellar de n'Hug||Berguedà|CAT|42.282186|1.999357|1
Castellar del Riu|Castellar del Riu||Berguedà|CAT|42.123729|1.758519|1
Castellar del Vallès|Castellar del Vallès||Vallès Occidental|CAT|41.622547|2.074325|1
Castellar-Oliveral|Castellar-Oliveral|||PV|39.42981|-0.363928|0
Castellarnau|Castellarnau||Vallès Occidental|CAT|41.557247|2.076549|0
Castellbell i el Vilar|Castellbell i el Vilar||Bages|CAT|41.638752|1.853355|1
Castellbisbal|Castellbisbal||Vallès Occidental|CAT|41.482795|1.968186|1
Castellcir|Castellcir||Moianès|CAT|41.772193|2.132586|1
Castellciutat|Castellciutat||Alt Urgell|CAT|42.355035|1.443231|0
Castelldans|Castelldans||Garrigues|CAT|41.49264|0.760792|1
Castelldefels|Castelldefels||Baix Llobregat|CAT|41.276581|1.970533|1
Castellet|Castellet||Alt Penedès|CAT|41.262816|1.633396|0
Castellet i la Gornal|Castellet i la Gornal||Alt Penedès|CAT|41.267572|1.63382|1
Castellfabib|Castielfabib||Racó d'Ademús|PV|40.128527|-1.335775|1
Castellfollit de la Roca|Castellfollit de la Roca||Garrotxa|CAT|42.21869|2.543123|1
Castellfollit de Riubregós|Castellfollit de Riubregós||Anoia|CAT|41.771006|1.4358|1
Castellfollit del Boix|Castellfollit del Boix||Bages|CAT|41.669487|1.677257|1
Castellfort|Castellfort||Ports|PV|40.506184|-0.199442|1
Castellgalí|Castellgalí||Bages|CAT|41.677413|1.827219|1
Castellmoster|Castellmoster||Baix Camp|CAT|41.194333|1.097608|0
Castellmoster|Castellmoster||Baix Camp|CAT|41.191913|1.102836|0
Castellnou|Castelnou||el Rosselló|CN|42.619725|2.709371|1
Castellnou|Castellnovo||Alt Palància|PV|39.854422|-0.448571|1
Castellnou d'Oluges|Castellnou d'Oluges||Segarra|CAT|41.703578|1.297661|0
Castellnou d'Ossó|Castellnou d'Ossó||Urgell|CAT|41.767948|1.144424|0
Castellnou de Bages|Castellnou de Bages||Bages|CAT|41.832841|1.818132|1
Castellnou de Bassella|Castellnou de Bassella||Alt Urgell|CAT|41.997018|1.286602|0
Castellnou de Carcolze|Castellnou de Carcolze||Alt Urgell|CAT|42.387185|1.582081|0
Castellnou de Montsec|Castellnou de Montsec||Pallars Jussà|CAT|42.102176|0.764938|0
Castellnou de Seana|Castellnou de Seana||Pla d'Urgell|CAT|41.652441|0.970899|1
Castellolí|Castellolí||Anoia|CAT|41.596297|1.703913|1
Castellonet de la Conquesta|Castellonet de la Conquesta||Safor|PV|38.906648|-0.264104|1
Castellonroi|Castillonroy||la Llitera|FR|41.879221|0.542062|1
Castellserà|Castellserà||Urgell|CAT|41.750747|0.990023|1
Castellterçol|Castellterçol||Moianès|CAT|41.751684|2.102719|1
Castellvell del Camp|Castellvell del Camp||Baix Camp|CAT|41.188743|1.090957|1
Castellví de la Marca|Castellví de la Marca||Alt Penedès|CAT|41.334297|1.603756|1
Castellví de Rosanes|Castellví de Rosanes||Baix Llobregat|CAT|41.448137|1.917135|1
Castellàs|Castellàs||Alt Urgell|CAT|42.329216|1.230953|0
Castelló|Castelló||Ribera Alta|PV|39.073377|-0.520292|1
Castelló d'Empúries|Castelló d'Empúries||Alt Empordà|CAT|42.247622|3.09771|1
Castelló de Farfanya|Castelló de Farfanya||Noguera|CAT|41.813764|0.717364|1
Castelló de la Plana|Castelló de la Plana||Plana Alta|PV|39.997252|-0.042997|1
Castelló de Rugat|Castelló de Rugat||Vall d'Albaida|PV|38.879736|-0.386339|1
Castigaleu|Castigaleu||la Ribagorça|FR|42.20671|0.575548|1
Castilblanques|Castilblanques||Vall de Cofrents|PV|39.320094|-1.016463|0
Castillo del Pla|Castillo del Pla||la Ribagorça|FR|42.027926|0.454229|0
Catadau|Catadau||Ribera Alta|PV|39.255361|-0.608456|1
Catalònia|Catalònia||Baix Empordà|CAT|41.974491|3.203993|0
Catamarruc|Catamarruc||Comtat|PV|38.782021|-0.321766|0
Catarroja|Catarroja||Horta Sud|PV|39.396371|-0.394684|1
Catllà|Catllar||el Conflent|CN|42.640751|2.422647|1
Catral|Catral||Baix Segura|PV|38.152346|-0.797577|1
Catí|Catí||Alt Maestrat|PV|40.474533|0.019963|1
Caudeli|Caudeli||Safor|PV|39.008192|-0.201954|0
Cauders de Conflent|Caudiès-de-Conflent||el Conflent|CN|42.563792|2.143427|1
Cauders de Fenollet|Caudiès-de-Fenouillèdes||la Fenolleda|CN|42.819407|2.377385|1
Caudete de las Fuentes|Caudete de las Fuentes||Plana d'Utiel|PV|39.545482|-1.291434|1
Caudiel|Caudiel||Alt Palància|PV|39.98707|-0.572103|1
Cava|Cava||Alt Urgell|CAT|42.315373|1.613523|1
Cavall Bernat|Cavall Bernat||Vallès Occidental|CAT|41.623799|2.011808|0
Cañada de Práez|Cañada de Práez||Baix Segura|PV|37.886236|-0.832169|0""",
        """Cañada del Charco|Cañada del Charco||Foia de Bunyol|PV|39.456657|-0.679607|0
Cañada del Trigo|Cañada del Trigo||Vinalopó Mitjà|PV|38.357855|-1.083443|0
Cañada Menescal|Cañada Menescal||Foia de Bunyol|PV|39.413999|-0.643288|0
Cañada Terrero|Cañada Terrero||Foia de Bunyol|PV|39.413648|-0.661163|0
Cedramán|Cedramán||Alt Millars|PV|40.157637|-0.385763|0
Cellers|Cellers||Solsonès|CAT|41.811681|1.476385|0
Cellers|Cellers||Pallars Jussà|CAT|42.067981|0.881168|0
Celrà|Celrà||Gironès|CAT|42.017887|2.878012|1
Centelles|Centelles||Osona|CAT|41.785387|2.221933|1
Centenys|Centenys||Pla de l'Estany|CAT|42.159697|2.786483|0
Centernac|Saint-Arnac||la Fenolleda|CN|42.776753|2.535572|1
Centre Penitenciari|Centre Penitenciari||Horta Sud|PV|39.314598|-0.436097|0
Cerbi|Cerbi||Pallars Sobirà|CAT|42.647173|1.148881|0
Cerc|Cerc||Alt Urgell|CAT|42.339459|1.493585|0
Cercs|Cercs||Berguedà|CAT|42.139443|1.850096|1
Cercèdol|Cercèdol||Alt Urgell|CAT|42.359424|1.367831|0
Cerdanyola del Vallès|Cerdanyola del Vallès||Vallès Occidental|CAT|41.480606|2.122739|1
Cerdà|Cerdà||Costera|PV|38.985664|-0.595921|1
Cereja|Cereja||Cerdanya|CAT|42.476509|1.968916|0
Ceret|Céret||el Vallespir|CN|42.474298|2.752603|1
Cervelló|Cervelló||Baix Llobregat|CAT|41.392834|1.942028|1
Cervera|Cervera||Segarra|CAT|41.679873|1.281953|1
Cervera de la Marenda|Cerbère||el Rosselló|CN|42.444358|3.147665|1
Cervera del Maestrat|Cervera del Maestre||Baix Maestrat|PV|40.443441|0.28231|1
Cervià de les Garrigues|Cervià de les Garrigues||Garrigues|CAT|41.436255|0.840878|1
Cervià de Ter|Cervià de Ter||Gironès|CAT|42.066703|2.908663|1
Cesalpina|Cesalpina||Baix Llobregat|CAT|41.359833|2.003932|0
Cesalpina|Cesalpina||Baix Llobregat|CAT|41.361936|1.991394|0
Cims de València|Cims de València||Costera|PV|38.902332|-0.796386|0
Cinc Estrelles|Cinc Estrelles||Tarragonès|CAT|41.179615|1.282461|0
Cinctorres|Cinctorres||Ports|PV|40.576223|-0.219605|1
Cirat|Cirat||Alt Millars|PV|40.051165|-0.455652|1
Ciscar|Ciscar||la Ribagorça|FR|42.076367|0.549323|0
Cistella|Cistella||Alt Empordà|CAT|42.280376|2.830633|1
Ciudad Jardín|Ciudad Jardín||Baix Segura|PV|38.095218|-0.957557|0
Ciutadella|Ciutadella||Menorca|IB|40.004386|3.846543|0
Ciutadella de Menorca|Ciutadella de Menorca||Menorca|IB|39.994752|3.893941|1
Ciutadilla|Ciutadilla||Urgell|CAT|41.54303|1.14541|1
Ciutat Jardí del Camí de Mura|Ciutat Jardí del Camí de Mura||Camp de Túria|PV|39.629374|-0.585079|0
Civit|Civit||Segarra|CAT|41.577439|1.380435|0
Civís|Civís||Alt Urgell|CAT|42.464758|1.423143|0
Clairà|Claira||el Rosselló|CN|42.763893|2.944973|1
Claravalls|Claravalls||Urgell|CAT|41.703335|1.124992|0
Claret|Claret||Solsonès|CAT|41.820403|1.464272|0
Clariana|Clariana||Alt Penedès|CAT|41.362058|1.783457|0
Clariana|Clariana||Alt Penedès|CAT|41.244544|1.616531|0
Clariana de Cardener|Clariana de Cardener||Solsonès|CAT|41.947237|1.599046|1
Claverol|Claverol||Pallars Jussà|CAT|42.244864|0.986636|0
Clerà i Villerac|Clara-Villerach||el Conflent|CN|42.584379|2.450903|1
Clot de Navarrete|Clot de Navarrete||Camp de Túria|PV|39.550925|-0.527156|0
Clot del Oliver|Clot del Oliver||Camp de Morvedre|PV|39.659183|-0.348586|0
Clotxa del Sec|Clotxa del Sec||Camp de Túria|PV|39.594986|-0.50811|0
Club Mont-roig|Club Mont-roig||Baix Camp|CAT|41.068597|0.975467|0
Club Segre|Club Segre||Noguera|CAT|41.836829|0.822392|0
Cocentaina|Cocentaina||Comtat|PV|38.7375|-0.44689|1
Codalet|Codalet||el Conflent|CN|42.599739|2.415317|1
Cofrents|Cofrentes||Vall de Cofrents|PV|39.254869|-1.08063|1
Cogullada|Cogullada||Ribera Alta|PV|39.110383|-0.462611|0
Coix|Cox||Baix Segura|PV|38.143774|-0.899943|1
Colera|Colera||Alt Empordà|CAT|42.402648|3.123526|1
Colinas Venta Cabrera Derecha|Colinas Venta Cabrera Derecha||Ribera Alta|PV|39.367664|-0.576834|0
Colinas Venta Cabrera Izquierda|Colinas Venta Cabrera Izquierda||Ribera Alta|PV|39.369593|-0.584851|0
Coll D'Arboç|Coll D'Arboç||Bages|CAT|41.680359|1.788979|0
Coll de Nargó|Coll de Nargó||Alt Urgell|CAT|42.157493|1.241776|1
Coll de Tapioles|Coll de Tapioles||Tarragonès|CAT|41.172619|1.302825|0
Coll del Llop|Coll del Llop||Selva|CAT|41.705433|2.874887|0
Collbató|Collbató||Baix Llobregat|CAT|41.57311|1.827619|1
Colldejou|Colldejou||Baix Camp|CAT|41.100276|0.87332|1
Colldelrat|Colldelrat||Noguera|CAT|41.899667|1.09231|0
Collformic|Collformic||Selva|CAT|41.751013|2.662214|0
Collfred|Collfred||Noguera|CAT|41.916076|1.113602|0
Collmorter|Collmorter||Pallars Jussà|CAT|42.102704|0.862985|0
Colls Majors|Colls Majors||Tarragonès|CAT|41.130107|1.297014|0
Collsacreu|Collsacreu||Maresme|CAT|41.630834|2.531315|0
Collsacreu|Collsacreu||Vallès Oriental|CAT|41.63167|2.528313|0
Collsuspina|Collsuspina||Moianès|CAT|41.831192|2.174305|1
Coloma|Coloma||Marina Baixa|PV|38.580059|-0.133663|0
Colomers|Colomers||Baix Empordà|CAT|42.084078|2.988653|1
Colomina|Colomina||Baix Empordà|CAT|41.930941|3.172357|0
Colonia Argentina|Colonia Argentina||Maresme|CAT|41.467038|2.27899|0
Colonia de la Hispanidad|Colonia de la Hispanidad||Alt Vinalopó|PV|38.512107|-0.88314|0
Colonia Gasol|Colonia Gasol||Baix Ebre|CAT|40.865763|0.522708|0
Colonia la Marinense|Colonia la Marinense||Camp de Túria|PV|39.73978|-0.530574|0
Colonia Rosal|Colonia Rosal||Berguedà|CAT|42.075061|1.87048|0
Colònia Canyada del Primal|Colònia Canyada del Primal||Ribera Alta|PV|39.388846|-0.591052|0
Colònia de Sant Jordi|Colònia de Sant Jordi||Mallorca|IB|39.319759|2.994161|0
Colònia de Sant Pere|Colònia de Sant Pere||Mallorca|IB|39.736831|3.280195|0
Colònia Fàbrica|Colònia Fàbrica||Ribera d'Ebre|CAT|41.227766|0.538636|0
Colònia la Fàbrica|Colònia la Fàbrica||Noguera|CAT|41.910504|1.075244|0
Colònia Motor del Vicari|Colònia Motor del Vicari||Ribera Alta|PV|39.380411|-0.604872|0
Colònia Santa Anna|Colònia Santa Anna||Ribera Alta|PV|39.288769|-0.463922|0
Comabella|Comabella||Segarra|CAT|41.749254|1.345998|0
Comellarets|Comellarets||Ribera d'Ebre|CAT|41.224875|0.540607|0
Comelles II|Comelles II||Vallès Occidental|CAT|41.605333|1.880234|0
Cometa Orbeta|Cometa Orbeta||Marina Alta|PV|38.788821|0.051477|0
Complejo Residencial de Vera|Complejo Residencial de Vera||Horta Nord|PV|39.48854|-0.326463|0
Comte de Sert|Comte de Sert||Vallès Occidental|CAT|41.483737|1.994356|0
Conat|Conat||el Conflent|CN|42.616917|2.338454|1
Conca de Dalt|Conca de Dalt||Pallars Jussà|CAT|42.24086|1.009445|1
Concabella|Concabella||Segarra|CAT|41.751367|1.234575|0
Condado de Jaruco|Condado de Jaruco||Selva|CAT|41.709903|2.824199|0
Conesa|Conesa||Conca de Barberà|CAT|41.514729|1.280218|1
Confrides|Confrides||Marina Baixa|PV|38.679212|-0.276635|1
Conques|Conques||Pallars Jussà|CAT|42.118769|1.013055|0
Consell|Consell||Mallorca|IB|39.665029|2.817936|1
Constantí|Constantí||Tarragonès|CAT|41.161223|1.191728|1
Contrast|Contrast||Anoia|CAT|41.581006|1.433828|0
Contreras|Contreras||Plana d'Utiel|PV|39.537628|-1.498442|0
Copons|Copons||Anoia|CAT|41.64298|1.50508|1
Coratxà|Coratxà||Baix Maestrat|PV|40.689892|0.084829|0
Corbera|Corbera||Ribera Baixa|PV|39.159025|-0.352535|1
Corbera d'Ebre|Corbera d'Ebre||Terra Alta|CAT|41.098695|0.475709|1
Corbera de les Cabanes|Corbère-les-Cabanes||el Rosselló|CN|42.660067|2.682603|1
Corbera de Llobregat|Corbera de Llobregat||Baix Llobregat|CAT|41.424433|1.930335|1
Corbera del Castell|Corbère||el Rosselló|CN|42.651432|2.66192|1
Corbins|Corbins||Segrià|CAT|41.686296|0.67971|1
Corcolilla|Corcolilla||Serrans|PV|39.929197|-1.011834|0
Cornellana|Cornellana||Alt Urgell|CAT|42.260039|1.538479|0
Cornellà de Conflent|Corneilla-de-Conflent||el Conflent|CN|42.575987|2.382522|1
Cornellà de la Ribera|Corneilla-la-Rivière||el Rosselló|CN|42.709725|2.72556|1
Cornellà de Llobregat|Cornellà de Llobregat||Baix Llobregat|CAT|41.355271|2.076379|1
Cornellà del Bèrcol|Corneilla-del-Vercol||el Rosselló|CN|42.622817|2.95178|1
Cornellà del Terri|Cornellà del Terri||Pla de l'Estany|CAT|42.090599|2.824137|1
Cornudella|Cornudella||Baix Penedès|CAT|41.299822|1.569843|0
Cornudella de Montsant|Cornudella de Montsant||Priorat|CAT|41.268964|0.917935|1
Corral d'en Cona|Corral d'en Cona||Garraf|CAT|41.220461|1.670122|0
Corral d'en Tort|Corral d'en Tort||Garraf|CAT|41.22695|1.673233|0
Corral del Moliner|Corral del Moliner||Safor|PV|38.983229|-0.244145|0
Correntilla|Correntilla||Plana Alta|PV|40.053409|-0.14562|0
Correntías Altas|Correntías Altas||Baix Segura|PV|38.076673|-0.928746|0
Correntías Medias|Correntías Medias||Baix Segura|PV|38.074616|-0.923399|0
Corroncui|Corroncui||Alta Ribagorça|CAT|42.333878|0.852712|0
Corró d'Amunt|Corró d'Amunt||Vallès Oriental|CAT|41.667928|2.326865|0
Corró d'Avall|Corró d'Avall||Vallès Oriental|CAT|41.632538|2.298603|0
Corsaví|Corsavy||el Vallespir|CN|42.482625|2.540531|1
Cortes d'Arenós|Cortes de Arenoso||Alt Millars|PV|40.171539|-0.528926|1
Cortes de Pallars|Cortes de Pallás||Vall de Cofrents|PV|39.249339|-0.938654|1
Cortichelles|Cortichelles||Ribera Alta|PV|39.399239|-0.617055|0
Corts|Corts||Pla de l'Estany|CAT|42.095165|2.788497|0
Cortàs|Cortàs||Cerdanya|CAT|42.39807|1.801284|0
Corçà|Corçà||Baix Empordà|CAT|41.994977|3.017501|1
Corçà|Corçà||Noguera|CAT|42.029737|0.685879|0
Coscó|Coscó||Noguera|CAT|41.821789|1.162741|0
Costa Bella|Costa Bella||Baix Segura|PV|38.126083|-0.649923|0
Costa d'en Blanes|Costa d'en Blanes||Mallorca|IB|39.540449|2.561046|0
Costa d'en Gallina|Costa d'en Gallina||Selva|CAT|41.694009|2.813737|0
Costa de la Calma|Costa de la Calma||Mallorca|IB|39.523809|2.47934|0
Costa de la Toia|Costa de la Toia||Tarragonès|CAT|41.20026|1.266655|0
Costa des Pins|Costa des Pins||Mallorca|IB|39.637796|3.413689|0
Costa Nova|Costa Nova||Marina Alta|PV|38.740112|0.209204|0
Costa Zèfir|Costa Zèfir||Baix Camp|CAT|41.022579|0.93988|0
Costablanca|Costablanca||Vallès Occidental|CAT|41.479219|1.94595|0
Costitx|Costitx||Mallorca|IB|39.655431|2.949319|1
Costoja|Coustouges||el Vallespir|CN|42.356384|2.638119|1
Costur|Costur||Alcalatén|PV|40.121713|-0.162372|1
Cotes|Cotes||Ribera Alta|PV|39.058458|-0.600881|1
Cotlliure|Collioure||el Rosselló|CN|42.513022|3.072659|1
Cotoveta|Cotoveta||Alacantí|PV|38.436447|-0.427447|0
Covatelles|Covatelles||Marina Alta|PV|38.756476|0.139801|0
Coves de Boronat|Coves de Boronat||Alacantí|PV|38.420557|-0.520937|0
Coves Llorito|Coves Llorito||Tarragonès|CAT|41.137681|1.263518|0
Covet|Covet||Pallars Jussà|CAT|42.087485|1.070714|0
Creixell|Creixell||Tarragonès|CAT|41.181985|1.432171|1
Creixell Mar|Creixell Mar||Tarragonès|CAT|41.165514|1.445096|0
Crespià|Crespià||Pla de l'Estany|CAT|42.192931|2.799429|1
Crevillent|Crevillent||Baix Vinalopó|PV|38.234639|-0.814323|1
Cruz de Galindo|Cruz de Galindo||Baix Segura|PV|38.098947|-0.798114|0
Cruïlles, Monells i Sant Sadurní de l'Heura|Cruïlles, Monells i Sant Sadurní de l'Heura||Baix Empordà|CAT|41.923637|2.981888|1
Cuatro Vientos|Cuatro Vientos||Camp de Túria|PV|39.569095|-0.550932|0
Cuatrocorz|Cuatrocorz||la Llitera|FR|41.957179|0.40701|0
Cubelles|Cubelles||Garraf|CAT|41.217587|1.660353|1
Cubells|Cubells||Noguera|CAT|41.837869|0.961176|1
Cuc|Cuc||Selva|CAT|41.958944|2.631648|0
Cuells|Cuells||Baix Empordà|CAT|42.035837|2.993075|0
Cuesta del Rato|Cuesta del Rato||Racó d'Ademús|PV|40.146594|-1.313234|0
Cuesta del Sastre|Cuesta del Sastre||Foia de Bunyol|PV|39.414261|-0.650581|0
Culebrón|Culebrón||Vinalopó Mitjà|PV|38.421886|-0.996673|0
Culla|Culla||Alt Maestrat|PV|40.308186|-0.123482|1
Cullera|Cullera||Ribera Baixa|PV|39.162894|-0.259418|1
Cunit|Cunit||Baix Penedès|CAT|41.209561|1.630294|1
Cussons|Cussons||Osona|CAT|42.096825|2.195854|0
Càlig|Càlig||Baix Maestrat|PV|40.467837|0.352474|1
Cànoes|Canohès||el Rosselló|CN|42.651806|2.834501|1
Cànoves|Cànoves||Vallès Oriental|CAT|41.694218|2.350353|0
Cànoves i Samalús|Cànoves i Samalús||Vallès Oriental|CAT|41.702739|2.337676|1
Càrcer|Càrcer||Ribera Alta|PV|39.060001|-0.572075|1
Cóll|Cóll||Alta Ribagorça|CAT|42.472856|0.771092|0
Daia Nova|Daya Nueva||Baix Segura|PV|38.109841|-0.762915|1
Daia Vella|Daya Vieja||Baix Segura|PV|38.105747|-0.735062|1
Daimús|Daimús||Safor|PV|38.97201|-0.150535|1
Dalt Sa Rapita|Dalt Sa Rapita||Mallorca|IB|39.371862|2.943631|0
Daltmar|Daltmar||Garraf|CAT|41.293244|1.703963|0
Daltmar|Daltmar||Alt Penedès|CAT|41.292728|1.696461|0
Darmós|Darmós||Ribera d'Ebre|CAT|41.097677|0.705719|0
Darnius|Darnius||Alt Empordà|CAT|42.363724|2.829184|1
Das|Das||Cerdanya|CAT|42.362405|1.867048|1
Dehesa de Campoamor|Dehesa de Campoamor||Baix Segura|PV|37.902456|-0.75344|0
Deià|Deià||Mallorca|IB|39.748588|2.648449|1
Delicies del Realon|Delicies del Realon||Horta Sud|PV|39.389387|-0.477535|0
Deltebre|Deltebre||Baix Ebre|CAT|40.735868|0.744907|1
Denúy|Denúy||la Ribagorça|FR|42.497016|0.633213|0
Deuloféu|Deuloféu||Cerdanya|CAT|42.439406|1.929408|0
Devesa|Devesa||Marina Baixa|PV|38.585374|-0.102861|0
Deveses|Deveses||Marina Alta|PV|38.873134|-0.000212|0
Diamant del Vallès|Diamant del Vallès||Vallès Oriental|CAT|41.679809|2.241967|0
Diana|Diana||Gironès|CAT|42.085003|2.952928|0
Dolors|Dolores||Baix Segura|PV|38.138893|-0.754923|1
Domeny|Domeny||Gironès|CAT|41.992228|2.789873|0
Domenyo|Domeño||Serrans|PV|39.700883|-0.924887|1
Donzell d'Urgell|Donzell d'Urgell||Urgell|CAT|41.83507|1.093551|0
Dorres|Dorres||l'Alta Cerdanya|CN|42.51356|1.927062|1
Dos Cales|Dos Cales||Baix Empordà|CAT|41.942075|3.212215|0
Dosaigües|Dos Aguas||Foia de Bunyol|PV|39.287147|-0.774329|1
Dosquers|Dosquers||Garrotxa|CAT|42.198592|2.759356|0
Dosrius|Dosrius||Maresme|CAT|41.60433|2.439651|1
Drac Parc|Drac Parc||Vallès Occidental|CAT|41.609225|2.013038|0
Ducat del Montseny|Ducat del Montseny||Selva|CAT|41.733643|2.586425|0
Duesaigües|Duesaigües||Baix Camp|CAT|41.15993|0.924975|1
Durro|Durro||Alta Ribagorça|CAT|42.498607|0.821712|0
Durro i Saraís|Durro i Saraís||Alta Ribagorça|CAT|42.535134|0.85689|0
Dusfort|Dusfort||Anoia|CAT|41.738414|1.497735|0
Dénia|Dénia||Marina Alta|PV|38.82427|0.079662|1
Dòrria|Dòrria||Ripollès|CAT|42.332887|2.066927|0
Edeta|Edeta||Camp de Túria|PV|39.658691|-0.575012|0
Eina|Eyne||l'Alta Cerdanya|CN|42.454799|2.108479|1
Eivissa|Eivissa||Eivissa|IB|38.913677|1.419108|1
Eixample|Eixample||Maresme|CAT|41.488809|2.293044|0
El Arenal|El Arenal||Baix Segura|PV|38.070805|-0.992961|0
El Azagador|El Azagador||Plana d'Utiel|PV|39.485116|-1.141398|0
el Bacarot|el Bacarot|||PV|38.311562|-0.544929|0
El Badén|El Badén||Baix Segura|PV|38.105722|-0.858784|0
el Baell|el Baell||Ripollès|CAT|42.278367|2.146593|0
El Baladre|El Baladre||Camp de Morvedre|PV|39.670304|-0.238344|0
El Balcó|El Balcó||Plana Alta|PV|40.083208|0.129547|0
el Balcó de Sant Llorenç|el Balcó de Sant Llorenç||Vallès Occidental|CAT|41.63617|2.075496|0
el Balcó del Pirineu|el Balcó del Pirineu||Alt Urgell|CAT|42.344318|1.434901|0
el Balneari de Vallfogona|el Balneari de Vallfogona||Conca de Barberà|CAT|41.563656|1.258098|0
El Balís|El Balís||Maresme|CAT|41.57204|2.498224|0
el Barcarès|Le Barcarès||el Rosselló|CN|42.812531|3.027884|1
El Barranc|El Barranc||Alacantí|PV|38.432458|-0.42441|0
El Barranco E-2|El Barranco E-2||Baix Segura|PV|37.944994|-0.753696|0
El Barranquet|El Barranquet||Alacantí|PV|38.415887|-0.507582|0
El Barrio-Sallavedra|El Barrio-Sallavedra||Baix Segura|PV|38.093913|-0.851108|0
el Bedorc|el Bedorc||Anoia|CAT|41.497648|1.721097|0
El Bellotero|El Bellotero||Foia de Bunyol|PV|39.41668|-0.645024|0
el Benafelí|el Benafelí||Plana Alta|PV|39.93871|-0.0216|0
El Bonaire|El Bonaire||Vallès Oriental|CAT|41.656399|2.236481|0
el Bonrepòs|el Bonrepòs||Baix Llobregat|CAT|41.424884|1.91931|0
el Borràs|el Borràs||Bages|CAT|41.630871|1.856464|0
el Bosc de l'Àliga|el Bosc de l'Àliga||Anoia|CAT|41.545203|1.748508|0""",
        """El Bosc de la Batllòria|El Bosc de la Batllòria||Selva|CAT|41.724579|2.549212|0
El Bosque|El Bosque||Foia de Bunyol|PV|39.443495|-0.618083|0
el Bosquet|el Bosquet||Alt Camp|CAT|41.279995|1.081614|0
El Boter Sector XXX-La Vall del Sol|El Boter Sector XXX-La Vall del Sol||Alacantí|PV|38.448217|-0.501021|0
El Brosquil|El Brosquil||Ribera Baixa|PV|39.119623|-0.242884|0
el Bruc|el Bruc||Anoia|CAT|41.609906|1.763502|1
el Bruc Residencial|el Bruc Residencial||Anoia|CAT|41.572025|1.799909|0
el Brull|el Brull||Osona|CAT|41.804053|2.316689|1
el Burés|el Burés||Bages|CAT|41.639193|1.856199|0
el Cabeç Bord|el Cabeç Bord||Horta Nord|PV|39.624878|-0.370911|0
El Calbet|El Calbet||Bages|CAT|41.684542|1.78175|0
El Camino de Cartagena|El Camino de Cartagena||Baix Segura|PV|38.069142|-0.949965|0
el Camp de les Comes|el Camp de les Comes||Gironès|CAT|42.032341|2.822919|0
el Camp de Mirra|el Camp de Mirra/Campo de Mirra||Alt Vinalopó|PV|38.703685|-0.801993|1
el Campell|Alcampell||la Llitera|FR|41.878888|0.435794|1
el Campell|el Campell||Marina Alta|PV|38.779558|-0.102715|0
el Campello|el Campello||Alacantí|PV|38.462145|-0.372938|1
El Campo|El Campo||Baix Segura|PV|38.054896|-0.682015|0
El Camí dels Molins|El Camí dels Molins||Baix Penedès|CAT|41.22971|1.534689|0
el Canalís de Crespo|el Canalís de Crespo|||CX|38.469203|-1.309809|0
el Cantó|el Cantó|El Cantón||CX|38.301167|-0.981955|0
El Canyadell|El Canyadell||Maresme|CAT|41.572415|2.533171|0
el Canós|el Canós||Segarra|CAT|41.688423|1.20466|0
el Cap de l'Horta|el Cap de l'Horta||Camp de Túria|PV|39.58077|-0.537368|0
El Captivador|El Captivador||Marina Baixa|PV|38.602683|-0.10805|0
el Capçó|el Capçó||Marina Alta|PV|38.786245|-0.060557|0
El Caramello|El Caramello||Camp de Túria|PV|39.644957|-0.584165|0
el Carrascalejo|el Carrascalejo|||CX|38.606373|-1.150803|0
El Carrer de Baix i la Plana de Can Periquet|El Carrer de Baix i la Plana de Can Periquet||Vallès Occidental|CAT|41.575212|2.179589|0
el Carrer de Dalt, la Pineda i la Plana de l'Alzina|el Carrer de Dalt, la Pineda i la Plana de l'Alzina||Vallès Occidental|CAT|41.59474|2.176041|0
el Carrer del Bosc|el Carrer del Bosc||Alt Penedès|CAT|41.450573|1.728117|0
El Casalot|El Casalot||Baix Camp|CAT|41.026684|0.92323|0
el Castell|el Castell||Maresme|CAT|41.493932|2.319076|0
El Castell|El Castell||Cerdanya|CAT|42.421851|1.882635|0
el Castell|el Castell||Montsià|CAT|40.608765|0.348573|0
el Castell de Guadalest|el Castell de Guadalest||Marina Baixa|PV|38.670006|-0.16847|1
el Castell de la Ventosa|el Castell de la Ventosa||Anoia|CAT|41.503381|1.730379|0
el Castell de Santa Maria|el Castell de Santa Maria||Segarra|CAT|41.681153|1.405097|0
el Castell de Vilamalefa|Castillo de Villamalefa||Alt Millars|PV|40.145919|-0.36706|1
El Castell i la Tenda Nova|El Castell i la Tenda Nova||Vallès Occidental|CAT|41.578185|2.184077|0
El Castellet|El Castellet||Vallès Occidental|CAT|41.525163|2.080032|0
el Castellot|el Castellot||Tarragonès|CAT|41.156677|1.362488|0
el Catllar|el Catllar||Tarragonès|CAT|41.178357|1.306955|1
El Cerrao|El Cerrao||Camp de Túria|PV|39.616396|-0.520304|0
El Clos|El Clos||Selva|CAT|41.799253|2.580641|0
el Codís|el Codís||Pla d'Urgell|CAT|41.629102|0.905142|0
el Cogul|el Cogul||Garrigues|CAT|41.457151|0.69288|1
el Colador|el Colador||Plana Baixa|PV|39.974632|-0.248169|0
el Coll de la Barraca|el Coll de la Barraca||Alt Penedès|CAT|41.442968|1.572833|0
el Coll de la Rualda|el Coll de la Rualda||Alt Maestrat|PV|40.362343|-0.127819|0
El Collado|El Collado||Serrans|PV|39.959873|-1.040915|0
El Collado|El Collado||Horta Sud|PV|39.51339|-0.50874|0
el Collado dels Gabriels|el Collado dels Gabriels|||CX|38.254955|-1.051719|0
El Collao|El Collao||Alacantí|PV|38.416998|-0.483857|0
El Collet|El Collet||Baix Empordà|CAT|41.851206|3.110127|0
El Corral de la Marquesa|El Corral de la Marquesa||Camp de Túria|PV|39.642318|-0.577878|0
El Cortal|El Cortal||Ripollès|CAT|42.24651|2.079796|0
El Cortès|El Cortès||Bages|CAT|41.796853|1.781562|0
El Coscojar|El Coscojar||Foia de Bunyol|PV|39.413793|-0.670636|0
El Cònsul|El Cònsul||Maresme|CAT|41.575854|2.526117|0
El Cós|El Cós||Garrotxa|CAT|42.221621|2.552436|0
El Derramador|El Derramador||Vinalopó Mitjà|PV|38.434771|-0.941032|0
El Derramador|El Derramador||Plana d'Utiel|PV|39.494587|-1.153355|0
El Descanso|El Descanso||Camp de Morvedre|PV|39.730809|-0.383001|0
el Despujol|el Despujol||Osona|CAT|42.018228|2.243645|0
el Dosser|el Dosser||Ribera Baixa|PV|39.192359|-0.225339|0
El Empalme|El Empalme||Plana Alta|PV|40.14011|0.152022|0
El Escorratel|El Escorratel||Baix Segura|PV|38.101344|-0.92079|0
el Far d'Empordà|el Far d'Empordà||Alt Empordà|CAT|42.245642|2.993075|1
El Farell|El Farell||Maresme|CAT|41.603042|2.597169|0
El Figuerar|El Figuerar||Baix Empordà|CAT|41.867123|3.129795|0
el Fonduco|el Fonduco||Menorca|IB|39.885643|4.279537|0
el Fondó|el Fondó||Vinalopó Mitjà|PV|38.439654|-0.917457|0
el Fondó de les Neus|el Fondó de les Neus / Hondón de las Nieves||Vinalopó Mitjà|PV|38.307763|-0.895061|1
el Fondó dels Frares|Hondón de los Frailes||Vinalopó Mitjà|PV|38.268956|-0.932234|1
el Forcall|el Forcall||Ports|PV|40.657166|-0.212336|1
el Forn|el Forn|||AD|42.5676|1.59756|0
el Francasset|el Francasset||Tarragonès|CAT|41.18081|1.481015|0
el Francàs|el Francàs||Baix Penedès|CAT|41.177892|1.500419|0
El Fresno|El Fresno||Vallès Occidental|CAT|41.613861|1.892223|0
El Fusteret|El Fusteret||Bages|CAT|41.817667|1.761541|0
el Galobard|el Galobard||Bages|CAT|41.763523|1.906397|0
El Gantxo|El Gantxo||Alacantí|PV|38.421722|-0.545454|0
el Genovés|el Genovés||Costera|PV|38.985918|-0.465063|1
el Ges|el Ges||Alt Urgell|CAT|42.316777|1.49757|0
el Golf de Sant Julià|el Golf de Sant Julià||Gironès|CAT|42.032264|2.808899|0
el Gos|el Gos||Noguera|CAT|41.886128|1.133758|0
El Graell|El Graell||Bages|CAT|41.675687|1.774086|0
El Grandet|El Grandet||Alt Penedès|CAT|41.474023|1.794028|0
el Grau de Moncofa|el Grau de Moncofa||Plana Baixa|PV|39.799413|-0.132611|0
El Guarrat|El Guarrat||Foia de Bunyol|PV|39.442751|-0.654398|0
el Guix|el Guix||Bages|CAT|41.743085|1.849554|0
el Guixaró|el Guixaró||Berguedà|CAT|41.996932|1.88647|0
El Gurri|El Gurri||Osona|CAT|41.881104|2.259798|0
El Hontanar|El Hontanar||Serrans|PV|39.94306|-1.021732|0
El Llac|El Llac||Bages|CAT|41.760945|1.908894|0
el Llac del Cigne|el Llac del Cigne||Selva|CAT|41.839181|2.784715|0
El Lledoner|El Lledoner||Baix Llobregat|CAT|41.38458|1.878686|0
El Lligallo del Gànguil|El Lligallo del Gànguil||Baix Ebre|CAT|40.760229|0.650314|0
el Lligallo del Roig|el Lligallo del Roig||Baix Ebre|CAT|40.764024|0.650891|0
el Lloar|el Lloar||Priorat|CAT|41.192395|0.74071|1
el Llor|el Llor||Segarra|CAT|41.746395|1.309711|0
El Mallol|El Mallol||Garrotxa|CAT|42.149094|2.43881|0
El Maltemps|El Maltemps||Maresme|CAT|41.581306|2.55561|0
El Mansegal-Vereda de Buenavida|El Mansegal-Vereda de Buenavida||Baix Segura|PV|38.058722|-0.970192|0
el Manyar|el Manyar||Vinalopó Mitjà|PV|38.430143|-0.933721|0
el Mar de Riudecanyes|el Mar de Riudecanyes||Baix Camp|CAT|41.132479|0.946102|0
el Mareny de les Barraquetes|el Mareny de les Barraquetes||Ribera Baixa|PV|39.246042|-0.26528|0
el Mareny de Sant Llorenç|el Mareny de Sant Llorenç||Ribera Baixa|PV|39.219482|-0.254211|0
el Mareny de Vilxes|el Mareny de Vilxes||Ribera Baixa|PV|39.221867|-0.254952|0
el Marenyet|el Marenyet||Ribera Baixa|PV|39.14767|-0.240286|0
El Mas Carbó|El Mas Carbó||Maresme|CAT|41.679386|2.731765|0
El Mas Corsini|El Mas Corsini||Tarragonès|CAT|41.130733|1.251717|0
el Mas d'Avall|el Mas d'Avall||Alcalatén|PV|40.128396|-0.171142|0
el Mas d'en Bosc|el Mas d'en Bosc||Alt Camp|CAT|41.30836|1.410096|0
el Mas d'en Gall|el Mas d'en Gall||Baix Llobregat|CAT|41.532206|1.838072|0
el Mas d'en Serra|el Mas d'en Serra||Garraf|CAT|41.229269|1.754955|0
el Mas de Blanc|el Mas de Blanc||Tarragonès|CAT|41.169536|1.311364|0
el Mas de Cargol|el Mas de Cargol||Tarragonès|CAT|41.173898|1.284113|0
el Mas de Cosme|el Mas de Cosme||Tarragonès|CAT|41.189971|1.293784|0
el Mas de Gerembí|el Mas de Gerembí||Tarragonès|CAT|41.195618|1.294649|0
el Mas de Moregons|el Mas de Moregons||Tarragonès|CAT|41.164997|1.276646|0
el Mas de Panxer|el Mas de Panxer||Tarragonès|CAT|41.147266|1.284928|0
el Mas de Pavia|el Mas de Pavia||Ribera Alta|PV|39.401916|-0.599304|0
El Mas del Frare|El Mas del Frare||Tarragonès|CAT|41.15706|1.257251|0
el Mas Falquet|el Mas Falquet||Baix Empordà|CAT|41.884972|3.119654|0
El Mas Flassià|El Mas Flassià||Selva|CAT|41.791652|2.771571|0
el Mas Gassons|el Mas Gassons||Alt Camp|CAT|41.411698|1.479963|0
el Mas Gordi|el Mas Gordi||Vallès Oriental|CAT|41.591816|2.226976|0
el Mas Mates|el Mas Mates||Alt Empordà|CAT|42.271735|3.165119|0
El Mas Morer|El Mas Morer||Alt Penedès|CAT|41.402686|1.709983|0
el Mas Móra|el Mas Móra||Maresme|CAT|41.714632|2.74528|0
el Mas Pau|el Mas Pau||Alt Empordà|CAT|42.238808|2.906413|0
El Mas Reixac|El Mas Reixac||Maresme|CAT|41.684666|2.721254|0
el Mas Reixac|el Mas Reixac||Maresme|CAT|41.687104|2.718221|0
el Mas Roig|el Mas Roig||Tarragonès|CAT|41.179433|1.475646|0
el Mas Tomasí|el Mas Tomasí||Baix Empordà|CAT|41.979158|3.168883|0
el Mas Vermell|el Mas Vermell||Alt Camp|CAT|41.41694|1.470403|0
El Maset|El Maset||Alt Penedès|CAT|41.436904|1.85783|0
El Maset|El Maset||Anoia|CAT|41.489162|1.799516|0
El Maset dels Cosins|El Maset dels Cosins||Alt Penedès|CAT|41.332427|1.59607|0
el Masnou|el Masnou||Maresme|CAT|41.482494|2.318575|1
el Masroig|el Masroig||Priorat|CAT|41.132132|0.732099|1
el Milà|el Milà||Alt Camp|CAT|41.251388|1.206221|1
El Miracle|El Miracle||Tarragonès|CAT|41.115975|1.270762|0
El Mirador|El Mirador||Baix Llobregat|CAT|41.387123|1.954493|0
El Mirador|El Mirador||Baix Llobregat|CAT|41.391555|1.925747|0
El Mirador de Montserrat|El Mirador de Montserrat||Bages|CAT|41.788448|1.824665|0
el Mirador del Penedès|el Mirador del Penedès||Baix Penedès|CAT|41.298612|1.429399|0
El Mojón|El Mojón||Baix Segura|PV|37.851881|-0.767585|0
el Molar|el Molar||Priorat|CAT|41.164964|0.706824|1
El Molí|El Molí||Vallès Oriental|CAT|41.651187|2.385292|0
El Molí|El Molí||Selva|CAT|41.770552|2.740734|0
el Molí d'Avall|el Molí d'Avall||Baix Camp|CAT|41.069024|1.063117|0
El Molí d'en Serra|El Molí d'en Serra||Baix Penedès|CAT|41.240394|1.546145|0
el Monte Blanco|el Monte Blanco||Plana Baixa|PV|39.962273|-0.249906|0
El Montemayor|El Montemayor||Horta Sud|PV|39.513014|-0.521867|0
el Montmell|el Montmell||Baix Penedès|CAT|41.331522|1.487024|1
el Montnegre-Cabeçó d'Or|el Montnegre-Cabeçó d'Or|||PV|38.382222|-0.537562|0
el Moralet|el Moralet|||PV|38.382222|-0.537562|0
el Morell|el Morell||Tarragonès|CAT|41.200793|1.215769|1
el Morer|el Morer||Maresme|CAT|41.607177|2.637928|0
El Morrón|El Morrón||Alt Vinalopó|PV|38.672124|-0.880882|0
El Mudamiento|El Mudamiento||Baix Segura|PV|38.118527|-0.835942|0
El Mujal|El Mujal||Bages|CAT|41.891491|1.853659|0
El Murtal|El Murtal||Camp de Morvedre|PV|39.681202|-0.366048|0
el Mèdol|el Mèdol||Tarragonès|CAT|41.173801|1.280808|0
El Oro|El Oro||Vall de Cofrents|PV|39.29918|-0.919392|0
El Pago|El Pago||Alt Penedès|CAT|41.384828|1.812174|0
el Palamó|el Palamó||Alacantí|PV|38.390602|-0.484715|0
El Palasiet|El Palasiet||Plana Alta|PV|40.057891|0.008757|0
el Palau d'Anglesola|el Palau d'Anglesola||Pla d'Urgell|CAT|41.649651|0.862291|1
El Palmar|El Palmar|||PV|39.31128|-0.317709|0
el Palomar|el Palomar||Vall d'Albaida|PV|38.853297|-0.490844|1
el Palà de Torroella|el Palà de Torroella||Bages|CAT|41.854469|1.720211|0
El Panorama I|El Panorama I||Safor|PV|38.917362|-0.139997|0
El Pany|El Pany||Alt Penedès|CAT|41.398595|1.545412|0
el Papagai|el Papagai||Baix Penedès|CAT|41.294167|1.517317|0
el Papagai|el Papagai||Baix Penedès|CAT|41.29441|1.520269|0
el Papiol|el Papiol||Baix Llobregat|CAT|41.442182|2.011913|1
el Papiolet|el Papiolet||Baix Penedès|CAT|41.281915|1.528614|0
el Paradon|el Paradon||Vinalopó Mitjà|PV|38.444992|-1.056556|0
El Paradís|El Paradís||Baix Camp|CAT|41.053934|1.012804|0
El Parc de Mont-roig|El Parc de Mont-roig||Baix Camp|CAT|41.023195|0.957307|0
el Pas de la Casa|el Pas de la Casa|||AD|42.53474|1.58014|0
El Passeig Marítim|El Passeig Marítim||Garraf|CAT|41.264102|1.937618|0
El Pasteral|El Pasteral||Selva|CAT|41.985998|2.60943|0
El Perellonet|El Perellonet|||PV|39.302692|-0.291182|0
el Perelló|el Perelló||Baix Ebre|CAT|40.887492|0.683824|1
el Perelló|el Perelló||Gironès|CAT|41.953219|2.796281|0
el Perelló|el Perelló||Ribera Baixa|PV|39.275094|-0.277058|0
El Perer|El Perer||Vallès Oriental|CAT|41.696314|2.176337|0
el Pertegàs|el Pertegàs||Vallès Oriental|CAT|41.710612|2.465264|0
el Pertús|Le Perthus||el Vallespir|CN|42.465673|2.859106|1
el Peucal|el Peucal||Camp de Túria|PV|39.666687|-0.424683|0
El Pi|El Pi||Alacantí|PV|38.396076|-0.457406|0
el Pi de Sant Just|el Pi de Sant Just||Solsonès|CAT|41.977733|1.555083|0
el Picarany|el Picarany||Baix Camp|CAT|41.199606|1.093568|0
el Pilar de la Foradada|Pilar de la Horadada||Baix Segura|PV|37.89987|-0.829575|1
el Pilar de la Mola|el Pilar de la Mola||Formentera|IB|38.669477|1.550331|0
El Pinar|El Pinar||Vallès Oriental|CAT|41.62814|2.237578|0
el Pinar|el Pinar||Baix Camp|CAT|41.173527|1.102174|0
El Pinar|El Pinar||Baix Maestrat|PV|40.265174|0.275308|0
El Pinar IV Fase|El Pinar IV Fase||Baix Camp|CAT|41.176079|1.100742|0
el Pinell de Brai|el Pinell de Brai||Terra Alta|CAT|41.018644|0.501692|1
el Pinetell|el Pinetell||Conca de Barberà|CAT|41.312535|1.126022|0
El Pintat|El Pintat||Alacantí|PV|38.404857|-0.572912|0
El Pinyot|El Pinyot||Bages|CAT|41.686641|1.790219|0
el Pinós|el Pinós/Pinoso||Vinalopó Mitjà|PV|38.412291|-1.029059|1
El Pla|El Pla||Vallès Oriental|CAT|41.62352|2.238445|0
El Pla|El Pla||Cerdanya|CAT|42.368341|1.845704|0
El Pla|El Pla||Alacantí|PV|38.389806|-0.582289|0
El Pla|El Pla||Comtat|PV|38.809092|-0.414265|0
el Pla de Baix|el Pla de Baix||Gironès|CAT|42.023623|2.825134|0
el Pla de la Font|el Pla de la Font|Pla de la Font|Segrià|CAT|41.686953|0.353658|0
El Pla de la Torre|El Pla de la Torre||Anoia|CAT|41.540375|1.683981|0
el Pla de la Vall-llonga|el Pla de la Vall-llonga|||PV|38.382222|-0.537562|0
el Pla de les Botges|el Pla de les Botges||Bages|CAT|41.641577|1.807821|0
el Pla de Manlleu|el Pla de Manlleu||Alt Camp|CAT|41.367574|1.501154|0
el Pla de Montsó|el Pla de Montsó||Segrià|CAT|41.652983|0.559769|0
El Pla de Sant Joan|El Pla de Sant Joan||Gironès|CAT|42.01993|2.680701|0
el Pla de Sant Llorenç|el Pla de Sant Llorenç||Vallès Occidental|CAT|41.617019|2.015039|0
el Pla de Sant Tirs|el Pla de Sant Tirs||Alt Urgell|CAT|42.316158|1.384638|0
el Pla de Santa Maria|el Pla de Santa Maria||Alt Camp|CAT|41.346889|1.311258|1
el Pla del Badó|el Pla del Badó||Moianès|CAT|41.723112|2.154853|0
El Pla del Bon Aire|El Pla del Bon Aire||Vallès Occidental|CAT|41.590051|2.019615|0
el Pla del Penedès|el Pla del Penedès||Alt Penedès|CAT|41.414703|1.721347|1
el Pla dels Olivars|el Pla dels Olivars||Plana Baixa|PV|39.996931|-0.192077|0
el Poal|el Poal||Pla d'Urgell|CAT|41.675666|0.871368|1
el Poble Nou de Benitatxell|el Poble Nou de Benitatxell/Benitachell||Marina Alta|PV|38.724636|0.148644|1
El Poble-sec|El Poble-sec||Osona|CAT|42.009321|2.24426|0
El Poble-sec|El Poble-sec||Vallès Occidental|CAT|41.530063|2.096349|0
El Poble-sec|El Poble-sec||Alt Urgell|CAT|42.361767|1.467009|0
El Poblenou|El Poblenou||Maresme|CAT|41.619985|2.672464|0
el Poblenou|el Poblenou||Vallès Occidental|CAT|41.559709|2.12502|0
El Poblenou|El Poblenou||Alt Empordà|CAT|42.290759|2.911932|0
el Poblenou del Delta|el Poblenou del Delta||Montsià|CAT|40.645219|0.686753|0
el Pont d'Alentorn|el Pont d'Alentorn||Noguera|CAT|41.908265|1.069768|0
el Pont d'Armentera|el Pont d'Armentera||Alt Camp|CAT|41.40671|1.340026|1
el Pont de Bar|el Pont de Bar||Alt Urgell|CAT|42.379539|1.614053|1
el Pont de Cabrianes|el Pont de Cabrianes||Bages|CAT|41.772435|1.902733|0
el Pont de Claverol|el Pont de Claverol||Pallars Jussà|CAT|42.243139|0.971477|0
el Pont de Montanyana|Puente de Montañana||la Ribagorça|FR|42.169231|0.669363|1
el Pont de Suert|el Pont de Suert||Alta Ribagorça|CAT|42.393987|0.800379|1
El Pont de Vilomara|El Pont de Vilomara||Bages|CAT|41.701167|1.874416|0
el Pont de Vilomara i Rocafort|el Pont de Vilomara i Rocafort||Bages|CAT|41.704135|1.906987|1
el Pont del Príncep|el Pont del Príncep||Alt Empordà|CAT|42.226775|2.96776|0
El Pontón|El Pontón||Plana d'Utiel|PV|39.476035|-1.120132|0
el Port|el Port||Alt Empordà|CAT|42.369979|3.160517|0
el Port|el Port||Mallorca|IB|39.78908|2.693629|0""",
        """el Port de la Selva|el Port de la Selva||Alt Empordà|CAT|42.324857|3.216341|1
el Port de Sagunt|el Port de Sagunt||Camp de Morvedre|PV|39.660566|-0.215318|0
el Port del Comte|el Port del Comte||Solsonès|CAT|42.173697|1.565867|0
El Portell|El Portell||Anoia|CAT|41.469355|1.792944|0
El Portús|El Portús||Alt Empordà|CAT|42.461517|2.864733|0
el Pou d'en Calbo|el Pou d'en Calbo||Alcalatén|PV|40.154089|-0.10853|0
El Pouet|El Pouet||Camp de Túria|PV|39.614416|-0.529342|0
el Prat de Calafell|el Prat de Calafell||Baix Penedès|CAT|41.189472|1.573256|0
el Prat de Llobregat|el Prat de Llobregat||Baix Llobregat|CAT|41.310022|2.098794|1
el Prat Roig|el Prat Roig||Pla de l'Estany|CAT|42.106357|2.75002|0
el Priorat|el Priorat||Alt Empordà|CAT|42.351201|2.980379|0
el Priorat de Banyeres|el Priorat de Banyeres||Baix Penedès|CAT|41.274048|1.548191|0
el Priorat de Banyeres|el Priorat de Banyeres||Baix Penedès|CAT|41.275323|1.542193|0
el Priorat de la Bisbal|el Priorat de la Bisbal||Baix Penedès|CAT|41.286576|1.472166|0
El Puig|El Puig||Alt Penedès|CAT|41.435999|1.847336|0
El Puig|El Puig||Ripollès|CAT|42.322294|2.104046|0
el Puig|el Puig||Baix Camp|CAT|41.183012|1.08321|0
el Puig de Sant Ramon|el Puig de Sant Ramon||Baix Empordà|CAT|41.966148|3.045888|0
el Puig de Santa Maria|el Puig de Santa Maria||Horta Nord|PV|39.598971|-0.320527|1
El Pujolet|El Pujolet||Baix Llobregat|CAT|41.571787|1.824056|0
El Pujolet|El Pujolet||Alt Penedès|CAT|41.399527|1.707408|0
el Quartico|el Quartico||Plana Alta|PV|40.189565|0.012852|0
El Quemado|El Quemado||Foia de Bunyol|PV|39.441679|-0.688808|0
el Querforadat|el Querforadat||Alt Urgell|CAT|42.323272|1.635946|0
El Raco|El Raco||Baix Empordà|CAT|41.977414|3.206158|0
el Racó|el Racó||Vallès Occidental|CAT|41.623935|2.060114|0
el Racó de Nadal|el Racó de Nadal||Marina Alta|PV|38.739676|0.119626|0
el Racó del Bosc|el Racó del Bosc||Vallès Oriental|CAT|41.688673|2.186318|0
el Racó del Bosc|el Racó del Bosc||Vallès Oriental|CAT|41.689281|2.183425|0
el Racó del Cèsar|el Racó del Cèsar||Tarragonès|CAT|41.167587|1.431935|0
El Raiguero|El Raiguero||Baix Segura|PV|38.081904|-0.792565|0
El Raiguero de la Matanza|El Raiguero de la Matanza||Baix Segura|PV|38.124142|-1.007219|0
el Raspai|el Raspai|Raspay||CX|38.443225|-1.090024|0
El Raval|El Raval||Bages|CAT|41.700504|1.966223|0
El Raval d'Aguilera|El Raval d'Aguilera||Anoia|CAT|41.612024|1.655377|0
el Raval d'Aleny|el Raval d'Aleny||Anoia|CAT|41.747706|1.516291|0
el Raval de Baix|el Raval de Baix||Baix Empordà|CAT|41.877959|3.127191|0
El Raval de Sant Feliu|El Raval de Sant Feliu||Lluçanès|CAT|41.94454|2.029961|0
El Raval del Pom|El Raval del Pom||Baix Ebre|CAT|40.756379|0.568836|0
el Raval Mas|el Raval Mas||Baix Llobregat|CAT|41.355363|1.967301|0
el Real de Gandia|el Real de Gandia||Safor|PV|38.947859|-0.203146|1
El Realengo|El Realengo||Baix Vinalopó|PV|38.201424|-0.811647|0
El Realengo|El Realengo||Costera|PV|39.058341|-0.421824|0
El Rebalso|El Rebalso||Vinalopó Mitjà|PV|38.291422|-0.90845|0
El Rebollar|El Rebollar||Plana d'Utiel|PV|39.468364|-1.011071|0
el Rebolledo|el Rebolledo|||PV|38.341969|-0.613791|0
el Refugi|el Refugi||Plana Alta|PV|40.063581|0.015325|0
El Regalón|El Regalón||Camp de Túria|PV|39.632609|-0.575274|0
El Rellano|El Rellano||Foia de Bunyol|PV|39.409417|-0.640608|0
El Remei|El Remei||Cerdanya|CAT|42.419837|1.890245|0
El Respirall|El Respirall||Ribera Alta|PV|39.151369|-0.406599|0
el Rieral|el Rieral||Vallès Oriental|CAT|41.740667|2.407723|0
El Rieral|El Rieral||Vallès Oriental|CAT|41.637938|2.231504|0
el Riurau|el Riurau||Ribera Alta|PV|39.057134|-0.450075|0
el Roc de Sant Gaietà|el Roc de Sant Gaietà||Tarragonès|CAT|41.168309|1.480965|0
El Rodeno|El Rodeno||Camp de Morvedre|PV|39.678072|-0.389418|0
el Rodolat|el Rodolat||Tarragonès|CAT|41.148552|1.26639|0
El Rodriguillo|El Rodriguillo||Vinalopó Mitjà|PV|38.367498|-1.046779|0
El Romaní|El Romaní||Ribera Baixa|PV|39.30262|-0.396541|0
el Roquer|el Roquer||Baix Ebre|CAT|40.784759|0.681682|0
el Rourell|el Rourell||Alt Camp|CAT|41.222345|1.220218|1
el Ràfol d'Almúnia|el Ràfol d'Almúnia||Marina Alta|PV|38.830798|-0.050922|1
el Ràfol de Salem|Ráfol de Salem||Vall d'Albaida|PV|38.864657|-0.408777|1
El Saladar|El Saladar||Baix Segura|PV|38.123959|-0.80065|0
El Saler|El Saler|||PV|39.382888|-0.332844|0
El Sallent|El Sallent||Garrotxa|CAT|42.154139|2.614666|0
El Salse|El Salse||Alt Vinalopó|PV|38.706828|-0.736822|0
El Sanatori|El Sanatori||Baix Penedès|CAT|41.186496|1.56118|0
El Senyal|El Senyal||Alacantí|PV|38.41907|-0.457359|0
El Serradet|El Serradet||Osona|CAT|42.100267|2.202842|0
el Serrat|el Serrat|||AD|42.55623|1.53319|0
El Serrat|El Serrat||Vallès Oriental|CAT|41.653857|2.20767|0
el Serrat de Castellnou|el Serrat de Castellnou||Bages|CAT|41.800655|1.826644|0
El Serrat de l'Ocata|El Serrat de l'Ocata||Vallès Oriental|CAT|41.688195|2.246092|0
el Solei dels Herbatges|el Solei dels Herbatges||Baix Llobregat|CAT|41.420217|1.906918|0
el Soler|Le Soler||el Rosselló|CN|42.677544|2.795013|1
El Soler|El Soler||Anoia|CAT|41.748689|1.464735|0
el Soleràs|el Soleràs||Garrigues|CAT|41.42057|0.699187|1
el Solà del Boix|el Solà del Boix||Moianès|CAT|41.768897|2.147012|0
El Solà-Ventolà|El Solà-Ventolà||Ripollès|CAT|42.317905|2.135442|0
el Sot dels Nostris|el Sot dels Nostris||Vallès Oriental|CAT|41.592391|2.236795|0
El Taió|El Taió||Baix Llobregat|CAT|41.451145|1.907164|0
el Talladell|el Talladell|Talladell|Urgell|CAT|41.649854|1.166469|0
el Tarròs|el Tarròs||Urgell|CAT|41.707094|1.033081|0
el Tarter|el Tarter|||AD|42.5676|1.59756|0
el Tec|Le Tech||el Vallespir|CN|42.441864|2.51291|1
El Terme|El Terme||Baix Empordà|CAT|41.923544|3.168874|0
El Tormo|El Tormo||Alt Millars|PV|40.063771|-0.428849|0
el Torn|el Torn||Garrotxa|CAT|42.160475|2.645363|0
el Toro|el Toro||Alt Palància|PV|39.96077|-0.77436|1
el Toro|el Toro||Mallorca|IB|39.48426|2.479288|0
El Torrentfondo|El Torrentfondo||Alt Penedès|CAT|41.473157|1.80952|0
El Torretxo|El Torretxo||Ribera Alta|PV|39.158434|-0.412294|0
el Torricó|Altorricón||la Llitera|FR|41.784337|0.412962|1
El Tosal|El Tosal||Safor|PV|38.937065|-0.209239|0
el Tossal|el Tossal||Noguera|CAT|41.915131|1.150361|0
el Tossal de les Forques|el Tossal de les Forques||Noguera|CAT|41.813556|0.876235|0
El Tossalet|El Tossalet||Marina Baixa|PV|38.579546|-0.082465|0
el Tossalet|el Tossalet||Marina Alta|PV|38.753703|0.193639|0
el Tossalet|el Tossalet||Plana Baixa|PV|39.958699|-0.254424|0
el Tremat|el Tremat|||AD|42.53474|1.58014|0
El Turó|El Turó||Vallès Oriental|CAT|41.676299|2.222391|0
El Turó|El Turó||Mallorca|IB|39.569825|2.721591|0
el Vedat|el Vedat||Horta Sud|PV|39.418286|-0.486808|0
el Vendrell|el Vendrell||Baix Penedès|CAT|41.205069|1.519959|1
El Ventorrillo|El Ventorrillo||Plana Alta|PV|40.164164|0.163488|0
el Verdegàs|el Verdegàs|||PV|38.402918|-0.583844|0
el Verger|el Verger||Marina Alta|PV|38.850604|-0.004756|1
El Veïnat de Dalt|El Veïnat de Dalt||Alt Empordà|CAT|42.331105|3.095951|0
El Vicenç|El Vicenç||Osona|CAT|41.995519|2.295965|0
el Vilar|el Vilar|||AD|42.5676|1.59756|0
el Vilar|el Vilar||Bages|CAT|41.632437|1.85768|0
el Vilar d'Aro|el Vilar d'Aro||Baix Empordà|CAT|41.798306|3.037152|0
El Vilar d'Urtx|El Vilar d'Urtx||Cerdanya|CAT|42.384643|1.902794|0
el Vilar de Cabó|el Vilar de Cabó||Alt Urgell|CAT|42.235142|1.270466|0
el Vilarenc|el Vilarenc||Baix Penedès|CAT|41.192112|1.57167|0
el Vilet|el Vilet||Urgell|CAT|41.55705|1.070386|0
el Villar|Villar del Arzobispo||Serrans|PV|39.707451|-0.817077|1
el Vilosell|el Vilosell||Garrigues|CAT|41.386524|0.951461|1
el Vilà|el Vilà||Mallorca|IB|39.902284|3.059113|0
El Viso|El Viso||Foia de Bunyol|PV|39.411093|-0.691956|0
el Viver|Le Vivier||la Fenolleda|CN|42.763591|2.443229|1
El Volaor|El Volaor||Alacantí|PV|38.433229|-0.462749|0
el Voló|Le Boulou||el Rosselló|CN|42.526499|2.831845|1
El Xaragall|El Xaragall||Vallès Oriental|CAT|41.542151|2.312881|0
el Xaró|el Xaró||Anoia|CAT|41.546057|1.674142|0
Elda|Elda||Vinalopó Mitjà|PV|38.474938|-0.820841|1
Elna|Elne||el Rosselló|CN|42.600463|2.971483|1
els Alamús|els Alamús||Segrià|CAT|41.604932|0.758686|1
els Algars|els Algars||Comtat|PV|38.718007|-0.449802|0
els Angles|Les Angles||el Capcir|CN|42.579775|2.052161|1
els Arcs|els Arcs||Pla d'Urgell|CAT|41.693845|0.831779|0
els Arquets|els Arquets||Baix Penedès|CAT|41.296276|1.564653|0
Els Banyets|Els Banyets||Alacantí|PV|38.440705|-0.383493|0
els Banys d'Arles i Palaldà|Amélie-les-Bains-Palalda||el Vallespir|CN|42.444018|2.683284|1
Els Bassars|Els Bassars||Baix Vinalopó|PV|38.244866|-0.566045|0
Els Blanquers|Els Blanquers||Baix Empordà|CAT|41.925559|3.178094|0
els Boscos|els Boscos||Baix Penedès|CAT|41.271637|1.571526|0
els Boscos de Tarragona|els Boscos de Tarragona||Tarragonès|CAT|41.140853|1.293759|0
els Campellos|els Campellos||Alcoià|PV|38.565065|-0.63029|0
Els Cards|Els Cards||Garraf|CAT|41.236804|1.764599|0
Els Casots|Els Casots||Alt Penedès|CAT|41.409562|1.80942|0
els Castells|els Castells||Alt Urgell|CAT|42.298903|1.177216|0
els Ciprerets|els Ciprerets||Baix Vinalopó|PV|38.204497|-0.576276|0
Els Clots|Els Clots||Baix Llobregat|CAT|41.568169|1.82404|0
els Cocons|els Cocons||Tarragonès|CAT|41.160604|1.320527|0
Els Colls|Els Colls||Garraf|CAT|41.222072|1.752529|0
els Comtals|els Comtals||Bages|CAT|41.696659|1.839806|0
els Càpitos|els Càpitos|||CX|38.469203|-1.309809|0
els Delfins|els Delfins||Menorca|IB|40.001402|3.800886|0
els Escandells|els Escandells|||CX|38.469203|-1.309809|0
els Fruiters|els Fruiters||Vallès Occidental|CAT|41.615923|2.096912|0
els Garidells|els Garidells||Alt Camp|CAT|41.209716|1.245035|1
Els Garrofers|Els Garrofers||Garraf|CAT|41.254893|1.785276|0
Els Gira-Sols|Els Gira-Sols||Alacantí|PV|38.412607|-0.489152|0
els Gorgs|els Gorgs||Vallès Oriental|CAT|41.663009|2.298496|0
els Griells-la Pletera|els Griells-la Pletera||Baix Empordà|CAT|42.040645|3.193515|0
els Guiamets|els Guiamets||Priorat|CAT|41.105978|0.747851|1
els Guixots|els Guixots||Baix Llobregat|CAT|41.414876|1.907025|0
Els Hostalets|Els Hostalets||Alt Empordà|CAT|42.288731|2.943898|0
els Hostalets|els Hostalets||Segarra|CAT|41.631384|1.338034|0
Els Hostalets|Els Hostalets||Tarragonès|CAT|41.15956|1.267649|0
els Hostalets d'en Bas|els Hostalets d'en Bas||Garrotxa|CAT|42.101668|2.449817|0
Els Hostalets de Balenyà|Els Hostalets de Balenyà||Osona|CAT|41.816465|2.235627|0
els Hostalets de Pierola|els Hostalets de Pierola||Anoia|CAT|41.542472|1.795488|1
els Ivasors|els Ivasors||Plana Alta|PV|40.228111|-0.007842|0
Els Lledoners|Els Lledoners||Vallès Oriental|CAT|41.642428|2.192416|0
Els Manantials|Els Manantials||Vallès Oriental|CAT|41.675625|2.192367|0
els Manantials|els Manantials||Alt Camp|CAT|41.340332|1.361479|0
els Manous|els Manous||Tarragonès|CAT|41.159901|1.280744|0
els Manous 2|els Manous 2||Tarragonès|CAT|41.155975|1.277847|0
Els Manxons|Els Manxons||Bages|CAT|41.787719|1.778157|0
Els Masets|Els Masets||Alt Maestrat|PV|40.330528|-0.061519|0
els Masos|Los Masos||el Conflent|CN|42.615071|2.460703|1
els Masos|els Masos||Baix Empordà|CAT|42.045204|2.99694|0
els Masos|els Masos||Baix Penedès|CAT|41.188357|1.506752|0
Els Masos de Pals|Els Masos de Pals||Baix Empordà|CAT|41.979241|3.162519|0
els Masos de Tamúrcia|els Masos de Tamúrcia||Pallars Jussà|CAT|42.280662|0.760456|0
Els Masos de Ventalló|Els Masos de Ventalló||Alt Empordà|CAT|42.149813|3.02197|0
els Merlets|els Merlets||Pla d'Urgell|CAT|41.635521|0.910441|0
Els Monjos|Els Monjos||Alt Penedès|CAT|41.317064|1.659746|0
Els Montesinos|Els Montesinos||Vinalopó Mitjà|PV|38.387262|-0.791617|0
els Muntells|els Muntells|Muntells|Montsià|CAT|40.67072|0.74991|0
els Olivars|els Olivars||Alt Empordà|CAT|42.316895|3.128906|0
els Omellons|els Omellons||Garrigues|CAT|41.494327|0.960119|1
els Omells de na Gaia|els Omells de na Gaia||Urgell|CAT|41.496878|1.076608|1
els Pallaresos|els Pallaresos||Tarragonès|CAT|41.168285|1.260006|1
Els Partidores|Els Partidores||Camp de Morvedre|PV|39.669247|-0.288253|0
Els Pinars|Els Pinars||Baix Llobregat|CAT|41.380477|1.941938|0
els Pinars|els Pinars||Selva|CAT|41.730168|2.82361|0
Els Pinars|Els Pinars||Tarragonès|CAT|41.135673|1.296226|0
Els Pinars del Badó|Els Pinars del Badó||Moianès|CAT|41.717138|2.159093|0
els Pinillos|els Pinillos|||CX|38.606373|-1.150803|0
Els Pins|Els Pins||Pla de l'Estany|CAT|42.133741|2.765163|0
els Pins Blancs|els Pins Blancs||Tarragonès|CAT|41.171604|1.306721|0
Els Pins de Miramar|Els Pins de Miramar||Baix Camp|CAT|41.027311|0.958298|0
Els Pinyerets|Els Pinyerets||Anoia|CAT|41.595437|1.707358|0
els Plans|els Plans|||AD|42.5676|1.59756|0
Els Plans d'Arau|Els Plans d'Arau||Anoia|CAT|41.569048|1.666779|0
els Plans de Sió|els Plans de Sió||Segarra|CAT|41.735532|1.206586|1
els Poblets|els Poblets||Marina Alta|PV|38.854716|0.017792|1
Els Polvorers|Els Polvorers||Bages|CAT|41.726524|1.857092|0
Els Pous|Els Pous||Camp de Túria|PV|39.523317|-0.519174|0
els Prats de Rei|els Prats de Rei||Anoia|CAT|41.706438|1.551811|1
els Pugets|els Pugets||Baix Camp|CAT|41.184621|1.10034|0
els Pèlics|els Pèlics||Osona|CAT|41.988412|2.318243|0
els Quarts|els Quarts||Plana Alta|PV|40.087902|0.132966|0
Els Quatre Garrofers|Els Quatre Garrofers||Tarragonès|CAT|41.128293|1.246123|0
Els Racons|Els Racons||Horta Sud|PV|39.384894|-0.533956|0
els Ranxos de Bonany|els Ranxos de Bonany||Alt Camp|CAT|41.399416|1.45738|0
Els Rasos de Peguera|Els Rasos de Peguera||Berguedà|CAT|42.137085|1.760815|0
els Refugis del Montseny|els Refugis del Montseny||Vallès Oriental|CAT|41.715969|2.395106|0
els Reguers|els Reguers||Baix Ebre|CAT|40.838705|0.447944|0
Els Rincons|Els Rincons||Camp de Túria|PV|39.637501|-0.546613|0
Els Rosildos|Els Rosildos||Plana Alta|PV|40.292332|-0.046495|0
Els Saulons|Els Saulons||Vallès Oriental|CAT|41.650469|2.16048|0
Els Saulons d'en Deu|Els Saulons d'en Deu||Vallès Oriental|CAT|41.672985|2.17358|0
els Suvians|els Suvians||Baix Camp|CAT|41.200383|1.080729|0
els Torms|els Torms||Garrigues|CAT|41.395105|0.721428|1
Els Tres Turons|Els Tres Turons||Maresme|CAT|41.600897|2.517364|0
els Valentins|els Valentins||Montsià|CAT|40.627115|0.366109|0
els Vilars|els Vilars|||AD|42.50729|1.53414|0
Els Vilars|Els Vilars||Alt Empordà|CAT|42.410211|2.996683|0
els Vilars|els Vilars||Alt Maestrat|PV|40.328637|-0.084831|0
Els Vinyals|Els Vinyals||Garraf|CAT|41.273549|1.750081|0
els Vivencs|els Vivencs||Anoia|CAT|41.55478|1.680125|0
Els Àngels|Els Àngels||Baix Llobregat|CAT|41.456391|1.89225|0
els Àngels|els Àngels||Segrià|CAT|41.665225|0.546707|0
Elx|Elx/Elche||Baix Vinalopó|PV|38.247228|-0.664804|1
Emperador|Emperador||Horta Nord|PV|39.553839|-0.339197|1
Empuriabrava|Empuriabrava||Alt Empordà|CAT|42.24949|3.115658|0
Encamp|Encamp|||AD|42.53474|1.58014|0
Engolasters|Engolasters|||AD|42.50729|1.53414|0
Engordany|Engordany|||AD|42.50729|1.53414|0
Entre Naranjos y Flores|Entre Naranjos y Flores||Marina Baixa|PV|38.594824|-0.099712|0
Entrenaranjos|Entrenaranjos||Baix Segura|PV|38.035447|-0.821725|0
Entrenaranjos|Entrenaranjos||Camp de Túria|PV|39.552655|-0.537488|0
Entrepinos|Entrepinos||Marina Alta|PV|38.749601|0.210656|0
Entrepins|Entrepins||Tarragonès|CAT|41.139091|1.314775|0
Enveig|Enveitg||l'Alta Cerdanya|CN|42.49657|1.892845|1
Enviny|Enviny||Pallars Sobirà|CAT|42.40193|1.103795|0
Er|Err||l'Alta Cerdanya|CN|42.410599|2.065932|1
Era Bordeta|Era Bordeta||la Vall d'Aran|CAT|42.750286|0.69651|0
Era Bordeta|Era Bordeta||la Vall d'Aran|CAT|42.749801|0.700849|0
Erill la Vall|Erill la Vall||Alta Ribagorça|CAT|42.525299|0.825003|0
Erinyà|Erinyà||Pallars Jussà|CAT|42.276345|0.9278|0
Eroles|Eroles||Pallars Jussà|CAT|42.172806|0.816929|0
Erols|Erols||Gironès|CAT|41.930691|2.857312|0
Erta|Erta||Alta Ribagorça|CAT|42.426215|0.84324|0
Erts|Erts|||AD|42.54499|1.51483|0""",
        """es Caló|es Caló||Formentera|IB|38.676214|1.517333|0
es Camp de Mar|es Camp de Mar||Mallorca|IB|39.538539|2.423653|0
es Canar|es Canar||Eivissa|IB|38.999598|1.576122|0
es Canutells|es Canutells||Menorca|IB|39.850876|4.171591|0
es Cap des Moro|es Cap des Moro||Mallorca|IB|39.336944|3.18314|0
es Caparrot|es Caparrot||Menorca|IB|39.846447|4.175662|0
es Capdellà|es Capdellà||Mallorca|IB|39.57866|2.468704|0
es Carregador|es Carregador||Mallorca|IB|39.692161|3.452097|0
es Carritxó|es Carritxó||Mallorca|IB|39.424565|3.167198|0
es Castell|es Castell||Menorca|IB|39.864403|4.285877|1
es Caülls|es Caülls||Mallorca|IB|39.633712|2.727398|0
es Coll d'en Rabassa|es Coll d'en Rabassa||Mallorca|IB|39.551682|2.696432|0
es Figueral|es Figueral||Eivissa|IB|39.049805|1.592293|0
es Garrovers|es Garrovers||Mallorca|IB|39.631602|2.703129|0
es Grau|es Grau||Menorca|IB|39.948755|4.268001|0
es Llombards|es Llombards||Mallorca|IB|39.342814|3.094894|0
es Mercadal|es Mercadal||Menorca|IB|40.012493|4.112096|1
es Migjorn Gran|es Migjorn Gran||Menorca|IB|39.943297|4.037928|1
es Murtar|es Murtar||Menorca|IB|39.904466|4.291665|0
es Nogueral|es Nogueral||Mallorca|IB|39.707158|2.629894|0
es Palmer|es Palmer||Mallorca|IB|39.378294|3.019036|0
es Pas|es Pas||Mallorca|IB|39.36668|2.877635|0
es Pelats|es Pelats||Mallorca|IB|39.719294|3.470013|0
es Pil.larí|es Pil.larí||Mallorca|IB|39.529878|2.754965|0
es Pinaret|es Pinaret||Mallorca|IB|39.590956|2.711639|0
es Pla de na Tesa|es Pla de na Tesa||Mallorca|IB|39.595386|2.708861|0
es Pont d'Inca|es Pont d'Inca||Mallorca|IB|39.595857|2.69281|0
es Pont d'Inca Nou|es Pont d'Inca Nou||Mallorca|IB|39.604173|2.696901|0
es Port des Canonge|es Port des Canonge||Mallorca|IB|39.699327|2.554974|0
es Pou Nou|es Pou Nou||Menorca|IB|39.846499|4.262008|0
es Provençals|es Provençals||Mallorca|IB|39.674724|3.452453|0
es Puig de na Ribes|es Puig de na Ribes||Eivissa|IB|38.94439|1.42222|0
es Puigderrós|es Puigderrós||Mallorca|IB|39.444995|2.752689|0
es Pujols|es Pujols||Formentera|IB|38.719659|1.459227|0
es Secar de la Real|es Secar de la Real||Mallorca|IB|39.60912|2.636767|0
Esblada|Esblada||Alt Camp|CAT|41.444648|1.440368|0
Escadarcs|Escadarcs||Cerdanya|CAT|42.384914|1.891483|0
Escaladei|Escaladei||Priorat|CAT|41.247987|0.811214|0
Escalarre|Escalarre||Pallars Sobirà|CAT|42.616137|1.139804|0
Escaldes|Escaldes|||AD|42.50729|1.53414|0
Escaldes-Engordany|Escaldes-Engordany|||AD|42.50729|1.53414|0
Escaló|Escaló||Pallars Sobirà|CAT|42.549647|1.155646|0
Escart|Escart||Pallars Sobirà|CAT|42.542609|1.131617|0
Escaró|Escaro||el Conflent|CN|42.534889|2.319375|1
Esclanyà|Esclanyà||Baix Empordà|CAT|41.927808|3.173038|0
Escorca|Escorca||Mallorca|IB|39.82792|2.848025|1
Escorpí|Escorpí||Tarragonès|CAT|41.143472|1.318714|0
Escunhau|Escunhau||la Vall d'Aran|CAT|42.697408|0.824009|0
Escunhau i Casarilh|Escunhau e Casarilh|Escunhau e Casarilh|Aran|CAT|42.705946|0.787125|0
Escàs|Escàs||Pallars Sobirà|CAT|42.462719|1.106059|0
Escós|Escós||Pallars Sobirà|CAT|42.37158|1.064988|0
Eslida|Eslida||Plana Baixa|PV|39.886944|-0.304912|1
Espadella|Espadilla||Alt Millars|PV|40.020768|-0.365981|1
Esparreguera|Esparreguera||Baix Llobregat|CAT|41.549763|1.862863|1
Espaén|Espaén||Alt Urgell|CAT|42.31276|1.276823|0
Espiells|Espiells||Alt Penedès|CAT|41.434239|1.806072|0
Espinalbet|Espinalbet||Berguedà|CAT|42.115245|1.800707|0
Espinavell|Espinavell||Ripollès|CAT|42.377901|2.39925|0
Espinavessa|Espinavessa||Alt Empordà|CAT|42.188704|2.844296|0
Espinelves|Espinelves||Osona|CAT|41.873748|2.423038|1
Espirà de Conflent|Espira-de-Conflent||el Conflent|CN|42.619189|2.494122|1
Espirà de l'Aglí|Espira-de-l'Agly||el Rosselló|CN|42.79668|2.822096|1
Espitau|Espitau||la Vall d'Aran|CAT|42.627131|0.763677|0
Espluga de Serra|Espluga de Serra||Pallars Jussà|CAT|42.284643|0.842922|0
Esplugues de Llobregat|Esplugues de Llobregat||Baix Llobregat|CAT|41.37902|2.090829|1
Espolla|Espolla||Alt Empordà|CAT|42.429267|3.005573|1
Esponellà|Esponellà||Pla de l'Estany|CAT|42.171305|2.801194|1
Esporles|Esporles||Mallorca|IB|39.662793|2.586893|1
Espot|Espot||Pallars Sobirà|CAT|42.565765|1.044126|1
Espui|Espui||Pallars Jussà|CAT|42.455384|0.989347|0
Espés|Espés||la Ribagorça|FR|42.438845|0.585584|0
Espés Alto|Espés Alto||la Ribagorça|FR|42.447658|0.587549|0
Establiments Nou|Establiments Nou||Mallorca|IB|39.633448|2.60749|0
Establiments Vells|Establiments Vells||Mallorca|IB|39.62399|2.620936|0
Estac|Estac||Pallars Sobirà|CAT|42.38091|1.075875|0
Estació|Estació||Baix Empordà|CAT|41.932372|3.138955|0
Estació de les Valls|Estació de les Valls||Camp de Morvedre|PV|39.719487|-0.240137|0
Estació Sant Vicenç de Calders|Estació Sant Vicenç de Calders||Baix Penedès|CAT|41.187732|1.525058|0
Estación de Monóvar|Estación de Monóvar||Vinalopó Mitjà|PV|38.444866|-0.814614|0
Estagell|Estagel||el Rosselló|CN|42.767943|2.707112|1
Estamariu|Estamariu||Alt Urgell|CAT|42.381677|1.525495|1
Estana|Estana||Cerdanya|CAT|42.320242|1.661979|0
Estaon|Estaon||Pallars Sobirà|CAT|42.587814|1.210358|0
Estaron|Estaron||Pallars Sobirà|CAT|42.530237|1.191062|0
Estaràs|Estaràs||Segarra|CAT|41.703696|1.38113|1
Estavar|Estavar||l'Alta Cerdanya|CN|42.474998|2.009273|1
Estaña|Estaña||la Ribagorça|FR|42.032098|0.518085|0
Estellencs|Estellencs||Mallorca|IB|39.648841|2.4765|1
Esterri d'Àneu|Esterri d'Àneu||Pallars Sobirà|CAT|42.622111|1.118341|1
Esterri de Cardós|Esterri de Cardós||Pallars Sobirà|CAT|42.587856|1.268595|1
Estivella|Estivella||Camp de Morvedre|PV|39.71448|-0.368795|1
Estoer|Estoher||el Conflent|CN|42.568136|2.482175|1
Estoll|Estoll||Cerdanya|CAT|42.386015|1.88632|0
Estopanyà|Estopiñán del Castillo||la Ribagorça|FR|42.010208|0.549551|1
Estorm|Estorm||Pallars Jussà|CAT|42.086147|0.841149|0
Estubeny|Estubeny||Costera|PV|39.0172|-0.616879|1
Etersa|Etersa||Baix Camp|CAT|41.026938|0.963745|0
Eth Pradet|Eth Pradet||la Vall d'Aran|CAT|42.82051|0.80525|0
Eth Pònt d'Arròs|Eth Pònt d'Arròs||la Vall d'Aran|CAT|42.737543|0.756191|0
Eucaliptus|Eucaliptus||Montsià|CAT|40.657628|0.78213|0
Europark|Europark||Marina Baixa|PV|38.498931|-0.25895|0
Eus|Eus||el Conflent|CN|42.657839|2.446595|1
Extramuros Geldo|Extramuros Geldo||Alt Palància|PV|39.83554|-0.469676|0
Fabrica Blanca|Fabrica Blanca||Bages|CAT|41.703751|1.835232|0
Fabrica Vermella|Fabrica Vermella||Bages|CAT|41.710837|1.832456|0
Fageca|Fageca||Comtat|PV|38.731563|-0.274359|1
Faió|Fayón||el Matarranya|FR|41.264399|0.319133|1
Fals|Fals||Bages|CAT|41.742502|1.714132|0
Falset|Falset||Priorat|CAT|41.160131|0.815297|1
Famorca|Famorca||Comtat|PV|38.730585|-0.240591|1
Fanadix|Fanadix||Marina Alta|PV|38.68274|0.008408|0
Fanadix|Fanadix||Marina Alta|PV|38.683059|0.110019|0
Fanzara|Fanzara||Alt Millars|PV|40.012507|-0.315297|1
Farena|Farena||Alt Camp|CAT|41.311758|1.085961|0
Faro|Faro||Ribera Baixa|PV|39.185151|-0.221654|0
Farrera|Farrera||Pallars Sobirà|CAT|42.483212|1.296339|1
Faura|Faura||Camp de Morvedre|PV|39.727866|-0.258228|1
Favanella|Abanilla|||CX|38.254955|-1.051719|0
Favara|Favara||Ribera Baixa|PV|39.11841|-0.299538|1
Favara de Matarranya|Fabara||el Matarranya|FR|41.197466|0.157074|1
Favaret|Favaret||Montsià|CAT|40.699576|0.554203|0
Felanitx|Felanitx||Mallorca|IB|39.451576|3.159794|1
Feliu Vila|Feliu Vila||Maresme|CAT|41.490091|2.344184|0
Fellines|Fellines||Gironès|CAT|42.094569|2.904116|0
Felluns|Feilluns||la Fenolleda|CN|42.761018|2.488225|1
Femades|Femades||Baix Llobregat|CAT|41.353741|2.093751|0
Fenollet|Fenouillet||la Fenolleda|CN|42.782556|2.373485|1
Ferran|Ferran||Segarra|CAT|41.729279|1.397142|0
Ferran|Ferran||Tarragonès|CAT|41.14513|1.35871|0
Ferreries|Ferreries||Menorca|IB|39.990365|4.003113|1
Fertilia|Fertilia|||AL|40.55969|8.31953|0
Fibracolor|Fibracolor||Maresme|CAT|41.695624|2.729635|0
Figaró|Figaró||Vallès Oriental|CAT|41.721821|2.2728|0
Figaró-Montmany|Figaró-Montmany||Vallès Oriental|CAT|41.717157|2.27221|1
Figueral-Can Farineta|Figueral-Can Farineta||Mallorca|IB|39.622437|2.72722|0
Figueres|Figueres||Alt Empordà|CAT|42.270594|2.97187|1
Figueres|Higueras||Alt Palància|PV|39.991763|-0.508517|1
Figuerola d'Orcau|Figuerola d'Orcau||Pallars Jussà|CAT|42.13093|0.996133|0
Figuerola de Meià|Figuerola de Meià||Noguera|CAT|41.974491|0.890546|0
Figuerola del Camp|Figuerola del Camp||Alt Camp|CAT|41.358594|1.254688|1
Figueroles|Figueroles||Alcalatén|PV|40.126398|-0.222378|1
Figueroles de Domenyo|Higueruelas||Serrans|PV|39.772497|-0.862998|1
Fillol|Fillol||Anoia|CAT|41.548079|1.469629|0
Fillols|Fillols||el Conflent|CN|42.557949|2.422617|1
Finca Don Jaime|Finca Don Jaime||Alacantí|PV|38.432265|-0.572614|0
Finca Terol|Finca Terol||Alcoià|PV|38.553268|-0.602377|0
Finca Verd|Finca Verd||Baix Empordà|CAT|41.862673|3.101848|0
Fincas la Vega|Fincas la Vega||Baix Segura|PV|38.106196|-0.723887|0
Finestrat|Finestrat||Marina Baixa|PV|38.574181|-0.203391|1
Finestret|Finestret||el Conflent|CN|42.604106|2.515854|1
Flaçà|Flaçà||Gironès|CAT|42.045497|2.962461|1
Fleix|Fleix||Marina Alta|PV|38.777103|-0.108738|0
Flix|Flix||Ribera d'Ebre|CAT|41.260997|0.5668|1
Florejacs|Florejacs||Segarra|CAT|41.800679|1.213652|0
Florimar|Florimar||Tarragonès|CAT|41.135141|1.299744|0
Fluvià|Fluvià||Garrotxa|CAT|42.220366|2.561197|0
Fogars de la Selva|Fogars de la Selva||Selva|CAT|41.720247|2.642493|1
Fogars de Montclús|Fogars de Montclús||Vallès Oriental|CAT|41.753807|2.439182|1
Fogueres de Montsoriu|Fogueres de Montsoriu||Selva|CAT|41.782259|2.520777|0
Foia l'Om|Foia l'Om||Marina Baixa|PV|38.589267|-0.106259|0
Foietes|Foietes||Marina Alta|PV|38.78151|-0.057841|0
Foios|Foios||Horta Nord|PV|39.54519|-0.36059|1
Foixà|Foixà||Baix Empordà|CAT|42.050501|3.000572|1
Folgueroles|Folgueroles||Osona|CAT|41.940681|2.314765|1
Fondarella|Fondarella||Pla d'Urgell|CAT|41.629095|0.866846|1
Fondespatla|Fuentespalda||el Matarranya|FR|40.8176|0.070601|1
Fonolleres|Fonolleres||Baix Empordà|CAT|42.021541|3.051059|0
Fonolleres|Fonolleres||Segarra|CAT|41.657472|1.202305|0
Fonollosa|Fonollosa||Bages|CAT|41.764927|1.712508|1
Font d'en Titus|Font d'en Titus||Osona|CAT|41.931726|2.304426|0
Font de Cera|Font de Cera||Maresme|CAT|41.510202|2.2964|0
Font de la Pólvora|Font de la Pólvora||Gironès|CAT|41.974948|2.843227|0
Font de la Roca|Font de la Roca||Alt Camp|CAT|41.207744|1.240203|0
Font de sa Cala|Font de sa Cala||Mallorca|IB|39.684038|3.450425|0
Font de Sant Llorenç|Font de Sant Llorenç||Selva|CAT|41.712529|2.881007|0
Font del Bou|Font del Bou||Vallès Oriental|CAT|41.662508|2.213461|0
Font del Lleó|Font del Lleó||Baix Empordà|CAT|41.859084|3.082574|0
Font Granada|Font Granada||Vallès Oriental|CAT|41.668509|2.202835|0
Font Roja|Font Roja||Alcoià|PV|38.665059|-0.54073|0
Font-Mezquitas|Font-Mezquitas||Alacantí|PV|38.416555|-0.412396|0
Font-rabiosa|Fontrabiouse||el Capcir|CN|42.642604|2.054166|1
Font-romeu, Odelló i Vià|Font-Romeu-Odeillo-Via||l'Alta Cerdanya|CN|42.513037|2.026999|1
Font-rubí|Font-rubí||Alt Penedès|CAT|41.422007|1.643978|1
Font-rubí|Font-rubí||Ripollès|CAT|42.32112|2.416346|0
Font-rubí dalt|Font-rubí dalt||Alt Penedès|CAT|41.436784|1.588027|0
Font-rubí de Baix|Font-rubí de Baix||Alt Penedès|CAT|41.434483|1.591025|0
Fontanals de Cerdanya|Fontanals de Cerdanya||Cerdanya|CAT|42.385607|1.919729|1
Fontanars dels Alforins|Fontanars dels Alforins||Vall d'Albaida|PV|38.779851|-0.785245|1
Fontaneda|Fontaneda|||AD|42.46372|1.49129|0
Fontanelles|Fontanelles||Osona|CAT|41.969319|2.300285|0
Fontanelles|Fontanelles||Osona|CAT|41.873973|2.182138|0
Fontanilles|Fontanilles||Baix Empordà|CAT|42.014681|3.108409|1
Fontcalent|Fontcalent|||PV|38.382222|-0.537562|0
Fontclara|Fontclara||Baix Empordà|CAT|41.993278|3.122308|0
Fontcoberta|Fontcoberta||Pla de l'Estany|CAT|42.136038|2.805096|1
Fontcoberta|Fontcoberta||Baix Camp|CAT|41.072757|1.060655|0
Fontdepou|Fontdepou||Noguera|CAT|41.961503|0.757416|0
Fonteta|Fonteta||Baix Empordà|CAT|41.950377|3.057079|0
Fontllonga|Fontllonga||Noguera|CAT|41.970904|0.858531|0
Fontllonga i Ametlla|Fontllonga i Ametlla||Noguera|CAT|41.913854|0.867405|0
Fontpedrosa|Fontpédrouse||el Conflent|CN|42.463074|2.194855|1
Fontpineda|Fontpineda||Baix Llobregat|CAT|41.422196|1.969548|0
Fontsagrada|Fontsagrada||Pallars Jussà|CAT|42.128101|0.924032|0
Fontsanta|Fontsanta||Baix Llobregat|CAT|41.35964|2.060824|0
Fontscaldes|Fontscaldes||Alt Camp|CAT|41.332494|1.231647|0
Foradada|Foradada||Noguera|CAT|41.87344|1.01161|1
Forallac|Forallac||Baix Empordà|CAT|41.938931|3.085592|1
Formentera|Formentera||Formentera|IB|38.691972|1.458945|1
Formentera del Segura|Formentera del Segura||Baix Segura|PV|38.094226|-0.736401|1
Formidables|Formidables||Camp de Túria|PV|39.638128|-0.575215|0
Formiguera|Formiguères||el Capcir|CN|42.621087|2.054508|1
Forn d'Alcedo|Forn d'Alcedo|||PV|39.420604|-0.358439|0
Forna|Forna||Marina Alta|PV|38.878404|-0.166666|0
Fornalutx|Fornalutx||Mallorca|IB|39.796293|2.751202|1
Fornells|Fornells||Baix Empordà|CAT|41.939527|3.214759|0
Fornells|Fornells||Menorca|IB|40.054524|4.129329|0
Fornells de la Muntanya|Fornells de la Muntanya||Ripollès|CAT|42.32419|2.047326|0
Fornells de la Selva|Fornells de la Selva||Gironès|CAT|41.92794|2.811749|1
Fornells Park|Fornells Park||Gironès|CAT|41.943955|2.808924|0
Forques|Fourques||el Rosselló|CN|42.584875|2.777702|1
Fortaleny|Fortaleny||Ribera Baixa|PV|39.177346|-0.316605|1
Fortià|Fortià||Alt Empordà|CAT|42.242856|3.03577|1
Forès|Forès||Conca de Barberà|CAT|41.492158|1.230297|1
Fossa|Fosse||la Fenolleda|CN|42.785566|2.432888|1
Fraga|Fraga||el Baix Cinca|FR|41.483431|0.21621|1
Franciac|Franciac||Selva|CAT|41.873623|2.784912|0
Fredes|Fredes||Baix Maestrat|PV|40.705517|0.171124|0
Freginals|Freginals||Montsià|CAT|40.658663|0.518476|1
Freixenet de Segarra|Freixenet de Segarra||Segarra|CAT|41.660299|1.396424|0
Freixinet|Freixinet||Solsonès|CAT|41.921686|1.567455|0
Fuen Vich|Fuen Vich||Plana d'Utiel|PV|39.352231|-1.056472|0
Fuenterrobles|Fuenterrobles||Plana d'Utiel|PV|39.578236|-1.368114|1
Fulleda|Fulleda||Garrigues|CAT|41.461078|1.03416|1
Fullà|Fuilla||el Conflent|CN|42.564714|2.35533|1
Fígols|Fígols||Berguedà|CAT|42.168456|1.785071|1
Fígols|Fígols||Alt Urgell|CAT|42.202423|1.338772|0
Fígols de Tremp|Fígols de Tremp||Pallars Jussà|CAT|42.152929|0.815093|0
Fígols i Alinyà|Fígols i Alinyà||Alt Urgell|CAT|42.195085|1.399338|1
Fórnols|Fórnols||Alt Urgell|CAT|42.253071|1.515441|0
Fórnols de Matarranya|Fórnoles||el Matarranya|FR|40.898728|-0.001126|1
Gabasa|Gabasa||la Llitera|FR|42.006871|0.414329|0
Gaianes|Gaianes||Comtat|PV|38.81593|-0.410837|1
Gaibiel|Gaibiel||Alt Palància|PV|39.929663|-0.499402|1
Gaià|Gaià||Bages|CAT|41.928304|1.928077|1
Galatzó|Galatzó||Mallorca|IB|39.525038|2.497482|0
Galera|Galera||Bages|CAT|41.9125|1.888531|0
Galilea|Galilea||Mallorca|IB|39.610441|2.506376|0
Gallifa|Gallifa||Vallès Occidental|CAT|41.694393|2.114148|1
Galliners|Galliners||Pla de l'Estany|CAT|42.149751|2.872117|0
Gallipont I|Gallipont I||Camp de Túria|PV|39.567974|-0.564724|0""",
        """Gallipont II|Gallipont II||Camp de Túria|PV|39.563832|-0.556008|0
Gandesa|Gandesa||Terra Alta|CAT|41.054105|0.42153|1
Gandia|Gandia||Safor|PV|38.984339|-0.215356|1
Garcia|Garcia||Ribera d'Ebre|CAT|41.142065|0.651857|1
Garden Curt-la Paloma|Garden Curt-la Paloma||Marina Baixa|PV|38.506436|-0.250616|0
Garraf|Garraf||Garraf|CAT|41.254327|1.901864|0
Garrigoles|Garrigoles||Baix Empordà|CAT|42.106024|3.024014|1
Garriguella|Garriguella||Alt Empordà|CAT|42.343619|3.0625|1
Garrigàs|Garrigàs||Alt Empordà|CAT|42.182379|2.9534|1
Garrofers|Garrofers||Maresme|CAT|41.498292|2.291889|0
Garràfols|Garràfols||Alt Camp|CAT|41.227708|1.246981|0
Garòs|Garòs||la Vall d'Aran|CAT|42.700851|0.846432|0
Gaserans|Gaserans||Selva|CAT|41.749124|2.590673|0
Gata de Gorgos|Gata de Gorgos||Marina Alta|PV|38.768062|0.069841|1
Gata Residencial|Gata Residencial||Marina Alta|PV|38.770249|0.049064|0
Gausac|Gausac||Aran|CAT|42.708706|0.791501|0
Gavarda|Gavarda||Ribera Alta|PV|39.093162|-0.560211|1
Gavarra|Gavarra||Alt Urgell|CAT|42.104032|1.196234|0
Gavet de la Conca|Gavet de la Conca||Pallars Jussà|CAT|42.07274|0.989734|1
Gavà|Gavà||Baix Llobregat|CAT|41.294527|1.983569|1
Gavàs|Gavàs||Pallars Sobirà|CAT|42.641346|1.160003|0
Gaüses|Gaüses||Baix Empordà|CAT|42.099273|2.964745|0
Geldo|Geldo||Alt Palància|PV|39.836016|-0.465579|1
Gelida|Gelida||Alt Penedès|CAT|41.435189|1.858987|1
Ger|Ger||Cerdanya|CAT|42.438196|1.828756|1
Gerb|Gerb||Noguera|CAT|41.822096|0.804363|0
Gerri de la Sal|Gerri de la Sal||Pallars Sobirà|CAT|42.32221|1.063227|0
Gessa|Gessa||Aran|CAT|42.706052|0.889767|0
Gilet|Gilet||Camp de Morvedre|PV|39.670092|-0.336628|1
Gimenells|Gimenells||Segrià|CAT|41.652535|0.389514|0
Gimenells i el Pla de la Font|Gimenells i el Pla de la Font||Segrià|CAT|41.667545|0.382659|1
Ginaste|Ginaste||la Ribagorça|FR|42.49476|0.713645|0
Ginestar|Ginestar||Ribera d'Ebre|CAT|41.029098|0.637239|1
Ginestar|Ginestar||Gironès|CAT|42.008273|2.743775|0
Ginestarre|Ginestarre||Pallars Sobirà|CAT|42.596857|1.262905|0
Giraba de Abajo|Giraba de Abajo||Alt Millars|PV|40.102418|-0.381796|0
Giraba de Arriba|Giraba de Arriba||Alt Millars|PV|40.105803|-0.381036|0
Girona|Girona||Gironès|CAT|41.984792|2.835282|1
Gironella|Gironella||Berguedà|CAT|42.026627|1.884626|1
Girul|Girul||Cerdanya|CAT|42.449243|1.776841|0
Gisclareny|Gisclareny||Berguedà|CAT|42.265307|1.786448|1
Glorianes|Glorianes||el Conflent|CN|42.59414|2.554707|1
Glorieta|Glorieta||Conca de Barberà|CAT|41.51987|1.204504|0
Godall|Godall||Montsià|CAT|40.662234|0.463853|1
Godella|Godella||Horta Nord|PV|39.535581|-0.42664|1
Godelleta|Godelleta||Foia de Bunyol|PV|39.426111|-0.666345|1
Golmés|Golmés||Pla d'Urgell|CAT|41.624758|0.92686|1
Gombrèn|Gombrèn||Ripollès|CAT|42.250652|2.070281|1
Gorga|Gorga||Comtat|PV|38.722161|-0.373777|1
Gorguja|Gorguja||Cerdanya|CAT|42.456894|2.001723|0
Gospí|Gospí||Segarra|CAT|41.738205|1.349579|0
Gotarta|Gotarta||Alta Ribagorça|CAT|42.42145|0.763942|0
Gra|Gra||Segarra|CAT|41.766566|1.250411|0
Grab|Grab||Baix Llobregat|CAT|41.39971|1.975335|0
Grabuac|Grabuac||Alt Penedès|CAT|41.394349|1.677892|0
Gramuntell|Gramuntell||Segarra|CAT|41.617012|1.260503|0
Gran Vista|Gran Vista||Maresme|CAT|41.513081|2.316664|0
Granera|Granera||Moianès|CAT|41.727127|2.063001|1
Granja Garcia|Granja Garcia||Baix Llobregat|CAT|41.399614|1.991067|0
Granollers|Granollers||Vallès Oriental|CAT|41.593579|2.278941|1
Granollers de Rocacorba|Granollers de Rocacorba||Gironès|CAT|42.068448|2.65182|0
Granyanella|Granyanella||Segarra|CAT|41.655345|1.21418|1
Granyena de les Garrigues|Granyena de les Garrigues||Garrigues|CAT|41.434967|0.653578|1
Granyena de Segarra|Granyena de Segarra||Segarra|CAT|41.620071|1.22052|1
Grao Vell|Grao Vell||Camp de Morvedre|PV|39.635078|-0.240096|0
Gratallops|Gratallops||Priorat|CAT|41.19139|0.772411|1
Grau i Platja|Grau i Platja||Safor|PV|38.999375|-0.162881|0
Graugés|Graugés||Berguedà|CAT|42.067306|1.845932|0
Graó Park|Graó Park||Segrià|CAT|41.67284|0.541708|0
Grions|Grions||Selva|CAT|41.761073|2.608521|0
Grup Santa Maria|Grup Santa Maria||Ripollès|CAT|42.20053|2.185058|0
Gréixer|Gréixer||Cerdanya|CAT|42.412857|1.823881|0
Guadassuar|Guadassuar||Ribera Alta|PV|39.181989|-0.530863|1
Guadasséquies|Guadasséquies||Vall d'Albaida|PV|38.927225|-0.494002|1
Gualba|Gualba||Vallès Oriental|CAT|41.737289|2.504246|1
Gualba de Baix|Gualba de Baix||Vallès Oriental|CAT|41.711401|2.531146|0
Gualta|Gualta||Baix Empordà|CAT|42.025378|3.117025|1
Gualter|Gualter||Noguera|CAT|41.927866|1.198289|0
Guarda-si-venes|Guarda-si-venes||Segarra|CAT|41.810093|1.273508|0
Guardamar de la Safor|Guardamar de la Safor||Safor|PV|38.963232|-0.146214|1
Guardamar del Segura|Guardamar del Segura||Baix Segura|PV|38.080324|-0.668396|1
Guardiola|Guardiola||Noguera|CAT|41.830124|1.221765|0
Guardiola de Berguedà|Guardiola de Berguedà||Berguedà|CAT|42.262575|1.882997|1
Guardiola de Font-rubí|Guardiola de Font-rubí||Alt Penedès|CAT|41.415306|1.64971|0
Guialmons|Guialmons||Conca de Barberà|CAT|41.512092|1.365532|0
Guils de Cerdanya|Guils de Cerdanya||Cerdanya|CAT|42.451277|1.867484|1
Guils del Cantó|Guils del Cantó||Alt Urgell|CAT|42.345806|1.283475|0
Guimerà|Guimerà||Urgell|CAT|41.565361|1.185036|1
Guissona|Guissona||Segarra|CAT|41.789141|1.287537|1
Guixers|Guixers||Solsonès|CAT|42.139591|1.641057|1
Gurb|Gurb||Osona|CAT|41.961954|2.225876|1
Gurp|Gurp||Pallars Jussà|CAT|42.223333|0.84808|0
Guàrdia de Noguera|Guàrdia de Noguera||Pallars Jussà|CAT|42.091757|0.876722|0
Gàrzola|Gàrzola||Noguera|CAT|41.963183|1.026279|0
Gàtova|Gátova||Camp de Túria|PV|39.761683|-0.530209|1
Gàver|Gàver||Segarra|CAT|41.687838|1.392691|0
Gènova|Gènova||Mallorca|IB|39.561751|2.595196|0
Gósol|Gósol||Berguedà|CAT|42.20858|1.668834|1
Habitat|Habitat||Mallorca|IB|39.903081|3.067447|0
Herbers|Herbers||Ports|PV|40.708719|-0.014789|1
Herbeset|Herbeset||Ports|PV|40.662432|-0.019566|0
Hervideros|Hervideros||Vall de Cofrents|PV|39.234694|-1.088896|0
Horno de Alcedo|Horno de Alcedo||València|PV|39.432255|-0.376405|0
Horta d'Avinyó|Horta d'Avinyó||Bages|CAT|41.821964|1.965463|0
Horta de Sant Joan|Horta de Sant Joan||Terra Alta|CAT|40.937533|0.323|1
Hortafà|Ortaffa||el Rosselló|CN|42.584481|2.920731|1
Hortoneda|Hortoneda||Pallars Jussà|CAT|42.245283|1.042628|0
Hortunas|Hortunas||Plana d'Utiel|PV|39.391466|-1.045537|0
Hostafrancs|Hostafrancs||Segarra|CAT|41.736529|1.243942|0
Hostal Nou|Hostal Nou||Garrotxa|CAT|42.23011|2.591629|0
Hostal Nou|Hostal Nou||Ports|PV|40.608762|-0.094271|0
Hostalric|Hostalric||Selva|CAT|41.740937|2.630407|1
Hoya de la Señora|Hoya de la Señora||Marina Baixa|PV|38.563828|-0.266995|0
Hurchillo|Hurchillo||Baix Segura|PV|38.059198|-0.92236|0
Ibars-Meia|Ibars-Meia||Maresme|CAT|41.490869|2.309385|0
Ibi|Ibi||Alcoià|PV|38.62487|-0.562986|1
Icomar|Icomar||Tarragonès|CAT|41.116859|1.225893|0
Idiada|Idiada||Baix Penedès|CAT|41.266801|1.52496|0
Iecla|Yecla|||CX|38.606373|-1.150803|0
Ignasi Sagnier|Ignasi Sagnier||Selva|CAT|41.734507|2.614921|0
Igualada|Igualada||Anoia|CAT|41.585945|1.614514|1
Illa del Riberal|Ille-sur-Têt||el Rosselló|CN|42.677217|2.616213|1
Inca|Inca||Mallorca|IB|39.711727|2.942162|1
Incles|Incles|||AD|42.5676|1.59756|0
Inter Club|Inter Club||Baix Llobregat|CAT|41.381794|1.948141|0
Inter-Club|Inter-Club||Baix Llobregat|CAT|41.382577|1.950948|0
Irgo|Irgo||Alta Ribagorça|CAT|42.444488|0.77258|0
Isavarre|Isavarre||Pallars Sobirà|CAT|42.650413|1.099378|0
Isil|Isil||Pallars Sobirà|CAT|42.678832|1.086306|0
Isil i Alós|Isil i Alós||Pallars Sobirà|CAT|42.682771|1.060597|0
Isona|Isona||Pallars Jussà|CAT|42.116963|1.043951|0
Isona i Conca Dellà|Isona i Conca Dellà||Pallars Jussà|CAT|42.126711|1.038082|1
Isàvena|Isábena||la Ribagorça|FR|42.303991|0.534882|1
Isòvol|Isòvol||Cerdanya|CAT|42.391334|1.823688|1
Ivars d'Urgell|Ivars d'Urgell||Pla d'Urgell|CAT|41.688633|0.966684|1
Ivars de Noguera|Ivars de Noguera||Noguera|CAT|41.868011|0.609635|1
Ivorra|Ivorra||Segarra|CAT|41.769879|1.390493|1
Iàtova|Yátova||Foia de Bunyol|PV|39.361117|-0.891736|1
Jafre|Jafre||Baix Empordà|CAT|42.073044|3.013231|1
Jaraguas|Jaraguas||Plana d'Utiel|PV|39.521141|-1.36205|0
Jardins de Sant Pol|Jardins de Sant Pol||Maresme|CAT|41.59965|2.607441|0
Jardins Imperi|Jardins Imperi||Tarragonès|CAT|41.16791|1.272188|0
Jardí de Lauro|Jardí de Lauro||Camp de Túria|PV|39.64417|-0.569724|0
Jardín de Alfaz|Jardín de Alfaz||Marina Baixa|PV|38.573867|-0.090129|0
Jardín de Jaravilla|Jardín de Jaravilla||Camp de Túria|PV|39.580976|-0.520123|0
Jesús|Jesús||Baix Ebre|CAT|40.827687|0.508367|0
Jesús|Jesús||Eivissa|IB|38.933407|1.448103|0
Jesús Pobre|Jesús Pobre||Marina Alta|PV|38.798415|0.084644|0
Joanet|Joanet||Selva|CAT|41.843841|2.52785|0
Joanetes|Joanetes||Garrotxa|CAT|42.121539|2.422299|0
Jorba|Jorba||Anoia|CAT|41.604721|1.529215|1
Jornets|Jornets||Mallorca|IB|39.675029|2.935291|0
Josa de Cadí|Josa de Cadí||Alt Urgell|CAT|42.256969|1.618553|0
Josa i Tuixén|Josa i Tuixén||Alt Urgell|CAT|42.246034|1.603177|1
Jou|Jou||Pallars Sobirà|CAT|42.598049|1.113039|0
Juberri|Juberri|||AD|42.46372|1.49129|0
Juià|Juià||Gironès|CAT|42.013449|2.907975|1
Jumella|Jumilla|||CX|38.469203|-1.309809|0
Juncosa|Juncosa||Garrigues|CAT|41.380348|0.786417|1
Juneda|Juneda||Garrigues|CAT|41.534601|0.823577|1
Junior Park|Junior Park||Selva|CAT|41.73092|2.536344|0
Jóc|Joch||el Conflent|CN|42.61563|2.525909|1
Jújols|Jujols||el Conflent|CN|42.582698|2.284992|1
King Parc|King Parc||Selva|CAT|41.8072|2.777877|0
L'Abella|L'Abella||Osona|CAT|41.764437|2.250718|0
L'Advocat|L'Advocat||Alacantí|PV|38.416295|-0.536964|0
l'Agrícola|l'Agrícola||Alt Penedès|CAT|41.355816|1.669676|0
L'Aigüeta|L'Aigüeta||Alt Empordà|CAT|42.278846|2.969134|0
l'Aixàviga|l'Aixàviga||Alt Camp|CAT|41.273682|1.083679|0
l'Albagés|l'Albagés||Garrigues|CAT|41.446421|0.753165|1
l'Albera|L'Albère||el Vallespir|CN|42.47606|2.895602|1
l'Alberquilla|l'Alberquilla|||CX|38.469203|-1.309809|0
l'Albi|l'Albi||Garrigues|CAT|41.432974|0.923083|1
l'Albiol|l'Albiol||Baix Camp|CAT|41.245842|1.104203|1
L'Albir|L'Albir||Marina Baixa|PV|38.565644|-0.072189|0
l'Alcora|l'Alcora||Alcalatén|PV|40.065667|-0.216056|1
l'Alcoraia|l'Alcoraia|||PV|38.369554|-0.629631|0
l'Alcúdia|l'Alcúdia||Ribera Alta|PV|39.192806|-0.523959|1
l'Alcúdia|l'Alcúdia||Comtat|PV|38.756718|-0.434097|0
l'Alcúdia de Crespins|l'Alcúdia de Crespins||Costera|PV|38.971467|-0.604708|1
l'Alcúdia de Veo|Alcudia de Veo||Plana Baixa|PV|39.920812|-0.344841|1
l'Aldea|l'Aldea||Baix Ebre|CAT|40.748155|0.611304|1
l'Aldosa de Canillo|l'Aldosa de Canillo|||AD|42.5676|1.59756|0
l'Aldosa de la Massana|l'Aldosa de la Massana|||AD|42.54499|1.51483|0
l'Aleixar|l'Aleixar||Baix Camp|CAT|41.208357|1.050065|1
l'Alfàs del Pi|l'Alfàs del Pi||Marina Baixa|PV|38.57833|-0.088709|1
l'Algar|l'Algar||Marina Baixa|PV|38.657753|-0.098037|0
l'Alguenya|Algueña||Vinalopó Mitjà|PV|38.343569|-1.004102|1
l'Almadrava|l'Almadrava||Baix Camp|CAT|40.936732|0.856207|0
L'Almadrava|L'Almadrava||Alacantí|PV|38.437398|-0.383278|0
l'Almafrà|l'Almafrà||Marina Baixa|PV|38.558183|-0.098345|0
l'Almafrà|l'Almafrà||Vinalopó Mitjà|PV|38.469487|-0.78283|0
L'Almaixada|L'Almaixada||Alacantí|PV|38.402011|-0.465508|0
L'Alqueria|L'Alqueria||Plana Baixa|PV|39.785896|-0.156545|0
l'Alqueria d'Asnar|l'Alqueria d'Asnar||Comtat|PV|38.770245|-0.418828|1
l'Alqueria de la Comtessa|l'Alqueria de la Comtessa||Safor|PV|38.9341|-0.153371|1
l'Alquerieta dels Capellans|l'Alquerieta dels Capellans||Comtat|PV|38.785378|-0.42654|0
l'Altet|l'Altet||Baix Vinalopó|PV|38.268974|-0.538494|0
l'Alzina|l'Alzina||Noguera|CAT|41.865702|1.241932|0
l'Alzina d'Alinyà|l'Alzina d'Alinyà||Alt Urgell|CAT|42.189527|1.445882|0
l'Alzinar|l'Alzinar||Alt Penedès|CAT|41.424354|1.636709|0
l'Alzineta|l'Alzineta||Alt Camp|CAT|41.282669|1.383808|0
l'Alzineta|l'Alzineta||Alt Camp|CAT|41.282848|1.386396|0
L'Amerador|L'Amerador||Alacantí|PV|38.445661|-0.379588|0
L'Ametlla de Casserres|L'Ametlla de Casserres||Berguedà|CAT|42.04619|1.872682|0
l'Ametlla de Mar|l'Ametlla de Mar||Baix Ebre|CAT|40.907285|0.775641|1
L'Ametlla de Merola|L'Ametlla de Merola||Berguedà|CAT|41.908449|1.882721|0
l'Ametlla de Segarra|l'Ametlla de Segarra||Segarra|CAT|41.575315|1.239539|0
l'Ametlla del Montsec|l'Ametlla del Montsec||Noguera|CAT|42.013593|0.825525|0
l'Ametlla del Vallès|l'Ametlla del Vallès||Vallès Oriental|CAT|41.663541|2.256071|1
l'Ampolla|l'Ampolla||Baix Ebre|CAT|40.812443|0.690429|1
l'Amunt|l'Amunt||Baix Llobregat|CAT|41.410859|1.906243|0
l'Apeadero de Betxí|l'Apeadero de Betxí||Plana Baixa|PV|39.951246|-0.180183|0
l'Aranyó|l'Aranyó||Segarra|CAT|41.70544|1.21822|0
l'Arbocet|l'Arbocet||Baix Camp|CAT|41.105368|0.961356|0
l'Arboç|l'Arboç||Baix Penedès|CAT|41.273104|1.60659|1
L'Arboçar|L'Arboçar||Alt Penedès|CAT|41.327695|1.768898|0
l'Arboçar|l'Arboçar||Alt Camp|CAT|41.375485|1.422473|0
L'Arbre Sec|L'Arbre Sec||Alt Empordà|CAT|42.161265|3.044599|0
L'Ardiaca|L'Ardiaca||Baix Camp|CAT|41.063771|1.024408|0
l'Arenal|l'Arenal||Marina Alta|PV|38.770583|0.189699|0
l'Argentera|l'Argentera||Baix Camp|CAT|41.131202|0.903184|1
l'Argentina|l'Argentina||Menorca|IB|39.916493|4.173579|0
L'Argila|L'Argila||Camp de Túria|PV|39.593761|-0.526248|0
l'Argilaga|l'Argilaga||Tarragonès|CAT|41.211938|1.294776|0
l'Armentera|l'Armentera||Alt Empordà|CAT|42.165966|3.08501|1
l'Armiana|l'Armiana|||AD|42.5676|1.59756|0
l'Arquet|l'Arquet||Baix Penedès|CAT|41.293115|1.560308|0
l'Aspre|l'Aspre||Marina Alta|PV|38.783791|-0.053072|0
l'Atzúbia|l'Atzúbia||Marina Alta|PV|38.859031|-0.168732|1
l'Avellà|l'Avellà||Alt Penedès|CAT|41.441739|1.607351|0
L'Avencó|L'Avencó||Vallès Oriental|CAT|41.763243|2.256558|0
L'Eixample|L'Eixample||Vallès Oriental|CAT|41.563891|2.227221|0
L'Eixample|L'Eixample||Vallès Oriental|CAT|41.556296|2.230223|0
l'Eixample|l'Eixample||Tarragonès|CAT|41.172977|1.325762|0
l'Eixample Residencial|l'Eixample Residencial||Tarragonès|CAT|41.189145|1.466287|0
l'Eliana|l'Eliana||Camp de Túria|PV|39.562382|-0.525931|1
l'Enllaç|l'Enllaç||Selva|CAT|41.773926|2.673096|0
L'Entredós|L'Entredós||Alacantí|PV|38.395968|-0.462458|0
l'Era Blanca|l'Era Blanca||Plana Baixa|PV|39.965816|-0.250431|0
l'Ermita|l'Ermita||Marina Baixa|PV|38.524482|-0.236909|0
l'Ermita de Marxuquera|l'Ermita de Marxuquera||Safor|PV|38.968247|-0.249058|0
l'Escala|l'Escala||Alt Empordà|CAT|42.121996|3.122717|1
L'Escorxador|L'Escorxador||Vallès Oriental|CAT|41.567429|2.231067|0
L'Esparra|L'Esparra||Selva|CAT|41.826294|2.657958|0
l'Espelt|l'Espelt||Anoia|CAT|41.599477|1.591331|0
l'Esperit Sant|l'Esperit Sant|||CX|38.469203|-1.309809|0
l'Esplai del Penedès|l'Esplai del Penedès||Baix Penedès|CAT|41.25911|1.463508|0
l'Esplai Tarragoní|l'Esplai Tarragoní||Tarragonès|CAT|41.168163|1.278512|0
l'Espluga Calba|l'Espluga Calba||Garrigues|CAT|41.490939|1.005665|1""",
        """l'Espluga de Francolí|l'Espluga de Francolí||Conca de Barberà|CAT|41.421214|1.104055|1
l'Espunyola|l'Espunyola||Berguedà|CAT|42.044237|1.75305|1
L'Espà|L'Espà||Berguedà|CAT|42.21816|1.693573|0
l'Esquerda|Lesquerde||la Fenolleda|CN|42.793872|2.543381|1
l'Esquerda|l'Esquerda||Osona|CAT|41.975152|2.312597|0
l'Esquirol|l'Esquirol||Osona|CAT|42.039289|2.360287|1
l'Estació|l'Estació||Vallès Oriental|CAT|41.717357|2.539683|0
L'Estació|L'Estació||Bages|CAT|41.732895|1.699118|0
L'Estació|L'Estació||Vallès Occidental|CAT|41.602149|1.895333|0
l'Estació|l'Estació||Conca de Barberà|CAT|41.400461|1.105383|0
l'Estany|l'Estany||Moianès|CAT|41.871677|2.109682|1
l'Estany|l'Estany||Baix Ebre|CAT|40.869101|0.787928|0
l'Estany|l'Estany||Ribera Baixa|PV|39.128604|-0.23409|0
l'Estartit|l'Estartit||Baix Empordà|CAT|42.052806|3.196662|0
L'Estrada|L'Estrada||Alt Empordà|CAT|42.39227|2.867739|0
l'Estrella|l'Estrella||Maresme|CAT|41.583686|2.569516|0
l'Hidalgo|l'Hidalgo||Baix Ebre|CAT|40.859252|0.774355|0
L'Hort del Bisbe|L'Hort del Bisbe||Maresme|CAT|41.579069|2.544483|0
l'Horta|l'Horta||Mallorca|IB|39.777829|2.711296|0
L'Horta|L'Horta||Alacantí|PV|38.411568|-0.47694|0
l'Hospitalet de l'Infant|l'Hospitalet de l'Infant||Baix Camp|CAT|40.991206|0.922802|0
l'Hospitalet de Llobregat|l'Hospitalet de Llobregat||Barcelonès|CAT|41.358233|2.112214|1
l'Hostal|l'Hostal||Baix Penedès|CAT|41.297828|1.541094|0
l'Hostal Nou i la Codosa|l'Hostal Nou i la Codosa||Noguera|CAT|41.787122|0.826054|0
L'Hostalet|L'Hostalet||Selva|CAT|41.74759|2.82026|0
L'Hostalnou de Bianya|L'Hostalnou de Bianya||Garrotxa|CAT|42.210957|2.455822|0
l'Hostalnou de Llierca|l'Hostalnou de Llierca||Garrotxa|CAT|42.212623|2.611318|0
l'illa de Tabarca|l'illa de Tabarca|||PV|38.382222|-0.537562|0
L'Obrera|L'Obrera||Alacantí|PV|38.421189|-0.468597|0
L'Oliva|L'Oliva||Alt Empordà|CAT|42.24664|2.995249|0
l'Olla|l'Olla||Marina Baixa|PV|38.617525|-0.034699|0
l'Olleria|l'Olleria||Vall d'Albaida|PV|38.914595|-0.544966|1
l'Ombria de la Sarsa|l'Ombria de la Sarsa|||CX|38.254955|-1.051719|0
l'Ortigós|l'Ortigós||Baix Penedès|CAT|41.283476|1.518361|0
l'Orxa|l'Orxa/Lorcha||Comtat|PV|38.849685|-0.308657|1
l'Àlia|l'Àlia||Baix Ebre|CAT|40.849509|0.76115|0
l'Énova|l'Énova||Ribera Alta|PV|39.042551|-0.464937|1
La Almeza|La Almeza||Serrans|PV|39.93814|-0.997531|0
La Alquería|La Alquería||Alt Millars|PV|40.063778|-0.51475|0
La Aparecida|La Aparecida||Baix Segura|PV|38.080527|-1.00832|0
La Arnella|La Arnella||Marina Alta|PV|38.697746|0.136619|0
La Artejuela|La Artejuela||Alt Millars|PV|40.105451|-0.498379|0
La Avella|La Avella||Alt Maestrat|PV|40.501374|0.00799|0
La Barceloneta|La Barceloneta||Baix Llobregat|CAT|41.335078|1.910019|0
La Barceloneta|La Barceloneta||Baix Empordà|CAT|41.937486|3.13174|0
la Barceloneta|la Barceloneta||Segrià|CAT|41.612122|0.74469|0
La Barona|La Barona||Plana Alta|PV|40.166391|-0.00639|0
la Baronia de Rialb|la Baronia de Rialb||Noguera|CAT|42.027566|1.181273|1
la Baronia de Sant Oïsme|la Baronia de Sant Oïsme||Noguera|CAT|41.997969|0.844739|0
la Baronia del Mar|la Baronia del Mar||Baix Penedès|CAT|41.216846|1.562946|0
La Barquera|La Barquera||Tarragonès|CAT|41.16753|1.461141|0
la Barraca d'Aigües Vives|la Barraca d'Aigües Vives||Ribera Alta|PV|39.099781|-0.370493|0
la Barraca d'Aigües Vives|la Barraca d'Aigües Vives||Ribera Alta|PV|39.09784|-0.372299|0
la Bassa Blanca|la Bassa Blanca||Baix Llobregat|CAT|41.331636|1.941209|0
la Basseta|la Basseta||Plana Alta|PV|40.158173|-0.070375|0
la Bastida|La Bastide||el Rosselló|CN|42.537187|2.571709|1
la Bastida d'Hortons|la Bastida d'Hortons||Alt Urgell|CAT|42.327732|1.471655|0
la Bastida de Sort|la Bastida de Sort||Pallars Sobirà|CAT|42.425352|1.131119|0
la Bateria|la Bateria||Alt Empordà|CAT|42.363381|3.156068|0
La Batllòria|La Batllòria||Vallès Oriental|CAT|41.719088|2.552532|0
la Bauma|la Bauma||Bages|CAT|41.622492|1.86694|0
La Beguda Alta|La Beguda Alta||Anoia|CAT|41.497367|1.840085|0
la Beguda Alta|la Beguda Alta||Baix Llobregat|CAT|41.498482|1.841548|0
La Beguda Alta|La Beguda Alta||Alt Penedès|CAT|41.492316|1.841384|0
La Beguda Baixa|La Beguda Baixa||Alt Penedès|CAT|41.486518|1.848883|0
la Bisbal d'Empordà|la Bisbal d'Empordà||Baix Empordà|CAT|41.944101|3.042975|1
La Bisbal de Falset|La Bisbal de Falset||Priorat|CAT|41.280384|0.723178|0
la Bisbal de Montsant|la Bisbal de Montsant||Priorat|CAT|41.282486|0.721047|1
la Bisbal del Penedès|la Bisbal del Penedès||Baix Penedès|CAT|41.280444|1.481326|1
La Bleda|La Bleda||Alt Penedès|CAT|41.344615|1.654719|0
la Bolleria-Sobrestany|la Bolleria-Sobrestany||Baix Empordà|CAT|42.079334|3.102757|0
la Bonaigua de Baix|la Bonaigua de Baix||Pallars Sobirà|CAT|42.637455|1.045345|0
La Bordeta|La Bordeta||Baix Empordà|CAT|41.955661|3.012025|0
La Bordeta|La Bordeta||Baix Empordà|CAT|41.957333|3.047188|0
la Borinquen|la Borinquen||Alacantí|PV|38.402512|-0.516981|0
La Botjosa|La Botjosa||Bages|CAT|41.811103|1.896956|0
La Burquera|La Burquera||Alt Camp|CAT|41.251593|1.158184|0
La Bóbila|La Bóbila||Pla de l'Estany|CAT|42.09792|2.765134|0
La Caballusa|La Caballusa||Vinalopó Mitjà|PV|38.382985|-1.082668|0
la Cabana|la Cabana||Alt Camp|CAT|41.27712|1.196975|0
la Cabanassa|La Cabanasse||l'Alta Cerdanya|CN|42.501954|2.106354|1
La Cabezuela|La Cabezuela||Vall de Cofrents|PV|39.31921|-1.040359|0
la Cadeneta|la Cadeneta||Alt Camp|CAT|41.296716|1.078745|0
La Cala|La Cala||Marina Baixa|PV|38.534205|-0.171748|0
la Cala Nova|la Cala Nova||Baix Ebre|CAT|40.89411|0.816985|0
La Campaneta|La Campaneta||Alacantí|PV|38.376316|-0.553597|0
La Campaneta|La Campaneta||Baix Segura|PV|38.087429|-0.894526|0
La Canaleja|La Canaleja||Serrans|PV|39.940763|-0.994433|0
La Canalosa|La Canalosa||Vinalopó Mitjà|PV|38.305416|-0.934615|0
la Canonja|la Canonja||Tarragonès|CAT|41.114809|1.180985|1
La Canya|La Canya||Garrotxa|CAT|42.208939|2.491348|0
La Canya|La Canya||Garrotxa|CAT|42.204564|2.496895|0
la Canyada|Cañada||Alt Vinalopó|PV|38.685559|-0.821741|1
la Canyada de Beric|Cañada de Verich, La||el Matarranya|FR|40.856281|-0.100591|1
La Canyada de Confites|La Canyada de Confites||Camp de Túria|PV|39.637809|-0.565674|0
la Canyada de l'Alenya|la Canyada de l'Alenya|Cañada de la Leña||CX|38.31719|-1.055405|0
La Canyada de Tello|La Canyada de Tello||Camp de Túria|PV|39.661252|-0.581991|0
la Canyada del Fenollar|la Canyada del Fenollar|||PV|38.382222|-0.537562|0
la Canyada del Trigo|la Canyada del Trigo|Cañada del Trigo||CX|38.348042|-1.091403|0
La Canyera|La Canyera||Gironès|CAT|41.82849|2.870651|0
La Capitana|La Capitana||Marina Baixa|PV|38.591395|-0.108945|0
la Cardosa|la Cardosa||Segarra|CAT|41.700021|1.25267|0
La Carena Llarga|La Carena Llarga||Vallès Occidental|CAT|41.598719|1.930335|0
la Carretera|la Carretera||Gironès|CAT|41.920212|2.850595|0
La Carroja|La Carroja||Marina Alta|PV|38.820102|-0.242771|0
la Carronya|la Carronya||Baix Penedès|CAT|41.314685|1.568835|0
La Casa Nova|La Casa Nova||Baix Empordà|CAT|41.786092|2.996558|0
La Caseta|La Caseta||Vallès Oriental|CAT|41.583063|2.212676|0
La Caseta Pigrau|La Caseta Pigrau||Ripollès|CAT|42.187918|2.194412|0
La Catalana|La Catalana||Bages|CAT|41.71609|1.81401|0
la Cativera|la Cativera||Tarragonès|CAT|41.187552|1.339517|0
La Cañada|La Cañada||Alacantí|PV|38.397602|-0.577063|0
La Cañada|La Cañada||Plana d'Utiel|PV|39.60113|-1.067838|0
la Cellera de Ter|la Cellera de Ter||Selva|CAT|41.971215|2.600094|1
la Central de Capdella|la Central de Capdella||Pallars Jussà|CAT|42.467053|0.991025|0
la Cerdera|la Cerdera||Segrià|CAT|41.663908|0.52718|0
la Cirera|la Cirera||Conca de Barberà|CAT|41.552643|1.330288|0
La Cisa|La Cisa||Maresme|CAT|41.50929|2.350663|0
La Ciutat del Remei|La Ciutat del Remei||Baix Llobregat|CAT|41.384841|1.979089|0
la Ciutat Jardí|la Ciutat Jardí||Maresme|CAT|41.660794|2.706326|0
la Clua|la Clua||Noguera|CAT|41.947021|1.016785|0
la Clua|la Clua||Pallars Jussà|CAT|42.096721|0.725591|0
la Codonyera|Codoñera, La||el Matarranya|FR|40.928352|-0.05891|1
La Cogullada|La Cogullada||Garraf|CAT|41.29732|1.712653|0
La Colonia Santa Eulalia|La Colonia Santa Eulalia||Alt Vinalopó|PV|38.56754|-0.84995|0
la Colònia Alcanís|la Colònia Alcanís||Segrià|CAT|41.702011|0.599808|0
la Colònia Castelltort|la Colònia Castelltort||Vallès Occidental|CAT|41.516786|2.091468|0
La Colònia del Carme|La Colònia del Carme||Vallès Occidental|CAT|41.476962|1.940474|0
la Colònia Estabanell|la Colònia Estabanell||Ripollès|CAT|42.294144|2.359864|0
la Colònia Güell|la Colònia Güell||Baix Llobregat|CAT|41.362996|2.025533|0
la Colònia Imbern|la Colònia Imbern||Osona|CAT|42.056934|2.245502|0
la Colònia Jorba|la Colònia Jorba||Moianès|CAT|41.765914|1.923545|0
la Colònia Lacambra|la Colònia Lacambra||Osona|CAT|42.037723|2.238634|0
La Colònia Noguera|La Colònia Noguera||Ripollès|CAT|42.214983|2.170893|0
la Colònia Oller|la Colònia Oller||Osona|CAT|41.757315|2.245999|0
la Colònia Sedó|la Colònia Sedó||Baix Llobregat|CAT|41.549879|1.870882|0
La Colònia Sorribes|La Colònia Sorribes||Ripollès|CAT|42.204379|2.178138|0
La Coma|La Coma||Alt Empordà|CAT|42.134012|3.112307|0
La Coma|La Coma||Alt Empordà|CAT|42.355129|3.166647|0
la Coma|la Coma||Solsonès|CAT|42.175235|1.591022|0
la Coma|la Coma||Tarragonès|CAT|41.175293|1.435486|0
La Coma|La Coma||Tarragonès|CAT|41.174626|1.347573|0
La Coma|La Coma||Horta Nord|PV|39.523685|-0.432499|0
La Coma|La Coma||Horta Sud|PV|39.328144|-0.450694|0
la Coma i la Pedra|la Coma i la Pedra||Solsonès|CAT|42.176051|1.583134|1
la Cometa|la Cometa||Garrotxa|CAT|42.22444|2.578411|0
La Conarda|La Conarda||Camp de Túria|PV|39.58877|-0.504615|0
la Conreria|la Conreria||Vallès Oriental|CAT|41.491391|2.246664|0
La Coromina|La Coromina||Bages|CAT|41.90831|1.69577|0
La Corona|La Corona||Cerdanya|CAT|42.411944|1.884413|0
La Corona|La Corona||Safor|PV|38.895644|-0.21754|0
la Cortinada|la Cortinada|||AD|42.55623|1.53319|0
La Costa|La Costa||Gironès|CAT|42.021079|2.914546|0
La Costa|La Costa||Alt Empordà|CAT|42.311985|3.216076|0
La Costa de la Perdiu|La Costa de la Perdiu||Baix Llobregat|CAT|41.413449|1.936663|0
la Costa del Montseny|la Costa del Montseny||Vallès Oriental|CAT|41.747138|2.418115|0
La Costeta|La Costeta||Baix Llobregat|CAT|41.330829|1.913464|0
la Coveta Fumada|la Coveta Fumada||Alacantí|PV|38.451132|-0.36325|0
La Creu|La Creu||Vallès Occidental|CAT|41.604197|1.868099|0
La Creu de Codines|La Creu de Codines||Osona|CAT|41.972291|2.300823|0
la Creu de l'Aragall|la Creu de l'Aragall||Baix Llobregat|CAT|41.428582|1.906108|0
la Creu de Lloret|la Creu de Lloret||Selva|CAT|41.740527|2.823042|0
la Creu de Sussalba|la Creu de Sussalba||Baix Llobregat|CAT|41.439506|1.960241|0
La Creueta|La Creueta||Maresme|CAT|41.62032|2.537674|0
La Creueta|La Creueta||Gironès|CAT|41.964974|2.839279|0
La Cruïlla|La Cruïlla||Vallès Oriental|CAT|41.6279|2.234682|0
La Cuevarruz|La Cuevarruz||Serrans|PV|39.935292|-0.980317|0
La Cuevarruz|La Cuevarruz||Serrans|PV|39.934965|-0.978443|0
la Curullada|la Curullada||Segarra|CAT|41.668103|1.228793|0
La Curva|La Curva||Baix Maestrat|PV|40.248007|0.28158|0
La Devesa de Saga|La Devesa de Saga||Cerdanya|CAT|42.412939|1.854713|0
la Drova|la Drova||Safor|PV|39.007057|-0.28286|0
La Empelta|La Empelta||Camp de Túria|PV|39.663035|-0.450611|0
La Encina|La Encina||Alt Vinalopó|PV|38.765489|-0.953434|0
La Eralta|La Eralta||Baix Segura|PV|38.116981|-0.78306|0
La Ermita|La Ermita||Serrans|PV|39.647666|-0.884506|0
La Estación|La Estación||Baix Vinalopó|PV|38.236083|-0.770702|0
La Estrada|La Estrada||Marina Baixa|PV|38.574077|-0.07774|0
La Farga|La Farga||Selva|CAT|41.957964|2.64711|0
La Farga de Bebié|La Farga de Bebié||Osona|CAT|42.130178|2.205688|0
la Farga de Bebié|la Farga de Bebié||Ripollès|CAT|42.130549|2.20719|0
la Farga de Moles|la Farga de Moles||Alt Urgell|CAT|42.431694|1.467084|0
la Farinera|la Farinera||Bages|CAT|41.621956|1.891818|0
La Farinera|La Farinera||Vallès Occidental|CAT|41.620357|1.8953|0
La Farrès|La Farrès||Pla de l'Estany|CAT|42.14779|2.779651|0
la Fatarella|la Fatarella||Terra Alta|CAT|41.171325|0.469093|1
la Febró|la Febró||Baix Camp|CAT|41.275161|0.998244|1
La Ferratgeta|La Ferratgeta||Cerdanya|CAT|42.422845|1.884358|0
La Ferreria|La Ferreria||Vallès Occidental|CAT|41.685786|2.100989|0
la Figuera|la Figuera||Priorat|CAT|41.201117|0.712051|1
La Figuerola|La Figuerola||Bages|CAT|41.839336|1.83157|0
la Figuerosa|la Figuerosa||Urgell|CAT|41.696705|1.168368|0
La Flor del Camp|La Flor del Camp||Baix Camp|CAT|41.190558|1.100955|0
la Floresta|la Floresta||Garrigues|CAT|41.497287|0.924226|1
La Floresta|La Floresta||Vallès Occidental|CAT|41.44684|2.078755|0
La Floresta|La Floresta||Maresme|CAT|41.49978|2.357907|0
la Floresta|la Floresta||Tarragonès|CAT|41.125305|1.21228|0
La Florida|La Florida||Vallès Occidental|CAT|41.523054|2.187808|0
La Foia|La Foia||Baix Vinalopó|PV|38.214021|-0.684191|0
La Foia|La Foia||Marina Alta|PV|38.786861|-0.055741|0
La Foia de l'Alcalaten|La Foia de l'Alcalaten||Alcalatén|PV|40.103797|-0.208958|0
la Font d'en Carròs|la Font d'en Carròs||Safor|PV|38.914635|-0.163056|1
la Font d'en Segures|la Font d'en Segures||Alt Maestrat|PV|40.36731|-0.137878|0
La Font de L'Espardenyera|La Font de L'Espardenyera||Vallès Occidental|CAT|41.590869|2.03122|0
la Font de la Figuera|la Font de la Figuera||Costera|PV|38.825725|-0.892414|1
la Font de la Gavatxa|la Font de la Gavatxa||Baix Penedès|CAT|41.20978|1.434287|0
la Font de la Reina|Fuente la Reina||Alt Millars|PV|40.057051|-0.609091|1
la Font de Quinto|la Font de Quinto||Baix Ebre|CAT|40.753723|0.565125|0
La Font del Còdol|La Font del Còdol||Baix Llobregat|CAT|41.561887|1.814682|0
La Font del Montnegre|La Font del Montnegre||Maresme|CAT|41.654383|2.569756|0
la Fontsanta i Can Fatjó|la Fontsanta i Can Fatjó||Baix Llobregat|CAT|41.359138|2.064884|0
La Fortesa|La Fortesa||Anoia|CAT|41.453888|1.76257|0
la Freixneda|Fresneda, La||el Matarranya|FR|40.927813|0.067014|1
la Fuliola|la Fuliola||Urgell|CAT|41.716294|1.001531|1
La Fumada|La Fumada||Baix Llobregat|CAT|41.554867|1.830258|0
La Fàbrica|La Fàbrica||Bages|CAT|41.676621|1.848455|0
la Galera|la Galera||Montsià|CAT|40.694613|0.439844|1
La Galia|La Galia||Baix Vinalopó|PV|38.28445|-0.689407|0
la Garriga|la Garriga||Vallès Oriental|CAT|41.690308|2.281837|1
La Garriga|La Garriga||Alt Empordà|CAT|42.275663|3.1535|0
La Garrofa|La Garrofa||Alt Penedès|CAT|41.390147|1.741266|0
La Garrofera|La Garrofera||Ribera Alta|PV|39.155601|-0.567827|0
La Gaviota|La Gaviota||Garraf|CAT|41.202424|1.643134|0
la Ginebrosa|Ginebrosa, La||el Matarranya|FR|40.857556|-0.181294|1
La Gleva|La Gleva||Osona|CAT|41.994922|2.240288|0
La Gloria|La Gloria||Alt Vinalopó|PV|38.755352|-0.905898|0
La Gloria-El Calvari-Gialma|La Gloria-El Calvari-Gialma||Alacantí|PV|38.406238|-0.451804|0
La Goba|La Goba||Selva|CAT|41.797879|2.807162|0
la Gornal|la Gornal||Alt Penedès|CAT|41.254105|1.593122|0
la Granada|la Granada||Alt Penedès|CAT|41.376886|1.722443|1
la Granadella|la Granadella||Garrigues|CAT|41.365842|0.650643|1
La Granja|La Granja||Tarragonès|CAT|41.121943|1.214813|0
la Granja d'Escarp|la Granja d'Escarp||Segrià|CAT|41.405364|0.373978|1
la Granja de la Costera|la Granja de la Costera||Costera|PV|38.991144|-0.558498|1
la Granja de Rocamora|Granja de Rocamora||Baix Segura|PV|38.154704|-0.894604|1
La Grua|La Grua||Anoia|CAT|41.512902|1.764486|0
la Guia|la Guia||Bages|CAT|41.718011|1.829727|0
la Guinardera|la Guinardera||Tarragonès|CAT|41.166035|1.30689|0
La Guingueta|La Guingueta||Cerdanya|CAT|42.43332|1.938271|0
la Guingueta d'Ix|Bourg-Madame||l'Alta Cerdanya|CN|42.438365|1.960631|1
la Guingueta d'Àneu|la Guingueta d'Àneu||Pallars Sobirà|CAT|42.602947|1.153737|1
La Guàrdia|La Guàrdia||Alt Penedès|CAT|41.386999|1.793155|0
la Guàrdia|la Guàrdia||Urgell|CAT|41.721466|1.056687|0
la Guàrdia d'Ares|la Guàrdia d'Ares|Guàrdia d'Ares|Alt Urgell|CAT|42.285885|1.234331|0
la Guàrdia dels Prats|la Guàrdia dels Prats||Conca de Barberà|CAT|41.397479|1.171033|0
la Guàrdia Lada|la Guàrdia Lada||Segarra|CAT|41.589358|1.294741|0
la Iessa|La Yesa||Serrans|PV|39.90215|-0.944587|1
La Incolla|La Incolla||Foia de Bunyol|PV|39.498767|-0.750461|0
la Jana|la Jana||Baix Maestrat|PV|40.502284|0.238572|1
la Joliesa|la Joliesa||Camp de Túria|PV|39.670623|-0.614164|0""",
        """la Joncosa del Montmell|la Joncosa del Montmell||Baix Penedès|CAT|41.315573|1.453545|0
la Jonquera|la Jonquera||Alt Empordà|CAT|42.43964|2.903292|1
la Llacuna|la Llacuna||Anoia|CAT|41.469326|1.538758|1
la Llacuneta|la Llacuneta||Baix Penedès|CAT|41.286799|1.626582|0
la Llagosta|la Llagosta||Vallès Oriental|CAT|41.514406|2.203177|1
la Llaguna|La Llagonne||el Capcir|CN|42.537531|2.094023|1
La Llastra|La Llastra||Ribera Baixa|PV|39.269534|-0.271169|0
La Llibra Casanova|La Llibra Casanova||Baix Llobregat|CAT|41.385188|1.945589|0
la Llobregosa|la Llobregosa||Vallès Oriental|CAT|41.732207|2.506585|0
La Llosa|La Llosa||Plana Baixa|PV|39.767179|-0.202431|1
La Llosa|La Llosa||Baix Camp|CAT|41.065303|1.047605|0
la Llosa de Camacho|la Llosa de Camacho||Marina Alta|PV|38.772064|-0.007375|0
la Llosa de Ranes|la Llosa de Ranes||Costera|PV|39.027975|-0.537201|1
la Llosa del Bisbe|Losa del Obispo||Serrans|PV|39.69851|-0.865105|1
La Loberuela|La Loberuela||Plana d'Utiel|PV|39.670785|-1.336136|0
La Magina|La Magina||Baix Llobregat|CAT|41.414952|1.991814|0
La Maimona-Corral d'Albert|La Maimona-Corral d'Albert||Camp de Túria|PV|39.650216|-0.555664|0
La Malla|La Malla||Horta Sud|PV|39.496224|-0.516069|0
La Mandarina|La Mandarina||Marina Alta|PV|38.750034|0.146002|0
La Manguilla|La Manguilla||Camp de Túria|PV|39.641755|-0.515273|0
la Margineda|la Margineda|||AD|42.50779|1.52109|0
La Marina|La Marina||Baix Vinalopó|PV|38.147954|-0.642319|0
la Marina de Creixell|la Marina de Creixell||Tarragonès|CAT|41.162856|1.456347|0
La Martorella|La Martorella||Tarragonès|CAT|41.191995|1.455956|0
La Masieta|La Masieta||Baix Penedès|CAT|41.264719|1.45547|0
la Masieta de Salort|la Masieta de Salort||Tarragonès|CAT|41.180906|1.288217|0
la Massana|la Massana|||AD|42.54499|1.51483|0
la Masó|la Masó||Alt Camp|CAT|41.237836|1.220265|1
la Masó|la Masó||Tarragonès|CAT|41.165844|1.438127|0
La Mata|La Mata||Baix Segura|PV|38.020614|-0.65594|0
la Mata de Morella|la Mata de Morella||Ports|PV|40.611483|-0.277713|1
la Mata de Pinyana|la Mata de Pinyana||Segrià|CAT|41.754589|0.586535|0
La Matanza|La Matanza||Baix Segura|PV|38.133275|-1.006861|0
La Melusa|La Melusa||la Llitera|FR|41.780463|0.374784|0
la Menera|Lamanère||el Vallespir|CN|42.357698|2.519772|1
La Merced|La Merced||Alacantí|PV|38.471987|-0.325032|0
la Miliana|la Miliana||Montsià|CAT|40.639571|0.415619|0
la Miralba|la Miralba||Baix Penedès|CAT|41.263515|1.491433|0
La Miranda del Vallès|La Miranda del Vallès||Vallès Oriental|CAT|41.580667|2.223286|0
la Moixeta|la Moixeta||Baix Penedès|CAT|41.359825|1.51806|0
La Mojonera|La Mojonera||Foia de Bunyol|PV|39.409385|-0.647379|0
la Molassa|la Molassa||Maresme|CAT|41.502436|2.315033|0
La Molina|La Molina||Cerdanya|CAT|42.341888|1.951071|0
la Molsosa|la Molsosa||Solsonès|CAT|41.784104|1.542061|1
La Monjoia|La Monjoia||Moianès|CAT|41.852231|2.130799|0
La Mont-Ravana|La Mont-Ravana||Camp de Túria|PV|39.679591|-0.669818|0
La Montañeta|La Montañeta||Camp de Túria|PV|39.567073|-0.550243|0
La Monzona|La Monzona||Alt Millars|PV|40.121717|-0.609093|0
la Morana|la Morana||Segarra|CAT|41.782324|1.263143|0
la Morera de Montsant|la Morera de Montsant||Priorat|CAT|41.269051|0.837225|1
la Mosquera|la Mosquera|||AD|42.53474|1.58014|0
La Muntanya Rodona|La Muntanya Rodona||Alt Penedès|CAT|41.375273|1.816804|0
La Muntanyeta|La Muntanyeta||Vallès Occidental|CAT|41.603362|2.027399|0
La Murada|La Murada||Baix Segura|PV|38.171385|-0.961486|0
La Murta|La Murta||Alacantí|PV|38.450878|-0.600538|0
la Móra|la Móra||Segarra|CAT|41.650471|1.193317|0
la Móra|la Móra||Tarragonès|CAT|41.132691|1.344116|0
La Múnia|La Múnia||Alt Penedès|CAT|41.325582|1.618353|0
la Nou de Berguedà|la Nou de Berguedà||Berguedà|CAT|42.177234|1.891121|1
la Nou de Gaià|la Nou de Gaià||Tarragonès|CAT|41.185791|1.379242|1
la Nucia|la Nucia||Marina Baixa|PV|38.601112|-0.12024|1
La Palanca|La Palanca||Alt Empordà|CAT|42.128458|3.120224|0
la Palma d'Ebre|la Palma d'Ebre||Ribera d'Ebre|CAT|41.270643|0.663841|1
la Palma de Cervelló|la Palma de Cervelló||Baix Llobregat|CAT|41.410206|1.963272|1
La Papiola|La Papiola||Baix Penedès|CAT|41.244879|1.514583|0
La Parreta|La Parreta||Plana Alta|PV|40.047733|0.015231|0
la Parròquia d'Hortó|la Parròquia d'Hortó||Alt Urgell|CAT|42.325241|1.375809|0
La Paz|La Paz||Camp de Morvedre|PV|39.674716|-0.328653|0
La Paz|La Paz||Horta Sud|PV|39.469394|-0.427076|0
La Pedralba|La Pedralba||Vallès Oriental|CAT|41.740316|2.266885|0
La Pedrera|La Pedrera||Marina Alta|PV|38.823285|0.095592|0
La Penyora|La Penyora||Moianès|CAT|41.783507|2.165712|0
la Pera|la Pera||Baix Empordà|CAT|42.017477|2.971297|1
La Pera i Pont Martí|La Pera i Pont Martí||Alt Empordà|CAT|42.243583|3.034899|0
La Perleta|La Perleta||Baix Vinalopó|PV|38.250282|-0.632689|0
la Perpinyana|la Perpinyana||Pla de l'Estany|CAT|42.10194|2.746309|0
La Peña|La Peña||Camp de Morvedre|PV|39.681066|-0.337618|0
La Pineda|La Pineda||Vallès Oriental|CAT|41.665717|2.190801|0
La Pineda|La Pineda||Anoia|CAT|41.734313|1.526359|0
La Pineda|La Pineda||Alt Penedès|CAT|41.442143|1.731564|0
La Pineda|La Pineda||Baix Empordà|CAT|41.85343|3.116275|0
la Pineda|la Pineda||Tarragonès|CAT|41.078742|1.178159|0
la Pineda de Santa Cristina|la Pineda de Santa Cristina||Baix Penedès|CAT|41.28103|1.437196|0
la Pineda Feu|la Pineda Feu||Vallès Oriental|CAT|41.62753|2.232231|0
La Pinetella|La Pinetella||Baix Llobregat|CAT|41.375324|1.902436|0
La Pinya|La Pinya||Garrotxa|CAT|42.174358|2.44365|0
La Plana|La Plana||Berguedà|CAT|42.052821|1.871707|0
La Plana|La Plana||Vallès Occidental|CAT|41.605277|2.0228|0
La Plana|La Plana||Alt Camp|CAT|41.296018|1.19704|0
la Plana|la Plana||Tarragonès|CAT|41.168589|1.455609|0
la Plana|la Plana||Tarragonès|CAT|41.129911|1.128449|0
la Plana d'en Berga|la Plana d'en Berga||Alt Camp|CAT|41.29994|1.23212|0
La Plana de les Torres|La Plana de les Torres||Alt Penedès|CAT|41.421242|1.548667|0
la Plana de Mont-ros|la Plana de Mont-ros||Pallars Jussà|CAT|42.381706|0.964782|0
la Plana del Pont Nou|la Plana del Pont Nou||Bages|CAT|41.728834|1.807162|0
La Planassa|La Planassa||Vallès Occidental|CAT|41.561847|1.953662|0
la Planeta|la Planeta||Alt Camp|CAT|41.380568|1.363266|0
La Planota|La Planota||Bages|CAT|41.756479|1.899884|0
la Platja d'Orpesa|la Platja d'Orpesa||Plana Alta|PV|40.090148|0.143395|0
la Platja de Calafell|la Platja de Calafell||Baix Penedès|CAT|41.187778|1.573056|0
La Pleta|La Pleta||Cerdanya|CAT|42.41929|1.868996|0
La Pleta de Saga|La Pleta de Saga||Cerdanya|CAT|42.419886|1.863122|0
La Pobla d'Alcolea|La Pobla d'Alcolea||Ports|PV|40.726484|-0.082012|0
la Pobla d'Arenós|Puebla de Arenoso||Alt Millars|PV|40.102666|-0.580758|1
la Pobla de Benifassà|la Pobla de Benifassà||Baix Maestrat|PV|40.685837|0.165255|1
la Pobla de Carivenys|la Pobla de Carivenys||Conca de Barberà|CAT|41.569521|1.440917|0
la Pobla de Claramunt|la Pobla de Claramunt||Anoia|CAT|41.558314|1.681167|1
la Pobla de Cérvoles|la Pobla de Cérvoles||Garrigues|CAT|41.381214|0.882252|1
la Pobla de Farnals|la Pobla de Farnals||Horta Nord|PV|39.57265|-0.310005|1
la Pobla de Lillet|la Pobla de Lillet||Berguedà|CAT|42.229486|1.975444|1
la Pobla de Mafumet|la Pobla de Mafumet||Tarragonès|CAT|41.182683|1.212994|1
la Pobla de Massaluca|la Pobla de Massaluca||Terra Alta|CAT|41.193385|0.34781|1
la Pobla de Montornès|la Pobla de Montornès||Tarragonès|CAT|41.183069|1.404945|1
la Pobla de Roda|La Puebla de Roda||la Ribagorça|FR|42.302829|0.54244|0
la Pobla de Sant Miquel|Puebla de San Miguel||Racó d'Ademús|PV|40.055797|-1.132231|1
la Pobla de Segur|la Pobla de Segur||Pallars Jussà|CAT|42.270626|0.980692|1
la Pobla de Vallbona|la Pobla de Vallbona||Camp de Túria|PV|39.601945|-0.542448|1
la Pobla del Duc|la Pobla del Duc||Vall d'Albaida|PV|38.905232|-0.423734|1
la Pobla Llarga|la Pobla Llarga||Ribera Alta|PV|39.081603|-0.472413|1
la Pobla Tornesa|la Pobla Tornesa||Plana Alta|PV|40.103476|-0.001221|1
La Pobleta|La Pobleta||Serrans|PV|39.837664|-0.823974|0
la Pobleta de Bellveí|la Pobleta de Bellveí||Pallars Jussà|CAT|42.343995|0.960308|0
La Ponderosa|La Ponderosa||Alt Penedès|CAT|41.423801|1.48672|0
La Porcha|La Porcha||Alacantí|PV|38.456533|-0.369683|0
la Portella|la Portella||Segrià|CAT|41.733578|0.642639|1
la Portellada|Portellada, La||el Matarranya|FR|40.883996|0.06291|1
La Portera|La Portera||Plana d'Utiel|PV|39.401862|-1.102035|0
la Prenyanosa|la Prenyanosa||Segarra|CAT|41.711944|1.289141|0
La Presa|La Presa||Horta Sud|PV|39.52179|-0.511004|0
La Puebla de Roda|La Puebla de Roda||la Ribagorça|FR|42.302829|0.54244|0
La Puentecilla|La Puentecilla||Alt Vinalopó|PV|38.625604|-0.876228|0
la Puigmola|la Puigmola||Safor|PV|39.014276|-0.314884|0
La Punta|La Punta|||PV|39.420604|-0.358439|0
La Quadra|La Quadra||Tarragonès|CAT|41.159038|1.284309|0
La Quadra dels Manous|La Quadra dels Manous||Tarragonès|CAT|41.159319|1.286387|0
la Quar|la Quar||Berguedà|CAT|42.09698|1.965394|1
La Rabassa|La Rabassa||Segarra|CAT|41.647798|1.38262|0
La Rabeia|La Rabeia||Bages|CAT|41.874631|1.886231|0
la Raixa|la Raixa|||CX|38.469203|-1.309809|0
La Ral|La Ral||Ripollès|CAT|42.279731|2.371005|0
la Rata|la Rata||Anoia|CAT|41.547299|1.687061|0
La Rectoria|La Rectoria||Baix Llobregat|CAT|41.328571|1.943321|0
la Rectoria|la Rectoria||Alt Penedès|CAT|41.360146|1.673255|0
La Rellana|La Rellana||Baix Segura|PV|38.062169|-0.79172|0
La Renegà|La Renegà||Plana Alta|PV|40.063381|0.113937|0
La Reprimala|La Reprimala||Safor|PV|38.883286|-0.233127|0
la Riba|la Riba||Alt Camp|CAT|41.306826|1.1715|1
La Riera|La Riera||Maresme|CAT|41.632803|2.711883|0
la Riera de Gaià|la Riera de Gaià||Tarragonès|CAT|41.163651|1.355768|1
La Riviera|La Riviera||Selva|CAT|41.718383|2.881541|0
La Riviera|La Riviera||Baix Camp|CAT|41.027681|0.96396|0
La Roca|La Roca||Alt Penedès|CAT|41.381182|1.609723|0
La Roca|La Roca||Ripollès|CAT|42.322488|2.330874|0
la Roca d'Albera|Laroque-des-Albères||el Rosselló|CN|42.511612|2.93304|1
la Roca del Vallès|la Roca del Vallès||Vallès Oriental|CAT|41.596186|2.334833|1
La Rodonella|La Rodonella||Berguedà|CAT|42.163885|1.855143|0
la Romana|la Romana||Vinalopó Mitjà|PV|38.355162|-0.907853|1
La Romaneta|La Romaneta||Vinalopó Mitjà|PV|38.382373|-0.899091|0
La Romànica|La Romànica||Vallès Occidental|CAT|41.52329|2.127325|0
La Roqueta|La Roqueta||Baix Empordà|CAT|42.048405|3.111774|0
La Rosaleda de Bages|La Rosaleda de Bages||Bages|CAT|41.741671|1.85772|0
La Rovira|La Rovira||Alt Penedès|CAT|41.334906|1.652205|0
la Ràpita|la Ràpita||Montsià|CAT|40.603571|0.625186|1
La Ràpita|La Ràpita||Alt Penedès|CAT|41.310273|1.650394|0
la Ràpita|la Ràpita||Noguera|CAT|41.782807|0.843937|0
la Règola|la Règola||Noguera|CAT|41.995495|0.793345|0
La Sabatera|La Sabatera||Marina Alta|PV|38.701644|0.121789|0
La Sagrera|La Sagrera||Vallès Oriental|CAT|41.631033|2.303352|0
La Sagrera|La Sagrera||Vallès Occidental|CAT|41.584899|2.165428|0
La Sagrera|La Sagrera||Vallès Oriental|CAT|41.654562|2.223307|0
la Saira|la Saira||Segrià|CAT|41.71609|0.511991|0
la Sala|la Sala||Baix Empordà|CAT|42.034949|3.020251|0
la Salzadella|la Salzadella||Baix Maestrat|PV|40.417387|0.147384|1
la Santa Faç|la Santa Faç||Alacantí|PV|38.39149|-0.438621|0
la Santa Faç|la Santa Faç|||PV|38.389381|-0.443324|0
la Sarsa|la Sarsa|La Zarza||CX|38.345791|-1.142198|0
la Savina|la Savina||Formentera|IB|38.730204|1.418356|0
la Secuita|la Secuita||Tarragonès|CAT|41.209422|1.275949|1
la Selleta|la Selleta||Montsià|CAT|40.565379|0.503047|0
La Selva|La Selva||Gironès|CAT|41.935436|2.798179|0
la Selva de Mar|la Selva de Mar||Alt Empordà|CAT|42.316789|3.187202|1
la Selva del Camp|la Selva del Camp||Baix Camp|CAT|41.211395|1.134528|1
la Sentiu de Sió|la Sentiu de Sió||Noguera|CAT|41.803615|0.878361|1
La Serra|La Serra||Vallès Occidental|CAT|41.577592|2.161707|0
La Serra|La Serra||Vallès Oriental|CAT|41.649984|2.216785|0
La Serra|La Serra||Anoia|CAT|41.513898|1.652033|0
La Serra|La Serra||Segrià|CAT|41.710081|0.580466|0
la Serra d'Almos|la Serra d'Almos|Serra d'Almos|Ribera d'Ebre|CAT|41.079933|0.744337|0
la Serra d'en Galceran|Sierra Engarcerán||Plana Alta|PV|40.26344|-0.02708|1
la Serra de Rialb|la Serra de Rialb||Noguera|CAT|41.949099|1.176697|0
la Serra-el Pla de Cavalls|la Serra-el Pla de Cavalls||Alt Penedès|CAT|41.363006|1.651532|0
la Serratella|la Serratella||Alt Maestrat|PV|40.309921|0.037623|1
La Serreta|La Serreta||Alacantí|PV|38.35911|-0.550771|0
la Seu d'Urgell|la Seu d'Urgell||Alt Urgell|CAT|42.355883|1.462614|1
La Sima|La Sima||Camp de Túria|PV|39.629721|-0.559123|0
La Solana|La Solana||Garraf|CAT|41.205161|1.654549|0
La Solana|La Solana||Baix Llobregat|CAT|41.377746|1.931309|0
La Solana|La Solana||Marina Alta|PV|38.757422|-0.041504|0
La Solana|La Solana||Vinalopó Mitjà|PV|38.344995|-1.039399|0
la Soleia|la Soleia||Baix Llobregat|CAT|41.441222|1.972653|0
la Sorollera|Cerollera, La||el Matarranya|FR|40.824541|-0.081593|1
la Sort|la Sort||Baix Camp|CAT|41.13514|0.908766|0
la Sénia|la Sénia||Montsià|CAT|40.702066|0.274131|1
La Talaia|La Talaia||Baix Empordà|CAT|42.018525|3.007992|0
La Tallada|La Tallada||Segarra|CAT|41.64025|1.416327|0
la Tallada d'Empordà|la Tallada d'Empordà||Baix Empordà|CAT|42.07629|3.06271|1
la Terra Baixa|la Terra Baixa||Segrià|CAT|41.664424|0.539999|0
La Timba|La Timba||Alt Empordà|CAT|42.245169|2.923018|0
la Todolella|Todolella||Ports|PV|40.644345|-0.262427|1
la Tor de França|Latour-de-France||la Fenolleda|CN|42.770525|2.651755|1
la Tor de Querol|Latour-de-Carol||l'Alta Cerdanya|CN|42.473019|1.873125|1
La Torre|La Torre||Priorat|CAT|41.157632|0.876947|0
La Torre|La Torre||Plana Baixa|PV|39.780694|-0.146239|0
La Torre|La Torre||Plana d'Utiel|PV|39.66928|-1.264126|0
La Torre|La Torre|||PV|39.420604|-0.358439|0
La Torre Baixa|La Torre Baixa||Anoia|CAT|41.540364|1.687878|0
la Torre d'Elna|Latour-Bas-Elne||el Rosselló|CN|42.60482|3.005063|1
la Torre d'en Besora|la Torre d'en Besora||Alt Maestrat|PV|40.320374|-0.080347|1
la Torre d'en Doménec|la Torre d'en Doménec||Plana Alta|PV|40.268152|0.072685|1
la Torre d'en Lloris|la Torre d'en Lloris||Costera|PV|39.029886|-0.48975|0
La Torre d'Oristà|La Torre d'Oristà||Lluçanès|CAT|41.955253|2.05467|0
la Torre de Cabdella|la Torre de Cabdella||Pallars Jussà|CAT|42.444108|0.972694|1
la Torre de Claramunt|la Torre de Claramunt||Anoia|CAT|41.523716|1.659091|1
la Torre de Fluvià|la Torre de Fluvià||Noguera|CAT|41.84103|0.988764|0
la Torre de Fontaubella|la Torre de Fontaubella||Priorat|CAT|41.12084|0.864291|1
la Torre de Gil|la Torre de Gil||Baix Ebre|CAT|40.820025|0.495611|0
la Torre de l'Espanyol|la Torre de l'Espanyol||Ribera d'Ebre|CAT|41.203901|0.651526|1
la Torre de la Sal|la Torre de la Sal||Plana Alta|PV|40.134417|0.165365|0
la Torre de les Maçanes|la Torre de les Maçanes/Torremanzanas||Alacantí|PV|38.604849|-0.411478|1
la Torre de Tamúrcia|la Torre de Tamúrcia||Pallars Jussà|CAT|42.292445|0.796543|0
la Torre de Vilella|Torrevelilla||el Matarranya|FR|40.913686|-0.120947|1
la Torre del Comte|Torre del Compte||el Matarranya|FR|40.94596|0.125658|1
la Torre del Rico|la Torre del Rico|Torre del Rico||CX|38.364354|-1.11335|0
La Torregassa|La Torregassa||Baix Penedès|CAT|41.292243|1.526887|0
La Torreta|La Torreta||Vallès Oriental|CAT|41.600594|2.302486|0
la Tosquilla|la Tosquilla|||CX|38.469203|-1.309809|0
la Vajol|la Vajol||Alt Empordà|CAT|42.40563|2.798842|1
La Valenciana|La Valenciana||Alt Penedès|CAT|41.428584|1.853116|0
la Valira|la Valira||Cerdanya|CAT|42.349799|1.861293|0
La Vall|La Vall||Vallès Oriental|CAT|41.640782|2.199873|0
La Vall|La Vall||Alt Empordà|CAT|42.364983|2.923812|0
La Vall|La Vall||Alt Empordà|CAT|42.298368|2.920186|0
la Vall d'Alba|Vall d'Alba||Plana Alta|PV|40.182117|-0.029332|1
la Vall d'Alcalà|la Vall d'Alcalà||Marina Alta|PV|38.788324|-0.253745|1
la Vall d'Almonesir|Vall de Almonacid||Alt Palància|PV|39.897851|-0.458215|1
la Vall d'Ebo|la Vall d'Ebo||Marina Alta|PV|38.800477|-0.170418|1
la Vall d'en Bas|la Vall d'en Bas||Garrotxa|CAT|42.125696|2.423629|1
la Vall d'Uixó|la Vall d'Uixó||Plana Baixa|PV|39.813599|-0.23497|1
la Vall de Bianya|la Vall de Bianya||Garrotxa|CAT|42.237705|2.433666|1
la Vall de Boí|la Vall de Boí||Alta Ribagorça|CAT|42.535134|0.85689|1""",
        """la Vall de Gallinera|la Vall de Gallinera||Marina Alta|PV|38.829608|-0.232132|1
la Vall de l'Arrabassada|la Vall de l'Arrabassada||Tarragonès|CAT|41.121663|1.273903|0
la Vall de Laguar|la Vall de Laguar||Marina Alta|PV|38.775058|-0.130877|1
La Vall de Montserrat|La Vall de Montserrat||Bages|CAT|41.617259|1.872656|0
La Vall de Santa Creu|La Vall de Santa Creu||Alt Empordà|CAT|42.335538|3.165906|0
la Vall de Tormo|Valdeltormo||el Matarranya|FR|40.975146|0.08797|1
la Valldan|la Valldan||Solsonès|CAT|42.093912|1.349365|0
la Vansa i Fórnols|la Vansa i Fórnols||Alt Urgell|CAT|42.246956|1.496834|1
la Venta de Sant Antoni|la Venta de Sant Antoni||Plana Alta|PV|40.153692|0.158395|0
la Venta i Can Mossarro|la Venta i Can Mossarro||Anoia|CAT|41.530463|1.73479|0
La Vereda|La Vereda||Marina Alta|PV|38.74865|-0.047163|0
la Victòria|la Victòria||Maresme|CAT|41.57859|2.535905|0
La Vidriera|La Vidriera||Camp de Morvedre|PV|39.664954|-0.344869|0
la Vila|la Vila||Baix Empordà|CAT|42.036527|3.00011|0
la Vila Joiosa|la Vila Joiosa||Marina Baixa|PV|38.52119|-0.251488|1
la Vila Joiosa/Villajoyosa|la Vila Joiosa/Villajoyosa||Marina Baixa|PV|38.510595|-0.228107|0
la Vilavella|la Vilavella||Plana Baixa|PV|39.863794|-0.192356|1
la Vilella Alta|la Vilella Alta||Priorat|CAT|41.230732|0.781009|1
la Vilella Baixa|la Vilella Baixa||Priorat|CAT|41.220657|0.75605|1
La Vileta de Serradúy|La Vileta de Serradúy||la Ribagorça|FR|42.322107|0.588931|0
la Vinya|la Vinya||Maresme|CAT|41.492112|2.316747|0
la Vinya de Bonastre|la Vinya de Bonastre||Baix Penedès|CAT|41.214115|1.442157|0
la Virreina|la Virreina||Vallès Occidental|CAT|41.620341|2.094654|0
La Virreina|La Virreina||Maresme|CAT|41.476742|2.280394|0
la Xara|la Xara||Marina Alta|PV|38.829237|0.063927|0
la Xarmada|la Xarmada||Alt Penedès|CAT|41.364928|1.675298|0
La Xelvaneta|La Xelvaneta||Camp de Túria|PV|39.667665|-0.576933|0
La Yesera|La Yesera||Baix Segura|PV|38.110413|-0.86506|0
La Zafra|La Zafra||Alt Vinalopó|PV|38.744366|-0.851626|0
La Zona Industrial|La Zona Industrial||Maresme|CAT|41.625247|2.666935|0
Lagosol-Sector F-2|Lagosol-Sector F-2||Baix Segura|PV|37.955243|-0.738806|0
Lançac|Lansac||la Fenolleda|CN|42.767585|2.56518|1
Las Américas|Las Américas||Vallès Oriental|CAT|41.56885|2.222417|0
Las Balsas|Las Balsas||Camp de Morvedre|PV|39.675845|-0.290251|0
Las Bayas|Las Bayas||Baix Vinalopó|PV|38.220524|-0.643848|0
Las Casas|Las Casas||Plana d'Utiel|PV|39.589328|-1.279863|0
Las Chozas|Las Chozas||Alt Vinalopó|PV|38.667183|-0.929405|0
Las Colonias|Las Colonias||Serrans|PV|39.728567|-1.110382|0
Las Cuevas|Las Cuevas||Plana d'Utiel|PV|39.621784|-1.281274|0
Las Heredades|Las Heredades||Baix Segura|PV|38.096043|-0.763685|0
Las Heredades|Las Heredades||Baix Segura|PV|38.090322|-0.760623|0
Las Lomas|Las Lomas||Baix Vinalopó|PV|38.254466|-0.618733|0
Las Monjas|Las Monjas||Plana d'Utiel|PV|39.465119|-1.300595|0
Las Nogueras|Las Nogueras||Plana d'Utiel|PV|39.585645|-1.084802|0
Las Norias|Las Norias||Baix Segura|PV|38.053887|-0.997101|0
Las Piscinas-Sector J-1|Las Piscinas-Sector J-1||Baix Segura|PV|37.940802|-0.735077|0
Las Ramblas|Las Ramblas||Baix Segura|PV|37.919988|-0.774242|0
Las Siete Casas|Las Siete Casas||Baix Segura|PV|38.143394|-1.015391|0
Las Tiesas|Las Tiesas||Alt Vinalopó|PV|38.612153|-0.848907|0
Las Virtudes|Las Virtudes||Alt Vinalopó|PV|38.6262|-0.932314|0
Lasquarri|Lascuarre||la Ribagorça|FR|42.189869|0.52084|1
Lavern|Lavern||Alt Penedès|CAT|41.390779|1.769989|0
Lavit|Lavit||Alt Penedès|CAT|41.44805|1.729313|0
Lel|Lel||Vinalopó Mitjà|PV|38.455341|-1.022893|0
Les|Les||la Vall d'Aran|CAT|42.809559|0.713136|1
les Alcubles|Alcublas||Serrans|PV|39.803787|-0.678725|1
Les Aldees|Les Aldees||Vallès Oriental|CAT|41.693581|2.154062|0
Les Almudes|Les Almudes||Camp de Túria|PV|39.615873|-0.445984|0
les Alqueries|les Alqueries/Alquerías del Niño Perdido||Plana Baixa|PV|39.889519|-0.129776|1
Les Amplaries|Les Amplaries||Plana Alta|PV|40.116064|0.153266|0
Les Amèriques|Les Amèriques||Garraf|CAT|41.283401|1.747387|0
Les Arboceres|Les Arboceres||Baix Camp|CAT|41.186561|1.104397|0
Les Arenes|Les Arenes||Vallès Occidental|CAT|41.646831|2.057741|0
Les Argiles|Les Argiles||Baix Empordà|CAT|41.929511|3.170378|0
les Avellanes|les Avellanes||Noguera|CAT|41.908168|0.764936|0
les Avellanes i Santa Linya|les Avellanes i Santa Linya||Noguera|CAT|41.918985|0.784479|1
Les Barqueres|Les Barqueres||Vallès Oriental|CAT|41.679187|2.457551|0
Les Barraques|Les Barraques||Ribera Alta|PV|39.156312|-0.446962|0
les Basses d'Alpicat|les Basses d'Alpicat||Segrià|CAT|41.646451|0.558802|0
Les Bassetes|Les Bassetes||Marina Alta|PV|38.848419|-0.160927|0
Les Bassioles|Les Bassioles||Baix Llobregat|CAT|41.364256|1.894316|0
Les Bateries|Les Bateries||Baix Empordà|CAT|41.793242|3.038329|0
les Bons|les Bons|||AD|42.53474|1.58014|0
les Bordes|Es Bòrdes||la Vall d'Aran|CAT|42.718928|0.703893|1
les Borges Blanques|les Borges Blanques||Garrigues|CAT|41.498569|0.865838|1
les Borges del Camp|les Borges del Camp||Baix Camp|CAT|41.178121|1.017194|1
les Botigues|les Botigues||Garraf|CAT|41.265621|1.936691|0
Les Brucardes|Les Brucardes||Bages|CAT|41.739714|1.885435|0
les Bruixes|les Bruixes||Segrià|CAT|41.656575|0.549617|0
les Cabanyes|les Cabanyes||Alt Penedès|CAT|41.371634|1.689945|1
les Canyades de Don Ciro|les Canyades de Don Ciro||Vinalopó Mitjà|PV|38.474109|-0.976474|0
Les Carolines|Les Carolines||Maresme|CAT|41.585157|2.567753|0
les Carpes de Vilalba|les Carpes de Vilalba||Baix Llobregat|CAT|41.50073|1.926945|0
Les Carreres|Les Carreres||Garrotxa|CAT|42.066555|2.622793|0
les Cases d'Alcanar|les Cases d'Alcanar||Montsià|CAT|40.553619|0.530485|0
Les Cases de Bàrcena|Les Cases de Bàrcena|||PV|39.420604|-0.358439|0
les Cases de l'Estació|les Cases de l'Estació||Pallars Jussà|CAT|42.05818|0.882554|0
les Cases de Pena|Cases-de-Pène||el Rosselló|CN|42.787499|2.778071|1
les Cases del Conill|les Cases del Conill|||CX|38.469203|-1.309809|0
les Cases del Riu|les Cases del Riu||Baix Maestrat|PV|40.63362|0.275009|0
les Cases dels Frares|les Cases dels Frares|||CX|38.254955|-1.051719|0
les Cases Noves|les Cases Noves||Alt Penedès|CAT|41.4108|1.69238|0
Les Cases Noves|Les Cases Noves||Alt Penedès|CAT|41.446143|1.854166|0
Les Cases Noves|Les Cases Noves||Osona|CAT|41.989767|2.318207|0
les Cases Noves de Cal Marques|les Cases Noves de Cal Marques||Alt Penedès|CAT|41.319743|1.603307|0
les Cases Noves de Can Pardo|les Cases Noves de Can Pardo||Alt Penedès|CAT|41.463029|1.588197|0
les Cases Noves de la Riera|les Cases Noves de la Riera||Alt Penedès|CAT|41.340362|1.587633|0
les Cases Pairals|les Cases Pairals||Baix Llobregat|CAT|41.425598|1.96452|0
Les Casetes|Les Casetes||Moianès|CAT|41.830673|2.174346|0
Les Casetes|Les Casetes||Bages|CAT|41.730616|1.697222|0
Les Casetes|Les Casetes||Ripollès|CAT|42.314126|2.112134|0
les Casetes d'en Raspall|les Casetes d'en Raspall||Alt Penedès|CAT|41.429374|1.675552|0
les Casetes de Ca n'Oliveró|les Casetes de Ca n'Oliveró||Vallès Occidental|CAT|41.498929|1.968432|0
les Casetes de la Gornal|les Casetes de la Gornal||Alt Penedès|CAT|41.243917|1.582765|0
les Casetes de Puigmoltó|les Casetes de Puigmoltó||Baix Penedès|CAT|41.291816|1.623209|0
les Casetes del Sitjar|les Casetes del Sitjar||Plana Baixa|PV|40.008097|-0.235522|0
Les Clotes|Les Clotes||Moianès|CAT|41.715413|2.168493|0
les Cluses|Les Cluses||el Vallespir|CN|42.484922|2.843874|1
Les Codines|Les Codines||Vallès Occidental|CAT|41.640466|1.904893|0
les Colònies|les Colònies||Pla d'Urgell|CAT|41.635614|0.93621|0
les Comelles del Palà|les Comelles del Palà||Vallès Occidental|CAT|41.609192|1.875701|0
les Comes|les Comes||Bages|CAT|41.642733|1.887689|0
Les Comes|Les Comes||Osona|CAT|41.76802|2.208681|0
Les Comes|Les Comes||Selva|CAT|41.856323|2.733277|0
Les Comes|Les Comes||Ribera Alta|PV|39.187329|-0.505109|0
Les Conques|Les Conques||Gironès|CAT|41.915733|2.848974|0
Les Coromines|Les Coromines||Bages|CAT|41.719479|1.578639|0
Les Costes|Les Costes||Maresme|CAT|41.46842|2.266413|0
Les Costes|Les Costes||Baix Camp|CAT|41.168532|1.005946|0
Les Costes|Les Costes||Baix Camp|CAT|41.167847|0.979962|0
les Coves de Penya-roja|les Coves de Penya-roja|||CX|38.469203|-1.309809|0
les Coves de Vinromà|les Coves de Vinromà||Plana Alta|PV|40.323749|0.11839|1
les Crebades|les Crebades||Alcalatén|PV|40.205398|-0.088966|0
Les Creus|Les Creus||Maresme|CAT|41.631021|2.6786|0
les Delícies|les Delícies||Maresme|CAT|41.509867|2.316008|0
les Destres|les Destres||Alt Camp|CAT|41.338321|1.379356|0
les Duanes de la Mar|les Duanes de la Mar||Marina Alta|PV|38.796832|0.180768|0
les Dunes|les Dunes||Baix Empordà|CAT|42.047066|3.159901|0
les Enzebres|les Enzebres||Vinalopó Mitjà|PV|38.39458|-0.991639|0
les Eres|les Eres||Tarragonès|CAT|41.168351|1.436373|0
Les Escaules|Les Escaules||Alt Empordà|CAT|42.320385|2.882125|0
les Escodines|les Escodines||Anoia|CAT|41.531739|1.614014|0
Les Escoles-Lo Ramos|Les Escoles-Lo Ramos||Alacantí|PV|38.42232|-0.538443|0
les Esglésies|les Esglésies||Pallars Jussà|CAT|42.38682|0.86863|0
Les Espiraltes|Les Espiraltes||Cerdanya|CAT|42.417361|1.872678|0
Les Esplugues de Carme|Les Esplugues de Carme||Anoia|CAT|41.532433|1.637468|0
Les Estoreres|Les Estoreres||Garraf|CAT|41.229411|1.664297|0
Les Faldes del Montseny|Les Faldes del Montseny||Vallès Oriental|CAT|41.693248|2.369129|0
les Ferreres|les Ferreres||Maresme|CAT|41.682939|2.669244|0
Les Fonts|Les Fonts||Vallès Occidental|CAT|41.524542|2.036555|0
Les Fonts|Les Fonts||Vallès Occidental|CAT|41.528419|2.038416|0
les Fonts d'Aiòder|Fuentes de Ayódar||Alt Millars|PV|40.020688|-0.425613|1
Les Fonts de Sant Bernat|Les Fonts de Sant Bernat||Alt Penedès|CAT|41.414049|1.511134|0
Les Forques|Les Forques||Alt Empordà|CAT|42.245963|2.959663|0
les Franqueses del Vallès|les Franqueses del Vallès||Vallès Oriental|CAT|41.649222|2.312659|1
les Garrigues|les Garrigues||Anoia|CAT|41.543796|1.654727|0
Les Ginesteres|Les Ginesteres||Maresme|CAT|41.569543|2.376939|0
les Guilleries|les Guilleries||Osona|CAT|41.87771|2.342129|0
les Guixeres|les Guixeres||Alt Penedès|CAT|41.385888|1.664063|0
Les Gunyoles|Les Gunyoles||Alt Penedès|CAT|41.352024|1.780778|0
les Gunyoles|les Gunyoles||Tarragonès|CAT|41.216794|1.252182|0
Les Hortasses|Les Hortasses||Alt Camp|CAT|41.315998|1.167917|0
Les Illes|Les Illes||Baix Llobregat|CAT|41.561018|1.817736|0
les Irles|les Irles||Baix Camp|CAT|41.170584|0.946343|0
les Llosses|les Llosses||Ripollès|CAT|42.166552|2.100503|1
Les Margarides|Les Margarides||Vallès Oriental|CAT|41.724166|2.417349|0
Les Marines|Les Marines||Marina Alta|PV|38.852945|0.069034|0
les Masies|les Masies||Alt Urgell|CAT|42.161017|1.288009|0
les Masies Catalanes|les Masies Catalanes||Baix Camp|CAT|41.236179|1.155251|0
les Masies Catalanes|les Masies Catalanes||Alt Camp|CAT|41.23552|1.160222|0
les Masies de Dalt|les Masies de Dalt||Alt Empordà|CAT|42.309992|2.968156|0
les Masies de Roda|les Masies de Roda||Osona|CAT|41.982519|2.325496|1
les Masies de Sant Miquel|les Masies de Sant Miquel||Baix Penedès|CAT|41.270768|1.559606|0
les Masies de Voltregà|les Masies de Voltregà||Osona|CAT|42.023623|2.226563|1
Les Masuques|Les Masuques||Alt Penedès|CAT|41.29434|1.642378|0
les Meravelles|les Meravelles||Mallorca|IB|39.519922|2.74809|0
Les Mines d'Osor|Les Mines d'Osor||Selva|CAT|41.949622|2.591465|0
Les Montcades|Les Montcades||Terra Alta|CAT|40.94189|0.281868|0
les Morisques|les Morisques||Tarragonès|CAT|41.162326|1.436022|0
les Nogueres|les Nogueres||Maresme|CAT|41.513399|2.315833|0
Les Oliveres|Les Oliveres||Vallès Oriental|CAT|41.6086|2.265866|0
Les Olives|Les Olives||Baix Empordà|CAT|42.104321|3.020805|0
les Oluges|les Oluges||Segarra|CAT|41.693139|1.339541|1
les Pallargues|les Pallargues||Segarra|CAT|41.761361|1.197058|0
Les Palmeres|Les Palmeres||Garraf|CAT|41.274879|1.706197|0
Les Palmeres|Les Palmeres||Ribera Baixa|PV|39.257219|-0.263224|0
les Palmes|les Palmes||Plana Alta|PV|40.04848|0.02419|0
Les Parellades|Les Parellades||Alt Penedès|CAT|41.415605|1.746087|0
les Parretes|les Parretes||Baix Llobregat|CAT|41.417817|1.913255|0
Les Paulines|Les Paulines||Alacantí|PV|38.399189|-0.457661|0
les Paüls|Laspaúles||la Ribagorça|FR|42.465083|0.596887|1
les Peces|les Peces||Baix Penedès|CAT|41.254977|1.500868|0
Les Pedreres|Les Pedreres||Pla de l'Estany|CAT|42.10354|2.771478|0
Les Pedreres|Les Pedreres||Baix Penedès|CAT|41.239052|1.536754|0
Les Pedretes|Les Pedretes||Camp de Túria|PV|39.642421|-0.544511|0
Les Pedritxes|Les Pedritxes||Vallès Occidental|CAT|41.615771|1.998277|0
Les Penyes|Les Penyes||Camp de Túria|PV|39.586356|-0.518091|0
Les Penyetes|Les Penyetes||Alacantí|PV|38.417019|-0.434284|0
Les Pereres|Les Pereres||Cerdanya|CAT|42.401924|1.932368|0
les Piles|les Piles||Conca de Barberà|CAT|41.501936|1.349426|1
les Pinedes Altes|les Pinedes Altes||Baix Penedès|CAT|41.363604|1.535843|0
Les Pinedes de Castellnou|Les Pinedes de Castellnou||Bages|CAT|41.811465|1.818302|0
Les Pinedes de l'Armengol|Les Pinedes de l'Armengol||Anoia|CAT|41.519351|1.621218|0
les Pinedes de l'Armengol|les Pinedes de l'Armengol||Anoia|CAT|41.513839|1.624733|0
les Planes|les Planes||Vallès Occidental|CAT|41.425672|2.096553|0
Les Planes|Les Planes||Alt Camp|CAT|41.241051|1.236261|0
les Planes d'Hostoles|les Planes d'Hostoles||Garrotxa|CAT|42.06449|2.54943|1
Les Planes de Llanars|Les Planes de Llanars||Ripollès|CAT|42.320816|2.347581|0
Les Planes del Puig|Les Planes del Puig||Baix Camp|CAT|41.186677|1.085818|0
Les Planes del Rei|Les Planes del Rei||Baix Camp|CAT|41.028943|0.898775|0
les Pobles|les Pobles||Alt Camp|CAT|41.360377|1.401374|0
les Preses|les Preses||Garrotxa|CAT|42.147388|2.473988|1
les Puelles|les Puelles||Urgell|CAT|41.800908|1.129493|0
les Pungoles|les Pungoles||Vallès Oriental|CAT|41.665886|2.370392|0
les Pungoles|les Pungoles||Vallès Oriental|CAT|41.673057|2.37054|0
Les Rajoleries|Les Rajoleries||Baix Empordà|CAT|41.931168|3.164854|0
Les Roques|Les Roques||Alt Empordà|CAT|42.149365|2.929436|0
les Roques|les Roques||Conca de Barberà|CAT|41.574005|1.427628|0
Les Roques Blanques|Les Roques Blanques||Maresme|CAT|41.593672|2.596294|0
les Roques Daurades|les Roques Daurades||Baix Ebre|CAT|40.871154|0.794007|0
Les Roquetes|Les Roquetes||Garraf|CAT|41.228693|1.745982|0
Les Roquetes 1|Les Roquetes 1||Vallès Oriental|CAT|41.561901|2.312274|0
Les Roquetes 2|Les Roquetes 2||Vallès Oriental|CAT|41.560226|2.310218|0
les Roses|les Roses||Maresme|CAT|41.58601|2.552918|0
Les Rouredes|Les Rouredes||Baix Camp|CAT|41.184647|1.094315|0
Les Rovires|Les Rovires||Baix Llobregat|CAT|41.398173|1.93858|0
les Sabrugues|les Sabrugues||Vallès Oriental|CAT|41.695544|2.169734|0
Les Salines|Les Salines||Garraf|CAT|41.199681|1.649888|0
les Serres|les Serres||Baix Camp|CAT|41.179016|1.093561|0
Les Simetes|Les Simetes||Horta Sud|PV|39.509244|-0.500297|0
les Solanes|les Solanes||Vallès Oriental|CAT|41.684918|2.15098|0
les Sènies|les Sènies||Maresme|CAT|41.514449|2.413045|0
les Sínies|les Sínies||Tarragonès|CAT|41.164701|1.451671|0
Les Tarumbes|Les Tarumbes||Alt Penedès|CAT|41.411675|1.733968|0
Les Tendes|Les Tendes||Horta Nord|PV|39.514584|-0.338389|0
les Teules|les Teules||Baix Empordà|CAT|41.820359|3.012852|0
Les Torres|Les Torres||Garraf|CAT|41.249145|1.790247|0
Les Torres|Les Torres||Tarragonès|CAT|41.1798|1.455846|0
les Tres Cales|les Tres Cales||Baix Ebre|CAT|40.909267|0.816603|0
les Tres Creus|les Tres Creus||Bages|CAT|41.719652|1.814801|0
les Tres Fonts|les Tres Fonts||Vinalopó Mitjà|PV|38.371649|-0.987007|0
les Useres|les Useres / Useras||Alcalatén|PV|40.178339|-0.133782|1
les Valls d'Aguilar|les Valls d'Aguilar||Alt Urgell|CAT|42.302939|1.242335|1
les Valls de Valira|les Valls de Valira||Alt Urgell|CAT|42.443332|1.454653|1
les Ventalles|les Ventalles||Montsià|CAT|40.644865|0.497088|0
les Ventoses|les Ventoses||Noguera|CAT|41.796341|1.012294|0
Les Vilares|Les Vilares||Maresme|CAT|41.469497|2.273443|0
les Vil·les|les Vil·les||Maresme|CAT|41.574576|2.539022|0
les Vil·les de Benicàssim|les Vil·les de Benicàssim||Plana Alta|PV|40.054135|0.071396|0
les Voltes|les Voltes||Baix Camp|CAT|41.166267|0.992725|0
les Àligues|les Àligues||Vallès Oriental|CAT|41.616083|2.272089|0
Lilla|Lilla||Conca de Barberà|CAT|41.348142|1.212577|0
Linya|Linya||Solsonès|CAT|41.968488|1.621954|0
Linyola|Linyola||Pla d'Urgell|CAT|41.707439|0.890287|1
Litera|Litera||el Baix Cinca|FR|41.536746|0.041904|0
Litera|Litera||la Ribagorça|FR|42.127264|0.642437|0
Llaberia|Llaberia||Ribera d'Ebre|CAT|41.085739|0.84131|0
Llabià|Llabià||Baix Empordà|CAT|42.018519|3.086872|0
Lladorre|Lladorre||Pallars Sobirà|CAT|42.673015|1.277091|1
Lladrós|Lladrós||Pallars Sobirà|CAT|42.605309|1.241235|0""",
        """Lladurs|Lladurs||Solsonès|CAT|42.062924|1.476683|1
Lladó|Lladó||Alt Empordà|CAT|42.254433|2.810194|1
Llafranc|Llafranc||Baix Empordà|CAT|41.895734|3.194724|0
Llagostera|Llagostera||Gironès|CAT|41.815013|2.908154|1
Llagostera Residencial|Llagostera Residencial||Gironès|CAT|41.831846|2.911987|0
Llagunes|Llagunes||Pallars Sobirà|CAT|42.370102|1.190083|0
Llambilles|Llambilles||Gironès|CAT|41.922804|2.86916|1
Llampaies|Llampaies||Alt Empordà|CAT|42.121809|2.936847|0
Llanars|Llanars||Ripollès|CAT|42.341748|2.346947|1
Llanera de Ranes|Llanera de Ranes||Costera|PV|38.99983|-0.59256|1
Llançà|Llançà||Alt Empordà|CAT|42.366417|3.126608|1
Llardecans|Llardecans||Segrià|CAT|41.387062|0.538528|1
Llarvén|Llarvén||Pallars Sobirà|CAT|42.400556|1.093967|0
Llaurí|Llaurí||Ribera Baixa|PV|39.138032|-0.323571|1
Llauró|Llauro||el Rosselló|CN|42.545678|2.744513|1
Llavorsí|Llavorsí||Pallars Sobirà|CAT|42.487268|1.190479|1
Lledó d'Algars|Lledó||el Matarranya|FR|40.948375|0.259136|1
Lleger|Lleger||Baix Penedès|CAT|41.294232|1.577721|0
Lleida|Lleida||Segrià|CAT|41.631205|0.581437|1
Lleret|Lleret||Pallars Sobirà|CAT|42.619269|1.235184|0
Llerona|Llerona||Vallès Oriental|CAT|41.649651|2.299945|0
Llers|Llers||Alt Empordà|CAT|42.294444|2.912229|1
Lles de Cerdanya|Lles de Cerdanya||Cerdanya|CAT|42.434856|1.667527|1
Llesp|Llesp||Alta Ribagorça|CAT|42.456803|0.758852|0
Llessui|Llessui||Pallars Sobirà|CAT|42.452222|1.076307|0
Llevantina|Llevantina||Tarragonès|CAT|41.133667|1.299417|0
Llimiana|Llimiana||Pallars Jussà|CAT|42.058658|0.928612|1
Llinars|Llinars||Solsonès|CAT|42.147779|1.379257|0
Llinars del Vallès|Llinars del Vallès||Vallès Oriental|CAT|41.635698|2.408114|1
Lliçà d'Amunt|Lliçà d'Amunt||Vallès Oriental|CAT|41.611524|2.22459|1
Lliçà de Vall|Lliçà de Vall||Vallès Oriental|CAT|41.58807|2.230885|1
Llo|Llo||l'Alta Cerdanya|CN|42.433792|2.089507|1
Llobatera|Llobatera||Camp de Túria|PV|39.511925|-0.533769|0
Llobera|Llobera||Solsonès|CAT|41.923352|1.461921|1
Llocalou|Llocalou||Garrotxa|CAT|42.208905|2.483496|0
Llocnou d'en Fenollet|Llocnou d'en Fenollet||Costera|PV|39.010086|-0.469876|1
Llocnou de la Corona|Llocnou de la Corona||Horta Sud|PV|39.420584|-0.381968|1
Llocnou de Sant Jeroni|Llocnou de Sant Jeroni||Safor|PV|38.902613|-0.289861|1
Llofriu|Llofriu||Alacantí|PV|38.467392|-0.505441|0
Lloixa|Lloixa||Alacantí|PV|38.397098|-0.446929|0
Lloma de la Verge|Lloma de la Verge||Horta Sud|PV|39.387496|-0.490298|0
Lloma del Porret|Lloma del Porret||Camp de Túria|PV|39.581221|-0.526557|0
Lloma Llarga|Lloma Llarga||Horta Nord|PV|39.511198|-0.429982|0
Lloma Llarga I|Lloma Llarga I||Camp de Túria|PV|39.631509|-0.52004|0
Lloma Llarga II|Lloma Llarga II||Camp de Túria|PV|39.633733|-0.527883|0
Llombai|Llombai||Ribera Alta|PV|39.295203|-0.592371|1
Llombai|Llombai||Marina Alta|PV|38.815549|-0.270141|0
Llorac|Llorac||Conca de Barberà|CAT|41.556192|1.330427|1
Llorenç de Rocafort|Llorenç de Rocafort||Urgell|CAT|41.534612|1.067897|0
Llorenç del Penedès|Llorenç del Penedès||Baix Penedès|CAT|41.285266|1.544667|1
Lloret Blau|Lloret Blau||Selva|CAT|41.740942|2.814867|0
Lloret de Mar|Lloret de Mar||Selva|CAT|41.722465|2.834609|1
Lloret de Vistalegre|Lloret de Vistalegre||Mallorca|IB|39.618903|2.97627|1
Lloret Verd|Lloret Verd||Selva|CAT|41.725207|2.808305|0
Llorts|Llorts|||AD|42.55623|1.53319|0
Llorà|Llorà||Gironès|CAT|42.022416|2.703986|0
Lloseta|Lloseta||Mallorca|IB|39.72026|2.858471|1
Llubí|Llubí||Mallorca|IB|39.699958|3.023931|1
Lluc|Lluc||Mallorca|IB|39.819743|2.884662|0
Lluc Alcari|Lluc Alcari||Mallorca|IB|39.76496|2.653721|0
Lluca|Lluca||Marina Alta|PV|38.755097|0.145984|0
Llucena|Llucena/Lucena del Cid||Alcalatén|PV|40.149576|-0.280977|1
Llucmajor|Llucmajor||Mallorca|IB|39.450117|2.851298|1
Llucmaçanes|Llucmaçanes||Menorca|IB|39.868016|4.242822|0
Lludient|Ludiente||Alt Millars|PV|40.09564|-0.377317|1
Llumeneres|Llumeneres|||AD|42.46372|1.49129|0
Llupià|Llupia||el Rosselló|CN|42.625566|2.78269|1
Llutxent|Llutxent||Vall d'Albaida|PV|38.945778|-0.341728|1
Lluçars|Lluçars||Noguera|CAT|41.975518|1.056609|0
Lluçà|Lluçà||Lluçanès|CAT|42.069444|2.051159|1
Llíber|Llíber||Marina Alta|PV|38.729492|0.008093|1
Llíria|Llíria||Camp de Túria|PV|39.690456|-0.658907|1
Llívia|Llívia||Cerdanya|CAT|42.466144|1.98138|1
Llívia|Llívia||Segrià|CAT|41.648751|0.63866|0
Lo Cartagena|Lo Cartagena||Baix Segura|PV|38.144782|-0.859532|0
Lo Frasquito|Lo Frasquito||Foia de Bunyol|PV|39.415924|-0.690217|0
lo Riber|lo Riber||Ribera d'Ebre|CAT|41.237698|0.549198|0
Loma Caballeros|Loma Caballeros||Foia de Bunyol|PV|39.413944|-0.716088|0
Loma Espí|Loma Espí||Alacantí|PV|38.447319|-0.570987|0
Loma Santa|Loma Santa||Foia de Bunyol|PV|39.433763|-0.650444|0
Lomas de Campoamor|Lomas de Campoamor||Baix Segura|PV|37.931667|-0.772616|0
Lomas de Polo-Pinomar|Lomas de Polo-Pinomar||Baix Segura|PV|38.035037|-0.659344|0
Loriguilla|Loriguilla||Camp de Túria|PV|39.631933|-0.949245|1
Los Almendros-Pau 26|Los Almendros-Pau 26||Baix Segura|PV|37.951879|-0.74706|0
Los Balcones y los Altos|Los Balcones y los Altos||Baix Segura|PV|37.9556|-0.731827|0
Los Calderones|Los Calderones||Vall de Cofrents|PV|39.059167|-1.050705|0
Los Calpes|Los Calpes||Alt Millars|PV|40.071442|-0.58296|0
Los Cantos|Los Cantos||Alt Millars|PV|40.116674|-0.609855|0
Los Cerezos|Los Cerezos||Marina Alta|PV|38.746302|0.149783|0
Los Cojos|Los Cojos||Plana d'Utiel|PV|39.421248|-1.307237|0
Los Corrales|Los Corrales||Plana d'Utiel|PV|39.595507|-1.271209|0
Los Desamparados|Los Desamparados||Baix Segura|PV|38.063844|-0.981956|0
Los Dolores|Los Dolores||Baix Segura|PV|38.140733|-0.841036|0
Los Duques|Los Duques||Plana d'Utiel|PV|39.433793|-1.205703|0
Los Felipes|Los Felipes||Foia de Bunyol|PV|39.444097|-0.645582|0
Los Herreros|Los Herreros||Vall de Cofrents|PV|39.321996|-0.987154|0
Los Hortelanos|Los Hortelanos||Baix Segura|PV|37.876309|-0.790617|0
Los Huertos|Los Huertos||Baix Segura|PV|38.088328|-0.91992|0
Los Isidros|Los Isidros||Plana d'Utiel|PV|39.42013|-1.292395|0
Los Luceros|Los Luceros||Baix Vinalopó|PV|38.252393|-0.668446|0
Los Marcos|Los Marcos||Plana d'Utiel|PV|39.492261|-1.28377|0
Los Martínez|Los Martínez||Baix Segura|PV|38.116897|-0.732876|0
Los Martínez Segundo Barrio|Los Martínez Segundo Barrio||Baix Segura|PV|38.119086|-0.738521|0
Los Monasterios|Los Monasterios||Horta Nord|PV|39.636433|-0.33862|0
Los Monasterios|Los Monasterios||Camp de Morvedre|PV|39.644972|-0.349042|0
Los Montesinos|Los Montesinos||Baix Segura|PV|38.024698|-0.755104|1
Los Pajares|Los Pajares||Racó d'Ademús|PV|40.095941|-1.260213|0
Los Palacios|Los Palacios||Baix Segura|PV|38.089998|-0.727681|0
Los Pedrones|Los Pedrones||Plana d'Utiel|PV|39.33488|-1.083197|0
Los Picapiedra|Los Picapiedra||Alacantí|PV|38.307574|-0.571098|0
Los Pinares|Los Pinares||Camp de Túria|PV|39.640006|-0.499279|0
Los Pinos|Los Pinos||Baix Vinalopó|PV|38.207144|-0.630319|0
Los Pérez|Los Pérez||Baix Segura|PV|38.173753|-0.939489|0
Los Rosales|Los Rosales||Vallès Occidental|CAT|41.525489|2.086728|0
Los Ruices|Los Ruices||Plana d'Utiel|PV|39.475892|-1.255457|0
Los Santos|Los Santos||Racó d'Ademús|PV|40.107851|-1.282587|0
Los Sardineros|Los Sardineros||Plana d'Utiel|PV|39.374771|-1.24789|0
Los Secanos|Los Secanos||Baix Segura|PV|38.078018|-0.656865|0
Los Sáez|Los Sáez||Baix Segura|PV|37.853905|-0.797356|0
Los Ángeles|Los Ángeles||Alt Palància|PV|39.892674|-0.516833|0
Losilla|Losilla||Serrans|PV|39.966593|-1.092707|0
Loteria-Fernandet|Loteria-Fernandet||Ribera Baixa|PV|39.260649|-0.265649|0
Luzás|Luzás||la Ribagorça|FR|42.155432|0.576131|0
l’Alguer|Alghero|Alguer||AL|40.55969|8.31953|1
Macastre|Macastre||Foia de Bunyol|PV|39.347871|-0.795994|1
Madremanya|Madremanya||Gironès|CAT|41.985931|2.953175|1
Maella|Maella||el Matarranya|FR|41.116061|0.101577|1
Mafet|Mafet||Urgell|CAT|41.808079|1.079962|0
Magaluf|Magaluf||Mallorca|IB|39.51163|2.533145|0
Mahuella Tauladella Rafalell y Vistabella|Mahuella Tauladella Rafalell y Vistabella||València|PV|39.544319|-0.333882|0
Maials|Maials||Segrià|CAT|41.343224|0.5064|1
Maians|Maians||Bages|CAT|41.635305|1.684403|0
Mainou|Mainou||Baix Camp|CAT|41.051165|0.998537|0
Maioris Decima|Maioris Decima||Mallorca|IB|39.454411|2.748726|0
Maitino|Maitino||Baix Vinalopó|PV|38.273427|-0.634566|0
Maià de Montcal|Maià de Montcal||Garrotxa|CAT|42.210977|2.749063|1
Malavella Parc|Malavella Parc||Selva|CAT|41.819867|2.803814|0
Maldà|Maldà||Urgell|CAT|41.531592|1.023764|1
Malgrat|Malgrat||Segarra|CAT|41.709605|1.301116|0
Malgrat de Mar|Malgrat de Mar||Maresme|CAT|41.650524|2.746007|1
Malla|Malla||Osona|CAT|41.885827|2.242176|1
Mallaeta|Mallaeta||Camp de Túria|PV|39.584259|-0.527498|0
Mallolís|Mallolís||Pallars Sobirà|CAT|42.495531|1.25863|0
Mallorquines|Mallorquines||Maresme|CAT|41.465304|2.27184|0
Mallorquines|Mallorquines||Selva|CAT|41.81973|2.735542|0
Malmercat|Malmercat||Pallars Sobirà|CAT|42.3699|1.122155|0
Malpartit|Malpartit||Segrià|CAT|41.691496|0.534681|0
Malpàs|Malpàs||Alta Ribagorça|CAT|42.408562|0.792764|0
Malvet|Malvet||Baix Empordà|CAT|41.830894|2.998125|0
Manacor|Manacor||Mallorca|IB|39.554844|3.235436|1
Mancor de la Vall|Mancor de la Vall||Mallorca|IB|39.760969|2.86421|1
Manises|Manises||Horta Sud|PV|39.498072|-0.495653|1
Manlleu|Manlleu||Osona|CAT|42.007354|2.279875|1
Manresa|Manresa||Bages|CAT|41.720644|1.825602|1
Manuel|Manuel||Ribera Alta|PV|39.054408|-0.492104|1
Manyanet|Manyanet||Pallars Jussà|CAT|42.423589|0.881628|0
Manyoses|Manyoses||Anoia|CAT|41.583547|1.641242|0
Maquiva I|Maquiva I||Camp de Túria|PV|39.629865|-0.538195|0
Maquiva II|Maquiva II||Camp de Túria|PV|39.621404|-0.529328|0
Maravisa Golden|Maravisa Golden||Camp de Túria|PV|39.584204|-0.518156|0
Maravisa I|Maravisa I||Camp de Túria|PV|39.586244|-0.523404|0
Maravisa II|Maravisa II||Camp de Túria|PV|39.584422|-0.522037|0
Marcovau|Marcovau||Noguera|CAT|41.868134|1.033303|0
Marenyà|Marenyà||Baix Empordà|CAT|42.091299|3.047221|0
Margalef|Margalef||Priorat|CAT|41.299831|0.7743|1
Marganell|Marganell||Bages|CAT|41.628877|1.802664|1
Margarida|Margarida||Comtat|PV|38.788745|-0.298598|0
Maria de la Salut|Maria de la Salut||Mallorca|IB|39.674458|3.094527|1
Marina|Marina||Maresme|CAT|41.60384|2.628098|0
Marina d'Or|Marina d'Or||Plana Alta|PV|40.106241|0.150428|0
Marina de Sant Jordi|Marina de Sant Jordi||Baix Ebre|CAT|40.918641|0.834322|0
Marina Manresa|Marina Manresa||Mallorca|IB|39.860457|3.116793|0
Marines|Marines||Camp de Túria|PV|39.723866|-0.549585|1
Maristella|Maristella|||AL|40.55969|8.31953|0
Marqueixanes|Marquixanes||el Conflent|CN|42.637654|2.483933|1
Marquet Paradís|Marquet Paradís||Bages|CAT|41.693009|1.876252|0
Marratxinet|Marratxinet||Mallorca|IB|39.645074|2.752501|0
Marratxí|Marratxí||Mallorca|IB|39.623761|2.728558|1
Marseta|Marseta||Alacantí|PV|38.424881|-0.463871|0
Martinet|Martinet||Cerdanya|CAT|42.360065|1.695086|0
Martivell|Martivell||Alt Penedès|CAT|41.420958|1.84733|0
Martorell|Martorell||Baix Llobregat|CAT|41.47906|1.917403|1
Martorelles|Martorelles||Vallès Oriental|CAT|41.531591|2.237173|1
Martossa|Martossa||Selva|CAT|41.708933|2.898268|0
Marxuquera|Marxuquera||Safor|PV|38.954009|-0.240326|0
Marzà|Marzà||Alt Empordà|CAT|42.310208|3.068707|0
Marçà|Marçà||Priorat|CAT|41.126635|0.801328|1
Mas Alba|Mas Alba||Garraf|CAT|41.264935|1.814111|0
Mas Aliu|Mas Aliu||Gironès|CAT|41.943392|2.781307|0
Mas Altaba|Mas Altaba||Selva|CAT|41.754672|2.714094|0
Mas Ambròs|Mas Ambròs||Baix Empordà|CAT|41.853148|3.065955|0
Mas Artigues|Mas Artigues||Baix Empordà|CAT|41.86072|3.027785|0
Mas Astor|Mas Astor||Baix Penedès|CAT|41.189995|1.492314|0
Mas Barceló|Mas Barceló||Baix Empordà|CAT|41.866301|3.075433|0
Mas Blanc|Mas Blanc||Alcalatén|PV|40.161782|-0.093952|0
Mas Bo|Mas Bo||Vallès Oriental|CAT|41.58373|2.21036|0
Mas Bohera|Mas Bohera||Alt Empordà|CAT|42.309916|3.145112|0
Mas Borràs|Mas Borràs||Baix Penedès|CAT|41.204217|1.498632|0
Mas Boscà-Argonista-El Cardo|Mas Boscà-Argonista-El Cardo||Alt Empordà|CAT|42.283442|3.166795|0
Mas Camarena|Mas Camarena||Camp de Túria|PV|39.551158|-0.457285|0
Mas Coll|Mas Coll||Maresme|CAT|41.504598|2.280094|0
Mas Corts|Mas Corts||Vallès Oriental|CAT|41.502474|2.25015|0
Mas d'en Bosc|Mas d'en Bosc||Baix Camp|CAT|41.083833|1.099229|0
Mas d'en Parès|Mas d'en Parès||Alt Camp|CAT|41.37239|1.390797|0
Mas d'en Pla|Mas d'en Pla||Bages|CAT|41.740289|1.823271|0
Mas d'En Puig|Mas d'En Puig||Baix Llobregat|CAT|41.438|1.975714|0
Mas d'en Rieres|Mas d'en Rieres||Plana Alta|PV|40.377633|0.133898|0
Mas d'Enric|Mas d'Enric||Tarragonès|CAT|41.153188|1.289584|0
Mas de Barberans|Mas de Barberans||Montsià|CAT|40.72147|0.35512|1
Mas de Bondia|Mas de Bondia||Segarra|CAT|41.605378|1.192566|0
Mas de Can Pi|Mas de Can Pi||Baix Llobregat|CAT|41.385548|1.96659|0
Mas de Centelles|Mas de Centelles||Alt Maestrat|PV|40.320529|-0.078034|0
Mas de Elies|Mas de Elies||Camp de Túria|PV|39.557556|-0.440118|0
Mas de Flors|Mas de Flors||Plana Alta|PV|40.067234|-0.154888|0
Mas de Gassol|Mas de Gassol||Alt Camp|CAT|41.27533|1.172729|0
Mas de Jacinto|Mas de Jacinto||Racó d'Ademús|PV|40.128904|-1.250721|0
Mas de la Jonquera|Mas de la Jonquera||Camp de Túria|PV|39.549767|-0.448907|0
Mas de la Peña|Mas de la Peña||Alt Millars|PV|40.202304|-0.435011|0
Mas de les Fonts|Mas de les Fonts||Baix Llobregat|CAT|41.362163|1.928554|0
Mas de Lleó|Mas de Lleó||Segrià|CAT|41.713042|0.474541|0
Mas de los Mudos|Mas de los Mudos||Racó d'Ademús|PV|40.111269|-1.25673|0
Mas de Pastoret|Mas de Pastoret||Tarragonès|CAT|41.158342|1.312394|0
Mas de Tous|Mas de Tous||Camp de Túria|PV|39.587971|-0.535224|0
Mas del Cano|Mas del Cano||Alt Camp|CAT|41.236483|1.162233|0
Mas del Carpa|Mas del Carpa||Baix Camp|CAT|41.150028|1.13038|0
Mas del Pi|Mas del Pi||Plana Alta|PV|40.067312|-0.109456|0
Mas del Plata|Mas del Plata||Alt Camp|CAT|41.390036|1.333309|0
Mas dels Frares|Mas dels Frares||Plana Alta|PV|40.04043|0.051027|0
Mas Domingo|Mas Domingo||Baix Empordà|CAT|41.933004|3.17425|0
Mas Dorca|Mas Dorca||Vallès Oriental|CAT|41.655576|2.276294|0
Mas Duran|Mas Duran||Vallès Occidental|CAT|41.533591|2.086192|0
Mas Enric|Mas Enric||Bages|CAT|41.641651|1.820801|0
Mas Fabrera|Mas Fabrera||Vallès Oriental|CAT|41.642519|2.258146|0
Mas Ferrer|Mas Ferrer||Baix Llobregat|CAT|41.325681|1.918351|0
Mas Fumats|Mas Fumats||Alt Empordà|CAT|42.292191|3.176246|0
Mas Gotarra|Mas Gotarra||Gironès|CAT|41.820908|2.889133|0
Mas Gros|Mas Gros||Alt Empordà|CAT|42.139072|3.069713|0
Mas Isaac|Mas Isaac||Alt Empordà|CAT|42.312645|3.136585|0
Mas la Pobla de Bellestar|Mas la Pobla de Bellestar||Ports|PV|40.463414|-0.310281|0
Mas Llombard|Mas Llombard||Vallès Oriental|CAT|41.503121|2.242089|0
Mas Llorenç|Mas Llorenç||Alt Camp|CAT|41.253101|1.157098|0
Mas Llosar|Mas Llosar||Ports|PV|40.444437|-0.273155|0
Mas Mateu|Mas Mateu||Baix Penedès|CAT|41.290263|1.411685|0
Mas Mel|Mas Mel||Baix Penedès|CAT|41.193927|1.588124|0
Mas Oliva|Mas Oliva||Alt Empordà|CAT|42.271343|3.190365|0
Mas Pallí|Mas Pallí||Baix Empordà|CAT|41.843178|3.068095|0
Mas Pallí dels Vilars II|Mas Pallí dels Vilars II||Baix Empordà|CAT|41.842111|3.071795|0
Mas Pares de d'Alt|Mas Pares de d'Alt||Garraf|CAT|41.255234|1.733672|0
Mas Pinell|Mas Pinell||Baix Empordà|CAT|42.009018|3.192098|0
Mas Planoi|Mas Planoi||Bages|CAT|41.66951|1.836591|0""",
        """Mas Rampinyo|Mas Rampinyo||Vallès Occidental|CAT|41.494234|2.183308|0
Mas Romeu|Mas Romeu||Baix Penedès|CAT|41.214712|1.593641|0
Mas Romeu Residencial|Mas Romeu Residencial||Selva|CAT|41.716532|2.833702|0
Mas Rovira 1er. Sector|Mas Rovira 1er. Sector||Baix Llobregat|CAT|41.387114|1.925854|0
Mas Sot|Mas Sot||Vallès Occidental|CAT|41.595088|2.030447|0
Mas Toi|Mas Toi||Baix Empordà|CAT|41.859542|3.060436|0
Mas Trader|Mas Trader||Garraf|CAT|41.224011|1.661031|0
Mas Trempat|Mas Trempat||Baix Empordà|CAT|41.797221|3.015089|0
Mas Trempat|Mas Trempat||Baix Empordà|CAT|41.798866|3.010913|0
Mas Vilar|Mas Vilar||Baix Empordà|CAT|41.841655|3.088658|0
Mas Vilet dels Pins|Mas Vilet dels Pins||Tarragonès|CAT|41.176534|1.304487|0
Masarac|Masarac||Alt Empordà|CAT|42.35083|2.972713|0
Masarac i Vilarnadal|Masarac i Vilarnadal||Alt Empordà|CAT|42.343363|2.962984|1
Masarbonès|Masarbonès||Baix Penedès|CAT|41.260005|1.43856|0
Masbarrat|Masbarrat||Alt Camp|CAT|41.34409|1.396036|0
Masboquera|Masboquera||Baix Camp|CAT|41.020435|0.857419|0
Mascarell|Mascarell||Plana Baixa|PV|39.860125|-0.14094|0
Masdenverge|Masdenverge||Montsià|CAT|40.713933|0.530644|1
Masella|Masella||Cerdanya|CAT|42.350744|1.90127|0
Maset d'en Martí|Maset d'en Martí||Anoia|CAT|41.574472|1.737396|0
Masia Boronat|Masia Boronat||Tarragonès|CAT|41.175997|1.33368|0
Masia del Racó|Masia del Racó||Ribera Baixa|PV|39.183359|-0.235068|0
Masies de Lloret|Masies de Lloret||Selva|CAT|41.738013|2.795838|0
Masllorenç|Masllorenç||Baix Penedès|CAT|41.263347|1.429542|1
Masmolets|Masmolets||Alt Camp|CAT|41.316095|1.237108|0
Masos d'en Blader|Masos d'en Blader||Baix Camp|CAT|41.031717|0.952809|0
Maspujols|Maspujols||Baix Camp|CAT|41.187703|1.034102|1
Masquefa|Masquefa||Anoia|CAT|41.500013|1.807258|1
Masriudoms|Masriudoms||Baix Camp|CAT|41.02009|0.884673|0
Massalavés|Massalavés||Ribera Alta|PV|39.15389|-0.517949|1
Massalcoreig|Massalcoreig||Segrià|CAT|41.45835|0.359551|1
Massalfassar|Massalfassar||Horta Nord|PV|39.554649|-0.315628|1
Massalió|Mazaleón||el Matarranya|FR|41.031448|0.089164|1
Massamagrell|Massamagrell||Horta Nord|PV|39.574318|-0.331871|1
Massanassa|Massanassa||Horta Sud|PV|39.397218|-0.372536|1
Massanes|Massanes||Selva|CAT|41.775222|2.648956|1
Massarrojos|Massarrojos|||PV|39.539345|-0.403416|0
Massoteres|Massoteres||Segarra|CAT|41.800801|1.332524|1
Masía Bach|Masía Bach||Baix Llobregat|CAT|41.495041|1.849448|0
Masía de la Cimorreta|Masía de la Cimorreta||Alt Millars|PV|40.163965|-0.401533|0
Masía de los Pérez|Masía de los Pérez||Alt Palància|PV|39.893068|-0.715114|0
Masía del Oliveral|Masía del Oliveral||Camp de Túria|PV|39.473669|-0.538237|0
Masía Giménez|Masía Giménez||Camp de Túria|PV|39.604103|-0.52305|0
Masías|Masías||Horta Nord|PV|39.560122|-0.404912|0
Mata|Mata||Gironès|CAT|41.829239|2.872969|0
Mata|Mata||Pla de l'Estany|CAT|42.102748|2.789851|0
Matadepera|Matadepera||Vallès Occidental|CAT|41.628359|2.012434|1
Matajudaica|Matajudaica||Baix Empordà|CAT|42.005311|3.043862|0
Matamala|Matemale||el Capcir|CN|42.576894|2.115493|1
Mataró|Mataró||Maresme|CAT|41.554247|2.446661|1
Matet|Matet||Alt Palància|PV|39.943707|-0.456426|1
Matola|Matola||Baix Vinalopó|PV|38.228491|-0.74695|0
Maurí|Maury||la Fenolleda|CN|42.815401|2.612945|1
Maçaners|Maçaners||Berguedà|CAT|42.233387|1.776791|0
Maçanet de Cabrenys|Maçanet de Cabrenys||Alt Empordà|CAT|42.384292|2.732753|1
Maçanet de la Selva|Maçanet de la Selva||Selva|CAT|41.763482|2.724789|1
Maçanet Residencial Parc|Maçanet Residencial Parc||Selva|CAT|41.77894|2.699401|0
Maó|Maó||Menorca|IB|39.915449|4.232282|1
Media Legua|Media Legua||Baix Segura|PV|38.096156|-0.918828|0
Medinyà|Medinyà||Gironès|CAT|42.047595|2.864964|0
Mediona|Mediona||Alt Penedès|CAT|41.481035|1.632135|1
Mediterráneo Almadraba|Mediterráneo Almadraba||Marina Baixa|PV|38.511623|-0.212298|0
Meliana|Meliana||Horta Nord|PV|39.525016|-0.334967|1
Melianta|Melianta||Pla de l'Estany|CAT|42.147201|2.771589|0
Melió|Melió||Segarra|CAT|41.672111|1.396804|0
Mentet|Mantet||el Conflent|CN|42.453366|2.294738|1
Menàrguens|Menàrguens||Noguera|CAT|41.738257|0.730091|1
Mequinensa|Mequinenza||el Baix Cinca|FR|41.338927|0.230092|1
Meranges|Meranges||Cerdanya|CAT|42.457889|1.765903|1
Meravelles-Cova Negra|Meravelles-Cova Negra||Safor|PV|38.968612|-0.234221|0
Meritxell|Meritxell|||AD|42.5676|1.59756|0
Merli|Merli||la Ribagorça|FR|42.342086|0.484877|0
Merymar|Merymar||Tarragonès|CAT|41.178905|1.45092|0
Miami Platja|Miami Platja||Baix Camp|CAT|41.013394|0.935539|0
Mieres|Mieres||Garrotxa|CAT|42.122092|2.631219|1
Mijaran|Mijaran||la Vall d'Aran|CAT|42.70897|0.797438|0
Mil Palmeras|Mil Palmeras||Baix Segura|PV|37.890927|-0.752863|0
Mil Palmeras|Mil Palmeras||Baix Segura|PV|37.886148|-0.76049|0
Millars|Millas||el Rosselló|CN|42.699216|2.689927|1
Millars|Millares||Canal de Navarrés|PV|39.211113|-0.793066|1
Millars|Millars||Gironès|CAT|41.988991|2.971095|0
Millena|Millena||Comtat|PV|38.740351|-0.378432|1
Milpins|Milpins||Vallès Oriental|CAT|41.63756|2.311069|0
Mines del Sant Pare|Mines del Sant Pare||Selva|CAT|41.947135|2.636339|0
Mira-rosa|Mira-rosa||Marina Alta|PV|38.848496|0.016568|0
Mira-Sol|Mira-Sol||Vallès Occidental|CAT|41.467883|2.055437|0
Miraflor|Miraflor||Marina Alta|PV|38.857607|0.014642|0
Miralcamp|Miralcamp||Pla d'Urgell|CAT|41.598359|0.895205|1
Miralles|Miralles||Baix Llobregat|CAT|41.451637|1.888225|0
Miralsot|Miralsot||el Baix Cinca|FR|41.55418|0.292261|0
Miramar|Miramar||Safor|PV|38.954811|-0.135566|1
Miramar|Miramar||Alt Camp|CAT|41.351757|1.230249|0
Mirambell|Mirambell||Anoia|CAT|41.731008|1.489562|0
Miravet|Miravet||Ribera d'Ebre|CAT|41.041367|0.577271|1
Mislata|Mislata||Horta Sud|PV|39.476005|-0.421466|1
Miànigues|Miànigues||Pla de l'Estany|CAT|42.105196|2.763284|0
Moixent|Mogente/Moixent||Costera|PV|38.86451|-0.797709|1
Moixent/Mogente|Moixent/Mogente||Costera|PV|38.873981|-0.751893|0
Moià|Moià||Moianès|CAT|41.819992|2.101776|1
Moja|Moja||Alt Penedès|CAT|41.324298|1.691759|0
Molig|Molitg-les-Bains||el Conflent|CN|42.664255|2.39586|1
Molino de la Ciudad|Molino de la Ciudad||Baix Segura|PV|38.079638|-0.975331|0
Molinos|Molinos||Pallars Jussà|CAT|42.405061|0.974331|0
Molins de Rei|Molins de Rei||Baix Llobregat|CAT|41.420746|2.034401|1
Molleres|Molleres|||AD|42.5676|1.59756|0
Mollerussa|Mollerussa||Pla d'Urgell|CAT|41.623713|0.885307|1
Mollet de Peralada|Mollet de Peralada||Alt Empordà|CAT|42.349541|3.009682|1
Mollet del Vallès|Mollet del Vallès||Vallès Oriental|CAT|41.548993|2.20985|1
Molló|Molló||Ripollès|CAT|42.369746|2.400369|1
Molló de la Creu|Molló de la Creu||Safor|PV|38.974497|-0.212974|0
Molí del Blanquillo|Molí del Blanquillo||Baix Penedès|CAT|41.243657|1.528486|0
Molí Foc|Molí Foc||Alacantí|PV|38.429089|-0.453977|0
Molí Nou|Molí Nou||Alacantí|PV|38.443582|-0.463375|0
Molíns|Molíns||Baix Segura|PV|38.08133|-0.9072|0
Moncofa|Moncofa||Plana Baixa|PV|39.79882|-0.15292|1
Monells|Monells||Baix Empordà|CAT|41.975349|2.999985|0
Monesma i Queixigar|Monesma y Cajigar||la Ribagorça|FR|42.229852|0.612153|1
Monistrol d'Anoia|Monistrol d'Anoia||Alt Penedès|CAT|41.446908|1.785333|0
Monistrol de Calders|Monistrol de Calders||Moianès|CAT|41.757451|2.020469|1
Monistrol de Montserrat|Monistrol de Montserrat||Bages|CAT|41.609526|1.842082|1
Monistrolet|Monistrolet||Bages|CAT|41.725278|1.766597|0
Monnars|Monnars||Tarragonès|CAT|41.135875|1.306261|0
Monsolis|Monsolis||Maresme|CAT|41.473327|2.289245|0
Mont|Mont||la Vall d'Aran|CAT|42.726909|0.797265|0
Mont Sant Pere Sescelades|Mont Sant Pere Sescelades||Tarragonès|CAT|41.135083|1.245418|0
Mont-ral|Mont-ral||Alt Camp|CAT|41.292752|1.091862|1
Mont-ras|Mont-ras||Baix Empordà|CAT|41.893909|3.148868|1
Mont-Rei|Mont-Rei||Gironès|CAT|41.830545|2.874687|0
Mont-roig|Mont-roig||Segarra|CAT|41.749904|1.184412|0
Mont-roig Badia|Mont-roig Badia||Baix Camp|CAT|41.04429|1.00203|0
Mont-roig de Tastavins|Monroyo||el Matarranya|FR|40.784143|-0.025714|1
Mont-roig del Camp|Mont-roig del Camp||Baix Camp|CAT|41.056645|0.955733|1
Mont-ros|Mont-ros||Pallars Jussà|CAT|42.402152|0.987425|0
Montagut|Montagut||Garrotxa|CAT|42.231456|2.59616|0
Montagut|Montagut||Gironès|CAT|42.060347|2.852737|0
Montagut|Montagut||Marina Baixa|PV|38.603981|-0.080639|0
Montagut i Oix|Montagut i Oix||Garrotxa|CAT|42.273783|2.553489|1
Montalba|Montalba||Plana Alta|PV|40.153111|-0.013801|0
Montalbà del Castell|Montalba-le-Château||la Fenolleda|CN|42.699677|2.565575|1
Montan de Tost|Montan de Tost||Alt Urgell|CAT|42.23514|1.387698|0
Montanejos|Montanejos||Alt Millars|PV|40.069367|-0.535098|1
Montanissell|Montanissell||Alt Urgell|CAT|42.199407|1.251646|0
Montant|Montán||Alt Millars|PV|40.029263|-0.550352|1
Montanui|Montanuy||la Ribagorça|FR|42.539676|0.680982|1
Montardit de Baix|Montardit de Baix||Pallars Sobirà|CAT|42.384528|1.115488|0
Montardit de Dalt|Montardit de Dalt||Pallars Sobirà|CAT|42.39492|1.104079|0
Montargull|Montargull||Noguera|CAT|41.969729|1.092944|0
Montargull|Montargull||Alt Urgell|CAT|42.237621|1.497815|0
Montaverner|Montaverner||Vall d'Albaida|PV|38.885575|-0.490811|1
Montañana|Montañana||la Ribagorça|FR|42.161062|0.678409|0
Montblanc|Montblanc||Conca de Barberà|CAT|41.361393|1.15449|1
Montblanquet|Montblanquet||Urgell|CAT|41.493806|1.114402|0
Montboló|Montbolo||el Vallespir|CN|42.491218|2.632239|1
Montbrió de la Marca|Montbrió de la Marca||Conca de Barberà|CAT|41.463344|1.295751|0
Montbrió del Camp|Montbrió del Camp||Baix Camp|CAT|41.11773|1.001355|1
Montbó|Montbó||Gironès|CAT|42.056712|2.767466|0
Montcada|Moncada||Horta Nord|PV|39.562897|-0.396538|1
Montcada|Montcada||Vallès Occidental|CAT|41.478634|2.187134|0
Montcada i Reixac|Montcada i Reixac||Vallès Occidental|CAT|41.489426|2.184339|1
Montcada/Moncada|Montcada/Moncada||Horta Nord|PV|39.545378|-0.394464|0
Montclar|Montclar||Berguedà|CAT|42.018747|1.775038|1
Montclar|Montclar||Baix Camp|CAT|41.142161|0.969916|0
Montclar d'Urgell|Montclar d'Urgell||Urgell|CAT|41.847665|1.042711|0
Montcorbau|Montcorbau||la Vall d'Aran|CAT|42.731363|0.790264|0
Montcortès|Montcortès||Pallars Sobirà|CAT|42.329207|1.005284|0
Montcortès de Segarra|Montcortès de Segarra||Segarra|CAT|41.705684|1.230288|0
Monte Alcedo|Monte Alcedo||Camp de Túria|PV|39.555793|-0.542457|0
Monte Alto|Monte Alto||Camp de Túria|PV|39.601011|-0.557499|0
Monte Cabrera|Monte Cabrera||Ribera Alta|PV|39.371201|-0.555331|0
Monte Colorado|Monte Colorado||Camp de Túria|PV|39.605163|-0.555269|0
Monte Pego|Monte Pego||Marina Alta|PV|38.846087|-4e-05|0
Monte Pino|Monte Pino||Safor|PV|38.980064|-0.257772|0
Monte Real|Monte Real||Safor|PV|38.954876|-0.19831|0
Monte Vernisa|Monte Vernisa||Safor|PV|38.918683|-0.291751|0
Monte-Pego|Monte-Pego||Marina Alta|PV|38.840215|-0.07065|0
Montebello|Montebello||Baix Segura|PV|38.065377|-0.749706|0
Monteblanco|Monteblanco||Camp de Túria|PV|39.669063|-0.587061|0
Montecollado|Montecollado||Camp de Túria|PV|39.635776|-0.619439|0
Montellà|Montellà||Cerdanya|CAT|42.353952|1.703883|0
Montellà i Martinet|Montellà i Martinet||Cerdanya|CAT|42.32174|1.693048|1
Montemar I - II|Montemar I - II||Baix Maestrat|PV|40.251771|0.270144|0
Montemolino|Montemolino||Plana Alta|PV|40.057555|0.053255|0
Montenartró|Montenartró||Pallars Sobirà|CAT|42.449554|1.2261|0
Montesa|Montesa||Costera|PV|38.941298|-0.641309|1
Montesano|Montesano||Camp de Túria|PV|39.577067|-0.505035|0
Montesclado|Montesclado||Pallars Sobirà|CAT|42.505203|1.244282|0
Montescot|Montescot||el Rosselló|CN|42.614157|2.923682|1
Montesol|Montesol||Vinalopó Mitjà|PV|38.312186|-0.747571|0
Montesquiu|Montesquiu||Osona|CAT|42.121096|2.212433|1
Montesquiu d'Albera|Montesquieu-des-Albères||el Rosselló|CN|42.51871|2.873299|1
Montfalcó d'Ossó|Montfalcó d'Ossó||Urgell|CAT|41.759348|1.131169|0
Montfalcó el Gros|Montfalcó el Gros||Anoia|CAT|41.657551|1.45444|0
Montfalcó Murallat|Montfalcó Murallat||Segarra|CAT|41.688706|1.33968|0
Montferrer|Montferrer||el Vallespir|CN|42.443472|2.564323|1
Montferrer|Montferrer||Alt Urgell|CAT|42.346383|1.424213|0
Montferrer i Castellbò|Montferrer i Castellbò||Alt Urgell|CAT|42.385227|1.328803|1
Montferri|Montferri||Alt Camp|CAT|41.259975|1.385854|1
Montfort|Monforte del Cid||Vinalopó Mitjà|PV|38.384992|-0.695004|1
Montfullà|Montfullà||Gironès|CAT|41.967012|2.760265|0
Montgai|Montgai||Noguera|CAT|41.796523|0.971417|1
Montgat|Montgat||Maresme|CAT|41.470889|2.279949|1
Montgó|Montgó||Marina Alta|PV|38.823572|0.122508|0
Montiboli|Montiboli||Marina Baixa|PV|38.495561|-0.266873|0
Montiró|Montiró||Alt Empordà|CAT|42.154395|3.070142|0
Montitxelvo|Montitxelvo/Montichelvo||Vall d'Albaida|PV|38.892646|-0.335536|1
Montjuïc|Montjuïc||Vallès Occidental|CAT|41.589165|2.166873|0
Montlloret|Montlloret||Selva|CAT|41.742391|2.835673|0
Montlluís|Mont-Louis||l'Alta Cerdanya|CN|42.509992|2.119719|1
Montmajor|Montmajor||Berguedà|CAT|42.00717|1.707269|1
Montmaneu|Montmaneu||Anoia|CAT|41.619094|1.399154|1
Montmar|Montmar||Maresme|CAT|41.584254|2.555551|0
Montmeló|Montmeló||Vallès Oriental|CAT|41.55685|2.251198|1
Montner|Montner||el Rosselló|CN|42.741506|2.687379|1
Montoliu de Lleida|Montoliu de Lleida||Segrià|CAT|41.553958|0.598019|1
Montoliu de Segarra|Montoliu de Segarra||Segarra|CAT|41.587766|1.270574|1
Montoriol|Montauriol||el Rosselló|CN|42.576511|2.724421|1
Montornès de Segarra|Montornès de Segarra||Segarra|CAT|41.599164|1.212602|1
Montornès del Vallès|Montornès del Vallès||Vallès Oriental|CAT|41.544736|2.265453|1
Montornès Nord|Montornès Nord||Vallès Oriental|CAT|41.555537|2.26501|0
Montornés|Montornés||Plana Alta|PV|40.051974|0.017853|0
Montortal|Montortal||Ribera Alta|PV|39.172851|-0.517666|0
Montpalau|Montpalau||Segarra|CAT|41.660997|1.367936|0
Montroi|Montroi/Montroy||Ribera Alta|PV|39.33265|-0.652344|1
Montseny|Montseny||Vallès Oriental|CAT|41.779702|2.390557|1
Montserrat|Montserrat||Ribera Alta|PV|39.363266|-0.584246|1
Montserrat Parc|Montserrat Parc||Anoia|CAT|41.606099|1.738634|0
Montsonís|Montsonís||Noguera|CAT|41.88669|1.022557|0
Montur|Montur||Ribera Alta|PV|39.391109|-0.611841|0
Montuïri|Montuïri||Mallorca|IB|39.564559|2.991113|1
Montví de Baix|Montví de Baix||Moianès|CAT|41.836523|2.094317|0
Monòver|Monòver/Monóvar||Vinalopó Mitjà|PV|38.428558|-0.912462|1
Moraira|Moraira||Marina Alta|PV|38.688078|0.133046|0
Moravit|Moravit||Marina Alta|PV|38.68927|0.110675|0
Morella|Morella||Ports|PV|40.597901|-0.075488|1
Morellàs i les Illes|Maureillas-las-Illas||el Vallespir|CN|42.460091|2.800815|1
Moron i Mola|Moron i Mola||la Vall d'Aran|CAT|42.833299|0.747605|0
Moror|Moror||Pallars Jussà|CAT|42.078799|0.835376|0
Moréns|Moréns||la Ribagorça|FR|42.373127|0.601627|0
Moscari|Moscari||Mallorca|IB|39.761671|2.93801|0
Mosoll|Mosoll||Cerdanya|CAT|42.375475|1.866565|0
Mosqueroles|Mosqueroles||Vallès Oriental|CAT|41.726463|2.44123|0
Mosset|Mosset||el Conflent|CN|42.674019|2.305078|1
Motilla|Motilla||Ribera Baixa|PV|39.252416|-0.261611|0
Muller|Muller||Segarra|CAT|41.708488|1.209899|0
Muntanya de la Sella|Muntanya de la Sella||Marina Alta|PV|38.808798|0.052496|0
Muntanya la Solana I|Muntanya la Solana I||Marina Alta|PV|38.795168|0.02128|0
Muntanya la Solana II|Muntanya la Solana II||Marina Alta|PV|38.785279|0.008922|0
Muntanyola|Muntanyola||Osona|CAT|41.890387|2.157482|1
Mura|Mura||Bages|CAT|41.683652|1.975075|1
Murla|Murla||Marina Alta|PV|38.765894|-0.077011|1""",
        """Muro|Muro||Mallorca|IB|39.7579|3.085594|1
Muro d'Alcoi|Muro de Alcoy||Comtat|PV|38.788715|-0.446125|1
Muscarola|Muscarola||Alt Penedès|CAT|41.336242|1.647055|0
Museros|Museros||Horta Nord|PV|39.574419|-0.359345|1
Mutxamel|Mutxamel||Alacantí|PV|38.434408|-0.474919|1
Móra d'Ebre|Móra d'Ebre||Ribera d'Ebre|CAT|41.098176|0.597251|1
Móra la Nova|Móra la Nova||Ribera d'Ebre|CAT|41.094412|0.66756|1
Músser|Músser||Cerdanya|CAT|42.38086|1.66581|0
na Macaret|na Macaret||Menorca|IB|40.016472|4.199212|0
na Taconera|na Taconera||Mallorca|IB|39.686357|3.443054|0
Nachá|Nachá||la Llitera|FR|41.956545|0.490929|0
Nagol|Nagol|||AD|42.46372|1.49129|0
Nalec|Nalec||Urgell|CAT|41.546856|1.117511|1
Nas|Nas||Cerdanya|CAT|42.348712|1.743588|0
Navaixes|Navajas||Alt Palància|PV|39.881427|-0.500523|1
Navalón de Abajo|Navalón de Abajo||Canal de Navarrés|PV|38.925967|-0.875047|0
Navarcles|Navarcles||Bages|CAT|41.754821|1.910368|1
Navarrés|Navarrés||Canal de Navarrés|PV|39.09246|-0.716227|1
Navata|Navata||Alt Empordà|CAT|42.217767|2.861733|1
Navàs|Navàs||Bages|CAT|41.888778|1.771796|1
Navès|Navès||Solsonès|CAT|42.047415|1.634869|1
Naüja|Nahuja||l'Alta Cerdanya|CN|42.423754|1.99794|1
Nefiac|Néfiach||el Rosselló|CN|42.695676|2.658546|1
Negrón|Negrón||Racó d'Ademús|PV|40.033327|-1.355716|0
Nerets|Nerets||Pallars Jussà|CAT|42.167583|0.908324|0
Neril|Neril||la Ribagorça|FR|42.471931|0.619542|0
Nevà|Nevà||Ripollès|CAT|42.313271|2.078161|0
Nieva|Nieva||Serrans|PV|39.710088|-1.1027|0
Niula|Niula||Cerdanya|CAT|42.430144|1.847659|0
Niàgara Parc|Niàgara Parc||Maresme|CAT|41.717574|2.681389|0
Noales|Noales||la Ribagorça|FR|42.458595|0.662696|0
Noedes|Nohèdes||el Conflent|CN|42.628497|2.261192|1
Noguero|Noguero||la Ribagorça|FR|42.240713|0.612242|0
Nonasp|Nonaspe||el Matarranya|FR|41.212363|0.242994|1
Nord-est de Treumal|Nord-est de Treumal||Baix Empordà|CAT|41.836742|3.072612|0
Nosa|Nosa||Bages|CAT|41.895483|1.877605|0
Nou Canyelles|Nou Canyelles||Garraf|CAT|41.276151|1.72257|0
Nova Alella|Nova Alella||Maresme|CAT|41.492719|2.289161|0
Nova Maravisa|Nova Maravisa||Camp de Túria|PV|39.58656|-0.51246|0
Novaliches|Novaliches||Alt Palància|PV|39.913746|-0.548577|0
Novelda|Novelda||Vinalopó Mitjà|PV|38.394262|-0.786129|1
Noves de Segre|Noves de Segre||Alt Urgell|CAT|42.293933|1.341743|0
Novetlè|Novetlè||Costera|PV|38.978409|-0.550615|1
Nuestra Señora de la Asunción|Nuestra Señora de la Asunción||Ribera Alta|PV|39.364409|-0.613053|0
Nules|Nules||Plana Baixa|PV|39.852819|-0.159299|1
Nulles|Nulles||Alt Camp|CAT|41.252585|1.28984|1
Nyer|Nyer||el Conflent|CN|42.501693|2.274472|1
Nàquera|Nàquera/Náquera||Camp de Túria|PV|39.637045|-0.413783|1
Oasi de Sant Vicent|Oasi de Sant Vicent||Camp de Túria|PV|39.649617|-0.570019|0
Obispo Hernández/Las Eras|Obispo Hernández/Las Eras||Serrans|PV|39.886838|-1.010837|0
Odèn|Odèn||Solsonès|CAT|42.122813|1.438938|1
Ogassa|Ogassa||Ripollès|CAT|42.264572|2.252554|1
Ogern|Ogern||Alt Urgell|CAT|42.023555|1.339292|0
Oix|Oix||Garrotxa|CAT|42.272144|2.528322|0
Olesa de Bonesvalls|Olesa de Bonesvalls||Alt Penedès|CAT|41.353426|1.852288|1
Olesa de Montserrat|Olesa de Montserrat||Baix Llobregat|CAT|41.553706|1.901497|1
Oleta|Olette||el Conflent|CN|42.596223|2.24223|1
Oliana|Oliana||Alt Urgell|CAT|42.09488|1.322968|1
Oliola|Oliola||Noguera|CAT|41.857094|1.173136|1
Olius|Olius||Solsonès|CAT|41.992512|1.530423|1
Oliva|Oliva||Safor|PV|38.896736|-0.104175|1
Olivella|Olivella||Garraf|CAT|41.299379|1.8117|1
Olivella-Urbanització|Olivella-Urbanització||Garraf|CAT|41.311991|1.772397|0
Olià|Olià||Cerdanya|CAT|42.354929|1.743272|0
Ollers|Ollers||Conca de Barberà|CAT|41.434018|1.218652|0
Olocau|Olocau||Camp de Túria|PV|39.688203|-0.52751|1
Olocau del Rei|Olocau del Rey||Ports|PV|40.641515|-0.339282|1
Olopte|Olopte||Cerdanya|CAT|42.393772|1.81475|0
Olost|Olost||Lluçanès|CAT|42.000969|2.094749|1
Olot|Olot||Garrotxa|CAT|42.176884|2.493976|1
Olp|Olp||Pallars Sobirà|CAT|42.430143|1.114536|0
Olvan|Olvan||Berguedà|CAT|42.062879|1.907744|1
Olèrdola|Olèrdola||Alt Penedès|CAT|41.319289|1.724473|1
Omet|Omet||Horta Sud|PV|39.353986|-0.474818|0
Oms|Oms||el Rosselló|CN|42.533928|2.70919|1
Onda|Onda||Plana Baixa|PV|39.979703|-0.227812|1
Ondara|Ondara||Marina Alta|PV|38.826978|0.015075|1
Onil|Onil||Alcoià|PV|38.633355|-0.660997|1
Ontinyent|Ontinyent||Vall d'Albaida|PV|38.812695|-0.655201|1
Orba|Orba||Marina Alta|PV|38.785082|-0.066166|1
Orbanyà|Urbanya||el Conflent|CN|42.643089|2.300017|1
Orcau|Orcau||Pallars Jussà|CAT|42.163016|0.982343|0
Ordal|Ordal||Alt Penedès|CAT|41.395686|1.849794|0
Ordenació Riells I Viabrea|Ordenació Riells I Viabrea||Selva|CAT|41.732127|2.54988|0
Ordino|Ordino|||AD|42.55623|1.53319|0
Ordis|Ordis||Alt Empordà|CAT|42.215904|2.894557|1
Ordèn|Ordèn||Cerdanya|CAT|42.40099|1.773652|0
Orellà|Oreilla||el Conflent|CN|42.576227|2.222424|1
Orfes|Orfes||Pla de l'Estany|CAT|42.172908|2.866972|0
Organyà|Organyà||Alt Urgell|CAT|42.210638|1.319026|1
Orient|Orient||Mallorca|IB|39.73456|2.760213|0
Oriola|Orihuela||Baix Segura|PV|38.067202|-0.909376|1
Oristà|Oristà||Lluçanès|CAT|41.948991|2.07035|1
Orito|Orito||Vinalopó Mitjà|PV|38.377685|-0.688839|0
Orpesa|Orpesa/Oropesa del Mar||Plana Alta|PV|40.092004|0.123061|1
Orpí|Orpí||Anoia|CAT|41.524237|1.577794|1
Orriols|Orriols||Alt Empordà|CAT|42.125189|2.906561|0
Ortells|Ortells||Ports|PV|40.69127|-0.017651|0
Orxeta|Orxeta||Marina Baixa|PV|38.553518|-0.279761|1
Orís|Orís||Osona|CAT|42.069663|2.223113|1
Os de Balaguer|Os de Balaguer||Noguera|CAT|41.899983|0.681906|1
Os de Civís|Os de Civís||Alt Urgell|CAT|42.510276|1.439423|0
Oset|Oset||Serrans|PV|39.813617|-0.77394|0
Osor|Osor||Selva|CAT|41.941773|2.562671|1
Osseja|Osséja||l'Alta Cerdanya|CN|42.395194|1.997073|1
Ossera|Ossera||Alt Urgell|CAT|42.217447|1.47165|0
Ossó de Sió|Ossó de Sió||Urgell|CAT|41.746095|1.147181|1
Otonel|Otonel||Vall de Cofrents|PV|39.258712|-0.8492|0
Otos|Otos||Vall d'Albaida|PV|38.868939|-0.447832|1
Oveix|Oveix||Pallars Jussà|CAT|42.394293|0.949458|0
P.i.antic de Vic|P.i.antic de Vic||Vallès Oriental|CAT|41.623679|2.315149|0
Pacs del Penedès|Pacs del Penedès||Alt Penedès|CAT|41.36028|1.666184|1
Padrinàs|Padrinàs||Alt Urgell|CAT|42.217726|1.49682|0
Paiporta|Paiporta||Horta Sud|PV|39.425879|-0.412316|1
Pal|Pal|||AD|42.54499|1.51483|0
Palafolls|Palafolls||Maresme|CAT|41.671457|2.730125|1
Palafrugell|Palafrugell||Baix Empordà|CAT|41.913728|3.168798|1
Palamós|Palamós||Baix Empordà|CAT|41.846359|3.129078|0
Palamós|Palamós||Segarra|CAT|41.640002|1.377762|0
Palamós i Sant Joan|Palamós i Sant Joan||Baix Empordà|CAT|41.866027|3.130301|1
Palanques|Palanques||Ports|PV|40.709918|-0.190161|1
Palau de Cerdanya|Palau-de-Cerdagne||l'Alta Cerdanya|CN|42.386022|1.979335|1
Palau de Noguera|Palau de Noguera||Pallars Jussà|CAT|42.147241|0.896196|0
Palau de Santa Eulàlia|Palau de Santa Eulàlia||Alt Empordà|CAT|42.179318|2.975347|1
Palau del Vidre|Palau-del-Vidre||el Rosselló|CN|42.568431|2.958182|1
Palau-sator|Palau-sator||Baix Empordà|CAT|41.984001|3.119874|1
Palau-saverdera|Palau-saverdera||Alt Empordà|CAT|42.300544|3.142023|1
Palau-solità i Plegamans|Palau-solità i Plegamans||Vallès Occidental|CAT|41.58048|2.174947|1
Palaudalba|Palaudalba||Vallès Oriental|CAT|41.590514|2.198184|0
Palaudaries|Palaudaries||Vallès Oriental|CAT|41.608269|2.199738|0
Pallaresos Park|Pallaresos Park||Tarragonès|CAT|41.177739|1.260613|0
Pallejà|Pallejà||Baix Llobregat|CAT|41.421589|1.985601|1
Pallerol|Pallerol||la Ribagorça|FR|42.336218|0.70451|0
Pallerols|Pallerols||Segarra|CAT|41.618311|1.353168|0
Pallerols del Cantó|Pallerols del Cantó||Alt Urgell|CAT|42.356119|1.311743|0
Palma|Palma||Mallorca|IB|39.556131|2.715485|1
Palma de Gandia|Palma de Gandía||Safor|PV|38.9456|-0.237748|1
Palmanova|Palmanova||Mallorca|IB|39.525672|2.541657|0
Palmanyola|Palmanyola||Mallorca|IB|39.661628|2.671281|0
Palmera|Palmera||Safor|PV|38.942574|-0.151717|1
Palmeria|Palmeria||Marina Alta|PV|38.780814|-0.071598|0
Palol d'Onyar|Palol d'Onyar||Gironès|CAT|41.953479|2.842094|0
Palol de Revardit|Palol de Revardit||Pla de l'Estany|CAT|42.056188|2.803856|1
Palou|Palou||Segarra|CAT|41.817911|1.251139|0
Palouet|Palouet||Segarra|CAT|41.796624|1.357015|0
Pals|Pals||Baix Empordà|CAT|41.982711|3.168905|1
Palsmar|Palsmar||Baix Empordà|CAT|41.985266|3.197976|0
Pamis|Pamis||Marina Alta|PV|38.816024|0.012483|0
Pantano de Benagéber|Pantano de Benagéber||Serrans|PV|39.727631|-1.106664|0
Pantans de Montbarbat|Pantans de Montbarbat||Selva|CAT|41.745861|2.780458|0
Papalús|Papalús||Selva|CAT|41.698371|2.818317|0
Paradis|Paradis||Maresme|CAT|41.512608|2.313032|0
Parc de Calella|Parc de Calella||Maresme|CAT|41.619892|2.655849|0
Parc de Cubelles|Parc de Cubelles||Garraf|CAT|41.202918|1.646043|0
Parc de Llevant 2|Parc de Llevant 2||Tarragonès|CAT|41.152536|1.313322|0
Parc Industrial|Parc Industrial||Pla d'Urgell|CAT|41.644923|0.902646|0
Parc Riu Clar|Parc Riu Clar||Tarragonès|CAT|41.1238|1.220346|0
Parc Sant Daniel|Parc Sant Daniel||Baix Empordà|CAT|41.862575|3.108025|0
Parcent|Parcent||Marina Alta|PV|38.739188|-0.073883|1
Pardinella|Pardinella||la Ribagorça|FR|42.353289|0.577219|0
Pardines|Pardines||Ripollès|CAT|42.30629|2.230459|1
Parets del Vallès|Parets del Vallès||Vallès Oriental|CAT|41.567348|2.232275|1
Parlavà|Parlavà||Baix Empordà|CAT|42.019979|3.042291|1
Parque Ansaldo|Parque Ansaldo||Alacantí|PV|38.410237|-0.433739|0
Parque Calablanca|Parque Calablanca||Marina Alta|PV|38.760749|0.203758|0
Parque Monte Alcedo|Parque Monte Alcedo||Camp de Túria|PV|39.551575|-0.546257|0
Partida Sant Joan de Ruanes|Partida Sant Joan de Ruanes||Alt Camp|CAT|41.274139|1.272032|0
Partida Siscar|Partida Siscar||Camp de Túria|PV|39.674939|-0.428609|0
Passanant|Passanant||Conca de Barberà|CAT|41.531889|1.197072|0
Passanant i Belltall|Passanant i Belltall||Conca de Barberà|CAT|41.520375|1.199719|1
Passatge Ramió|Passatge Ramió||Pla de l'Estany|CAT|42.101363|2.764877|0
Passeig del Marques Casa Riera|Passeig del Marques Casa Riera||Maresme|CAT|41.564638|2.515531|0
Paterna|Paterna||Horta Nord|PV|39.524007|-0.467556|1
Pau|Pau||Alt Empordà|CAT|42.309148|3.113811|1
PAU 21-Colinas Golf|PAU 21-Colinas Golf||Baix Segura|PV|37.927367|-0.807765|0
Pau 8-Canal Norte|Pau 8-Canal Norte||Baix Segura|PV|37.948336|-0.750901|0
Pavia|Pavia||Segarra|CAT|41.597845|1.34717|0
Pavies|Pavías||Alt Palància|PV|39.970066|-0.481701|1
Pavos|Pavos||Plana Alta|PV|39.923084|-0.016391|0
Paçà|Passa||el Rosselló|CN|42.565403|2.804986|1
Paüls|Paüls||Baix Ebre|CAT|40.927534|0.408909|1
Paüls de Flamisell|Paüls de Flamisell||Pallars Jussà|CAT|42.396454|0.982897|0
Pedralba|Pedralba||Serrans|PV|39.596345|-0.727394|1
Pedreguer|Pedreguer||Marina Alta|PV|38.793461|0.031469|1
Pedret i Marzà|Pedret i Marzà||Alt Empordà|CAT|42.306943|3.069891|1
Pedrol|Pedrol||Tarragonès|CAT|41.133529|1.253138|0
Pego|Pego||Marina Alta|PV|38.843667|-0.099964|1
Peguera|Peguera||Mallorca|IB|39.536742|2.443953|0
Pegueroles|Pegueroles||Solsonès|CAT|41.990631|1.705091|0
Pelacalç|Pelacalç||Alt Empordà|CAT|42.144939|3.07099|0
Pelagalls|Pelagalls||Segarra|CAT|41.751116|1.204058|0
Pelai|Pelai||Baix Camp|CAT|41.160077|1.084089|0
Pena-roja|Peñarroya de Tastavins||el Matarranya|FR|40.746787|0.065474|1
Penelles|Penelles||Noguera|CAT|41.729512|0.949732|1
Penya Forcada|Penya Forcada||Baix Llobregat|CAT|41.378323|1.892092|0
Penàguila|Penàguila||Alcoià|PV|38.658153|-0.373503|1
Penén de Albosa|Penén de Albosa||Plana d'Utiel|PV|39.403673|-1.276213|0
Peníscola|Peníscola/Peñíscola||Baix Maestrat|PV|40.356934|0.355109|1
Perafita|Perafita||Lluçanès|CAT|42.048063|2.107405|1
Perafort|Perafort||Tarragonès|CAT|41.189068|1.24063|1
Peralada|Peralada||Alt Empordà|CAT|42.301839|3.029769|1
Peralta de Calassanç|Peralta de Calasanz||la Llitera|FR|42.000784|0.384037|1
Peralta de la Sal|Peralta de la Sal||la Llitera|FR|41.992648|0.386196|0
Peramea|Peramea||Pallars Sobirà|CAT|42.32871|1.048135|0
Peramola|Peramola||Alt Urgell|CAT|42.080119|1.263495|1
Peratallada|Peratallada||Baix Empordà|CAT|41.977024|3.090013|0
Perelló-Mar|Perelló-Mar||Baix Ebre|CAT|40.826048|0.739734|0
Perestortes|Peyrestortes||el Rosselló|CN|42.748278|2.845442|1
Perles|Perles||Alt Urgell|CAT|42.177997|1.393694|0
Perpinyà|Perpignan||el Rosselló|CN|42.696441|2.898977|1
Perves|Perves||Alta Ribagorça|CAT|42.362157|0.850134|0
Pesillà de Conflent|Pézilla-de-Conflent||la Fenolleda|CN|42.735706|2.48904|1
Pesillà de la Ribera|Pézilla-la-Rivière||el Rosselló|CN|42.705318|2.764565|1
Pessonada|Pessonada||Pallars Jussà|CAT|42.214767|1.020905|0
Petra|Petra||Mallorca|IB|39.628732|3.142976|1
Petrer|Petrer||Vinalopó Mitjà|PV|38.492163|-0.731658|1
Petrés|Petrés||Camp de Morvedre|PV|39.683908|-0.311351|1
Peñalva|Peñalva||Alt Palància|PV|39.862831|-0.476966|0
Pi|Pi||Cerdanya|CAT|42.351648|1.76235|0
Pi de Conflent|Py||el Conflent|CN|42.473126|2.359996|1
Picamoixons|Picamoixons||Alt Camp|CAT|41.302495|1.202237|0
Picanya|Picanya||Horta Sud|PV|39.433902|-0.436218|1
Picassent|Picassent||Horta Sud|PV|39.347454|-0.48609|1
Piera|Piera||Anoia|CAT|41.511152|1.749945|1
Piles|Piles||Safor|PV|38.944387|-0.127853|1
Pilo Gros|Pilo Gros||Foia de Bunyol|PV|39.466671|-0.73012|0
Pilzán|Pilzán||la Ribagorça|FR|42.051982|0.485736|0
Pina|Pina de Montalgrao||Alt Palància|PV|40.014325|-0.649646|1
Pina|Pina||Mallorca|IB|39.603674|2.922222|0
Pinalbert|Pinalbert||Tarragonès|CAT|41.154739|1.314282|0
Pinar del Abogat|Pinar del Abogat||Marina Alta|PV|38.687976|0.117901|0
Pinar i Portús|Pinar i Portús||Vallès Oriental|CAT|41.669258|2.23946|0
Pinares del Meclí|Pinares del Meclí||Alcoià|PV|38.527216|-0.614552|0
Pineda de Bages|Pineda de Bages||Bages|CAT|41.759403|1.834607|0
Pineda de Mar|Pineda de Mar||Maresme|CAT|41.632331|2.672587|1
Pineda de Santa Cristina|Pineda de Santa Cristina||Alt Camp|CAT|41.279348|1.435284|0
Pineda del Vallés|Pineda del Vallés||Vallès Oriental|CAT|41.609058|2.213318|0
Pineda Pals|Pineda Pals||Baix Empordà|CAT|41.980786|3.195911|0
Pinedo|Pinedo|||PV|39.421308|-0.338695|0
Pinell de Solsonès|Pinell de Solsonès||Solsonès|CAT|41.951648|1.368689|1
Pinemar|Pinemar||Maresme|CAT|41.635413|2.692131|0
Pinet|Pinet||Vall d'Albaida|PV|38.984186|-0.326906|1
Pins Manous|Pins Manous||Tarragonès|CAT|41.150676|1.277397|0
Pinós|Pinós||Solsonès|CAT|41.84447|1.550061|1
Pira|Pira||Conca de Barberà|CAT|41.420642|1.192131|1
Pià|Pia||el Rosselló|CN|42.749129|2.914834|1
Pla Contxeta|Pla Contxeta||Alacantí|PV|38.412959|-0.514211|0
Pla de Can Migrat|Pla de Can Migrat||Baix Llobregat|CAT|41.564536|1.810274|0
Pla de Can Vilarnau|Pla de Can Vilarnau||Pla de l'Estany|CAT|42.102295|2.768128|0
Pla de Corrals|Pla de Corrals||Safor|PV|39.014443|-0.367607|0""",
        """Pla de l'Avellà|Pla de l'Avellà||Maresme|CAT|41.510376|2.400764|0
Pla de l'Ermita|Pla de l'Ermita||Alta Ribagorça|CAT|42.513884|0.85806|0
Pla de la Costa|Pla de la Costa||Vallès Oriental|CAT|41.702423|2.160299|0
Pla de la Cova|Pla de la Cova||Camp de Túria|PV|39.601868|-0.527727|0
Pla de la Font|Pla de la Font||Selva|CAT|41.785595|2.777811|0
Pla de la Torre|Pla de la Torre||Plana Alta|PV|39.919536|-0.091905|0
Pla de la Torre|Pla de la Torre||Camp de Túria|PV|39.662085|-0.518995|0
Pla de Ses Vinyes|Pla de Ses Vinyes||Baix Empordà|CAT|41.931735|3.191236|0
Pla de Xirau|Pla de Xirau||Alacantí|PV|38.466631|-0.594091|0
Pla del Castell|Pla del Castell||Baix Llobregat|CAT|41.568108|1.814673|0
Pla del Mar|Pla del Mar||Marina Alta|PV|38.693977|0.140594|0
Pla del Pelac|Pla del Pelac||Baix Llobregat|CAT|41.37298|1.889372|0
Pla del Pèlec|Pla del Pèlec||Alt Penedès|CAT|41.36664|1.884084|0
Pla del Remei|Pla del Remei||Vallès Oriental|CAT|41.699073|2.435609|0
Pla del Temple|Pla del Temple||Vallès Oriental|CAT|41.686555|2.476718|0
Pla Olivera|Pla Olivera||Alacantí|PV|38.436197|-0.552695|0
Pla Parcial Platja|Pla Parcial Platja||Marina Baixa|PV|38.576239|-0.06818|0
Pla Parcial Sector 28|Pla Parcial Sector 28||Baix Camp|CAT|41.047815|0.998568|0
Pladevall|Pladevall||Garrotxa|CAT|42.148074|2.461941|0
Plandogau|Plandogau||Noguera|CAT|41.86723|1.209097|0
Planes|Planes||Comtat|PV|38.792285|-0.331585|1
Planes Reine|Planes Reine||Baix Maestrat|PV|40.574534|0.392907|0
Planeses|Planèzes||la Fenolleda|CN|42.774381|2.621957|1
Planoles|Planoles||Ripollès|CAT|42.321942|2.102567|1
Plans-Gasparot|Plans-Gasparot||Marina Baixa|PV|38.503802|-0.244111|0
Planès|Planès||l'Alta Cerdanya|CN|42.472483|2.145449|1
Planès|Planès||Ripollès|CAT|42.318185|2.098225|0
Platges de Fornells|Platges de Fornells||Menorca|IB|40.048455|4.111015|0
Platja d'Alcúdia|Platja d'Alcúdia||Mallorca|IB|39.833219|3.117817|0
Platja d'Almenara Casablanca|Platja d'Almenara Casablanca||Plana Baixa|PV|39.740112|-0.180106|0
Platja d'Aro|Platja d'Aro||Baix Empordà|CAT|41.817582|3.067324|0
Platja d'en Bossa|Platja d'en Bossa||Eivissa|IB|38.885011|1.402965|0
Platja de Binissafúller|Platja de Binissafúller||Menorca|IB|39.831418|4.215863|0
Platja de Canet|Platja de Canet||Camp de Morvedre|PV|39.678313|-0.204113|0
Platja de Muro|Platja de Muro||Mallorca|IB|39.80572|3.11763|0
Platja de Nules|Platja de Nules||Plana Baixa|PV|39.826723|-0.110481|0
Platja de Puçol|Platja de Puçol||Horta Nord|PV|39.60707|-0.266122|0
Platja de Tavernes|Platja de Tavernes||Safor|PV|39.058232|-0.205737|0
Platja de Xeraco|Platja de Xeraco||Safor|PV|39.047464|-0.193932|0
Platja de Xilxes|Platja de Xilxes||Plana Baixa|PV|39.766651|-0.158069|0
Platja del Rei|Platja del Rei||Ribera Baixa|PV|39.244158|-0.256815|0
Playa|Playa||Horta Nord|PV|39.575617|-0.27725|0
Playa Brava|Playa Brava||Selva|CAT|41.707847|2.892826|0
Playa Flamenca I|Playa Flamenca I||Baix Segura|PV|37.941076|-0.716446|0
Playa Flamenca II|Playa Flamenca II||Baix Segura|PV|37.934168|-0.731504|0
Playa Romana-Carregador|Playa Romana-Carregador||Baix Maestrat|PV|40.235749|0.27018|0
Pleta de Bolvir-Ger|Pleta de Bolvir-Ger||Cerdanya|CAT|42.418701|1.867714|0
Pobellà|Pobellà||Pallars Jussà|CAT|42.398005|0.989166|0
Poblado Cortes-La Muela|Poblado Cortes-La Muela||Vall de Cofrents|PV|39.259136|-0.937936|0
Poblado Iberdrola|Poblado Iberdrola||Vall de Cofrents|PV|39.259616|-0.948282|0
Poblats Maritims|Poblats Maritims||Plana Baixa|PV|39.871516|-0.064623|0
Poble|Poble||Baix Empordà|CAT|41.930125|3.131996|0
Poble Antic de Gavarda|Poble Antic de Gavarda||Ribera Alta|PV|39.091285|-0.560154|0
Poble Nou|Poble Nou|||PV|39.506391|-0.384176|0
Poble Nou de Sant Rafel|Poble Nou de Sant Rafel||Comtat|PV|38.711133|-0.44077|0
Poblet|Poblet||Conca de Barberà|CAT|41.380713|1.080664|0
Pobletà|Pobletà||Bages|CAT|41.798392|1.77775|0
Poboleda|Poboleda||Priorat|CAT|41.231829|0.845083|1
Poima|Poima||Menorca|IB|39.887658|4.241493|0
Polinyà|Polinyà||Vallès Occidental|CAT|41.557918|2.152116|1
Polinyà de Xúquer|Polinyà de Xúquer||Ribera Baixa|PV|39.190676|-0.37231|1
Politg|Politg||Noguera|CAT|41.981436|1.219637|0
Pollença|Pollença||Mallorca|IB|39.886722|3.029504|1
Pollestres|Pollestres||el Rosselló|CN|42.637487|2.872868|1
Polop|Polop||Marina Baixa|PV|38.620773|-0.152433|1
Pols|Pols||Alt Empordà|CAT|42.225495|2.892144|0
Polígon Barcelones|Polígon Barcelones||Baix Llobregat|CAT|41.508649|1.897234|0
Polígon Empordá Internacional|Polígon Empordá Internacional||Alt Empordà|CAT|42.228483|2.981303|0
Polígon Industrial|Polígon Industrial||Maresme|CAT|41.528777|2.416917|0
Polígon Industrial|Polígon Industrial||Alt Penedès|CAT|41.396752|1.660034|0
Polígon Industrial Can Jorn|Polígon Industrial Can Jorn||Vallès Oriental|CAT|41.627206|2.325559|0
Polígon Industrial Can Prunera|Polígon Industrial Can Prunera||Baix Llobregat|CAT|41.374659|1.909152|0
Polígon Industrial de Can Valero|Polígon Industrial de Can Valero||Mallorca|IB|39.595928|2.633804|0
Polígon industrial de Rosanes|Polígon industrial de Rosanes||Baix Llobregat|CAT|41.453592|1.892926|0
Polígon Industrial de Sant Pere Molanta|Polígon Industrial de Sant Pere Molanta||Alt Penedès|CAT|41.355333|1.732411|0
Polígon Industrial de Son Castello|Polígon Industrial de Son Castello||Mallorca|IB|39.602202|2.664166|0
Polígon Industrial dels Vents|Polígon Industrial dels Vents||Camp de Túria|PV|39.596222|-0.391988|0
Polígon Industrial des Castell|Polígon Industrial des Castell||Menorca|IB|39.874574|4.282774|0
Polígon Industrial el Clot de Moja|Polígon Industrial el Clot de Moja||Alt Penedès|CAT|41.331147|1.682898|0
Polígon Industrial Ferreries|Polígon Industrial Ferreries||Menorca|IB|39.987599|4.022561|0
Polígon Industrial L'Avenar|Polígon Industrial L'Avenar||Tarragonès|CAT|41.183064|1.44968|0
Polígon Industrial la Pobla|Polígon Industrial la Pobla||Bages|CAT|41.81004|1.758525|0
Polígon Industrial Pla de la Bruguera|Polígon Industrial Pla de la Bruguera||Vallès Occidental|CAT|41.597239|2.091903|0
Polígon Industrial Puigtió|Polígon Industrial Puigtió||Selva|CAT|41.790956|2.729958|0
Polígon Industrial Riera de Caldes|Polígon Industrial Riera de Caldes||Vallès Occidental|CAT|41.556855|2.180813|0
Polígon Industrial Sector Autopista|Polígon Industrial Sector Autopista||Vallès Oriental|CAT|41.557256|2.236859|0
Polígon Industrial Z|Polígon Industrial Z||Vallès Oriental|CAT|41.566165|2.237111|0
Polígon Llevant Industrial|Polígon Llevant Industrial||Vallès Oriental|CAT|41.57829|2.250562|0
Polígon Riera d'Esclanya|Polígon Riera d'Esclanya||Baix Empordà|CAT|41.935215|3.158103|0
Polígon Sant Ermengol|Polígon Sant Ermengol||Baix Llobregat|CAT|41.517972|1.890887|0
Polígono de la Florida|Polígono de la Florida||Alt Vinalopó|PV|38.523478|-0.907466|0
Pomar|Pomar||Segarra|CAT|41.626554|1.349071|0
Pont de Molins|Pont de Molins||Alt Empordà|CAT|42.320108|2.925917|1
Pont de Tiana|Pont de Tiana||Maresme|CAT|41.473353|2.277954|0
Pont-xetmar|Pont-xetmar||Pla de l'Estany|CAT|42.094632|2.799303|0
Pontaut|Pontaut||la Vall d'Aran|CAT|42.835414|0.730572|0
Pontellà|Ponteilla||el Rosselló|CN|42.63277|2.826257|1
Pontils|Pontils||Conca de Barberà|CAT|41.468927|1.392253|1
Pontons|Pontons||Alt Penedès|CAT|41.412709|1.512096|1
Ponts|Ponts||Noguera|CAT|41.915462|1.16642|1
Pontós|Pontós||Alt Empordà|CAT|42.184755|2.911506|1
Porcingles|Porcingles||la Vall d'Aran|CAT|42.826641|0.770828|0
Porqueres|Porqueres||Pla de l'Estany|CAT|42.121828|2.730341|1
Porrera|Porrera||Priorat|CAT|41.196433|0.85507|1
Porreres|Porreres||Mallorca|IB|39.507281|3.030759|1
Port|Port||Marina Alta|PV|38.689088|0.144984|0
Port d'Addaia|Port d'Addaia||Menorca|IB|40.008665|4.195994|0
Port d'Alcúdia|Port d'Alcúdia||Mallorca|IB|39.839114|3.157791|0
Port d'Andratx|Port d'Andratx||Mallorca|IB|39.553364|2.383911|0
Port de Pollença|Port de Pollença||Mallorca|IB|39.89878|3.071016|0
Port de sa Calobra|Port de sa Calobra||Mallorca|IB|39.844592|2.793062|0
Port de Sant Miquel|Port de Sant Miquel||Eivissa|IB|39.079717|1.44203|0
Port de Valldemosa|Port de Valldemosa||Mallorca|IB|39.717973|2.58947|0
Port Romà|Port Romà||Tarragonès|CAT|41.161254|1.451762|0
Port Verd|Port Verd||Mallorca|IB|39.626752|3.394264|0
Porta|Porta||l'Alta Cerdanya|CN|42.515767|1.791921|1
Porta-Coeli|Porta-Coeli||Camp de Túria|PV|39.643496|-0.514314|0
Portals Nous|Portals Nous||Mallorca|IB|39.535469|2.568939|0
Portals Vells|Portals Vells||Mallorca|IB|39.473564|2.518899|0
Portbou|Portbou||Alt Empordà|CAT|42.426962|3.138122|1
Portell|Portell||Segarra|CAT|41.742107|1.383583|0
Portell de Morella|Portell de Morella||Ports|PV|40.539298|-0.26812|1
Portimar|Portimar||Maresme|CAT|41.587437|2.557337|0
Portinatx|Portinatx||Eivissa|IB|39.108551|1.511327|0
Portinyol|Portinyol||Maresme|CAT|41.583338|2.560372|0
Porto Cristo|Porto Cristo||Mallorca|IB|39.544694|3.337256|0
Portocolom|Portocolom||Mallorca|IB|39.417137|3.252395|0
Portopetro|Portopetro||Mallorca|IB|39.358672|3.209466|0
Portvendres|Port-Vendres||el Rosselló|CN|42.502458|3.104277|1
Portè|Porté-Puymorens||l'Alta Cerdanya|CN|42.555344|1.835055|1
Potries|Potries||Safor|PV|38.914741|-0.190298|1
Pou del Merli|Pou del Merli||Baix Llobregat|CAT|41.474852|1.902437|0
Pozo San Lázaro|Pozo San Lázaro||Camp de Túria|PV|39.591173|-0.51369|0
Prada|Prades||el Conflent|CN|42.614873|2.430687|1
Pradell|Pradell||Noguera|CAT|41.796885|1.02991|0
Pradell de la Teixeta|Pradell de la Teixeta||Priorat|CAT|41.152119|0.873368|1
Prades|Prades||Baix Camp|CAT|41.314732|0.991479|1
Prades|Prades||Solsonès|CAT|41.797666|1.579464|0
Prat de Comte|Prat de Comte||Terra Alta|CAT|40.975235|0.411196|1
Pratdalles|Pratdalles||Pla de l'Estany|CAT|42.108467|2.750961|0
Pratdip|Pratdip||Baix Camp|CAT|41.054327|0.881148|1
Prats|Prats|||AD|42.5676|1.59756|0
Prats|Prats||Cerdanya|CAT|42.365149|1.836841|0
Prats de Lluçanès|Prats de Lluçanès||Lluçanès|CAT|41.997926|2.023164|1
Prats de Molló i la Presta|Prats-de-Mollo-la-Preste||el Vallespir|CN|42.419942|2.441077|1
Prats de Sornià|Prats-de-Sournia||la Fenolleda|CN|42.742574|2.459292|1
Prats i Sansor|Prats i Sansor||Cerdanya|CAT|42.366148|1.833081|1
Preixana|Preixana||Urgell|CAT|41.601467|1.046558|1
Preixens|Preixens||Noguera|CAT|41.800792|1.033747|1
Premià de Dalt|Premià de Dalt||Maresme|CAT|41.505771|2.343898|1
Premià de Mar|Premià de Mar||Maresme|CAT|41.492216|2.358168|1
Prenafeta|Prenafeta||Conca de Barberà|CAT|41.378468|1.22239|0
Prullans|Prullans||Cerdanya|CAT|42.387821|1.727038|1
Prunet i Bellpuig|Prunet-et-Belpuig||el Rosselló|CN|42.566308|2.633524|1
Prunyanes|Prugnanes||la Fenolleda|CN|42.826525|2.436329|1
Pueblo Blanco|Pueblo Blanco||Marina Alta|PV|38.780713|0.184349|0
Pueblo Español|Pueblo Español||Alacantí|PV|38.436942|-0.381543|0
Pueblo Príncipe|Pueblo Príncipe||Baix Segura|PV|37.935313|-0.76915|0
Puente de Don Pedro|Puente de Don Pedro||Baix Segura|PV|38.095428|-0.818767|0
Puente los Cirios|Puente los Cirios||Baix Segura|PV|38.094183|-0.901225|0
Puig d'en Valls|Puig d'en Valls||Eivissa|IB|38.926236|1.434856|0
Puig de Can Vinyets|Puig de Can Vinyets||Eivissa|IB|38.919521|1.470262|0
Puig de Pedra-Sot del Pi|Puig de Pedra-Sot del Pi||Maresme|CAT|41.503863|2.353132|0
Puig de Vidre|Puig de Vidre||Alt Penedès|CAT|41.351458|1.848158|0
Puig i Lis|Puig i Lis||Camp de Túria|PV|39.639896|-0.579663|0
Puig Rabat|Puig Rabat||Anoia|CAT|41.607109|1.649207|0
Puig Rosell|Puig Rosell||Baix Empordà|CAT|41.87203|3.067644|0
Puig-reig|Puig-reig||Berguedà|CAT|41.965429|1.868885|1
Puigbalador|Puyvalador||el Capcir|CN|42.655832|2.104083|1
Puigcercós|Puigcercós||Pallars Jussà|CAT|42.131524|0.891443|0
Puigcerdà|Puigcerdà||Cerdanya|CAT|42.415965|1.938539|1
Puigdelfí|Puigdelfí||Tarragonès|CAT|41.194052|1.237524|0
Puigdàlber|Puigdàlber||Alt Penedès|CAT|41.403706|1.703026|1
Puiggròs|Puiggròs||Garrigues|CAT|41.558327|0.878935|1
Puigmoltó|Puigmoltó||Garraf|CAT|41.249635|1.767148|0
Puigmontmany|Puigmontmany||Baix Llobregat|CAT|41.409685|1.935765|0
Puigpalter|Puigpalter||Pla de l'Estany|CAT|42.121261|2.796105|0
Puigpalter de Dalt|Puigpalter de Dalt||Pla de l'Estany|CAT|42.121486|2.789948|0
Puigpelat|Puigpelat||Alt Camp|CAT|41.279285|1.299785|1
Puigpunyent|Puigpunyent||Mallorca|IB|39.626906|2.528966|1
Puigsaguilla|Puigsaguilla||Baix Empordà|CAT|41.987677|3.194266|0
Puigsurís|Puigsurís||Pla de l'Estany|CAT|42.100616|2.732462|0
Puigventós|Puigventós||Selva|CAT|41.751661|2.794699|0
Puigventós|Puigventós||Baix Empordà|CAT|41.965704|3.028534|0
Puigverd d'Agramunt|Puigverd d'Agramunt||Urgell|CAT|41.75248|1.103006|1
Puigverd de Lleida|Puigverd de Lleida||Segrià|CAT|41.542558|0.737216|1
Pujada de Mianegues|Pujada de Mianegues||Pla de l'Estany|CAT|42.107985|2.756419|0
Pujal de Cabó|Pujal de Cabó||Alt Urgell|CAT|42.221931|1.312845|0
Pujalt|Pujalt||Anoia|CAT|41.706771|1.434858|1
Pujalt|Pujalt||Pallars Sobirà|CAT|42.416102|1.110621|0
Pumanyons|Pumanyons||Pallars Jussà|CAT|42.236975|0.962874|0
Punta Grossa, Polígono I|Punta Grossa, Polígono I||Menorca|IB|40.026091|4.189036|0
Punta Grossa, Polígono II|Punta Grossa, Polígono II||Menorca|IB|40.030453|4.191563|0
Puntabrava|Puntabrava||Baix Empordà|CAT|41.77085|3.004178|0
Purroy de la Solana|Purroy de la Solana||la Ribagorça|FR|42.048716|0.460601|0
Purroy Viejo|Purroy Viejo||la Ribagorça|FR|42.057156|0.460602|0
Puçol|Puçol||Horta Nord|PV|39.618006|-0.305817|1
Pòrtol|Pòrtol||Mallorca|IB|39.613976|2.770328|0
Púbol|Púbol||Baix Empordà|CAT|42.014328|2.982494|0
Quart|Quart||Gironès|CAT|41.953271|2.887256|1
Quart de les Valls|Quart de les Valls||Camp de Morvedre|PV|39.744692|-0.288841|1
Quart de Poblet|Quart de Poblet||Horta Sud|PV|39.472292|-0.495638|1
Quartell|Quartell||Camp de Morvedre|PV|39.728561|-0.238532|1
Quatretonda|Quatretonda||Vall d'Albaida|PV|38.969296|-0.385246|1
Quatretondeta|Quatretondeta||Comtat|PV|38.7166|-0.314529|1
Queixans|Queixans||Cerdanya|CAT|42.396437|1.920771|0
Queralbs|Queralbs||Ripollès|CAT|42.378633|2.171202|1
Queretes|Cretas||el Matarranya|FR|40.922997|0.205193|1
Querol|Querol||Alt Camp|CAT|41.42043|1.429628|1
Quesa|Quesa||Canal de Navarrés|PV|39.10443|-0.778439|1
Quilòmetre 3|Quilòmetre 3||Maresme|CAT|41.628909|2.640362|0
Quinta Avinguda|Quinta Avinguda||Vallès Oriental|CAT|41.61603|2.268947|0
Quitapellejos|Quitapellejos|||CX|38.606373|-1.150803|0
Rabós|Rabós||Alt Empordà|CAT|42.399825|3.046507|1
Racholar|Racholar||Plana Baixa|PV|39.965966|-0.248218|0
Racons|Racons||Baix Ebre|CAT|40.857477|0.764123|0
Racó de Bellver|Racó de Bellver||Ribera Baixa|PV|39.178167|-0.242253|0
Racó del Cirer|Racó del Cirer||Vall d'Albaida|PV|38.757376|-0.54101|0
Racó dels Flares|Racó dels Flares||Safor|PV|38.990269|-0.222461|0
Rafal|Rafal||Baix Segura|PV|38.106179|-0.850015|1
Rafalcaid|Rafalcaid||Safor|PV|38.984639|-0.154919|0
Rafelbunyol|Rafelbunyol||Horta Nord|PV|39.595186|-0.347986|1
Rafelcofer|Rafelcofer||Safor|PV|38.93401|-0.169315|1
Rafelguaraf|Rafelguaraf||Ribera Alta|PV|39.049925|-0.438708|1
Raiguero de Bonanza|Raiguero de Bonanza||Baix Segura|PV|38.090615|-0.976225|0
Raiguero Levante|Raiguero Levante||Baix Segura|PV|38.081871|-0.99914|0
Raiguero Poniente|Raiguero Poniente||Baix Segura|PV|38.076006|-1.018844|0
Raimat|Raimat||Segrià|CAT|41.677193|0.477212|0
Rajadell|Rajadell||Bages|CAT|41.720878|1.712842|1
Ral|Réal||el Capcir|CN|42.64151|2.146042|1
Ralleu|Railleu||el Conflent|CN|42.590165|2.158695|1
Rambla del Pepior|Rambla del Pepior||Alacantí|PV|38.401213|-0.577697|0
Rambla del Rollet|Rambla del Rollet||Alacantí|PV|38.396672|-0.586371|0
Ramon Vilalta i Sole|Ramon Vilalta i Sole||Segrià|CAT|41.715913|0.602637|0
Ramones/Sorianos|Ramones/Sorianos||Baix Segura|PV|38.12231|-0.831545|0
Randa|Randa||Mallorca|IB|39.526994|2.914951|0
Ransol|Ransol|||AD|42.5676|1.59756|0
Ras-Pas|Ras-Pas||Alacantí|PV|38.413074|-0.524954|0
Rasigueres|Rasiguères||la Fenolleda|CN|42.774654|2.595397|1
Rasquera|Rasquera||Ribera d'Ebre|CAT|40.975024|0.61494|1
Ratera|Ratera||Segarra|CAT|41.752493|1.223074|0
Ratpenat|Ratpenat||Garraf|CAT|41.264814|1.925718|0
Rauric|Rauric||Conca de Barberà|CAT|41.542537|1.35937|0
Raval|Raval||Ribera Alta|PV|39.195014|-0.444822|0
Raval d'en Xicota|Raval d'en Xicota||Vallès Oriental|CAT|41.612767|2.249858|0
Raval de Cristo|Raval de Cristo||Baix Ebre|CAT|40.80883|0.495355|0
Raval de Merlès|Raval de Merlès||Vallès Oriental|CAT|41.609782|2.25281|0
Raval de Taradell|Raval de Taradell||Osona|CAT|41.901722|2.287609|0
Raval de Torrelletes|Raval de Torrelletes||Baix Llobregat|CAT|41.346311|1.951045|0
Raval del Bonaire|Raval del Bonaire||Vallès Occidental|CAT|41.603882|1.866574|0""",
        """Raval del Jordi|Raval del Jordi||Bages|CAT|41.647914|1.857009|0
Raval del Parrot|Raval del Parrot||Bages|CAT|41.667537|1.77367|0
Raval del Sellerès|Raval del Sellerès||Bages|CAT|41.682891|1.757481|0
Raval Parruca|Raval Parruca||Baix Llobregat|CAT|41.360802|1.982243|0
Raval Picanyol|Raval Picanyol||Moianès|CAT|41.825765|2.16614|0
Raval Roig|Raval Roig||Baix Llobregat|CAT|41.363917|1.98386|0
Raval Sant Martí|Raval Sant Martí||Baix Llobregat|CAT|41.333413|1.923578|0
Ravel|Ravel||Alacantí|PV|38.408783|-0.458479|0
Real|Real||Ribera Alta|PV|39.313512|-0.622514|1
Rebollet|Rabouillet||la Fenolleda|CN|42.727693|2.371247|1
Redovà|Redován||Baix Segura|PV|38.122331|-0.921508|1
Regadiu Nord|Regadiu Nord||Maresme|CAT|41.575023|2.50324|0
Regadiu Sud|Regadiu Sud||Maresme|CAT|41.568444|2.514331|0
Regencós|Regencós||Baix Empordà|CAT|41.949598|3.171824|1
Reiners|Reynès||el Vallespir|CN|42.475183|2.705764|1
Relleu|Relleu||Marina Baixa|PV|38.571334|-0.33954|1
Rellinars|Rellinars||Vallès Occidental|CAT|41.643432|1.918728|1
Renau|Renau||Tarragonès|CAT|41.217853|1.31575|1
Requena|Requena||Plana d'Utiel|PV|39.436756|-1.136126|1
Residence Park|Residence Park||Marina Baixa|PV|38.602804|-0.113122|0
Residencial|Residencial||Baix Penedès|CAT|41.195259|1.562515|0
Residencial Balneario de Aigües|Residencial Balneario de Aigües||Alacantí|PV|38.491867|-0.368672|0
Residencial Borgonya|Residencial Borgonya||Pla de l'Estany|CAT|42.100422|2.800026|0
Residencial Camí d'Alella|Residencial Camí d'Alella||Maresme|CAT|41.475215|2.281551|0
Residencial Can Carner|Residencial Can Carner||Vallès Occidental|CAT|41.602471|2.082776|0
Residencial la Pobla|Residencial la Pobla||Camp de Túria|PV|39.599135|-0.556731|0
Residencial Masos|Residencial Masos||Baix Empordà|CAT|41.982107|3.171755|0
Residencial Monnars|Residencial Monnars||Tarragonès|CAT|41.139301|1.300865|0
Residencial Remei|Residencial Remei||Alt Camp|CAT|41.266731|1.158874|0
Residencial Reva|Residencial Reva||Camp de Túria|PV|39.484123|-0.57211|0
Retiro del Pla|Retiro del Pla||Camp de Túria|PV|39.618025|-0.526777|0
Reus|Reus||Baix Camp|CAT|41.148726|1.120103|1
Rialp|Rialp||Pallars Sobirà|CAT|42.461274|1.146734|1
Rials|Rials||Maresme|CAT|41.490994|2.313016|0
Riba-roja d'Ebre|Riba-roja d'Ebre||Ribera d'Ebre|CAT|41.261252|0.446791|1
Riba-roja de Túria|Riba-roja de Túria||Camp de Túria|PV|39.520308|-0.56041|1
Ribamar|Ribamar||Baix Maestrat|PV|40.26989|0.30749|0
Ribelles|Ribelles||Noguera|CAT|41.883403|1.276912|0
Riber|Riber||Segarra|CAT|41.731817|1.250319|0
Ribera d'Ondara|Ribera d'Ondara||Segarra|CAT|41.631109|1.325684|1
Ribera d'Urgellet|Ribera d'Urgellet||Alt Urgell|CAT|42.291057|1.397842|1
Ribera de Cardós|Ribera de Cardós||Pallars Sobirà|CAT|42.563615|1.22763|0
Ribes de Freser|Ribes de Freser||Ripollès|CAT|42.30004|2.171394|1
Ribesalbes|Ribesalbes||Plana Baixa|PV|40.026963|-0.277834|1
Ribesaltes|Rivesaltes||el Rosselló|CN|42.778934|2.876853|1
Riells del Fai|Riells del Fai||Vallès Oriental|CAT|41.696985|2.196908|0
Riells i Viabrea|Riells i Viabrea||Selva|CAT|41.761303|2.521024|1
Riera de Torrentbo|Riera de Torrentbo||Maresme|CAT|41.580081|2.520379|0
Rigardà|Rigarda||el Conflent|CN|42.628002|2.537481|1
Riguala de Serradúy|Riguala de Serradúy||la Ribagorça|FR|42.316679|0.597397|0
Rincón de los Pablos|Rincón de los Pablos||Baix Vinalopó|PV|38.159012|-0.831718|0
Rincón de Seca|Rincón de Seca||Baix Segura|PV|38.094513|-0.960886|0
Rincón del Silencio|Rincón del Silencio||Marina Alta|PV|38.793112|-0.02151|0
Riner|Riner||Solsonès|CAT|41.922302|1.553607|1
Riola|Riola||Ribera Baixa|PV|39.189724|-0.337424|1
Ripoll|Ripoll||Ripollès|CAT|42.186194|2.213895|1
Ripollet|Ripollet||Vallès Occidental|CAT|41.499603|2.154226|1
Riquelme-Capirulos|Riquelme-Capirulos||Baix Segura|PV|38.170874|-0.969566|0
Riu Clar|Riu Clar||Tarragonès|CAT|41.120163|1.224017|0
Riu de Cerdanya|Riu de Cerdanya||Cerdanya|CAT|42.323916|1.823718|1
Riu de Santa Maria|Riu de Santa Maria||Cerdanya|CAT|42.36699|1.787872|0
Riu-club|Riu-club||Baix Camp|CAT|41.171716|0.974678|0
Riuclar|Riuclar||Selva|CAT|41.787619|2.621261|0
Riudarenes|Riudarenes||Selva|CAT|41.821819|2.671866|1
Riudaura|Riudaura||Garrotxa|CAT|42.190473|2.388668|1
Riudecanyes|Riudecanyes||Baix Camp|CAT|41.135557|0.958002|1
Riudecols|Riudecols||Baix Camp|CAT|41.173553|0.964698|1
Riudellots de la Selva|Riudellots de la Selva||Selva|CAT|41.89335|2.796379|1
Riudoms|Riudoms||Baix Camp|CAT|41.136138|1.058018|1
Riudovelles|Riudovelles||Urgell|CAT|41.714337|1.195206|0
Riufred|Riufred||Baix Empordà|CAT|41.862225|3.066575|0
Riumar|Riumar||Baix Ebre|CAT|40.726933|0.836687|0
Riumors|Riumors||Alt Empordà|CAT|42.223342|3.046145|1
Riuràs|Riuràs||Baix Empordà|CAT|42.026427|2.959479|0
River Parc|River Parc||Bages|CAT|41.715904|1.898965|0
Rivert|Rivert||Pallars Jussà|CAT|42.249569|0.895097|0
Rià i Sirac|Ria-Sirach||el Conflent|CN|42.61288|2.388947|1
Roca de Malvet|Roca de Malvet||Baix Empordà|CAT|41.83752|3.000175|0
Roca-rossa|Roca-rossa||Maresme|CAT|41.698226|2.641652|0
Rocabruna|Rocabruna||Ripollès|CAT|42.338543|2.451085|0
Rocafort|Rocafort||Horta Nord|PV|39.536054|-0.413331|1
Rocafort|Rocafort||Bages|CAT|41.715097|1.934184|0
Rocafort de Queralt|Rocafort de Queralt||Conca de Barberà|CAT|41.480089|1.269656|1
Rocafort de Vallbona|Rocafort de Vallbona||Urgell|CAT|41.554287|1.094248|0
Rocallaura|Rocallaura||Urgell|CAT|41.507087|1.148181|0
Rocallisa|Rocallisa||Alt Penedès|CAT|41.240238|1.591934|0
Rocamar|Rocamar||Garraf|CAT|41.234865|1.782209|0
Roda de Berà|Roda de Berà||Tarragonès|CAT|41.186218|1.461369|1
Roda de Isábena|Roda de Isábena||la Ribagorça|FR|42.290916|0.528798|0
Roda de Ter|Roda de Ter||Osona|CAT|41.980533|2.309142|1
Rodonyà|Rodonyà||Alt Camp|CAT|41.278654|1.407267|1
Rodés|Rodès||el Conflent|CN|42.656045|2.55057|1
Rodés|Rodés||Pallars Sobirà|CAT|42.454923|1.148916|0
Rofes|Rofes||Anoia|CAT|41.494093|1.569491|0
Rojals|Rojales||Baix Segura|PV|38.073993|-0.718393|1
Rojals|Rojals||Conca de Barberà|CAT|41.33843|1.111975|0
Roma|Roma||Plana d'Utiel|PV|39.506638|-1.157688|0
Romanyà de la Selva|Romanyà de la Selva||Baix Empordà|CAT|41.853501|2.984642|0
Romeral|Romeral||Marina Baixa|PV|38.572088|-0.109693|0
Roní|Roní||Pallars Sobirà|CAT|42.457384|1.179786|0
Roquetes|Roquetes||Baix Ebre|CAT|40.787996|0.401653|1
Rosamar|Rosamar||Baix Empordà|CAT|41.763561|2.976594|0
Roses|Roses||Alt Empordà|CAT|42.271257|3.199727|1
Rossell|Rossell||Baix Maestrat|PV|40.622267|0.224625|1
Rossell|Rossell||Selva|CAT|41.709686|2.840445|0
Rosselló|Rosselló||Segrià|CAT|41.695149|0.59202|1
Rotes|Rotes||Marina Alta|PV|38.8298|0.141274|0
Rotglà i Corberà|Rotglà i Corberà||Costera|PV|39.011561|-0.563594|1
Royal Park|Royal Park||Vallès Oriental|CAT|41.726157|2.531768|0
Ruberts|Ruberts||Mallorca|IB|39.622741|2.936914|0
Rubinat|Rubinat||Segarra|CAT|41.624297|1.319352|0
Rubió|Rubió||Anoia|CAT|41.657124|1.57477|1
Rubió de Dalt|Rubió de Dalt||Noguera|CAT|41.88862|0.992237|0
Rubió del Mig|Rubió del Mig||Noguera|CAT|41.889975|0.988641|0
Rubí|Rubí||Vallès Occidental|CAT|41.501589|2.014672|1
Rugat|Rugat||Vall d'Albaida|PV|38.881444|-0.361572|1
Rupit|Rupit||Osona|CAT|42.024659|2.466126|0
Rupit i Pruit|Rupit i Pruit||Osona|CAT|42.029121|2.463276|1
Rupià|Rupià||Baix Empordà|CAT|42.016882|3.009726|1
Rustical Balneari|Rustical Balneari||Baix Camp|CAT|41.053165|0.994384|0
Rustical Mont-roig|Rustical Mont-roig||Baix Camp|CAT|41.03664|0.966336|0
Ràfels|Ráfales||el Matarranya|FR|40.848572|0.014362|1
Río Park|Río Park||Selva|CAT|41.774869|2.566948|0
Río Park|Río Park||Alacantí|PV|38.449818|-0.483721|0
Ríos de Abajo|Ríos de Abajo||Alt Palància|PV|39.903968|-0.697536|0
Ròtova|Ròtova||Safor|PV|38.939432|-0.2686|1
s'Agaró|s'Agaró||Baix Empordà|CAT|41.79776|3.056203|0
s'Algar|s'Algar||Menorca|IB|39.835896|4.300524|0
s'Alqueria Blanca|s'Alqueria Blanca||Mallorca|IB|39.38878|3.164411|0
s'Aranjassa|s'Aranjassa||Mallorca|IB|39.541723|2.775223|0
s'Arenal|s'Arenal||Mallorca|IB|39.498161|2.752944|0
s'Arenal|s'Arenal||Mallorca|IB|39.507888|2.757094|0
s'Arenella|s'Arenella||Alt Empordà|CAT|42.352618|3.176561|0
s'Argamassa|s'Argamassa||Eivissa|IB|38.995086|1.566747|0
s'Arracó|s'Arracó||Mallorca|IB|39.579316|2.395358|0
s'Arxiduc|s'Arxiduc||Mallorca|IB|39.713297|2.629019|0
s'Empeltada|s'Empeltada||Mallorca|IB|39.75596|2.647928|0
s'Esgleieta|s'Esgleieta||Mallorca|IB|39.656341|2.644583|0
s'Estany d'en Mas|s'Estany d'en Mas||Mallorca|IB|39.519322|3.303957|0
s'Estanyol|s'Estanyol||Mallorca|IB|39.732857|3.260303|0
s'Estanyol de Migjorn|s'Estanyol de Migjorn||Mallorca|IB|39.364899|2.92469|0
s'Horta|s'Horta||Mallorca|IB|39.407754|3.209081|0
s'Hostalot|s'Hostalot||Mallorca|IB|39.594083|2.730061|0
s'Illot|s'Illot||Mallorca|IB|39.569786|3.371496|0
s'Illot-Cala Morlanda|s'Illot-Cala Morlanda||Mallorca|IB|39.561486|3.371205|0
s'Ullastrar|s'Ullastrar||Menorca|IB|39.842949|4.248611|0
sa Cabaneta|sa Cabaneta||Mallorca|IB|39.616413|2.74983|0
sa Cala|sa Cala||Mallorca|IB|39.759552|2.64049|0
sa Carroca|sa Carroca||Eivissa|IB|38.90473|1.396892|0
sa Casa Blanca|sa Casa Blanca||Mallorca|IB|39.570149|2.757455|0
sa Coma|sa Coma||Mallorca|IB|39.669632|2.688933|0
sa Coma|sa Coma||Mallorca|IB|39.579363|3.369865|0
sa Costa de Canyamel|sa Costa de Canyamel||Mallorca|IB|39.643087|3.431806|0
sa Font|sa Font||Eivissa|IB|38.978281|1.515421|0
sa Font Seca|sa Font Seca||Mallorca|IB|39.669245|2.667619|0
sa Garriga|sa Garriga||Mallorca|IB|39.631135|2.664139|0
sa Indioteria|sa Indioteria||Mallorca|IB|39.607667|2.676624|0
Sa Marineta|Sa Marineta||Mallorca|IB|39.552364|3.316466|0
sa Mesquida|sa Mesquida||Menorca|IB|39.911086|4.287643|0
sa Pedruscada|sa Pedruscada||Mallorca|IB|39.697162|3.452588|0
sa Pobla|sa Pobla||Mallorca|IB|39.780345|3.028853|1
sa Porrassa|sa Porrassa||Mallorca|IB|39.500767|2.517345|0
sa Punta Prima|sa Punta Prima||Menorca|IB|39.81537|4.279396|0
Sa Riera|Sa Riera||Baix Empordà|CAT|41.970689|3.210219|0
sa Roca|sa Roca||Menorca|IB|39.987003|4.132994|0
sa Ràpita|sa Ràpita||Mallorca|IB|39.365773|2.941122|0
Sa Segada|Sa Segada|||AL|40.55969|8.31953|0
sa Sorda|sa Sorda||Mallorca|IB|39.399316|2.969931|0
sa Sínia d'en Riera|sa Sínia d'en Riera||Menorca|IB|39.868354|4.283447|0
sa Torre|sa Torre||Mallorca|IB|39.439149|2.800455|0
Sa Trenca|Sa Trenca||Eivissa|IB|38.992819|1.570366|0
Sa Tuna|Sa Tuna||Baix Empordà|CAT|41.960705|3.228791|0
sa Vinyola|sa Vinyola||Mallorca|IB|39.393841|2.948573|0
Sabadell|Sabadell||Vallès Occidental|CAT|41.555201|2.101643|1
Sabanell|Sabanell||Alt Penedès|CAT|41.415753|1.695153|0
Sacanyet|Sacañet||Alt Palància|PV|39.852513|-0.734539|1
Safareig|Safareig||Camp de Túria|PV|39.636462|-0.568961|0
Safareig-Canyada de Confites-El Secanet|Safareig-Canyada de Confites-El Secanet||Camp de Túria|PV|39.640357|-0.558966|0
Safari|Safari||Alt Penedès|CAT|41.421718|1.885479|0
Saga|Saga||Cerdanya|CAT|42.415498|1.860289|0
Sagarras Bajas|Sagarras Bajas||la Ribagorça|FR|42.129133|0.535648|0
Sagra|Sagra||Marina Alta|PV|38.818131|-0.066836|1
Sagunt|Sagunt/Sagunto||Camp de Morvedre|PV|39.690118|-0.278247|1
Sagàs|Sagàs||Berguedà|CAT|42.029018|1.94397|1
Saidí|Zaidín||el Baix Cinca|FR|41.625687|0.285597|1
Saifores|Saifores||Baix Penedès|CAT|41.263595|1.550286|0
Saix|Sax||Alt Vinalopó|PV|38.548456|-0.813331|1
Salafranca|Salafranca||Alacantí|PV|38.403769|-0.444085|0
Salardú|Salardú||la Vall d'Aran|CAT|42.707212|0.902417|0
Saldes|Saldes||Berguedà|CAT|42.229035|1.735012|1
Saldet|Saldet||Alt Empordà|CAT|42.170602|3.062527|0
Salelles|Saleilles||el Rosselló|CN|42.657182|2.948548|1
Salelles|Salelles||Bages|CAT|41.700998|1.793386|0
Salem|Salem||Vall d'Albaida|PV|38.84912|-0.382261|1
Sales de Llierca|Sales de Llierca||Garrotxa|CAT|42.265131|2.635023|1
Salines|Salinas||Alt Vinalopó|PV|38.507813|-0.910474|1
Salinetes|Salinetes||Vinalopó Mitjà|PV|38.466421|-0.772806|0
Salionç|Salionç||Selva|CAT|41.74739|2.964474|0
Salitja|Salitja||Selva|CAT|41.907225|2.747029|0
Sallagosa|Saillagouse||l'Alta Cerdanya|CN|42.458959|2.039381|1
Sallent|Sallent||Bages|CAT|41.832057|1.905884|1
Salo|Salo||Bages|CAT|41.844552|1.642224|0
Salom|Salom||Baix Empordà|CAT|41.81654|3.007659|0
Salomó|Salomó||Tarragonès|CAT|41.234185|1.373486|1
Salou|Salou||Tarragonès|CAT|41.079515|1.144505|1
Salses|Salses-le-Château||el Rosselló|CN|42.837214|2.91587|1
Salt|Salt||Gironès|CAT|41.973652|2.784743|1
Salto de Cofrentes|Salto de Cofrentes||Vall de Cofrents|PV|39.243434|-1.095992|0
Saltó|Sauto||el Conflent|CN|42.515714|2.14895|1
Salàs de Pallars|Salàs de Pallars||Pallars Jussà|CAT|42.22195|0.921648|1
San Antonio|San Antonio||Plana d'Utiel|PV|39.520026|-1.153769|0
San Antón|San Antón||Baix Segura|PV|38.095458|-0.943186|0
San Bartolomé|San Bartolomé||Baix Segura|PV|38.093923|-0.860004|0
San Benito|San Benito||Vall de Cofrents|PV|38.943802|-1.108317|0
San Bernabé|San Bernabé||Alt Vinalopó|PV|38.65921|-0.843828|0
San Bernardo|San Bernardo||Marina Alta|PV|38.653154|0.03619|0
San Carlos|San Carlos||Baix Segura|PV|38.132072|-0.929854|0
San Dionisio|San Dionisio||Marina Alta|PV|38.674675|0.056527|0
San Francisco|San Francisco||Horta Sud|PV|39.500568|-0.477661|0
San Gabriel|San Gabriel||Marina Alta|PV|38.644555|0.058566|0
San Isidro de Benagéber|San Isidro de Benagéber||Horta Nord|PV|39.567512|-0.398058|0
San Jerónimo|San Jerónimo||Horta Sud|PV|39.484852|-0.459297|0
San José|San José||Baix Segura|PV|38.128655|-0.859693|0
San Juan|San Juan||Plana d'Utiel|PV|39.523007|-1.172961|0
San Martín|San Martín||Camp de Túria|PV|39.581575|-0.522483|0
San Pedro|San Pedro||Baix Segura|PV|38.143808|-0.832784|0
San Pedro|San Pedro||Baix Segura|PV|38.14466|-0.832151|0
San Pío V|San Pío V||Camp de Morvedre|PV|39.721919|-0.354514|0
San Roque|San Roque||Baix Segura|PV|38.1371|-0.835616|0
San Sebastián|San Sebastián||Camp de Túria|PV|39.563838|-0.558144|0
San Vicente|San Vicente||Alt Millars|PV|40.14032|-0.500183|0
Sanata|Sanata||Vallès Oriental|CAT|41.660291|2.450652|0
Sanavastre|Sanavastre||Cerdanya|CAT|42.385106|1.85059|0
Sanaüja|Sanaüja||Segarra|CAT|41.865247|1.308445|1
Sanaüja|Sanaüja||Vallès Oriental|CAT|41.631532|2.27669|0
Saneja|Saneja||Cerdanya|CAT|42.444471|1.902254|0
Sanet i els Negrals|Sanet y Negrals||Marina Alta|PV|38.815688|-0.028858|1
Sant Adrià|Sant Adrià||Pallars Jussà|CAT|42.187158|0.833586|0
Sant Adrià de Besòs|Sant Adrià de Besòs||Barcelonès|CAT|41.424134|2.221687|1
Sant Agustí|Sant Agustí||Mallorca|IB|39.551747|2.598729|0
Sant Agustí de Lluçanès|Sant Agustí de Lluçanès||Osona|CAT|42.081546|2.131443|1
Sant Agustí des Vedrà|Sant Agustí des Vedrà||Eivissa|IB|38.942053|1.292908|0
Sant Amanç|Sant Amanç||Baix Empordà|CAT|41.785986|3.014762|0
Sant Andreu|Sant Andreu||Alt Urgell|CAT|42.395403|1.306193|0
Sant Andreu de la Barca|Sant Andreu de la Barca||Baix Llobregat|CAT|41.45135|1.963698|1
Sant Andreu de Llavaneres|Sant Andreu de Llavaneres||Maresme|CAT|41.577562|2.48398|1
Sant Andreu de Sureda|Saint-André||el Rosselló|CN|42.552541|2.980345|1
Sant Andreu Salou|Sant Andreu Salou||Gironès|CAT|41.869583|2.823235|1
Sant Aniol de Finestres|Sant Aniol de Finestres||Garrotxa|CAT|42.074568|2.602197|1
Sant Antolí i Vilanova|Sant Antolí i Vilanova||Segarra|CAT|41.627999|1.341903|0""",
        """Sant Antoni|Sant Antoni||Alt Empordà|CAT|42.316943|2.780296|0
Sant Antoni|Sant Antoni||Alt Urgell|CAT|42.361523|1.474485|0
Sant Antoni|Sant Antoni||Menorca|IB|39.876838|4.30361|0
Sant Antoni de Benaixeve|San Antonio de Benagéber||Camp de Túria|PV|39.562215|-0.495986|1
Sant Antoni de Calonge|Sant Antoni de Calonge||Baix Empordà|CAT|41.848218|3.106304|0
Sant Antoni de Portmany|Sant Antoni de Portmany||Eivissa|IB|38.996362|1.35859|1
Sant Antoni de Vilamajor|Sant Antoni de Vilamajor||Vallès Oriental|CAT|41.660868|2.395091|1
Sant Bartomeu de la Valldan|Sant Bartomeu de la Valldan||Berguedà|CAT|42.092795|1.822728|0
Sant Bartomeu del Grau|Sant Bartomeu del Grau||Osona|CAT|41.969314|2.154487|1
Sant Benet de Bages|Sant Benet de Bages||Bages|CAT|41.744003|1.900508|0
Sant Berger|Sant Berger||Maresme|CAT|41.497574|2.331551|0
Sant Bernat|Sant Bernat||Ribera Alta|PV|39.146917|-0.385689|0
Sant Boi de Llobregat|Sant Boi de Llobregat||Baix Llobregat|CAT|41.334982|2.04191|1
Sant Boi de Lluçanès|Sant Boi de Lluçanès||Osona|CAT|42.053252|2.158852|1
Sant Carles|Sant Carles||Vallès Oriental|CAT|41.599619|2.377951|0
Sant Carles de Peralta|Sant Carles de Peralta||Eivissa|IB|39.034332|1.566429|0
Sant Cebrià de Rosselló|Saint-Cyprien||el Rosselló|CN|42.621665|3.015896|1
Sant Cebrià de Vallalta|Sant Cebrià de Vallalta||Maresme|CAT|41.629548|2.604078|1
Sant Celoni|Sant Celoni||Vallès Oriental|CAT|41.689013|2.540282|1
Sant Climent|Sant Climent||Menorca|IB|39.871338|4.208178|0
Sant Climent de Llobregat|Sant Climent de Llobregat||Baix Llobregat|CAT|41.333272|1.985587|1
Sant Climent Sescebes|Sant Climent Sescebes||Alt Empordà|CAT|42.388411|2.966667|1
Sant Climenç|Sant Climenç||Solsonès|CAT|41.949911|1.41823|0
Sant Corneli|Sant Corneli||Berguedà|CAT|42.184956|1.853495|0
Sant Cristòfol|Sant Cristòfol||Bages|CAT|41.641142|1.816299|0
Sant Cristòfol|Sant Cristòfol||Baix Llobregat|CAT|41.416243|1.903336|0
Sant Cristòfol|Sant Cristòfol||Vallès Oriental|CAT|41.635807|2.251307|0
Sant Cristòfol les Fonts|Sant Cristòfol les Fonts||Garrotxa|CAT|42.173706|2.497681|0
Sant Cugat del Vallès|Sant Cugat del Vallès||Vallès Occidental|CAT|41.461958|2.073057|1
Sant Cugat Sesgarrigues|Sant Cugat Sesgarrigues||Alt Penedès|CAT|41.35595|1.75346|1
Sant Dalmai|Sant Dalmai||Selva|CAT|41.911103|2.732639|0
Sant Daniel|Sant Daniel||Maresme|CAT|41.714591|2.762197|0
Sant Daniel|Sant Daniel||Baix Empordà|CAT|41.857687|3.105324|0
Sant Domí|Sant Domí||Segarra|CAT|41.665753|1.413744|0
Sant Elies|Sant Elies||Alt Penedès|CAT|41.495237|1.670838|0
Sant Elm|Sant Elm||Mallorca|IB|39.579449|2.352453|0
Sant Eloi|Sant Eloi||Selva|CAT|41.727389|2.90685|0
Sant Esteve|Sant Esteve||Bages|CAT|41.883522|1.885176|0
Sant Esteve d'en Bas|Sant Esteve d'en Bas||Garrotxa|CAT|42.117633|2.459449|0
Sant Esteve de Guialbes|Sant Esteve de Guialbes||Pla de l'Estany|CAT|42.114994|2.877555|0
Sant Esteve de la Sarga|Sant Esteve de la Sarga||Pallars Jussà|CAT|42.077484|0.767623|1
Sant Esteve de Llitera|San Esteban de Litera||la Llitera|FR|41.897385|0.330684|1
Sant Esteve de Llémena|Sant Esteve de Llémena||Garrotxa|CAT|42.063309|2.61459|0
Sant Esteve de Palautordera|Sant Esteve de Palautordera||Vallès Oriental|CAT|41.714085|2.421441|1
Sant Esteve del Monestir|Saint-Estève||el Rosselló|CN|42.715137|2.846241|1
Sant Esteve Sesrovires|Sant Esteve Sesrovires||Baix Llobregat|CAT|41.486546|1.876328|1
Sant Felip Neri|Sant Felip Neri||Baix Vinalopó|PV|38.177038|-0.798511|0
Sant Feliu|Sant Feliu||Vallès Oriental|CAT|41.624015|2.277655|0
Sant Feliu|Sant Feliu||Anoia|CAT|41.606654|1.683381|0
Sant Feliu d'Amunt|Saint-Féliu-d'Amont||el Rosselló|CN|42.675777|2.717999|1
Sant Feliu d'Avall|Saint-Féliu-d'Avall||el Rosselló|CN|42.672575|2.74814|1
Sant Feliu de Boada|Sant Feliu de Boada||Baix Empordà|CAT|41.972906|3.125541|0
Sant Feliu de Buixalleu|Sant Feliu de Buixalleu||Selva|CAT|41.781271|2.586906|1
Sant Feliu de Codines|Sant Feliu de Codines||Vallès Oriental|CAT|41.698675|2.161137|1
Sant Feliu de Guíxols|Sant Feliu de Guíxols||Baix Empordà|CAT|41.783142|3.011053|1
Sant Feliu de Llobregat|Sant Feliu de Llobregat||Baix Llobregat|CAT|41.395071|2.049417|1
Sant Feliu de Pallerols|Sant Feliu de Pallerols||Garrotxa|CAT|42.098928|2.507226|1
Sant Feliu Racó|Sant Feliu Racó||Vallès Occidental|CAT|41.619599|2.071224|0
Sant Feliu Sasserra|Sant Feliu Sasserra||Bages|CAT|41.938485|2.007241|1
Sant Ferran de ses Roques|Sant Ferran de ses Roques||Formentera|IB|38.707481|1.458258|0
Sant Ferriol|Sant Ferriol||Garrotxa|CAT|42.185976|2.66486|1
Sant Fost de Campsentelles|Sant Fost de Campsentelles||Vallès Oriental|CAT|41.507696|2.233524|1
Sant Francesc|Sant Francesc||Horta Sud|PV|39.359272|-0.483762|0
Sant Francesc de Formentera|Sant Francesc de Formentera||Formentera|IB|38.7062|1.425825|0
Sant Fruitós de Bages|Sant Fruitós de Bages||Bages|CAT|41.755877|1.871993|1
Sant Fulgenci|San Fulgencio||Baix Segura|PV|38.125097|-0.692723|1
Sant Gallard|Sant Gallard||Conca de Barberà|CAT|41.512903|1.391156|0
Sant Genís|Sant Genís||Anoia|CAT|41.601278|1.566809|0
Sant Genís de Fontanes|Saint-Génis-des-Fontaines||el Rosselló|CN|42.549661|2.919923|1
Sant Genís de Palafolls|Sant Genís de Palafolls||Maresme|CAT|41.660767|2.717706|0
Sant Gerard|Sant Gerard||Camp de Túria|PV|39.658108|-0.58837|0
Sant Grau|Sant Grau||Selva|CAT|41.759667|2.948188|0
Sant Gregori|Sant Gregori||Gironès|CAT|42.001085|2.74744|1
Sant Guim de Freixenet|Sant Guim de Freixenet||Segarra|CAT|41.655149|1.405866|1
Sant Guim de la Plana|Sant Guim de la Plana||Segarra|CAT|41.763815|1.344604|1
Sant Guim de la Rabassa|Sant Guim de la Rabassa||Segarra|CAT|41.648599|1.406185|0
Sant Hilari Sacalm|Sant Hilari Sacalm||Selva|CAT|41.902023|2.501242|1
Sant Hipòlit de la Salanca|Saint-Hippolyte||el Rosselló|CN|42.806938|2.973471|1
Sant Hipòlit de Voltregà|Sant Hipòlit de Voltregà||Osona|CAT|42.015656|2.237841|1
Sant Iscle d'Empordà|Sant Iscle d'Empordà||Baix Empordà|CAT|42.031681|3.057465|0
Sant Iscle de Vallalta|Sant Iscle de Vallalta||Maresme|CAT|41.639297|2.5678|1
Sant Iscle i Santa Victòria|Sant Iscle i Santa Victòria||Bages|CAT|41.758544|1.842103|0
Sant Isidre|San Isidro||Baix Segura|PV|38.174793|-0.842192|1
Sant Jaume|Sant Jaume||Menorca|IB|39.904288|4.073429|0
Sant Jaume d'Enveja|Sant Jaume d'Enveja||Montsià|CAT|40.691136|0.780306|1
Sant Jaume de Frontanyà|Sant Jaume de Frontanyà||Berguedà|CAT|42.190668|2.029802|1
Sant Jaume de Llierca|Sant Jaume de Llierca||Garrotxa|CAT|42.209812|2.593938|1
Sant Jaume dels Domenys|Sant Jaume dels Domenys||Baix Penedès|CAT|41.307843|1.555359|1
Sant Jaume Sesoliveres|Sant Jaume Sesoliveres||Anoia|CAT|41.470982|1.75029|0
Sant Joan|Sant Joan||Mallorca|IB|39.587994|3.045216|1
Sant Joan|Sant Joan||Alt Penedès|CAT|41.428517|1.819314|0
Sant Joan|Sant Joan||Baix Camp|CAT|41.167789|1.115857|0
Sant Joan d'Alacant|Sant Joan d'Alacant||Alacantí|PV|38.40459|-0.428478|1
Sant Joan de la Cella|Saint-Jean-Lasseille||el Rosselló|CN|42.585848|2.864651|1
Sant Joan de Labritja|Sant Joan de Labritja||Eivissa|IB|39.062134|1.487714|1
Sant Joan de les Abadesses|Sant Joan de les Abadesses||Ripollès|CAT|42.233916|2.302028|1
Sant Joan de Mediona|Sant Joan de Mediona||Alt Penedès|CAT|41.477168|1.61331|0
Sant Joan de Mollet|Sant Joan de Mollet||Gironès|CAT|42.046238|2.938187|1
Sant Joan de Moró|Sant Joan de Moró||Plana Alta|PV|40.068798|-0.126974|1
Sant Joan de Palamós|Sant Joan de Palamós||Baix Empordà|CAT|41.857486|3.130266|0
Sant Joan de Toran|Sant Joan de Toran||la Vall d'Aran|CAT|42.823943|0.796486|0
Sant Joan de Vilatorrada|Sant Joan de Vilatorrada||Bages|CAT|41.763229|1.797322|1
Sant Joan de Vinyafrescal|Sant Joan de Vinyafrescal||Pallars Jussà|CAT|42.228394|0.959781|0
Sant Joan del Pas|Sant Joan del Pas||Montsià|CAT|40.605175|0.392678|0
Sant Joan Despí|Sant Joan Despí||Baix Llobregat|CAT|41.367334|2.056559|1
Sant Joan Fumat|Sant Joan Fumat||Alt Urgell|CAT|42.43006|1.421424|0
Sant Joan les Fonts|Sant Joan les Fonts||Garrotxa|CAT|42.202547|2.535401|1
Sant Joan Pladecorts|Saint-Jean-Pla-de-Corts||el Vallespir|CN|42.515467|2.794652|1
Sant Joan Samora|Sant Joan Samora||Alt Penedès|CAT|41.477084|1.854445|0
Sant Joanet|Sant Joanet||Ribera Alta|PV|39.071852|-0.489012|1
Sant Jordi|Sant Jordi/San Jorge||Baix Maestrat|PV|40.537379|0.359618|1
Sant Jordi|Sant Jordi||Anoia|CAT|41.515517|1.762992|0
Sant Jordi|Sant Jordi||Baix Penedès|CAT|41.226906|1.531706|0
Sant Jordi|Sant Jordi||Mallorca|IB|39.556271|2.778277|0
Sant Jordi d'Alfama|Sant Jordi d'Alfama||Baix Ebre|CAT|40.915106|0.831732|0
Sant Jordi de Cercs|Sant Jordi de Cercs||Berguedà|CAT|42.150049|1.85852|0
Sant Jordi de Muller|Sant Jordi de Muller||Noguera|CAT|41.797567|0.834978|0
Sant Jordi de ses Salines|Sant Jordi de ses Salines||Eivissa|IB|38.882246|1.373759|0
Sant Jordi Desvalls|Sant Jordi Desvalls||Gironès|CAT|42.073832|2.953849|1
Sant Josep|Sant Josep||Vallès Oriental|CAT|41.618738|2.368653|0
Sant Josep|Sant Josep||Berguedà|CAT|42.173484|1.858507|0
Sant Josep|Sant Josep||Baix Empordà|CAT|41.924318|3.172078|0
Sant Josep|Sant Josep||Horta Sud|PV|39.398376|-0.469617|0
Sant Josep de Fontdepou|Sant Josep de Fontdepou||Noguera|CAT|41.966133|0.755508|0
Sant Josep de sa Talaia|Sant Josep de sa Talaia||Eivissa|IB|38.908219|1.308312|1
Sant Julià d'Alfou|Sant Julià d'Alfou||Vallès Oriental|CAT|41.650463|2.379384|0
Sant Julià de Cerdanyola|Sant Julià de Cerdanyola||Berguedà|CAT|42.218583|1.909043|1
Sant Julià de Lòria|Sant Julià de Lòria|||AD|42.46372|1.49129|0
Sant Julià de Ramis|Sant Julià de Ramis||Gironès|CAT|42.045519|2.849039|1
Sant Julià de Vilatorta|Sant Julià de Vilatorta||Osona|CAT|41.907198|2.32562|1
Sant Julià del Llor|Sant Julià del Llor||Selva|CAT|41.966527|2.643328|0
Sant Julià del Llor i Bonmatí|Sant Julià del Llor i Bonmatí||Selva|CAT|41.975229|2.650333|1
Sant Just Desvern|Sant Just Desvern||Baix Llobregat|CAT|41.392561|2.079184|1
Sant Llop|Sant Llop||Selva|CAT|41.743965|2.545722|0
Sant Llorenç d'Hortons|Sant Llorenç d'Hortons||Alt Penedès|CAT|41.47035|1.834774|1
Sant Llorenç de Balàfia|Sant Llorenç de Balàfia||Eivissa|IB|39.029499|1.476275|0
Sant Llorenç de Cerdans|Saint-Laurent-de-Cerdans||el Vallespir|CN|42.396972|2.621612|1
Sant Llorenç de la Muga|Sant Llorenç de la Muga||Alt Empordà|CAT|42.325905|2.769871|1
Sant Llorenç de la Salanca|Saint-Laurent-de-la-Salanque||el Rosselló|CN|42.794219|3.001852|1
Sant Llorenç de les Arenes|Sant Llorenç de les Arenes||Baix Empordà|CAT|42.061177|2.978157|0
Sant Llorenç de Montgai|Sant Llorenç de Montgai||Noguera|CAT|41.867398|0.840856|0
Sant Llorenç de Morunys|Sant Llorenç de Morunys||Solsonès|CAT|42.13239|1.587455|1
Sant Llorenç dels Pins|Sant Llorenç dels Pins||Vallès Occidental|CAT|41.60908|2.03031|0
Sant Llorenç des Cardassar|Sant Llorenç des Cardassar||Mallorca|IB|39.617265|3.30534|1
Sant Llorenç Savall|Sant Llorenç Savall||Vallès Occidental|CAT|41.682941|2.053997|1
Sant Lluís|Sant Lluís||Menorca|IB|39.836536|4.254033|1
Sant Lluís|Sant Lluís||Horta Sud|PV|39.345837|-0.442878|0
Sant Magí de Brufaganya|Sant Magí de Brufaganya||Conca de Barberà|CAT|41.484272|1.448799|0
Sant Marc|Sant Marc||Tarragonès|CAT|41.124444|1.268573|0
Sant Martí|Sant Martí||Garrotxa|CAT|42.154449|2.578874|0
Sant Martí d'Albars|Sant Martí d'Albars||Lluçanès|CAT|42.034384|2.070129|1
Sant Martí d'Albera|Saint-Martin-de-Fenouillet||la Fenolleda|CN|42.784899|2.478479|1
Sant Martí d'Aravó|Sant Martí d'Aravó||Cerdanya|CAT|42.431373|1.908528|0
Sant Martí d'Empúries|Sant Martí d'Empúries||Alt Empordà|CAT|42.139859|3.116164|0
Sant Martí de Barcedana|Sant Martí de Barcedana||Pallars Jussà|CAT|42.067856|0.958312|0
Sant Martí de Canals|Sant Martí de Canals||Pallars Jussà|CAT|42.225389|0.990415|0
Sant Martí de Centelles|Sant Martí de Centelles||Osona|CAT|41.75079|2.218076|1
Sant Martí de Cerdanya|Sant Martí de Cerdanya||Cerdanya|CAT|42.424806|1.902527|0
Sant Martí de la Morana|Sant Martí de la Morana||Segarra|CAT|41.774533|1.25627|0
Sant Martí de Llémena|Sant Martí de Llémena||Gironès|CAT|42.037608|2.671166|1
Sant Martí de Maldà|Sant Martí de Maldà||Urgell|CAT|41.559825|1.054179|0
Sant Martí de Riucorb|Sant Martí de Riucorb||Urgell|CAT|41.559469|1.064813|1
Sant Martí de Torroella|Sant Martí de Torroella||Bages|CAT|41.773728|1.791795|0
Sant Martí de Tous|Sant Martí de Tous||Anoia|CAT|41.552337|1.501227|1
Sant Martí Sacalm|Sant Martí Sacalm||Selva|CAT|42.014399|2.547187|0
Sant Martí Sapresa|Sant Martí Sapresa||Selva|CAT|41.919877|2.654177|0
Sant Martí Sarroca|Sant Martí Sarroca||Alt Penedès|CAT|41.37448|1.619886|1
Sant Martí Sescorts|Sant Martí Sescorts||Osona|CAT|42.013261|2.325673|0
Sant Martí Sesgueioles|Sant Martí Sesgueioles||Anoia|CAT|41.703424|1.503195|1
Sant Martí Vell|Sant Martí Vell||Gironès|CAT|42.005505|2.928059|1
Sant Marçal|Saint-Marsal||el Rosselló|CN|42.527826|2.614198|1
Sant Marçal|Sant Marçal||Alt Penedès|CAT|41.308693|1.604237|0
Sant Marçal-Sa Vinya de Son Veri|Sant Marçal-Sa Vinya de Son Veri||Mallorca|IB|39.618782|2.738083|0
Sant Mateu|Sant Mateu||Baix Maestrat|PV|40.465943|0.173936|1
Sant Mateu d'Albarca|Sant Mateu d'Albarca||Eivissa|IB|39.039513|1.382579|0
Sant Mateu de Bages|Sant Mateu de Bages||Bages|CAT|41.815889|1.669888|1
Sant Medir|Sant Medir||Gironès|CAT|42.02716|2.77306|0
Sant Miquel|Sant Miquel||Baix Llobregat|CAT|41.53076|1.920531|0
Sant Miquel|Sant Miquel||Noguera|CAT|41.810359|0.861686|0
Sant Miquel|Sant Miquel||Tarragonès|CAT|41.208554|1.36765|0
Sant Miquel d'Aro|Sant Miquel d'Aro||Baix Empordà|CAT|41.86094|2.972598|0
Sant Miquel d'Olèrdola|Sant Miquel d'Olèrdola||Alt Penedès|CAT|41.319949|1.721495|0
Sant Miquel de Balansat|Sant Miquel de Balansat||Eivissa|IB|39.055042|1.437815|0
Sant Miquel de Balenyà|Sant Miquel de Balenyà||Osona|CAT|41.84084|2.247124|0
Sant Miquel de Campmajor|Sant Miquel de Campmajor||Pla de l'Estany|CAT|42.128446|2.677657|1
Sant Miquel de Fluvià|Sant Miquel de Fluvià||Alt Empordà|CAT|42.176776|2.99507|1
Sant Miquel de Gonteres|Sant Miquel de Gonteres||Vallès Occidental|CAT|41.56731|1.974084|0
Sant Miquel de la Vall|Sant Miquel de la Vall||Pallars Jussà|CAT|42.06985|0.961209|0
Sant Miquel de les Salines|San Miguel de Salinas||Baix Segura|PV|37.97485|-0.803112|1
Sant Miquel de Llotes|Saint-Michel-de-Llotes||el Rosselló|CN|42.644362|2.624705|1
Sant Miquel de Mont-roig|Sant Miquel de Mont-roig||Baix Camp|CAT|41.032828|0.964669|0
Sant Mori|Sant Mori||Alt Empordà|CAT|42.151059|2.988471|1
Sant Moritz|Sant Moritz||Vallès Oriental|CAT|41.594932|2.235496|0
Sant Nazari|Saint-Nazaire||el Rosselló|CN|42.662468|2.986448|1
Sant Pau|Sant Pau||Bages|CAT|41.714655|1.833517|0
Sant Pau d'Ordal|Sant Pau d'Ordal||Alt Penedès|CAT|41.382701|1.795807|0
Sant Pau de Fenollet|Saint-Paul-de-Fenouillet||la Fenolleda|CN|42.820703|2.507417|1
Sant Pau de la Guàrdia|Sant Pau de la Guàrdia||Anoia|CAT|41.611832|1.747384|0
Sant Pau de Segúries|Sant Pau de Segúries||Ripollès|CAT|42.265295|2.361631|1
Sant Pere|Sant Pere||Anoia|CAT|41.582256|1.638089|0
Sant Pere|Sant Pere||Maresme|CAT|41.706701|2.727823|0
Sant Pere|Sant Pere||Alt Urgell|CAT|42.361189|1.481894|0
Sant Pere|Sant Pere||Alt Urgell|CAT|42.229162|1.496004|0
Sant Pere de l'Arç|Sant Pere de l'Arç||Anoia|CAT|41.762464|1.515316|0
Sant Pere de Ribes|Sant Pere de Ribes||Garraf|CAT|41.262136|1.776066|1
Sant Pere de Riudebitlles|Sant Pere de Riudebitlles||Alt Penedès|CAT|41.458496|1.697416|1
Sant Pere de Rodes|Sant Pere de Rodes||Alt Empordà|CAT|42.323326|3.166376|0
Sant Pere de Torelló|Sant Pere de Torelló||Osona|CAT|42.086504|2.33006|1
Sant Pere de Vilamajor|Sant Pere de Vilamajor||Vallès Oriental|CAT|41.711527|2.386252|1
Sant Pere del Vim|Sant Pere del Vim||Anoia|CAT|41.681189|1.527929|0
Sant Pere dels Arquells|Sant Pere dels Arquells||Segarra|CAT|41.642951|1.317487|0
Sant Pere dels Forcats|Saint-Pierre-dels-Forcats||l'Alta Cerdanya|CN|42.47943|2.118268|1
Sant Pere I Sant Pau|Sant Pere I Sant Pau||Tarragonès|CAT|41.139067|1.252699|0
Sant Pere Molanta|Sant Pere Molanta||Alt Penedès|CAT|41.347788|1.740618|0
Sant Pere Pescador|Sant Pere Pescador||Alt Empordà|CAT|42.186449|3.091861|1
Sant Pere Sacarrera|Sant Pere Sacarrera||Alt Penedès|CAT|41.488852|1.652468|0
Sant Pere Sallavinera|Sant Pere Sallavinera||Anoia|CAT|41.751418|1.562981|1
Sant Pol|Sant Pol||Baix Empordà|CAT|41.930183|3.036025|0
Sant Pol de Mar|Sant Pol de Mar||Maresme|CAT|41.607373|2.615832|1
Sant Privat d'en Bas|Sant Privat d'en Bas||Garrotxa|CAT|42.150558|2.417798|0
Sant Quintí de Mediona|Sant Quintí de Mediona||Alt Penedès|CAT|41.452731|1.665402|1
Sant Quirze|Sant Quirze||Maresme|CAT|41.621151|2.660854|0
Sant Quirze|Sant Quirze||Osona|CAT|41.882305|2.300778|0
Sant Quirze de Besora|Sant Quirze de Besora||Osona|CAT|42.102797|2.229789|1
Sant Quirze del Vallès|Sant Quirze del Vallès||Vallès Occidental|CAT|41.524998|2.070027|1
Sant Quirze Jardí|Sant Quirze Jardí||Vallès Occidental|CAT|41.532729|2.07481|0
Sant Quirze Safaja|Sant Quirze Safaja||Moianès|CAT|41.725857|2.185246|1
Sant Rafel de sa Creu|Sant Rafel de sa Creu||Eivissa|IB|38.958639|1.403971|0
Sant Rafel del Riu|San Rafael del Río||Baix Maestrat|PV|40.594527|0.339314|1
Sant Ramon|Sant Ramon||Segarra|CAT|41.733404|1.358728|1
Sant Ramon|Sant Ramon||Anoia|CAT|41.577698|1.645243|0
Sant Ramon|Sant Ramon||Tarragonès|CAT|41.155538|1.243014|0
Sant Ramon|Sant Ramon||Horta Sud|PV|39.386301|-0.469044|0
Sant Roc|Sant Roc||Baix Llobregat|CAT|41.378647|2.010092|0
Sant Roc|Sant Roc||Selva|CAT|41.768653|2.669125|0
Sant Roc|Sant Roc||Tarragonès|CAT|41.204062|1.296086|0
Sant Roc|Sant Roc||Tarragonès|CAT|41.204652|1.295043|0
Sant Roc|Sant Roc||Marina Baixa|PV|38.609719|-0.058988|0
Sant Romà d'Abella|Sant Romà d'Abella||Pallars Jussà|CAT|42.140642|1.040559|0
Sant Romà de Sau|Sant Romà de Sau||Osona|CAT|41.966354|2.409239|0
Sant Sadurní d'Anoia|Sant Sadurní d'Anoia||Alt Penedès|CAT|41.438621|1.787338|1
Sant Sadurní d'Osormort|Sant Sadurní d'Osormort||Osona|CAT|41.90512|2.37986|1
Sant Sadurní de l'Heura|Sant Sadurní de l'Heura||Baix Empordà|CAT|41.95614|2.990549|0
Sant Salvador|Sant Salvador||Alt Penedès|CAT|41.454269|1.865971|0
Sant Salvador|Sant Salvador||Tarragonès|CAT|41.163521|1.243706|0
Sant Salvador|Sant Salvador||Baix Penedès|CAT|41.184823|1.544202|0
Sant Salvador de Guardiola|Sant Salvador de Guardiola||Bages|CAT|41.676274|1.762103|1
Sant Salvador de la Calçada|Sant Salvador de la Calçada||Alt Penedès|CAT|41.453198|1.868456|0
Sant Salvador de Toló|Sant Salvador de Toló||Pallars Jussà|CAT|42.08113|1.031263|0
Sant Sebastià dels Gorgs|Sant Sebastià dels Gorgs||Alt Penedès|CAT|41.380748|1.765256|0""",
        """Sant Serni|Sant Serni||Pallars Jussà|CAT|42.105194|0.921532|0
Sant Serni|Sant Serni||Solsonès|CAT|41.844002|1.465974|0
Sant Tomàs|Sant Tomàs||Menorca|IB|39.916323|4.039858|0
Sant Tomàs de Fluvià|Sant Tomàs de Fluvià||Alt Empordà|CAT|42.181552|3.010279|0
Sant Valerià|Sant Valerià||Vallès Oriental|CAT|41.584347|2.232677|0
Sant Vicent|Sant Vicent||Camp de Túria|PV|39.6479|-0.561368|0
Sant Vicent de sa Cala|Sant Vicent de sa Cala||Eivissa|IB|39.077752|1.559112|0
Sant Vicent del Raspeig|Sant Vicent del Raspeig / San Vicente del Raspeig||Alacantí|PV|38.427089|-0.534442|1
Sant Vicenç de Calders|Sant Vicenç de Calders||Baix Penedès|CAT|41.203036|1.515533|0
Sant Vicenç de Castellet|Sant Vicenç de Castellet||Bages|CAT|41.669101|1.876989|1
Sant Vicenç de Montalt|Sant Vicenç de Montalt||Maresme|CAT|41.581254|2.508157|1
Sant Vicenç de Torelló|Sant Vicenç de Torelló||Osona|CAT|42.064719|2.264864|1
Sant Vicenç dels Horts|Sant Vicenç dels Horts||Baix Llobregat|CAT|41.390527|2.002929|1
Santa Agnès de Corona|Santa Agnès de Corona||Eivissa|IB|39.038476|1.336249|0
Santa Agnès de Malanyanes|Santa Agnès de Malanyanes||Vallès Oriental|CAT|41.600904|2.351529|0
Santa Anna|Santa Anna||Maresme|CAT|41.62921|2.674145|0
Santa Anna|Santa Anna||Menorca|IB|39.875753|4.293248|0
Santa Anna-Tió|Santa Anna-Tió||Maresme|CAT|41.497518|2.362561|0
Santa Bàrbara|Santa Bàrbara||Montsià|CAT|40.723193|0.481355|1
Santa Cecília de Voltregà|Santa Cecília de Voltregà||Osona|CAT|41.997941|2.210759|1
Santa Coloma|Santa Coloma|||AD|42.50779|1.52109|0
Santa Coloma de Cervelló|Santa Coloma de Cervelló||Baix Llobregat|CAT|41.367861|2.018731|1
Santa Coloma de Farners|Santa Coloma de Farners||Selva|CAT|41.868624|2.625325|1
Santa Coloma de Gramenet|Santa Coloma de Gramenet||Barcelonès|CAT|41.457793|2.20595|1
Santa Coloma de la Comanda|Sainte-Colombe-de-la-Commanderie||el Rosselló|CN|42.615036|2.739958|1
Santa Coloma de Queralt|Santa Coloma de Queralt||Conca de Barberà|CAT|41.546091|1.395895|1
Santa Creu|Santa Creu||Vallès Oriental|CAT|41.671291|2.254274|0
Santa Creu|Santa Creu||Alt Urgell|CAT|42.404916|1.311569|0
Santa Creu de Jutglar|Santa Creu de Jutglar||Lluçanès|CAT|42.006835|2.062656|0
Santa Cristina|Santa Cristina||Baix Empordà|CAT|41.987056|3.013718|0
Santa Cristina d'Aro|Santa Cristina d'Aro||Baix Empordà|CAT|41.827571|2.981667|1
Santa Càndia|Santa Càndia||Anoia|CAT|41.523675|1.576217|0
Santa Engràcia|Santa Engràcia||Pallars Jussà|CAT|42.223107|0.873884|0
Santa Eugènia|Santa Eugènia||Mallorca|IB|39.618723|2.834267|1
Santa Eugènia de Berga|Santa Eugènia de Berga||Osona|CAT|41.908298|2.280938|1
Santa Eugènia de Relat|Santa Eugènia de Relat||Bages|CAT|41.880792|1.977448|0
Santa Eugènia del Congost|Santa Eugènia del Congost||Vallès Oriental|CAT|41.731927|2.268486|0
Santa Eulalia|Santa Eulalia||Alt Vinalopó|PV|38.570638|-0.84231|0
Santa Eulàlia|Santa Eulàlia||Baix Llobregat|CAT|41.337441|1.916672|0
Santa Eulàlia|Santa Eulàlia||Alt Empordà|CAT|42.173871|2.964153|0
Santa Eulàlia de Puig-oriol|Santa Eulàlia de Puig-oriol||Lluçanès|CAT|42.067169|2.073397|0
Santa Eulàlia de Riuprimer|Santa Eulàlia de Riuprimer||Osona|CAT|41.912571|2.173607|1
Santa Eulàlia de Ronçana|Santa Eulàlia de Ronçana||Vallès Oriental|CAT|41.647|2.221768|1
Santa Eulària des Riu|Santa Eulària des Riu||Eivissa|IB|38.993039|1.497121|1
Santa Fe|Santa Fe||Maresme|CAT|41.513539|2.311069|0
Santa Fe|Santa Fe||Segarra|CAT|41.69944|1.363228|0
Santa Fe|Santa Fe||Baix Vinalopó|PV|38.178593|-0.673462|0
Santa Fe del Penedès|Santa Fe del Penedès||Alt Penedès|CAT|41.388554|1.72007|1
Santa Gertrudis de Fruitera|Santa Gertrudis de Fruitera||Eivissa|IB|38.998928|1.427978|0
Santa Gèmma|Santa Gèmma||la Vall d'Aran|CAT|42.71508|0.798445|0
Santa Justa|Santa Justa||Vallès Oriental|CAT|41.622929|2.248886|0
Santa Linya|Santa Linya||Noguera|CAT|41.928451|0.800613|0
Santa Llocaia|Sainte-Léocadie||l'Alta Cerdanya|CN|42.433771|2.009613|1
Santa Llogaia d'Àlguema|Santa Llogaia d'Àlguema||Alt Empordà|CAT|42.232924|2.952476|1
Santa Magdalena|Santa Magdalena||Alt Urgell|CAT|42.352311|1.454363|0
Santa Magdalena de Polpís|Santa Magdalena de Pulpis||Baix Maestrat|PV|40.3747|0.273308|1
Santa Margalida|Santa Margalida||Mallorca|IB|39.721043|3.154705|1
Santa Margarida|Santa Margarida||Vallès Oriental|CAT|41.715402|2.427587|0
Santa Margarida|Santa Margarida||Alt Empordà|CAT|42.265453|3.147297|0
Santa Margarida de Montbui|Santa Margarida de Montbui||Anoia|CAT|41.560677|1.576604|1
Santa Margarida de Montbui-nucli antic|Santa Margarida de Montbui-nucli antic||Anoia|CAT|41.555478|1.602988|0
Santa Margarida i els Monjos|Santa Margarida i els Monjos||Alt Penedès|CAT|41.311662|1.664058|1
Santa Maria|Santa Maria||Alt Penedès|CAT|41.423103|1.664071|0
Santa Maria d'Oló|Santa Maria d'Oló||Moianès|CAT|41.869404|2.048798|1
Santa Maria de Besora|Santa Maria de Besora||Osona|CAT|42.1219|2.260109|1
Santa Maria de Bonaire|Santa Maria de Bonaire||Ribera Alta|PV|39.150891|-0.41846|0
Santa Maria de Gimenells|Santa Maria de Gimenells||Segrià|CAT|41.655283|0.403746|0
Santa Maria de l'Avall|Santa Maria de l'Avall||Baix Llobregat|CAT|41.424171|1.949544|0
Santa Maria de Llorell|Santa Maria de Llorell||Selva|CAT|41.716785|2.905984|0
Santa Maria de Martorelles|Santa Maria de Martorelles||Vallès Oriental|CAT|41.517842|2.261554|1
Santa Maria de Meià|Santa Maria de Meià||Noguera|CAT|41.993215|0.993798|0
Santa Maria de Merlès|Santa Maria de Merlès||Berguedà|CAT|41.988836|1.968245|1
Santa Maria de Miralles|Santa Maria de Miralles||Anoia|CAT|41.505486|1.515921|1
Santa Maria de Montmagastrell|Santa Maria de Montmagastrell||Urgell|CAT|41.720327|1.106297|0
Santa Maria de Palautordera|Santa Maria de Palautordera||Vallès Oriental|CAT|41.684247|2.453567|1
Santa Maria de Vilalba i Can Torres|Santa Maria de Vilalba i Can Torres||Baix Llobregat|CAT|41.507517|1.930365|0
Santa Maria del Camí|Santa Maria del Camí||Mallorca|IB|39.664126|2.772133|1
Santa Maria del Camí|Santa Maria del Camí||Anoia|CAT|41.629075|1.474754|0
Santa Maria I Sud Sumella|Santa Maria I Sud Sumella||Garraf|CAT|41.21294|1.678098|0
Santa Maria la Mar|Sainte-Marie-la-Mer||el Rosselló|CN|42.726052|3.016795|1
Santa Maria La Palma|Santa Maria La Palma|||AL|40.55969|8.31953|0
Santa Marina|Santa Marina||Baix Camp|CAT|41.040016|0.854405|0
Santa Marina|Santa Marina||Ribera Alta|PV|39.09578|-0.364861|0
Santa Marta|Santa Marta||Safor|PV|38.977904|-0.204761|0
Santa Olga|Santa Olga||Anoia|CAT|41.601669|1.640971|0
Santa Oliva|Santa Oliva||Baix Penedès|CAT|41.249085|1.5388|1
Santa Pau|Santa Pau||Garrotxa|CAT|42.151215|2.569255|1
Santa Perpètua de Gaià|Santa Perpètua de Gaià||Conca de Barberà|CAT|41.462034|1.392534|0
Santa Perpètua de Mogoda|Santa Perpètua de Mogoda||Vallès Occidental|CAT|41.533184|2.176551|1
Santa Pola|Santa Pola||Baix Vinalopó|PV|38.203509|-0.577304|1
Santa Ponça|Santa Ponça||Mallorca|IB|39.506831|2.477674|0
Santa Quitèria|Santa Quitèria||Plana Alta|PV|39.95742|-0.09819|0
Santa Rita|Santa Rita||Vallès Occidental|CAT|41.460072|1.991067|0
Santa Rosa|Santa Rosa||Baix Llobregat|CAT|41.38905|1.950871|0
Santa Rosa|Santa Rosa||Camp de Túria|PV|39.550207|-0.533926|0
Santa Rosa de Lima|Santa Rosa de Lima||Baix Ebre|CAT|40.875429|0.516901|0
Santa Rosa dels Pins|Santa Rosa dels Pins||Maresme|CAT|41.601416|2.512835|0
Santa Susanna|Santa Susanna||Maresme|CAT|41.649958|2.695841|1
Santa Susanna|Santa Susanna||Solsonès|CAT|41.9459|1.60117|0
Santa Susanna de Peralta|Santa Susanna de Peralta||Baix Empordà|CAT|41.95467|3.090832|0
Santa Tecla|Santa Tecla||Tarragonès|CAT|41.19959|1.294048|0
Santa Teresita|Santa Teresita||Vallès Occidental|CAT|41.46717|1.982995|0
Santanyí|Santanyí||Mallorca|IB|39.349396|3.132196|1
Santes Creus|Santes Creus||Alt Camp|CAT|41.346296|1.362703|0
Santoréns|Santoréns||la Ribagorça|FR|42.350354|0.715715|0
Santpedor|Santpedor||Bages|CAT|41.783664|1.835818|1
Sants Metges|Sants Metges||Berguedà|CAT|42.05906|1.767387|0
Sanui i Alins|Azanuy-Alins||la Llitera|FR|41.981327|0.310195|1
Sançà|Sansa||el Conflent|CN|42.619696|2.180527|1
Saorra|Sahorre||el Conflent|CN|42.523293|2.359025|1
Sapeira|Sapeira||Pallars Jussà|CAT|42.253876|0.789861|0
Sarañana|Sarañana||Ports|PV|40.633518|-0.245508|0
Sardinyola|Sardinyola||Alt Penedès|CAT|41.316366|1.645842|0
Sarral|Sarral||Conca de Barberà|CAT|41.453147|1.271981|1
Sarrià de Ter|Sarrià de Ter||Gironès|CAT|42.013956|2.812348|1
Sarroca de Bellera|Sarroca de Bellera||Pallars Jussà|CAT|42.408492|0.886476|1
Sarroca de Lleida|Sarroca de Lleida||Segrià|CAT|41.464049|0.559953|1
Sarva|Sarva||Camp de Morvedre|PV|39.72821|-0.373529|0
Saurí|Saurí||Pallars Sobirà|CAT|42.445656|1.073097|0
Saus|Saus||Alt Empordà|CAT|42.132234|2.977989|0
Saus, Camallera i Llampaies|Saus, Camallera i Llampaies||Alt Empordà|CAT|42.125949|2.961289|1
Savallà del Comtat|Savallà del Comtat||Conca de Barberà|CAT|41.543358|1.287497|1
Seana|Seana||Urgell|CAT|41.670428|1.018689|0
Sector Camping|Sector Camping||Baix Empordà|CAT|41.802836|3.010898|0
Sector Casa Groga|Sector Casa Groga||Baix Empordà|CAT|41.819944|3.004173|0
Sector D|Sector D||Montsià|CAT|40.559823|0.533677|0
Sector E|Sector E||Montsià|CAT|40.566381|0.535354|0
Sector F|Sector F||Montsià|CAT|40.576292|0.545838|0
Sector G|Sector G||Montsià|CAT|40.584198|0.556717|0
Sector H|Sector H||Montsià|CAT|40.591637|0.562528|0
Sector I|Sector I||Montsià|CAT|40.595651|0.57102|0
Sector J|Sector J||Montsià|CAT|40.602337|0.574241|0
Sector R-6|Sector R-6||Camp de Túria|PV|39.578446|-0.483506|0
Sector R-7|Sector R-7||Camp de Túria|PV|39.564166|-0.477207|0
Sector Residencial Golf|Sector Residencial Golf||Selva|CAT|41.856487|2.764512|0
Sector Riera Dret|Sector Riera Dret||Maresme|CAT|41.498563|2.322162|0
Sector Riera Esquerre|Sector Riera Esquerre||Maresme|CAT|41.499797|2.31893|0
Sectores La Cuerda y Lomas de Cabo Roig|Sectores La Cuerda y Lomas de Cabo Roig||Baix Segura|PV|37.919891|-0.749446|0
Sedaví|Sedaví||Horta Sud|PV|39.423949|-0.376161|1
Sedó|Sedó||Segarra|CAT|41.732372|1.260224|0
Segart|Segart||Camp de Morvedre|PV|39.678259|-0.381877|1
Segudet|Segudet|||AD|42.55623|1.53319|0
Seguer|Seguer||Conca de Barberà|CAT|41.438489|1.404387|0
Segur|Segur||Anoia|CAT|41.68089|1.469118|0
Segur de Calafell|Segur de Calafell||Baix Penedès|CAT|41.194174|1.608391|0
Segura|Segura||Conca de Barberà|CAT|41.544023|1.265387|0
Sella|Sella||Marina Baixa|PV|38.617886|-0.278834|1
Sellent|Sellent||Ribera Alta|PV|39.033425|-0.592247|1
Sellui|Sellui||Pallars Sobirà|CAT|42.321497|1.060813|0
Selva|Selva||Mallorca|IB|39.768765|2.908995|1
Selva Brava|Selva Brava||Gironès|CAT|41.788629|2.897174|0
Selva Negra|Selva Negra||Baix Llobregat|CAT|41.377139|1.941484|0
Selvanera|Selvanera||Segarra|CAT|41.832234|1.275766|0
Sempere|Sempere||Vall d'Albaida|PV|38.918489|-0.471524|1
Senan|Senan||Conca de Barberà|CAT|41.476685|1.069493|1
Sencelles|Sencelles||Mallorca|IB|39.640429|2.898045|1
Senda de Les Vaques|Senda de Les Vaques||Horta Sud|PV|39.420441|-0.365107|0
Senet|Senet||Alta Ribagorça|CAT|42.557398|0.752866|0
Senija|Senija||Marina Alta|PV|38.734438|0.044287|1
Senterada|Senterada||Pallars Jussà|CAT|42.321659|0.923598|1
Sentmenat|Sentmenat||Vallès Occidental|CAT|41.613391|2.130506|1
Sentís|Sentís||Pallars Jussà|CAT|42.396943|0.850545|0
Senyera|Senyera||Ribera Alta|PV|39.064656|-0.506294|1
Serdinyà|Serdinya||el Conflent|CN|42.576135|2.322534|1
Serinyà|Serinyà||Pla de l'Estany|CAT|42.167423|2.73791|1
Serpentona|Serpentona||Menorca|IB|39.941397|3.959672|0
Serra|Serra||Camp de Túria|PV|39.684637|-0.455351|1
Serra de Daró|Serra de Daró||Baix Empordà|CAT|42.030764|3.065518|1
Serrabrava|Serrabrava||Selva|CAT|41.720832|2.873517|0
Serradalt|Serradalt||Alt Camp|CAT|41.281512|1.200903|0
Serradell|Serradell||Pallars Jussà|CAT|42.275182|0.900871|0
Serradúy|Serradúy||la Ribagorça|FR|42.318819|0.572489|0
Serrallonga|Serralongue||el Vallespir|CN|42.382807|2.557472|1
Serrat|Serrat||Ripollès|CAT|42.342106|2.184202|0
Serrat de la Capella|Serrat de la Capella||Alt Urgell|CAT|42.363746|1.456357|0
Serres|Serres||Selva|CAT|41.789386|2.77527|0
Serreta de la Torre|Serreta de la Torre||Alacantí|PV|38.410931|-0.560396|0
Serveres|Serveres||Alacantí|PV|38.414023|-0.465438|0
Seròs|Seròs||Segrià|CAT|41.414412|0.42299|1
Seró|Seró||Noguera|CAT|41.874435|1.107445|0
ses Bardetes|ses Bardetes||Formentera|IB|38.697861|1.425615|0
ses Cadenes|ses Cadenes||Mallorca|IB|39.514133|2.757917|0
Ses Cases Noves|Ses Cases Noves||Mallorca|IB|39.610885|2.702278|0
ses Coves|ses Coves||Mallorca|IB|39.761377|2.650462|0
ses Covetes|ses Covetes||Mallorca|IB|39.354664|2.971763|0
ses Salines|ses Salines||Mallorca|IB|39.331016|3.043562|1
ses Salines|ses Salines||Menorca|IB|40.040382|4.122761|0
ses Torres|ses Torres||Eivissa|IB|38.922196|1.458072|0
Set-Rengs|Set-Rengs||Bages|CAT|41.675706|1.742853|0
Setaigües|Siete Aguas||Foia de Bunyol|PV|39.484443|-0.924628|1
Setcases|Setcases||Ripollès|CAT|42.400111|2.294321|1
Setcases|Setcases||Alt Empordà|CAT|42.366788|3.150593|0
Setla|Setla||Comtat|PV|38.789053|-0.428862|0
Setla|Setla||Marina Alta|PV|38.853901|0.015114|0
Seva|Seva||Osona|CAT|41.830361|2.281418|1
Señiú|Señiú||la Ribagorça|FR|42.459789|0.642665|0
Sidamon|Sidamon||Pla d'Urgell|CAT|41.626127|0.83358|1
Silet|Silet||Pla de l'Estany|CAT|42.122126|2.711847|0
Silla|Silla||Horta Sud|PV|39.356552|-0.402622|1
Sils|Sils||Selva|CAT|41.827736|2.739283|1
Simat de la Valldigna|Simat de la Valldigna||Safor|PV|39.041885|-0.339917|1
Sinarques|Sinarcas||Plana d'Utiel|PV|39.745761|-1.210247|1
Sineu|Sineu||Mallorca|IB|39.646357|3.021196|1
Sispony|Sispony|||AD|42.54499|1.51483|0
Sisquer|Sisquer||Alt Urgell|CAT|42.248296|1.472393|0
Sisteró|Sisteró||Segarra|CAT|41.754269|1.207489|0
Sitges|Sitges||Garraf|CAT|41.255998|1.860058|1
Siurana|Siurana||Alt Empordà|CAT|42.211975|3.007125|1
Siurana|Siurana||Priorat|CAT|41.257894|0.932406|0
So n'Espanyol|So n'Espanyol||Mallorca|IB|39.638868|2.63909|0
So na Rossa|So na Rossa||Mallorca|IB|39.635139|2.867004|0
Soanyes|Souanyas||el Conflent|CN|42.548394|2.283873|1
Sobremunt|Sobremunt||Lluçanès|CAT|42.021622|2.170163|1
Sobrevila|Sobrevila||Baix Empordà|CAT|42.02294|3.002872|0
Sobrànigues|Sobrànigues||Gironès|CAT|42.060069|2.962964|0
Sogorb|Segorbe||Alt Palància|PV|39.787812|-0.463045|1
Sol de l'Est|Sol de l'Est||Menorca|IB|39.872944|4.297428|0
Sol de Mallorca|Sol de Mallorca||Mallorca|IB|39.480929|2.524583|0
Sol del Este|Sol del Este||Marina Alta|PV|38.75586|0.168577|0
Sol i Aire|Sol i Aire||Bages|CAT|41.721733|1.808834|0
Sol i Camp|Sol i Camp||Alcoià|PV|38.711924|-0.470733|0
Sol-i-vista|Sol-i-vista||Baix Camp|CAT|41.157794|1.087545|0
Solans|Solans||Alt Urgell|CAT|42.334691|1.251954|0
Soldeu|Soldeu|||AD|42.5676|1.59756|0
Soleia|Soleia||Selva|CAT|41.694915|2.809587|0
Soleia Dos|Soleia Dos||Baix Llobregat|CAT|41.386188|1.939647|0
Soleia U|Soleia U||Baix Llobregat|CAT|41.387988|1.941269|0
Solimar|Solimar||Tarragonès|CAT|41.129282|1.298485|0
Solivella|Solivella||Conca de Barberà|CAT|41.463959|1.183399|1
Solivent|Solivent||Alt Camp|CAT|41.302292|1.245028|0
Sollana|Sollana||Ribera Baixa|PV|39.28134|-0.377102|1
Solsona|Solsona||Solsonès|CAT|41.992787|1.518237|1
Son|Son||Pallars Sobirà|CAT|42.619995|1.096771|0
Son Ametler|Son Ametler||Mallorca|IB|39.603552|2.723423|0
Son Bessó|Son Bessó||Mallorca|IB|39.607827|2.653675|0
Son Blanc|Son Blanc||Menorca|IB|39.986212|3.833295|0
Son Bou|Son Bou||Menorca|IB|39.898953|4.077815|0
Son Cabrisses|Son Cabrisses||Menorca|IB|39.959094|3.843203|0
Son Carrió|Son Carrió||Menorca|IB|39.97534|3.839846|0
Son Carrió|Son Carrió||Mallorca|IB|39.587714|3.326936|0
Son Coll|Son Coll||Mallorca|IB|39.694209|2.555741|0
Son Coll|Son Coll||Mallorca|IB|39.765986|2.660908|0
Son Daviu|Son Daviu||Mallorca|IB|39.621476|2.718821|0
Son Ferrandell|Son Ferrandell||Mallorca|IB|39.704674|2.579429|0
Son Ferrer|Son Ferrer||Mallorca|IB|39.49173|2.512658|0
Son Ferriol|Son Ferriol||Mallorca|IB|39.574959|2.718186|0
Son Ganxo|Son Ganxo||Menorca|IB|39.811022|4.266478|0
Son Granada|Son Granada||Mallorca|IB|39.472118|2.752447|0
Son Gual|Son Gual||Mallorca|IB|39.565889|2.789928|0
Son Macià|Son Macià||Mallorca|IB|39.514081|3.218315|0""",
        """Son Macià-Nova Cabana|Son Macià-Nova Cabana||Mallorca|IB|39.613034|2.688832|0
Son Maixella|Son Maixella||Mallorca|IB|39.66923|2.640127|0
Son Mas|Son Mas||Mallorca|IB|39.580451|3.241156|0
Son Moja|Son Moja||Mallorca|IB|39.338245|3.136401|0
Son Moll|Son Moll||Mallorca|IB|39.702636|3.453334|0
Son Moro|Son Moro||Mallorca|IB|39.595416|3.381513|0
Son Moro Bonavista|Son Moro Bonavista||Mallorca|IB|39.603234|3.36756|0
Son Parc|Son Parc||Menorca|IB|40.024423|4.163819|0
Son Ramonell|Son Ramonell||Mallorca|IB|39.61831|2.71039|0
Son Rapinya|Son Rapinya||Mallorca|IB|39.590339|2.618696|0
Son Remei|Son Remei||Menorca|IB|39.813256|4.267434|0
Son Riera|Son Riera||Mallorca|IB|39.617034|2.59365|0
Son Roca|Son Roca||Mallorca|IB|39.602555|2.615911|0
Son Santjoan|Son Santjoan||Mallorca|IB|39.551962|2.738275|0
Son Sardina|Son Sardina||Mallorca|IB|39.618385|2.656788|0
Son Serra de Marina|Son Serra de Marina||Mallorca|IB|39.734484|3.229899|0
Son Serra Perera|Son Serra Perera||Mallorca|IB|39.598258|2.64414|0
Son Serralta|Son Serralta||Mallorca|IB|39.616962|2.544481|0
Son Servera|Son Servera||Mallorca|IB|39.640352|3.371125|1
Son Talent|Son Talent||Mallorca|IB|39.591956|3.221649|0
Son Verí Nou|Son Verí Nou||Mallorca|IB|39.492433|2.741484|0
Son Vida|Son Vida||Mallorca|IB|39.59055|2.589571|0
Son Vilar|Son Vilar||Menorca|IB|39.880628|4.279492|0
Son Vitamina|Son Vitamina||Menorca|IB|39.869919|4.162403|0
Son Xoriguer|Son Xoriguer||Menorca|IB|39.927102|3.843437|0
Soneja|Soneja||Alt Palància|PV|39.819459|-0.376315|1
Sopeira|Sopeira||la Ribagorça|FR|42.338643|0.7203|1
Sora|Sora||Osona|CAT|42.105858|2.170496|1
Soriguera|Soriguera||Pallars Sobirà|CAT|42.375964|1.146016|1
Soriguerola|Soriguerola||Cerdanya|CAT|42.397509|1.880927|0
Sorio|Sorio||Costera|PV|39.005947|-0.528933|0
Sorita|Zorita del Maestrazgo||Ports|PV|40.741695|-0.164809|1
Sornià|Sournia||la Fenolleda|CN|42.714957|2.414181|1
Sornàs|Sornàs|||AD|42.55623|1.53319|0
Sorpe|Sorpe||Pallars Sobirà|CAT|42.651919|1.079817|0
Sorre|Sorre||Pallars Sobirà|CAT|42.452499|1.09812|0
Sorribes de la Vansa|Sorribes de la Vansa||Alt Urgell|CAT|42.233716|1.48046|0
Sort|Sort||Pallars Sobirà|CAT|42.443033|1.08027|1
Soses|Soses||Segrià|CAT|41.539329|0.466554|1
Sossís|Sossís||Pallars Jussà|CAT|42.253029|0.985992|0
Sot d'El Boter|Sot d'El Boter||Maresme|CAT|41.615637|2.612065|0
Sot de Ferrer|Sot de Ferrer||Alt Palància|PV|39.801672|-0.4013|1
Sot de Xera|Sot de Chera||Serrans|PV|39.610066|-0.913739|1
Sotaia|Sotaia||Safor|PV|38.943111|-0.156754|0
Su|Su||Solsonès|CAT|41.888704|1.564309|0
Subirats|Subirats||Alt Penedès|CAT|41.398134|1.804687|1
Sucaina|Zucaina||Alt Millars|PV|40.131643|-0.44719|1
Sucs|Sucs||Segrià|CAT|41.701752|0.411531|0
Sudanell|Sudanell||Segrià|CAT|41.555469|0.569188|1
Sudoest|Sudoest||Baix Camp|CAT|40.995617|0.930222|0
Sueca|Sueca||Ribera Baixa|PV|39.240058|-0.30834|1
Suera|Suera/Sueras||Plana Baixa|PV|39.953758|-0.353376|1
Suils|Suils||la Ribagorça|FR|42.473405|0.60184|0
Sumacàrcer|Sumacàrcer||Ribera Alta|PV|39.086259|-0.633037|1
Sunyer|Sunyer||Segrià|CAT|41.520172|0.588217|1
Sureda|Sorède||el Rosselló|CN|42.499716|2.980916|1
Suredes d'en Mairo-Grecs-Muntanyeta|Suredes d'en Mairo-Grecs-Muntanyeta||Alt Empordà|CAT|42.264341|3.185423|0
Surp|Surp||Pallars Sobirà|CAT|42.453321|1.126563|0
Surri|Surri||Pallars Sobirà|CAT|42.565652|1.221348|0
Susqueda|Susqueda||Selva|CAT|41.995373|2.518831|1
Suterranya|Suterranya||Pallars Jussà|CAT|42.152031|0.949932|0
Síller|Síller||Mallorca|IB|39.904642|3.069003|0
Sóller|Sóller||Mallorca|IB|39.774987|2.706334|1
Súria|Súria||Bages|CAT|41.834698|1.764027|1
Tabarca|Tabarca||Alacantí|PV|38.166064|-0.48205|0
Tagamanent|Tagamanent||Vallès Oriental|CAT|41.756188|2.301175|1
Taialà|Taialà||Gironès|CAT|41.999973|2.788497|0
Talamanca|Talamanca||Bages|CAT|41.731866|1.951914|1
Talarn|Talarn||Pallars Jussà|CAT|42.178469|0.896087|1
Talavera|Talavera||Segarra|CAT|41.584886|1.361142|1
Tales|Tales||Plana Baixa|PV|39.945242|-0.300619|1
Talltendre|Talltendre||Cerdanya|CAT|42.403302|1.765523|0
Talltorta|Talltorta||Cerdanya|CAT|42.403557|1.899323|0
Talló|Talló||Cerdanya|CAT|42.363075|1.780764|0
Talteüll|Tautavel||el Rosselló|CN|42.813577|2.719363|1
Talteüll|Talteüll||Segarra|CAT|41.81914|1.328295|0
Tamarit de Llitera|Tamarite de Litera||la Llitera|FR|41.815592|0.38269|1
Tamariu|Tamariu||Baix Empordà|CAT|41.918407|3.206656|0
Tapis|Tapis||Alt Empordà|CAT|42.380302|2.7009|0
Taradell|Taradell||Osona|CAT|41.874342|2.286499|1
Taravaus|Taravaus||Alt Empordà|CAT|42.244429|2.889704|0
Tarerac|Tarerach||el Conflent|CN|42.684928|2.510937|1
Targasona|Targasonne||l'Alta Cerdanya|CN|42.503595|1.988613|1
Tarragona|Tarragona||Tarragonès|CAT|41.132104|1.269023|1
Tarroja de Segarra|Tarroja de Segarra||Segarra|CAT|41.722251|1.277156|1
Tarrés|Tarrés||Garrigues|CAT|41.429661|1.021805|1
Tartareu|Tartareu||Noguera|CAT|41.92039|0.7176|0
Tartera|Tartera||Cerdanya|CAT|42.35898|1.85295|0
Taurinyà|Taurinya||el Conflent|CN|42.558894|2.442555|1
Tavascan|Tavascan||Pallars Sobirà|CAT|42.644182|1.256928|0
Tavernes Blanques|Tavernes Blanques||Horta Nord|PV|39.507349|-0.364367|1
Tavernes de la Valldigna|Tavernes de la Valldigna||Safor|PV|39.080343|-0.254718|1
Tavertet|Tavertet||Osona|CAT|42.004869|2.40352|1
Tavèrnoles|Tavèrnoles||Osona|CAT|41.956107|2.344329|1
Taús|Taús||Alt Urgell|CAT|42.290698|1.19278|0
Taüll|Taüll||Alta Ribagorça|CAT|42.519266|0.848842|0
Teià|Teià||Maresme|CAT|41.500543|2.321671|1
Tejerías|Tejerías||Plana Baixa|PV|39.952644|-0.304334|0
Tellet|Taillet||el Rosselló|CN|42.516862|2.679043|1
Tendrui|Tendrui||Pallars Jussà|CAT|42.175838|0.848788|0
Tercui|Tercui||Pallars Jussà|CAT|42.178902|0.721277|0
Teresa|Teresa||Alt Palància|PV|39.888709|-0.674894|1
Teresa de Cofrents|Teresa de Cofrentes||Vall de Cofrents|PV|39.111656|-0.982387|1
Terra Nostra|Terra Nostra||Vallès Occidental|CAT|41.478773|2.166609|0
Terrabrava|Terrabrava||Maresme|CAT|41.733949|2.732802|0
Terradelles|Terradelles||Berguedà|CAT|42.238372|1.876056|0
Terradelles|Terradelles||Pla de l'Estany|CAT|42.118734|2.888393|0
Terrades|Terrades||Alt Empordà|CAT|42.311606|2.842666|1
Terrafortuna|Terrafortuna||Selva|CAT|41.760685|2.783354|0
Terramelar|Terramelar||Horta Nord|PV|39.508181|-0.428997|0
Terrassa|Terrassa||Vallès Occidental|CAT|41.57536|2.008002|1
Terrassola|Terrassola||Alt Penedès|CAT|41.447908|1.726616|0
Terrateig|Terrateig||Vall d'Albaida|PV|38.898098|-0.313268|1
Terrats|Terrats||el Rosselló|CN|42.602755|2.759835|1
Terveu|Terveu||Pallars Sobirà|CAT|42.511557|1.228971|0
Tesoro de Jávea|Tesoro de Jávea||Marina Alta|PV|38.749065|0.144064|0
Tesà|Théza||el Rosselló|CN|42.63965|2.945867|1
Teulada|Teulada||Marina Alta|PV|38.711906|0.114441|1
Teulís|Taulis||el Rosselló|CN|42.514311|2.618777|1
Tiana|Tiana||Maresme|CAT|41.485815|2.268219|1
Tibel|Tibel||Vallès Oriental|CAT|41.619506|2.269264|0
Tibi|Tibi||Alcoià|PV|38.521479|-0.59432|1
Titaigües|Titaguas||Serrans|PV|39.870731|-1.111948|1
Tiurana|Tiurana||Noguera|CAT|41.956517|1.235419|1
Tivenys|Tivenys||Baix Ebre|CAT|40.915347|0.552318|1
Tivissa|Tivissa||Ribera d'Ebre|CAT|41.018586|0.73357|1
Toga|Toga||Alt Millars|PV|40.05155|-0.375875|1
Toixa|Tuéjar||Serrans|PV|39.804348|-1.113398|1
Tolba|Tolva||la Ribagorça|FR|42.134593|0.564655|1
Tolleric|Tolleric||Mallorca|IB|39.417051|2.746966|0
Tollos|Tollos||Comtat|PV|38.762705|-0.247393|1
Toloriu|Toloriu||Alt Urgell|CAT|42.362424|1.628247|0
Toluges|Toulouges||el Rosselló|CN|42.669875|2.824054|1
Ton Ferrer|Ton Ferrer||Camp de Morvedre|PV|39.736672|-0.354088|0
Tona|Tona||Osona|CAT|41.85574|2.219402|1
Tor|Tor||Baix Empordà|CAT|42.088815|3.066351|0
Toralla|Toralla||Pallars Jussà|CAT|42.264073|0.921637|0
Torallola|Torallola||Pallars Jussà|CAT|42.240743|0.938369|0
Tordera|Tordera||Maresme|CAT|41.692825|2.685998|1
Tordera Parc|Tordera Parc||Maresme|CAT|41.723463|2.748875|0
Torderes|Tordères||el Rosselló|CN|42.565579|2.758877|1
Torelló|Torelló||Osona|CAT|42.038291|2.266405|1
Tormos|Tormos||Marina Alta|PV|38.803826|-0.072811|1
Tornabous|Tornabous||Urgell|CAT|41.71077|1.059551|1
Tornafort|Tornafort||Pallars Sobirà|CAT|42.368855|1.140204|0
Torralba|Torralba del Pinar||Alt Millars|PV|39.994877|-0.449164|1
Torre Cerdà|Torre Cerdà||Costera|PV|38.981636|-0.576875|0
Torre Colomina|Torre Colomina||Baix Empordà|CAT|41.853278|3.111613|0
Torre de la Horadada|Torre de la Horadada||Baix Segura|PV|37.867861|-0.762865|0
Torre de Porta Coeli|Torre de Porta Coeli||Camp de Túria|PV|39.65119|-0.511183|0
Torre de Santa Maria|Torre de Santa Maria||Alt Camp|CAT|41.348771|1.327489|0
Torre del Ram|Torre del Ram||Menorca|IB|40.002178|3.808141|0
Torre del Veguer|Torre del Veguer||Garraf|CAT|41.251702|1.732728|0
Torre Simona|Torre Simona||Baix Empordà|CAT|41.899322|3.151157|0
Torre Vileta|Torre Vileta||Baix Llobregat|CAT|41.393799|1.983125|0
Torre-Romeu|Torre-Romeu||Vallès Occidental|CAT|41.541831|2.133557|0
Torre-serona|Torre-serona||Segrià|CAT|41.676377|0.63587|1
Torre-xiva|Torrechiva||Alt Millars|PV|40.049121|-0.404132|1
Torrealta|Torrealta||Racó d'Ademús|PV|40.119115|-1.25691|0
Torrebaixa|Torrebaja||Racó d'Ademús|PV|40.101665|-1.259847|1
Torrebesses|Torrebesses||Segrià|CAT|41.420719|0.598345|1
Torreblanca|Torreblanca||Plana Alta|PV|40.209827|0.201495|1
Torreblanca I|Torreblanca I||Vallès Occidental|CAT|41.592483|1.918157|0
Torreblanca II|Torreblanca II||Vallès Occidental|CAT|41.593541|1.924814|0
Torrebonica|Torrebonica||Vallès Occidental|CAT|41.562871|2.051187|0
Torredarques|Torre de Arcas||el Matarranya|FR|40.761157|-0.096405|1
Torredembarra|Torredembarra||Tarragonès|CAT|41.15104|1.408194|1
Torrefarrera|Torrefarrera||Segrià|CAT|41.693506|0.556726|1
Torrefeta|Torrefeta||Segarra|CAT|41.754433|1.274872|0
Torrefeta i Florejacs|Torrefeta i Florejacs||Segarra|CAT|41.792444|1.260144|1
Torreforta|Torreforta||Tarragonès|CAT|41.118991|1.218093|0
Torregrossa|Torregrossa||Pla d'Urgell|CAT|41.583745|0.809778|1
Torrelameu|Torrelameu||Noguera|CAT|41.712267|0.702946|1
Torrelavit|Torrelavit||Alt Penedès|CAT|41.440632|1.722903|1
Torrella|Torrella||Costera|PV|38.98734|-0.573749|1
Torrellano|Torrellano||Baix Vinalopó|PV|38.29345|-0.584703|0
Torrelles|Torreilles||el Rosselló|CN|42.753845|3.00837|1
Torrelles de Foix|Torrelles de Foix||Alt Penedès|CAT|41.409347|1.570246|1
Torrelles de Llobregat|Torrelles de Llobregat||Baix Llobregat|CAT|41.354524|1.971072|1
Torrelletes|Torrelletes||Alt Penedès|CAT|41.277263|1.652382|0
Torremendo|Torremendo||Baix Segura|PV|37.992288|-0.866165|0
Torremirona|Torremirona||Alt Empordà|CAT|42.235335|2.864179|0
Torrenostra|Torrenostra||Plana Alta|PV|40.194021|0.220544|0
Torrent|Torrent||Baix Empordà|CAT|41.951505|3.12907|1
Torrent|Torrent||Horta Sud|PV|39.417635|-0.522346|1
Torrent d'en Puig|Torrent d'en Puig||Maresme|CAT|41.605064|2.536267|0
Torrent de Cinca|Torrente de Cinca||el Baix Cinca|FR|41.450309|0.303473|1
Torrent de Fenollet|Torrent de Fenollet||Costera|PV|39.001393|-0.57164|0
Torrent del Lluc|Torrent del Lluc||Baix Penedès|CAT|41.234733|1.544725|0
Torrentbò|Torrentbò||Maresme|CAT|41.596279|2.509931|0
Torres d'Alàs|Torres d'Alàs||Alt Urgell|CAT|42.357996|1.526623|0
Torres de Segre|Torres de Segre||Segrià|CAT|41.530162|0.519595|1
Torres Torres|Torres Torres||Camp de Morvedre|PV|39.731716|-0.383336|1
Torressolí Nou|Torressolí Nou||Menorca|IB|39.910813|4.075411|0
Torret|Torret||Menorca|IB|39.841375|4.257618|0
Torret de Baix|Torret de Baix||Menorca|IB|39.815529|4.256064|0
Torretes|Torretes||Marina Baixa|PV|38.514021|-0.230899|0
Torrevella|Torrevieja||Baix Segura|PV|38.000393|-0.704697|1
Torroella de Baix|Torroella de Baix||Bages|CAT|41.759032|1.89697|0
Torroella de Fluvià|Torroella de Fluvià||Alt Empordà|CAT|42.186329|3.030534|1
Torroella de Montgrí|Torroella de Montgrí||Baix Empordà|CAT|42.057268|3.160705|1
Torroja del Priorat|Torroja del Priorat||Priorat|CAT|41.220368|0.80783|1
Tortellà|Tortellà||Garrotxa|CAT|42.243474|2.620797|1
Tortosa|Tortosa||Baix Ebre|CAT|40.81965|0.532654|1
Torà|Torà||Solsonès|CAT|41.842004|1.442001|1
Toràs|Torás||Alt Palància|PV|39.928901|-0.686275|1
Torís|Turís||Ribera Alta|PV|39.373612|-0.686778|1
Toscal|Toscal||Marina Alta|PV|38.755788|0.20791|0
Toses|Toses||Ripollès|CAT|42.325862|2.035702|1
Tossa de Mar|Tossa de Mar||Selva|CAT|41.74184|2.91499|1
Tossal Gros d'en Carròs|Tossal Gros d'en Carròs||Safor|PV|38.915746|-0.144977|0
Tossal-Los Bancales|Tossal-Los Bancales||Marina Alta|PV|38.677031|0.007565|0
Tossalnou|Tossalnou||Ribera Alta|PV|39.045828|-0.468736|0
Tourist Club|Tourist Club||Selva|CAT|41.842791|2.762305|0
Tous|Tous||Ribera Alta|PV|39.187938|-0.661357|1
Toès i Entrevalls|Thuès-Entre-Valls||el Conflent|CN|42.503788|2.229833|1
Tragó|Tragó||Alt Urgell|CAT|42.061265|1.29658|0
Traiguera|Traiguera||Baix Maestrat|PV|40.54905|0.31818|1
Travesseres|Travesseres||Cerdanya|CAT|42.375013|1.688172|0
Trebalúger|Trebalúger||Menorca|IB|39.85368|4.27854|0
Tredòs|Tredòs||Aran|CAT|42.702065|0.914377|0
Tregurà de Baix|Tregurà de Baix||Ripollès|CAT|42.339178|2.281673|0
Tregurà de Dalt|Tregurà de Dalt||Ripollès|CAT|42.343751|2.287785|0
Tremp|Tremp||Pallars Jussà|CAT|42.215178|0.802245|1
Trencall|Trencall||Marina Alta|PV|38.74343|0.211981|0
Tressera|Tresserre||el Rosselló|CN|42.555917|2.837788|1
Treumal|Treumal||Baix Empordà|CAT|41.827697|3.081395|0
Treumal de Dalt|Treumal de Dalt||Baix Empordà|CAT|41.834663|3.076084|0
Trevillac|Trévillach||la Fenolleda|CN|42.70936|2.508984|1
Trillà|Trilla||la Fenolleda|CN|42.738722|2.521748|1
Tros Alt|Tros Alt||Ribera Alta|PV|39.387827|-0.54596|0
Trullars|Trouillas||el Rosselló|CN|42.606423|2.820058|1
Trullent|Trullent||Marina Alta|PV|38.786424|-0.045952|0
Tudela de Segre|Tudela de Segre||Noguera|CAT|41.870938|1.084753|0
Tuixent|Tuixent||Alt Urgell|CAT|42.231258|1.566458|0
Turballos|Turballos||Comtat|PV|38.801315|-0.430846|0
Turbiàs|Turbiàs||Alt Urgell|CAT|42.380942|1.322537|0
Turó del Mar|Turó del Mar||Maresme|CAT|41.469078|2.280448|0
Tuïr|Thuir||el Rosselló|CN|42.643305|2.758581|1
Tàngel|Tàngel|||PV|38.409853|-0.468411|0
Tàrbena|Tàrbena||Marina Baixa|PV|38.707183|-0.096578|1
Tàrrega|Tàrrega||Urgell|CAT|41.67267|1.141666|1
Térmens|Térmens||Noguera|CAT|41.719755|0.786907|1
Tírig|Tírig||Alt Maestrat|PV|40.432982|0.070509|1
Tírvia|Tírvia||Pallars Sobirà|CAT|42.518674|1.234104|1
Tòrrec|Tòrrec||Noguera|CAT|41.97959|1.073527|0
U.r.m.i.|U.r.m.i.||Baix Maestrat|PV|40.358991|0.393811|0
Ullaró|Ullaró||Mallorca|IB|39.784458|2.983845|0
Ullastre|Ullastre||Alt Empordà|CAT|42.363187|2.972848|0
Ullastrell|Ullastrell||Vallès Occidental|CAT|41.525477|1.963282|1""",
        """Ullastret|Ullastret||Baix Empordà|CAT|41.999661|3.071084|1
Ulldecona|Ulldecona||Montsià|CAT|40.617666|0.437192|1
Ulldemolins|Ulldemolins||Priorat|CAT|41.325172|0.855653|1
Ullà|Ullà||Baix Empordà|CAT|42.050916|3.103876|1
Ultramort|Ultramort||Baix Empordà|CAT|42.039747|3.037116|1
Unarre|Unarre||Pallars Sobirà|CAT|42.633608|1.148753|0
Unha|Unha||Aran|CAT|42.709978|0.901918|0
Ur|Ur||l'Alta Cerdanya|CN|42.462147|1.941449|1
Urbanització Abella|Urbanització Abella||Segrià|CAT|41.567726|0.59673|0
Urbanització Afanies i Alcormar|Urbanització Afanies i Alcormar||Plana Alta|PV|40.025764|-0.013696|0
Urbanització Aguas Perdidas|Urbanització Aguas Perdidas||Foia de Bunyol|PV|39.480071|-0.688276|0
Urbanització Alabastre|Urbanització Alabastre||Alacantí|PV|38.394447|-0.600798|0
Urbanització Aldea de les Coves|Urbanització Aldea de les Coves||Marina Alta|PV|38.785492|-0.013886|0
Urbanització Alhama|Urbanització Alhama||Marina Baixa|PV|38.639544|-0.048096|0
Urbanització Altamar|Urbanització Altamar||Foia de Bunyol|PV|39.479998|-0.600096|0
Urbanització Ausiàs March|Urbanització Ausiàs March||Ribera Alta|PV|39.253625|-0.495691|0
Urbanització Badia Park|Urbanització Badia Park||Ribera Baixa|PV|39.172905|-0.243537|0
Urbanització Balcó al Mar|Urbanització Balcó al Mar||Marina Alta|PV|38.739624|0.223786|0
Urbanització Bell Horitzó|Urbanització Bell Horitzó||Marina Baixa|PV|38.603599|-0.088841|0
Urbanització Bell-Lloc II|Urbanització Bell-Lloc II||Baix Empordà|CAT|41.838698|2.976205|0
Urbanització Belmonte-L'Oasi|Urbanització Belmonte-L'Oasi||Marina Baixa|PV|38.594907|-0.116293|0
Urbanització Benadressa|Urbanització Benadressa||Plana Alta|PV|40.030128|-0.01411|0
Urbanització Bon Relax|Urbanització Bon Relax||Alt Empordà|CAT|42.182416|3.092194|0
Urbanització Bonaire|Urbanització Bonaire||Alcoià|PV|38.506888|-0.59963|0
Urbanització Bonanza|Urbanització Bonanza||Camp de Túria|PV|39.615197|-0.392499|0
Urbanització Bonança|Urbanització Bonança||Alacantí|PV|38.425331|-0.422296|0
Urbanització Buenos Aires|Urbanització Buenos Aires||Baix Vinalopó|PV|38.304976|-0.638903|0
Urbanització Cabeza Redondo|Urbanització Cabeza Redondo||Foia de Bunyol|PV|39.490811|-0.616232|0
Urbanització Cabeç Bort|Urbanització Cabeç Bort||Camp de Túria|PV|39.628668|-0.388757|0
Urbanització Cabiscol|Urbanització Cabiscol||Alacantí|PV|38.390013|-0.423559|0
Urbanització Cala Advocat-Bonavista|Urbanització Cala Advocat-Bonavista||Marina Alta|PV|38.678166|0.00911|0
Urbanització Cala del Rei|Urbanització Cala del Rei||Baix Empordà|CAT|41.975201|3.207332|0
Urbanització Cala Llenya|Urbanització Cala Llenya||Eivissa|IB|39.017092|1.58851|0
Urbanització Cala Llonga|Urbanització Cala Llonga||Eivissa|IB|38.95524|1.518301|0
Urbanització Cala Tarida|Urbanització Cala Tarida||Eivissa|IB|38.940304|1.243678|0
Urbanització Calders|Urbanització Calders||Alt Camp|CAT|41.271727|1.26813|0
Urbanització Camp de l'Hort|Urbanització Camp de l'Hort||Baix Empordà|CAT|41.854283|3.104877|0
Urbanització Campolivar|Urbanització Campolivar||Horta Nord|PV|39.529995|-0.426566|0
Urbanització Can Bou|Urbanització Can Bou||Anoia|CAT|41.485974|1.764267|0
Urbanització Can Fornet|Urbanització Can Fornet||Eivissa|IB|38.94841|1.458691|0
Urbanització Can Jordi|Urbanització Can Jordi||Selva|CAT|41.904436|2.78516|0
Urbanització Can Mitjans|Urbanització Can Mitjans||Alt Penedès|CAT|41.342459|1.8138|0
Urbanització Can Pep Simó|Urbanització Can Pep Simó||Eivissa|IB|38.91682|1.468984|0
Urbanització Can Reixac|Urbanització Can Reixac||Baix Empordà|CAT|41.81278|2.964283|0
Urbanització Can Rossell|Urbanització Can Rossell||Alt Penedès|CAT|41.401255|1.797373|0
Urbanització Can Sendro|Urbanització Can Sendro||Alt Penedès|CAT|41.429685|1.491521|0
Urbanització Can Solà Gros I|Urbanització Can Solà Gros I||Selva|CAT|41.85404|2.804102|0
Urbanització Can Solà Gros II|Urbanització Can Solà Gros II||Selva|CAT|41.854068|2.808674|0
Urbanització Cantallops|Urbanització Cantallops||Baix Empordà|CAT|41.925322|3.183666|0
Urbanització Canyapar|Urbanització Canyapar||Ribera Alta|PV|39.394109|-0.672441|0
Urbanització Cap Rubi|Urbanització Cap Rubi||Baix Empordà|CAT|41.940751|3.216982|0
Urbanització Casa de Camp|Urbanització Casa de Camp||Baix Empordà|CAT|41.925759|3.189295|0
Urbanització Casa Roja|Urbanització Casa Roja||Baix Penedès|CAT|41.276958|1.564843|0
Urbanització Castell de Montornés|Urbanització Castell de Montornés||Tarragonès|CAT|41.192915|1.406885|0
Urbanització Cims de Calicanto|Urbanització Cims de Calicanto||Foia de Bunyol|PV|39.432539|-0.616251|0
Urbanització Cims de Calicanto|Urbanització Cims de Calicanto||Horta Sud|PV|39.429586|-0.596467|0
Urbanització Cims de Sant Antoni|Urbanització Cims de Sant Antoni||Camp de Túria|PV|39.56527|-0.4802|0
Urbanització Clot del Basso i Mota de Sant Pere|Urbanització Clot del Basso i Mota de Sant Pere||Garraf|CAT|41.199928|1.66574|0
Urbanització Coblanca|Urbanització Coblanca||Marina Baixa|PV|38.5835|-0.164676|0
Urbanització Collao dels Llops|Urbanització Collao dels Llops||Camp de Túria|PV|39.648451|-0.442731|0
Urbanització Corral Nou|Urbanització Corral Nou||Camp de Túria|PV|39.649761|-0.438801|0
Urbanització Costa dels Pins|Urbanització Costa dels Pins||Plana Alta|PV|40.031395|-0.013093|0
Urbanització Creu d'en Rallo|Urbanització Creu d'en Rallo||Baix Empordà|CAT|41.952824|3.217953|0
Urbanització Creu del Maigmó|Urbanització Creu del Maigmó||Alcoià|PV|38.509945|-0.603777|0
Urbanització Cullera Park|Urbanització Cullera Park||Ribera Baixa|PV|39.18034|-0.239631|0
Urbanització de l'Alt|Urbanització de l'Alt||Ribera Alta|PV|39.383316|-0.554666|0
Urbanització de la Vall del Sol|Urbanització de la Vall del Sol||Marina Alta|PV|38.751548|0.18127|0
Urbanització de Roderes|Urbanització de Roderes||Segrià|CAT|41.700178|0.615291|0
Urbanització Eixample-Llevant|Urbanització Eixample-Llevant||Tarragonès|CAT|41.177451|1.417817|0
Urbanització El Balcó de Montroi|Urbanització El Balcó de Montroi||Ribera Alta|PV|39.317421|-0.669709|0
Urbanització El Baradello-El Sergent|Urbanització El Baradello-El Sergent||Alcoià|PV|38.706706|-0.523489|0
Urbanització El Boter|Urbanització El Boter||Alacantí|PV|38.457317|-0.506391|0
Urbanització El Carril|Urbanització El Carril||Alacantí|PV|38.480494|-0.422441|0
Urbanització el Cassarell|Urbanització el Cassarell||Maresme|CAT|41.608821|2.623292|0
Urbanització El Cim del Sol|Urbanització El Cim del Sol||Marina Alta|PV|38.715956|0.166708|0
Urbanització el Ciscar|Urbanització el Ciscar||Camp de Túria|PV|39.566316|-0.422707|0
Urbanització el Corralet|Urbanització el Corralet||Camp de Túria|PV|39.626874|-0.397396|0
Urbanització El Flare|Urbanització El Flare||Ribera Alta|PV|39.373702|-0.608905|0
Urbanització El Franc i Espinós|Urbanització El Franc i Espinós||Alacantí|PV|38.388106|-0.429228|0
Urbanització El Garroferal|Urbanització El Garroferal||Alacantí|PV|38.42334|-0.569504|0
Urbanització El Geperut|Urbanització El Geperut||Alacantí|PV|38.335438|-0.603264|0
Urbanització El Gran Alacant|Urbanització El Gran Alacant||Baix Vinalopó|PV|38.229115|-0.524103|0
Urbanització El Maigmó|Urbanització El Maigmó||Alcoià|PV|38.501661|-0.598146|0
Urbanització El Molinet|Urbanització El Molinet||Camp de Túria|PV|39.555474|-0.550393|0
Urbanització El Molló|Urbanització El Molló||Plana Alta|PV|40.015789|-0.090331|0
Urbanització El Montcaio|Urbanització El Montcaio||Baix Segura|PV|38.056772|-0.658632|0
Urbanització El Morral|Urbanització El Morral||Plana Baixa|PV|40.010972|-0.172861|0
Urbanització El Pedregal|Urbanització El Pedregal||Horta Sud|PV|39.364667|-0.493815|0
Urbanització el Pilar|Urbanització el Pilar||Vall d'Albaida|PV|38.829085|-0.636333|0
Urbanització El Pinar|Urbanització El Pinar||Horta Sud|PV|39.344477|-0.524072|0
Urbanització El Pinar de la Rambla|Urbanització El Pinar de la Rambla||Plana Baixa|PV|40.012853|-0.16152|0
Urbanització el Pla de l'Estepar|Urbanització el Pla de l'Estepar||Camp de Túria|PV|39.62316|-0.42388|0
Urbanització El Pla de les Clotxes|Urbanització El Pla de les Clotxes||Ribera Alta|PV|39.299994|-0.481864|0
Urbanització El Planet|Urbanització El Planet||Marina Baixa|PV|38.589661|-0.070778|0
Urbanització El Poble Llevantí|Urbanització El Poble Llevantí||Baix Vinalopó|PV|38.20798|-0.566816|0
Urbanització El Portell|Urbanització El Portell||Alacantí|PV|38.457184|-0.488834|0
Urbanització El Quadro de Santiago|Urbanització El Quadro de Santiago||Plana Alta|PV|40.02828|0.04082|0
Urbanització el Racó|Urbanització el Racó||Ribera Alta|PV|39.148629|-0.409495|0
Urbanització El Rafalet|Urbanització El Rafalet||Marina Alta|PV|38.765223|0.152298|0
Urbanització El Sitjar de Baix|Urbanització El Sitjar de Baix||Plana Baixa|PV|39.989897|-0.172939|0
Urbanització el Tomellar|Urbanització el Tomellar||Plana Alta|PV|40.029936|-0.01453|0
Urbanització El Tossal|Urbanització El Tossal||Marina Baixa|PV|38.614176|-0.107961|0
Urbanització El Totxar|Urbanització El Totxar||Camp de Morvedre|PV|39.729245|-0.39922|0
Urbanització El Vedat|Urbanització El Vedat||Ribera Alta|PV|39.133211|-0.561275|0
Urbanització El Ventós|Urbanització El Ventós||Alacantí|PV|38.45784|-0.625907|0
Urbanització els Amics|Urbanització els Amics||Ribera Alta|PV|39.080456|-0.422499|0
Urbanització els Arcs|Urbanització els Arcs||Alt Camp|CAT|41.273249|1.304459|0
Urbanització Els Arenals del Sol|Urbanització Els Arenals del Sol||Baix Vinalopó|PV|38.250341|-0.520437|0
Urbanització Els Calistros i l'Assagador|Urbanització Els Calistros i l'Assagador||Marina Alta|PV|38.732642|0.156276|0
Urbanització Els Carrions|Urbanització Els Carrions||Marina Alta|PV|38.652068|0.062109|0
Urbanització Els Castellons|Urbanització Els Castellons||Marina Alta|PV|38.731731|0.115603|0
Urbanització Els Estanys|Urbanització Els Estanys||Baix Segura|PV|38.089217|-0.667112|0
Urbanització Els Llimoners|Urbanització Els Llimoners||Baix Vinalopó|PV|38.257098|-0.552805|0
Urbanització Els Mitjans|Urbanització Els Mitjans||Marina Alta|PV|38.803117|0.086646|0
Urbanització Els Pilans|Urbanització Els Pilans||Baix Ebre|CAT|40.828119|0.458203|0
Urbanització els Trencalls|Urbanització els Trencalls||Camp de Túria|PV|39.65956|-0.431825|0
Urbanització els Vilars|Urbanització els Vilars||Baix Empordà|CAT|41.847854|3.094665|0
Urbanització es Castellet|Urbanització es Castellet||Baix Empordà|CAT|41.940433|3.207875|0
Urbanització es Valls|Urbanització es Valls||Baix Empordà|CAT|41.963112|3.202164|0
Urbanització Escandinàvia|Urbanització Escandinàvia||Marina Baixa|PV|38.588496|-0.112304|0
Urbanització Falda de Montí|Urbanització Falda de Montí||Plana Baixa|PV|39.94766|-0.301866|0
Urbanització Flor de Almendro|Urbanització Flor de Almendro||Tarragonès|CAT|41.18831|1.400329|0
Urbanització Foia dels Patos|Urbanització Foia dels Patos||Alacantí|PV|38.466202|-0.437035|0
Urbanització Font de l'Almaguer|Urbanització Font de l'Almaguer||Ribera Alta|PV|39.29009|-0.490461|0
Urbanització Font de la Salut|Urbanització Font de la Salut||Baix Empordà|CAT|41.946883|3.216778|0
Urbanització Font del Bosc|Urbanització Font del Bosc||Alt Penedès|CAT|41.497537|1.654575|0
Urbanització Font Nova|Urbanització Font Nova||Baix Maestrat|PV|40.334464|0.367083|0
Urbanització Forn Romà de Treumal|Urbanització Forn Romà de Treumal||Baix Empordà|CAT|41.83235|3.086571|0
Urbanització Garraf II|Urbanització Garraf II||Garraf|CAT|41.268295|1.93666|0
Urbanització Golf Badia|Urbanització Golf Badia||Marina Baixa|PV|38.551995|-0.18063|0
Urbanització Golf Costa Brava|Urbanització Golf Costa Brava||Baix Empordà|CAT|41.805865|2.981662|0
Urbanització Golf Peralada|Urbanització Golf Peralada||Alt Empordà|CAT|42.31298|3.020886|0
Urbanització Gormaig|Urbanització Gormaig||Comtat|PV|38.721223|-0.460162|0
Urbanització Hort de la Rabassa|Urbanització Hort de la Rabassa||Ribera Alta|PV|39.377047|-0.589371|0
Urbanització Hèlios|Urbanització Hèlios||Vall d'Albaida|PV|38.811988|-0.647702|0
Urbanització Istobal|Urbanització Istobal||Ribera Alta|PV|39.201403|-0.497999|0
Urbanització Jardins Palau|Urbanització Jardins Palau||Baix Empordà|CAT|41.855051|3.113158|0
Urbanització L'Alcassar|Urbanització L'Alcassar||Marina Alta|PV|38.708663|0.142191|0
Urbanització L'Aljub|Urbanització L'Aljub||Alcoià|PV|38.493311|-0.623623|0
Urbanització L'Almèdia|Urbanització L'Almèdia||Marina Baixa|PV|38.661656|-0.122037|0
Urbanització L'Atzúbia Pinos|Urbanització L'Atzúbia Pinos||Marina Alta|PV|38.851492|-0.153012|0
Urbanització L'Ermita|Urbanització L'Ermita||Marina Alta|PV|38.792147|0.126861|0
Urbanització L'Ermita Vella|Urbanització L'Ermita Vella||Baix Vinalopó|PV|38.243304|-0.584331|0
Urbanització L'Estepar|Urbanització L'Estepar||Alcoià|PV|38.653831|-0.452558|0
Urbanització la Borna|Urbanització la Borna||Baix Empordà|CAT|41.95424|3.22713|0
Urbanització La Buitrera|Urbanització La Buitrera||Camp de Túria|PV|39.60575|-0.610288|0
Urbanització La Canyada|Urbanització La Canyada||Horta Nord|PV|39.525355|-0.488275|0
Urbanització la Carrasca|Urbanització la Carrasca||Camp de Túria|PV|39.665788|-0.429508|0
Urbanització La Colònia|Urbanització La Colònia||Baix Vinalopó|PV|38.208655|-0.583149|0
Urbanització la Coma|Urbanització la Coma||Baix Empordà|CAT|41.96637|3.211401|0
Urbanització La Cometa|Urbanització La Cometa||Marina Alta|PV|38.733486|0.045728|0
Urbanització La Cometa|Urbanització La Cometa||Marina Alta|PV|38.68908|0.124863|0
Urbanització La Corona|Urbanització La Corona||Marina Alta|PV|38.797532|0.176971|0
Urbanització La Corona|Urbanització La Corona||Safor|PV|38.898612|-0.221395|0
Urbanització La Creu de Gràcia|Urbanització La Creu de Gràcia||Horta Nord|PV|39.54299|-0.44178|0
Urbanització La Creu de Gràcia|Urbanització La Creu de Gràcia||Horta Nord|PV|39.533972|-0.441427|0
Urbanització La Finca|Urbanització La Finca||Marina Alta|PV|38.760534|0.178735|0
Urbanització La Foia Blanca|Urbanització La Foia Blanca||Marina Baixa|PV|38.592621|-0.086111|0
Urbanització la Font|Urbanització la Font||Mallorca|IB|39.892283|3.017362|0
Urbanització la Font de l'Oro|Urbanització la Font de l'Oro||Camp de Túria|PV|39.657375|-0.401673|0
Urbanització La Fustera-Els Carrions|Urbanització La Fustera-Els Carrions||Marina Alta|PV|38.669119|0.008438|0
Urbanització La Galera Alta|Urbanització La Galera Alta||Marina Baixa|PV|38.633381|-0.022221|0
Urbanització La Galera Baixa|Urbanització La Galera Baixa||Marina Baixa|PV|38.629928|-0.013263|0
Urbanització La Lloma|Urbanització La Lloma||Camp de Túria|PV|39.672334|-0.531595|0
Urbanització La Lloma del Calderer|Urbanització La Lloma del Calderer||Camp de Túria|PV|39.567754|-0.429127|0
Urbanització La Lloma i els Mallols|Urbanització La Lloma i els Mallols||Plana Alta|PV|40.020484|-0.118619|0
Urbanització la Lloma-Horta Nova|Urbanització la Lloma-Horta Nova||Camp de Túria|PV|39.64783|-0.429677|0
Urbanització la Loma|Urbanització la Loma||Camp de Túria|PV|39.64656|-0.427329|0
Urbanització La Masia|Urbanització La Masia||Camp de Túria|PV|39.612167|-0.461844|0
Urbanització La Muntanyosa|Urbanització La Muntanyosa||Vinalopó Mitjà|PV|38.293715|-0.890223|0
Urbanització La Pisnella|Urbanització La Pisnella||Alacantí|PV|38.472835|-0.429413|0
Urbanització La Plana|Urbanització La Plana||Marina Alta|PV|38.782733|-0.080342|0
Urbanització la Planella|Urbanització la Planella||Alt Camp|CAT|41.278922|1.301261|0
Urbanització la Planeta|Urbanització la Planeta||Alt Camp|CAT|41.265078|1.369826|0
Urbanització la Riera|Urbanització la Riera||Osona|CAT|42.072194|2.303202|0
Urbanització la Rimbalda|Urbanització la Rimbalda||Alt Penedès|CAT|41.427327|1.479795|0
Urbanització La Sima|Urbanització La Sima||Camp de Túria|PV|39.597509|-0.515958|0
Urbanització la Sínia|Urbanització la Sínia||Alt Camp|CAT|41.274196|1.298351|0
Urbanització La Torre Colomera|Urbanització La Torre Colomera||Plana Alta|PV|40.061561|0.108424|0
Urbanització La Torre de Bellver|Urbanització La Torre de Bellver||Plana Alta|PV|40.061152|0.100946|0
Urbanització La Vall d'Umbrí|Urbanització La Vall d'Umbrí||Plana Alta|PV|40.029775|-0.083752|0
Urbanització La Vall del Sol|Urbanització La Vall del Sol||Alacantí|PV|38.459084|-0.512481|0
Urbanització la Xarnada|Urbanització la Xarnada||Baix Empordà|CAT|41.958834|3.208994|0
Urbanització Les Aigües|Urbanització Les Aigües||Vall d'Albaida|PV|38.819669|-0.621686|0
Urbanització Les Blasques|Urbanització Les Blasques||Ribera Alta|PV|39.386267|-0.647656|0
Urbanització Les Boveres|Urbanització Les Boveres||Alt Camp|CAT|41.286262|1.282025|0
Urbanització Les Cometes|Urbanització Les Cometes||Marina Alta|PV|38.661958|0.071018|0
Urbanització Les Fonts|Urbanització Les Fonts||Marina Alta|PV|38.741307|0.13399|0
Urbanització Les Fonts|Urbanització Les Fonts||Baix Maestrat|PV|40.254358|0.284502|0
Urbanització Les Llomes|Urbanització Les Llomes||Alacantí|PV|38.413947|-0.610284|0
Urbanització les Llomes|Urbanització les Llomes||Camp de Túria|PV|39.632956|-0.415542|0
Urbanització Les Planes|Urbanització Les Planes||Baix Camp|CAT|41.045811|0.954531|0
Urbanització Les Platgetes|Urbanització Les Platgetes||Plana Alta|PV|40.05969|0.09378|0
Urbanització Les Talaies|Urbanització Les Talaies||Baix Maestrat|PV|40.365113|0.387085|0
Urbanització Les Teules II|Urbanització Les Teules II||Baix Empordà|CAT|41.823826|3.012241|0
Urbanització Les Tres Platges|Urbanització Les Tres Platges||Baix Maestrat|PV|40.228117|0.267094|0
Urbanització Llanorel|Urbanització Llanorel||Foia de Bunyol|PV|39.329369|-0.821282|0
Urbanització Lloma Badada|Urbanització Lloma Badada||Vinalopó Mitjà|PV|38.450904|-0.764073|0
Urbanització Lloma de Jarque|Urbanització Lloma de Jarque||Camp de Túria|PV|39.654135|-0.565183|0
Urbanització Lloma Llarga|Urbanització Lloma Llarga||Camp de Túria|PV|39.610167|-0.409238|0
Urbanització Lo Vincle|Urbanització Lo Vincle||Baix Vinalopó|PV|38.259285|-0.594068|0
Urbanització los Olivos|Urbanització los Olivos||Baix Empordà|CAT|41.943736|3.210459|0
Urbanització los Picapiedra|Urbanització los Picapiedra||Camp de Túria|PV|39.665419|-0.532793|0
Urbanització Mallorquines|Urbanització Mallorquines||Selva|CAT|41.81567|2.73799|0
Urbanització Mar Blau|Urbanització Mar Blau||Marina Alta|PV|38.750414|0.221131|0
Urbanització Mas de Traver|Urbanització Mas de Traver||Camp de Túria|PV|39.542521|-0.546702|0
Urbanització Mas Falet del Migdia|Urbanització Mas Falet del Migdia||Baix Empordà|CAT|41.852783|3.106981|0
Urbanització Mas Gispert|Urbanització Mas Gispert||Baix Empordà|CAT|41.959184|3.20559|0
Urbanització Mas Mato|Urbanització Mas Mato||Baix Empordà|CAT|41.968391|3.205961|0
Urbanització Mas Patxot|Urbanització Mas Patxot||Baix Empordà|CAT|41.824804|2.948638|0
Urbanització Mas Pere|Urbanització Mas Pere||Baix Empordà|CAT|41.876222|3.069465|0
Urbanització Mas Prats|Urbanització Mas Prats||Baix Empordà|CAT|41.955831|3.217854|0
Urbanització Mas Sopes|Urbanització Mas Sopes||Alt Empordà|CAT|42.182922|3.106079|0
Urbanització Mas Trader II|Urbanització Mas Trader II||Garraf|CAT|41.233414|1.653827|0
Urbanització Maset del Pou|Urbanització Maset del Pou||Ribera Alta|PV|39.359568|-0.576325|0
Urbanització Masia de Gaeta|Urbanització Masia de Gaeta||Plana Alta|PV|40.013047|-0.119764|0
Urbanització Medicàlia|Urbanització Medicàlia||Horta Nord|PV|39.564772|-0.285391|0
Urbanització Molí Blanquillo|Urbanització Molí Blanquillo||Baix Penedès|CAT|41.241671|1.530244|0
Urbanització Mont Tesor|Urbanització Mont Tesor||Ribera Alta|PV|39.395007|-0.601062|0
Urbanització Mont-ros|Urbanització Mont-ros||Camp de Túria|PV|39.63677|-0.433564|0
Urbanització Montaragó|Urbanització Montaragó||Camp de Túria|PV|39.648345|-0.646628|0
Urbanització Montblau|Urbanització Montblau||Camp de Morvedre|PV|39.733113|-0.356916|0
Urbanització Montcati|Urbanització Montcati||Camp de Túria|PV|39.649324|-0.656438|0
Urbanització Monte Rosado|Urbanització Monte Rosado||Ribera Alta|PV|39.378994|-0.579471|0
Urbanització Monteamor|Urbanització Monteamor||Camp de Túria|PV|39.669927|-0.431732|0
Urbanització Monterrei|Urbanització Monterrei||Safor|PV|38.945881|-0.221202|0
Urbanització Monterrei|Urbanització Monterrei||Safor|PV|38.948532|-0.219761|0
Urbanització Montes de Palancia|Urbanització Montes de Palancia||Camp de Morvedre|PV|39.777527|-0.381592|0
Urbanització Montfarrutx|Urbanització Montfarrutx||Mallorca|IB|39.733826|3.27399|0
Urbanització Montgó-Toscamar|Urbanització Montgó-Toscamar||Marina Alta|PV|38.797961|0.112215|0
Urbanització Montmar-La Vinya|Urbanització Montmar-La Vinya||Marina Alta|PV|38.690684|0.00916|0
Urbanització Montpego|Urbanització Montpego||Marina Alta|PV|38.841452|-0.047832|0
Urbanització Montpicaio|Urbanització Montpicaio||Camp de Morvedre|PV|39.637878|-0.313721|0
Urbanització Montsol|Urbanització Montsol||Alcoià|PV|38.688665|-0.460955|0
Urbanització Montsol|Urbanització Montsol||Marina Baixa|PV|38.524715|-0.227943|0
Urbanització Montsol|Urbanització Montsol||Safor|PV|38.98159|-0.265216|0
Urbanització Morro de Toix-Mar i Vila|Urbanització Morro de Toix-Mar i Vila||Marina Alta|PV|38.640539|0.018462|0
Urbanització Muntanya de Cristina|Urbanització Muntanya de Cristina||Plana Alta|PV|40.031379|-0.014753|0
Urbanització Muntanya de Pedreguer|Urbanització Muntanya de Pedreguer||Marina Alta|PV|38.78011|0.051462|0
Urbanització Muntanya del Xúquer|Urbanització Muntanya del Xúquer||Ribera Alta|PV|39.099427|-0.539894|0
Urbanització Mutxavista|Urbanització Mutxavista||Alacantí|PV|38.408943|-0.405429|0
Urbanització Paigi|Urbanització Paigi||Marina Alta|PV|38.703077|0.141931|0
Urbanització Paraíso|Urbanització Paraíso||Camp de Túria|PV|39.650347|-0.397272|0
Urbanització Parc de Llevant|Urbanització Parc de Llevant||Tarragonès|CAT|41.155382|1.317957|0
Urbanització Pedralbilla|Urbanització Pedralbilla||Camp de Túria|PV|39.653931|-0.518311|0
Urbanització Pedralta|Urbanització Pedralta||Baix Empordà|CAT|41.804497|3.006447|0
Urbanització Penya de les Àguiles|Urbanització Penya de les Àguiles||Baix Vinalopó|PV|38.266172|-0.736538|0
Urbanització Penya Llisa|Urbanització Penya Llisa||Camp de Morvedre|PV|39.726232|-0.365501|0
Urbanització Pescadors|Urbanització Pescadors||Segrià|CAT|41.487144|0.514019|0
Urbanització Pinares|Urbanització Pinares||Alt Camp|CAT|41.248675|1.266729|0""",
        """Urbanització Pla d'Aguiló|Urbanització Pla d'Aguiló||Camp de Morvedre|PV|39.664435|-0.339912|0
Urbanització Pla de la Lloma|Urbanització Pla de la Lloma||Alacantí|PV|38.492867|-0.426069|0
Urbanització Pla de Marco-Les Forquetes|Urbanització Pla de Marco-Les Forquetes||Camp de Túria|PV|39.692416|-0.548929|0
Urbanització Platja d'Oliva|Urbanització Platja d'Oliva||Safor|PV|38.927906|-0.094271|0
Urbanització Platja de Bellreguard|Urbanització Platja de Bellreguard||Safor|PV|38.96489|-0.132571|0
Urbanització Platja de Guardamar|Urbanització Platja de Guardamar||Safor|PV|38.968503|-0.135887|0
Urbanització Platja de Miramar|Urbanització Platja de Miramar||Safor|PV|38.955634|-0.12256|0
Urbanització Platja de Piles|Urbanització Platja de Piles||Safor|PV|38.950087|-0.116104|0
Urbanització Platja Fonda|Urbanització Platja Fonda||Baix Empordà|CAT|41.941862|3.214908|0
Urbanització Poblamar|Urbanització Poblamar||Tarragonès|CAT|41.170232|1.414894|0
Urbanització Poble de l'Alcassar|Urbanització Poble de l'Alcassar||Marina Alta|PV|38.719385|0.145838|0
Urbanització Port Des Pi|Urbanització Port Des Pi||Baix Empordà|CAT|41.972216|3.21527|0
Urbanització Port Saplatja|Urbanització Port Saplatja||Horta Nord|PV|39.509942|-0.322521|0
Urbanització Puig del Montcal|Urbanització Puig del Montcal||Baix Empordà|CAT|41.935891|3.204744|0
Urbanització Puig Gros|Urbanització Puig Gros||Ribera Alta|PV|39.07284|-0.436264|0
Urbanització Puignou|Urbanització Puignou||Alt Camp|CAT|41.275687|1.301774|0
Urbanització Puigsesforques|Urbanització Puigsesforques||Baix Empordà|CAT|41.855688|3.109422|0
Urbanització Rambutxar Nord|Urbanització Rambutxar Nord||Alacantí|PV|38.455639|-0.569747|0
Urbanització Rambutxar Sud|Urbanització Rambutxar Sud||Alacantí|PV|38.445097|-0.562019|0
Urbanització Rec de Sa Tuna|Urbanització Rec de Sa Tuna||Baix Empordà|CAT|41.958417|3.222859|0
Urbanització Residencial Begur|Urbanització Residencial Begur||Baix Empordà|CAT|41.945927|3.199665|0
Urbanització Residencial la Riera|Urbanització Residencial la Riera||Tarragonès|CAT|41.16965|1.364576|0
Urbanització Riu de Oro|Urbanització Riu de Oro||Baix Empordà|CAT|41.876881|3.060489|0
Urbanització Roca Llisa|Urbanització Roca Llisa||Eivissa|IB|38.939233|1.494967|0
Urbanització Romanyà de la Selva|Urbanització Romanyà de la Selva||Baix Empordà|CAT|41.850521|2.985339|0
Urbanització S'Antiga|Urbanització S'Antiga||Baix Empordà|CAT|41.97282|3.220646|0
Urbanització sa Fontansa|Urbanització sa Fontansa||Baix Empordà|CAT|41.955455|3.201768|0
Urbanització sa Nau Perduda|Urbanització sa Nau Perduda||Baix Empordà|CAT|41.968886|3.229438|0
Urbanització Sa Punta|Urbanització Sa Punta||Baix Empordà|CAT|41.980666|3.206179|0
Urbanització San Miguel|Urbanització San Miguel||Camp de Túria|PV|39.648786|-0.410781|0
Urbanització Sant Blai|Urbanització Sant Blai||Ribera Alta|PV|39.114072|-0.425472|0
Urbanització Sant Cristòfol|Urbanització Sant Cristòfol||Ribera Alta|PV|39.118675|-0.564471|0
Urbanització Sant Daniel|Urbanització Sant Daniel||Baix Empordà|CAT|41.859027|3.101062|0
Urbanització Sant Jaume|Urbanització Sant Jaume||Marina Alta|PV|38.686584|0.010351|0
Urbanització Sant Miquel|Urbanització Sant Miquel||Camp de Túria|PV|39.638272|-0.631355|0
Urbanització Sant Pere|Urbanització Sant Pere||Mallorca|IB|39.732676|3.277629|0
Urbanització Sant Rafael|Urbanització Sant Rafael||Marina Baixa|PV|38.585167|-0.115866|0
Urbanització Sant Roc|Urbanització Sant Roc||Camp de Morvedre|PV|39.742066|-0.354901|0
Urbanització Santa Anna|Urbanització Santa Anna||Horta Sud|PV|39.39707|-0.434593|0
Urbanització Santa Barbara|Urbanització Santa Barbara||Serrans|PV|39.659542|-0.890656|0
Urbanització Santa Barbara|Urbanització Santa Barbara||València|PV|39.549184|-0.418429|0
Urbanització Santa Clara|Urbanització Santa Clara||Marina Baixa|PV|38.638521|-0.068154|0
Urbanització Santa Clara|Urbanització Santa Clara||Marina Baixa|PV|38.64311|-0.070626|0
Urbanització Santa Marina|Urbanització Santa Marina||Ribera Alta|PV|39.09425|-0.364918|0
Urbanització Santa Mònica|Urbanització Santa Mònica||Camp de Túria|PV|39.558101|-0.553606|0
Urbanització Sector Industrial|Urbanització Sector Industrial||Selva|CAT|41.732487|2.608941|0
Urbanització Sector Sud d'Aiguablava|Urbanització Sector Sud d'Aiguablava||Baix Empordà|CAT|41.926141|3.208051|0
Urbanització Serra d'Altea|Urbanització Serra d'Altea||Marina Baixa|PV|38.635554|-0.009883|0
Urbanització Serra i Mar|Urbanització Serra i Mar||Horta Sud|PV|39.308121|-0.482732|0
Urbanització Serra Marina|Urbanització Serra Marina||Alacantí|PV|38.508276|-0.362363|0
Urbanització ses Costes|Urbanització ses Costes||Baix Empordà|CAT|41.946219|3.211783|0
Urbanització ses Coves Noves|Urbanització ses Coves Noves||Menorca|IB|40.013757|4.183741|0
Urbanització ses Falugues|Urbanització ses Falugues||Baix Empordà|CAT|41.935234|3.208231|0
Urbanització Siesta|Urbanització Siesta||Eivissa|IB|38.973856|1.526506|0
Urbanització Solana Vall del Paradís|Urbanització Solana Vall del Paradís||Marina Alta|PV|38.754124|-0.014619|0
Urbanització Solemio|Urbanització Solemio||Baix Camp|CAT|41.025654|0.952771|0
Urbanització Talaia Mediterrània|Urbanització Talaia Mediterrània||Baix Penedès|CAT|41.339182|1.543971|0
Urbanització Tancat de l'Alter|Urbanització Tancat de l'Alter||Horta Sud|PV|39.370629|-0.479987|0
Urbanització Tartera|Urbanització Tartera||Cerdanya|CAT|42.365923|1.857327|0
Urbanització Tir de Colom|Urbanització Tir de Colom||Ribera Alta|PV|39.118408|-0.427645|0
Urbanització Torre Blava|Urbanització Torre Blava||Baix Vinalopó|PV|38.275669|-0.61577|0
Urbanització Torre d'en Conill|Urbanització Torre d'en Conill||Camp de Túria|PV|39.570134|-0.467368|0
Urbanització Torre Gran-Torre Vella|Urbanització Torre Gran-Torre Vella||Baix Empordà|CAT|42.05437|3.164622|0
Urbanització Torre Moratxa|Urbanització Torre Moratxa||Baix Empordà|CAT|42.054907|3.192958|0
Urbanització Torre Serra|Urbanització Torre Serra||Noguera|CAT|41.822296|0.814484|0
Urbanització Torre Valentina|Urbanització Torre Valentina||Baix Empordà|CAT|41.838886|3.092763|0
Urbanització Tossal|Urbanització Tossal||Segrià|CAT|41.490692|0.517312|0
Urbanització Tossal del Mar|Urbanització Tossal del Mar||Baix Maestrat|PV|40.355275|0.396395|0
Urbanització Tossal del Sol|Urbanització Tossal del Sol||Marina Alta|PV|38.664152|0.045466|0
Urbanització Tres Rutas|Urbanització Tres Rutas||Camp de Túria|PV|39.608317|-0.399431|0
Urbanització Vall de Flors|Urbanització Vall de Flors||Camp de Túria|PV|39.575123|-0.410745|0
Urbanització Vall del Túria|Urbanització Vall del Túria||Camp de Túria|PV|39.594028|-0.604566|0
Urbanització Vall Repòs|Urbanització Vall Repòs||Baix Empordà|CAT|41.862188|3.002|0
Urbanització Vall-Llobrega|Urbanització Vall-Llobrega||Baix Empordà|CAT|41.889286|3.123856|0
Urbanització Venta de Lanussa|Urbanització Venta de Lanussa||Alacantí|PV|38.479192|-0.311839|0
Urbanització Vescomtat de Cabanyes|Urbanització Vescomtat de Cabanyes||Baix Empordà|CAT|41.854511|3.056826|0
Urbanització Vil·la de la Serra|Urbanització Vil·la de la Serra||Alacantí|PV|38.430262|-0.532118|0
Urbanització Vinya Ceguet|Urbanització Vinya Ceguet||Alt Penedès|CAT|41.454751|1.872543|0
Urbanització Vinyamalata|Urbanització Vinyamalata||Ribera Alta|PV|39.409054|-0.724728|0
Urbanización Alenda Golf|Urbanización Alenda Golf||Vinalopó Mitjà|PV|38.359285|-0.671819|0
Urbanización Alto Ideal|Urbanización Alto Ideal||Alt Vinalopó|PV|38.513979|-0.883381|0
Urbanización Atalaya de Levante|Urbanización Atalaya de Levante||Foia de Bunyol|PV|39.476577|-0.612638|0
Urbanización Atalaya Park|Urbanización Atalaya Park||Baix Segura|PV|38.068448|-0.731259|0
Urbanización Bella Orxeta|Urbanización Bella Orxeta||Marina Baixa|PV|38.552437|-0.261113|0
Urbanización Benfis-Park|Urbanización Benfis-Park||Baix Segura|PV|38.147752|-0.965972|0
Urbanización Benfisol|Urbanización Benfisol||Baix Segura|PV|38.147489|-0.961303|0
Urbanización Benimar|Urbanización Benimar||Baix Segura|PV|38.080277|-0.731897|0
Urbanización Blue Lagoon|Urbanización Blue Lagoon||Baix Segura|PV|37.942483|-0.772996|0
Urbanización Bonalba|Urbanización Bonalba||Alacantí|PV|38.463038|-0.431424|0
Urbanización Cabo Roig|Urbanización Cabo Roig||Baix Segura|PV|37.916695|-0.727932|0
Urbanización Casa Medina|Urbanización Casa Medina||Plana d'Utiel|PV|39.625466|-1.168521|0
Urbanización Castillo de Don Juan|Urbanización Castillo de Don Juan||Baix Segura|PV|37.932316|-0.765876|0
Urbanización Cava Negra|Urbanización Cava Negra||Alt Vinalopó|PV|38.52031|-0.924707|0
Urbanización Cañada Fría|Urbanización Cañada Fría||Foia de Bunyol|PV|39.499327|-0.630014|0
Urbanización Charco|Urbanización Charco||Marina Baixa|PV|38.492053|-0.28204|0
Urbanización Ciudad de las Telecomunicaciones|Urbanización Ciudad de las Telecomunicaciones||Baix Segura|PV|37.95648|-0.797894|0
Urbanización Ciudad Quesada|Urbanización Ciudad Quesada||Baix Segura|PV|38.066689|-0.712202|0
Urbanización Colinas de San Antonio|Urbanización Colinas de San Antonio||Camp de Túria|PV|39.551686|-0.497798|0
Urbanización Colinas de San Antonio|Urbanización Colinas de San Antonio||Camp de Túria|PV|39.566257|-0.491722|0
Urbanización Collado|Urbanización Collado||Marina Alta|PV|38.722688|0.001889|0
Urbanización Colonia Virgen del Rosario|Urbanización Colonia Virgen del Rosario||Alt Vinalopó|PV|38.514874|-0.916545|0
Urbanización Cumbres de Calicanto|Urbanización Cumbres de Calicanto||Foia de Bunyol|PV|39.43611|-0.571632|0
Urbanización El Alto del Royo|Urbanización El Alto del Royo||Foia de Bunyol|PV|39.431984|-0.690439|0
Urbanización El Bosque|Urbanización El Bosque||Foia de Bunyol|PV|39.448149|-0.617809|0
Urbanización El Carambolo|Urbanización El Carambolo||Foia de Bunyol|PV|39.470009|-0.608196|0
Urbanización El Chaparral|Urbanización El Chaparral||Baix Segura|PV|38.017154|-0.69271|0
Urbanización El Corral de Carmelo|Urbanización El Corral de Carmelo||Foia de Bunyol|PV|39.445299|-0.634975|0
Urbanización El Hontanar|Urbanización El Hontanar||Plana d'Utiel|PV|39.629408|-1.161426|0
Urbanización el Pinar de Casinos|Urbanización el Pinar de Casinos||Camp de Túria|PV|39.682744|-0.728353|0
Urbanización el Poblet|Urbanización el Poblet||Marina Alta|PV|38.786122|-0.02884|0
Urbanización el Presidente|Urbanización el Presidente||Baix Segura|PV|37.940028|-0.770819|0
Urbanización Entrepinos|Urbanización Entrepinos||Foia de Bunyol|PV|39.44278|-0.609634|0
Urbanización Galandú|Urbanización Galandú||Marina Baixa|PV|38.52997|-0.19765|0
Urbanización Horizonte|Urbanización Horizonte||Baix Segura|PV|37.944377|-0.724415|0
Urbanización L'Abeller|Urbanización L'Abeller||Plana Alta|PV|40.020476|-0.012892|0
Urbanización L'Almunia|Urbanización L'Almunia||Marina Alta|PV|38.835724|-0.061385|0
Urbanización la Almazara|Urbanización la Almazara||Marina Alta|PV|38.753324|-0.027888|0
Urbanización la Capitana|Urbanización la Capitana||Vinalopó Mitjà|PV|38.368097|-0.693065|0
Urbanización La Cañada de Farandola|Urbanización La Cañada de Farandola||Foia de Bunyol|PV|39.435648|-0.695851|0
Urbanización La Cañada del Fraile|Urbanización La Cañada del Fraile||Foia de Bunyol|PV|39.426105|-0.675848|0
Urbanización La Cenia|Urbanización La Cenia||Baix Segura|PV|37.925159|-0.725437|0
Urbanización La Cenia II|Urbanización La Cenia II||Baix Segura|PV|37.927791|-0.73119|0
Urbanización la Coma|Urbanización la Coma||Plana Alta|PV|40.035888|-0.048475|0
Urbanización la Florida|Urbanización la Florida||Baix Segura|PV|37.946611|-0.733502|0
Urbanización La Fuentecica|Urbanización La Fuentecica||Foia de Bunyol|PV|39.419021|-0.668534|0
Urbanización La Loma|Urbanización La Loma||Foia de Bunyol|PV|39.451562|-0.606737|0
Urbanización La Loma de Miralcampo|Urbanización La Loma de Miralcampo||Foia de Bunyol|PV|39.452656|-0.60427|0
Urbanización La Loma del Castillo|Urbanización La Loma del Castillo||Foia de Bunyol|PV|39.463151|-0.716686|0
Urbanización La Marina-El Oasis|Urbanización La Marina-El Oasis||Baix Segura|PV|38.132876|-0.683737|0
Urbanización La Mediera|Urbanización La Mediera||Foia de Bunyol|PV|39.452407|-0.596094|0
Urbanización la Regia|Urbanización la Regia||Baix Segura|PV|37.917745|-0.735424|0
Urbanización la Solana|Urbanización la Solana||Baix Segura|PV|37.928984|-0.74841|0
Urbanización La Torreta|Urbanización La Torreta||Baix Segura|PV|37.995011|-0.696926|0
Urbanización la Virgen|Urbanización la Virgen||Alt Vinalopó|PV|38.512238|-0.91246|0
Urbanización las Beatas|Urbanización las Beatas||Alt Vinalopó|PV|38.517691|-0.889131|0
Urbanización Las Filipinas|Urbanización Las Filipinas||Baix Segura|PV|37.945721|-0.774227|0
Urbanización las Mimosas-La Chismosa|Urbanización las Mimosas-La Chismosa||Baix Segura|PV|37.944229|-0.73804|0
Urbanización Las Pedrizas|Urbanización Las Pedrizas||Foia de Bunyol|PV|39.499264|-0.739474|0
Urbanización Las Pesqueras|Urbanización Las Pesqueras||Baix Segura|PV|38.116072|-0.655405|0
Urbanización Llano de los Pastores|Urbanización Llano de los Pastores||Alacantí|PV|38.478631|-0.444579|0
Urbanización Lo Crispín|Urbanización Lo Crispín||Baix Segura|PV|38.050688|-0.738657|0
Urbanización Lomas de Don Juan|Urbanización Lomas de Don Juan||Baix Segura|PV|37.924114|-0.757176|0
Urbanización Los Balcones|Urbanización Los Balcones||Baix Segura|PV|37.959033|-0.726172|0
Urbanización Los Cabuchuelos|Urbanización Los Cabuchuelos||Foia de Bunyol|PV|39.418489|-0.67796|0
Urbanización Los Dolses|Urbanización Los Dolses||Baix Segura|PV|37.930821|-0.746263|0
Urbanización Los Felipes|Urbanización Los Felipes||Foia de Bunyol|PV|39.446227|-0.645275|0
Urbanización Los Visos|Urbanización Los Visos||Foia de Bunyol|PV|39.532228|-0.722975|0
Urbanización Miralcampo|Urbanización Miralcampo||Foia de Bunyol|PV|39.457712|-0.614295|0
Urbanización Mirasoles|Urbanización Mirasoles||Foia de Bunyol|PV|39.442287|-0.62953|0
Urbanización Monsec|Urbanización Monsec||Foia de Bunyol|PV|39.42425|-0.615389|0
Urbanización Monte Cenia - Torre Cenia|Urbanización Monte Cenia - Torre Cenia||Baix Segura|PV|37.923749|-0.747166|0
Urbanización Monte-Sano|Urbanización Monte-Sano||Marina Alta|PV|38.821799|-0.029668|0
Urbanización Monteazul|Urbanización Monteazul||Baix Segura|PV|38.066343|-0.742558|0
Urbanización Montecid|Urbanización Montecid||Vinalopó Mitjà|PV|38.34525|-0.645702|0
Urbanización Montemar|Urbanización Montemar||Baix Segura|PV|38.067334|-0.783038|0
Urbanización Montepinar|Urbanización Montepinar||Baix Segura|PV|38.116731|-0.956278|0
Urbanización Montesano|Urbanización Montesano||Camp de Túria|PV|39.577844|-0.512226|0
Urbanización Olimar|Urbanización Olimar||Foia de Bunyol|PV|39.469578|-0.620727|0
Urbanización Orito San José|Urbanización Orito San José||Vinalopó Mitjà|PV|38.372251|-0.686237|0
Urbanización Panorámica|Urbanización Panorámica||Baix Maestrat|PV|40.548925|0.385172|0
Urbanización Peñascos|Urbanización Peñascos||Safor|PV|38.911022|-0.196191|0
Urbanización Pinar de Campoverde|Urbanización Pinar de Campoverde||Baix Segura|PV|37.90466|-0.838126|0
Urbanización Plans|Urbanización Plans||Marina Baixa|PV|38.512533|-0.249521|0
Urbanización Playamonte|Urbanización Playamonte||Canal de Navarrés|PV|39.078052|-0.697492|0
Urbanización Primera, Segunda y Tercera F|Urbanización Primera, Segunda y Tercera F||Camp de Morvedre|PV|39.667871|-0.338113|0
Urbanización Punta Prima|Urbanización Punta Prima||Baix Segura|PV|37.944223|-0.713729|0
Urbanización Punta Prima|Urbanización Punta Prima||Baix Segura|PV|37.945819|-0.708899|0
Urbanización Santa Bárbara|Urbanización Santa Bárbara||Horta Nord|PV|39.544664|-0.425838|0
Urbanización Sierra de Perenchiza|Urbanización Sierra de Perenchiza||Foia de Bunyol|PV|39.439135|-0.599527|0
Urbanización Sierra Verde|Urbanización Sierra Verde||Marina Baixa|PV|38.632687|-0.108965|0
Urbanización Sol, Mar y Naranjos|Urbanización Sol, Mar y Naranjos||Ribera Baixa|PV|39.237396|-0.253534|0
Urbanización Solana|Urbanización Solana||Marina Alta|PV|38.756236|-0.055856|0
Urbanización Solana Park|Urbanización Solana Park||Marina Alta|PV|38.747909|-0.008936|0
Urbanización Torres I|Urbanización Torres I||Marina Baixa|PV|38.518447|-0.202934|0
Urbanización Torres II|Urbanización Torres II||Marina Baixa|PV|38.521633|-0.196411|0
Urbanización Valle del Sol|Urbanización Valle del Sol||Marina Alta|PV|38.791495|-0.019984|0
Urbanización Valle Dorado|Urbanización Valle Dorado||Alacantí|PV|38.478335|-0.426117|0
Urbanización Villa martín|Urbanización Villa martín||Baix Segura|PV|37.931575|-0.752299|0
Urbanización Villacosta I|Urbanización Villacosta I||Baix Segura|PV|37.932979|-0.766475|0
Urbanización Villacosta II-Las Filipinas|Urbanización Villacosta II-Las Filipinas||Baix Segura|PV|37.935809|-0.77799|0
Urbanización Virgen de la Estrella|Urbanización Virgen de la Estrella||Camp de Túria|PV|39.582795|-0.465827|0
Urbanova|Urbanova||Alacantí|PV|38.28106|-0.522355|0
Urbapol|Urbapol||Maresme|CAT|41.607013|2.627184|0
Urbisol|Urbisol||Bages|CAT|41.807309|2.018013|0
Urtx|Urtx||Cerdanya|CAT|42.387046|1.911191|0
Urús|Urús||Cerdanya|CAT|42.33073|1.85918|1
Useu|Useu||Pallars Sobirà|CAT|42.309867|1.085621|0
Utiel|Utiel||Plana d'Utiel|PV|39.616999|-1.222734|1
Vacarisses|Vacarisses||Vallès Occidental|CAT|41.603918|1.917486|1
Valentia|Valentia||Camp de Túria|PV|39.601075|-0.560501|0
Vall d'Aigües Vives|Vall d'Aigües Vives||Ribera Alta|PV|39.087498|-0.348858|0
Vall de Cardós|Vall de Cardós||Pallars Sobirà|CAT|42.586994|1.216444|1
Vall de Lliria|Vall de Lliria||Camp de Túria|PV|39.662752|-0.572945|0
Vall del Sol|Vall del Sol||Baix Llobregat|CAT|41.391967|1.935989|0
Vall Llebrerola|Vall Llebrerola||Noguera|CAT|41.941384|1.083068|0
Vall Suau i Can Feliu|Vall Suau i Can Feliu||Vallès Occidental|CAT|41.527962|2.071615|0
Vall-de-roures|Valderrobres||el Matarranya|FR|40.853928|0.145232|1
Vall-llebrera|Vall-llebrera||Noguera|CAT|41.934624|1.082192|0
Vall-llobrega|Vall-llobrega||Baix Empordà|CAT|41.886005|3.124387|1
Vallada|Vallada||Costera|PV|38.899809|-0.688734|1
Vallanca|Vallanca||Racó d'Ademús|PV|40.054465|-1.354934|1
Vallat|Vallat||Alt Millars|PV|40.036964|-0.333783|1
Vallbona|Vallbona||Maresme|CAT|41.500943|2.2885|0
Vallbona d'Anoia|Vallbona d'Anoia||Anoia|CAT|41.527832|1.70868|1
Vallbona de les Monges|Vallbona de les Monges||Urgell|CAT|41.501936|1.123283|1
Vallbonica|Vallbonica||Anoia|CAT|41.530701|1.718473|0
Vallcanera Parc|Vallcanera Parc||Selva|CAT|41.845892|2.73692|0
Vallcarca|Vallcarca||Garraf|CAT|41.24092|1.85843|0
Vallcebollera|Valcebollère||l'Alta Cerdanya|CN|42.380842|2.042798|1
Vallcebre|Vallcebre||Berguedà|CAT|42.206235|1.812311|1
Vallclara|Vallclara||Conca de Barberà|CAT|41.37119|0.992334|1
Valldaina|Valldaina||Baix Llobregat|CAT|41.461362|1.937718|0
Valldellou|Baldellou||la Llitera|FR|41.912911|0.556033|1
Valldemossa|Valldemossa||Mallorca|IB|39.70389|2.619985|1
Valldenguli|Valldenguli||Maresme|CAT|41.614766|2.645049|0
Valldeperes|Valldeperes||Conca de Barberà|CAT|41.493088|1.429655|0
Valldevià|Valldevià||Baix Empordà|CAT|42.121844|3.001661|0
Valldoreix|Valldoreix||Vallès Occidental|CAT|41.458255|2.053796|0
Vallegrande Este|Vallegrande Este||Alacantí|PV|38.430907|-0.559275|0
Vallegrande Oeste|Vallegrande Oeste||Alacantí|PV|38.425822|-0.562734|0
Vallespinosa|Vallespinosa||Conca de Barberà|CAT|41.446084|1.343805|0
Vallestàvia|Baillestavy||el Conflent|CN|42.564617|2.521684|1
Vallfogona de Balaguer|Vallfogona de Balaguer||Noguera|CAT|41.752375|0.833897|1
Vallfogona de Ripollès|Vallfogona de Ripollès||Ripollès|CAT|42.185922|2.305285|1
Vallfogona de Riucorb|Vallfogona de Riucorb||Conca de Barberà|CAT|41.556386|1.23488|1
Vallgorguina|Vallgorguina||Vallès Oriental|CAT|41.646655|2.496913|1
Vallgornera|Vallgornera||Mallorca|IB|39.365655|2.867749|0
Vallibona|Vallibona||Ports|PV|40.608002|0.08861|1
Vallirana|Vallirana||Baix Llobregat|CAT|41.373634|1.916955|1
Vallirana Parc|Vallirana Parc||Baix Llobregat|CAT|41.372685|1.941538|0
Valljunquera|Valjunquera||el Matarranya|FR|40.964168|0.016073|1
Vallmanya|Valmanya||el Conflent|CN|42.521533|2.514001|1
Vallmanya|Vallmanya||Solsonès|CAT|41.830727|1.587943|0
Vallmoll|Vallmoll||Alt Camp|CAT|41.245751|1.252131|1
Vallmoll Paradís|Vallmoll Paradís||Alt Camp|CAT|41.25368|1.24929|0
Vallmora|Vallmora||Maresme|CAT|41.489679|2.315347|0
Vallpineda|Vallpineda||Garraf|CAT|41.246548|1.794386|0
Vallromanes|Vallromanes||Vallès Oriental|CAT|41.526856|2.298854|1
Valls|Valls||Alt Camp|CAT|41.301701|1.239165|1
Valls de Torroella|Valls de Torroella||Bages|CAT|41.848489|1.71837|0
Vallserena|Vallserena||Vallès Oriental|CAT|41.690447|2.408659|0
Vallserrat|Vallserrat||Baix Llobregat|CAT|41.501892|1.861275|0
Vallvallida|Vallvallida||Maresme|CAT|41.489662|2.320181|0
Vallverd|Vallverd||Pla d'Urgell|CAT|41.696942|0.951091|0
Vallverd|Vallverd||Conca de Barberà|CAT|41.479762|1.314298|0
Vallverde|Vallverde||Baix Vinalopó|PV|38.234469|-0.596174|0
Vallvidrera|Vallvidrera||Maresme|CAT|41.590658|2.535932|0
Vallès Parc|Vallès Parc||Vallès Occidental|CAT|41.525276|2.071753|0
Vallés|Vallés||Costera|PV|38.988167|-0.557005|1
Valveralla|Valveralla||Alt Empordà|CAT|42.160861|3.036159|0""",
        """València|València||València|PV|39.420604|-0.358439|1
València d'Àneu|València d'Àneu||Pallars Sobirà|CAT|42.633964|1.108604|0
València la Vella|València la Vella||Camp de Túria|PV|39.536347|-0.537183|0
Vandellòs|Vandellòs||Baix Camp|CAT|41.019963|0.831567|0
Vandellòs i l'Hospitalet de l'Infant|Vandellòs i l'Hospitalet de l'Infant||Baix Camp|CAT|40.995636|0.843596|1
Veciana|Veciana||Anoia|CAT|41.668184|1.480699|1
Veinat de l'Estacio|Veinat de l'Estacio||Gironès|CAT|41.919288|2.847766|0
Vensilló|Vencillón||la Llitera|FR|41.708579|0.326151|1
Venta de Poio|Venta de Poio||Camp de Túria|PV|39.474623|-0.56868|0
Venta del Moro|Venta del Moro||Plana d'Utiel|PV|39.455434|-1.39147|1
Venta Gaeta|Venta Gaeta||Vall de Cofrents|PV|39.308025|-0.960267|0
Ventalló|Ventalló||Alt Empordà|CAT|42.151157|3.041702|1
Ventorrillo|Ventorrillo||Plana Alta|PV|40.064824|-0.133261|0
Venècia|Venècia||Safor|PV|38.991562|-0.158542|0
Veo|Veo||Plana Baixa|PV|39.918431|-0.340247|0
Verdú|Verdú||Urgell|CAT|41.59512|1.126553|1
Vereda Brunete|Vereda Brunete||Baix Segura|PV|38.082123|-0.898176|0
Vereda de los Cubos|Vereda de los Cubos||Baix Segura|PV|38.142339|-0.834722|0
Vereda del Rollo|Vereda del Rollo||Baix Segura|PV|38.038061|-0.98391|0
Vereda Liorna|Vereda Liorna||Baix Segura|PV|38.043949|-0.978809|0
Vereda Serranos|Vereda Serranos||Baix Segura|PV|38.07736|-0.910223|0
Verge de Montserrat|Verge de Montserrat||Vallès Occidental|CAT|41.59837|2.022362|0
Verge del Pilar|Verge del Pilar||Tarragonès|CAT|41.11301|1.213605|0
Verger de Sant Miquel|Verger de Sant Miquel||Camp de Túria|PV|39.645054|-0.563805|0
Verges|Verges||Baix Empordà|CAT|42.068056|3.043037|1
Vergós|Vergós||Segarra|CAT|41.664173|1.299356|0
Vergós Guerrejat|Vergós Guerrejat||Segarra|CAT|41.676447|1.353646|0
Vernet|Vernet-les-Bains||el Conflent|CN|42.538232|2.415917|1
Vernet|Vernet||Noguera|CAT|41.904968|1.040785|0
Vespella|Vespella||Osona|CAT|41.975507|2.203029|0
Vespella|Vespella||Tarragonès|CAT|41.205996|1.363273|0
Vespella de Gaià|Vespella de Gaià||Tarragonès|CAT|41.202945|1.36134|1
Veïnat de Clarà|Veïnat de Clarà||Maresme|CAT|41.557713|2.367677|0
Veïnat de Madà|Veïnat de Madà||Maresme|CAT|41.540077|2.407773|0
Veïnat de Mas Masó|Veïnat de Mas Masó||Gironès|CAT|42.073724|2.961963|0
Veïnat de Mas Nadal|Veïnat de Mas Nadal||Gironès|CAT|42.015656|2.909738|0
Veïnat de Sant Miquel del Cros|Veïnat de Sant Miquel del Cros||Maresme|CAT|41.535436|2.417545|0
Veïnat del Roc Llarg|Veïnat del Roc Llarg||Osona|CAT|41.981885|2.191312|0
Viabrea|Viabrea||Selva|CAT|41.728598|2.566858|0
Viacamp i Lliterà|Viacamp y Litera||la Ribagorça|FR|42.072934|0.634358|1
Vic|Vic||Osona|CAT|41.922363|2.23624|1
Vicfred|Vicfred||Segarra|CAT|41.773547|1.357446|0
Victor Font Gual|Victor Font Gual||Maresme|CAT|41.488386|2.325303|0
Vidreres|Vidreres||Selva|CAT|41.774531|2.811805|1
Vidrà|Vidrà||Osona|CAT|42.142228|2.327518|1
Vielha|Vielha||la Vall d'Aran|CAT|42.701784|0.795493|0
Viella i Mitjaran|Vielha e Mijaran||la Vall d'Aran|CAT|42.705946|0.787125|1
Vila|Vila|||AD|42.53474|1.58014|0
Vila|Vila||la Vall d'Aran|CAT|42.736448|0.776481|0
Vila i Vall de Castellbò|Vila i Vall de Castellbò||Alt Urgell|CAT|42.374763|1.35598|0
Vila-real|Vila-real||Plana Baixa|PV|39.933131|-0.128762|1
Vila-Robau|Vila-Robau||Alt Empordà|CAT|42.16432|3.008764|0
Vila-rodona|Vila-rodona||Alt Camp|CAT|41.308444|1.368004|1
Vila-Roja|Vila-Roja||Gironès|CAT|41.971624|2.842166|0
Vila-Rubla|Vila-Rubla||Alt Urgell|CAT|42.346869|1.236397|0
Vila-sacra|Vila-sacra||Alt Empordà|CAT|42.26226|3.02097|1
Vila-sana|Vila-sana||Pla d'Urgell|CAT|41.662194|0.921408|1
Vila-seca|Vila-seca||Tarragonès|CAT|41.101512|1.144552|1
Vila-Seca|Vila-Seca||Osona|CAT|42.061108|2.260666|0
Vilabella|Vilabella||Alt Camp|CAT|41.245382|1.328918|1
Vilabertran|Vilabertran||Alt Empordà|CAT|42.28334|2.981282|1
Vilablareix|Vilablareix||Gironès|CAT|41.953417|2.781545|1
Vilac|Vilac||Aran|CAT|42.721529|0.798811|0
Vilacolum|Vilacolum||Alt Empordà|CAT|42.194219|3.037007|0
Vilada|Vilada||Berguedà|CAT|42.136488|1.923068|1
Viladamat|Viladamat||Alt Empordà|CAT|42.127691|3.063336|1
Viladasens|Viladasens||Gironès|CAT|42.097136|2.924051|1
Viladecans|Viladecans||Baix Llobregat|CAT|41.30295|2.031932|1
Viladecavalls|Viladecavalls||Vallès Occidental|CAT|41.557755|1.951496|1
Viladellops|Viladellops||Alt Penedès|CAT|41.305777|1.736438|0
Vilademuls|Vilademuls||Pla de l'Estany|CAT|42.134383|2.864737|1
Viladeperdius|Viladeperdius||Conca de Barberà|CAT|41.480719|1.408481|0
Viladomiu|Viladomiu||Berguedà|CAT|41.957314|1.88199|0
Viladomiu Nou|Viladomiu Nou||Berguedà|CAT|42.003998|1.886644|0
Viladomiu Vell|Viladomiu Vell||Berguedà|CAT|42.009178|1.885471|0
Viladordis|Viladordis||Bages|CAT|41.725095|1.863628|0
Viladrau|Viladrau||Osona|CAT|41.844471|2.372559|1
Vilafamés|Vilafamés||Plana Alta|PV|40.121764|-0.071046|1
Vilafant|Vilafant||Alt Empordà|CAT|42.249526|2.945056|1
Vilafermosa|Villahermosa del Río||Alt Millars|PV|40.205727|-0.423011|1
Vilafortuny|Vilafortuny||Baix Camp|CAT|41.073864|1.099688|0
Vilafranca|Vilafranca/Villafranca del Cid||Ports|PV|40.431486|-0.263631|1
Vilafranca de Bonany|Vilafranca de Bonany||Mallorca|IB|39.557029|3.117389|1
Vilafranca de Conflent|Villefranche-de-Conflent||el Conflent|CN|42.596861|2.360377|1
Vilafranca del Penedès|Vilafranca del Penedès||Alt Penedès|CAT|41.347519|1.700594|1
Vilafreser|Vilafreser||Pla de l'Estany|CAT|42.083773|2.88209|0
Vilafruns|Vilafruns||Bages|CAT|41.839689|1.877716|0
Vilagrassa|Vilagrassa||Urgell|CAT|41.643098|1.08806|1
Vilagrasseta|Vilagrasseta||Segarra|CAT|41.604879|1.282022|0
Vilajoan|Vilajoan||Alt Empordà|CAT|42.165732|2.934707|0
Vilajuïga|Vilajuïga||Alt Empordà|CAT|42.332745|3.102963|1
Vilalba|Vilalba||Vallès Oriental|CAT|41.629252|2.352134|0
Vilalba dels Arcs|Vilalba dels Arcs||Terra Alta|CAT|41.147891|0.400594|1
Vilalba Sasserra|Vilalba Sasserra||Vallès Oriental|CAT|41.640334|2.463141|1
Vilalleons|Vilalleons||Osona|CAT|41.893274|2.318511|0
Vilaller|Vilaller||Alta Ribagorça|CAT|42.541162|0.758124|1
Vilallobent|Vilallobent||Cerdanya|CAT|42.407744|1.950652|0
Vilallonga|Vilallonga/Villalonga||Safor|PV|38.881088|-0.227043|1
Vilallonga de la Salanca|Villelongue-de-la-Salanque||el Rosselló|CN|42.725479|2.977592|1
Vilallonga de Ter|Vilallonga de Ter||Ripollès|CAT|42.341861|2.278447|1
Vilallonga del Camp|Vilallonga del Camp||Tarragonès|CAT|41.211562|1.195694|1
Vilallonga dels Monts|Villelongue-dels-Monts||el Rosselló|CN|42.523268|2.899876|1
Vilalta|Vilalta||Noguera|CAT|41.850473|1.241399|0
Vilamacolum|Vilamacolum||Alt Empordà|CAT|42.199336|3.056594|1
Vilamajor|Vilamajor||Anoia|CAT|41.67721|1.427702|0
Vilamajor|Vilamajor||Noguera|CAT|41.837897|1.219454|0
Vilamalla|Vilamalla||Alt Empordà|CAT|42.222062|2.972973|1
Vilamalur|Villamalur||Alt Millars|PV|39.958929|-0.406934|1
Vilamaniscle|Vilamaniscle||Alt Empordà|CAT|42.376241|3.066905|1
Vilamarxant|Vilamarxant||Camp de Túria|PV|39.5602|-0.655333|1
Vilamitjana|Vilamitjana||Pallars Jussà|CAT|42.146666|0.919087|0
Vilamitjana|Vilamitjana||Alt Urgell|CAT|42.348343|1.379008|0
Vilamolat de Mur|Vilamolat de Mur||Pallars Jussà|CAT|42.122808|0.83981|0
Vilamulaca|Villemolaque||el Rosselló|CN|42.593913|2.848591|1
Vilamur|Vilamur||Pallars Sobirà|CAT|42.379883|1.16145|0
Vilamòs|Vilamòs||la Vall d'Aran|CAT|42.75547|0.732894|1
Vilanant|Vilanant||Alt Empordà|CAT|42.256702|2.880043|1
Vilanna|Vilanna||Gironès|CAT|41.971161|2.703949|0
Vilanova|Vilanova||Camp de Túria|PV|39.576131|-0.610421|0
Vilanova d'Alcolea|Vilanova d'Alcolea||Plana Alta|PV|40.239384|0.090347|1
Vilanova d'Escornalbou|Vilanova d'Escornalbou||Baix Camp|CAT|41.105605|0.929975|1
Vilanova d'Espoia|Vilanova d'Espoia||Anoia|CAT|41.5156|1.654266|0
Vilanova de Banat|Vilanova de Banat||Alt Urgell|CAT|42.341576|1.552761|0
Vilanova de Bellpuig|Vilanova de Bellpuig||Pla d'Urgell|CAT|41.601689|0.955162|1
Vilanova de l'Aguda|Vilanova de l'Aguda||Noguera|CAT|41.898527|1.262034|1
Vilanova de la Barca|Vilanova de la Barca||Segrià|CAT|41.681455|0.735034|1
Vilanova de la Muga|Vilanova de la Muga||Alt Empordà|CAT|42.280983|3.042667|0
Vilanova de la Ribera|Villeneuve-la-Rivière||el Rosselló|CN|42.705429|2.798677|1
Vilanova de la Sal|Vilanova de la Sal||Noguera|CAT|41.878368|0.786708|0
Vilanova de Meià|Vilanova de Meià||Noguera|CAT|41.993268|1.007088|1
Vilanova de Prades|Vilanova de Prades||Conca de Barberà|CAT|41.338913|0.948782|1
Vilanova de Raó|Villeneuve-de-la-Raho||el Rosselló|CN|42.639843|2.910518|1
Vilanova de Sau|Vilanova de Sau||Osona|CAT|41.942921|2.42216|1
Vilanova de Segrià|Vilanova de Segrià||Segrià|CAT|41.711586|0.621049|1
Vilanova de Viver|Villanueva de Viver||Alt Millars|PV|40.057848|-0.638547|1
Vilanova del Camí|Vilanova del Camí||Anoia|CAT|41.561266|1.634027|1
Vilanova del Vallès|Vilanova del Vallès||Vallès Oriental|CAT|41.555047|2.306643|1
Vilanova i la Geltrú|Vilanova i la Geltrú||Garraf|CAT|41.237083|1.704802|1
Vilanoveta|Vilanoveta||Garraf|CAT|41.24503|1.74584|0
Vilaplana|Vilaplana||Baix Camp|CAT|41.249672|1.035541|1
Vilaplana|Vilaplana||Noguera|CAT|41.99769|1.248067|0
Vilar de Canes|Vilar de Canes||Alt Maestrat|PV|40.35414|-0.063153|1
Vilardell|Vilardell||Vallès Oriental|CAT|41.692362|2.524849|0
Vilardida|Vilardida||Alt Camp|CAT|41.285712|1.363113|0
Vilardida|Vilardida||Alt Camp|CAT|41.285906|1.362985|0
Vilarnadal|Vilarnadal||Alt Empordà|CAT|42.341376|2.953191|0
Vilartolí|Vilartolí||Alt Empordà|CAT|42.390989|2.971593|0
Vilassar de Dalt|Vilassar de Dalt||Maresme|CAT|41.524169|2.347658|1
Vilassar de Mar|Vilassar de Mar||Maresme|CAT|41.505383|2.383854|1
Vilatenim|Vilatenim||Alt Empordà|CAT|42.268627|2.994355|0
Vilavenut|Vilavenut||Pla de l'Estany|CAT|42.139148|2.820526|0
Vilaverd|Vilaverd||Conca de Barberà|CAT|41.33443|1.164216|1
Vilaür|Vilaür||Alt Empordà|CAT|42.138903|2.956264|1
Vilella|Vilella||Cerdanya|CAT|42.35361|1.774085|0
Vilella de Cinca|Velilla de Cinca||el Baix Cinca|FR|41.580786|0.25496|1
Vilert|Vilert||Pla de l'Estany|CAT|42.17373|2.821327|0
Vileta de Mar|Vileta de Mar||Tarragonès|CAT|41.122752|1.281421|0
Viliella|Viliella||Cerdanya|CAT|42.415426|1.696|0
Villa Carmen|Villa Carmen||Horta Sud|PV|39.402969|-0.417091|0
Villa Rosa|Villa Rosa||Baix Segura|PV|37.949183|-0.722377|0
Villapiedra-Sector B2|Villapiedra-Sector B2||Baix Segura|PV|37.918579|-0.73935|0
Villaportes|Villaportes||Alacantí|PV|38.409589|-0.420542|0
Villar de Olmos|Villar de Olmos||Plana d'Utiel|PV|39.595887|-1.061346|0
Villar de Tejas|Villar de Tejas||Serrans|PV|39.631823|-1.08784|0
Villargordo del Cabriel|Villargordo del Cabriel||Plana d'Utiel|PV|39.531897|-1.451543|1
Villarrué|Villarrué||la Ribagorça|FR|42.479478|0.590201|0
Villas l'Eliana|Villas l'Eliana||Camp de Túria|PV|39.578061|-0.520285|0
Villatorcas|Villatorcas||Alt Palància|PV|39.825154|-0.449419|0
Villena|Villena||Alt Vinalopó|PV|38.633954|-0.913906|1
Villores|Villores||Ports|PV|40.679418|-0.209352|1
Vilobí d'Onyar|Vilobí d'Onyar||Selva|CAT|41.891475|2.739284|1
Vilobí del Penedès|Vilobí del Penedès||Alt Penedès|CAT|41.385732|1.666068|1
Vilopriu|Vilopriu||Baix Empordà|CAT|42.10938|2.987458|1
Vilves|Vilves||Noguera|CAT|41.91769|1.08256|0
Vimbodí|Vimbodí||Conca de Barberà|CAT|41.40028|1.050974|0
Vimbodí i Poblet|Vimbodí i Poblet||Conca de Barberà|CAT|41.375298|1.046339|1
Vinaixa|Vinaixa||Garrigues|CAT|41.439932|0.97591|1
Vinalesa|Vinalesa||Horta Nord|PV|39.534273|-0.368979|1
Vinallop|Vinallop||Baix Ebre|CAT|40.774892|0.513208|0
Vinaròs|Vinaròs||Baix Maestrat|PV|40.510253|0.435228|1
Vinebre|Vinebre||Ribera d'Ebre|CAT|41.216151|0.622602|1
Vingrau|Vingrau||el Rosselló|CN|42.857833|2.79049|1
Vinya del Pollastre|Vinya del Pollastre||Alt Penedès|CAT|41.459079|1.865106|0
Vinyoles|Vinyoles||Osona|CAT|42.038978|2.232404|0
Vinyols|Vinyols||Baix Camp|CAT|41.113744|1.039249|0
Vinyols i els Arcs|Vinyols i els Arcs||Baix Camp|CAT|41.098937|1.054729|1
Vinçà|Vinça||el Conflent|CN|42.643619|2.523092|1
Virgen de Montserrat|Virgen de Montserrat||Ribera Alta|PV|39.379843|-0.547165|0
Virgen del Camino|Virgen del Camino||Baix Segura|PV|38.1653|-0.936807|0
Virgen del Camino|Virgen del Camino||Baix Segura|PV|38.165049|-0.930014|0
Virgili|Virgili||Tarragonès|CAT|41.170754|1.371136|0
Virà|Vira||la Fenolleda|CN|42.763315|2.394446|1
Vista Alegre|Vista Alegre||Bages|CAT|41.719127|1.819706|0
Vista Calderona|Vista Calderona||Camp de Túria|PV|39.606837|-0.526194|0
Vista del Pirineu|Vista del Pirineu||Bages|CAT|41.79026|1.972607|0
Vistabella|Vistabella||Tarragonès|CAT|41.209058|1.264793|0
Vistabella|Vistabella||Baix Segura|PV|38.023865|-0.836936|0
Vistabella del Maestrat|Vistabella del Maestrat||Alcalatén|PV|40.306373|-0.297577|1
Vistamar|Vistamar||Maresme|CAT|41.610516|2.587356|0
Viu de Llevata|Viu de Llevata||Alta Ribagorça|CAT|42.369572|0.812676|0
Viver|Viver||Alt Palància|PV|39.932237|-0.630117|1
Viver de Segarra|Viver de Segarra||Segarra|CAT|41.749675|1.367991|0
Viver i Serrateix|Viver i Serrateix||Berguedà|CAT|41.947809|1.79338|1
Vivers|Vivès||el Rosselló|CN|42.529208|2.767849|1
Viñamalata|Viñamalata||Foia de Bunyol|PV|39.418254|-0.704761|0
Viñuelas|Viñuelas||Vall de Cofrents|PV|39.327767|-1.008334|0
Vora Sitges|Vora Sitges||Garraf|CAT|41.281183|1.737466|0
Vulpellac|Vulpellac||Baix Empordà|CAT|41.959223|3.055407|0
Vía Marina|Vía Marina||Baix Camp|CAT|41.02716|0.926874|0
Víllec i Estana|Víllec i Estana||Cerdanya|CAT|42.32174|1.693048|0
Xacarella|Jacarilla||Baix Segura|PV|38.047928|-0.851719|1
Xalans|Jalance||Vall de Cofrents|PV|39.196197|-1.098761|1
Xalets de Lladró|Xalets de Lladró||Horta Nord|PV|39.610845|-0.370114|0
Xalets de Salou i Mirador de Salou|Xalets de Salou i Mirador de Salou||Tarragonès|CAT|41.084727|1.155275|0
Xaló|Xaló||Marina Alta|PV|38.712527|-0.031086|1
Xamaleta|Xamaleta|||CX|38.469203|-1.309809|0
Xarafull|Jarafuel||Vall de Cofrents|PV|39.148882|-1.109763|1
Xaró|Xaró||Anoia|CAT|41.541152|1.669672|0
Xauxa|Xauxa||Safor|PV|38.980294|-0.240796|0
Xelardo|Xelardo||Camp de Túria|PV|39.666965|-0.581227|0
Xella|Chella||Canal de Navarrés|PV|39.03934|-0.699564|1
Xelva|Chelva||Serrans|PV|39.742599|-1.017199|1
Xera|Chera||Plana d'Utiel|PV|39.584402|-0.963389|1
Xeraco|Xeraco||Safor|PV|39.036782|-0.22877|1
Xerallo|Xerallo||Pallars Jussà|CAT|42.369737|0.865222|0
Xeresa|Xeresa||Safor|PV|39.01318|-0.225819|1
Xert|Xert||Baix Maestrat|PV|40.533877|0.134355|1
Xerta|Xerta||Baix Ebre|CAT|40.902576|0.463351|1
Xest|Cheste||Foia de Bunyol|PV|39.508446|-0.677661|1
Xestalgar|Gestalgar||Serrans|PV|39.581751|-0.842471|1
Xilxes|Chilches/Xilxes||Plana Baixa|PV|39.779687|-0.181071|1
Xinorlet|Xinorlet||Vinalopó Mitjà|PV|38.415151|-0.962104|0
Xirivella|Xirivella||Horta Sud|PV|39.459162|-0.430231|1
Xirles|Xirles||Marina Baixa|PV|38.635453|-0.142295|0
Xiva|Chiva||Foia de Bunyol|PV|39.487737|-0.742284|1
Xiva de Morella|Xiva de Morella||Ports|PV|40.659092|-0.125864|0
Xixona|Jijona/Xixona||Alacantí|PV|38.551089|-0.489616|1
Xixona/Jijona|Xixona/Jijona||Alacantí|PV|38.534046|-0.505213|0
Xodos|Xodos/Chodos||Alcalatén|PV|40.230775|-0.289589|1
Xulilla|Chulilla||Serrans|PV|39.651789|-0.858974|1
Xàbia|Jávea/Xàbia||Marina Alta|PV|38.767376|0.163033|1
Xàbia/Jávea|Xàbia/Jávea||Marina Alta|PV|38.787468|0.155808|0
Xàtiva|Xàtiva||Costera|PV|39.002718|-0.511816|1
Xèrica|Jérica||Alt Palància|PV|39.889729|-0.584474|1
Xóvar|Chóvar||Alt Palància|PV|39.849145|-0.324037|1
Yale|Yale||Camp de Túria|PV|39.567985|-0.543842|0
Yeseras|Yeseras||Alacantí|PV|38.393577|-0.587191|0
Zarra|Zarra||Vall de Cofrents|PV|39.100105|-1.117719|1
Zona Costera|Zona Costera||Tarragonès|CAT|41.170437|1.47657|0
Zona Educacional|Zona Educacional||Tarragonès|CAT|41.132071|1.241043|0""",
        """Zona Eixample|Zona Eixample||Tarragonès|CAT|41.123185|1.237358|0
Zona Industrial|Zona Industrial||Marina Baixa|PV|38.519684|-0.208194|0
Àger|Àger||Noguera|CAT|42.000606|0.73892|1
Àgora Parc|Àgora Parc||Maresme|CAT|41.701571|2.657695|0
Àreu|Àreu||Pallars Sobirà|CAT|42.588122|1.326377|0
Àrreu|Àrreu||Pallars Sobirà|CAT|42.66395|1.074195|0
Èguet|Égat||l'Alta Cerdanya|CN|42.50233|2.017666|1
Éller|Éller||Cerdanya|CAT|42.416551|1.792583|0
Énguera|Enguera||Canal de Navarrés|PV|38.963937|-0.810407|1
Òdena|Òdena||Anoia|CAT|41.618601|1.631426|1
Òpol i Perellós|Opoul-Périllos||el Rosselló|CN|42.880115|2.857973|1
Òrrius|Òrrius||Maresme|CAT|41.555115|2.351046|1
Úbeda|Úbeda||Vinalopó Mitjà|PV|38.447881|-0.984281|0"""
    )
    private val resolved by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val index = HashMap<String, IosGeoPlace>(8000)
        val byRegion = HashMap<String, MutableList<IosGeoPlace>>()
        rows.asSequence().flatMap { it.lineSequence() }.forEach { row ->
            val v = row.split('|')
            if (v.size != 8) return@forEach
            val lat = v[5].toDoubleOrNull() ?: return@forEach
            val lon = v[6].toDoubleOrNull() ?: return@forEach
            val p = IosGeoPlace(v[0],v[3],v[4],lat,lon,v[7] == "1")
            for (name in listOf(v[0],v[1]) + v[2].split(';')) {
                val k = norm(name)
                if (k.isNotBlank() && (index[k] == null || (!index.getValue(k).municipality && p.municipality))) index[k] = p
            }
            byRegion.getOrPut(norm(p.comarca)) { mutableListOf() }.add(p)
        }
        index to byRegion
    }
    fun allTowns(): List<String> = resolved.first.values.asSequence().filter { it.municipality }.map { it.name }.distinct().sorted().toList()
    fun allComarques(): List<String> = resolved.second.values.mapNotNull { it.firstOrNull()?.comarca }.distinct().sorted()
    fun searchTowns(query: String, limit: Int = 25): List<String> {
        val term = norm(query)
        if (term.length < 2) return emptyList()
        return resolved.first.asSequence().filter { (alias,_) -> alias.contains(term) }
            .map { it.value.name }.distinct().take(limit).toList().sorted()
    }
    fun place(name: String): IosGeoPlace? = resolved.first[norm(name)]
    fun comarcaPoints(name: String): List<IosGeoPlace> = resolved.second[norm(name)].orEmpty()
    fun kilometres(a: IosGeoPlace, b: IosGeoPlace): Double {
        val r = 6371.0088
        val dLat = (b.latitude-a.latitude) * PI / 180.0
        val dLon = (b.longitude-a.longitude) * PI / 180.0
        val h = sin(dLat/2).pow(2.0) + cos(a.latitude*PI/180.0)*cos(b.latitude*PI/180.0)*sin(dLon/2).pow(2.0)
        return 2.0*r*asin(sqrt(h.coerceIn(0.0,1.0)))
    }
    fun within(eventTown: String, towns: Set<String>, comarcaNames: Set<String>, townRadii: Map<String,Int>,
               defaultRadius: Int = 30, fixedRadius: Int? = null): Boolean {
        val place = place(eventTown)
        val eventKey = norm(eventTown)
        if (towns.any { norm(it) == eventKey }) return true
        if (comarcaNames.any { norm(it) == norm(place?.comarca.orEmpty()) }) return true
        if (place == null) return false // Nunca ampliar arbitràriament una població no localitzada.
        val townMatch = towns.any { name ->
            val origin = place(name) ?: return@any false
            val radius = fixedRadius ?: (townRadii[name] ?: defaultRadius)
            kilometres(origin,place) <= radius
        }
        if (townMatch) return true
        return comarcaNames.any { comarca ->
            val points = comarcaPoints(comarca)
            points.any { kilometres(it,place) <= (fixedRadius ?: defaultRadius) }
        }
    }
}
