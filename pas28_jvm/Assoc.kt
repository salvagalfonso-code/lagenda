package com.example.agendavalenciana.ios
fun assoc(remote: List<IosArtistBell>): List<IosArtistBell> {
 val projected = remote.associateByTo(linkedMapOf()) { IosArtistBellPolicy.key(it.artistName) }
 projected["x"] = IosArtistBell("x","WEEKLY",true)
 return projected.values.toList()
}
