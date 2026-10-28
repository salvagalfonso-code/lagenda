import com.example.agendavalenciana.model.IosAdminPermissions

fun main() {
    var n = 0
    fun checkCase(value: Boolean, name: String) { check(value) { name }; n++ }
    checkCase(IosAdminPermissions.canOpen("SUPER_ADMIN"), "admin enters")
    checkCase(!IosAdminPermissions.canOpen("PUBLIC"), "public denied")
    checkCase(!IosAdminPermissions.canOpen("PROMOTER"), "promoter denied")
    checkCase(!IosAdminPermissions.canOpen("super_admin"), "case-sensitive role")
    checkCase(IosAdminPermissions.canEdit(3,"SUPER_ADMIN",4), "admin changes another user")
    checkCase(!IosAdminPermissions.canEdit(3,"SUPER_ADMIN",3), "cannot edit self")
    checkCase(!IosAdminPermissions.canEdit(3,"ORGANIZER",4), "organizer denied")
    checkCase(!IosAdminPermissions.canEdit(0,"SUPER_ADMIN",4), "anonymous denied")
    checkCase(IosAdminPermissions.validRole("SUPER_ADMIN"), "accepted backend role")
    checkCase(!IosAdminPermissions.validRole("ROOT"), "unknown role denied")
    checkCase(IosAdminPermissions.validTarget(true, "", 0), "all recipients")
    checkCase(IosAdminPermissions.validTarget(false, "Lletra", 0), "artist targeting")
    checkCase(IosAdminPermissions.validTarget(false, "", 5), "saved event targeting")
    checkCase(!IosAdminPermissions.validTarget(false, "", 0), "empty targeting denied")
    checkCase(IosAdminPermissions.canConfirmRemoval(42,"Infracció identificada"), "moderation reason")
    checkCase(!IosAdminPermissions.canConfirmRemoval(42,"breu"), "short reason denied")
    checkCase(!IosAdminPermissions.canConfirmRemoval(0,"Infracció identificada"), "invalid event denied")
    println("OK: $n comprovacions superadmin")
}
