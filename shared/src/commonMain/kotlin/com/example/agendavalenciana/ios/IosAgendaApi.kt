package com.example.agendavalenciana.ios

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.serialization.json.*
import com.example.agendavalenciana.model.IosSavedListInvitation
import com.example.agendavalenciana.model.IosAdminPermissions

/** API contract backed by the PHP endpoints bundled with the v336 server snapshot. */
interface IosSessionBridge {
    /** Implemented with iOS Keychain in Swift. Never use UserDefaults for API tokens. */
    fun loadToken(): String?
    fun saveToken(token: String)
    fun clearToken()
    fun readCache(key: String): String?
    fun writeCache(key: String, value: String)
    fun scheduleLocalReminderConfirmed(userId: Int, eventId: Int, title: String, isoDate: String, callback: IosReminderSchedulingCallback)
    fun cancelLocalReminder(userId: Int, eventId: Int)
    /** Shows a QR using the exact issued ticket code; never manufactures a new code. */
    fun showTicketQr(code: String, title: String)
    /** Opens AVFoundation QR scanner; scanned codes are validated only by the server. */
    fun showTicketScanner(eventId: Int)
    /** Opens iOS notification permission settings. */
    fun showNotificationSettings()
    /** Opens the native iOS share sheet for a saved-list invitation. */
    fun shareText(text: String)
    /** Ask iOS for remote notifications; registration with APNs happens asynchronously. */
    fun requestRemoteNotifications(callback: IosReminderSchedulingCallback)
    fun setPushOpenListener(listener: IosPushOpenCallback)
    /** Native identity-provider UI; ID token + nonce are verified by our server. */
    fun beginSocialSignIn(provider: String, callback: IosSocialAuthCallback)
    /** Photos are JPEG-compressed before crossing the KMP bridge, not kept in preferences. */
    fun pickProfilePhoto(callback: IosPickedPhotoCallback)
    fun eraseLocalAccountData(userId: Int)
}

interface IosSocialAuthCallback {
    fun onComplete(idToken: String, nonce: String, error: String)
}
interface IosPickedPhotoCallback {
    fun onComplete(base64Jpeg: String, error: String)
}

/** Completion is invoked only after Apple's notification center accepts or rejects the request. */
interface IosReminderSchedulingCallback {
    fun onResult(success: Boolean, message: String)
}

/** A tapped APNs artist alert opens the corresponding artist popup, including when resumed. */
interface IosPushOpenCallback { fun onOpenArtist(artist: String) }

/** The iOS bridge is installed before any Compose state is restored. */
object IosNativeBridge {
    var current: IosSessionBridge? = null
}

class IosAgendaApi(private val sessionBridge: IosSessionBridge? = null) {
    private val client = HttpClient {
        install(HttpTimeout) { requestTimeoutMillis = 15_000; connectTimeoutMillis = 10_000 }
    }
    private val base = "https://quimerafest.com/app/api/"
    // Not persisted in plaintext: session only survives for the lifetime of this process.
    var token: String? = sessionBridge?.loadToken()
        private set
    var account: IosAccount? = null
        private set

    private suspend fun request(path: String, data: JsonObject? = null, authenticated: Boolean = false): JsonObject {
        val bearer = token
        if (authenticated) require(!bearer.isNullOrEmpty()) { "Cal iniciar sessió." }
        val response = if (data == null) client.get(base + path) {
            if (!bearer.isNullOrEmpty()) header(HttpHeaders.Authorization, "Bearer $bearer")
        } else client.post(base + path) {
            contentType(ContentType.Application.Json)
            if (!bearer.isNullOrEmpty()) header(HttpHeaders.Authorization, "Bearer $bearer")
            setBody(data.toString())
        }
        val root = runCatching { Json.parseToJsonElement(response.bodyAsText()) as JsonObject }
            .getOrElse { error("El servidor ha retornat una resposta invàlida.") }
        require(root.bool("ok")) { root.text("message", "error").ifBlank { "HTTP ${response.status.value}" } }
        return root
    }
    suspend fun restoreAccount(): IosAccount? {
        if (token.isNullOrBlank()) return null
        return runCatching {
            val root = request("auth/me.php", authenticated = true)
            val user = root["user"] as? JsonObject ?: error("La sessió no és vàlida")
            IosAccount(user.text("id").toIntOrNull() ?: 0, user.text("username"), user.text("email"), user.text("role"))
                .also { account = it }
        }.getOrElse {
            // Network outages must not erase a valid keychain token. The app will retry next launch.
            null
        }
    }
    /** Generic server response avoids revealing whether an account exists. */
    suspend fun requestPasswordReset(email: String): String {
        require(email.contains("@")) { "Introduïx un correu electrònic vàlid." }
        val response = request("auth/request-password-reset.php", buildJsonObject { put("email", email.trim()) })
        return response.text("message").ifBlank { "Si existix el compte, rebràs instruccions per correu." }
    }
    suspend fun signIn(login: String, password: String): IosAccount {
        val root = request("auth/login.php", buildJsonObject {
            put("login", login.trim()); put("password", password); put("device_name", "iOS l'Agenda KMP")
        })
        val newToken = root.text("token")
        require(newToken.isNotBlank()) { "Falta el token de la sessió." }
        val user = root["user"] as? JsonObject ?: error("Falta el compte.")
        token = newToken
        sessionBridge?.saveToken(newToken)
        return IosAccount(user.text("id").toIntOrNull() ?: 0, user.text("username"), user.text("email"), user.text("role")).also { account = it }
    }
    suspend fun registerAccount(username: String, email: String, password: String): IosAccount {
        require(username.trim().length in 3..80) { "El nom d'usuari ha de tindre entre 3 i 80 caràcters." }
        require("@" in email && "." in email.substringAfter("@")) { "Correu electrònic no vàlid." }
        require(password.length >= 8) { "La contrasenya ha de tindre almenys 8 caràcters." }
        val response = request("auth/register.php", buildJsonObject {
            put("username", username.trim())
            put("email", email.trim())
            put("password", password)
            put("device_name", "iOS l'Agenda KMP")
        })
        val newToken = response.text("token")
        require(newToken.isNotEmpty()) { "No s'ha rebut una sessió vàlida." }
        val person = response["user"] as? JsonObject ?: error("Falten dades del compte.")
        token = newToken
        sessionBridge?.saveToken(newToken)
        return IosAccount(person.text("id").toIntOrNull() ?: 0, person.text("username"), person.text("email"), person.text("role"))
            .also { account = it }
    }
    /** No social provider credential is accepted as our own API bearer token. */
    suspend fun signInWithProvider(provider: String, idToken: String, nonce: String): IosAccount {
        val path = when (provider.uppercase()) {
            "APPLE" -> "auth/apple-ios.php"
            "GOOGLE" -> "auth/google-ios.php"
            else -> error("Proveïdor no admés")
        }
        require(idToken.length > 100 && nonce.length in 16..256) { "Credencial d'identitat incompleta" }
        val root = request(path, buildJsonObject {
            put("id_token", idToken); put("nonce", nonce); put("device_name", "iOS l'Agenda KMP")
        })
        val newToken = root.text("token")
        require(newToken.length >= 32) { "El servidor no ha creat una sessió vàlida." }
        val user = root["user"] as? JsonObject ?: error("El servidor no ha retornat un compte")
        val result = IosAccount(user.text("id").toIntOrNull() ?: 0, user.text("username"), user.text("email"), user.text("role"))
        require(result.id > 0) { "Identificador de compte invàlid" }
        token = newToken
        sessionBridge?.saveToken(newToken)
        account = result
        return result
    }

    suspend fun myPublicProfile(): IosMyProfile {
        val root = request("account/profile-ios.php", authenticated = true)
        val profile = root["profile"] as? JsonObject ?: error("No s'ha rebut el perfil")
        return IosMyProfile(profile.text("display_name"), profile.text("bio"),
            resolveProfileImage(profile.text("image_url")))
    }
    suspend fun updateMyPublicProfile(displayName: String, bio: String = "", removeImage: Boolean = false): IosMyProfile {
        val username = account?.username ?: error("No hi ha compte")
        require(displayName.trim().length in 1..190) { "Nom de perfil invàlid" }
        val root = request("account/profile-ios-edit.php", buildJsonObject {
            put("display_name", displayName.trim())
            put("bio", bio.take(3000)); put("remove_image", removeImage)
        }, authenticated = true)
        val profile = root["profile"] as? JsonObject ?: error("No s'ha rebut el perfil actualitzat")
        return IosMyProfile(profile.text("display_name"), profile.text("bio"),
            resolveProfileImage(profile.text("image_url")))
    }
    suspend fun uploadMyPhoto(base64Jpeg: String): String {
        val username = account?.username ?: error("No hi ha compte")
        require(base64Jpeg.length in 100..8_000_000) { "Imatge massa gran o buida" }
        val root = request("images/upload.php", buildJsonObject {
            put("owner_type", "USER"); put("owner_key", username)
            put("image_base64", base64Jpeg); put("mime_type", "image/jpeg")
        }, authenticated = true)
        return resolveProfileImage(root.text("image_url"))
    }
    private fun resolveProfileImage(raw: String): String = when {
        raw.startsWith("https://") -> raw
        raw.startsWith("/") -> "https://quimerafest.com" + raw
        raw.startsWith("images/") -> "https://quimerafest.com/app/" + raw
        else -> ""
    }
    /** Requires an explicit phrase and, for password accounts, a recent password recheck on the server. */
    suspend fun deleteMyAccount(confirmation: String, password: String) {
        require(confirmation == "ELIMINAR") { "Cal confirmar la baixa escrivint ELIMINAR." }
        request("account/delete-ios.php", buildJsonObject {
            put("confirmation", confirmation)
            put("password", password)
        }, authenticated = true)
        // Only clear the Keychain after the server confirms irreversible deactivation.
        signOut()
    }
    fun signOut() { token = null; account = null; sessionBridge?.clearToken() }
    /** Revoke this phone's APNs token before invalidating the session. */
    private suspend fun unregisterRemoteDevice() {
        val deviceToken = platformReadPublicCache("apns_device_token")
        if (deviceToken.isNotBlank()) request("notifications/apns-device.php", buildJsonObject {
            put("action", "unregister"); put("device_token", deviceToken)
        }, authenticated = true)
    }
    /** Revokes the server token when online; offline logout still destroys its local copy. */
    suspend fun signOutSecurely() {
        try {
            if (!token.isNullOrBlank()) {
                runCatching { unregisterRemoteDevice() }
                request("auth/logout.php", buildJsonObject { }, authenticated = true)
            }
        } finally { signOut() }
    }

    /** Public event reports are accepted for guests too. A stable device-scoped ID is required
     * by the existing Apps Script report workflow; it is not a login credential. */
    suspend fun reportEvent(event: AgendaEventSummary, reason: String, details: String) {
        val accepted = setOf("ANIMAL_ABUSE", "HUMAN_RIGHTS", "HATE_DISCRIMINATION", "INCITEMENT_VIOLENCE",
            "CANCELLED", "INCORRECT_INFO", "DUPLICATE", "NOT_CULTURAL", "POSSIBLE_SCAM", "INAPPROPRIATE_CONTENT", "OTHER")
        require(reason in accepted) { "Motiu del report no admés" }
        var guestId = platformReadPublicCache("ios_report_guest_id")
        if (!guestId.matches(Regex("[A-Za-z0-9-]{16,80}"))) {
            // Persistent anonymous installation identifier (not a security token).
            val digits = "0123456789abcdef"
            guestId = buildString { repeat(32) { append(digits[kotlin.random.Random.nextInt(digits.length)]) } }
            platformWritePublicCache("ios_report_guest_id", guestId)
        }
        request("events/report.php", buildJsonObject {
            put("event_id", event.id)
            put("event_title", event.title.take(255))
            put("event_town", event.town.take(190))
            put("event_date", event.date.take(10))
            put("event_time", event.time.take(5))
            put("venue", event.venue.take(255))
            put("artists", event.artists.joinToString(", ").take(1000))
            put("reason", reason)
            put("details", details.trim().take(1500))
            put("guest_id", guestId)
        })
    }

    /** Named saved lists: same authenticated collections API used by the existing web app. */
    /** The exact public event-detail endpoint used by Android for festival sessions.
     * Fetch on popup opening, never block the initial feed or invent missing days. */
    suspend fun festivalSchedule(eventId: Int): List<AgendaScheduleEntry> {
        require(eventId > 0) { "Identificador d'esdeveniment invàlid" }
        val root = request("special-events/public.php?event_id=$eventId")
        return IosFestivalScheduleDecoder.decode(root)
    }

    suspend fun savedLists(): List<IosSavedList> = request("collections/index.php?action=list", authenticated = true)
        .items("lists").mapNotNull { item ->
            val id = item.text("id").toIntOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
            IosSavedList(id, item.text("name"), item.bool("owner"), item.text("invite_url"),
                item.text("share_code"), item.arr("event_ids").mapNotNull { (it as? JsonPrimitive)?.intOrNull }.toSet())
        }
    private suspend fun listOperation(action: String, listId: Int? = null, eventId: Int? = null,
                                      name: String? = null, add: Boolean? = null, invite: String? = null): JsonObject =
        request("collections/index.php", buildJsonObject {
            put("action", action)
            if (listId != null) put("list_id", listId)
            if (eventId != null) put("event_id", eventId)
            if (name != null) put("name", name)
            if (add != null) put("add", add)
            if (invite != null) put("invite", invite)
        }, authenticated = true)
    suspend fun createSavedList(name: String, firstEventId: Int? = null) {
        require(name.trim().length in 1..100) { "El nom ha de tindre entre 1 i 100 caràcters." }
        listOperation("create_list", eventId = firstEventId, name = name.trim())
    }
    suspend fun renameSavedList(id: Int, name: String) {
        require(name.trim().length in 1..100) { "El nom ha de tindre entre 1 i 100 caràcters." }
        listOperation("rename_list", listId = id, name = name.trim())
    }
    suspend fun deleteSavedList(id: Int) { listOperation("delete_list", listId = id) }
    suspend fun changeSavedListEvent(id: Int, eventId: Int, add: Boolean) {
        listOperation("list_event", listId = id, eventId = eventId, add = add)
    }
    suspend fun shareSavedList(id: Int): IosSavedListShare {
        val response = listOperation("share_list", listId = id)
        return IosSavedListShare(response.text("invite_url"), response.text("share_code"))
    }
    suspend fun joinSavedList(code: String) {
        val clean = IosSavedListInvitation.parse(code)
            ?: error("El codi o enllaç d'invitació no és vàlid.")
        listOperation("join_list", invite = clean)
    }
    suspend fun leaveSavedList(id: Int) { listOperation("leave_list", listId = id) }

    /** Named search filters live on the authenticated server account, not just this device. */
    suspend fun savedSearches(): List<IosSavedSearch> =
        request("preferences/search-filters.php", authenticated = true).items("presets").mapNotNull { item ->
            val name = item.text("name").takeIf(String::isNotBlank) ?: return@mapNotNull null
            val filter = item["filters"] as? JsonObject ?: return@mapNotNull null
            IosSavedSearch(name, filter.text("town"), filter.names("categories"))
        }
    suspend fun saveTownSearch(name: String, town: String, categories: List<String> = emptyList()) {
        require(name.trim().length in 1..52) { "El nom del filtre ha de tindre entre 1 i 52 caràcters." }
        require(town.trim().length in 1..120) { "Tria una població abans de guardar el filtre." }
        request("preferences/search-filters.php", buildJsonObject {
            put("action", "save")
            put("name", name.trim())
            put("filters", buildJsonObject { put("town", town.trim()); put("categories", JsonArray(categories.map { JsonPrimitive(it) })) })
        }, authenticated = true)
    }
    suspend fun removeSavedSearch(name: String) {
        request("preferences/search-filters.php", buildJsonObject {
            put("action", "delete"); put("name", name)
        }, authenticated = true)
    }

    suspend fun profileSettings(): IosProfileSettings {
        val r = request("account/profile-settings.php", authenticated = true)
        return IosProfileSettings(r.text("role"), r.text("phone"), r.bool("email_verified"))
    }
    suspend fun saveProfileSettings(role: String, phone: String): IosProfileSettings {
        require(role in listOf("PUBLIC", "PROFESSIONAL")) { "Tipus de compte no vàlid." }
        if (role == "PROFESSIONAL") {
            require(phone.trim().matches(Regex("[+0-9() .-]{7,24}"))) { "Indica un telèfon professional vàlid." }
        }
        val r = request("account/profile-settings.php", buildJsonObject {
            put("role", role); put("phone", if (role == "PROFESSIONAL") phone.trim() else "")
        }, authenticated = true)
        return IosProfileSettings(r.text("role"), r.text("phone"), r.bool("email_verified"))
    }

    suspend fun userState(): IosUserState {
        val root = request("user-state/mine.php", authenticated = true)
        return IosUserState(
            follows = root.items("follows").map { IosFollow(it.text("entity_type"), it.text("entity_key"), it.text("entity_label")) },
            saved = root.items("event_states").filter { it.bool("saved") }.mapNotNull { it.text("event_id").toIntOrNull() }.toSet(),
            alerts = root.arr("event_alerts").mapNotNull { (it as? JsonPrimitive)?.intOrNull }.toSet()
        )
    }
    suspend fun saveEvent(event: AgendaEventSummary, saved: Boolean) = request("events/save.php", buildJsonObject {
        put("event_id", event.id); put("entity_label", event.title); put("saved", saved)
    }, authenticated = true)
    suspend fun alertEvent(event: AgendaEventSummary, active: Boolean) = request("user-state/event-alert.php", buildJsonObject {
        put("event_id", event.id); put("entity_label", event.title); put("active", active)
    }, authenticated = true)
    /** Same authenticated endpoint and entity types used by the Android client. */
    suspend fun followEntity(type: String, entityKey: String, entityLabel: String, following: Boolean) {
        require(type in setOf("ARTIST", "ORGANIZER", "VENUE", "CATEGORY")) { "Tipus de seguiment invàlid." }
        require(entityKey.isNotBlank() && entityLabel.isNotBlank()) { "Falta l'entitat." }
        request("user-state/follow.php", buildJsonObject {
            put("entity_type", type); put("entity_key", entityKey.trim())
            put("entity_label", entityLabel.trim()); put("following", following)
        }, authenticated = true)
    }
    suspend fun followArtist(artist: String, following: Boolean) =
        followEntity("ARTIST", artist.trim(), artist.trim(), following)
    /** Artist bell subscription is independent of following. Preferences are per account on the server. */
    suspend fun artistBells(): List<IosArtistBell> = request("notifications/artist-bell.php", authenticated = true)
        .items("artists").mapNotNull { obj ->
            val name = obj.text("artist_name").trim()
            if (name.isBlank()) null else IosArtistBell(name,
                IosArtistBellPolicy.parseFrequency(obj.text("frequency")), obj.bool("enabled"))
        }
    suspend fun saveArtistBell(artist: String, enabled: Boolean, frequency: String): IosArtistBell {
        val name = artist.trim()
        require(name.isNotEmpty() && name.length <= 190) { "Nom d'artista no vàlid." }
        require(frequency in IosArtistBellPolicy.frequencies) { "Freqüència no vàlida." }
        val result = request("notifications/artist-bell.php", buildJsonObject {
            put("artist_name", name); put("enabled", enabled); put("frequency", frequency)
        }, authenticated = true)
        return IosArtistBell(result.text("artist_name").ifBlank { name },
            IosArtistBellPolicy.parseFrequency(result.text("frequency")), result.bool("enabled"))
    }
    suspend fun notifications(): List<IosNotification> {
        val main = request("notifications/mine.php", authenticated = true)
            .items("notifications").map { IosNotification(it.text("id").toIntOrNull() ?: 0,
                it.text("title"), it.text("body"), it.text("read_at").isNotBlank(), it.text("created_at"),
                it.text("deep_link_type"), it.text("deep_link_value")) }
        // Artist alerts are a separate inbox; negative IDs distinguish them from campaign messages.
        val artist = runCatching { request("notifications/artist-inbox.php", authenticated = true)
            .items("notifications").mapNotNull { item ->
                val id = item.text("id").toIntOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
                IosNotification(-id, item.text("title"), item.text("body"),
                    item.text("read_at").isNotBlank(), item.text("created_at"),
                    "NEW_ARTIST_EVENT", item.text("artist_name"))
            }
        }.getOrDefault(emptyList())
        return (main + artist).sortedByDescending { it.createdAt }
    }
    suspend fun markRead(id: Int) {
        if (id < 0) request("notifications/artist-inbox.php", buildJsonObject {
            put("id", -id)
        }, authenticated = true)
        else request("notifications/read.php", buildJsonObject { put("id", id) }, authenticated = true)
    }
    suspend fun ticketOptions(eventId: Int): List<IosTicketOption> =
        request("tickets/options.php?event_id=$eventId").items("options").map { option ->
            IosTicketOption(
                id = option.text("id").toIntOrNull() ?: 0,
                name = option.text("name"), price = option.text("price").toDoubleOrNull() ?: 0.0,
                remaining = option.text("remaining").toIntOrNull(),
                maxPerOrder = option.text("max_per_order").toIntOrNull() ?: 1,
                sessionDate = option.text("session_date"), sessionTime = option.text("session_time")
            )
        }
    /** The existing backend issues tickets only when the selected option is free.
     * Never create a paid order here without an integrated, confirmed payment. */
    suspend fun reserveFreeTicket(eventId: Int, optionId: Int, quantity: Int): List<IosTicket> {
        require(quantity in 1..6) { "Quantitat no admesa" }
        val root = request("tickets/purchase.php", buildJsonObject {
            put("event_id", eventId); put("ticket_option_id", optionId); put("quantity", quantity)
        }, authenticated = true)
        return root.items("tickets").map { t ->
            IosTicket(t.text("id"), t.text("event_id"), "", t.text("status"), t.text("code"))
        }
    }
    /** Validates only through the privileged server endpoint; not an offline QR validation. */
    suspend fun validateTicket(eventId: Int, code: String): String {
        val root = request("tickets/validate.php", buildJsonObject {
            put("event_id", eventId); put("code", code)
        }, authenticated = true)
        val outcome = root.text("outcome").uppercase()
        val message = root.text("message").ifBlank { outcome }
        // A HTTP 200 and ok=true may still report NOT_FOUND, ALREADY_USED or CANCELLED.
        return if (outcome == "VALIDATED") "Entrada validada. $message" else "Entrada NO validada ($outcome). $message"
    }
    suspend fun createCommunityGroup(eventId: Int, title: String, type: String, open: Boolean,
                                     description: String = "", origin: String = "", meetingAt: String = "",
                                     capacity: Int = 0): IosCommunityGroup {
        val root = request("community/create.php", buildJsonObject {
            put("event_id", eventId); put("title", title.trim()); put("type", type)
            put("access_mode", if (open) "OPEN" else "APPROVAL")
            put("description", description.trim().take(1000)); put("origin_label", origin.trim().take(190))
            if (meetingAt.isNotBlank()) put("meeting_at", meetingAt.trim())
            if (capacity > 0) put("capacity", capacity.coerceAtMost(100))
        }, authenticated = true)
        val g = root["group"] as? JsonObject ?: error("El servidor no ha retornat el tauló.")
        return decodeCommunityGroup(g)
    }
    suspend fun updateCommunityGroup(g: IosCommunityGroup, title: String, description: String,
                                     origin: String, meeting: String, capacity: Int,
                                     type: String, access: String): IosCommunityGroup {
        val root = request("community/update.php", buildJsonObject {
            put("group_id",g.id); put("title",title.trim()); put("description",description.trim())
            put("origin_label",origin.trim()); put("meeting_at",meeting.trim())
            if(capacity>0) put("capacity",capacity.coerceAtMost(100)) else put("capacity", JsonNull)
            put("type",type);put("access_mode",access)
        }, authenticated=true)
        return decodeCommunityGroup(root["group"] as? JsonObject ?: error("Falten dades del tauló"))
    }
    suspend fun setCommunityStatus(groupId:Int, status:String) {
        require(status in setOf("OPEN","CLOSED","CANCELLED"))
        request("community/status.php",buildJsonObject{put("group_id",groupId);put("status",status)},authenticated=true)
    }
    suspend fun reportCommunityMessage(groupId: Int, messageId: Int, reason: String) {
        require(groupId>0 && messageId>0 && reason.isNotBlank())
        request("community/report.php", buildJsonObject {
            put("group_id",groupId);put("message_id",messageId);put("reason",reason.trim().take(500))
        }, authenticated=true)
    }
    suspend fun reportCommunityGroup(groupId:Int,reason:String) {
        require(groupId>0 && reason.isNotBlank())
        request("community/report.php",buildJsonObject {put("group_id",groupId);put("reason",reason.trim().take(500))},authenticated=true)
    }
    suspend fun community(eventId: Int? = null, mine: Boolean = false): List<IosCommunityGroup> {
        val args = listOfNotNull(eventId?.let { "event_id=$it" }, if (mine) "mine=1" else null)
        val path = "community/groups.php" + if (args.isEmpty()) "" else "?"+args.joinToString("&")
        return request(path,authenticated=true).items("groups").map(::decodeCommunityGroup)
    }
    suspend fun pendingRequests(groupId: Int): List<IosMemberRequest> =
        request("community/requests.php?group_id=$groupId", authenticated = true).items("requests").map {
            IosMemberRequest(it.text("user_id").toIntOrNull() ?: 0, it.text("display_name"), it.text("requested_at"))
        }
    suspend fun respondToMember(groupId: Int, userId: Int, accept: Boolean) {
        request("community/respond.php", buildJsonObject {
            put("group_id", groupId); put("user_id", userId)
            put("action", if (accept) "ACCEPT" else "REJECT")
        }, authenticated = true)
    }
    suspend fun joinGroup(id: Int): String = request("community/join.php", buildJsonObject {put("group_id",id)},authenticated=true)
        .text("membership_status")
    suspend fun leaveGroup(id: Int) {request("community/leave.php",buildJsonObject{put("group_id",id)},authenticated=true)}
    suspend fun messages(id: Int): List<IosMessage> = request("community/messages.php?group_id=$id", authenticated = true)
        .items("messages").map { IosMessage(it.text("id"),it.text("author"),it.text("body","text"),
            it.text("created_at"),it.text("user_id").toIntOrNull() ?: 0) }
    suspend fun sendMessage(id: Int, body: String) {
        require(body.isNotBlank() && body.length<=2000)
        request("community/send.php",buildJsonObject{put("group_id",id);put("text",body.trim())},authenticated=true)
    }
    /** Blocking and moderation endpoints require SERVIDOR_COMUNITAT SQL+PHP patch. */
    suspend fun blockedCommunityUsers(): List<IosCommunityUser> =
        request("community/blocks.php",authenticated=true).items("blocked").map {
            IosCommunityUser(it.text("user_id").toIntOrNull() ?: 0,it.text("display_name"))
        }
    suspend fun setCommunityBlocked(userId:Int,blocked:Boolean) {
        require(userId>0)
        request("community/blocks.php",buildJsonObject {put("user_id",userId)
            put("action",if(blocked) "BLOCK" else "UNBLOCK")},authenticated=true)
    }
    suspend fun communityManageMembers(groupId:Int): IosCommunityManagement {
        val root=request("community/manage.php?group_id=$groupId",authenticated=true)
        return IosCommunityManagement(root.items("members").map {
            IosCommunityMember(it.text("user_id").toIntOrNull()?:0,it.text("display_name"),it.text("role"),it.text("status"))
        },root.items("bans").map {IosCommunityUser(it.text("user_id").toIntOrNull()?:0,it.text("display_name"))})
    }
    suspend fun moderateCommunityGroup(groupId:Int,action:String,userId:Int=0,messageId:Int=0) {
        require(action in setOf("DELETE_MESSAGE","BAN_MEMBER","UNBAN_MEMBER"))
        request("community/manage.php",buildJsonObject {put("group_id",groupId);put("action",action)
            if(userId>0) put("user_id",userId)
            if(messageId>0) put("message_id",messageId)},authenticated=true)
    }
    suspend fun communityReports(all:Boolean=false):List<IosCommunityReport> =
        request("community/admin-reports.php?status="+if(all) "ALL" else "OPEN",authenticated=true)
            .items("reports").map {IosCommunityReport(
                it.text("id").toIntOrNull()?:0,it.text("group_id").toIntOrNull()?:0,
                it.text("message_id").toIntOrNull()?:0,it.text("reported_user_id").toIntOrNull()?:0,
                it.text("group_title"),it.text("reported_name"),it.text("reason"),it.text("message_body"),it.text("status")) }
    suspend fun resolveCommunityReport(reportId:Int,action:String) {
        require(action in setOf("DISMISS","REVIEW","DELETE_MESSAGE","CLOSE_GROUP","BAN_MEMBER"))
        request("community/admin-reports.php",buildJsonObject {put("report_id",reportId);put("action",action)},authenticated=true)
    }
    /** Followed artists are sorted first by the server. Walk subsequent pages only when
     *  the first 50 rows do not contain six DISTINCT followed artists.
     *  A bounded scan avoids mistaking 50 publications by one artist for 50 Batecs. */
    suspend fun batecs(followedArtists: Set<String> = emptySet()): List<IosBatec> {
        val followed = followedArtists.map { it.trim().lowercase() }.filter(String::isNotBlank).toSet()
        val collected = linkedMapOf<String, IosBatec>()
        var offset = 0
        repeat(if (followed.isEmpty()) 1 else 4) {
            val root = request("batecs/index.php?offset=$offset&limit=50")
            val items = root.items("batecs").mapNotNull {
                val id = it.text("id").toIntOrNull()?.takeIf { n -> n > 0 } ?: return@mapNotNull null
                IosBatec(id.toString(), it.text("event_id").toIntOrNull() ?: 0,
                    it.text("event_artist", "artist_name"), it.text("event_title", "title"),
                    it.text("body", "caption", "text"),
                    validMediaUrl(it.text("image_url", "media_url")),
                    it.text("story_key").ifBlank { "manual:$id" })
            }
            items.forEach { if (it.storyKey.isNotBlank()) collected.putIfAbsent(it.storyKey, it) }
            val covered = collected.values.map { it.artist.trim().lowercase() }.filter { it in followed }.distinct().size
            if (!root.bool("has_more") || items.isEmpty() || (followed.isNotEmpty() && covered >= 6))
                return collected.values.toList()
            offset = root.text("next_offset").toIntOrNull()?.takeIf { it > offset }
                ?: (offset + items.size)
        }
        return collected.values.toList()
    }
    /** Seen state is account-specific, server authoritative, and never marked for guest users. */
    suspend fun seenBatecs(): Set<String> = request("batecs/seen.php", authenticated = true)
        .arr("seen_keys").mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
        .filter { it.matches(Regex("(?:manual|event):\\d+")) }.toSet()
    suspend fun markBatecSeen(storyKey: String) {
        require(storyKey.matches(Regex("(?:manual|event):\\d+"))) { "Identificador de batec no vàlid" }
        request("batecs/seen.php", buildJsonObject { put("story_key", storyKey) }, authenticated = true)
    }
    /** Public entity metadata, matching the source already used by the Android recommendation engine. */
    suspend fun entityStyles(): JsonObject = request("entities/images.php")

    suspend fun promoted(): IosPromoted = decodePromoted(request("ads/active-global.php"))

    /** Same public ads/track.php endpoint and payload used by Android.
     * Call only when the actual sponsored creative is displayed or opened. */
    suspend fun trackAdvertisement(ad: IosAd, click: Boolean) {
        require(ad.id.toIntOrNull()?.let { it > 0 } == true) { "Anunci invàlid" }
        request("ads/track.php", buildJsonObject {
            put("ad_id", ad.id.toInt())
            put("placement", ad.placement)
            put("action", if (click) "click" else "impression")
        })
    }

    /** Read-only administration: each route enforces SUPER_ADMIN in PHP. */
    suspend fun adminDashboard(days: Int = 30): JsonObject =
        request("admin/dashboard.php?days=${days.coerceIn(1, 365)}", authenticated = true)

    suspend fun adminUsers(): List<JsonObject> =
        request("admin/users.php", authenticated = true).items("users")

    suspend fun adminUserDetail(userId: Int): JsonObject {
        require(userId > 0)
        return request("admin/user-detail.php?user_id=$userId&days=90", authenticated = true)
    }

    /** Requires deploying SERVIDOR_SUPERADMIN_IOS; does not edit profile fields. */
    suspend fun adminSetUserAccess(userId: Int, role: String, active: Boolean): JsonObject {
        require(userId > 0 && IosAdminPermissions.validRole(role))
        require(account?.role == "SUPER_ADMIN")
        require(account?.id != userId) { "No pots canviar els teus propis permisos." }
        return request("admin/user-access-ios.php", buildJsonObject {
            put("user_id", userId); put("role", role); put("active", active)
            put("confirmation", "CANVIAR ACCES")
        }, authenticated = true)
    }

    suspend fun adminFindEvents(query: String, page: Int = 1): JsonObject =
        request("admin/events.php?q=${query.trim().take(180).encodeURLParameter()}&page=${page.coerceIn(1, 10000)}&limit=25", authenticated = true)

    /** Existing server endpoint anonymizes account; it never hard-deletes fiscal records. */
    suspend fun adminDeleteUser(userId: Int, username: String): JsonObject {
        require(userId > 0 && username.isNotBlank() && userId != account?.id)
        return request("admin/user-delete.php", buildJsonObject {
            put("user_id", userId); put("confirmation", "ELIMINAR $username")
        }, authenticated = true)
    }

    suspend fun adminEventReports(): List<JsonObject> =
        request("admin/reports.php", authenticated = true).items("reports")

    suspend fun adminCommunityPending(): List<JsonObject> =
        request("events/community-pending.php", authenticated = true).items("items")

    suspend fun adminReviewCommunity(eventId: Int, decision: String, notes: String) {
        require(eventId > 0 && decision in setOf("APPROVE", "REJECT"))
        if (decision == "REJECT") require(notes.trim().length >= 12)
        request("events/community-review.php", buildJsonObject {
            put("event_id", eventId); put("decision", decision); put("notes", notes.trim().take(2000))
        }, authenticated = true)
    }

    suspend fun adminModerateEvent(eventId: Int, action: String, reason: String) {
        require(eventId > 0 && action in setOf("APPROVE", "REMOVE_CONFIRMED", "RESTORE"))
        if (action == "REMOVE_CONFIRMED") require(reason.trim().length >= 12)
        request("moderation/review.php", buildJsonObject {
            put("event_id", eventId); put("action", action); put("reason", reason.trim().take(2000))
        }, authenticated = true)
    }

    suspend fun adminPreviewTargets(filters: JsonObject): JsonObject =
        request("admin/notifications-preview.php", buildJsonObject { put("filters", filters) }, authenticated = true)

    suspend fun adminSendTargets(title: String, body: String, count: Int, filters: JsonObject): JsonObject {
        require(title.trim().isNotBlank() && body.trim().isNotBlank() && count > 0)
        return request("admin/notifications-send.php", buildJsonObject {
            put("filters", filters)
            put("title", title.trim().take(160)); put("body", body.trim().take(1200))
            put("expected_count", count); put("confirmation", "ENVIAR")
        }, authenticated = true)
    }

    suspend fun adminPreview(): Int {
        val root = request("admin/notifications-preview.php", buildJsonObject {
            put("filters", buildJsonObject { put("all_users", true); put("platform", "") })
        }, authenticated = true)
        return root.text("count").toIntOrNull() ?: 0
    }
    suspend fun adminSend(title: String, body: String, count: Int) = request("admin/notifications-send.php", buildJsonObject {
        put("filters", buildJsonObject { put("all_users", true); put("platform", "") })
        put("title", title.take(160)); put("body", body.take(1200)); put("expected_count", count); put("confirmation", "ENVIAR")
    }, authenticated = true)
}

data class IosAccount(val id: Int, val username: String, val email: String, val role: String)
data class IosFollow(val type: String, val key: String, val label: String)
data class IosUserState(val follows: List<IosFollow> = emptyList(), val saved: Set<Int> = emptySet(), val alerts: Set<Int> = emptySet()) {
    fun follows(artist: String) = followsType("ARTIST", artist)
    fun followsType(type: String, name: String): Boolean = name.isNotBlank() && follows.any {
        it.type.equals(type, true) && (it.label.equals(name, true) || it.key.equals(name, true))
    }
}
data class IosNotification(val id: Int, val title: String, val body: String, val read: Boolean, val createdAt: String, val linkType: String, val linkValue: String)
data class IosTicket(val id: String, val eventId: String, val eventTitle: String, val status: String, val code: String)
data class IosCommunityGroup(val id: Int, val eventId: Int, val title: String, val type: String,
    val access: String, val membership: String, val description: String, val creatorUserId: Int = 0,
    val status: String = "OPEN", val origin: String = "", val meeting: String = "", val capacity: Int = 0,
    val participants: Int = 0, val creatorName: String = "")
data class IosCommunityUser(val id: Int, val name: String)
data class IosCommunityMember(val id:Int,val name:String,val role:String,val status:String)
data class IosCommunityManagement(val members:List<IosCommunityMember>,val bans:List<IosCommunityUser>)
data class IosCommunityReport(val id:Int,val groupId:Int,val messageId:Int,val reportedUserId:Int,
    val groupTitle:String,val reportedName:String,val reason:String,val messageText:String,val status:String)
internal fun decodeCommunityGroup(g: JsonObject): IosCommunityGroup = IosCommunityGroup(
    g.text("id").toIntOrNull()?:0,g.text("event_id").toIntOrNull()?:0,g.text("title","name"),
    g.text("type","group_type"),g.text("access_mode"),g.text("membership_status","my_status"),
    g.text("description"),g.text("creator_user_id").toIntOrNull()?:0,g.text("status").ifBlank{"OPEN"},
    g.text("origin_label"),g.text("meeting_at"),g.text("capacity").toIntOrNull()?:0,
    g.text("participant_count").toIntOrNull()?:0,g.text("creator_name"))
data class IosMemberRequest(val userId: Int, val displayName: String, val requestedAt: String)
data class IosMessage(val id: String, val author: String, val body: String, val createdAt: String, val userId:Int=0)
data class IosBatec(val id: String, val eventId: Int, val artist: String, val eventTitle: String, val text: String, val imageUrl: String, val storyKey: String = "")
data class IosAd(val id: String, val eventId: Int, val placement: String, val imageUrl: String,
                     val focusX: Float = 0.5f, val focusY: Float = 0.5f)
data class IosPromoted(val guaranteedEventIds: Set<Int> = emptySet(), val ads: List<IosAd> = emptyList(),
                       val priorityEvents: List<AgendaEventSummary> = emptyList())

/** Values come from tickets/options.php, not calculated from the event card. */
data class IosTicketOption(val id: Int, val name: String, val price: Double, val remaining: Int?, val maxPerOrder: Int, val sessionDate: String, val sessionTime: String)

/** Owner is authoritative: invited list members cannot edit another user's list. */
data class IosSavedList(val id: Int, val name: String, val owner: Boolean,
                        val inviteUrl: String, val shareCode: String, val eventIds: Set<Int>)
data class IosSavedListShare(val inviteUrl: String, val shareCode: String)
data class IosProfileSettings(val role: String, val phone: String, val emailVerified: Boolean)

/** Only supported server fields are decoded. */
data class IosSavedSearch(val name: String, val town: String, val categories: List<String> = emptyList())

/** Public server response. Every priority array is included, including internal ticket sales
 * without orders. Never apply affinity/search/exclusion filters to advertising inventory. */
internal fun decodePromoted(root: JsonObject): IosPromoted {
    val ids = (listOf("priority_event_ids", "platform_ticket_event_ids", "paid_ticket_event_ids")
        .flatMap { root.arr(it).mapNotNull { value -> (value as? JsonPrimitive)?.intOrNull } } +
        root.items("events").mapNotNull { it.text("event_id", "id").toIntOrNull() } +
        root.items("priority_events").mapNotNull { it.text("event_id", "id").toIntOrNull() })
        .filter { it > 0 }.toSet()
    val ads = root.items("ads").mapNotNull { obj ->
        val eventId = obj.text("event_id").toIntOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
        val adId = obj.text("id").toIntOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
        val placement = obj.text("placement").uppercase()
        if (placement !in setOf("HEADER", "FEED_PREMIUM", "FEED_NORMAL", "POPUP")) return@mapNotNull null
        IosAd(adId.toString(), eventId, placement, validMediaUrl(obj.text("image_url")),
            obj.text("card_focus_x").toFloatOrNull()?.coerceIn(0f,1f) ?: .5f,
            obj.text("card_focus_y").toFloatOrNull()?.coerceIn(0f,1f) ?: .5f)
    }.distinctBy { it.id }
    val events = IosEventCatalog.decodeObjects(root.items("priority_events") + root.items("events"))
    return IosPromoted(ids + ads.map { it.eventId }, ads, events)
}

/** Public profile data; personal images are always stored by the server. */
data class IosMyProfile(val displayName: String, val bio: String, val photoUrl: String)
