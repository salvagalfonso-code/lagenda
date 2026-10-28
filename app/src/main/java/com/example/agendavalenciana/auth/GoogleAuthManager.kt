package com.example.agendavalenciana.auth

import android.content.Context
import android.util.Base64
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.agendavalenciana.BuildConfig
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.security.SecureRandom

/**
 * Accés amb Google via Android Credential Manager.
 * L'ID token no es confia localment: s'envia a l'API d'Agenda perquè el servidor
 * valide la signatura de Google, audience, issuer, expiració i nonce.
 */
object GoogleAuthManager {
    data class GoogleToken(val idToken: String, val nonce: String)

    suspend fun signIn(context: Context): Result<GoogleToken> = runCatching {
        val clientId = BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()
        require(clientId.isNotBlank()) {
            "Falta configurar AGENDA_GOOGLE_WEB_CLIENT_ID en gradle.properties."
        }

        val nonce = secureNonce()
        val option = GetSignInWithGoogleOption.Builder(clientId)
            .setNonce(nonce)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        val result = CredentialManager.create(context).getCredential(
            context = context,
            request = request
        )
        val credential = result.credential
        require(
            credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) { "Google ha tornat una credencial no compatible." }

        val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
        GoogleToken(googleCredential.idToken, nonce)
    }

    private fun secureNonce(byteLength: Int = 32): String {
        val bytes = ByteArray(byteLength)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(
            bytes,
            Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING
        )
    }
}
