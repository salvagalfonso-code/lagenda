package com.example.agendavalenciana.data

import com.example.agendavalenciana.model.AccountRole
import com.example.agendavalenciana.model.AppAccount
import com.example.agendavalenciana.model.Concert
import com.example.agendavalenciana.model.CommunityGroup
import com.example.agendavalenciana.model.CommunityGroupType
import com.example.agendavalenciana.model.CommunityIdentity
import com.example.agendavalenciana.model.ChatMessage
import com.example.agendavalenciana.model.ConcertComment
import com.example.agendavalenciana.model.ArtistPublicProfile
import com.example.agendavalenciana.model.CommunityChatMessage
import com.example.agendavalenciana.model.GroupPost
import com.example.agendavalenciana.model.Story

object DemoData {

    val accounts = listOf(
        AppAccount(
            id = 1,
            username = "Salva",
            password = "prova",
            displayName = "Salva",
            role = AccountRole.SUPER_ADMIN
        ),
        AppAccount(2, "pau", "prova", "Pau", AccountRole.USER),
        AppAccount(3, "marina", "prova", "Marina", AccountRole.USER),
        AppAccount(4, "nuria", "prova", "Núria", AccountRole.USER),
        AppAccount(5, "joan", "prova", "Joan", AccountRole.USER),
        AppAccount(6, "aina", "prova", "Aina", AccountRole.USER),
        AppAccount(7, "vicent", "prova", "Vicent", AccountRole.USER),
        AppAccount(8, "clara", "prova", "Clara", AccountRole.USER),
        AppAccount(9, "laia", "prova", "Laia", AccountRole.USER),
        AppAccount(10, "andreu", "prova", "Andreu", AccountRole.USER),
        AppAccount(11, "mireia", "prova", "Mireia", AccountRole.USER),
        AppAccount(101, "lafumiga", "prova", "La Fúmiga", AccountRole.GROUP, setOf("La Fúmiga")),
        AppAccount(102, "lagossasorda", "prova", "La Gossa Sorda", AccountRole.GROUP, setOf("La Gossa Sorda")),
        AppAccount(103, "auxili", "prova", "Auxili", AccountRole.GROUP, setOf("Auxili")),
        AppAccount(104, "eldiluvi", "prova", "El Diluvi", AccountRole.GROUP, setOf("El Diluvi")),
        AppAccount(105, "tardor", "prova", "Tardor", AccountRole.GROUP, setOf("Tardor")),
        AppAccount(106, "titopontet", "prova", "Tito Pontet", AccountRole.GROUP, setOf("Tito Pontet")),
        AppAccount(107, "malifeta", "prova", "Malifeta", AccountRole.GROUP, setOf("Malifeta")),
        AppAccount(108, "cactus", "prova", "Cactus", AccountRole.GROUP, setOf("Cactus")),
        AppAccount(109, "romanticdimoni", "prova", "Romàntic Dimoni", AccountRole.GROUP, setOf("Romàntic Dimoni")),
        AppAccount(110, "maluks", "prova", "Maluks", AccountRole.GROUP, setOf("Maluks")),
        AppAccount(150, "auditorivila", "prova", "Auditori Municipal Vila-real", AccountRole.VENUE, managedVenues = setOf("Auditori Municipal")),
        AppAccount(151, "promotoracultural", "prova", "Promotora Cultural", AccountRole.PROMOTER, managedArtists = setOf("Cactus", "Romàntic Dimoni")),

        // Comptes professionals amb permisos multi-grup.
        AppAccount(201, "agenciavalencia", "prova", "Agència Valenciana", AccountRole.AGENCY, setOf("La Fúmiga", "Auxili", "Tardor")),
        AppAccount(202, "segellvalencia", "prova", "Segell Valencià", AccountRole.LABEL, setOf("Malifeta", "Cactus", "Romàntic Dimoni", "Maluks"))
    )
    val concerts = listOf(
        Concert(
            id = 1,
            artist = "La Fúmiga",
            date = "2026-10-03",
            time = "22:30",
            town = "Alzira",
            comarca = "Ribera Alta",
            venue = "Recinte Firal",
            styles = listOf("Pop festiu", "Ska"),
            price = 18,
            language = "Valencià",
            description = "Concert amb informació d’agenda, entrades i comunitat."
        ),
        Concert(
            id = 2,
            artist = "La Gossa Sorda",
            date = "2026-10-09",
            time = "23:00",
            town = "Pego",
            comarca = "Marina Alta",
            venue = "Pla de la Font",
            styles = listOf("Ska punk", "Rock"),
            price = 20,
            language = "Valencià",
            description = "Informació del concert i accés a la comunitat."
        ),
        Concert(
            id = 3,
            artist = "Auxili",
            date = "2026-10-10",
            time = "22:00",
            town = "Ontinyent",
            comarca = "Vall d'Albaida",
            venue = "Parc de Benarrai",
            styles = listOf("Reggae", "Ska"),
            price = 15,
            language = "Valencià",
            description = "Informació del concert i accés a la comunitat."
        ),
        Concert(
            id = 4,
            artist = "El Diluvi",
            date = "2026-10-16",
            time = "20:30",
            town = "Xàtiva",
            comarca = "Costera",
            venue = "Gran Teatre",
            styles = listOf("Folk", "Mestissatge"),
            price = 12,
            language = "Valencià",
            description = "Informació del concert i accés a la comunitat."
        ),
        Concert(
            id = 5,
            artist = "Tardor",
            date = "2026-10-17",
            time = "21:00",
            town = "Burjassot",
            comarca = "Horta Nord",
            venue = "Auditori",
            styles = listOf("Indie", "Pop rock"),
            price = 16,
            language = "Valencià",
            description = "Informació del concert i accés a la comunitat."
        ),
        Concert(
            id = 6,
            artist = "Tito Pontet",
            date = "2026-09-25",
            time = "22:30",
            town = "Castelló de la Plana",
            comarca = "Plana Alta",
            venue = "Plaça Major",
            styles = listOf("Ska", "Reggae"),
            price = 0,
            language = "Valencià",
            description = "Concert gratuït amb accés a comunitat."
        ),
        Concert(
            id = 7,
            artist = "Malifeta",
            date = "2026-09-26",
            time = "23:30",
            town = "Gandia",
            comarca = "Safor",
            venue = "Moll dels Borja",
            styles = listOf("Urbana", "Electrònica"),
            price = 10,
            language = "Valencià",
            description = "Informació del concert i accés a la comunitat."
        ),
        Concert(
            id = 8,
            artist = "Cactus",
            date = "2026-10-30",
            time = "22:00",
            town = "Vila-real",
            comarca = "Plana Baixa",
            venue = "Auditori Municipal",
            styles = listOf("Pop", "Urbana", "Electrònica"),
            price = 14,
            language = "Valencià",
            description = "Informació del concert i accés a la comunitat.",
            sponsored = true,
            sponsorLabel = "Concert destacat · Promotora",
            coEditorUsernames = setOf("auditorivila", "promotoracultural")
        ),
        Concert(
            id = 9,
            artist = "Romàntic Dimoni",
            date = "2026-10-31",
            time = "23:45",
            town = "Sagunt",
            comarca = "Camp de Morvedre",
            venue = "Casal Jove",
            styles = listOf("Punk", "Rock"),
            price = 8,
            language = "Valencià",
            description = "Informació del concert i accés a la comunitat.",
            coEditorUsernames = setOf("promotoracultural")
        ),
        Concert(
            id = 10,
            artist = "Maluks",
            date = "2026-11-07",
            time = "21:30",
            town = "València",
            comarca = "València",
            venue = "La Mutant",
            styles = listOf("Electrònica", "Reggae", "Urbana"),
            price = 13,
            language = "Valencià",
            description = "Informació del concert i accés a la comunitat."
        )
    )

    private val batecsNow = System.currentTimeMillis()

    val stories = listOf(
        Story(1, "La Fúmiga", "LF", "Nova data", "Hem afegit una nova data al calendari.", batecsNow - 5L * 60L * 1000L),
        Story(2, "Auxili", "AU", "Entrades", "Entrades disponibles.", batecsNow - 18L * 60L * 1000L),
        Story(7, "La Fúmiga", "LF", "Últim assaig", "Preparant el directe d'esta setmana.", batecsNow - 34L * 60L * 1000L),
        Story(3, "Malifeta", "MA", "Hui toquem", "Ens veiem esta nit.", batecsNow - 60L * 60L * 1000L),
        Story(4, "El Diluvi", "ED", "Nou vídeo", "Ja podeu veure el nou vídeo.", batecsNow - 3L * 60L * 60L * 1000L),
        Story(5, "Romàntic Dimoni", "RD", "Assaig", "Preparant el pròxim directe.", batecsNow - 10L * 60L * 60L * 1000L),
        Story(6, "Maluks", "MK", "Backstage", "Un poc de backstage abans del concert.", batecsNow - 26L * 60L * 60L * 1000L),
        Story(8, "Auxili", "AU", "Fa dos dies", "Este batec ja ha caducat i no es mostrarà.", batecsNow - 49L * 60L * 60L * 1000L)
    )

    val posts = listOf(
        GroupPost(1, "Romàntic Dimoni", "Nova data de gira", "Hem afegit una nova parada a la gira."),
        GroupPost(2, "El Diluvi", "Nou vídeo", "Ja podeu veure el nou vídeo."),
        GroupPost(3, "Malifeta", "Cartell complet", "Ja tenim el cartell complet.")
    )

    val artistProfiles = listOf(
        ArtistPublicProfile("La Fúmiga", "Grup valencià de música festiva.", "Pop festiu, vents i ska per a directe.", "Alzira", "Spotify · Instagram · YouTube"),
        ArtistPublicProfile("La Gossa Sorda", "Projecte de rock i ska en valencià.", "Ska punk, rock i directe combatiu.", "Pego", "Spotify · Instagram"),
        ArtistPublicProfile("Auxili", "Banda valenciana de reggae i ska.", "Reggae, ska i mestissatge.", "Ontinyent", "Spotify · YouTube"),
        ArtistPublicProfile("El Diluvi", "Folk valencià contemporani.", "Folk, arrel i mestissatge.", "l'Alcoià", "Spotify · Instagram"),
        ArtistPublicProfile("Tardor", "Pop-rock valencià.", "Indie i pop-rock.", "València", "Spotify · Instagram"),
        ArtistPublicProfile("Tito Pontet", "Ritmes festius valencians.", "Ska i reggae.", "l'Alcoià", "Spotify"),
        ArtistPublicProfile("Malifeta", "Projecte urbà valencià.", "Urbana i electrònica.", "València", "Spotify · Instagram"),
        ArtistPublicProfile("Cactus", "Projecte de pop i música urbana.", "Pop, urbana i electrònica.", "València", "Spotify · Instagram · YouTube"),
        ArtistPublicProfile("Romàntic Dimoni", "Punk valencià.", "Punk i rock.", "País Valencià", "Instagram · Bandcamp"),
        ArtistPublicProfile("Maluks", "Electrònica, reggae i música urbana.", "Electrònica, reggae i urbana.", "València", "Spotify · Instagram")
    )

    // Identitat mínima visible dels particulars dins dels xats.
    // No són perfils navegables: només foto (si n'hi ha), nom i gustos.
    val communityPeople = listOf(
        CommunityIdentity("Pau", listOf("Punk", "Ska", "Rock"), "asset://demo/users/pau.jpg"),
        CommunityIdentity("Marina", listOf("Indie", "Pop rock", "Electrònica"), "asset://demo/users/marina.jpg"),
        CommunityIdentity("Núria", listOf("Rock", "Punk", "Folk"), "asset://demo/users/nuria.jpg"),
        CommunityIdentity("Joan", listOf("Reggae", "Ska", "Mestissatge"), "asset://demo/users/joan.jpg"),
        CommunityIdentity("Aina", listOf("Urbana", "Electrònica", "Pop"), "asset://demo/users/aina.jpg"),
        CommunityIdentity("Vicent", listOf("Punk", "Hardcore", "Rock"), "asset://demo/users/vicent.jpg"),
        CommunityIdentity("Clara", listOf("Folk", "Pop", "Indie"), "asset://demo/users/clara.jpg"),
        CommunityIdentity("Laia", listOf("Folk", "Reggae", "Mestissatge"), "asset://demo/users/laia.jpg"),
        CommunityIdentity("Andreu", listOf("Punk", "Rock", "Ska"), "asset://demo/users/andreu.jpg"),
        CommunityIdentity("Mireia", listOf("Indie", "Rock", "Electrònica"), "asset://demo/users/mireia.jpg")
    )

    val communityGroups = listOf(
        CommunityGroup(
            id = 1,
            concertId = 8,
            type = CommunityGroupType.CAR,
            creator = "Marina",
            town = "Almassora",
            title = "Cotxe des d’Almassora",
            description = "Som un cotxe de 5 i ens queden 2 places. Eixim cap a Vila-real a les 20:30.",
            capacity = 5,
            members = setOf("Marina", "Pau", "Núria")
        ),
        CommunityGroup(
            id = 2,
            concertId = 8,
            type = CommunityGroupType.TRAIN,
            creator = "Joan",
            town = "Castelló de la Plana",
            title = "Tren des de Castelló",
            description = "Anem amb el tren de les 19:48 i tornem amb el primer del matí.",
            members = setOf("Joan", "Aina", "Vicent", "Clara")
        ),
        CommunityGroup(
            id = 3,
            concertId = 8,
            type = CommunityGroupType.HOSTING,
            creator = "Laia",
            town = "Vila-real",
            title = "Tinc lloc per dormir",
            description = "Tinc sofà i una habitació lliure per a 2 persones després del concert.",
            capacity = 2,
            members = setOf("Laia")
        ),
        CommunityGroup(
            id = 4,
            concertId = 9,
            type = CommunityGroupType.COMPANY,
            creator = "Andreu",
            town = "València",
            title = "Gent de València per anar a Sagunt",
            description = "Busque gent per anar junts i tornar després del concert.",
            members = setOf("Andreu", "Mireia")
        ),
        CommunityGroup(
            id = 5,
            concertId = 8,
            type = CommunityGroupType.WOMEN_ONLY,
            creator = "Núria",
            town = "Castelló de la Plana",
            title = "Xiques que anem a Cactus",
            description = "Xat intern per a xiques que vulguen quedar i anar juntes al concert.",
            members = setOf("Núria", "Clara")
        ),
        CommunityGroup(
            id = 6,
            concertId = 8,
            type = CommunityGroupType.VEG_FRIENDLY,
            creator = "Pau",
            town = "Vila-real",
            title = "Sopar vegà abans del concert",
            description = "Per trobar gent vegana o vegetariana i quedar per sopar abans d'entrar.",
            members = setOf("Pau")
        ),
        CommunityGroup(
            id = 7,
            concertId = 8,
            type = CommunityGroupType.LGBTQ_FRIENDLY,
            creator = "Aina",
            town = "Castelló de la Plana",
            title = "Grup LGTBIQ+ friendly",
            description = "Espai friendly per conéixer gent i anar al concert en grup.",
            members = setOf("Aina", "Vicent")
        ),
        CommunityGroup(
            id = 8,
            concertId = 9,
            type = CommunityGroupType.CAR,
            creator = "Mireia",
            town = "València",
            title = "Cotxe València → Sagunt",
            description = "Eixim de Benimaclet a les 21:45. Tenim 3 places i tornem després del concert.",
            capacity = 5,
            members = setOf("Mireia", "Andreu")
        ),
        CommunityGroup(
            id = 9,
            concertId = 4,
            type = CommunityGroupType.TRAIN,
            creator = "Clara",
            town = "València",
            title = "Tren València → Xàtiva",
            description = "Quedem a València Nord i agafem el tren de la vesprada per anar a El Diluvi.",
            members = setOf("Clara", "Laia", "Joan")
        ),
        CommunityGroup(
            id = 10,
            concertId = 2,
            type = CommunityGroupType.NEED_HOSTING,
            creator = "Vicent",
            town = "Gandia",
            title = "Busquem allotjament a Pego",
            description = "Som dues persones de la Safor i busquem lloc per dormir després del concert.",
            capacity = 4,
            members = setOf("Vicent", "Pau")
        ),
        CommunityGroup(
            id = 11,
            concertId = 3,
            type = CommunityGroupType.MEETUP,
            creator = "Joan",
            town = "Ontinyent",
            title = "Quedem abans d'Auxili",
            description = "Quedada a les 20:00 prop del Parc de Benarrai per sopar i entrar junts.",
            members = setOf("Joan", "Aina", "Marina")
        ),
        CommunityGroup(
            id = 12,
            concertId = 5,
            type = CommunityGroupType.WOMEN_ONLY,
            creator = "Marina",
            town = "València",
            title = "Xiques cap a Burjassot",
            description = "Grup per quedar a València, anar en metro i tornar juntes després de Tardor.",
            members = setOf("Marina", "Núria", "Mireia")
        )
    )

    val communityChatMessages = listOf(
        CommunityChatMessage(1, 1, "Marina", "Ens queden dues places al cotxe. Eixim d'Almassora a les 20:30.", "18:05"),
        CommunityChatMessage(2, 1, "Pau", "Perfecte, jo puge des d'Almassora. Puc quedar a la plaça.", "18:08"),
        CommunityChatMessage(3, 1, "Núria", "A mi em va bé. Porte 5 € per gasolina.", "18:10"),
        CommunityChatMessage(4, 2, "Joan", "Tren de Castelló a les 19:48. Quedem 15 min abans al vestíbul?", "18:11"),
        CommunityChatMessage(5, 2, "Aina", "Sí! Jo vaig amb motxilla roja perquè ens trobem fàcil.", "18:14"),
        CommunityChatMessage(6, 5, "Núria", "Podem quedar a l'estació i anar juntes fins al recinte.", "18:12"),
        CommunityChatMessage(7, 5, "Clara", "Genial. Jo arribe sobre les 20:00.", "18:15"),
        CommunityChatMessage(8, 6, "Pau", "He vist un lloc amb opcions veganes prop de l'auditori.", "18:16"),
        CommunityChatMessage(9, 7, "Aina", "Qui vulga pot quedar amb nosaltres abans d'entrar 🏳️‍🌈", "18:19"),
        CommunityChatMessage(10, 8, "Mireia", "Eixim de Benimaclet. Encara tenim tres seients lliures.", "19:02"),
        CommunityChatMessage(11, 8, "Andreu", "Jo torne amb vosaltres. Compartim gasolina sense problema.", "19:05"),
        CommunityChatMessage(12, 9, "Clara", "He mirat horaris: el tren ens deixa temps per sopar a Xàtiva.", "17:22"),
        CommunityChatMessage(13, 9, "Laia", "Perfecte, porte entrepà però podem prendre alguna cosa allí.", "17:26"),
        CommunityChatMessage(14, 10, "Vicent", "Si algú de Pego té sofà, som tranquils i només necessitem dormir.", "20:01"),
        CommunityChatMessage(15, 10, "Pau", "Jo també busque lloc. Si trobem hostal podem compartir habitació.", "20:04"),
        CommunityChatMessage(16, 11, "Joan", "Quedem a les 20:00. He posat el punt prop de l'entrada del parc.", "16:40"),
        CommunityChatMessage(17, 11, "Marina", "Arribaré un poc abans. Ens veiem allí.", "16:43"),
        CommunityChatMessage(18, 12, "Marina", "Podem quedar a Àngel Guimerà i anar totes juntes en metro.", "18:31"),
        CommunityChatMessage(19, 12, "Mireia", "A mi em va perfecte. Per tornar també anem juntes.", "18:35")
    )

    val chatMessages = listOf(
        ChatMessage(1, 8, "Cactus", "Portes a les 20:30. Ens veiem a Vila-real!", official = true, timestamp = "18:02"),
        ChatMessage(2, 8, "Pau", "Algú va des d’Almassora?", timestamp = "18:11"),
        ChatMessage(3, 8, "Marina", "Nosaltres tenim dues places lliures al cotxe.", timestamp = "18:14"),
        ChatMessage(4, 8, "Joan", "Hi ha un grup per anar en tren des de Castelló 👆", timestamp = "18:20"),
        ChatMessage(5, 9, "Romàntic Dimoni", "Canvi: l’obertura de portes serà a les 22:45.", official = true, timestamp = "17:32"),
        ChatMessage(6, 9, "Mireia", "Algú torna a València després?", timestamp = "17:40")
    )

    val concertComments = listOf(
        ConcertComment(1, 8, "Marina", "Tinc moltes ganes d'este concert 🔥", "ahir"),
        ConcertComment(2, 8, "Pau", "Algú sap si hi haurà taquilla?", "ahir"),
        ConcertComment(3, 8, "Cactus", "Sí, si queden entrades també hi haurà taquilla.", "fa 3 h"),
        ConcertComment(4, 9, "Mireia", "Ens veiem a Sagunt!", "fa 1 h"),
        ConcertComment(5, 3, "Joan", "Planàs per a dissabte.", "fa 2 h")
    )

    fun artistDemoPhotoUri(artist: String): String? = when (artist) {
        "La Fúmiga" -> "asset://demo/bands/la_fumiga.jpg"
        "La Gossa Sorda" -> "asset://demo/bands/la_gossa_sorda.jpg"
        "Auxili" -> "asset://demo/bands/auxili.jpg"
        "El Diluvi" -> "asset://demo/bands/el_diluvi.jpg"
        "Tardor" -> "asset://demo/bands/tardor.jpg"
        "Tito Pontet" -> "asset://demo/bands/tito_pontet.jpg"
        "Malifeta" -> "asset://demo/bands/malifeta.jpg"
        "Cactus" -> "asset://demo/bands/cactus.jpg"
        "Romàntic Dimoni" -> "asset://demo/bands/romantic_dimoni.jpg"
        "Maluks" -> "asset://demo/bands/maluks.jpg"
        else -> null
    }

    fun userDemoPhotoUri(displayName: String): String? = when (displayName) {
        "Pau" -> "asset://demo/users/pau.jpg"
        "Marina" -> "asset://demo/users/marina.jpg"
        "Núria" -> "asset://demo/users/nuria.jpg"
        "Joan" -> "asset://demo/users/joan.jpg"
        "Aina" -> "asset://demo/users/aina.jpg"
        "Vicent" -> "asset://demo/users/vicent.jpg"
        "Clara" -> "asset://demo/users/clara.jpg"
        "Laia" -> "asset://demo/users/laia.jpg"
        "Andreu" -> "asset://demo/users/andreu.jpg"
        "Mireia" -> "asset://demo/users/mireia.jpg"
        else -> null
    }

    val artists: List<String> = concerts.map { it.artist }.distinct().sorted()
    val towns: List<String> = concerts.map { it.town }.distinct().sorted()
    val comarques: List<String> = concerts.map { it.comarca }.distinct().sorted()
    val styles: List<String> = concerts.flatMap { it.styles }.distinct().sorted()
}
