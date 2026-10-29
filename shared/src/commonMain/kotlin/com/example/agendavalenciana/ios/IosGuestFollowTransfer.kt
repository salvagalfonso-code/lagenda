package com.example.agendavalenciana.ios

/**
 * Durable, per-account journal for transferring follows created without a login.
 * Android merges account ∪ guest and posts only the missing remote follows. This
 * performs the same additive migration without ever replacing the account state.
 * The journal is written BEFORE the guest record is cleared. Failed posts stay
 * queued across launches and across account changes on the same device.
 */
class IosGuestFollowTransfer(
    private val read: (String) -> String,
    private val write: (String, String) -> Unit
) {
    companion object {
        const val GUEST_KEY = "ios_guest_follows_v350"
        fun pendingKey(accountId: Int) = "ios_pending_guest_follows_v350_$accountId"

        /** Length-prefixed fields allow names containing tabs, newlines and accents. */
        fun encode(items: List<IosFollow>): String = buildString {
            append("v1\n")
            items.forEach { item ->
                for (part in listOf(item.type, item.key, item.label)) {
                    append(part.length).append(':').append(part)
                }
            }
        }
        fun decode(raw: String): List<IosFollow> {
            if (!raw.startsWith("v1\n")) return emptyList()
            var offset = 3
            fun part(): String? {
                val begin = offset
                while (offset < raw.length && raw[offset] in '0'..'9' && offset - begin < 6) offset++
                if (offset == begin || offset >= raw.length || raw[offset] != ':') return null
                val length = raw.substring(begin, offset).toIntOrNull() ?: return null
                offset++
                if (length !in 0..65535 || length > raw.length - offset) return null
                val result = raw.substring(offset, offset + length)
                offset += length
                return result
            }
            val result = mutableListOf<IosFollow>()
            while (offset < raw.length) {
                val type = part() ?: return emptyList()
                val key = part() ?: return emptyList()
                val label = part() ?: return emptyList()
                result += IosFollow(type, key, label)
            }
            return union(emptyList(), result)
        }
        fun contains(items: List<IosFollow>, candidate: IosFollow): Boolean = items.any { existing ->
            existing.type.equals(candidate.type, ignoreCase = true) &&
                (existing.key.equals(candidate.key, ignoreCase = true) ||
                    // Subcategories can share the same display label in different disciplines.
                    // For CATEGORY only the canonical code identifies a follow.
                    (!candidate.type.equals("CATEGORY", ignoreCase = true) &&
                        (existing.key.equals(candidate.label, ignoreCase = true) ||
                            existing.label.equals(candidate.key, ignoreCase = true) ||
                            existing.label.equals(candidate.label, ignoreCase = true))))
        }
        /** The remote/account record is kept when the same entity exists twice. */
        fun union(account: List<IosFollow>, extra: List<IosFollow>): List<IosFollow> {
            val result = account.toMutableList()
            extra.forEach { item ->
                val type = item.type.trim().uppercase()
                val key = item.key.trim()
                if (type in setOf("ARTIST", "ORGANIZER", "VENUE", "CATEGORY") && key.isNotBlank()) {
                    val normalized = IosFollow(type, key, item.label.trim().ifBlank { key })
                    if (!contains(result, normalized)) result += normalized
                }
            }
            return result
        }
    }

    fun guest(): List<IosFollow> = decode(read(GUEST_KEY))
    fun pending(accountId: Int): List<IosFollow> = if (accountId > 0) decode(read(pendingKey(accountId))) else emptyList()

    fun setGuest(follows: List<IosFollow>) {
        write(GUEST_KEY, encode(union(emptyList(), follows)))
    }

    /** Record the transfer first; NEVER clear guest data until the journal is readable. */
    fun stage(accountId: Int, extraGuestCategories: List<IosFollow> = emptyList()): List<IosFollow> {
        require(accountId > 0)
        val newGuest = union(guest(), extraGuestCategories)
        if (newGuest.isEmpty()) return pending(accountId)
        val next = union(pending(accountId), newGuest)
        write(pendingKey(accountId), encode(next))
        // Native iOS caches must round-trip before deleting the guest record.
        if (!next.all { contains(pending(accountId), it) }) return next
        write(GUEST_KEY, encode(emptyList()))
        return next
    }

    fun preview(accountId: Int, server: List<IosFollow>): List<IosFollow> = union(server, pending(accountId))

    /** Only pending entities not present remotely need POST /user-state/follow.php. */
    fun missing(accountId: Int, remote: List<IosFollow>): List<IosFollow> =
        pending(accountId).filterNot { contains(remote, it) }

    fun acknowledge(accountId: Int, confirmed: IosFollow) {
        val remaining = pending(accountId).filterNot { contains(listOf(confirmed), it) }
        write(pendingKey(accountId), encode(remaining))
    }
}
