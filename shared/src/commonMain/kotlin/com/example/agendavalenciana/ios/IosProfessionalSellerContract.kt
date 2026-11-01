package com.example.agendavalenciana.ios

/** Pas 43: pagaments i dades fiscals només autoritzats des del servidor d'Android.
 * Un import brut NO és liquidació bancària ni factura legal.
 */
internal data class IosProCompany(
    val id: Int, val name: String, val taxId: String, val address: String,
    val postcode: String, val town: String, val province: String, val kind: String
)
internal data class IosProEvent(
    val id: Int, val title: String, val date: String, val town: String,
    val status: String, val enabled: Boolean, val companyId: Int?,
    val sold: Int, val orders: Int, val grossCents: Long
)
internal data class IosProSale(
    val id: Long, val eventId: Int, val title: String, val paidAt: String,
    val quantity: Int, val cents: Long, val optionName: String
)
internal data class IosProInvoice(
    val number: String, val company: String, val issuedAt: String,
    val cents: Long, val status: String
)
internal data class IosProDashboard(
    val companies: List<IosProCompany>, val events: List<IosProEvent>,
    val sales: List<IosProSale>, val invoices: List<IosProInvoice>,
    val sold: Int, val orders: Int, val grossCents: Long,
    val eventsTruncated: Boolean
)
internal data class IosProTicketOption(
    val id: Long, val name: String, val cents: Long, val capacity: Int?,
    val sold: Int, val reserved: Int, val maxPerOrder: Int,
    val date: String, val time: String, val sector: String, val offer: String,
    val starts: String, val ends: String, val active: Boolean
)
internal data class IosProTicketCatalog(
    val eventId: Int, val enabled: Boolean, val options: List<IosProTicketOption>
)
internal data class IosProTicketDraft(
    val id: Long, val name: String, val cents: Long, val capacity: Int?,
    val maxPerOrder: Int, val date: String, val time: String, val sector: String,
    val offer: String, val starts: String, val ends: String, val active: Boolean
)
internal object IosProfessionalSellerPolicy {
    val periods = setOf("all", "30d", "7d")
    fun validPeriod(period: String) = period in periods

    fun euros(cents: Long): String {
        // Impossible de representar abs(Long.MIN_VALUE) en Long; només xifres monetàries de servidor >=0.
        val absolute = if (cents == Long.MIN_VALUE) Long.MAX_VALUE else if (cents >= 0) cents else -cents
        return (if (cents < 0) "-" else "") +
            (absolute / 100).toString() + "," + (absolute % 100).toString().padStart(2, '0') + " €"
    }

    /** Sense coma flotant: 12,50 són sempre 1250 cèntims. */
    fun cents(text: String): Long? {
        val parts = text.trim().replace(',', '.').split('.')
        if (parts.size !in 1..2 || parts[0].isEmpty() || !parts[0].all(Char::isDigit) ||
            (parts.size == 2 && (parts[1].isEmpty() || parts[1].length > 2 || !parts[1].all(Char::isDigit)))) return null
        val whole = parts[0].toLongOrNull() ?: return null
        if (whole > 999_999_999L) return null
        return whole * 100 + (if (parts.size == 2) parts[1].padEnd(2, '0').toLong() else 0L)
    }
    private fun date(value: String): Boolean {
        if (!value.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) return false
        val year = value.take(4).toIntOrNull() ?: return false
        val month = value.substring(5, 7).toIntOrNull() ?: return false
        val day = value.substring(8, 10).toIntOrNull() ?: return false
        if (year < 1900 || month !in 1..12) return false
        val leap = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
        val lengths = listOf(31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        return day in 1..lengths[month - 1]
    }
    private fun time(time: String) = time.matches(Regex("\\d{2}:\\d{2}")) &&
        (time.substring(0, 2).toIntOrNull() ?: 24) < 24 && (time.substring(3, 5).toIntOrNull() ?: 60) < 60
    private fun stamp(value: String) = value.matches(Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")) &&
        time(value.substring(11, 16)) && value.substring(17, 19).toIntOrNull() in 0..59 && date(value.take(10))
    fun validOption(draft: IosProTicketDraft, before: IosProTicketOption? = null): Boolean =
        draft.id >= 0 && draft.name.trim().length in 1..180 && draft.cents in 0..99_999_999 &&
        draft.maxPerOrder in 1..20 && draft.capacity?.let { it > 0 && it.toLong() >= (before?.sold ?: 0).toLong() + (before?.reserved ?: 0).toLong() } != false &&
        (draft.date.isBlank() || date(draft.date)) && (draft.time.isBlank() || time(draft.time)) &&
        (draft.starts.isBlank() && draft.ends.isBlank() ||
            stamp(draft.starts) && stamp(draft.ends) && draft.starts < draft.ends) &&
        draft.sector.length <= 160 && draft.offer.length <= 190 &&
        (before == null || before.id == draft.id)
    fun matches(d: IosProTicketDraft, o: IosProTicketOption): Boolean =
        d.name.trim() == o.name && d.cents == o.cents && d.capacity == o.capacity &&
        d.maxPerOrder == o.maxPerOrder && d.date == o.date && d.time == o.time &&
        d.sector.trim() == o.sector && d.offer.trim() == o.offer &&
        d.starts == o.starts && d.ends == o.ends && d.active == o.active
    fun verifiedUpsert(before: IosProTicketCatalog, after: IosProTicketCatalog, draft: IosProTicketDraft,
                       acknowledgedId: Long = 0): Boolean {
        if (after.eventId != before.eventId || after.eventId <= 0) return false
        if (draft.id > 0) return after.options.count { it.id == draft.id && matches(draft, it) } == 1
        val existing = before.options.map { it.id }.toSet()
        return after.options.count { it.id > 0 && it.id !in existing &&
            (acknowledgedId == 0L || it.id == acknowledgedId) && matches(draft, it) } == 1
    }
    fun canAssign(dashboard: IosProDashboard, eventId: Int, companyId: Int): Boolean =
        dashboard.events.any { it.id == eventId } && dashboard.companies.any { it.id == companyId }
    fun confirmedAssignment(dashboard: IosProDashboard, eventId: Int, companyId: Int) =
        dashboard.events.count { it.id == eventId && it.companyId == companyId } == 1

    /** Només document extern ja emés; mai generar una factura a l'app. */
    fun canRegisterInvoice(dashboard: IosProDashboard, companyId: Int, number: String,
                           issued: String, amount: Long, confirmedExternal: Boolean): Boolean =
        confirmedExternal && dashboard.companies.any { it.id == companyId } &&
        number.trim().length in 1..80 && date(issued) && amount in 0..999_999_999_999L &&
        dashboard.invoices.none { it.number == number.trim() }
    fun confirmedInvoice(before: IosProDashboard, after: IosProDashboard, number: String): Boolean =
        before.invoices.none { it.number == number } && after.invoices.count { it.number == number } == 1

    fun validSummary(d: IosProDashboard): Boolean = d.sold >= 0 && d.orders >= 0 && d.grossCents >= 0 &&
        d.companies.all { it.id > 0 } && d.events.all { it.id > 0 && it.sold >= 0 && it.orders >= 0 && it.grossCents >= 0 } &&
        d.sales.all { it.id > 0 && it.quantity > 0 && it.cents >= 0 } &&
        d.invoices.all { it.number.isNotBlank() }

    fun reportCsv(d: IosProDashboard): String = buildString {
        append("ID;Esdeveniment;Data de cobrament;Tipus;Quantitat;Import EUR\n")
        fun quoted(value: String): String {
            // Avoid CSV formula injection when opened in spreadsheet software.
            val safe = if (value.trimStart().firstOrNull() in listOf('=', '+', '-', '@')) "'" + value else value
            return "\"" + safe.replace("\"", "\"\"") + "\""
        }
        d.sales.forEach { s ->
            append("${s.id};${quoted(s.title)};${quoted(s.paidAt)};${quoted(s.optionName)};${s.quantity};")
            append("${s.cents / 100},${(s.cents % 100).toString().padStart(2, '0')}\n")
        }
    }
}
