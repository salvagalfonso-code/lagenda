package com.example.agendavalenciana.ios

import kotlinx.serialization.json.*
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.example.agendavalenciana.model.IosSavedListInvitation
import com.example.agendavalenciana.model.IosAdminPermissions

/** API contract backed by the PHP endpoints bundled with the v336 server snapshot. */
interface IosSessionBridge {
    /** Implemented with iOS Keychain in Swift. Never use UserDefaults for API tokens. */
    fun loadToken(): String?
    fun saveToken(token: String)
    fun clearToken()
    /** Different Keychain account from the registered user's Bearer. */
    fun loadGuestIdentity(): String?
    /** Returns false on failed secure persistence; never fall back to UserDefaults. */
    fun saveGuestIdentity(json: String): Boolean
    /** Remove only the server-verified/claimed guest. Never purge on request failure. */
    fun clearGuestIdentity(): Boolean
    /** Encrypted AES-GCM file with key held in native Keychain. null = read/decrypt error. */
    fun loadEncryptedPendingOperations(): String?
    fun saveEncryptedPendingOperations(json: String): Boolean
    fun readCache(key: String): String?
    fun writeCache(key: String, value: String)
    fun scheduleLocalReminderConfirmed(userId: Int, eventId: Int, title: String, isoDate: String, callback: IosReminderSchedulingCallback)
    fun cancelLocalReminder(userId: Int, eventId: Int)
    fun reminderEpochMillis(isoDate: String): Long
    fun formatReminderEpochMillis(epochMillis: Long): String
    /** Shows a QR using the exact issued ticket code; never manufactures a new code. */
    fun showTicketQr(code: String, title: String)
    /** Exclusivament QR de serveis emesos per payments/service-status.php. No és una entrada. */
    fun showSpecialServiceQr(code: String, title: String)
    /** Càmera: només retorna el codi escanejat; inspecció i bescanvi sempre al servidor. */
    fun scanSpecialServiceQr(callback: IosSpecialServiceQrCallback)
    /** Only issued tickets may remain available offline; AES-GCM file, owner/bearer bound. */
    fun saveOfflineTickets(ownerId: Int, json: String): Boolean
    /** Null when absent, corrupt, erased, or no longer bound to the active Keychain bearer. */
    fun loadOfflineTicketsForCurrentSession(): String?
    /** Opens AVFoundation QR scanner; scanned codes are validated only by the server. */
    fun showTicketScanner(eventId: Int)
    /** Opens iOS notification permission settings. */
    fun showNotificationSettings()
    /** Opens the native iOS share sheet for a saved-list invitation. */
    fun shareText(text: String)
    /** Ask iOS for remote notifications; registration with APNs happens asynchronously. */
    fun requestRemoteNotifications(callback: IosReminderSchedulingCallback)
    fun setPushDeliveryEnabled(enabled: Boolean)
    fun setPushOpenListener(listener: IosPushOpenCallback)
    /** App background/foreground events: preserve position without storing user data in UserDefaults. */
    fun setFeedLifecycleListener(listener: IosFeedLifecycleCallback)
    /** Native identity-provider UI; ID token + nonce are verified by our server. */
    fun beginSocialSignIn(provider: String, callback: IosSocialAuthCallback)
    /** Photos are JPEG-compressed before crossing the KMP bridge, not kept in preferences. */
    fun pickProfilePhoto(callback: IosPickedPhotoCallback)
    /** Present Stripe PaymentSheet on iOS; completion is not proof of payment. */
    fun presentTicketPayment(clientSecret: String, publishableKey: String,
                             merchantName: String, callback: IosTicketPaymentCallback)
    fun eraseLocalAccountData(userId: Int)
}

interface IosSpecialServiceQrCallback {
    fun onScanned(code: String, error: String)
}

interface IosSocialAuthCallback {
    fun onComplete(idToken: String, nonce: String, error: String)
}
interface IosTicketPaymentCallback {
    /** COMPLETED, CANCELLED or FAILED. Only payments/status.php proves success. */
    fun onComplete(result: String, error: String)
}

interface IosPickedPhotoCallback {
    fun onComplete(base64Jpeg: String, error: String)
}

/** Completion is invoked only after Apple's notification center accepts or rejects the request. */
interface IosReminderSchedulingCallback {
    fun onResult(success: Boolean, message: String)
}

/** A tapped APNs artist alert opens the corresponding artist popup, including when resumed. */
interface IosPushOpenCallback {
    fun onOpenArtist(artist: String)
    fun onOpenEvent(eventId: Int)
}

interface IosFeedLifecycleCallback {
    fun onBackground()
    fun onForeground()
}

/** The iOS bridge is installed before any Compose state is restored. */
object IosNativeBridge {
    var current: IosSessionBridge? = null
}

class IosAgendaApi(private val sessionBridge: IosSessionBridge? = null) {
    /** Serialise account-changing operations; a delayed HTTP response must never
     * revoke or overwrite another user's authenticated Keychain session. */
    private val accountTransitionMutex = Mutex()

    private fun persistVerifiedSession(receipt: JsonObject): IosAccount {
        val bearer = receipt.text("token")
        val identity = receipt["user"] as? JsonObject ?: error("El servidor no ha confirmat el compte")
        val result = IosAccount(identity.text("id").toIntOrNull() ?: 0,
            identity.text("username"), identity.text("email"), identity.text("role"))
        IosAccountLifecyclePolicy.requireLoginReceipt(result.id, result.username, bearer)
        val bridge = sessionBridge ?: error("Cal el Keychain segur per a autenticar-se")
        bridge.saveToken(bearer)
        check(bridge.loadToken() == bearer) { "No s'ha pogut conservar la sessió al Keychain" }
        token = bearer
        account = result
        return result
    }
    // Token del compte registrat: mai es guarda fora del Keychain.
    var token: String? = sessionBridge?.loadToken()
        private set
    var account: IosAccount? = null
        private set

    /** Same guest/create.php contract as Android; credentials survive app relaunch in Keychain. */
    private val guestSession = IosGuestSession(
        loadSecure = { sessionBridge?.loadGuestIdentity() },
        saveSecure = { data -> sessionBridge?.saveGuestIdentity(data) == true },
        clearSecure = { sessionBridge?.clearGuestIdentity() == true },
        createGuest = { IosServerConnection.json("guest/create.php", buildJsonObject {}) }
    )

    fun hasRegisteredSessionToken(): Boolean = !token.isNullOrBlank()
    fun guestUserId(): Int? = guestSession.current()?.userId
    fun hasPendingGuestClaim(): Boolean = guestSession.current() != null && account != null && !token.isNullOrBlank()
    private val guestLink = IosGuestAccountLinker(
        guestSession = guestSession,
        onConfirmedBeforeDiscard = { guestId, accountId -> transferPendingAfterVerifiedClaim(guestId, accountId) },
        claimOnServer = { guestToken, registeredToken ->
            IosServerConnection.json("guest/claim.php", buildJsonObject {
                put("guest_token", guestToken)
            }, registeredToken, authenticated = true)
        }
    )

    /** Claim an existing guest into the authenticated account. On network/server error,
     * retain Keychain guest credentials for a later retry. The PHP transaction owns merges.
     * Registered auth must never fail merely because this follow-up request is offline.
     */
    suspend fun claimPendingGuestIdentity(expectedAccountId: Int): Boolean {
        val registered = account ?: return false
        val registeredBearer = token?.takeIf(String::isNotBlank) ?: return false
        if (registered.id != expectedAccountId || expectedAccountId <= 0) return false
        return guestLink.claimInto(expectedAccountId, registeredBearer)
    }
    suspend fun ensureGuestIdentity(): Int {
        check(sessionBridge != null) { "No està disponible el Keychain d'iPhone." }
        return guestSession.ensure().userId
    }

    /** Do not confuse guest bearer with registered login. Personal APIs can accept either. */
    private suspend fun request(path: String, data: JsonObject? = null, authenticated: Boolean = false): JsonObject =
        IosServerConnection.json(path, data,
            token ?: if (authenticated) guestSession.current()?.token else null, authenticated)

    /** Pas 4: hydrate an authenticated guest or registered user from the Android v336
     * central MariaDB store. The bearer and expected user ID are frozen BEFORE IO,
     * so switching accounts can never mix two users' snapshots.
     * This operation is read-only: writes and offline mutations belong to steps 5/6.
     */
    internal suspend fun loadServerState(): IosHydratedServerState {
        val registeredToken = token?.takeIf { it.isNotBlank() }
        val identityId: Int
        val bearer: String
        if (registeredToken != null) {
            identityId = account?.id?.takeIf { it > 0 }
                ?: error("Encara no s'ha restaurat el compte registrat")
            bearer = registeredToken
        } else {
            val guest = guestSession.ensure()
            identityId = guest.userId
            bearer = guest.token
        }
        // The general state must succeed. Dedicated domains are optional reads only.
        val central = IosCentralStateSnapshot.decode(
            IosServerConnection.json("sync/state.php", bearer = bearer, authenticated = true), identityId)
        val extra = coroutineScope {
            val locations = async { runCatching { IosServerLocations.decode(
                IosServerConnection.json("preferences/locations.php", bearer = bearer, authenticated = true)) }.getOrNull() }
            val feed = async { runCatching { IosServerFeedPreferences.decode(
                IosServerConnection.json("preferences/feed.php", bearer = bearer, authenticated = true)) }.getOrNull() }
            val personal = async { runCatching {
                IosServerConnection.json("user-state/mine.php", bearer = bearer, authenticated = true)
            }.getOrNull() }
            Triple(locations.await(), feed.await(), personal.await())
        }
        // The screen also checks identity before applying; do not return an old login's data.
        check(token == registeredToken &&
            (if (registeredToken != null) account?.id == identityId else guestSession.current()?.userId == identityId)) {
            "La sessió ha canviat durant la càrrega de dades."
        }
        val visibility = runCatching { IosVisibilitySnapshot.decode(IosServerConnection.json(
            "visibility/index.php", bearer = bearer, authenticated = true)) }.getOrNull()
        return IosHydratedServerState(central, extra.first, extra.second, extra.third, visibility)
    }

    /** Pas 6: one encrypted write-ahead queue; never persist Bearer tokens with actions.
     * A given queue entry may ONLY be delivered with its owner's current bearer.
     */
    private val pendingQueue: IosPendingServerQueue? = sessionBridge?.let { bridge ->
        IosPendingServerQueue(
            loadSecure = { bridge.loadEncryptedPendingOperations() },
            saveSecure = { json -> bridge.saveEncryptedPendingOperations(json) },
            deliverAndVerify = { action, bearer ->
                check(activePersonalId() == action.ownerId) { "El compte actiu ha canviat" }
                check(personalBearer(action.ownerId) == bearer) { "La sessió ha canviat" }
                val path = when (action.kind) {
                    "state" -> "sync/state.php"
                    "locations" -> "preferences/locations.php"
                    "feed" -> "preferences/feed.php"
                    "post" -> action.path ?: error("Falta ruta")
                    else -> error("Tipus de cua invàlid")
                }
                val response = IosServerConnection.json(path, action.payload, bearer, authenticated = true)
                when (action.kind) {
                    "state" -> {
                        check(response["user_id"]?.jsonPrimitive?.intOrNull == action.ownerId) {
                            "L'API ha retornat un usuari diferent"
                        }
                        val expectedIds = (action.payload["mutations"] as? JsonArray)
                            ?.map { (it as JsonObject)["id"]?.jsonPrimitive?.contentOrNull ?: error("ID invàlid") }
                            ?.toSet() ?: error("Mutacions invàlides")
                        val results = response["results"] as? JsonArray ?: error("Falta la confirmació del servidor")
                        val ackIds = results.mapNotNull { row ->
                            val entry = row as? JsonObject ?: return@mapNotNull null
                            entry["id"]?.jsonPrimitive?.contentOrNull?.takeIf {
                                entry["status"]?.jsonPrimitive?.contentOrNull in setOf("APPLIED", "ALREADY_APPLIED")
                            }
                        }.toSet()
                        check(ackIds == expectedIds && results.size == expectedIds.size) {
                            "La confirmació de les preferències és incompleta"
                        }
                        check(response["revision"]?.jsonPrimitive?.longOrNull != null) { "Falta revisió" }
                    }
                    "locations" -> {
                        val remote = IosServerLocations.decode(response)
                        check(remote.exists && remote.revision > 0L) { "Llocs sense confirmació o revisió" }
                        val expectedTowns = (action.payload["towns"] as? JsonArray)
                            ?.map { it as? JsonObject ?: error("Poble de cua invàlid") }
                            ?.associate { row ->
                                (row["name"]?.jsonPrimitive?.contentOrNull ?: error("Falta poble")) to
                                    (row["radius_km"]?.jsonPrimitive?.intOrNull ?: error("Falta radi"))
                            } ?: error("Falten pobles de la cua")
                        val expectedComarques = (action.payload["comarques"] as? JsonArray)
                            ?.map { it.jsonPrimitive.content }?.toSet() ?: error("Falten comarques de la cua")
                        val expected = IosLocationRules.prepare(expectedTowns.keys, expectedComarques, expectedTowns)
                        val actual = IosLocationRules.prepare(remote.towns.keys, remote.comarques, remote.towns)
                        check(IosLocationRules.matches(expected, actual)) {
                            "El servidor no ha confirmat els mateixos pobles, comarques i radis"
                        }
                    }
                    "feed" -> {
                        val saved = response["preferences"] as? JsonObject ?: error("Feed no confirmat")
                        check(response["exists"]?.jsonPrimitive?.booleanOrNull == true) {
                            "El servidor no confirma que existisquen preferències guardades"
                        }
                        check(listOf("suggestions", "discovery", "valencian_first").all {
                            saved[it] == action.payload[it]
                        }) { "Les preferències rebudes no coincidixen" }
                    }
                    "post" -> if (action.path == "visibility/index.php") {
                        check(response.bool("ok")) { "El servidor no ha confirmat l'exclusió" }
                        val command = IosVisibilityMutation(action.payload.text("kind"),
                            action.payload.text("target"), action.payload.text("action"))
                        val persisted = IosVisibilitySnapshot.decode(IosServerConnection.json(
                            "visibility/index.php", bearer = bearer, authenticated = true))
                        check(IosVisibilityAffinityContract.confirm(persisted, command)) {
                            "L'exclusió no apareix aplicada al servidor; conservar en cua"
                        }
                    } else if (action.path == "affinities/record.php") {
                        check(response.bool("ok")) { "L'afinitat no està confirmada" }
                        val item = response["affinity"] as? JsonObject ?: error("Falta afinitat")
                        check(item.text("entity_type").uppercase() == action.payload.text("entity_type").uppercase() &&
                            IosFollowMutation.canonicalKey(item.text("entity_key")) ==
                            IosFollowMutation.canonicalKey(action.payload.text("entity_key"))) {
                            "El servidor no confirma la mateixa afinitat"
                        }
                    } else if (action.path == "events/save.php") {
                        IosSavedEventContract.verifyPost(response, action.payload)
                        // PHP returns no user_id for save; verify the exact same bearer/ID
                        // by reading the canonical MariaDB saved-events list.
                        val remoteSaved = IosServerConnection.json(
                            "events/saved.php", bearer = bearer, authenticated = true)
                        IosSavedEventContract.verifyCanonicalList(remoteSaved, action.payload)
                    } else if (action.path == "batecs/seen.php") {
                        val key = IosBatecOperations.canonicalKey(action.payload.text("story_key"))
                        IosBatecOperations.verifySeenReceipt(response, key)
                        val remote = IosServerConnection.json("batecs/seen.php", bearer = bearer, authenticated = true)
                        check(key in IosBatecOperations.seenKeys(remote)) {
                            "El Batec no figura com a vist al servidor; es conservarà en la cua"
                        }
                    } else if (action.path == "user-state/event-alert.php") {
                        val eventId = action.payload["event_id"]?.jsonPrimitive?.intOrNull ?: error("Falta event_id")
                        val enabled = action.payload["active"]?.jsonPrimitive?.booleanOrNull ?: error("Falta active")
                        check(response.bool("ok") && response["event_id"]?.jsonPrimitive?.intOrNull == eventId &&
                            response["active"]?.jsonPrimitive?.booleanOrNull == enabled) {
                            "La resposta de la campaneta no correspon amb l'acció"
                        }
                        val canonical = IosServerConnection.json("user-state/mine.php", bearer = bearer, authenticated = true)
                        val ids = canonical.arr("event_alerts").mapNotNull { it.jsonPrimitive.intOrNull }.toSet()
                        check((eventId in ids) == enabled) { "La campaneta de l'esdeveniment no està confirmada" }
                    } else if (action.path == "notifications/artist-bell.php") {
                        val name = action.payload.text("artist_name")
                        val enabled = action.payload["enabled"]?.jsonPrimitive?.booleanOrNull ?: error("Falta enabled")
                        val freq = action.payload.text("frequency")
                        check(response.bool("ok") && IosArtistBellPolicy.key(response.text("artist_name")) == IosArtistBellPolicy.key(name) &&
                            response.text("frequency") == freq && response["enabled"]?.jsonPrimitive?.booleanOrNull == enabled) {
                            "La resposta de la campaneta d'artista no correspon amb la petició"
                        }
                        val remote = IosServerConnection.json("notifications/artist-bell.php", bearer = bearer, authenticated = true)
                        val current = remote.items("artists").firstOrNull {
                            IosArtistBellPolicy.key(it.text("artist_name")) == IosArtistBellPolicy.key(name)
                        }
                        check(if (enabled) current != null && current.text("frequency") == freq && current.bool("enabled")
                            else current == null || !current.bool("enabled")) {
                            "La freqüència de la campaneta no està confirmada a MariaDB"
                        }
                    } else if (action.path == "mail/preferences-api.php") {
                        // Idempotent, exact desired state: offline retries cannot invent digest options.
                        val remote = IosServerConnection.json("mail/preferences-api.php",
                            bearer = bearer, authenticated = true)
                        check((remote["preferences"] as? JsonObject)?.let { preferences ->
                            listOf("digest_frequency", "recommendations_enabled", "followed_artists_enabled",
                                "saved_reminders_enabled", "ads_enabled").all {
                                preferences[it] == action.payload[it]
                            }
                        } == true) { "El servidor no ha confirmat les preferències de correu" }
                    } else if (action.path == "user-state/follow.php") {
                        val expectedType = action.payload.text("entity_type").uppercase()
                        val expectedKey = IosFollowMutation.canonicalKey(action.payload.text("entity_key"))
                        val expectedFollowing = action.payload["following"]?.jsonPrimitive?.booleanOrNull
                            ?: error("Falta l'acció de seguiment")
                        check(response.text("entity_type").uppercase() == expectedType &&
                            IosFollowMutation.canonicalKey(response.text("entity_key")) == expectedKey &&
                            response["following"]?.jsonPrimitive?.booleanOrNull == expectedFollowing) {
                            "La resposta no confirma el seguiment exacte; mantenim l'acció xifrada"
                        }
                    } // Other POSTs retain their existing transport contract.
                }
                check(activePersonalId() == action.ownerId && personalBearer(action.ownerId) == bearer) {
                    "Sessió canviada abans de confirmar"
                }
            }
        )
    }
    private suspend fun queuedWrite(ownerId: Int, kind: String, payload: JsonObject, path: String? = null) {
        val bearer = personalBearer(ownerId)
        val queue = pendingQueue ?: error("No hi ha emmagatzematge xifrat disponible")
        val item = IosQueuedAction(IosPersonalMutationId.make(), ownerId, kind, payload, path)
        queue.append(item) // encrypted & durable BEFORE sending; crash-safe retry
        queue.flush(ownerId, bearer)
        if (queue.contains(item.id)) throw IosQueuedForRetryException()
    }
    /** Retry after app startup, foreground/tab changes and every 60s while visible. */
    suspend fun flushOfflineActions(): Int {
        val id = activePersonalId() ?: return 0
        val bearer = personalBearer(id)
        return pendingQueue?.flush(id, bearer) ?: 0
    }
    suspend fun pendingOfflineActionCount(): Int {
        val id = activePersonalId() ?: return 0
        return pendingQueue?.countFor(id) ?: 0
    }
    /** Do not merge queues before server confirms guest ownership transfer. */
    private suspend fun transferPendingAfterVerifiedClaim(guestId: Int, registeredId: Int) {
        pendingQueue?.transferAfterConfirmedClaim(guestId, registeredId)
    }

    /** Pas 5: serialized, acknowledged mutations. Exactly one server identity owns a write.
     * Only the STEP 6 offline queue may keep unacknowledged mutations durably.
     */
    private val personalWriteMutex = Mutex()

    internal suspend fun savePersonalState(expectedUserId: Int, values: Map<String, JsonObject>): Long =
        personalWriteMutex.withLock {
            require(expectedUserId > 0 && values.isNotEmpty() && values.size <= 100)
            check(activePersonalId() == expectedUserId) { "Ha canviat el compte actiu" }
            val mutations = buildJsonArray {
                values.forEach { (key, value) ->
                    require(key.matches(Regex("[a-zA-Z0-9_.:-]{1,190}")))
                    require(value["t"] != null && value["v"] != null)
                    add(buildJsonObject {
                        put("id", IosPersonalMutationId.make()); put("key", key)
                        put("op", "put"); put("value", value)
                    })
                }
            }
            queuedWrite(expectedUserId, "state", buildJsonObject { put("mutations", mutations) })
            // Successful POST acknowledgement is verified inside the queue. Callers currently
            // ignore the return; 0 means "revision not read", NOT a cached revision.
            0L
        }

    internal suspend fun saveLocationPreferences(expectedUserId: Int, towns: Set<String>,
                                                  comarques: Set<String>, radii: Map<String, Int>) {
        val prepared = IosLocationRules.prepare(towns, comarques, radii)
        check(activePersonalId() == expectedUserId) { "Ha canviat el compte actiu" }
        queuedWrite(expectedUserId, "locations", buildJsonObject {
            put("towns", buildJsonArray {
                prepared.towns.forEach { (town, radius) -> add(buildJsonObject {
                    put("name", town)
                    put("radius_km", radius)
                }) }
            })
            put("comarques", buildJsonArray { prepared.comarques.forEach { add(it) } })
        })
    }

    /** No restaurar una fotografia remota antiga sobre una modificació offline pendent. */
    internal suspend fun hasPendingLocationWrites(expectedUserId: Int): Boolean {
        check(activePersonalId() == expectedUserId) { "Ha canviat el compte actiu" }
        return pendingQueue?.hasPendingKind(expectedUserId, "locations") ?: true
    }

    /** Feed domain (3 flags); UI language lives in sync/state.php, never in this payload. */
    internal suspend fun saveFeedPreferences(expectedUserId: Int, suggestions: Boolean,
                                              discovery: String, valencianFirst: Boolean) {
        require(IosFeedPreferencesContract.validDiscovery(discovery))
        check(activePersonalId() == expectedUserId) { "Ha canviat el compte actiu" }
        queuedWrite(expectedUserId, "feed", buildJsonObject {
            put("suggestions", suggestions); put("discovery", discovery)
            put("valencian_first", valencianFirst)
        })
    }

    /** A pending feed/state mutation is newer than an in-flight GET; never roll UI back. */
    internal suspend fun hasPendingFeedWrites(expectedUserId: Int): Boolean {
        check(activePersonalId() == expectedUserId) { "Ha canviat el compte actiu" }
        val queue = pendingQueue ?: return true // fail closed if there is no secure storage
        return queue.hasPendingKind(expectedUserId, "feed") ||
            queue.hasPendingKind(expectedUserId, "state")
    }

    internal fun activePersonalId(): Int? = if (!token.isNullOrBlank()) account?.id else guestSession.current()?.userId
    private suspend fun personalBearer(expectedUserId: Int): String {
        val bearer = token?.takeIf(String::isNotBlank)
        if (bearer != null) {
            check(account?.id == expectedUserId) { "Compte incorrecte" }
            return bearer
        }
        val guest = guestSession.ensure()
        check(guest.userId == expectedUserId) { "Convidat incorrecte" }
        return guest.token
    }

    /** Before converting a guest in-place, preserve its existing notifications: the
     * CURRENT register.php overwrites these 4 flags when the payload lacks notifications.
     * Fail closed instead of silently converting and erasing preferences if GET fails.
     */
    private suspend fun guestRegistrationNotifications(guestToken: String): JsonObject {
        val root = IosServerConnection.json("user-state/mine.php", bearer = guestToken, authenticated = true)
        return when (val existing = root["notification_preferences"]) {
            is JsonObject -> {
                val keys = listOf("followed_artist_concerts", "followed_artist_news", "taste_suggestions", "nearby_suggestions")
                require(keys.all { key -> existing[key]?.jsonPrimitive?.booleanOrNull != null }) {
                    "Preferències de notificació incompletes: no es convertirà el convidat."
                }
                existing
            }
            null, JsonNull -> buildJsonObject { } // no preference row exists yet
            else -> error("Preferències del convidat invàlides")
        }
    }

    suspend fun restoreAccount(): IosAccount? = accountTransitionMutex.withLock {
        val restoredBearer = token?.takeIf(String::isNotBlank) ?: return@withLock null
        runCatching {
            val root = IosServerConnection.json("auth/me.php", bearer = restoredBearer, authenticated = true)
            val user = root["user"] as? JsonObject ?: error("La sessió no és vàlida")
            val result = IosAccount(user.text("id").toIntOrNull() ?: 0, user.text("username"),
                user.text("email"), user.text("role"))
            IosAccountLifecyclePolicy.requireLoginReceipt(result.id, result.username, restoredBearer)
            check(token == restoredBearer) { "La sessió ha canviat durant la restauració" }
            account = result
            result
        }.getOrElse { null } // Connection failure never silently removes a Keychain bearer.
    }
    /** Generic server response avoids revealing whether an account exists. */
    suspend fun requestPasswordReset(email: String): String {
        require(email.contains("@")) { "Introduïx un correu electrònic vàlid." }
        val response = request("auth/request-password-reset.php", buildJsonObject { put("email", email.trim()) })
        return response.text("message").ifBlank { "Si existix el compte, rebràs instruccions per correu." }
    }
    suspend fun signIn(login: String, password: String): IosAccount = accountTransitionMutex.withLock {
        val root = IosServerConnection.json("auth/login.php", buildJsonObject {
            put("login", login.trim()); put("password", password); put("device_name", "iOS l'Agenda KMP")
        })
        persistVerifiedSession(root)
    }
    suspend fun registerAccount(username: String, email: String, password: String): IosAccount = accountTransitionMutex.withLock {
        require(username.trim().length in 3..80) { "El nom d'usuari ha de tindre entre 3 i 80 caràcters." }
        require("@" in email && "." in email.substringAfter("@")) { "Correu electrònic no vàlid." }
        require(password.length >= 8) { "La contrasenya ha de tindre almenys 8 caràcters." }
        // Android v336 sends the guest Bearer to register.php. That endpoint converts
        // the SAME users.id in-place (not guest/claim.php for newly created accounts).
        val guestBeforeRegistration = guestSession.current()
        val guestNotifications = guestBeforeRegistration?.let { guestRegistrationNotifications(it.token) }
        val response = IosServerConnection.json("auth/register.php", buildJsonObject {
            put("username", username.trim())
            put("email", email.trim())
            put("password", password)
            put("device_name", "iOS l'Agenda KMP")
            guestBeforeRegistration?.let { put("guest_token", it.token) }
            guestNotifications?.let { put("notifications", it) }
        })
        val registeredAccount = persistVerifiedSession(response)
        // register.php has already committed the atomic in-place guest conversion.
        // If deleting the Keychain entry fails, the next authenticated restore retries.
        if (response.bool("guest_upgraded_in_place") &&
            guestBeforeRegistration?.userId == registeredAccount.id && registeredAccount.id > 0) {
            runCatching { guestLink.discardUpgradedGuest(registeredAccount.id) }
        }
        registeredAccount
    }
    /** No social provider credential is accepted as our own API bearer token. */
    suspend fun signInWithProvider(provider: String, idToken: String, nonce: String): IosAccount =
        accountTransitionMutex.withLock {
            val path = when (provider.uppercase()) {
                "APPLE" -> "auth/apple-ios.php"
                "GOOGLE" -> "auth/google-ios.php"
                else -> error("Proveïdor no admés")
            }
            IosAccountLifecyclePolicy.requireSocialCredential(idToken, nonce)
            // Identity-provider tokens are NEVER an Agenda Bearer and login requests
            // must not inherit an existing guest or registered bearer.
            val root = IosServerConnection.json(path, buildJsonObject {
                put("id_token", idToken); put("nonce", nonce)
                put("device_name", "iOS l'Agenda KMP")
            })
            persistVerifiedSession(root)
        }

    /** Pas 34: Android v336's user-profiles/catalog + user-profiles/update, and
     * images/upload with the same USER/username owner. Never synthesize a guest profile
     * or silently discard professional fields that Android also sends on update.
     */
    private val profileWriteMutex = Mutex()
    private fun registeredProfileIdentity(): Triple<Int, String, String> {
        val current = account ?: error("El perfil públic requerix un compte registrat")
        val bearer = token?.takeIf { it.isNotBlank() } ?: error("Falta una sessió registrada")
        require(current.id > 0 && current.username.isNotBlank())
        return Triple(current.id, current.username, bearer)
    }
    private fun checkRegisteredProfileIdentity(expected: Triple<Int, String, String>) {
        check(account?.id == expected.first && account?.username == expected.second && token == expected.third) {
            "La sessió ha canviat mentre s'actualitzava el perfil"
        }
    }
    private fun profileRow(row: JsonObject, expected: String): IosMyProfile =
        IosMyProfile(row.text("display_name").ifBlank { expected }, row.text("bio"),
            resolveProfileImage(row.text("image_url")),
            homeTown = row.text("home_town"), discipline = row.text("discipline"),
            companyName = row.text("company_name"), castText = row.text("cast_text"),
            publisherName = row.text("publisher_name"), booksText = row.text("books_text"),
            worksText = row.text("works_text"), spacesText = row.text("spaces_text"))

    private suspend fun profileSnapshot(expected: Triple<Int, String, String>): IosMyProfile {
        val catalog = IosServerConnection.json("user-profiles/catalog.php", bearer = expected.third, authenticated = true)
        checkRegisteredProfileIdentity(expected)
        val ownRows = catalog.items("profiles").filter { row ->
            IosProfileParity.sameUsername(row.text("username"), expected.second)
        }
        check(ownRows.size <= 1) { "El catàleg conté perfils duplicats per al compte" }
        return ownRows.singleOrNull()?.let { profileRow(it, expected.second) }
            ?: IosMyProfile(expected.second, "", "") // No public profile created yet.
    }
    suspend fun myPublicProfile(): IosMyProfile {
        val expected = registeredProfileIdentity()
        return profileSnapshot(expected)
    }
    suspend fun updateMyPublicProfile(
        displayName: String, bio: String = "", removeImage: Boolean = false,
        discipline: String? = null, companyName: String? = null, castText: String? = null,
        publisherName: String? = null, booksText: String? = null,
        worksText: String? = null, spacesText: String? = null
    ): IosMyProfile = profileWriteMutex.withLock {
        val expected = registeredProfileIdentity()
        val before = profileSnapshot(expected)
        val clean = IosProfileParity.validateProfile(displayName, bio)
        val desired = before.copy(displayName = clean.first, bio = clean.second,
            discipline = discipline?.trim() ?: before.discipline,
            companyName = companyName?.trim() ?: before.companyName,
            castText = castText?.trim() ?: before.castText,
            publisherName = publisherName?.trim() ?: before.publisherName,
            booksText = booksText?.trim() ?: before.booksText,
            worksText = worksText?.trim() ?: before.worksText,
            spacesText = spacesText?.trim() ?: before.spacesText)
        checkRegisteredProfileIdentity(expected)
        val root = IosServerConnection.json("user-profiles/update.php", buildJsonObject {
            put("username", expected.second); put("display_name", desired.displayName)
            put("bio", desired.bio); put("discipline", desired.discipline)
            put("company_name", desired.companyName); put("cast_text", desired.castText)
            put("publisher_name", desired.publisherName); put("books_text", desired.booksText)
            put("works_text", desired.worksText); put("spaces_text", desired.spacesText)
            put("remove_image", removeImage)
        }, expected.third, authenticated = true)
        checkRegisteredProfileIdentity(expected)
        val ack = root["profile"] as? JsonObject ?: error("El servidor no ha confirmat el perfil")
        check(IosProfileParity.sameUsername(ack.text("username"), expected.second) &&
            ack.text("display_name") == desired.displayName && ack.text("bio") == desired.bio) {
            "El servidor ha retornat un perfil diferent del sol·licitat"
        }
        val updated = profileRow(ack, expected.second)
        check(IosProfileParity.sameProfessionalFields(desired, updated)) {
            "El servidor no ha confirmat els camps professionals"
        }
        updated
    }
    suspend fun uploadMyPhoto(base64Jpeg: String): String = profileWriteMutex.withLock {
        val expected = registeredProfileIdentity()
        require(base64Jpeg.length in 100..8_000_000) { "Imatge massa gran o buida" }
        val root = IosServerConnection.json("images/upload.php", buildJsonObject {
            put("owner_type", "USER"); put("owner_key", expected.second)
            put("image_base64", base64Jpeg); put("mime_type", "image/jpeg")
        }, expected.third, authenticated = true)
        checkRegisteredProfileIdentity(expected)
        val url = resolveProfileImage(root.text("image_url"))
        check(url.isNotBlank()) { "No s'ha rebut una URL vàlida de la fotografia" }
        // Same authoritative owner endpoint as Android's updateOwnerMedia.
        val path = "images/owner.php?owner_type=USER&owner_key=${expected.second.encodeURLParameter()}"
        val owner = IosServerConnection.json(path, bearer = expected.third, authenticated = true)
        checkRegisteredProfileIdentity(expected)
        check(resolveProfileImage(owner.text("image_url")) == url) {
            "La foto encara no està confirmada al servidor"
        }
        url
    }
    /** Android v336 account/update.php: username, email, home_town; a separate
     * user-profiles/update.php writes the public biography and photo. No local cache.
     */
    suspend fun updateMyAccountIdentity(username: String, email: String, homeTown: String): IosAccount =
        profileWriteMutex.withLock {
            val expected = registeredProfileIdentity()
            val desired = IosProfileParity.validateIdentity(username, email, homeTown)
            val root = IosServerConnection.json("account/update.php", buildJsonObject {
                put("username", desired.first); put("email", desired.second)
                put("home_town", desired.third)
            }, expected.third, authenticated = true)
            checkRegisteredProfileIdentity(expected)
            val user = root["user"] as? JsonObject ?: error("El servidor no ha confirmat el compte")
            check(IosProfileParity.sameUsername(user.text("username"), desired.first) &&
                user.text("email").equals(desired.second, ignoreCase = true) &&
                user.text("home_town") == desired.third) {
                "Les dades del compte no estan confirmades al servidor"
            }
            val newId = user.text("id").toIntOrNull()
            check(newId == null || newId == expected.first) { "El servidor ha retornat un usuari diferent" }
            val updated = account!!.copy(username = user.text("username"), email = user.text("email"))
            account = updated
            updated
        }
    /** Guests have a real server ID/preferences but Android's profile editor is
     * registered-only. Never pass an anonymous user to an account editing endpoint. */
    suspend fun guestProfileIdentity(): Int {
        check(token.isNullOrBlank() && account == null) { "Hi ha un compte registrat actiu" }
        return ensureGuestIdentity()
    }
    private fun resolveProfileImage(raw: String): String = when {
        raw.startsWith("https://") -> raw
        raw.startsWith("/") -> IosServerConnection.SITE_BASE + raw
        raw.startsWith("images/") -> IosServerConnection.APP_BASE + raw
        else -> ""
    }
    /** Deletion is irreversible and must be confirmed by the actual MariaDB API.
     * Freeze bearer + identity; never clear a different account after network IO.
     */
    suspend fun deleteMyAccount(confirmation: String, password: String) = accountTransitionMutex.withLock {
        val current = account ?: error("La baixa requerix un compte registrat")
        val bearer = token?.takeIf(String::isNotBlank) ?: error("Falta la sessió registrada")
        IosAccountLifecyclePolicy.requireDeleteConfirmation(confirmation, current.id)
        val root = IosServerConnection.json("account/delete-ios.php", buildJsonObject {
            put("confirmation", confirmation); put("password", password)
        }, bearer = bearer, authenticated = true)
        check(account?.id == current.id && token == bearer) { "Ha canviat el compte durant la baixa" }
        IosAccountLifecyclePolicy.requireDeleteReceipt(root.bool("deleted"), root.text("mode"))
        // Even if the encrypted queue cannot be opened, a deleted account must not
        // remain authenticated. Report local cleanup errors explicitly after logout.
        val cleanup = runCatching { pendingQueue?.discardDeletedAccount(current.id) }
        signOut()
        sessionBridge?.eraseLocalAccountData(current.id)
        cleanup.getOrThrow()
    }
    fun signOut() {
        sessionBridge?.clearToken()
        check(sessionBridge?.loadToken().isNullOrBlank()) { "No s'ha pogut revocar la credencial del Keychain" }
        token = null; account = null
    }
    /** Revokes remote session with a frozen bearer. Network outages must never
     * prevent an explicit logout on the device. Account switching is serialised.
     */
    suspend fun signOutSecurely() = accountTransitionMutex.withLock {
        val bearer = token?.takeIf(String::isNotBlank)
        val userId = account?.id
        try {
            if (bearer != null) {
                val deviceToken = platformReadPublicCache("apns_device_token")
                if (deviceToken.isNotBlank()) runCatching {
                    IosServerConnection.json("notifications/apns-device.php", buildJsonObject {
                        put("action", "unregister"); put("device_token", deviceToken)
                    }, bearer = bearer, authenticated = true)
                }
                runCatching { IosServerConnection.json("auth/logout.php", buildJsonObject { },
                    bearer = bearer, authenticated = true) }
            }
        } finally {
            if (token == bearer && account?.id == userId) signOut()
        }
    }

    /** Reports use an existing identity issued by guest/create.php (MariaDB), rather than
     * creating another durable, device-only identifier in UserDefaults. The server still
     * decides whether to accept or link a report; this does not imply cross-device report
     * history is synchronized by the current report-results API. */
    suspend fun reportEvent(event: AgendaEventSummary, reason: String, details: String) {
        val accepted = setOf("ANIMAL_ABUSE", "HUMAN_RIGHTS", "HATE_DISCRIMINATION", "INCITEMENT_VIOLENCE",
            "CANCELLED", "INCORRECT_INFO", "DUPLICATE", "NOT_CULTURAL", "POSSIBLE_SCAM", "INAPPROPRIATE_CONTENT", "OTHER")
        require(reason in accepted) { "Motiu del report no admés" }
        val guestId = guestSession.ensure().guestId
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

    /** PAS 17: identical collections/index.php contract to Android v336.
     * Lists are never authoritative on-device, including for server-backed guests.
     * Non-idempotent create is deliberately NOT put in the offline retry queue:
     * re-POSTing it after an ambiguous timeout could duplicate a list in MariaDB.
     */
    private val savedListWriteMutex = Mutex()

    private suspend fun collectionIdentity(): Pair<Int, String> {
        val owner = activePersonalId() ?: ensureGuestIdentity()
        return owner to personalBearer(owner)
    }
    private suspend fun canonicalLists(bearer: String): List<IosSavedList> =
        IosSavedListContract.decode(IosServerConnection.json(
            "collections/index.php?action=list", bearer = bearer, authenticated = true))

    suspend fun savedLists(): List<IosSavedList> {
        val (owner, bearer) = collectionIdentity()
        val lists = canonicalLists(bearer)
        check(activePersonalId() == owner && personalBearer(owner) == bearer) {
            "La sessió ha canviat durant la lectura de llistes."
        }
        return lists
    }

    /** A successful write is returned only after canonical GET confirms the result.
     * Every write is serialized for the current installation and uses a frozen Bearer.
     * A POST-success/GET-failure is reported as indeterminate: never auto-retry create.
     */
    private suspend fun listOperation(action: String, listId: Int? = null,
        eventId: Int? = null, name: String? = null, add: Boolean? = null,
        invite: String? = null): IosVerifiedListChange = savedListWriteMutex.withLock {
        val (owner, bearer) = collectionIdentity()
        val before = canonicalLists(bearer)
        val command = IosSavedListCommand(action, listId, eventId, name, add, invite)
        IosSavedListContract.preflight(before, command)
        check(activePersonalId() == owner && personalBearer(owner) == bearer) {
            "La sessió ha canviat abans de modificar la llista."
        }
        val response = IosServerConnection.json("collections/index.php", buildJsonObject {
            put("action", action)
            if (listId != null) put("list_id", listId)
            if (eventId != null) put("event_id", eventId)
            if (name != null) put("name", name)
            if (add != null) put("add", add)
            if (invite != null) put("invite", invite)
        }, bearer, authenticated = true)
        check(activePersonalId() == owner && personalBearer(owner) == bearer) {
            "La sessió ha canviat després de modificar la llista."
        }
        val after = canonicalLists(bearer)
        check(activePersonalId() == owner && personalBearer(owner) == bearer) {
            "La sessió ha canviat abans de confirmar les llistes."
        }
        IosSavedListContract.verify(before, after, command, response)
    }

    suspend fun createSavedList(name: String, firstEventId: Int? = null) {
        val clean = name.trim()
        require(clean.length in 1..100) { "El nom ha de tindre entre 1 i 100 caràcters." }
        require(firstEventId == null || firstEventId > 0)
        listOperation("create_list", eventId = firstEventId, name = clean)
    }
    suspend fun renameSavedList(id: Int, name: String) {
        val clean = name.trim()
        require(clean.length in 1..100) { "El nom ha de tindre entre 1 i 100 caràcters." }
        listOperation("rename_list", listId = id, name = clean)
    }
    suspend fun deleteSavedList(id: Int) { listOperation("delete_list", listId = id) }
    suspend fun changeSavedListEvent(id: Int, eventId: Int, add: Boolean) {
        require(eventId > 0)
        listOperation("list_event", listId = id, eventId = eventId, add = add)
    }
    suspend fun shareSavedList(id: Int): IosSavedListShare {
        val confirmed = listOperation("share_list", listId = id)
        return confirmed.share ?: error("Falta confirmació de la invitació.")
    }
    suspend fun joinSavedList(code: String) {
        val clean = IosSavedListInvitation.parse(code)
            ?: error("El codi o enllaç d'invitació no és vàlid.")
        listOperation("join_list", invite = clean)
    }
    suspend fun leaveSavedList(id: Int) { listOperation("leave_list", listId = id) }
    suspend fun revokeSavedListSharing(id: Int) {
        listOperation("revoke_list_share", listId = id)
    }

    /** Same MariaDB table and endpoint as Android v336, for registered AND guest identities.
     * Identity and bearer are frozen across network requests; no cross-account leakage.
     */
    private val savedSearchMutex = Mutex()

    private suspend fun searchFilterIdentity(): Pair<Int, String> {
        val accountToken = token?.takeIf(String::isNotBlank)
        if (accountToken != null) {
            val id = account?.id?.takeIf { it > 0 } ?: error("No hi ha un compte autenticat.")
            return id to accountToken
        }
        val identity = guestSession.ensure()
        return identity.userId to identity.token
    }

    private fun ensureSearchIdentity(identity: Pair<Int, String>) {
        check(activePersonalId() == identity.first &&
            (token ?: guestSession.current()?.token) == identity.second) {
            "La sessió ha canviat durant la sincronització de filtres."
        }
    }

    private suspend fun searchFiltersRead(identity: Pair<Int, String>): List<IosSavedSearch> {
        val response = IosServerConnection.json("preferences/search-filters.php",
            bearer = identity.second, authenticated = true)
        ensureSearchIdentity(identity)
        require(response.bool("ok")) { "El servidor no ha confirmat les cerques guardades." }
        return response.items("presets").mapNotNull { item ->
            val name = item.text("name").takeIf(String::isNotBlank) ?: return@mapNotNull null
            val filters = item["filters"] as? JsonObject ?: return@mapNotNull null
            // Never turn a malformed server row into an empty, misleading filter.
            runCatching { IosSavedSearch(name, IosSearchPresetContract.fromServer(filters)) }.getOrNull()
        }
    }

    suspend fun savedSearches(): List<IosSavedSearch> {
        val identity = searchFilterIdentity()
        return searchFiltersRead(identity)
    }

    suspend fun saveSearch(name: String, filters: IosSearchFilters): List<IosSavedSearch> = savedSearchMutex.withLock {
        val cleaned = name.trim()
        require(cleaned.length in 1..52) { "El nom del filtre ha de tindre entre 1 i 52 caràcters." }
        val payload = IosSearchPresetContract.toServer(filters)
        val identity = searchFilterIdentity()
        val result = IosServerConnection.json("preferences/search-filters.php", buildJsonObject {
            put("action", "save"); put("name", cleaned); put("filters", payload)
        }, bearer = identity.second, authenticated = true)
        ensureSearchIdentity(identity)
        require(result.bool("ok") && result.bool("saved")) { "El servidor no ha confirmat el guardat." }
        // A POST alone is not proof that the same data is readable after saving.
        val actual = searchFiltersRead(identity)
        check(actual.any { it.name.equals(cleaned, ignoreCase = true) &&
            IosSearchPresetContract.same(it.filters, filters) }) { "No s'ha pogut verificar el filtre guardat." }
        actual
    }

    suspend fun removeSavedSearch(name: String): List<IosSavedSearch> = savedSearchMutex.withLock {
        val cleaned = name.trim()
        require(cleaned.length in 1..52)
        val identity = searchFilterIdentity()
        val result = IosServerConnection.json("preferences/search-filters.php", buildJsonObject {
            put("action", "delete"); put("name", cleaned)
        }, bearer = identity.second, authenticated = true)
        ensureSearchIdentity(identity)
        require(result.bool("ok") && result.bool("deleted")) { "El servidor no ha confirmat l'eliminació." }
        val actual = searchFiltersRead(identity)
        check(actual.none { it.name.equals(cleaned, ignoreCase = true) }) { "El filtre encara consta al servidor." }
        actual
    }

    // Pas 36: Android's publisher-access, publication-trust, my-published-v312,
    // community-submit and professional-profiles/request endpoints. No local role escalation.
    private suspend fun publisherIdentity(): Triple<Int, String, String> {
        val current = account
        if (current != null) {
            val t = token?.takeIf(String::isNotBlank) ?: error("Sessió professional no disponible")
            require(current.id > 0)
            return Triple(current.id, t, "")
        }
        val guest = guestSession.ensure()
        return Triple(guest.userId, guest.token, guest.guestId)
    }
    private fun checkPublisherIdentity(identity: Triple<Int, String, String>) {
        check(activePersonalId() == identity.first) { "La sessió ha canviat durant l'operació." }
        if (identity.third.isEmpty()) check(account?.id == identity.first && token == identity.second) { "El compte ha canviat." }
        else check(account == null && guestSession.current()?.token == identity.second) { "El convidat ha canviat." }
    }
    private suspend fun ownedPublishedFor(identity: Triple<Int, String, String>): List<IosPublishedEvent> {
        // Android passes guest_id for legacy ownership; authenticated guest bearer also identifies users.id.
        val raw = IosServerConnection.json("events/my-published-v312.php?guest_id=${identity.third.encodeURLParameter()}",
            bearer = identity.second, authenticated = true)
        checkPublisherIdentity(identity)
        return raw.items("items").mapNotNull { item ->
            val id = (item.text("event_id").toIntOrNull() ?: 0)
            if (id <= 0) null else IosPublishedEvent(
                id, item.text("title"), item.text("status"), item.text("event_date"),
                item.text("town"), item.text("category"), item.text("description"), item.text("subtitle"),
                item.text("event_time").take(5), item.text("end_date"), item.text("end_time").take(5),
                item.text("comarca"), item.text("venue"),
                item.names("artists"), item.names("organizers"), item.names("styles"),
                item.text("price").toDoubleOrNull()?.toInt() ?: 0,
                item.bool("ticket_enabled") || item.text("ticket_enabled") == "1", item.text("ticket_url"),
                validMediaUrl(item.text("poster_url")))
        }
    }
    suspend fun publisherOwnEvents(): List<IosPublishedEvent> {
        val identity = publisherIdentity()
        return ownedPublishedFor(identity)
    }
    suspend fun publisherSnapshot(): IosPublisherSnapshot {
        val identity = publisherIdentity()
        val owned = ownedPublishedFor(identity)
        if (identity.third.isNotEmpty()) return IosPublisherSnapshot(identity.first, "GUEST",
            ownedPublishedIds = owned.filter { it.status.equals("published", true) }.map { it.eventId }.toSet(),
            serverConfirmed = true)
        val access = IosServerConnection.json("account/publisher-access.php", bearer = identity.second, authenticated = true)
        checkPublisherIdentity(identity)
        val trust = IosServerConnection.json("account/publication-trust.php", bearer = identity.second, authenticated = true)
        checkPublisherIdentity(identity)
        val role = access.text("role")
        check(role.isNotBlank()) { "El servidor no ha retornat el rol." }
        return IosPublisherSnapshot(identity.first, role,
            managedArtists = (access["managed_artists"] as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull }?.toSet().orEmpty(),
            professionalVerified = access.bool("professional"),
            approvedCount = (trust.text("approved_count").toIntOrNull() ?: 0),
            canAutoPublish = trust.bool("auto_publish_enabled"),
            suspended = trust.bool("suspended_by_report"),
            ownedPublishedIds = owned.filter { it.status.equals("published",true) }.map { it.eventId }.toSet(),
            serverConfirmed = true)
    }
    private fun publishPayload(d: IosPublishDraft, id: Int? = null, guestId: String = ""): JsonObject = buildJsonObject {
        if (id != null) put("event_id", id)
        put("guest_id", guestId)
        put("title", d.title.trim()); put("subtitle", d.subtitle.trim())
        put("date", d.date); put("time", d.time); put("end_date", d.endDate); put("end_time", d.endTime)
        put("town", d.town.trim()); put("comarca", d.comarca.trim()); put("venue", d.venue.trim())
        put("category", d.category); put("description", d.description.trim())
        put("artists", JsonArray(d.artists.map { JsonPrimitive(it.trim()) }.filter { it.content.isNotBlank() }))
        put("organizers", JsonArray(d.organizers.map { JsonPrimitive(it.trim()) }.filter { it.content.isNotBlank() }))
        put("styles", JsonArray(d.styles.map { JsonPrimitive(it.trim()) }.filter { it.content.isNotBlank() }))
        put("price", d.price)
        put("ticket_enabled", d.ticketChannel != "NONE")
        put("ticket_channel", d.ticketChannel)
        if (d.ticketChannel != "NONE") put("ticket_price", d.price)
        put("ticket_url", if (d.ticketChannel == "EXTERNAL") d.ticketUrl.trim() else "")
    }
    suspend fun publisherSubmit(draft: IosPublishDraft): IosPublicationReceipt {
        draft.validate()
        val identity = publisherIdentity()
        // Revalidate moderation restrictions on the server BEFORE the non-idempotent request.
        // For guests the server ultimately enforces review and suspensions.
        if (identity.third.isEmpty()) {
            val snapshot = publisherSnapshot()
            checkPublisherIdentity(identity)
            require(snapshot.userId == identity.first && !snapshot.suspended) {
                "La publicació està suspesa per moderació."
            }
        }
        // NON-idempotent: never automatically repeat after timeout or ambiguous response.
        val reply = IosServerConnection.json("events/community-submit.php", publishPayload(draft, guestId = identity.third),
            identity.second, authenticated = true)
        checkPublisherIdentity(identity)
        val reviewQueued = (reply["review_queued"] as? JsonPrimitive)?.booleanOrNull
            ?: (reply["queued_for_review"] as? JsonPrimitive)?.booleanOrNull
        return IosPublicationReviewPolicy.fromServer(
            reply.text("event_id").toIntOrNull() ?: 0,
            reply.text("status"), reply.text("review_status"), reviewQueued
        )
    }
    suspend fun publisherEdit(eventId: Int, draft: IosPublishDraft) {
        draft.validate()
        require(eventId > 0)
        val identity = publisherIdentity()
        check(ownedPublishedFor(identity).any { it.eventId == eventId && it.status.equals("published", true) }) {
            "La publicació no pertany a este usuari o no està activa."
        }
        IosServerConnection.json("events/community-update.php", publishPayload(draft,eventId,identity.third),
            identity.second, authenticated = true)
        checkPublisherIdentity(identity)
        val verified = ownedPublishedFor(identity).firstOrNull { it.eventId == eventId }
        check(IosPublicationEditPolicy.confirmed(eventId, draft, verified)) {
            "La resposta no acredita tots els canvis de l'edició. Comprova la fitxa abans de reintentar-la."
        }
    }
    /** Pas 41: JPEG picker is native and returns transient base64. Do not persist media locally. */
    suspend fun publisherUploadPoster(eventId: Int, base64Jpeg: String): String {
        require(eventId > 0 && base64Jpeg.length in 500..7_000_000 &&
            base64Jpeg.all { it.isLetterOrDigit() || it == '+' || it == '/' || it == '=' }) {
            "El cartell JPEG no és vàlid o supera la mida admesa."
        }
        val identity = publisherIdentity()
        check(ownedPublishedFor(identity).any { it.eventId == eventId && it.status.equals("published", true) }) {
            "La publicació no pertany a esta sessió."
        }
        // Android's events/community-poster.php accepts JPEG via image_base64.
        // Non-idempotent: after an uncertain reply never silently POST again.
        val reply = IosServerConnection.json("events/community-poster.php", buildJsonObject {
            put("event_id", eventId); put("guest_id", identity.third)
            put("image_focus_x", 0.5); put("image_focus_y", 0.5); put("image_scale", 1.0)
            put("mime_type", "image/jpeg"); put("image_base64", base64Jpeg)
        }, identity.second, authenticated = true)
        checkPublisherIdentity(identity)
        val url = validMediaUrl(reply.text("image_url"))
        require(url.isNotBlank()) { "El servidor no ha retornat l'URL del cartell." }
        val confirmed = ownedPublishedFor(identity).firstOrNull { it.eventId == eventId }
        check(IosPublicationEditPolicy.posterConfirmed(url, confirmed?.posterUrl.orEmpty())) {
            "El cartell s'ha enviat però encara no consta en la fitxa del servidor. Revisa-la abans de repetir."
        }
        return url
    }

    /** Special-event schedule is editable ONLY with the can_edit_schedule permission from PHP. */
    suspend fun publisherSchedule(eventId: Int): IosSpecialScheduleAccess {
        require(eventId > 0 && account != null) { "Cal un compte registrat per gestionar programacions." }
        val identity = publisherIdentity()
        require(identity.third.isEmpty())
        val raw = IosServerConnection.json("special-events/manage/config.php?event_id=$eventId",
            bearer = identity.second, authenticated = true)
        checkPublisherIdentity(identity)
        val serverEventId = (raw["event"] as? JsonObject)?.text("event_id")?.toIntOrNull()
        check(serverEventId == eventId) { "El servidor no ha acreditat este esdeveniment especial." }
        val access = raw["access"] as? JsonObject ?: error("El servidor no ha retornat els permisos.")
        val canEdit = access.bool("can_edit_schedule") || access.text("can_edit_schedule") == "1"
        val rows = raw.items("schedule").mapNotNull { item ->
            val id = item.text("id").toLongOrNull() ?: 0L
            if (id <= 0L) null else {
                val date = item.text("date").ifBlank { item.text("schedule_date") }
                    .ifBlank { item.text("start_at").take(10) }
                val start = item.text("start_time").ifBlank { item.text("start_at").substringAfter(' ', "") }.take(5)
                val end = item.text("end_time").ifBlank { item.text("end_at").substringAfter(' ', "") }.take(5)
                IosSpecialScheduleSlot(id,eventId,item.text("title"),date,start,end,
                    item.text("stage").ifBlank { item.text("location") },item.text("artist_name"),
                    item.text("details"),validMediaUrl(item.text("image_url")),item.text("sort_order").toIntOrNull() ?: 0,
                    item.text("artist_id").toLongOrNull() ?: 0L)
            }
        }
        return IosSpecialScheduleAccess(eventId, canEdit, rows)
    }

    suspend fun publisherSaveSchedule(slot: IosSpecialScheduleSlot): IosSpecialScheduleAccess {
        slot.validate()
        val identity = publisherIdentity()
        require(identity.third.isEmpty()) { "La gestió de festivals requerix un compte registrat." }
        val before = publisherSchedule(slot.eventId)
        require(before.canEdit) { "El servidor no autoritza modificar la programació." }
        if (slot.id > 0L) require(before.slots.any { it.id == slot.id }) { "L'actuació no existeix al servidor." }
        val start = "${slot.date} ${slot.startTime}"
        val end = slot.endTime.takeIf(String::isNotBlank)?.let { "${slot.date} $it" }.orEmpty()
        IosServerConnection.json("special-events/manage/save-schedule.php", buildJsonObject {
            put("event_id", slot.eventId); put("id", slot.id)
            put("title", slot.title.trim()); put("date", slot.date); put("schedule_date", slot.date)
            put("start_time", slot.startTime); put("end_time", slot.endTime)
            put("start_at", start); put("end_at", end)
            put("stage", slot.stage.trim()); put("location", slot.stage.trim())
            put("artist_name", slot.artist.trim())
            if (slot.artistId > 0L) put("artist_id", slot.artistId) else put("artist_id", JsonNull)
            put("details", slot.details.trim())
            put("image_url", slot.imageUrl.trim()); put("sort_order", slot.sortOrder)
            put("active", true)
        }, identity.second, authenticated = true)
        checkPublisherIdentity(identity)
        val after = publisherSchedule(slot.eventId)
        check(after.canEdit && IosSpecialSchedulePolicy.confirmed(slot, after.slots, before.slots.map { it.id }.toSet())) {
            "Canvis no confirmats per la programació del servidor. Comprova-la abans de repetir l'enviament."
        }
        return after
    }

    suspend fun publisherDeleteSchedule(eventId: Int, slotId: Long): IosSpecialScheduleAccess {
        require(eventId > 0 && slotId > 0)
        val identity = publisherIdentity()
        require(identity.third.isEmpty())
        val before = publisherSchedule(eventId)
        require(before.canEdit && before.slots.any { it.id == slotId }) { "L'actuació no és editable." }
        IosServerConnection.json("special-events/manage/delete-schedule.php", buildJsonObject {
            put("event_id", eventId); put("id", slotId)
        }, identity.second, authenticated = true)
        checkPublisherIdentity(identity)
        val after = publisherSchedule(eventId)
        check(after.canEdit && after.slots.none { it.id == slotId }) {
            "L'eliminació no està confirmada pel servidor."
        }
        return after
    }

    suspend fun requestProfessionalClaim(entity: String, type: String, relation: String, contact: String, message: String): String {
        require(account != null) { "Inicia sessió per reclamar un perfil professional." }
        require(entity.isNotBlank() && relation.isNotBlank() && contact.isNotBlank())
        val identity = publisherIdentity()
        val reply = IosServerConnection.json("professional-profiles/request.php", buildJsonObject {
            put("entity_name", entity.trim()); put("profile_type", if(type=="GROUP") "GROUP" else "AGENCY")
            put("relation", relation.trim()); put("contact_email", if ('@' in contact) contact.trim() else "")
            put("contact_phone", if ('@' !in contact) contact.trim() else ""); put("message", message.trim())
        }, identity.second, authenticated = true)
        checkPublisherIdentity(identity)
        return reply.text("message").ifBlank { "Sol·licitud enviada." }
    }
    suspend fun adminPendingProfessionalClaims(): List<JsonObject> {
        val expected = publisherIdentity()
        require(expected.third.isEmpty()) { "Cal un compte registrat." }
        val confirmed = publisherSnapshot()
        require(confirmed.userId == expected.first && IosPublisherRoles.isSuperAdmin(confirmed.role) && confirmed.serverConfirmed) {
            "Accés reservat a superadministració."
        }
        val result = IosServerConnection.json("professional-profiles/pending-requests.php",
            bearer = expected.second, authenticated = true)
        checkPublisherIdentity(expected)
        return result.items("items")
    }
    suspend fun adminReviewProfessionalClaim(claimId: Int, accept: Boolean, notes: String) {
        require(IosAdminActionPolicy.validProfessionalClaim(claimId, accept, notes)) {
            "Indica una decisió vàlida i justifica el rebuig amb 12 caràcters com a mínim."
        }
        val expected = publisherIdentity()
        require(expected.third.isEmpty())
        val before = adminPendingProfessionalClaims()
        require(before.any { it.text("id").toIntOrNull() == claimId }) { "Sol·licitud no pendent o no visible." }
        val receipt = IosServerConnection.json("professional-profiles/review-request.php", buildJsonObject {
            put("claim_id", claimId); put("decision", if (accept) "APPROVE" else "REJECT")
            put("notes", notes.trim().take(1000))
        }, expected.second, authenticated = true)
        checkPublisherIdentity(expected)
        check(!receipt.containsKey("ok") || receipt.bool("ok")) {
            "El servidor no ha confirmat la revisió del perfil."
        }
        check(adminPendingProfessionalClaims().none { it.text("id").toIntOrNull() == claimId }) {
            "La sol·licitud encara està pendent; consulta el servidor abans de repetir la decisió."
        }
    }

    private val sellerActivationMutex = Mutex()

    private fun decodeSellerStatus(root: JsonObject): IosSellerStatus = IosSellerStatus(
        hasBilling = root.bool("has_billing"),
        policiesReady = root.bool("policies_ready"),
        privacyUrl = root.text("privacy_url"),
        salesTermsUrl = root.text("sales_terms_url"),
        companies = root.items("companies").mapNotNull { company ->
            val id = company.text("id").toIntOrNull() ?: 0
            if (id <= 0) null else IosSellerCompany(
                id, company.text("display_name"),
                company.bool("notices_current") || company.text("notices_current") == "1")
        }
    )

    suspend fun sellerOnboardingStatus(): IosSellerStatus {
        require(account != null) { "Cal iniciar sessió o registrar-se per vendre entrades." }
        val identity = publisherIdentity()
        check(identity.third.isEmpty()) { "Els convidats no poden activar la venda." }
        val reply = IosServerConnection.json("seller-onboarding/index.php", bearer = identity.second, authenticated = true)
        checkPublisherIdentity(identity)
        return decodeSellerStatus(reply)
    }

    /** No implicit retries: creating a fiscal company is not an idempotent POST. */
    suspend fun sellerActivate(draft: IosSellerActivation): IosSellerStatus = sellerActivationMutex.withLock {
        draft.validate()
        val identity = publisherIdentity()
        require(identity.third.isEmpty()) { "Cal un compte registrat per activar la venda." }
        val owned = ownedPublishedFor(identity)
        val published = owned.filter { it.status.equals("published", true) }.map { it.eventId }.toSet()
        val before = IosServerConnection.json("seller-onboarding/index.php", bearer = identity.second, authenticated = true)
        checkPublisherIdentity(identity)
        val previous = decodeSellerStatus(before)
        require(IosSellerLegalPolicy.canSubmit(draft, previous, true, published)) {
            "Cal un esdeveniment publicat propi, un titular vàlid i llegir les condicions legals vigents."
        }
        val reply = IosServerConnection.json("seller-onboarding/index.php", buildJsonObject {
            put("event_id", draft.eventId)
            if (draft.existingCompanyId > 0) put("company_id", draft.existingCompanyId)
            else {
                put("kind", draft.kind)
                put("display_name", draft.displayName.trim())
                put("tax_id", draft.taxId.trim().uppercase())
                put("address", draft.address.trim())
                put("postal_code", draft.postalCode.trim())
                put("town", draft.town.trim())
                put("province", draft.province.trim())
            }
            put("accept_privacy_notice", draft.acceptedPrivacy)
            put("accept_sales_terms", draft.acceptedSales)
        }, identity.second, authenticated = true)
        checkPublisherIdentity(identity)
        val companyId = reply.text("company_id").toIntOrNull() ?: 0
        check(IosSellerLegalPolicy.postAcknowledged(
            reply.text("event_id").toIntOrNull() ?: 0,
            companyId, reply.bool("ticket_setup_ready"), reply.bool("ticket_enabled"),
            draft.eventId, draft.existingCompanyId)) {
            "L'alta no està confirmada. Consulta la facturació abans de tornar a enviar-la."
        }
        val verified = IosServerConnection.json("seller-onboarding/index.php", bearer = identity.second, authenticated = true)
        checkPublisherIdentity(identity)
        val current = decodeSellerStatus(verified)
        check(IosSellerLegalPolicy.getConfirmed(current, companyId)) {
            "No s'ha verificat el titular i els consentiments. No repetisques l'alta automàticament."
        }
        current
    }

    private fun decodeSellerSale(eventId: Int, json: JsonObject): IosSellerSaleState {
        val options = json.items("options")
        return IosSellerSaleState(eventId,
            enabled = json.bool("ticket_enabled") || json.text("ticket_enabled") == "1",
            activeOptions = options.count { it.bool("active") || it.text("active") == "1" },
            totalOptions = options.size)
    }

    private suspend fun sellerSaleOptionsFor(eventId: Int, identity: Triple<Int, String, String>): IosSellerSaleState {
        val json = IosServerConnection.json("professional-seller/options.php?event_id=$eventId",
            bearer = identity.second, authenticated = true)
        checkPublisherIdentity(identity)
        return decodeSellerSale(eventId, json)
    }

    suspend fun sellerSaleOptions(eventId: Int): IosSellerSaleState {
        require(account != null && eventId > 0) { "Inicia sessió per gestionar la venda." }
        val identity = publisherIdentity()
        require(identity.third.isEmpty())
        require(ownedPublishedFor(identity).any { it.eventId == eventId && it.status.equals("published", true) }) {
            "Només pots gestionar una activitat pròpia publicada."
        }
        return sellerSaleOptionsFor(eventId, identity)
    }

    /** Activation is separate from fiscal registration and must be explicitly requested. */
    suspend fun sellerSetSaleEnabled(eventId: Int, enable: Boolean): IosSellerSaleState = sellerActivationMutex.withLock {
        require(account != null && eventId > 0)
        val identity = publisherIdentity()
        require(identity.third.isEmpty())
        val own = ownedPublishedFor(identity).filter { it.status.equals("published", true) }.map { it.eventId }.toSet()
        require(eventId in own) { "Esta activitat no és una publicació pròpia confirmada." }
        val before = sellerSaleOptionsFor(eventId, identity)
        if (before.enabled == enable) return@withLock before
        if (enable) {
            val statusJson = IosServerConnection.json("seller-onboarding/index.php",
                bearer = identity.second, authenticated = true)
            checkPublisherIdentity(identity)
            require(IosSellerSalePolicy.canEnable(before, decodeSellerStatus(statusJson), own)) {
                "Cal facturació validada, condicions actuals i almenys una modalitat d'entrada activa."
            }
        }
        // No automated retry: post has side effects, and a lost response is ambiguous.
        IosServerConnection.json("professional-seller/options.php", buildJsonObject {
            put("event_id", eventId)
            put("action", "set_enabled")
            put("enabled", enable)
        }, identity.second, authenticated = true)
        checkPublisherIdentity(identity)
        val after = sellerSaleOptionsFor(eventId, identity)
        check(IosSellerSalePolicy.verified(eventId, enable, after)) {
            "El servidor no ha confirmat el canvi de venda. Consulta l'estat abans de tornar a provar."
        }
        after
    }

    // Pas 43: resum i gestió professional exactament amb els endpoints d'Android v336.
    // No hi ha dades econòmiques persistents al dispositiu.
    private suspend fun proIdentity(): Triple<Int, String, String> {
        require(account != null) { "Inicia sessió per consultar el panell professional." }
        val identity = publisherIdentity()
        require(identity.third.isBlank()) { "Un convidat no pot consultar dades econòmiques." }
        return identity
    }

    private fun decodeProDashboard(raw: JsonObject): IosProDashboard {
        fun JsonObject.integer(k: String) = text(k).toIntOrNull() ?: 0
        fun JsonObject.long(k: String) = text(k).toLongOrNull() ?: 0L
        val required = listOf("companies", "events", "sales", "invoices", "tickets_sold", "orders_count", "gross_cents")
        require(required.all { raw.containsKey(it) } &&
            listOf("companies", "events", "sales", "invoices").all { raw[it] is JsonArray }) {
            "El servidor no ha retornat el resum econòmic complet."
        }
        val data = IosProDashboard(
            companies = raw.items("companies").map { IosProCompany(it.integer("id"), it.text("display_name"),
                it.text("tax_id"), it.text("address"), it.text("postal_code"), it.text("town"), it.text("province"), it.text("kind")) },
            events = raw.items("events").map { IosProEvent(it.integer("id"), it.text("title"), it.text("event_date"),
                it.text("town"), it.text("status"), it.bool("ticket_enabled"), it.integer("company_id").takeIf { n -> n > 0 },
                it.integer("sold"), it.integer("orders"), it.long("gross_cents")) },
            sales = raw.items("sales").map { IosProSale(it.long("id"), it.integer("event_id"), it.text("event_title"),
                it.text("paid_at"), it.integer("quantity"), it.long("total_cents"), it.text("option_name").ifBlank { "Entrada general" }) },
            invoices = raw.items("invoices").map { IosProInvoice(it.text("invoice_number"), it.text("company"),
                it.text("issued_at"), it.long("total_cents"), it.text("status")) },
            sold = raw.integer("tickets_sold"), orders = raw.integer("orders_count"),
            grossCents = raw.long("gross_cents"), eventsTruncated = raw.bool("events_truncated")
        )
        check(IosProfessionalSellerPolicy.validSummary(data)) { "El servidor ha retornat dades econòmiques incompletes o invàlides." }
        return data
    }

    private suspend fun proDashboardFor(identity: Triple<Int, String, String>, company: Int, period: String): IosProDashboard {
        require(company >= 0 && IosProfessionalSellerPolicy.validPeriod(period))
        val raw = IosServerConnection.json("professional-seller/dashboard.php?company_id=$company&period=$period",
            bearer = identity.second, authenticated = true)
        checkPublisherIdentity(identity)
        return decodeProDashboard(raw)
    }
    suspend fun professionalDashboard(companyId: Int = 0, period: String = "all"): IosProDashboard =
        proDashboardFor(proIdentity(), companyId, period)

    private suspend fun proOptionsFor(identity: Triple<Int, String, String>, eventId: Int): IosProTicketCatalog {
        require(eventId > 0)
        val raw = IosServerConnection.json("professional-seller/options.php?event_id=$eventId",
            bearer = identity.second, authenticated = true)
        checkPublisherIdentity(identity)
        require(raw["options"] is JsonArray && raw.containsKey("ticket_enabled")) {
            "El servidor no ha retornat un catàleg de venda complet."
        }
        val responseId = raw.text("event_id").toIntOrNull()
        check(responseId == null || responseId == eventId) { "El servidor ha retornat una altra activitat." }
        val options = raw.items("options").map { item ->
            val cap = item.text("capacity").toIntOrNull()
            IosProTicketOption(item.text("id").toLongOrNull() ?: 0L, item.text("name"),
                item.text("price_cents").toLongOrNull() ?: -1L, cap,
                item.text("sold").toIntOrNull() ?: 0, item.text("reserved").toIntOrNull() ?: 0,
                item.text("max_per_order").toIntOrNull() ?: 6,
                item.text("session_date"), item.text("session_time"), item.text("sector"),
                item.text("offer"), item.text("sale_starts_at"), item.text("sale_ends_at"), item.bool("active"))
        }
        check(options.all { it.id > 0 && it.cents >= 0 && it.sold >= 0 && it.reserved >= 0 &&
            it.maxPerOrder in 1..20 && (it.capacity == null || it.capacity.toLong() >= it.sold.toLong() + it.reserved.toLong()) }) {
            "El catàleg de modalitats del servidor és invàlid."
        }
        return IosProTicketCatalog(eventId, raw.bool("ticket_enabled"), options)
    }
    suspend fun professionalTicketOptions(eventId: Int): IosProTicketCatalog {
        val identity = proIdentity()
        val dashboard = proDashboardFor(identity, 0, "all")
        require(dashboard.events.any { it.id == eventId }) { "No tens permisos sobre este esdeveniment." }
        return proOptionsFor(identity, eventId)
    }

    /** Panell professional: Android deixa gestionar activitats reclamades pel perfil autoritzat,
     * no només esdeveniments d'autoria directa. El PHP és l'autoritat final del permís.
     */
    suspend fun professionalSetSaleEnabled(eventId: Int, enabled: Boolean): IosProTicketCatalog = sellerActivationMutex.withLock {
        val identity = proIdentity()
        val dashboard = proDashboardFor(identity, 0, "all")
        val event = dashboard.events.singleOrNull { it.id == eventId }
            ?: error("L'activitat no figura entre les gestionables al servidor.")
        val before = proOptionsFor(identity, eventId)
        if (before.enabled == enabled) return@withLock before
        if (enabled) {
            require(event.companyId != null && before.options.any { it.active }) {
                "Assigna un titular i almenys una modalitat activa abans d'activar la venda."
            }
            val fiscal = decodeSellerStatus(IosServerConnection.json("seller-onboarding/index.php",
                bearer = identity.second, authenticated = true))
            checkPublisherIdentity(identity)
            require(IosSellerLegalPolicy.policiesUsable(fiscal) && fiscal.hasBilling &&
                IosSellerLegalPolicy.selectedCompany(fiscal, event.companyId)?.noticesCurrent == true) {
                "El titular fiscal no té els consentiments vigents verificats."
            }
        }
        IosServerConnection.json("professional-seller/options.php", buildJsonObject {
            put("action", "set_enabled"); put("event_id", eventId); put("enabled", enabled)
        }, bearer = identity.second, authenticated = true)
        checkPublisherIdentity(identity)
        val after = proOptionsFor(identity, eventId)
        check(after.enabled == enabled && after.eventId == eventId) {
            "El servidor no confirma el canvi de venda; consulta l'estat abans de reintentar."
        }
        after
    }

    /** Upsert no idempotent: consulta GET abans i després; davant timeout cal refrescar, no repetir. */
    suspend fun professionalSaveTicketOption(eventId: Int, draft: IosProTicketDraft): IosProTicketCatalog =
        sellerActivationMutex.withLock {
            val identity = proIdentity()
            val dashboard = proDashboardFor(identity, 0, "all")
            require(dashboard.events.any { it.id == eventId && it.companyId != null }) {
                "Cal un esdeveniment propi amb titular fiscal assignat."
            }
            val before = proOptionsFor(identity, eventId)
            val existing = before.options.singleOrNull { it.id == draft.id }
            require(draft.id == 0L || existing != null) { "Esta modalitat no pertany al catàleg actual." }
            require(IosProfessionalSellerPolicy.validOption(draft, existing)) { "Comprova els preus, les dates, el contingent i la zona." }
            val receipt = IosServerConnection.json("professional-seller/options.php", buildJsonObject {
                put("event_id", eventId); put("action", "upsert"); put("id", draft.id)
                put("name", draft.name.trim()); put("price_cents", draft.cents)
                if (draft.capacity == null) put("capacity", JsonNull) else put("capacity", draft.capacity)
                put("max_per_order", draft.maxPerOrder)
                if (draft.date.isBlank()) put("session_date", JsonNull) else put("session_date", draft.date)
                if (draft.time.isBlank()) put("session_time", JsonNull) else put("session_time", draft.time)
                put("sector", draft.sector.trim()); put("offer", draft.offer.trim())
                put("sale_starts_at", draft.starts); put("sale_ends_at", draft.ends); put("active", draft.active)
            }, identity.second, authenticated = true)
            checkPublisherIdentity(identity)
            val after = proOptionsFor(identity, eventId)
            val acknowledged = receipt.text("option_id").toLongOrNull() ?: receipt.text("id").toLongOrNull() ?: 0L
            check(IosProfessionalSellerPolicy.verifiedUpsert(before, after, draft, acknowledged)) {
                "El servidor encara no confirma la modalitat. Consulta el catàleg abans de reintentar."
            }
            after
        }

    suspend fun professionalImportTicketDrafts(eventId: Int): IosProTicketCatalog = sellerActivationMutex.withLock {
        val identity = proIdentity()
        val dashboard = proDashboardFor(identity, 0, "all")
        require(dashboard.events.any { it.id == eventId && it.companyId != null }) { "Titular fiscal no assignat." }
        val before = proOptionsFor(identity, eventId)
        require(before.options.isEmpty()) { "La importació només es permet si no hi ha modalitats creades." }
        IosServerConnection.json("events/ticket-drafts.php", buildJsonObject {
            put("action", "import"); put("event_id", eventId)
        }, identity.second, authenticated = true)
        checkPublisherIdentity(identity)
        val after = proOptionsFor(identity, eventId)
        check(after.options.isNotEmpty()) { "No es confirma la importació; consulta el servidor abans de repetir-la." }
        after
    }

    suspend fun professionalAssignCompany(eventId: Int, companyId: Int): IosProDashboard = sellerActivationMutex.withLock {
        val identity = proIdentity()
        val before = proDashboardFor(identity, 0, "all")
        require(IosProfessionalSellerPolicy.canAssign(before, eventId, companyId)) { "Empresa o activitat no autoritzada." }
        if (IosProfessionalSellerPolicy.confirmedAssignment(before, eventId, companyId)) return@withLock before
        IosServerConnection.json("professional-seller/assign-company.php", buildJsonObject {
            put("event_id", eventId); put("company_id", companyId)
        }, identity.second, authenticated = true)
        checkPublisherIdentity(identity)
        val after = proDashboardFor(identity, 0, "all")
        check(IosProfessionalSellerPolicy.confirmedAssignment(after, eventId, companyId)) {
            "No està confirmada l'assignació fiscal. Consulta el servidor abans de repetir-la."
        }
        after
    }

    suspend fun professionalRegisterExternalInvoice(companyId: Int, number: String, date: String,
        cents: Long, description: String, alreadyIssued: Boolean): IosProDashboard = sellerActivationMutex.withLock {
        val identity = proIdentity()
        val before = proDashboardFor(identity, 0, "all")
        require(IosProfessionalSellerPolicy.canRegisterInvoice(before, companyId, number, date, cents, alreadyIssued)) {
            "Cal una factura externa ja emesa, import vàlid i empresa autoritzada."
        }
        IosServerConnection.json("professional-seller/invoices.php", buildJsonObject {
            put("company_id", companyId); put("invoice_number", number.trim())
            put("issued_at", date); put("total_amount", "${cents / 100}.${(cents % 100).toString().padStart(2,'0')}")
            put("description", description.trim().take(500))
        }, identity.second, authenticated = true)
        checkPublisherIdentity(identity)
        val after = proDashboardFor(identity, 0, "all")
        check(IosProfessionalSellerPolicy.confirmedInvoice(before, after, number.trim())) {
            "El registre de factura no està confirmat. Consulta les factures abans de repetir."
        }
        after
    }

    suspend fun profileSettings(): IosProfileSettings {
        val expected = registeredProfileIdentity()
        val r = IosServerConnection.json("account/profile-settings.php", bearer = expected.third, authenticated = true)
        checkRegisteredProfileIdentity(expected)
        return IosProfileSettings(r.text("role"), r.text("phone"), r.bool("email_verified"))
    }
    suspend fun saveProfileSettings(role: String, phone: String): IosProfileSettings = profileWriteMutex.withLock {
        val expected = registeredProfileIdentity()
        require(account?.role?.uppercase() != "SUPER_ADMIN") { "El rol d'administració no s'edita des del perfil" }
        require(role in listOf("PUBLIC", "PROFESSIONAL")) { "Tipus de compte no vàlid." }
        val desiredPhone = if (role == "PROFESSIONAL") phone.trim() else ""
        if (role == "PROFESSIONAL") {
            require(desiredPhone.matches(Regex("[+0-9() .-]{7,24}"))) { "Indica un telèfon professional vàlid." }
        }
        val r = IosServerConnection.json("account/profile-settings.php", buildJsonObject {
            put("role", role); put("phone", desiredPhone)
        }, expected.third, authenticated = true)
        checkRegisteredProfileIdentity(expected)
        check(r.text("role").uppercase() == role && r.text("phone") == desiredPhone) {
            "El servidor no ha confirmat el tipus de compte"
        }
        val verified = IosServerConnection.json("account/profile-settings.php", bearer = expected.third, authenticated = true)
        checkRegisteredProfileIdentity(expected)
        check(verified.text("role").uppercase() == role && verified.text("phone") == desiredPhone) {
            "La configuració encara no consta al servidor"
        }
        IosProfileSettings(verified.text("role"), verified.text("phone"), verified.bool("email_verified"))
    }

    /** Guests and registered accounts read the same MariaDB user_follows table.
     * A slow GET never rolls back follows already accepted into the encrypted queue.
     */
    suspend fun userState(): IosUserState {
        val expectedId = activePersonalId() ?: ensureGuestIdentity()
        val remote = request("user-state/mine.php", authenticated = true)
        check(activePersonalId() == expectedId) { "La sessió ha canviat durant la lectura" }
        return reconcilePendingUserState(decodeUserState(remote), expectedId)
    }
    /** Reconcile both follows and saved IDs using unconfirmed per-user encrypted actions. */
    internal suspend fun reconcilePendingUserState(server: IosUserState, expectedId: Int): IosUserState {
        val followed = reconcilePendingFollows(server, expectedId)
        check(activePersonalId() == expectedId) { "Ha canviat l'usuari durant els guardats" }
        val pending = pendingQueue?.pendingSavedActions(expectedId).orEmpty().map { it.payload }
        val projected = IosSavedEventContract.project(followed.saved, pending)
        check(activePersonalId() == expectedId) { "Ha canviat l'usuari durant la projecció" }
        val alertActions = pendingQueue?.pendingEventAlertActions(expectedId).orEmpty().map { action ->
            (action.payload["event_id"]?.jsonPrimitive?.intOrNull ?: error("ID de recordatori invàlid")) to
                (action.payload["active"]?.jsonPrimitive?.booleanOrNull ?: error("Campaneta invàlida"))
        }
        return followed.copy(saved = projected, alerts = IosReminderPolicy.projectedAlerts(followed.alerts, alertActions))
    }
    internal suspend fun reconcilePendingFollows(server: IosUserState, expectedId: Int): IosUserState {
        check(activePersonalId() == expectedId) { "Identitat incorrecta en els seguiments" }
        val pending = pendingQueue?.pendingFollowActions(expectedId).orEmpty()
        var projected = server.follows
        pending.forEach { action ->
            val payload = action.payload
            val type = payload.text("entity_type").uppercase()
            val key = payload.text("entity_key")
            val label = payload.text("entity_label").ifBlank { key }
            val follow = payload["following"]?.jsonPrimitive?.booleanOrNull ?: error("Seguiment en cua invàlid")
            projected = IosFollowMutation.updated(projected, type, key, label, follow)
        }
        check(activePersonalId() == expectedId) { "Sessió canviada durant la projecció" }
        return server.copy(follows = projected)
    }
    internal fun decodeUserState(root: JsonObject): IosUserState {
        return IosUserState(
            follows = root.items("follows").map { IosFollow(it.text("entity_type"), it.text("entity_key"), it.text("entity_label")) },
            saved = root.items("event_states").filter { it.bool("saved") }.mapNotNull { it.text("event_id").toIntOrNull() }.toSet(),
            alerts = root.arr("event_alerts").mapNotNull { (it as? JsonPrimitive)?.intOrNull }.toSet(),
            affinities = root.items("affinities").mapNotNull(IosServerAffinity::decode)
        )
    }
    suspend fun saveEvent(event: AgendaEventSummary, saved: Boolean,
                          userTown: String = "", userComarca: String = "") {
        val id = activePersonalId() ?: ensureGuestIdentity()
        queuedWrite(id, "post", IosSavedEventContract.payload(event, saved, userTown, userComarca),
            "events/save.php")
    }
    suspend fun alertEvent(event: AgendaEventSummary, active: Boolean) {
        val id = activePersonalId() ?: ensureGuestIdentity()
        queuedWrite(id, "post", buildJsonObject {
            put("event_id", event.id); put("entity_label", event.title); put("active", active)
        }, "user-state/event-alert.php")
    }
    /** Queue BOTH state and event alert before sending either; offline cannot lose the second half.
     * sync/state.php projects when_ID to agenda_user_reminders for the server dispatcher.
     * A missing event_ids snapshot MUST NOT be replaced with an empty list.
     */
    private val reminderWriteMutex = Mutex()
    suspend fun configureEventReminder(event: AgendaEventSummary, enabled: Boolean, atMillis: Long,
                                       knownEventIds: Set<String>?): Unit = reminderWriteMutex.withLock {
        IosReminderPolicy.requireFuture(event.id, enabled, atMillis, currentEpochMillis())
        val owner = activePersonalId() ?: ensureGuestIdentity()
        val bearer = personalBearer(owner)
        val queue = pendingQueue ?: error("No es pot guardar sense cua xifrada")
        // Prefer the current canonical server list, then any local, unacknowledged update.
        val freshIds = runCatching {
            val root = IosServerConnection.json("sync/state.php", bearer = bearer, authenticated = true)
            val snapshot = IosCentralStateSnapshot.decode(root, owner)
            if ("event_ids" in snapshot.values) snapshot.strings("event_ids")
                ?: error("El servidor ha retornat event_ids amb un tipus invàlid")
            else emptySet()
        }.getOrNull() ?: knownEventIds
        var ids = freshIds
        queue.pendingStateActions(owner).forEach { action ->
            (action.payload["mutations"] as? JsonArray).orEmpty().forEach { mutation ->
                val row = mutation as? JsonObject ?: return@forEach
                if (row.text("key") == "event_ids") {
                    ids = ((row["value"] as? JsonObject)?.get("v") as? JsonArray)
                        ?.mapNotNull { it.jsonPrimitive.contentOrNull }?.toSet() ?: ids
                }
            }
        }
        val changedIds = IosReminderPolicy.updatedIds(ids, event.id, enabled)
        val values = linkedMapOf(
            "when_${event.id}" to IosPersonalEncoded.long(if (enabled) atMillis else 0L),
            "title_${event.id}" to IosPersonalEncoded.string(event.title.take(160)),
            "town_${event.id}" to IosPersonalEncoded.string(event.town.take(120)),
            "custom_${event.id}" to IosPersonalEncoded.bool(enabled)
        )
        if (changedIds != null) values["event_ids"] = IosPersonalEncoded.strings(changedIds)
        val state = buildJsonObject {
            put("mutations", buildJsonArray {
                values.forEach { (key, value) -> add(buildJsonObject {
                    put("id", IosPersonalMutationId.make()); put("key", key); put("op", "put"); put("value", value)
                }) }
            })
        }
        queue.append(IosQueuedAction(IosPersonalMutationId.make(), owner, "state", state))
        queue.append(IosQueuedAction(IosPersonalMutationId.make(), owner, "post", buildJsonObject {
            put("event_id", event.id); put("entity_label", event.title); put("active", enabled)
        }, "user-state/event-alert.php"))
        queue.flush(owner, bearer)
        if (queue.pendingStateActions(owner).any { action ->
                (action.payload["mutations"] as? JsonArray).orEmpty().any {
                    (it as? JsonObject)?.text("key") == "when_${event.id}"
                }
            } || queue.pendingEventAlertActions(owner).any {
                it.payload["event_id"]?.jsonPrimitive?.intOrNull == event.id
            }) throw IosQueuedForRetryException()
        check(activePersonalId() == owner) { "La sessió ha canviat després del recordatori" }
    }

    suspend fun followEntity(type: String, entityKey: String, entityLabel: String, following: Boolean) {
        require(type in IosFollowMutation.supported) { "Tipus de seguiment invàlid." }
        require(IosFollowMutation.canonicalKey(entityKey).isNotBlank() && entityLabel.isNotBlank()) { "Falta l'entitat." }
        require(entityKey.length <= 190 && entityLabel.length <= 190) { "Nom d'entitat massa llarg." }
        val id = activePersonalId() ?: ensureGuestIdentity()
        queuedWrite(id, "post", buildJsonObject {
            put("entity_type", type); put("entity_key", entityKey.trim())
            put("entity_label", entityLabel.trim()); put("following", following)
        }, "user-state/follow.php")
    }
    suspend fun followArtist(artist: String, following: Boolean) =
        followEntity("ARTIST", artist.trim(), artist.trim(), following)

    /** Android's ORGANIZER follow uses the user's username as entity_key.
     * Resolve a display name only when a UNIQUE public user profile matches.
     * Ambiguous/unclaimed professional profiles must never be silently guessed.
     */
    suspend fun publicOrganizerAccountKeys(): Map<String, String> {
        val root = request("user-profiles/catalog.php")
        val users = root.items("profiles").mapNotNull { row ->
            val username = row.text("username")
            val label = row.text("display_name").ifBlank { username }
            if (username.isBlank()) null else IosFollowMutation.canonicalKey(label) to username
        }
        return users.groupBy({ it.first }, { it.second }).mapNotNull { (label, keys) ->
            keys.distinct().singleOrNull()?.let { label to it }
        }.toMap()
    }
    /** Android v336 visibility/index.php is the only server authority for hidden items. */
    suspend fun visibility(): IosVisibilitySnapshot {
        val owner = activePersonalId() ?: ensureGuestIdentity()
        val remote = IosVisibilitySnapshot.decode(request("visibility/index.php", authenticated = true))
        return reconcilePendingVisibility(remote, owner)
    }
    internal suspend fun reconcilePendingVisibility(remote: IosVisibilitySnapshot, owner: Int): IosVisibilitySnapshot {
        check(activePersonalId() == owner) { "Ha canviat l'usuari" }
        var projected = remote
        pendingQueue?.pendingVisibilityActions(owner).orEmpty().forEach { queued ->
            projected = IosVisibilityAffinityContract.apply(projected,
                IosVisibilityMutation(queued.payload.text("kind"), queued.payload.text("target"),
                    queued.payload.text("action")))
        }
        check(activePersonalId() == owner) { "Ha canviat l'usuari" }
        return projected
    }
    /** Each differential write is queued before HTTPS; never overwrite another device's whole set. */
    suspend fun updateVisibility(owner: Int, previous: IosVisibilitySnapshot, desired: IosVisibilitySnapshot): Boolean {
        check(activePersonalId() == owner)
        var delayed = false
        for (mutation in IosVisibilityAffinityContract.mutations(previous, desired)) {
            try {
                queuedWrite(owner, "post", mutation.payload(), "visibility/index.php")
            } catch (_: IosQueuedForRetryException) {
                delayed = true // continue queuing all changes, even if offline
            }
        }
        return delayed
    }
    /** Android's affinities/record.php; dedupe_key survives network retries. */
    suspend fun recordSearchAffinity(term: String) {
        val owner = activePersonalId() ?: ensureGuestIdentity()
        queuedWrite(owner, "post", IosAffinitySignals.search(term, IosPersonalMutationId.make()), "affinities/record.php")
    }
    suspend fun recordOpenAffinity(event: AgendaEventSummary) {
        val owner = activePersonalId() ?: ensureGuestIdentity()
        queuedWrite(owner, "post", IosAffinitySignals.open(event, IosPersonalMutationId.make()), "affinities/record.php")
    }
    /** A queued personal mutation makes any already-prepared Android cache unsafe to reuse. */
    internal suspend fun hasPendingPersonalWrites(owner: Int): Boolean {
        check(activePersonalId() == owner)
        return pendingQueue?.countFor(owner)?.let { it > 0 } ?: true
    }

    /** Step 12. Read Android's REAL prepared cache only with its signed hard key.
     * Otherwise ask the existing server selection engine to calculate from MariaDB.
     * Never overwrite Android's for-you/index.php with an iOS approximation.
     */
    internal suspend fun forYouServerSelection(expectedOwner: Int, androidSignature: Int?,
        culturalDay: String, preferences: JsonObject): IosForYouServerIds {
        val bearer = personalBearer(expectedOwner)
        check(activePersonalId() == expectedOwner)
        if (androidSignature != null && culturalDay.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
            val cached = try {
                IosForYouServerIds.androidCache(IosServerConnection.json(
                    "for-you/index.php?sig=$androidSignature&day=$culturalDay",
                    bearer = bearer, authenticated = true))
            } catch (cancel: kotlinx.coroutines.CancellationException) {
                throw cancel
            } catch (_: Exception) {
                null // Old cache unavailable: use the authenticated server-v1 engine.
            }
            if (cached != null) {
                check(activePersonalId() == expectedOwner && personalBearer(expectedOwner) == bearer)
                return cached
            }
        }
        val result = IosForYouServerIds.serverCalculation(IosServerConnection.json(
            "for-you/feed.php", preferences, bearer, authenticated = true))
        check(activePersonalId() == expectedOwner && personalBearer(expectedOwner) == bearer) {
            "El compte ha canviat mentre calculàvem Per a tu"
        }
        return result
    }

    /** Pas 27: same public professional-profile, entity image and artist-store contracts as Android v336.
     * Never take the first LIKE match: the exact normalized name must match.
     * Missing GROUP profile means there is no artist store, not a fabricated catalogue.
     */
    suspend fun artistPublicPage(name: String): IosArtistPublicPage = coroutineScope {
        val wanted = name.trim().takeIf { it.isNotBlank() && it.length <= 190 }
            ?: error("Nom d'artista no vàlid")
        val encoded = wanted.encodeURLParameter()
        val profilesJob = async { runCatching {
            request("professional-profiles/catalog.php?type=GROUP&q=$encoded").items("profiles")
        } }
        // Images endpoint includes ARTIST photos even when there is no claimed GROUP profile.
        val imagesJob = async { runCatching { request("entities/images.php").items("entities") } }
        val storeJob = async { runCatching {
            request("artist-store/catalog.php?artist=$encoded")
        } }
        val profilesResult = profilesJob.await()
        val entitiesResult = imagesJob.await()
        val result = storeJob.await()
        // Do not silently claim "no public products" when all public APIs are unreachable.
        if (profilesResult.isFailure && entitiesResult.isFailure && result.isFailure) {
            error("No s'ha pogut carregar el perfil des del servidor.")
        }
        val matched = profilesResult.getOrDefault(emptyList()).firstOrNull {
            it.text("profile_type") == "GROUP" && IosArtistPagePolicy.sameArtist(it.text("name"), wanted)
        }
        val entity = entitiesResult.getOrDefault(emptyList()).firstOrNull {
            it.text("type") == "ARTIST" && IosArtistPagePolicy.sameArtist(it.text("name"), wanted)
        }
        val photo = IosArtistPagePolicy.imageUrl(matched?.text("image_url").orEmpty())
            .ifBlank { IosArtistPagePolicy.imageUrl(entity?.text("image_url").orEmpty()) }
        val profile = if (matched != null || photo.isNotBlank()) IosArtistProfessional(
            wanted, photo, matched?.text("style_primary").orEmpty(),
            matched?.text("language_primary").orEmpty()) else null
        val catalog = result.getOrNull()
        val officialName = catalog?.text("artist").orEmpty()
        // Do not display merchandise belonging to an artist with a similar name.
        val products = if (catalog != null && IosArtistPagePolicy.sameArtist(officialName, wanted))
            catalog.items("products").mapNotNull { row ->
                val id = row.text("id").toLongOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
                val title = row.text("name").takeIf(String::isNotBlank) ?: return@mapNotNull null
                IosArtistProduct(id, wanted, title, row.text("product_type"), row.text("description"),
                    IosArtistPagePolicy.imageUrl(row.text("image_url")),
                    row.text("price_cents").toLongOrNull()?.coerceAtLeast(0) ?: 0,
                    row.text("shipping_cents").toLongOrNull()?.coerceAtLeast(0) ?: 0,
                    row["stock"]?.let { if (it is JsonNull) null else it.jsonPrimitive.intOrNull },
                    row.text("active").let { it.isBlank() || it == "1" || it.equals("true", true) })
            } else emptyList()
        IosArtistPublicPage(wanted, profile, products,
            if (result.isFailure && matched != null) "No s'ha pogut carregar el catàleg de productes."
            else if (result.isFailure && profilesResult.isFailure) "No s'ha pogut verificar la botiga de l'artista."
            else "")
    }

    // PAS 45: store authority is PHP/MariaDB. Only Stripe's PaymentSheet uses native UI.
    private val artistStoreMutex = Mutex()
    private var pendingArtistOrder: IosStorePayment? = null // volatile RAM only, never UserDefaults
    private var uncertainArtistCreation = false
    internal fun pendingArtistStoreOrder(): IosStorePayment? =
        pendingArtistOrder?.takeIf { it.ownerId == activePersonalId() }
    internal fun artistStoreCreationUncertain(): Boolean = uncertainArtistCreation
    internal fun clearArtistStoreOrder(confirmedId: Long) {
        if (pendingArtistOrder?.orderId == confirmedId) pendingArtistOrder = null
    }
    /** A lost POST response may have created an order: user must explicitly decide on retry. */
    internal fun acknowledgeUncertainArtistOrder() { uncertainArtistCreation = false }

    private fun storeIdentity(): Pair<Int, String> {
        val user = account?.id?.takeIf { it > 0 } ?: error("Cal iniciar sessió per a comprar o gestionar la botiga.")
        val bearer = token?.takeIf(String::isNotBlank) ?: error("Falta el compte registrat.")
        return user to bearer
    }
    private fun checkStoreIdentity(identity: Pair<Int, String>) {
        check(account?.id == identity.first && token == identity.second) { "Ha canviat el compte durant la compra." }
    }
    internal suspend fun managedArtistStoreNames(): List<String> {
        val identity = storeIdentity()
        val raw = IosServerConnection.json("professional-profiles/mine.php", bearer = identity.second, authenticated = true)
        checkStoreIdentity(identity)
        check(raw["managed_artists"] is JsonArray) { "No es poden verificar els artistes que gestiones." }
        return raw.arr("managed_artists").mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.trim() }
            .filter(IosArtistStorePolicy::validArtist).distinctBy(IosArtistPagePolicy::key)
    }
    private suspend fun requireManagedArtist(identity: Pair<Int, String>, artist: String) {
        checkStoreIdentity(identity)
        require(managedArtistStoreNames().any { IosArtistPagePolicy.sameArtist(it, artist) }) {
            "El servidor no confirma que gestiones este artista."
        }
        checkStoreIdentity(identity)
    }
    private fun storeProducts(root: JsonObject, artist: String): List<IosArtistProduct> {
        check(root.text("artist").let { IosArtistPagePolicy.sameArtist(it, artist) }) {
            "El catàleg correspon a un artista diferent."
        }
        check(root["products"] is JsonArray) { "Falta la llista de productes del servidor." }
        return root.items("products").mapNotNull { item ->
            val id = item.text("id").toLongOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
            val title = item.text("name").takeIf(String::isNotBlank) ?: return@mapNotNull null
            IosArtistProduct(id, item.text("artist_name").ifBlank { artist }, title,
                item.text("product_type").ifBlank { "MERCH" }, item.text("description"),
                IosArtistPagePolicy.imageUrl(item.text("image_url")),
                item.text("price_cents").toLongOrNull() ?: 0L,
                item.text("shipping_cents").toLongOrNull() ?: 0L,
                item["stock"]?.let { if (it is JsonNull) null else it.jsonPrimitive.intOrNull },
                item.text("active").let { it.isBlank() || it == "1" || it.equals("true", true) })
        }.filter { IosArtistPagePolicy.sameArtist(it.artist, artist) }
    }
    internal suspend fun artistStoreCatalog(artist: String): List<IosArtistProduct> {
        require(IosArtistStorePolicy.validArtist(artist))
        val identity = activePersonalId()
        val raw = request("artist-store/catalog.php?artist=${artist.trim().encodeURLParameter()}")
        if (identity != null) check(activePersonalId() == identity) { "La sessió ha canviat." }
        return storeProducts(raw, artist)
    }
    internal suspend fun artistStoreSave(d: IosStoreDraft): List<IosArtistProduct> = artistStoreMutex.withLock {
        require(IosArtistStorePolicy.validDraft(d))
        val identity = storeIdentity()
        requireManagedArtist(identity, d.artist)
        val before = artistStoreCatalog(d.artist)
        if (d.id > 0) require(before.any { it.id == d.id }) { "Producte absent del catàleg oficial." }
        val response = IosServerConnection.json("artist-store/save-product.php", buildJsonObject {
            put("artist", d.artist); put("id", d.id); put("name", d.name.trim())
            put("product_type", d.type); put("description", d.description.trim())
            if (d.imageUrl.isBlank()) put("image_url", JsonNull) else put("image_url", d.imageUrl.trim())
            put("price_cents", d.priceCents); put("shipping_cents", d.shippingCents)
            if (d.stock == null) put("stock", JsonNull) else put("stock", d.stock)
            put("active", true)
        }, identity.second, authenticated = true)
        checkStoreIdentity(identity)
        val after = artistStoreCatalog(d.artist)
        check(IosArtistStorePolicy.saved(d, before, after,
            response.text("id").toLongOrNull() ?: response.text("product_id").toLongOrNull() ?: 0L)) {
            "No s'ha confirmat el producte al servidor. Comprova el catàleg abans de repetir el guardat."
        }
        after
    }
    internal suspend fun artistStoreDelete(artist: String, id: Long): List<IosArtistProduct> = artistStoreMutex.withLock {
        require(id > 0 && IosArtistStorePolicy.validArtist(artist))
        val identity = storeIdentity(); requireManagedArtist(identity, artist)
        val before = artistStoreCatalog(artist)
        require(before.any { it.id == id }) { "Este producte ja no existix." }
        IosServerConnection.json("artist-store/delete-product.php", buildJsonObject {
            put("artist", artist); put("id", id)
        }, identity.second, authenticated = true)
        checkStoreIdentity(identity)
        val after = artistStoreCatalog(artist)
        check(IosArtistStorePolicy.deleted(id, before, after)) { "El servidor no confirma l'eliminació." }
        after
    }
    internal suspend fun artistStoreCreatePayment(product: IosArtistProduct, quantity: Int,
        address: IosStoreAddress): IosStorePayment = artistStoreMutex.withLock {
        val identity = storeIdentity()
        require(IosArtistStorePolicy.validAddress(address)) { "Completa les dades d'enviament." }
        require(IosArtistStorePolicy.canBuy(product, quantity)) { "Quantitat o existències no disponibles." }
        check(pendingArtistOrder?.takeIf { it.ownerId == identity.first } == null && !uncertainArtistCreation) {
            "Ja hi ha una compra pendent o incerta. Comprova-la abans de crear una altra comanda."
        }
        val live = artistStoreCatalog(product.artist).singleOrNull { it.id == product.id }
            ?: error("El producte ja no està disponible.")
        require(IosArtistStorePolicy.canBuy(live, quantity)) { "El servidor indica que no hi ha prou existències." }
        uncertainArtistCreation = true // no silent retry if POST was received but response lost
        val root = IosServerConnection.json("artist-store/payment-create.php", buildJsonObject {
            put("product_id", product.id); put("quantity", quantity)
            put("recipient_name", address.recipient.trim()); put("shipping_address", address.street.trim())
            put("shipping_postal_code", address.postcode.trim())
            put("shipping_city", address.city.trim()); put("shipping_country", address.country.trim())
        }, identity.second, authenticated = true)
        checkStoreIdentity(identity)
        val start = IosStorePayment(root.text("order_id").toLongOrNull() ?: 0L,
            product.id, identity.first, root.text("client_secret"), root.text("publishable_key"),
            root.text("amount_cents").toLongOrNull() ?: 0L, root.text("currency").ifBlank { "eur" },
            root.text("merchant_name").ifBlank { "l'Agenda" }, root.text("mode").ifBlank { "test" })
        check(IosArtistStorePolicy.validPayment(start)) { "Resposta Stripe no vàlida; consulta la comanda abans de repetir." }
        pendingArtistOrder = start
        uncertainArtistCreation = false
        start
    }
    internal suspend fun artistStoreStatus(orderId: Long): IosStorePaidOrder {
        require(orderId > 0)
        val identity = storeIdentity()
        val raw = IosServerConnection.json("artist-store/payment-status.php?order_id=$orderId",
            bearer = identity.second, authenticated = true)
        checkStoreIdentity(identity)
        val result = IosStorePaidOrder(raw.text("order_id").toLongOrNull() ?: 0L,
            raw.text("status"), raw.text("product_name"), raw.text("quantity").toIntOrNull() ?: 0,
            raw.text("product_total_cents").toLongOrNull() ?: 0L,
            raw.text("shipping_cents").toLongOrNull() ?: 0L,
            raw.text("total_cents").toLongOrNull() ?: 0L)
        check(result.id == orderId) { "La resposta és d'una altra comanda." }
        return result
    }
    internal suspend fun artistStoreOrders(artist: String): List<IosStoreOrder> {
        val identity = storeIdentity(); requireManagedArtist(identity, artist)
        val raw = IosServerConnection.json("artist-store/manage/orders.php?artist=${artist.encodeURLParameter()}",
            bearer = identity.second, authenticated = true)
        checkStoreIdentity(identity)
        check(raw["orders"] is JsonArray) { "El servidor no ha retornat les comandes." }
        return raw.items("orders").mapNotNull { row ->
            val id = row.text("id").toLongOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
            IosStoreOrder(id, row.text("product_name"), row.text("quantity").toIntOrNull() ?: 0,
                row.text("total_cents").toLongOrNull() ?: 0L, row.text("status"),
                row.text("recipient_name"), row.text("shipping_address"),
                row.text("shipping_postal_code"), row.text("shipping_city"),
                row.text("shipping_country"), row.text("created_at"))
        }
    }
    internal suspend fun artistStoreShip(artist: String, id: Long): List<IosStoreOrder> = artistStoreMutex.withLock {
        require(id > 0)
        val identity = storeIdentity(); requireManagedArtist(identity, artist)
        val before = artistStoreOrders(artist)
        require(before.any { it.id == id && it.status.equals("paid",true) }) { "La comanda no està pendent d'enviament." }
        IosServerConnection.json("artist-store/manage/mark-shipped.php", buildJsonObject {
            put("artist", artist); put("order_id", id)
        }, identity.second, authenticated = true)
        checkStoreIdentity(identity)
        val after = artistStoreOrders(artist)
        check(IosArtistStorePolicy.confirmedShipped(id, before, after)) { "El servidor no ha confirmat l'enviament." }
        after
    }

    /** Artist bells are owned by the same MariaDB user, including a real server guest.
     * Queue and read-after-write confirmation, never a parallel iPhone preference.
     */
    suspend fun artistBells(): List<IosArtistBell> {
        val owner = activePersonalId() ?: ensureGuestIdentity()
        val remote = request("notifications/artist-bell.php", authenticated = true)
            .items("artists").mapNotNull { obj ->
                val name = obj.text("artist_name").trim()
                if (name.isBlank()) null else IosArtistBell(name,
                    IosArtistBellPolicy.parseFrequency(obj.text("frequency")), obj.bool("enabled"))
            }
        check(activePersonalId() == owner) { "La sessió ha canviat durant la consulta" }
        val projected = remote.associateByTo(linkedMapOf()) { IosArtistBellPolicy.key(it.artistName) }
        pendingQueue?.pendingArtistBellActions(owner).orEmpty().forEach { action ->
            val name = action.payload.text("artist_name")
            val enabled = action.payload["enabled"]?.jsonPrimitive?.booleanOrNull ?: error("Campaneta pendent invàlida")
            val bell = IosArtistBell(name, action.payload.text("frequency"), enabled)
            if (enabled) projected[IosArtistBellPolicy.key(name)] = bell else projected.remove(IosArtistBellPolicy.key(name))
        }
        check(activePersonalId() == owner) { "La sessió ha canviat durant la consulta" }
        return projected.values.toList()
    }
    suspend fun saveArtistBell(artist: String, enabled: Boolean, frequency: String): IosArtistBell {
        val name = artist.trim()
        require(name.isNotEmpty() && name.length <= 190) { "Nom d'artista no vàlid." }
        require(frequency in IosArtistBellPolicy.frequencies) { "Freqüència no vàlida." }
        val owner = activePersonalId() ?: ensureGuestIdentity()
        queuedWrite(owner, "post", buildJsonObject {
            put("artist_name", name); put("enabled", enabled); put("frequency", frequency)
        }, "notifications/artist-bell.php")
        check(activePersonalId() == owner) { "La sessió ha canviat durant la campaneta" }
        return IosArtistBell(name, frequency, enabled)
    }
    /** Pas 29: server-authoritative user settings and the same mail preferences endpoint as Android.
     * No editable personal settings are written to an iPhone preference store.
     */
    internal suspend fun notificationCenterSettings(expectedUserId: Int): IosNotificationCenterSettings {
        val bearer = personalBearer(expectedUserId)
        val central = IosCentralStateSnapshot.decode(
            IosServerConnection.json("sync/state.php", bearer = bearer, authenticated = true), expectedUserId)
        var result = IosNotificationCenterSettings().withCentral(central)
        if (account?.id == expectedUserId) {
            val email = IosServerConnection.json(
                "mail/preferences-api.php", bearer = bearer, authenticated = true)
            require(email["preferences"] is JsonObject) {
                "El servidor no ha retornat les preferències de correu; no es guardaran valors per defecte."
            }
            result = result.withEmail(email)
        }
        check(activePersonalId() == expectedUserId) { "La sessió ha canviat en llegir notificacions" }
        return result
    }

    internal suspend fun hasPendingNotificationSettingsWrites(expectedUserId: Int): Boolean =
        pendingQueue?.pendingStateActions(expectedUserId)?.any { action ->
            (action.payload["mutations"] as? JsonArray).orEmpty().any { mutation ->
                val key = (mutation as? JsonObject)?.get("key")?.jsonPrimitive?.contentOrNull.orEmpty()
                key.startsWith("alert_") || key.startsWith("ios_push_")
            }
        } == true

    internal suspend fun saveNotificationCenterSettings(expectedUserId: Int,
                                                       draft: IosNotificationCenterSettings): Boolean =
        personalWriteMutex.withLock {
            val registered = account?.id == expectedUserId
            draft.validate(registered)
            val bearer = personalBearer(expectedUserId)
            check(activePersonalId() == expectedUserId) { "La sessió ha canviat" }
            val queue = pendingQueue ?: error("No hi ha cua xifrada disponible")
            val mutations = buildJsonArray {
                draft.centralMutations().forEach { (key, value) ->
                    add(buildJsonObject {
                        put("id", IosPersonalMutationId.make())
                        put("key", key)
                        put("op", "put")
                        put("value", value)
                    })
                }
            }
            // Both actions are durably encrypted before ANY network request. The mail
            // endpoint is an idempotent update, so automatic offline replay is safe.
            queue.append(IosQueuedAction(IosPersonalMutationId.make(), expectedUserId,
                "state", buildJsonObject { put("mutations", mutations) }))
            if (registered) {
                queue.append(IosQueuedAction(IosPersonalMutationId.make(), expectedUserId,
                    "post", draft.emailPayload(), "mail/preferences-api.php"))
            }
            queue.flush(expectedUserId, bearer)
            check(activePersonalId() == expectedUserId) { "La sessió ha canviat" }
            queue.countFor(expectedUserId) > 0
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
    /** Entrades reals de MariaDB; el cache xifrat és una còpia només per a mostrar QR offline. */
    suspend fun tickets(): List<IosTicket> {
        val owner = activePersonalId() ?: ensureGuestIdentity()
        val bearer = personalBearer(owner)
        val result = IosServerConnection.json("tickets/mine.php", bearer = bearer, authenticated = true)
        check(activePersonalId() == owner && personalBearer(owner) == bearer) {
            "La sessió ha canviat durant la consulta."
        }
        val serverTickets = result.items("tickets").map { ticket ->
            IosTicket(
                id = ticket.text("id"), eventId = ticket.text("event_id"),
                eventTitle = ticket.text("event_title", "title"),
                status = ticket.text("status"), code = ticket.text("code"),
                ticketOptionName = ticket.text("ticket_option_name"),
                sessionDate = ticket.text("session_date"),
                sessionTime = ticket.text("session_time"), sector = ticket.text("sector"),
                offer = ticket.text("offer"), validatedAt = ticket.text("validated_at")
            )
        }
        // Rebutjar dades invàlides: mai guardar codis incomplets o duplicats al dispositiu.
        IosOfflineTicketPolicy.requireValid(serverTickets)
        check(activePersonalId() == owner && personalBearer(owner) == bearer) {
            "La sessió ha canviat."
        }
        storeIssuedTicketsOffline(owner, serverTickets, replace = true)
        return serverTickets
    }

    /** Called only after a server-confirmed issue; never from PaymentSheet UI success alone.
     * A newly issued ticket is immediately available if network drops before tickets/mine.php.
     */
    private fun storeIssuedTicketsOffline(owner: Int, issued: List<IosTicket>, replace: Boolean) {
        if (owner <= 0 || activePersonalId() != owner) return
        if (runCatching { IosOfflineTicketPolicy.requireValid(issued) }.isFailure) return
        val prior = if (replace) emptyList() else offlineTickets().orEmpty()
        val ids = issued.map { it.id }.toSet()
        val codes = issued.map { it.code }.toSet()
        val combined = prior.filterNot { it.id in ids || it.code in codes } + issued
        if (runCatching { IosOfflineTicketPolicy.requireValid(combined) }.isFailure) return
        val snapshot = buildJsonObject {
            put("version", 1); put("owner_id", owner)
            put("tickets", buildJsonArray { combined.forEach { t -> add(buildJsonObject {
                put("id", t.id); put("event_id", t.eventId); put("event_title", t.eventTitle)
                put("status", t.status); put("code", t.code)
                put("ticket_option_name", t.ticketOptionName)
                put("session_date", t.sessionDate); put("session_time", t.sessionTime)
                put("sector", t.sector); put("offer", t.offer); put("validated_at", t.validatedAt)
            }) } })
        }.toString()
        sessionBridge?.saveOfflineTickets(owner, snapshot)
    }

    /** Offline snapshot is bound by native Keychain to this exact bearer and server owner.
     * It is NOT a second source of truth: statuses may be stale and validation remains online.
     * Also works after app relaunch when the server cannot restore the full profile.
     */
    fun offlineTickets(): List<IosTicket>? {
        if (token.isNullOrBlank() && guestSession.current() == null) return null
        val stored = sessionBridge?.loadOfflineTicketsForCurrentSession() ?: return null
        return runCatching {
            val root = Json.parseToJsonElement(stored) as? JsonObject ?: return null
            if (root["version"]?.jsonPrimitive?.intOrNull != 1) return null
            val owner = root["owner_id"]?.jsonPrimitive?.intOrNull ?: return null
            if (owner <= 0 || (activePersonalId() != null && activePersonalId() != owner)) return null
            val items = root["tickets"] as? JsonArray ?: return null
            val loaded = items.map { element ->
                val t = element as? JsonObject ?: error("Entrada en cache invàlida")
                IosTicket(t.text("id"), t.text("event_id"), t.text("event_title"),
                    t.text("status"), t.text("code"), t.text("ticket_option_name"),
                    t.text("session_date"), t.text("session_time"), t.text("sector"),
                    t.text("offer"), t.text("validated_at"))
            }
            IosOfflineTicketPolicy.requireValid(loaded)
            loaded
        }.getOrNull()
    }

    /** Exposa totes les combinacions de data, zona i contingent, sense duplicar regles d'Android. */
    suspend fun ticketCatalog(eventId: Int): IosTicketCatalog {
        require(eventId > 0) { "Esdeveniment invàlid." }
        val root = request("tickets/options.php?event_id=$eventId")
        val options = root.items("options").mapNotNull { row ->
            val id = row.text("id").toIntOrNull() ?: return@mapNotNull null
            // No convertir preus inexistents o invàlids en entrades gratuïtes.
            val price = row.text("price").toDoubleOrNull() ?: return@mapNotNull null
            val option = IosTicketOption(
                id = id,
                name = row.text("name").ifBlank { "Entrada" }, price = price,
                remaining = row.text("remaining").toIntOrNull(),
                maxPerOrder = (row.text("max_per_order").toIntOrNull() ?: 6).coerceIn(1, 20),
                sessionDate = row.text("session_date"), sessionTime = row.text("session_time"),
                sector = row.text("sector"), offer = row.text("offer"),
                capacity = row.text("capacity").toIntOrNull()?.takeIf { it >= 0 },
                sold = (row.text("sold").toIntOrNull() ?: 0).coerceAtLeast(0),
                reserved = (row.text("reserved").toIntOrNull() ?: 0).coerceAtLeast(0),
                saleStartsAt = row.text("sale_starts_at"), saleEndsAt = row.text("sale_ends_at"),
                active = row.text("active").lowercase().let { it.isBlank() || it == "1" || it == "true" }
            )
            option.takeIf(IosTicketCatalogPolicy::valid)
        }
        return IosTicketCatalog(options, root.text("capacity").toIntOrNull()?.takeIf { it >= 0 })
    }

    suspend fun ticketOptions(eventId: Int): List<IosTicketOption> = ticketCatalog(eventId).options
    /** The existing backend issues tickets only when the selected option is free.
     * Never create a paid order here without an integrated, confirmed payment. */
    suspend fun reserveFreeTicket(eventId: Int, optionId: Int, quantity: Int): List<IosTicket> {
        require(eventId > 0 && optionId > 0) { "Cal triar una modalitat d'entrada vàlida." }
        val owner = activePersonalId() ?: ensureGuestIdentity()
        val bearer = personalBearer(owner)
        val option = ticketOptions(eventId).firstOrNull { it.id == optionId }
        require(IosTicketCatalogPolicy.selectable(option) && option!!.price == 0.0 &&
            quantity in 1..IosTicketCatalogPolicy.quantityLimit(option)) {
            "La modalitat gratuïta no està disponible per a la quantitat sol·licitada."
        }
        check(activePersonalId() == owner && personalBearer(owner) == bearer) {
            "La sessió ha canviat. Torna a consultar les entrades."
        }
        // POST no idempotent: no reintentar després d'una resposta de xarxa incerta.
        val root = IosServerConnection.json("tickets/purchase.php", buildJsonObject {
            put("event_id", eventId); put("ticket_option_id", optionId); put("quantity", quantity)
        }, bearer, authenticated = true)
        val issued = root.items("tickets").map { t ->
            IosTicket(t.text("id"), t.text("event_id"), "", t.text("status"), t.text("code"))
        }
        check(activePersonalId() == owner && personalBearer(owner) == bearer) { "La sessió ha canviat durant la reserva." }
        check(issued.size == quantity && issued.all { it.id.isNotBlank() && it.code.isNotBlank() &&
            it.eventId == eventId.toString() }) {
            "El servidor no ha confirmat totes les entrades. Consulta 'Les meues entrades' abans de tornar a reservar."
        }
        storeIssuedTicketsOffline(owner, issued, replace = false)
        return issued
    }
    /** Pas 38: Create the PaymentIntent ONLY at the original Android v336 PHP endpoint.
     * All payment POSTs are non-idempotent: never queue or automatically repeat them.
     */
    suspend fun createTicketPayment(eventId: Int, optionId: Int, quantity: Int): IosTicketPaymentStart {
        val owner = activePersonalId() ?: ensureGuestIdentity()
        val bearer = personalBearer(owner)
        val catalog = ticketCatalog(eventId)
        val chosen = catalog.options.firstOrNull { it.id == optionId }
        IosPaidTicketPolicy.requirePurchasable(eventId, chosen, quantity)
        ensurePaymentSession(owner, bearer)
        val root = IosServerConnection.json("payments/create.php", buildJsonObject {
            put("event_id",eventId); put("ticket_option_id",optionId); put("quantity",quantity)
        }, bearer, authenticated=true)
        ensurePaymentSession(owner, bearer)
        val start = IosTicketPaymentStart(
            root.text("order_id").toLongOrNull() ?: 0L,
            root.text("client_secret"), root.text("publishable_key"),
            root.text("amount_cents").toLongOrNull() ?: 0L,
            root.text("currency").ifBlank { "eur" },
            root.text("merchant_name").ifBlank { "Agenda Valenciana" },
            root.text("mode"), owner, eventId, optionId, quantity)
        IosPaidTicketPolicy.requireValidStart(start)
        return start
    }

    private suspend fun ensurePaymentSession(expectedUserId: Int, expectedBearer: String) {
        check(activePersonalId() == expectedUserId && personalBearer(expectedUserId) == expectedBearer) {
            "El compte ha canviat durant la compra. Consulta les teues entrades."
        }
    }

    /** A native SDK result alone never creates tickets or marks an order as paid. */
    suspend fun ticketPaymentStatus(start: IosTicketPaymentStart): IosTicketPaymentConfirmation {
        IosPaidTicketPolicy.requireValidStart(start)
        val bearer = personalBearer(start.userId)
        ensurePaymentSession(start.userId, bearer)
        val root = IosServerConnection.json("payments/status.php?order_id=${start.orderId}",
            bearer=bearer,authenticated=true)
        ensurePaymentSession(start.userId, bearer)
        val returnedOrder = root.text("order_id").toLongOrNull() ?: 0L
        check(returnedOrder == start.orderId) { "La comanda retornada no correspon al pagament iniciat." }
        val status = root.text("status").lowercase()
        val issued = root.items("tickets").map { item ->
            IosTicket(item.text("id"),item.text("event_id"),
                item.text("event_title","title"),item.text("status"),item.text("code"))
        }
        val answer = IosTicketPaymentConfirmation(returnedOrder,status,issued)
        IosPaidTicketPolicy.requireConsistentResult(start,answer)
        if (IosPaidTicketPolicy.isPaid(answer.status)) {
            storeIssuedTicketsOffline(start.userId, issued, replace = false)
        }
        return answer
    }

    /** Same transactional receipt endpoint Android calls, only AFTER status paid + QR present.
     * A failed email does not revoke a paid ticket, and is never retried automatically here.
     */
    suspend fun requestTicketReceipt(start: IosTicketPaymentStart) {
        val bearer = personalBearer(start.userId)
        ensurePaymentSession(start.userId, bearer)
        IosServerConnection.json("mail/ticket-receipt-now.php", buildJsonObject {
            put("order_id",start.orderId)
        }, bearer, authenticated=true)
        ensurePaymentSession(start.userId,bearer)
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
    /** Keep both PHP representations consistent with Android v336. The secondary
     * board-settings POST is essential: without it HOSTING and legacy subtypes become OTHER.
     * Do NOT automatically retry creation after a network error (not idempotent).
     */
    suspend fun createCommunityGroup(eventId: Int, title: String, type: String, open: Boolean,
                                     description: String = "", origin: String = "", meetingAt: String = "",
                                     capacity: Int = 0, chatEnabled: Boolean = true): IosCommunityGroup {
        require(IosCommunityBoardContract.validCreate(eventId, title.trim(), type, capacity, meetingAt.trim())) {
            "Comprova el nom, la data i les places del tauló."
        }
        val owner = activePersonalId() ?: ensureGuestIdentity()
        val bearer = personalBearer(owner)
        // Fail before creation if the two-phase board-settings API is unavailable.
        IosServerConnection.json("community/board-settings.php?group_ids=1", bearer=bearer, authenticated=true)
        checkCommunityOwner(owner,bearer)
        val root = IosServerConnection.json("community/create.php", buildJsonObject {
            put("event_id",eventId);put("title",title.trim());put("type",IosCommunityBoardContract.serverType(type))
            put("access_mode", if(open) "OPEN" else "APPROVAL")
            put("description",description.trim().take(1000));put("origin_label",origin.trim().take(190))
            if(meetingAt.isNotBlank()) put("meeting_at",meetingAt.trim())
            if(capacity>0) put("capacity",capacity)
        }, bearer, authenticated=true)
        val raw=root["group"] as? JsonObject ?: error("El servidor no ha retornat el tauló.")
        val created=decodeCommunityGroup(raw)
        require(created.id>0 && created.eventId==eventId && created.creatorUserId==owner) {
            "Identitat o esdeveniment del tauló incorrecte."
        }
        checkCommunityOwner(owner,bearer)
        try {
            saveBoardSettings(created.id,type,chatEnabled,owner,bearer)
        } catch (e: Exception) {
            // The first POST may have committed; never repeat it and create a duplicate.
            error("Tauló #${created.id} creat, però no s'ha pogut confirmar la classificació. No el tornes a crear: revisa els ajustos. ${e.message.orEmpty()}")
        }
        checkCommunityOwner(owner,bearer)
        return created.copy(type=type,chatEnabled=chatEnabled)
    }

    private fun checkCommunityOwner(owner: Int, bearer: String) {
        check(activePersonalId()==owner &&
            (if(!token.isNullOrBlank()) token==bearer else guestSession.current()?.token==bearer)) {
            "La sessió ha canviat durant l'operació de Comunitat."
        }
    }

    private suspend fun saveBoardSettings(groupId:Int, type:String, chatEnabled:Boolean,
                                          owner:Int, bearer:String) {
        val saved=IosServerConnection.json("community/board-settings.php",buildJsonObject {
            put("group_id",groupId); put("board_type",type); put("chat_enabled",chatEnabled)
        },bearer,authenticated=true)
        val response=saved["board"] as? JsonObject ?: error("No s'ha confirmat el tipus de tauló")
        require(response.text("group_id").toIntOrNull()==groupId && response.text("board_type")==type &&
            response["chat_enabled"]?.jsonPrimitive?.booleanOrNull==chatEnabled) { "Configuració del tauló diferent de la sol·licitada" }
        checkCommunityOwner(owner,bearer)
        val verify=IosServerConnection.json("community/board-settings.php?group_ids=$groupId",bearer=bearer,authenticated=true)
        require(verify.items("boards").any { it.text("group_id").toIntOrNull()==groupId &&
            it.text("board_type")==type && it["chat_enabled"]?.jsonPrimitive?.booleanOrNull==chatEnabled }) {
            "El servidor no ha conservat el tipus de tauló."
        }
    }

    suspend fun updateCommunityGroup(g: IosCommunityGroup, title: String, description: String,
                                     origin: String, meeting: String, capacity: Int,
                                     type: String, access: String, chatEnabled:Boolean=g.chatEnabled): IosCommunityGroup {
        require(IosCommunityBoardContract.validCreate(g.eventId,title.trim(),type,capacity,meeting.trim()))
        val owner=activePersonalId() ?: error("No hi ha sessió activa")
        require(g.creatorUserId==owner) { "Només el creador pot modificar el tauló." }
        val bearer=personalBearer(owner)
        IosServerConnection.json("community/board-settings.php?group_ids=${g.id}",bearer=bearer,authenticated=true)
        checkCommunityOwner(owner,bearer)
        val root=IosServerConnection.json("community/update.php",buildJsonObject {
            put("group_id",g.id);put("title",title.trim());put("description",description.trim().take(1000))
            put("origin_label",origin.trim().take(190));put("meeting_at",meeting.trim())
            if(capacity>0) put("capacity",capacity) else put("capacity",JsonNull)
            put("type",IosCommunityBoardContract.serverType(type));put("access_mode",access)
        },bearer,authenticated=true)
        val changed=decodeCommunityGroup(root["group"] as? JsonObject ?: error("Falten dades del tauló"))
        require(changed.id==g.id && changed.creatorUserId==owner) { "Resposta d'edició incorrecta" }
        checkCommunityOwner(owner,bearer)
        try { saveBoardSettings(g.id,type,chatEnabled,owner,bearer) }
        catch(e:Exception) { error("Tauló #${g.id} editat parcialment: revisa els ajustos. ${e.message.orEmpty()}") }
        return changed.copy(type=type,chatEnabled=chatEnabled)
    }
    suspend fun setCommunityStatus(groupId:Int, status:String) {
        require(status in setOf("OPEN","CLOSED","CANCELLED"))
        request("community/status.php",buildJsonObject{put("group_id",groupId);put("status",status)},authenticated=true)
    }
    /** Reports are non-idempotent POSTs: never enqueue/retry after an ambiguous response. */
    suspend fun reportCommunityMessage(groupId: Int, messageId: Int, reason: String) {
        require(IosCommunityModerationPolicy.validReport(groupId, messageId, reason))
        val (owner,bearer)=communityIdentity()
        val reply=IosServerConnection.json("community/report.php",buildJsonObject {
            put("group_id",groupId);put("message_id",messageId);put("reason",reason.trim())
        },bearer,authenticated=true)
        checkCommunityOwner(owner,bearer)
        check(reply.bool("ok")) { "El servidor no ha confirmat la denúncia." }
    }
    suspend fun reportCommunityGroup(groupId:Int,reason:String) {
        require(IosCommunityModerationPolicy.validReport(groupId,0,reason))
        val (owner,bearer)=communityIdentity()
        val reply=IosServerConnection.json("community/report.php",buildJsonObject {
            put("group_id",groupId);put("reason",reason.trim())
        },bearer,authenticated=true)
        checkCommunityOwner(owner,bearer)
        check(reply.bool("ok")) { "El servidor no ha confirmat la denúncia del tauló." }
    }
    suspend fun community(eventId: Int? = null, mine: Boolean = false): List<IosCommunityGroup> {
        val owner=activePersonalId() ?: ensureGuestIdentity()
        val bearer=personalBearer(owner)
        val args=listOfNotNull(eventId?.takeIf { it>0 }?.let { "event_id=$it" }, if(mine) "mine=1" else null)
        val path="community/groups.php"+if(args.isEmpty()) "" else "?"+args.joinToString("&")
        val primary=IosServerConnection.json(path,bearer=bearer,authenticated=true).items("groups").map(::decodeCommunityGroup)
        // Android v336 also lists boards contacted privately (without chat membership).
        val contacted=if(mine) runCatching {
            IosServerConnection.json("community/board-settings.php?contacted=1",bearer=bearer,authenticated=true)
                .items("groups").map(::decodeCommunityGroup)
        }.getOrDefault(emptyList()) else emptyList()
        val all=(primary+contacted).distinctBy { it.id }.filter { eventId==null || it.eventId==eventId }
        // One GET per <=100 IDs, never a network call per group.
        val settings=mutableMapOf<Int,Pair<String,Boolean>>()
        all.map {it.id}.filter {it>0}.chunked(100).forEach { ids ->
            val extra=runCatching { IosServerConnection.json(
                "community/board-settings.php?group_ids=${ids.joinToString(",")}",bearer=bearer,authenticated=true)
            }.getOrNull()
            extra?.items("boards")?.forEach { board ->
                val groupId=board.text("group_id").toIntOrNull() ?: return@forEach
                settings[groupId]=IosCommunityBoardContract.canonicalType(board.text("board_type")) to
                    (board["chat_enabled"]?.jsonPrimitive?.booleanOrNull ?: true)
            }
        }
        checkCommunityOwner(owner,bearer)
        return all.map {g -> settings[g.id]?.let {(type,chat)->g.copy(type=type,chatEnabled=chat)} ?:g }
    }
    /** All Community calls freeze the server identity before IO, including guests.
     * Non-idempotent POSTs are never queued/replayed automatically.
     */
    private suspend fun communityIdentity(): Pair<Int,String> {
        val owner=activePersonalId() ?: ensureGuestIdentity()
        return owner to personalBearer(owner)
    }
    private suspend fun communityGroupById(groupId:Int,eventId:Int,owner:Int,bearer:String): IosCommunityGroup {
        val group=IosServerConnection.json("community/groups.php?event_id=$eventId",bearer=bearer,authenticated=true)
            .items("groups").firstOrNull { it.text("id").toIntOrNull()==groupId }
            ?: error("El servidor no ha retornat el tauló #$groupId")
        checkCommunityOwner(owner,bearer)
        return decodeCommunityGroup(group)
    }
    suspend fun communityMemberState(groupId:Int,eventId:Int):IosCommunityGroup {
        require(groupId>0 && eventId>0)
        val (owner,bearer)=communityIdentity()
        return communityGroupById(groupId,eventId,owner,bearer)
    }
    suspend fun pendingRequests(groupId: Int): List<IosMemberRequest> {
        require(groupId>0)
        val (owner,bearer)=communityIdentity()
        val result=IosServerConnection.json("community/requests.php?group_id=$groupId",bearer=bearer,authenticated=true)
            .items("requests").mapNotNull { obj ->
                val id=obj.text("user_id").toIntOrNull() ?: 0
                if(id<=0) null else IosMemberRequest(id,obj.text("display_name"),obj.text("requested_at"))
            }
        checkCommunityOwner(owner,bearer)
        return result
    }
    suspend fun respondToMember(groupId: Int, userId: Int, accept: Boolean) {
        require(groupId>0 && userId>0)
        val (owner,bearer)=communityIdentity()
        // GET requires creator permissions and verifies the target really is pending.
        require(pendingRequests(groupId).any { it.userId==userId }) { "La sol·licitud ja no està pendent." }
        checkCommunityOwner(owner,bearer)
        val root=IosServerConnection.json("community/respond.php",buildJsonObject {
            put("group_id",groupId);put("user_id",userId)
            put("action",if(accept)"ACCEPT" else "REJECT")
        },bearer,authenticated=true)
        val confirmed=root["group"] as? JsonObject ?: error("Resposta sense tauló del servidor")
        require(confirmed.text("id").toIntOrNull()==groupId &&
            confirmed.text("creator_user_id").toIntOrNull()==owner) { "Resposta d'aprovació incorrecta" }
        checkCommunityOwner(owner,bearer)
        // A failure here does NOT replay the POST, which already may have committed.
        require(pendingRequests(groupId).none { it.userId==userId }) { "La sol·licitud continua pendent." }
    }
    suspend fun joinGroup(id: Int, eventId: Int, access: String): String {
        require(id>0 && eventId>0)
        val (owner,bearer)=communityIdentity()
        val root=IosServerConnection.json("community/join.php",buildJsonObject {put("group_id",id)},bearer,authenticated=true)
        val posted=root.text("membership_status").uppercase()
        val group=root["group"] as? JsonObject ?: error("El servidor no ha confirmat el grup")
        // The access mode may have changed since the UI loaded; trust the server's
        // membership result, not a cached OPEN/APPROVAL value.
        val status=group.text("my_status", "membership_status").uppercase()
        require(IosCommunityPolicy.verifiedJoin(posted,id,group.text("id").toIntOrNull()?:0,status,status)) {
            "La resposta del grup no concorda amb la sol·licitud."
        }
        checkCommunityOwner(owner,bearer)
        val remote=communityGroupById(id,eventId,owner,bearer)
        require(remote.membership.uppercase()==posted) { "L'estat d'accés no s'ha confirmat en el servidor." }
        return remote.membership.uppercase()
    }
    suspend fun leaveGroup(id: Int, eventId:Int) {
        require(id>0 && eventId>0)
        val (owner,bearer)=communityIdentity()
        val root=IosServerConnection.json("community/leave.php",buildJsonObject{put("group_id",id)},bearer,authenticated=true)
        require((root["group"] as? JsonObject)?.text("id")?.toIntOrNull()==id) { "Resposta d'eixida incorrecta." }
        checkCommunityOwner(owner,bearer)
        require(communityGroupById(id,eventId,owner,bearer).membership=="LEFT") { "No s'ha confirmat l'eixida del grup." }
    }
    suspend fun messages(id: Int): List<IosMessage> {
        require(id>0)
        val (owner,bearer)=communityIdentity()
        val result=IosServerConnection.json("community/messages.php?group_id=$id",bearer=bearer,authenticated=true)
            .items("messages").mapNotNull { obj ->
                val msgId=obj.text("id").toIntOrNull() ?: 0
                if(msgId<=0 || obj.text("group_id").toIntOrNull()!=id) null
                else IosMessage(msgId.toString(),obj.text("author"),obj.text("body","text"),
                    obj.text("created_at"),obj.text("user_id").toIntOrNull()?:0)
            }
        checkCommunityOwner(owner,bearer)
        return result
    }
    suspend fun sendMessage(id: Int, body: String): IosMessage {
        require(id>0 && IosCommunityPolicy.validMessage(body)) { "Escriu un missatge de màxim 2000 caràcters." }
        val (owner,bearer)=communityIdentity()
        val root=IosServerConnection.json("community/send.php",buildJsonObject {
            put("group_id",id);put("text",body.trim())
        },bearer,authenticated=true)
        val msg=root["message"] as? JsonObject ?: error("El servidor no ha confirmat el missatge")
        require(IosCommunityPolicy.verifiedSentMessage(id,owner,body,msg.text("id").toIntOrNull()?:0,
            msg.text("group_id").toIntOrNull()?:0,msg.text("user_id").toIntOrNull()?:0,
            msg.text("body","text"))) { "La resposta no correspon al missatge enviat." }
        checkCommunityOwner(owner,bearer)
        return IosMessage(msg.text("id"),msg.text("author"),msg.text("body","text"),
            msg.text("created_at"),owner)
    }
    /** Private inquiries share the same user_id as the group, not a local mobile identity.
     * Endpoint is shipped in SERVIDOR_COMUNITAT and needs deployment verification.
     */
    suspend fun communityInquiries(groupId:Int):List<IosCommunityInquiry> {
        require(groupId>0)
        val (owner,bearer)=communityIdentity()
        val result=IosServerConnection.json("community/inquiries.php?group_id=$groupId",bearer=bearer,authenticated=true)
            .items("messages").mapNotNull { v ->
                val id=v.text("id").toIntOrNull()?:0
                if(id<=0 || v.text("group_id").toIntOrNull()!=groupId) null else
                    IosCommunityInquiry(id,groupId,v.text("peer_user_id").toIntOrNull()?:0,
                        v.text("peer_name"),v.text("author"),v.text("body"),v.text("created_at"),
                        v["own"]?.jsonPrimitive?.booleanOrNull?:false)
            }
        checkCommunityOwner(owner,bearer)
        return result
    }
    suspend fun communityInbox():List<IosCommunityInquiryInbox> {
        val (owner,bearer)=communityIdentity()
        val messages=IosServerConnection.json("community/inquiries.php?inbox=1",bearer=bearer,authenticated=true)
            .items("messages").mapNotNull { v ->
                val id=v.text("id").toIntOrNull()?:0; val gid=v.text("group_id").toIntOrNull()?:0
                if(id<=0 || gid<=0) null else IosCommunityInquiryInbox(id,gid,v.text("event_id").toIntOrNull()?:0,
                    v.text("board_title"),v.text("author"),v.text("author_user_id").toIntOrNull()?:0,
                    v.text("body"),v.text("created_at"))
            }
        checkCommunityOwner(owner,bearer)
        return messages
    }
    suspend fun sendCommunityInquiry(groupId:Int,body:String,peerUserId:Int?=null):Int {
        require(groupId>0 && IosCommunityPolicy.validMessage(body))
        val (owner,bearer)=communityIdentity()
        require(peerUserId==null || (peerUserId>0 && peerUserId!=owner))
        // Not idempotent; on an uncertain error, do not retry automatically.
        val root=IosServerConnection.json("community/inquiries.php",buildJsonObject {
            put("group_id",groupId);put("body",body.trim())
            if(peerUserId!=null)put("peer_user_id",peerUserId)
        },bearer,authenticated=true)
        val messageId=root.text("message_id").toIntOrNull()?:0
        require(messageId>0) { "El servidor no ha confirmat la consulta privada." }
        checkCommunityOwner(owner,bearer)
        return messageId
    }
    /** Blocking and moderation need the separate SERVIDOR_COMUNITAT SQL+PHP deployment.
     * Never cache or replay potentially destructive operations in the offline queue.
     */
    private suspend fun communityBlockedSnapshot(owner:Int,bearer:String):List<IosCommunityUser> {
        val root=IosServerConnection.json("community/blocks.php",bearer=bearer,authenticated=true)
        checkCommunityOwner(owner,bearer)
        return root.items("blocked").mapNotNull { row ->
            val id=row.text("user_id").toIntOrNull()?:0
            if(id>0) IosCommunityUser(id,row.text("display_name")) else null
        }
    }
    suspend fun blockedCommunityUsers(): List<IosCommunityUser> {
        val (owner,bearer)=communityIdentity()
        return communityBlockedSnapshot(owner,bearer)
    }
    suspend fun setCommunityBlocked(userId:Int,blocked:Boolean) {
        val (owner,bearer)=communityIdentity()
        require(IosCommunityModerationPolicy.validBlock(owner,userId)) { "No pots bloquejar-te a tu mateix." }
        val reply=IosServerConnection.json("community/blocks.php",buildJsonObject {
            put("user_id",userId);put("action",if(blocked) "BLOCK" else "UNBLOCK")
        },bearer,authenticated=true)
        checkCommunityOwner(owner,bearer)
        require(reply.bool("ok") && reply["blocked"]?.jsonPrimitive?.booleanOrNull==blocked) {
            "La resposta de bloqueig no coincidix amb l'acció sol·licitada."
        }
        val actual=communityBlockedSnapshot(owner,bearer).any { it.id==userId }
        check(IosCommunityModerationPolicy.verifiedBlock(blocked,actual)) {
            "El servidor no ha confirmat el bloqueig en la llista canònica."
        }
    }
    private suspend fun communityManagementSnapshot(groupId:Int,owner:Int,bearer:String):IosCommunityManagement {
        val root=IosServerConnection.json("community/manage.php?group_id=$groupId",bearer=bearer,authenticated=true)
        checkCommunityOwner(owner,bearer)
        return IosCommunityManagement(root.items("members").mapNotNull { row ->
            val id=row.text("user_id").toIntOrNull()?:0
            if(id<=0) null else IosCommunityMember(id,row.text("display_name"),row.text("role"),row.text("status"))
        },root.items("bans").mapNotNull { row ->
            val id=row.text("user_id").toIntOrNull()?:0
            if(id<=0) null else IosCommunityUser(id,row.text("display_name"))
        })
    }
    suspend fun communityManageMembers(groupId:Int): IosCommunityManagement {
        require(groupId>0)
        val (owner,bearer)=communityIdentity()
        return communityManagementSnapshot(groupId,owner,bearer)
    }
    suspend fun moderateCommunityGroup(groupId:Int,action:String,userId:Int=0,messageId:Int=0) {
        val (owner,bearer)=communityIdentity()
        require(IosCommunityModerationPolicy.validManagement(groupId,action,userId,messageId,owner)) {
            "Paràmetres o permisos locals invàlids."
        }
        // GET is creator-only. Even when the UI was stale, avoid a destructive POST.
        val before=communityManagementSnapshot(groupId,owner,bearer)
        require(IosCommunityModerationPolicy.eligibleManagement(before,action,userId)) {
            "La persona no és membre del tauló o no es pot aplicar esta acció."
        }
        val reply=IosServerConnection.json("community/manage.php",buildJsonObject {
            put("group_id",groupId);put("action",action)
            if(userId>0) put("user_id",userId)
            if(messageId>0) put("message_id",messageId)
        },bearer,authenticated=true)
        checkCommunityOwner(owner,bearer)
        check(reply.bool("ok") && reply.text("action")==action) { "El servidor no confirma l'acció de moderació." }
        if(action=="BAN_MEMBER" || action=="UNBAN_MEMBER") {
            val after=communityManagementSnapshot(groupId,owner,bearer)
            check(IosCommunityModerationPolicy.verifiedBan(after.bans.map { it.id }.toSet(),userId,action)) {
                "El canvi de membres no apareix al servidor."
            }
        } else {
            // DELETE_MESSAGE returns an authenticated {ok:true,action}; the PHP does not
            // expose a deletion receipt ID or complete chat history for independent proof.
            // Never resend this non-idempotent mutation after an uncertain response.
        }
    }
    private suspend fun communityReportsSnapshot(owner:Int,bearer:String,all:Boolean):List<IosCommunityReport> {
        val root=IosServerConnection.json("community/admin-reports.php?status=${if(all) "ALL" else "OPEN"}",
            bearer=bearer,authenticated=true)
        checkCommunityOwner(owner,bearer)
        return root.items("reports").mapNotNull { row ->
            val id=row.text("id").toIntOrNull()?:0
            if(id<=0) null else IosCommunityReport(id,row.text("group_id").toIntOrNull()?:0,
                row.text("message_id").toIntOrNull()?:0,row.text("reported_user_id").toIntOrNull()?:0,
                row.text("group_title"),row.text("reported_name"),row.text("reason"),
                row.text("message_body"),row.text("status"))
        }
    }
    suspend fun communityReports(all:Boolean=false):List<IosCommunityReport> {
        require(account?.role?.uppercase()=="SUPER_ADMIN") { "Només superadministrador." }
        val (owner,bearer)=communityIdentity()
        return communityReportsSnapshot(owner,bearer,all)
    }
    suspend fun resolveCommunityReport(reportId:Int,action:String) {
        require(account?.role?.uppercase()=="SUPER_ADMIN") { "Només superadministrador." }
        val (owner,bearer)=communityIdentity()
        require(reportId>0 && IosCommunityModerationPolicy.validAdminAction(action))
        val before=communityReportsSnapshot(owner,bearer,false)
        val original=before.firstOrNull { it.id==reportId }
            ?: error("La denúncia ja no està pendent de revisió.")
        require(IosCommunityModerationPolicy.eligibleAdmin(original,action)) {
            "Esta denúncia no té les dades necessàries per a l'acció."
        }
        val reply=IosServerConnection.json("community/admin-reports.php",buildJsonObject {
            put("report_id",reportId);put("action",action)
        },bearer,authenticated=true)
        checkCommunityOwner(owner,bearer)
        val expected=IosCommunityModerationPolicy.adminStatus(action)
        check(reply.bool("ok") && reply.text("status")==expected) {
            "El servidor no confirma la resolució de la denúncia."
        }
        val after=communityReportsSnapshot(owner,bearer,true)
        check(IosCommunityModerationPolicy.verifiedReportStatus(after,reportId,expected)) {
            "L'estat de la denúncia encara no està confirmat en la bústia."
        }
    }
    /** Same data endpoints as Android; DO NOT use manual post author as the artist.
     * Android generates additional items from recent-events.php and followed artists.
     * Avoid scanning at all when fewer than seven artists are followed.
     */
    suspend fun batecs(
        followedArtists: Set<String>, catalog: List<AgendaEventSummary>, blockedArtists: Set<String> = emptySet()
    ): List<IosBatec> {
        val eligible = followedArtists.map(IosBatecFeedContract::norm).filter(String::isNotBlank)
            .toSet() - blockedArtists.map(IosBatecFeedContract::norm).toSet()
        if (eligible.size <= 6) return emptyList()
        val owner = activePersonalId() ?: return emptyList()
        val manual = linkedMapOf<String, IosBatec>()
        var offset = 0
        for (pageIndex in 0 until 6) { // PHP returns at most 300; do not stop at one artist.
            val page = request("batecs/index.php?offset=$offset&limit=50", authenticated = true)
            if (activePersonalId() != owner) return emptyList()
            val items = page.items("batecs")
            items.forEach { item ->
                val id = item.text("id").toIntOrNull()?.takeIf { it > 0 } ?: return@forEach
                val eventId = item.text("event_id").toIntOrNull() ?: return@forEach
                val key = item.text("story_key").ifBlank { "manual:$id" }
                manual.putIfAbsent(key, IosBatec(
                    id.toString(), eventId, item.text("event_artist", "author_name", "author_username"),
                    item.text("event_title", "title"), item.text("body", "caption", "text"),
                    validMediaUrl(item.text("image_url", "media_url")), key, "MANUAL",
                    item.text("published_at"), item.text("event_date")
                ))
            }
            val nextOffset = page.text("next_offset").toIntOrNull()?.takeIf { it > offset }
                ?: offset + items.size
            if (!page.bool("has_more") || items.isEmpty()) break
            offset = nextOffset
        }
        val recent = runCatching {
            request("batecs/recent-events.php", authenticated = true).items("events")
                .mapNotNull { obj ->
                    obj.text("event_id").toIntOrNull()?.let { it to obj.text("created_at") }
                }.toMap()
        }.getOrDefault(emptyMap())
        if (activePersonalId() != owner) return emptyList()
        return IosBatecFeedContract.project(followedArtists, blockedArtists, catalog,
            manual.values.toList(), recent, currentLocalDateIso())
    }
    /** Read server-confirmed seen state plus ONLY the pending local encrypted overlay.
     * The overlay is ephemeral presentation, never a second permanent authority.
     */
    suspend fun seenBatecs(): Set<String> {
        val owner = activePersonalId() ?: ensureGuestIdentity()
        val pending = pendingQueue?.pendingBatecSeenActions(owner).orEmpty().mapNotNull { action ->
            runCatching { IosBatecOperations.canonicalKey(action.payload.text("story_key")) }.getOrNull()
        }
        val remote = try {
            IosBatecOperations.seenKeys(request("batecs/seen.php", authenticated = true))
        } catch (cancel: kotlinx.coroutines.CancellationException) {
            throw cancel
        } catch (error: Exception) {
            // When offline, only show what is verifiably present in the encrypted pending
            // queue. Never invent a server-confirmed seen list from local preferences.
            if (pending.isEmpty()) throw error
            emptySet()
        }
        check(activePersonalId() == owner) { "La sessió ha canviat llegint els Batecs vistos" }
        return remote + pending
    }
    /** Uses the same 7-day, 1000-key server list as Android and its own encrypted retry queue.
     * This is safe to retry because the PHP endpoint performs an UPSERT per (user, story_key).
     */
    suspend fun markBatecSeen(storyKey: String) {
        val key = IosBatecOperations.canonicalKey(storyKey)
        val owner = activePersonalId() ?: ensureGuestIdentity()
        queuedWrite(owner, "post", buildJsonObject { put("story_key", key) }, "batecs/seen.php")
    }
    /** Public read; use the bearer if registered so can_edit is calculated server-side. */
    suspend fun eventBatecs(eventId: Int): List<IosManualBatec> {
        require(eventId > 0)
        val owner = activePersonalId()
        val result = linkedMapOf<Int, IosManualBatec>()
        var offset = 0
        for (page in 0 until 12) {
            val root = request("batecs/index.php?event_id=$eventId&offset=$offset&limit=50", authenticated = false)
            check(activePersonalId() == owner) { "El compte ha canviat en llegir Batecs" }
            IosBatecOperations.decodeList(root, eventId).forEach { result[it.id] = it }
            if (!root.bool("has_more")) return result.values.toList()
            val next = root.text("next_offset").toIntOrNull()
                ?: error("Paginació de Batecs invàlida")
            check(next > offset) { "Paginació de Batecs no avança" }
            offset = next
        }
        error("Massa pàgines de Batecs; no es retornaran resultats parcials")
    }
    suspend fun batecEligibility(eventId: Int): IosBatecEligibility {
        require(eventId > 0)
        val owner = registeredBatecOwner()
        val root = IosServerConnection.json("batecs/eligibility.php?event_id=$eventId",
            bearer = owner.second, authenticated = true)
        checkSameRegisteredBatecOwner(owner)
        return IosBatecOperations.eligibility(root, eventId)
    }
    /** Creation is NOT retried offline: create.php does not accept an idempotency key. */
    suspend fun createBatec(eventId: Int, title: String, body: String): IosManualBatec {
        require(eventId > 0)
        val content = IosBatecOperations.validate(title, body)
        val owner = registeredBatecOwner()
        val root = IosServerConnection.json("batecs/create.php", buildJsonObject {
            put("event_id", eventId); put("title", content.first); put("body", content.second)
        }, owner.second, authenticated = true)
        checkSameRegisteredBatecOwner(owner)
        return IosBatecOperations.verifyMutation(root, eventId, content.first, content.second)
    }
    suspend fun updateBatec(id: Int, eventId: Int, title: String, body: String): IosManualBatec {
        require(id > 0 && eventId > 0)
        val content = IosBatecOperations.validate(title, body)
        val owner = registeredBatecOwner()
        val root = IosServerConnection.json("batecs/update.php", buildJsonObject {
            put("id", id); put("title", content.first); put("body", content.second)
        }, owner.second, authenticated = true)
        checkSameRegisteredBatecOwner(owner)
        return IosBatecOperations.verifyMutation(root, eventId, content.first, content.second, id)
    }
    suspend fun deleteBatec(id: Int, eventId: Int) {
        require(id > 0 && eventId > 0)
        val owner = registeredBatecOwner()
        val response = IosServerConnection.json("batecs/delete.php", buildJsonObject { put("id", id) },
            owner.second, authenticated = true)
        IosBatecOperations.verifyDelete(response, id)
        checkSameRegisteredBatecOwner(owner)
        // Do not accept a stale deletion after switching users.
        val checkList = IosServerConnection.json("batecs/index.php?event_id=$eventId&limit=50",
            bearer = owner.second, authenticated = true)
        checkSameRegisteredBatecOwner(owner)
        check(IosBatecOperations.decodeList(checkList, eventId).none { it.id == id }) {
            "La publicació continua al servidor; no es confirmarà l'eliminació"
        }
    }
    private fun registeredBatecOwner(): Pair<Int, String> {
        val id = account?.id?.takeIf { it > 0 } ?: error("Cal iniciar sessió per publicar o editar Batecs")
        val bearer = token?.takeIf(String::isNotBlank) ?: error("Cal iniciar sessió")
        return id to bearer
    }
    private fun checkSameRegisteredBatecOwner(owner: Pair<Int, String>) {
        check(account?.id == owner.first && token == owner.second) { "El compte ha canviat durant l'operació" }
    }
    /** Public entity metadata, matching the source already used by the Android recommendation engine. */
    suspend fun entityStyles(): JsonObject = request("entities/images.php")

    /** Android v336: public global inventory first; older ads/active.php only on failure.
     * Advertising is independent of the user token, personal filters and feed cache.
     * The legacy fallback cannot promise internal-ticket priority events outside the catalog.
     * The optional visual map applies an advertiser's own creative and focal point, not the
     * generic event poster, exactly as Android's mergeAdvertisementVisuals does.
     */
    suspend fun promoted(): IosPromoted {
        val global = try {
            request("ads/active-global.php")
        } catch (cancel: kotlinx.coroutines.CancellationException) {
            throw cancel
        } catch (_: Exception) {
            request("ads/active.php")
        }
        val inventory = decodePromoted(global)
        val visuals = try {
            IosCommercialVisuals.decode(request("ads/visual-map.php"))
        } catch (cancel: kotlinx.coroutines.CancellationException) {
            throw cancel
        } catch (_: Exception) { emptyMap() }
        return inventory.copy(ads = IosCommercialVisuals.apply(inventory.ads, visuals))
    }

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


    // Pas 44: el backend d'Android és l'única font de veritat de les campanyes.
    private val advertisingWriteMutex = Mutex()
    // RAM only. Closing/reopening the form does not create a second Stripe intent.
    private var pendingCampaignSession: Pair<Int, IosCampaignPayment>? = null
    internal fun pendingCampaignPayment(): IosCampaignPayment? =
        pendingCampaignSession?.takeIf { it.first == activePersonalId() }?.second
    internal fun clearPendingCampaignPayment(adId: Int) {
        if (pendingCampaignSession?.second?.adId == adId) pendingCampaignSession = null
    }
    private suspend fun campaignIdentity(): Triple<Int, String, String> {
        val identity = publisherIdentity()
        require(identity.first > 0 && identity.third.isEmpty() && account != null && !token.isNullOrBlank()) {
            "Inicia sessió per contractar publicitat. Les activitats de convidats es conserven."
        }
        return identity
    }
    private fun campaignTarget(raw: JsonObject): IosCampaignTarget {
        val t = raw["targets"] as? JsonObject ?: raw
        fun set(key: String): Set<String> = t.arr(key).mapNotNull {
            (it as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf(String::isNotBlank)
        }.toSet()
        return IosCampaignTarget(set("styles"), set("search_terms"), set("towns"), set("comarques"),
            t.bool("followers"), t.bool("high_intent"), raw.text("match_mode").equals("ALL", true))
    }
    private fun campaignRow(raw: JsonObject): IosCampaign {
        val id = raw.text("id").toIntOrNull() ?: 0
        val eventId = raw.text("event_id").toIntOrNull() ?: 0
        check(id > 0 && eventId > 0 && raw.text("placement") in IosAdvertisingPolicy.placements) {
            "El servidor ha retornat una campanya amb identificació invàlida."
        }
        val amount = raw.text("amount").toDoubleOrNull()?.let { kotlin.math.round(it * 100).toLong() } ?: 0L
        return IosCampaign(id, eventId, raw.text("placement"), raw.text("status"),
            raw.text("starts_at"), raw.text("ends_at"), amount,
            validMediaUrl(raw.text("image_url")), raw.text("card_focus_x").toFloatOrNull()?.coerceIn(0f,1f) ?: .5f,
            raw.text("card_focus_y").toFloatOrNull()?.coerceIn(0f,1f) ?: .5f, campaignTarget(raw))
    }
    private suspend fun campaignsFor(identity: Triple<Int, String, String>): List<IosCampaign> {
        val raw = IosServerConnection.json("ads/mine.php", bearer = identity.second, authenticated = true)
        checkPublisherIdentity(identity)
        check(raw["ads"] is JsonArray) { "El servidor no ha retornat la llista de campanyes." }
        val ads = raw.items("ads").map(::campaignRow)
        // Android overlays the paid creative's image and focal point from visual-map.php.
        val mapping = try {
            IosCommercialVisuals.decode(IosServerConnection.json("ads/visual-map.php",
                bearer = identity.second, authenticated = true))
        } catch (cancel: kotlinx.coroutines.CancellationException) {
            throw cancel
        } catch (_: Exception) { emptyMap() }
        checkPublisherIdentity(identity)
        return ads.map { campaign ->
            val visual = mapping[campaign.id]
            if (visual == null) campaign else campaign.copy(
                imageUrl = visual.imageUrl.ifBlank { campaign.imageUrl },
                focusX = visual.focusX, focusY = visual.focusY)
        }
    }
    suspend fun myCampaigns(): List<IosCampaign> = campaignsFor(campaignIdentity())
    private fun targetingPayload(target: IosCampaignTarget) = buildJsonObject {
        put("target_styles", JsonArray(target.styles.sorted().map { JsonPrimitive(it) }))
        put("target_search_terms", JsonArray(target.terms.sorted().map { JsonPrimitive(it) }))
        put("target_towns", JsonArray(target.towns.sorted().map { JsonPrimitive(it) }))
        put("target_comarques", JsonArray(target.comarques.sorted().map { JsonPrimitive(it) }))
        put("target_followers", target.followers)
        put("target_high_intent", target.highIntent)
        put("match_mode", if (target.all) "ALL" else "ANY")
    }
    private fun campaignStart(raw: JsonObject): IosCampaignPayment {
        val result = IosCampaignPayment(raw.text("ad_id").toIntOrNull() ?: 0,
            raw.text("client_secret"), raw.text("publishable_key"), raw.text("amount_cents").toLongOrNull() ?: 0L,
            raw.text("currency").ifBlank { "eur" }, raw.text("merchant_name").ifBlank { "l'Agenda" }, raw.text("mode").ifBlank { "test" })
        check(IosAdvertisingPolicy.validPayment(result)) { "El servidor no ha proporcionat un pagament Stripe vàlid." }
        return result
    }
    /** Non-idempotent: no POST retry on an ambiguous response; reconcile via ads/mine.php. */
    suspend fun createCampaignPayment(draft: IosCampaignDraft): IosCampaignPayment = advertisingWriteMutex.withLock {
        require(IosAdvertisingPolicy.valid(draft)) { "Duració, ubicació o segmentació invàlides." }
        val identity = campaignIdentity()
        check(pendingCampaignPayment() == null) { "Ja hi ha un pagament de campanya pendent. Consulta'l o reprén-lo." }
        val previous = campaignsFor(identity)
        check(previous.none { it.status.lowercase() in setOf("payment_pending", "pending", "requires_payment") }) {
            "El servidor ja té una campanya amb pagament pendent. Consulta l'estat abans de crear-ne una altra."
        }
        val own = ownedPublishedFor(identity).filter { it.status.equals("published", true) }.map { it.eventId }.toSet()
        val claimed = if (draft.eventId in own) emptySet() else try {
            proDashboardFor(identity, 0, "all").events.map { it.id }.toSet()
        } catch (cancel: kotlinx.coroutines.CancellationException) {
            throw cancel
        } catch (_: Exception) { emptySet() }
        require(draft.eventId in own || draft.eventId in claimed) { "El servidor no confirma la gestió d'este esdeveniment." }
        val root = IosServerConnection.json("ads/create-payment.php", buildJsonObject {
            put("event_id", draft.eventId)
            put("guest_id", "")
            put("placement", draft.placement)
            put("duration_days", draft.durationDays)
            targetingPayload(draft.target).forEach { (key, value) -> put(key, value) }
        }, identity.second, authenticated = true)
        checkPublisherIdentity(identity)
        campaignStart(root).also { pendingCampaignSession = identity.first to it }
    }
    private suspend fun campaignStatusFor(identity: Triple<Int, String, String>, adId: Int): IosCampaignStatus {
        val root = IosServerConnection.json("ads/status.php?ad_id=$adId", bearer = identity.second, authenticated = true)
        checkPublisherIdentity(identity)
        val serverId = root.text("ad_id").toIntOrNull() ?: adId
        check(serverId == adId) { "La resposta correspon a una altra campanya." }
        return IosCampaignStatus(adId, root.text("status").ifBlank { "payment_pending" },
            (root["ad"] as? JsonObject)?.let(::campaignRow))
    }
    suspend fun campaignStatus(adId: Int): IosCampaignStatus {
        require(adId > 0)
        return campaignStatusFor(campaignIdentity(), adId)
    }
    /** PaymentSheet COMPLETED is NOT an active campaign; recheck both endpoints. */
    suspend fun confirmedActiveCampaign(adId: Int): IosCampaign? {
        require(adId > 0)
        val identity = campaignIdentity()
        val status = campaignStatusFor(identity, adId)
        if (!status.status.equals("active", true)) return null
        val current = campaignsFor(identity)
        check(IosAdvertisingPolicy.activeConfirmed(adId, status, current)) {
            "El servidor encara no confirma que la campanya estiga activa."
        }
        return current.single { it.id == adId }
    }
    suspend fun updateCampaignTarget(ad: IosCampaign, target: IosCampaignTarget): IosCampaign = advertisingWriteMutex.withLock {
        require(IosAdvertisingPolicy.valid(IosCampaignDraft(ad.eventId, ad.placement, 1, target)))
        val identity = campaignIdentity()
        val before = campaignsFor(identity).singleOrNull { it.id == ad.id && it.eventId == ad.eventId }
            ?: error("La campanya no està vinculada al compte actual.")
        IosServerConnection.json("ads/update.php", buildJsonObject {
            put("ad_id", before.id)
            targetingPayload(target).forEach { (key, value) -> put(key, value) }
        }, identity.second, authenticated = true)
        checkPublisherIdentity(identity)
        val after = campaignsFor(identity)
        check(IosAdvertisingPolicy.updatedTarget(before, target, after)) {
            "La segmentació encara no està confirmada pel servidor; revisa-la abans de repetir."
        }
        after.single { it.id == before.id }
    }
    suspend fun updateCampaignVisual(ad: IosCampaign, x: Float, y: Float,
                                     base64Jpeg: String? = null, removeImage: Boolean = false): IosCampaign = advertisingWriteMutex.withLock {
        require(ad.id > 0 && x in 0f..1f && y in 0f..1f && !(removeImage && base64Jpeg != null))
        val identity = campaignIdentity()
        require(campaignsFor(identity).any { it.id == ad.id && it.eventId == ad.eventId }) {
            "La campanya no està vinculada al compte."
        }
        var uploadedUrl: String? = null
        if (base64Jpeg != null) {
            require(base64Jpeg.length in 500..7_000_000 && base64Jpeg.all {
                it.isLetterOrDigit() || it == '+' || it == '/' || it == '='
            }) { "JPEG massa gran o invàlid." }
            val uploadReceipt = IosServerConnection.json("images/upload.php", buildJsonObject {
                put("owner_type", "AD"); put("owner_key", ad.id.toString())
                put("mime_type", "image/jpeg"); put("image_base64", base64Jpeg)
            }, identity.second, authenticated = true)
            checkPublisherIdentity(identity)
            uploadedUrl = validMediaUrl(uploadReceipt.text("image_url"))
            check(!uploadedUrl.isNullOrBlank()) { "El servidor no ha confirmat l'URL de la nova creativitat." }
        }
        IosServerConnection.json("ads/visual.php", buildJsonObject {
            put("ad_id", ad.id); put("card_focus_x", x); put("card_focus_y", y)
            put("remove_image", removeImage)
        }, identity.second, authenticated = true)
        checkPublisherIdentity(identity)
        val after = campaignsFor(identity)
        check(IosAdvertisingPolicy.updatedVisual(ad.id, x, y, removeImage, after)) {
            "El servidor no confirma la composició de l'anunci; consulta-la abans de tornar a enviar."
        }
        val verified = after.single { it.id == ad.id }
        if (base64Jpeg != null) check(verified.imageUrl == uploadedUrl) {
            "La nova imatge no apareix encara en l'anunci. Revisa el servidor abans de repetir."
        }
        verified
    }

    // PAS 46 — donacions a grups i serveis especials, PHP/MariaDB autoritatius.
    // POST de pagament no idempotents: bloqueig per sessió i resposta incerta sense reintents automàtics.
    private val donationMutex = Mutex()
    private val servicePaymentMutex = Mutex()
    private var donationPending: IosDonationPayment? = null
    private var donationCreationUncertain = false
    private var servicePending: IosSpecialPayment? = null
    private var serviceCreationUncertain = false
    internal fun pendingDonation() = donationPending?.takeIf { it.ownerId == activePersonalId() }
    internal fun donationUncertain() = donationCreationUncertain
    internal fun clearDonation(id: Long) { if (donationPending?.donationId == id) donationPending = null }
    internal fun acknowledgeDonationUncertain() { donationCreationUncertain = false }
    internal fun pendingSpecialPayment() = servicePending?.takeIf { it.ownerId == activePersonalId() }
    internal fun specialPaymentUncertain() = serviceCreationUncertain
    internal fun clearSpecialPayment(id: Long) { if (servicePending?.orderId == id) servicePending = null }
    internal fun acknowledgeSpecialUncertain() { serviceCreationUncertain = false }

    private fun donationDecode(c: JsonObject) = IosDonationConfig(c.bool("active"),
        c.text("title"), c.text("description"), c.text("image_url"),
        c.arr("suggested_amounts_cents").mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.toLongOrNull() }.filter { it > 0 },
        c.text("min_amount_cents").toLongOrNull() ?: 100L,
        c.text("max_amount_cents").toLongOrNull() ?: 100000L)

    suspend fun artistDonationConfig(artist: String): IosDonationConfig {
        require(IosArtistStorePolicy.validArtist(artist))
        val raw = request("artist-store/donations/config.php?artist=${artist.trim().encodeURLParameter()}")
        val c = raw["config"] as? JsonObject ?: error("Falta la configuració de donacions.")
        return donationDecode(c)
    }
    suspend fun saveDonationConfig(artist: String, config: IosDonationConfig): IosDonationConfig {
        require(IosSpecialCommercePolicy.validConfig(config))
        val who = storeIdentity(); requireManagedArtist(who, artist)
        IosServerConnection.json("artist-store/donations/save-config.php", buildJsonObject {
            put("artist", artist); put("active", config.active); put("title", config.title)
            put("description", config.description); put("image_url", config.imageUrl)
            put("suggested_amounts_cents", JsonArray(config.suggested.map { JsonPrimitive(it) }))
            put("min_amount_cents", config.minCents); put("max_amount_cents", config.maxCents)
        }, who.second, authenticated = true)
        checkStoreIdentity(who)
        val fresh = artistDonationConfig(artist)
        check(fresh == config) { "Les donacions encara no estan confirmades al servidor; consulta-ho abans de repetir." }
        return fresh
    }
    suspend fun createDonation(artist: String, amount: Long, name: String, message: String,
        anonymous: Boolean): IosDonationPayment = donationMutex.withLock {
        val who = storeIdentity()
        require(IosArtistStorePolicy.validArtist(artist) && name.length <= 190 && message.length <= 2000)
        require(IosSpecialCommercePolicy.validDonationAmount(artistDonationConfig(artist), amount)) { "Import no disponible." }
        check(donationPending == null && !donationCreationUncertain) { "Consulta primer la donació pendent." }
        donationCreationUncertain = true
        val raw = IosServerConnection.json("artist-store/donations/payment-create.php", buildJsonObject {
            put("artist", artist); put("amount_cents", amount); put("donor_name", name.trim())
            put("message", message.trim()); put("anonymous", anonymous)
        }, who.second, authenticated = true)
        checkStoreIdentity(who)
        val result = IosDonationPayment(who.first, raw.text("donation_id").toLongOrNull() ?: 0,
            artist, raw.text("amount_cents").toLongOrNull() ?: 0,
            raw.text("client_secret"), raw.text("publishable_key"),
            raw.text("merchant_name").ifBlank { "l'Agenda" }, raw.text("mode").ifBlank { "test" })
        check(IosSpecialCommercePolicy.validDonationPayment(result) && result.amountCents == amount) { "Stripe ha retornat dades incongruents." }
        donationPending = result; donationCreationUncertain = false
        result
    }
    suspend fun donationStatus(id: Long): IosDonationReceipt {
        require(id > 0)
        val who = storeIdentity()
        val raw = IosServerConnection.json("artist-store/donations/payment-status.php?donation_id=$id",
            bearer = who.second, authenticated = true)
        checkStoreIdentity(who)
        return IosDonationReceipt(raw.text("donation_id").toLongOrNull() ?: id,
            raw.text("status"), raw.text("amount_cents").toLongOrNull() ?: -1L)
    }

    private fun decodeSpecial(raw: JsonObject): IosSpecialEvent {
        val event = raw["event"] as? JsonObject ?: error("El servidor no ha retornat l'esdeveniment especial.")
        val eventId = event.text("event_id").toIntOrNull() ?: 0
        val access = raw["access"] as? JsonObject
        val services = raw.items("services").map { s ->
            val serviceId = s.text("id").toLongOrNull() ?: 0L
            IosSpecialService(serviceId, s.text("event_id").toIntOrNull() ?: eventId,
                s.text("name"), s.text("description"), s.text("image_url"),
                s.text("overlay_color").ifBlank { "#FFE082" },
                s.text("active").let { it.isBlank() || it == "1" || it.equals("true", true) },
                s.items("products").map { p ->
                    IosServiceProduct(p.text("id").toLongOrNull() ?: 0L,
                        p.text("service_id").toLongOrNull() ?: serviceId, p.text("name"),
                        p.text("description"), p.text("image_url"), p.text("price_cents").toLongOrNull() ?: 0L,
                        p.bool("reserve_only"), p.text("stock").toIntOrNull(),
                        p.text("active").let { it.isBlank() || it == "1" || it.equals("true", true) })
                })
        }
        check(eventId > 0) { "Identificador de servei no vàlid." }
        return IosSpecialEvent(eventId, event.text("display_title"),
            event.text("enabled").let { it.isBlank() || it == "1" || it.equals("true", true) },
            access?.bool("can_edit_services") == true, services)
    }
    suspend fun specialEvent(eventId: Int, manage: Boolean = false): IosSpecialEvent {
        require(eventId > 0)
        val raw = if (manage) {
            val who = storeIdentity()
            val data = IosServerConnection.json("special-events/manage/config.php?event_id=$eventId",
                bearer = who.second, authenticated = true)
            checkStoreIdentity(who); data
        } else request("special-events/public.php?event_id=$eventId")
        val result = decodeSpecial(raw)
        check(result.id == eventId) { "El servidor ha retornat un altre esdeveniment." }
        return result
    }
    suspend fun managedSpecialEvents(): List<IosSpecialEventAccess> {
        val who = storeIdentity()
        val raw = IosServerConnection.json("special-events/manage/my-events.php",
            bearer = who.second, authenticated = true)
        checkStoreIdentity(who)
        return raw.items("events").mapNotNull { item ->
            val id = item.text("event_id").toIntOrNull() ?: return@mapNotNull null
            if (id <= 0) null else IosSpecialEventAccess(id, item.text("display_title"),
                item.text("enabled") != "0")
        }
    }
    suspend fun saveSpecialService(eventId: Int, id: Long, name: String, desc: String,
        image: String, color: String): IosSpecialEvent {
        require(id >= 0 && IosSpecialCommercePolicy.validServiceName(name) && desc.length <= 4000 &&
            IosSpecialCommercePolicy.validImage(image) && color.matches(Regex("#[0-9A-Fa-f]{6}")))
        val who = storeIdentity()
        val before = specialEvent(eventId, manage = true)
        require(before.canEditServices) { "El servidor no confirma permís d'edició." }
        IosServerConnection.json("special-events/manage/save-service.php", buildJsonObject {
            put("event_id", eventId); put("id", id); put("name", name.trim())
            put("description", desc.trim()); put("image_url", image); put("overlay_color", color)
            put("sort_order", 0); put("active", true)
        }, who.second, authenticated = true)
        checkStoreIdentity(who)
        val after = specialEvent(eventId, manage = true)
        check(IosSpecialCommercePolicy.serviceSaved(eventId, before.services, after.services, id,
            name.trim(), desc.trim(), image.trim(), color)) {
            "El servidor no confirma els canvis del servei; no repetisques automàticament." }
        return after
    }
    suspend fun deleteSpecialService(eventId: Int, id: Long): IosSpecialEvent {
        require(id > 0)
        val who = storeIdentity(); val before = specialEvent(eventId, manage = true)
        require(before.canEditServices && before.services.any { it.id == id })
        IosServerConnection.json("special-events/manage/delete-service.php", buildJsonObject {
            put("event_id", eventId); put("id", id)
        }, who.second, authenticated = true)
        checkStoreIdentity(who)
        val after = specialEvent(eventId, manage = true)
        check(after.services.none { it.id == id && it.active }) { "El servei encara apareix al servidor." }
        return after
    }
    suspend fun saveSpecialProduct(eventId: Int, draft: IosServiceProduct): IosSpecialEvent {
        require(IosSpecialCommercePolicy.validProduct(draft))
        val who = storeIdentity(); val before = specialEvent(eventId, manage = true)
        require(before.canEditServices)
        val svc = before.services.firstOrNull { it.id == draft.serviceId } ?: error("Servei no autoritzat.")
        IosServerConnection.json("special-events/manage/save-product.php", buildJsonObject {
            put("event_id", eventId); put("service_id", draft.serviceId); put("id", draft.id)
            put("name", draft.name.trim()); put("description", draft.description.trim())
            put("image_url", draft.imageUrl); put("price_cents", draft.priceCents)
            put("reserve_only", draft.reserveOnly); if (draft.stock != null) put("stock", draft.stock)
            else put("stock", JsonNull)
            put("sort_order", 0); put("active", true)
        }, who.second, authenticated = true)
        checkStoreIdentity(who)
        val after = specialEvent(eventId, manage = true)
        val products = after.services.firstOrNull { it.id == draft.serviceId }?.products.orEmpty()
        check(IosSpecialCommercePolicy.productSaved(svc.products, products, draft)) {
            "El servidor no confirma el producte. Consulta abans de repetir." }
        return after
    }
    suspend fun deleteSpecialProduct(eventId: Int, serviceId: Long, id: Long): IosSpecialEvent {
        require(serviceId > 0 && id > 0)
        val who = storeIdentity(); val before = specialEvent(eventId, manage = true)
        require(before.canEditServices && before.services.any { s -> s.id == serviceId && s.products.any { it.id == id } })
        IosServerConnection.json("special-events/manage/delete-product.php", buildJsonObject {
            put("event_id", eventId); put("service_id", serviceId); put("id", id)
        }, who.second, authenticated = true)
        checkStoreIdentity(who)
        val after = specialEvent(eventId, manage = true)
        check(after.services.none { s -> s.id == serviceId && s.products.any { it.id == id && it.active } }) {
            "El producte encara apareix al servidor." }
        return after
    }
    suspend fun createSpecialPayment(eventId: Int, service: IosSpecialService,
        items: List<Pair<IosServiceProduct, Int>>): IosSpecialPayment = servicePaymentMutex.withLock {
        val who = storeIdentity()
        require(service.eventId == eventId && items.isNotEmpty() && items.size <= 30) { "La comanda és invàlida." }
        require(items.map { it.first.id }.distinct().size == items.size) { "Productes duplicats." }
        check(servicePending == null && !serviceCreationUncertain) { "Consulta primer la comanda pendent." }
        val publicEvent = specialEvent(eventId)
        require(publicEvent.enabled) { "Este esdeveniment especial no està actiu." }
        val live = publicEvent.services.firstOrNull { it.id == service.id }
            ?: error("Este servei ja no està disponible.")
        val selected = items.map { (oldProduct, count) ->
            val item = live.products.firstOrNull { it.id == oldProduct.id }
                ?: error("Un producte ja no està disponible.")
            require(IosSpecialCommercePolicy.canBuy(live, item, count)) { "Quantitat o estoc no vàlids." }
            item to count
        }
        serviceCreationUncertain = true
        val raw = IosServerConnection.json("payments/service-create.php", buildJsonObject {
            put("event_id", eventId); put("service_id", live.id); put("service_name", live.name)
            put("items", JsonArray(selected.map { (item, count) -> buildJsonObject {
                put("product_id", item.id); put("product_name", item.name); put("quantity", count)
            } }))
        }, who.second, authenticated = true)
        checkStoreIdentity(who)
        val result = IosSpecialPayment(who.first, raw.text("order_id").toLongOrNull() ?: 0L,
            eventId, live.id, raw.text("client_secret"), raw.text("publishable_key"),
            raw.text("amount_cents").toLongOrNull() ?: 0L,
            raw.text("merchant_name").ifBlank { "l'Agenda" }, raw.text("mode").ifBlank { "test" })
        check(IosSpecialCommercePolicy.validSpecialPayment(result)) { "Resposta del servidor/Stripe no vàlida." }
        servicePending = result; serviceCreationUncertain = false
        result
    }
    suspend fun specialOrderStatus(id: Long): IosSpecialOrder {
        require(id > 0)
        val who = storeIdentity()
        val raw = IosServerConnection.json("payments/service-status.php?order_id=$id",
            bearer = who.second, authenticated = true)
        checkStoreIdentity(who)
        return IosSpecialOrder(raw.text("order_id").toLongOrNull() ?: id, raw.text("status"),
            raw.text("service_name"), raw.text("total_cents").toLongOrNull() ?: 0L, raw.text("qr_payload"))
    }
    suspend fun inspectSpecialOrder(code: String): IosSpecialInspection {
        require(code.isNotBlank() && code.length <= 4096)
        val who = storeIdentity()
        val raw = IosServerConnection.json("special-events/order-inspect.php", buildJsonObject {
            put("qr_token", code.trim())
        }, who.second, authenticated = true)
        checkStoreIdentity(who)
        return IosSpecialInspection(raw.text("order_id").toLongOrNull() ?: 0L,
            raw.text("event_id").toIntOrNull() ?: 0, raw.text("status"),
            raw.text("service_name"), raw.text("total_cents").toLongOrNull() ?: 0L)
    }
    suspend fun redeemSpecialOrder(code: String, expected: IosSpecialInspection): String {
        require(IosSpecialCommercePolicy.redeemAllowed(expected)) { "Esta comanda no és bescanviable." }
        val who = storeIdentity()
        val raw = IosServerConnection.json("special-events/order-redeem.php", buildJsonObject {
            put("qr_token", code.trim())
        }, who.second, authenticated = true)
        checkStoreIdentity(who)
        val status = raw.text("status")
        check(status.equals("redeemed", true)) { "El servidor no confirma el bescanvi: $status" }
        return status
    }

    // Pas 47: security-sensitive admin calls use the exact registered identity and
    // bearer frozen before IO. A guest bearer or an outdated account must never
    // be substituted through request() on a delayed, privileged operation.
    private val adminMutationMutex = Mutex()
    private fun adminIdentity(): Pair<Int, String> {
        val current = account ?: error("Cal una sessió registrada de superadministrador.")
        val bearer = token?.takeIf { it.isNotBlank() } ?: error("Sessió caducada.")
        require(IosAdminPermissions.canOpen(current.role) && current.id > 0) {
            "Accés exclusiu de superadministració."
        }
        return current.id to bearer
    }
    private fun checkAdminIdentity(who: Pair<Int, String>) {
        check(account?.id == who.first && account?.role == "SUPER_ADMIN" && token == who.second) {
            "La sessió d'administració ha canviat. Descarta la resposta."
        }
    }
    private suspend fun adminRead(path: String): JsonObject {
        val who = adminIdentity()
        val response = IosServerConnection.json(path, bearer = who.second, authenticated = true)
        checkAdminIdentity(who)
        return response
    }
    private suspend fun adminWrite(path: String, payload: JsonObject): JsonObject {
        val who = adminIdentity()
        val response = IosServerConnection.json(path, payload, who.second, authenticated = true)
        checkAdminIdentity(who)
        check(!response.containsKey("ok") || response.bool("ok")) {
            "El servidor no ha confirmat l'operació d'administració."
        }
        return response
    }
    /** Read-only administration: server must independently enforce SUPER_ADMIN. */
    suspend fun adminDashboard(days: Int = 30): JsonObject =
        adminRead("admin/dashboard.php?days=${days.coerceIn(1, 365)}")

    suspend fun adminUsers(): List<JsonObject> = adminRead("admin/users.php").items("users")

    suspend fun adminUserDetail(userId: Int): JsonObject {
        require(userId > 0)
        return adminRead("admin/user-detail.php?user_id=$userId&days=90")
    }

    /** This server endpoint requires the separate SQL migration and deployment. */
    suspend fun adminSetUserAccess(userId: Int, role: String, active: Boolean): JsonObject = adminMutationMutex.withLock {
        val who = adminIdentity()
        require(IosAdminPermissions.canEdit(who.first, "SUPER_ADMIN", userId) && IosAdminPermissions.validRole(role))
        val before = adminUsers().firstOrNull { it.text("id").toIntOrNull() == userId }
            ?: error("Cal verificar abans l'usuari i els permisos al servidor.")
        if (before.text("role") == role && before.bool("active") == active) return@withLock before
        val receipt = adminWrite("admin/user-access-ios.php", buildJsonObject {
            put("user_id", userId); put("role", role); put("active", active)
            put("confirmation", "CANVIAR ACCES")
        })
        require(receipt.text("user_id").toIntOrNull() == userId && receipt.text("role") == role &&
            receipt.bool("active") == active) { "Resposta d'accés incompatible amb el canvi sol·licitat." }
        val confirmed = adminUsers().firstOrNull { it.text("id").toIntOrNull() == userId }
        check(confirmed != null && confirmed.text("role") == role && confirmed.bool("active") == active) {
            "El canvi d'accés no està confirmat per la lectura posterior del servidor. No repetisques l'operació sense revisar-la."
        }
        confirmed
    }

    suspend fun adminFindEvents(query: String, page: Int = 1): JsonObject =
        adminRead("admin/events.php?q=${query.trim().take(180).encodeURLParameter()}&page=${page.coerceIn(1, 10000)}&limit=25")

    /** Existing server endpoint anonymizes account; it never hard-deletes fiscal records. */
    suspend fun adminDeleteUser(userId: Int, username: String): JsonObject = adminMutationMutex.withLock {
        val who = adminIdentity()
        require(IosAdminPermissions.canEdit(who.first, "SUPER_ADMIN", userId) && username.isNotBlank())
        val before = adminUsers().firstOrNull { it.text("id").toIntOrNull() == userId }
        require(before != null && before.text("username") == username) { "Cal comprovar la identitat exacta de l'usuari." }
        val receipt = adminWrite("admin/user-delete.php", buildJsonObject {
            put("user_id", userId); put("confirmation", "ELIMINAR $username")
        })
        val after = adminUsers().firstOrNull { it.text("id").toIntOrNull() == userId }
        check(after == null || !after.bool("active") && after.text("username") != username) {
            "No s'ha pogut verificar l'anonimització al servidor. Comprova-ho manualment abans de repetir-la."
        }
        receipt
    }

    suspend fun adminEventReports(): List<JsonObject> = adminRead("admin/reports.php").items("reports")

    suspend fun adminCommunityPending(): List<JsonObject> =
        adminRead("events/community-pending.php").items("items")

    suspend fun adminReviewCommunity(eventId: Int, decision: String, notes: String) = adminMutationMutex.withLock {
        require(IosAdminActionPolicy.validReview(eventId, decision, notes)) { "Revisió sense permís, identificador o justificació vàlida." }
        val before = adminCommunityPending()
        require(before.any { it.text("event_id").toIntOrNull() == eventId }) {
            "La publicació ja no està pendent. Actualitza la cua abans de decidir."
        }
        adminWrite("events/community-review.php", buildJsonObject {
            put("event_id", eventId); put("decision", decision); put("notes", notes.trim().take(2000))
        })
        check(adminCommunityPending().none { it.text("event_id").toIntOrNull() == eventId }) {
            "La publicació continua pendent; revisa-la abans de repetir la decisió."
        }
    }

    suspend fun adminModerateEvent(eventId: Int, action: String, reason: String): JsonObject = adminMutationMutex.withLock {
        require(IosAdminActionPolicy.validModeration(eventId, action, reason)) { "Motiu o acció de moderació invàlids." }
        // moderation/review.php may include side effects (warnings or suspensions).
        // Never retry automatically: a successful POST alone doesn't prove that
        // a report was formally resolved or that the event disappeared from cache.
        adminWrite("moderation/review.php", buildJsonObject {
            put("event_id", eventId); put("action", action); put("reason", reason.trim().take(2000))
        })
    }

    suspend fun adminPreviewTargets(filters: JsonObject): JsonObject =
        adminWrite("admin/notifications-preview.php", buildJsonObject { put("filters", filters) })

    suspend fun adminSendTargets(title: String, body: String, count: Int, filters: JsonObject): JsonObject = adminMutationMutex.withLock {
        require(IosAdminActionPolicy.validCampaign(title, body, count))
        val live = adminPreviewTargets(filters).text("count").toIntOrNull() ?: -1
        check(live == count) { "Els destinataris han canviat: torna a previsualitzar abans d'enviar." }
        val receipt = adminWrite("admin/notifications-send.php", buildJsonObject {
            put("filters", filters); put("title", title.trim().take(160)); put("body", body.trim().take(1200))
            put("expected_count", count); put("confirmation", "ENVIAR")
        })
        check(receipt.text("recipient_count").toIntOrNull() == count) {
            "La confirmació de l'enviament no coincidix amb els destinataris previstos. No el repetisques sense comprovar l'historial."
        }
        receipt
    }

    suspend fun adminPreview(): Int = adminPreviewTargets(buildJsonObject {
        put("all_users", true); put("platform", "")
    }).text("count").toIntOrNull() ?: 0

    suspend fun adminSend(title: String, body: String, count: Int): JsonObject = adminSendTargets(title, body, count,
        buildJsonObject { put("all_users", true); put("platform", "") })

}

data class IosAccount(val id: Int, val username: String, val email: String, val role: String)
data class IosFollow(val type: String, val key: String, val label: String)
data class IosUserState(val follows: List<IosFollow> = emptyList(), val saved: Set<Int> = emptySet(),
    val alerts: Set<Int> = emptySet(), val affinities: List<IosServerAffinity> = emptyList()) {
    fun follows(artist: String) = followsType("ARTIST", artist)
    fun followsType(type: String, name: String): Boolean = IosFollowMutation.contains(follows, type, name)
}
data class IosNotification(val id: Int, val title: String, val body: String, val read: Boolean, val createdAt: String, val linkType: String, val linkValue: String)
data class IosTicket(val id: String, val eventId: String, val eventTitle: String, val status: String,
    val code: String, val ticketOptionName: String = "", val sessionDate: String = "",
    val sessionTime: String = "", val sector: String = "", val offer: String = "",
    val validatedAt: String = "")
data class IosCommunityGroup(val id: Int, val eventId: Int, val title: String, val type: String,
    val access: String, val membership: String, val description: String, val creatorUserId: Int = 0,
    val status: String = "OPEN", val origin: String = "", val meeting: String = "", val capacity: Int = 0,
    val participants: Int = 0, val creatorName: String = "", val chatEnabled: Boolean = true,
    val members: List<IosCommunityMember> = emptyList())
data class IosCommunityUser(val id: Int, val name: String)
data class IosCommunityMember(val id:Int,val name:String,val role:String,val status:String)
data class IosCommunityManagement(val members:List<IosCommunityMember>,val bans:List<IosCommunityUser>)
data class IosCommunityReport(val id:Int,val groupId:Int,val messageId:Int,val reportedUserId:Int,
    val groupTitle:String,val reportedName:String,val reason:String,val messageText:String,val status:String)
internal fun decodeCommunityGroup(g: JsonObject): IosCommunityGroup = IosCommunityGroup(
    g.text("id").toIntOrNull()?:0,g.text("event_id").toIntOrNull()?:0,g.text("title","name"),
    IosCommunityBoardContract.canonicalType(g.text("board_type").ifBlank { g.text("type","group_type") }),
    g.text("access_mode"),g.text("membership_status","my_status"),
    g.text("description"),g.text("creator_user_id").toIntOrNull()?:0,g.text("status").ifBlank{"OPEN"},
    g.text("origin_label"),g.text("meeting_at"),g.text("capacity").toIntOrNull()?:0,
    g.text("participant_count").toIntOrNull()?:0,g.text("creator_name"),
    g["chat_enabled"]?.jsonPrimitive?.booleanOrNull ?: true,
    g.items("members").mapNotNull { m ->
        val id=m.text("user_id").toIntOrNull() ?: 0
        if(id<=0) null else IosCommunityMember(id,m.text("display_name"),m.text("role"),m.text("status"))
    })
data class IosCommunityInquiry(val id:Int,val groupId:Int,val peerUserId:Int,val peerName:String,
    val author:String,val body:String,val createdAt:String,val own:Boolean)
data class IosCommunityInquiryInbox(val id:Int,val groupId:Int,val eventId:Int,val boardTitle:String,
    val author:String,val authorUserId:Int,val body:String,val createdAt:String)
data class IosMemberRequest(val userId: Int, val displayName: String, val requestedAt: String)
data class IosMessage(val id: String, val author: String, val body: String, val createdAt: String, val userId:Int=0)
data class IosBatec(val id: String, val eventId: Int, val artist: String, val eventTitle: String,
    val text: String, val imageUrl: String, val storyKey: String = "",
    val sourceType: String = "MANUAL", val publishedAt: String = "", val eventDate: String = "")
data class IosAd(val id: String, val eventId: Int, val placement: String, val imageUrl: String,
                     val focusX: Float = 0.5f, val focusY: Float = 0.5f)
data class IosPromoted(val guaranteedEventIds: Set<Int> = emptySet(), val ads: List<IosAd> = emptyList(),
                       val priorityEvents: List<AgendaEventSummary> = emptyList())

/** Values come from tickets/options.php, not calculated from the event card. */
/* IosTicketOption i IosTicketCatalog: IosTicketCatalogPolicy.kt */

/** Owner is authoritative: invited list members cannot edit another user's list. */
data class IosSavedList(val id: Int, val name: String, val owner: Boolean,
                        val inviteUrl: String, val shareCode: String, val eventIds: Set<Int>)
data class IosSavedListShare(val inviteUrl: String, val shareCode: String)
data class IosProfileSettings(val role: String, val phone: String, val emailVerified: Boolean)

/** Only supported server fields are decoded. */
// IosSavedSearch lives in IosSearchPresetContract.kt; the server owns its complete Filters payload.

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
data class IosMyProfile(val displayName: String, val bio: String, val photoUrl: String,
    val homeTown: String = "", val discipline: String = "", val companyName: String = "",
    val castText: String = "", val publisherName: String = "", val booksText: String = "",
    val worksText: String = "", val spacesText: String = "")
