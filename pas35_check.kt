import com.example.agendavalenciana.ios.IosAccountLifecyclePolicy

fun expectFailure(block:()->Unit) { var failed=false;try{block()}catch(_:IllegalArgumentException){failed=true}catch(_:IllegalStateException){failed=true};check(failed) }
fun main() {
    val p=IosAccountLifecyclePolicy
    p.requireLoginReceipt(1,"salva","a".repeat(64))
    p.requireLoginReceipt(123456,"x_2","0".repeat(64))
    expectFailure{p.requireLoginReceipt(0,"x","a".repeat(64))}
    expectFailure{p.requireLoginReceipt(-1,"x","a".repeat(64))}
    expectFailure{p.requireLoginReceipt(1," ","a".repeat(64))}
    expectFailure{p.requireLoginReceipt(1,"x","a".repeat(63))}
    expectFailure{p.requireLoginReceipt(1,"x","g".repeat(64))}
    expectFailure{p.requireLoginReceipt(1,"x","https://google.example")}
    p.requireSocialCredential("a".repeat(150)+"."+"b".repeat(150)+"."+"c".repeat(100),"nonce_secure_0123456789")
    expectFailure{p.requireSocialCredential("x".repeat(400),"nonce_secure_0123456789")}
    expectFailure{p.requireSocialCredential("a.b.c","nonce_secure_0123456789")}
    expectFailure{p.requireSocialCredential("a".repeat(50)+"."+"b".repeat(50)+"."+"c".repeat(50),"unsafe nonce")}
    expectFailure{p.requireSocialCredential("a".repeat(50)+"."+"b".repeat(50)+"."+"c".repeat(50),"short")}
    p.requireDeleteConfirmation("ELIMINAR",8)
    expectFailure{p.requireDeleteConfirmation("eliminar",8)}
    expectFailure{p.requireDeleteConfirmation("ELIMINAR",0)}
    p.requireDeleteReceipt(true,"DELETED")
    p.requireDeleteReceipt(true,"ANONYMIZED_LEGAL_RETENTION")
    expectFailure{p.requireDeleteReceipt(false,"DELETED")}
    expectFailure{p.requireDeleteReceipt(true,"")}
    expectFailure{p.requireDeleteReceipt(true,"UNKNOWN")}
    println("PAS35_POLICY_JVM_PASS 20 assertions")
}
