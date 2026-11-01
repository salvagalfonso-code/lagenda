package com.example.agendavalenciana.ios

import kotlinx.serialization.json.*
import kotlin.test.*

/** Contract tests; requires running commonTest with the Gradle toolchain. */
class IosSavedListContractTest {
    private fun list(id: Int = 21, name: String = "Teatre", owner: Boolean = true,
        events: Set<Int> = setOf(51), url: String = "", code: String = "") =
        IosSavedList(id, name, owner, url, code, events)
    private fun result(vararg entries: IosSavedList): JsonObject = buildJsonObject {
        put("ok", true)
        put("lists", buildJsonArray { entries.forEach { row -> add(buildJsonObject {
            put("id", row.id); put("name", row.name); put("owner", row.owner)
            put("invite_url", row.inviteUrl); put("share_code", row.shareCode)
            put("event_ids", buildJsonArray { row.eventIds.forEach { add(it) } })
        }) } })
    }
    private val ok = buildJsonObject { put("ok", true) }
    @Test fun guestAndRegisteredUseSameListIds() {
        val own = IosSavedListContract.decode(result(list())).single()
        assertEquals(21, own.id)
        assertTrue(own.owner)
        assertFalse(IosSavedListContract.decode(result(list(owner=false))).single().owner)
    }
    @Test fun rejectsCorruptOrDuplicateSnapshots() {
        assertFails { IosSavedListContract.decode(ok) }
        assertFails { IosSavedListContract.decode(result(list(),list(name="Duplicada"))) }
    }
    @Test fun onlyOwnerCanEditOrShare() {
        val foreign = listOf(list(owner=false))
        listOf("rename_list","delete_list","share_list","list_event","revoke_list_share").forEach { action ->
            assertFails { IosSavedListContract.preflight(foreign, IosSavedListCommand(action,listId=21)) }
        }
        assertFails { IosSavedListContract.preflight(listOf(list()), IosSavedListCommand("leave_list",listId=21)) }
    }
    @Test fun createRenameDeleteAndEventsRequireReadback() {
        val original = list()
        val added = list(id=22,name="Dansa",events=setOf(55))
        IosSavedListContract.verify(listOf(original),listOf(original,added),
            IosSavedListCommand("create_list",name="Dansa",eventId=55),ok)
        assertFails { IosSavedListContract.verify(listOf(original),listOf(original),
            IosSavedListCommand("create_list",name="Teatre"),ok) }
        IosSavedListContract.verify(listOf(original),listOf(original.copy(name="Nova")),
            IosSavedListCommand("rename_list",listId=21,name="Nova"),ok)
        assertFails { IosSavedListContract.verify(listOf(original),listOf(original),
            IosSavedListCommand("rename_list",listId=21,name="Nova"),ok) }
        IosSavedListContract.verify(listOf(original),emptyList(),IosSavedListCommand("delete_list",listId=21),ok)
        IosSavedListContract.verify(listOf(original),listOf(original.copy(eventIds=setOf(51,55))),
            IosSavedListCommand("list_event",listId=21,eventId=55,add=true),ok)
        assertFails { IosSavedListContract.verify(listOf(original),listOf(original),
            IosSavedListCommand("list_event",listId=21,eventId=55,add=true),ok) }
    }
    @Test fun shareRevokeAndLeaveAreServerOwned() {
        val before = list()
        val code = "ABCDEF1234"
        val url = "https://agendaval.com/app/lists.php?invite=${"a".repeat(48)}"
        val after = before.copy(shareCode=code,inviteUrl=url)
        val receipt = buildJsonObject { put("ok",true);put("invite_url",url);put("share_code",code) }
        assertEquals(code,IosSavedListContract.verify(listOf(before),listOf(after),
            IosSavedListCommand("share_list",listId=21),receipt).share?.shareCode)
        IosSavedListContract.verify(listOf(after),listOf(before),
            IosSavedListCommand("revoke_list_share",listId=21),ok)
        assertFails { IosSavedListContract.verify(listOf(after),listOf(after),
            IosSavedListCommand("revoke_list_share",listId=21),ok) }
        IosSavedListContract.verify(listOf(before.copy(owner=false)),emptyList(),
            IosSavedListCommand("leave_list",listId=21),ok)
    }
}
