package com.example.agendavalenciana.ios

import kotlinx.serialization.json.JsonObject

/** Align with ServerApiRepository.parseSpecialEvent on Android. */
object IosFestivalScheduleDecoder {
    fun decode(root: JsonObject): List<AgendaScheduleEntry> = root.items("schedule")
        .filter { row -> row.text("active").let { it.isBlank() || it == "1" || it.equals("true", true) } }
        .map { row ->
            val date = row.text("date", "schedule_date", "start_at").take(10)
            val time = row.text("start_time", "time").ifBlank {
                row.text("start_at").substringAfter(' ', "").take(5)
            }.take(5)
            AgendaScheduleEntry(date, time, row.text("title"), row.text("artist_name"),
                row.text("stage", "location"), groupName = row.text("group_name"),
                itemType = row.text("item_type").ifBlank { "performance" },
                sortOrder = row.text("sort_order").toIntOrNull() ?: 0,
                endTime = row.text("end_time", "end_at").takeLast(8).take(5),
                details = row.text("details"))
        }.filter { it.date.isNotBlank() && (it.artist.isNotBlank() || it.title.isNotBlank()) }
        .distinctBy { listOf(it.date, it.time, it.artist, it.title, it.location) }
        .sortedWith(compareBy<AgendaScheduleEntry> { it.date }.thenBy { it.time })
}
