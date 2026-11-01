package com.example.agendavalenciana.ios

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/** Pas 1: contracte de transport i URL canònica idèntics als d'Android v336.
 *  Android queda intacte. Açò no crea usuaris convidats ni sincronitza preferències:
 *  s'incorporaran en els passos posteriors.
 */
internal object IosServerConnection {
    const val SITE_BASE = "https://quimerafest.com"
    const val APP_BASE = "$SITE_BASE/app/"
    const val API_BASE = "${APP_BASE}api/"

    // ServerApiRepository.kt v336: CONNECT_TIMEOUT = 12000, READ_TIMEOUT = 25000.
    private val client = HttpClient {
        install(HttpTimeout) {
            connectTimeoutMillis = 12_000
            socketTimeoutMillis = 25_000
            requestTimeoutMillis = 30_000
        }
    }

    fun endpoint(path: String): String {
        require(path.isNotBlank() && !path.startsWith('/') &&
                !path.contains("://") && !path.contains("..") && !path.contains('#')) {
            "Ruta d'API no vàlida"
        }
        return API_BASE + path
    }

    /** GET/POST JSON, Accept JSON, no-cache i Bearer com Android; el servidor valida el token. */
    suspend fun json(path: String, payload: JsonObject? = null, bearer: String? = null,
                     authenticated: Boolean = false): JsonObject {
        if (authenticated) require(!bearer.isNullOrBlank()) { "Cal iniciar sessió." }
        val response = if (payload == null) client.get(endpoint(path)) {
            header(HttpHeaders.Accept, "application/json")
            header(HttpHeaders.CacheControl, "no-cache")
            if (!bearer.isNullOrBlank()) header(HttpHeaders.Authorization, "Bearer $bearer")
        } else client.post(endpoint(path)) {
            header(HttpHeaders.Accept, "application/json")
            header(HttpHeaders.CacheControl, "no-cache")
            contentType(ContentType.Application.Json)
            if (!bearer.isNullOrBlank()) header(HttpHeaders.Authorization, "Bearer $bearer")
            setBody(payload.toString())
        }
        val status = response.status.value
        val raw = response.bodyAsText()
        val root = runCatching { Json.parseToJsonElement(raw) as? JsonObject }.getOrNull()
        if (status !in 200..299) {
            error(root?.text("message", "error")?.ifBlank { "HTTP $status" } ?: "HTTP $status")
        }
        require(root != null) { "El servidor ha retornat una resposta invàlida." }
        require(root.bool("ok")) {
            root.text("message", "error").ifBlank { "Resposta no vàlida del servidor." }
        }
        return root
    }

    /** Catàleg públic: mateixa configuració HTTP; retorna JSON cru per al cache efímer. */
    suspend fun publicJsonText(path: String): String {
        val response = client.get(endpoint(path)) {
            header(HttpHeaders.Accept, "application/json")
            header(HttpHeaders.CacheControl, "no-cache")
        }
        val raw = response.bodyAsText()
        if (response.status.value !in 200..299) {
            val detail = runCatching { Json.parseToJsonElement(raw) as? JsonObject }
                .getOrNull()?.text("message", "error").orEmpty()
            error(detail.ifBlank { "HTTP ${response.status.value}" })
        }
        return raw
    }
}
