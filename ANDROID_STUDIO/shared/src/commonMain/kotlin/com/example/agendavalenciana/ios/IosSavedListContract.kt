package com.example.agendavalenciana.ios

import com.example.agendavalenciana.model.IosSavedListInvitation
import kotlinx.serialization.json.*

/** Server-owned collections: same entity IDs and permissions as Android v336. */
internal data class IosSavedListCommand(
    val action: String, val listId: Int? = null, val eventId: Int? = null,
    val name: String? = null, val add: Boolean? = null, val invite: String? = null
)
internal data class IosVerifiedListChange(
    val lists: List<IosSavedList>, val share: IosSavedListShare? = null
)

internal object IosSavedListContract {
    fun decode(root: JsonObject): List<IosSavedList> {
        require(root["ok"]?.jsonPrimitive?.booleanOrNull == true) { "Llistes sense confirmació." }
        val array = root["lists"] as? JsonArray ?: error("Falten llistes a la resposta.")
        val lists = array.map { element ->
            val obj = element as? JsonObject ?: error("Llista mal formada.")
            val id = obj["id"]?.jsonPrimitive?.intOrNull?.takeIf { it > 0 }
                ?: error("ID de llista invàlid.")
            val name = obj["name"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                ?: error("Nom de llista invàlid.")
            val owned = obj["owner"]?.jsonPrimitive?.booleanOrNull
                ?: error("Falta el permís de la llista.")
            val events = obj["event_ids"] as? JsonArray ?: error("Falten esdeveniments.")
            IosSavedList(id, name, owned,
                obj["invite_url"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                obj["share_code"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                events.map { e -> e.jsonPrimitive.intOrNull?.takeIf { it > 0 }
                    ?: error("ID d'esdeveniment invàlid.") }.toSet())
        }
        require(lists.map { it.id }.distinct().size == lists.size) { "Llistes duplicades." }
        return lists
    }

    fun preflight(before: List<IosSavedList>, op: IosSavedListCommand) {
        if (op.action == "create_list" || op.action == "join_list") return
        require((op.listId ?: 0) > 0) { "Identificador de llista invàlid." }
        val list = before.firstOrNull { it.id == op.listId }
            ?: error("La llista no pertany a este compte.")
        when (op.action) {
            "rename_list", "delete_list", "list_event", "share_list", "revoke_list_share" ->
                check(list.owner) { "Només el creador pot modificar o compartir esta llista." }
            "leave_list" -> check(!list.owner) { "El creador no pot abandonar la seua llista." }
            else -> error("Operació de llista no disponible.")
        }
    }

    fun verify(before: List<IosSavedList>, after: List<IosSavedList>, op: IosSavedListCommand,
               response: JsonObject): IosVerifiedListChange {
        require(response["ok"]?.jsonPrimitive?.booleanOrNull == true) { "El servidor no ha confirmat l'operació." }
        val old = before.firstOrNull { it.id == op.listId }
        val current = after.firstOrNull { it.id == op.listId }
        var shared: IosSavedListShare? = null
        when (op.action) {
            "create_list" -> check(after.any { row ->
                row.owner && row.id !in before.map { it.id } && row.name == op.name &&
                    (op.eventId == null || op.eventId in row.eventIds)
            }) { "No s'ha pogut identificar la llista creada al servidor." }
            "rename_list" -> check(old?.owner == true && current?.owner == true &&
                current.name == op.name) { "El nom no s'ha confirmat." }
            "delete_list", "leave_list" -> check(old != null && current == null) {
                "La llista encara apareix al servidor."
            }
            "list_event" -> check(current?.owner == true && op.eventId != null &&
                (op.eventId in current.eventIds) == (op.add == true)) {
                "El canvi d'esdeveniment no s'ha confirmat."
            }
            "share_list" -> {
                val url = response["invite_url"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val code = response["share_code"]?.jsonPrimitive?.contentOrNull.orEmpty()
                check(current?.owner == true && code.isNotBlank() && url.isNotBlank() &&
                    current.shareCode == code && current.inviteUrl == url &&
                    IosSavedListInvitation.parse(url) != null) {
                    "El servidor no ha confirmat el codi i l'enllaç compartits."
                }
                shared = IosSavedListShare(url, code)
            }
            "join_list" -> {
                // PHP returns only a message, not the target id. An already joined user
                // legitimately receives no new list. Require a stable or expanded snapshot.
                val beforeIds = before.map { it.id }.toSet()
                check(beforeIds.all { id -> after.any { it.id == id } }) {
                    "La lectura posterior de llistes és incoherent."
                }
                // Success is established by PHP's membership transaction and ok=true.
                // Without target id in the response, pre-existing membership cannot be
                // independently linked to the supplied invite token.
            }
            "revoke_list_share" -> check(current?.owner == true &&
                current.shareCode.isBlank() && current.inviteUrl.isBlank()) {
                "Les invitacions encara no consten revocades."
            }
            else -> error("Acció desconeguda")
        }
        return IosVerifiedListChange(after, shared)
    }
}
