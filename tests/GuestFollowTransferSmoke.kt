package com.example.agendavalenciana.ios

// Compile this standalone test with IosGuestFollowTransfer.kt: does not change Android.
data class IosFollow(val type: String, val key: String, val label: String)

fun main() {
    var checks = 0
    fun verify(condition: Boolean, name: String) { check(condition) { name }; checks++ }
    val storage = mutableMapOf<String,String>()
    val transfer = IosGuestFollowTransfer({ storage[it].orEmpty() }, { key,value -> storage[key] = value })
    val accent = IosFollow("ARTIST", "La Maria", "La Maria\nCantant")
    val category = IosFollow("CATEGORY", "MUSIC__TRADICIONAL", "Música · tradicional")
    val existing = IosFollow("ARTIST", "ZOO", "ZOO")
    transfer.setGuest(listOf(accent, category, accent))
    verify(transfer.guest().size == 2, "Guests deduplicate repeated follows")
    verify(transfer.guest().first() == accent, "Accent/newline codec round-trip")
    verify(IosGuestFollowTransfer.decode("v1\n4:NOT") .isEmpty(), "Corrupt journal ignored")
    val staged = transfer.stage(71)
    verify(staged.size == 2 && transfer.guest().isEmpty(), "Guest clears only after durable journal")
    verify(transfer.pending(71).size == 2, "Journal persists additions")
    verify(transfer.preview(71,listOf(existing,accent)).size == 3, "Account ∪ guest without duplicates")
    verify(transfer.missing(71,listOf(existing,accent)) == listOf(category), "Only missing categories sent")
    transfer.acknowledge(71,accent)
    verify(transfer.pending(71) == listOf(category), "Ack only removes confirmed follow")
    val restarted = IosGuestFollowTransfer({ storage[it].orEmpty() }, { k,v -> storage[k] = v })
    verify(restarted.pending(71).size == 1, "Pending survives app restart")
    verify(restarted.pending(72).isEmpty(), "Per-account isolation")
    restarted.setGuest(listOf(IosFollow("ARTIST","Oques Grasses","Oques Grasses")))
    restarted.stage(72)
    verify(restarted.pending(72).size == 1 && restarted.pending(71).size == 1, "Second account does not steal pending first account")
    restarted.acknowledge(71,category)
    verify(restarted.pending(71).isEmpty(), "Only successful server acknowledgements clear journal")
    verify(IosGuestFollowTransfer.union(listOf(existing),listOf(existing)).size == 1, "Server follows take precedence")
    val a = IosFollow("CATEGORY","TALKS__CIENCIA_TECNOLOGIA","Ciència i tecnologia")
    val b = IosFollow("CATEGORY","WORKSHOPS__CIENCIA_TECNOLOGIA","Ciència i tecnologia")
    verify(IosGuestFollowTransfer.union(listOf(a),listOf(b)).size == 2,
        "Different category codes with identical labels both survive")
    val failedWrites = mutableMapOf<String,String>()
    val broken = IosGuestFollowTransfer({ failedWrites[it].orEmpty() }, { k,v ->
        if (!k.startsWith("ios_pending")) failedWrites[k] = v
    })
    broken.setGuest(listOf(accent)); broken.stage(91)
    verify(broken.guest().size == 1, "Guest retained if journal cannot be read back")
    println("OK: $checks guest-follow transfer tests")
}
