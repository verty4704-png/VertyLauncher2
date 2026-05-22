package com.vertylauncher.feature.auth.microsoft

import android.content.Context
import com.vertylauncher.feature.auth.AuthProfile
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class MicrosoftAuth(private val context: Context) {

    companion object {
        const val CLIENT_ID = "00000000402b5328"
        const val REDIRECT_URI = "https://login.live.com/oauth20_desktop.srf"
        const val AUTHORIZE_URL = "https://login.live.com/oauth20_authorize.srf"
        const val TOKEN_URL = "https://login.live.com/oauth20_token.srf"
        const val XBOX_AUTH_URL = "https://user.auth.xboxlive.com/user/authenticate"
        const val XSTS_AUTH_URL = "https://xsts.auth.xboxlive.com/xsts/authorize"
        const val MINECRAFT_AUTH_URL = "https://api.minecraftservices.com/authentication/login_with_xbox"
        const val MINECRAFT_PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile"
    }

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    fun getAuthorizationUrl(): String {
        return "$AUTHORIZE_URL?client_id=$CLIENT_ID&response_type=code&redirect_uri=$REDIRECT_URI&scope=XboxLive.signin%20offline_access&prompt=select_account"
    }

    suspend fun authenticateWithCode(code: String): Result<AuthProfile> {
        return try {
            val msToken = getMicrosoftToken(code)
            val xboxToken = getXboxToken(msToken.accessToken)
            val xstsToken = getXSTSToken(xboxToken.token, xboxToken.userHash)
            val mcToken = getMinecraftToken(xstsToken.userHash, xstsToken.token)
            val profile = getMinecraftProfile(mcToken.accessToken)

            Result.success(AuthProfile(
                username = profile.name,
                uuid = profile.id,
                accessToken = mcToken.accessToken,
                userType = "mojang",
                versionId = "",
                assetsIndex = ""
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun authenticate(): Result<AuthProfile> {
        return Result.failure(NotImplementedError("Use authenticateWithCode(code) after WebView OAuth flow"))
    }

    private suspend fun getMicrosoftToken(code: String): MicrosoftTokenResponse {
        return client.post(TOKEN_URL) {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(FormDataContent(Parameters.build {
                append("client_id", CLIENT_ID)
                append("code", code)
                append("grant_type", "authorization_code")
                append("redirect_uri", REDIRECT_URI)
                append("scope", "XboxLive.signin offline_access")
            }))
        }.body()
    }

    private suspend fun getXboxToken(accessToken: String): XboxTokenResponse {
        return client.post(XBOX_AUTH_URL) {
            contentType(ContentType.Application.Json)
            setBody(XboxAuthRequest(
                Properties = XboxAuthProperties(
                    AuthMethod = "RPS",
                    SiteName = "user.auth.xboxlive.com",
                    RpsTicket = "d=$accessToken"
                ),
                RelyingParty = "http://auth.xboxlive.com",
                TokenType = "JWT"
            ))
        }.body()
    }

    private suspend fun getXSTSToken(xboxToken: String, userHash: String): XSTSTokenResponse {
        return client.post(XSTS_AUTH_URL) {
            contentType(ContentType.Application.Json)
            setBody(XSTSAuthRequest(
                Properties = XSTSProperties(
                    SandboxId = "RETAIL",
                    UserTokens = listOf(xboxToken)
                ),
                RelyingParty = "rp://api.minecraftservices.com/",
                TokenType = "JWT"
            ))
        }.body()
    }

    private suspend fun getMinecraftToken(userHash: String, xstsToken: String): MinecraftTokenResponse {
        return client.post(MINECRAFT_AUTH_URL) {
            contentType(ContentType.Application.Json)
            setBody(MinecraftAuthRequest(identityToken = "XBL3.0 x=$userHash;$xstsToken"))
        }.body()
    }

    private suspend fun getMinecraftProfile(accessToken: String): MinecraftProfile {
        return client.get(MINECRAFT_PROFILE_URL) {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }.body()
    }

    @Serializable
    data class MicrosoftTokenResponse(
        @SerialName("access_token") val accessToken: String,
        @SerialName("refresh_token") val refreshToken: String,
        @SerialName("expires_in") val expiresIn: Int
    )

    @Serializable
    data class XboxAuthRequest(
        val Properties: XboxAuthProperties,
        val RelyingParty: String,
        val TokenType: String
    )

    @Serializable
    data class XboxAuthProperties(
        val AuthMethod: String,
        val SiteName: String,
        val RpsTicket: String
    )

    @Serializable
    data class XboxTokenResponse(
        @SerialName("Token") val token: String,
        @SerialName("DisplayClaims") val displayClaims: DisplayClaims
    ) {
        val userHash: String get() = displayClaims.xui.firstOrNull()?.uhs ?: ""
    }

    @Serializable
    data class DisplayClaims(val xui: List<XuiClaim>)

    @Serializable
    data class XuiClaim(val uhs: String)

    @Serializable
    data class XSTSAuthRequest(
        val Properties: XSTSProperties,
        val RelyingParty: String,
        val TokenType: String
    )

    @Serializable
    data class XSTSProperties(
        val SandboxId: String,
        val UserTokens: List<String>
    )

    @Serializable
    data class XSTSTokenResponse(
        @SerialName("Token") val token: String,
        @SerialName("DisplayClaims") val displayClaims: DisplayClaims
    ) {
        val userHash: String get() = displayClaims.xui.firstOrNull()?.uhs ?: ""
    }

    @Serializable
    data class MinecraftAuthRequest(val identityToken: String)

    @Serializable
    data class MinecraftTokenResponse(
        @SerialName("access_token") val accessToken: String,
        @SerialName("expires_in") val expiresIn: Int
    )

    @Serializable
    data class MinecraftProfile(
        val id: String,
        val name: String,
        val skins: List<Skin>? = null
    )

    @Serializable
    data class Skin(val id: String, val state: String, val url: String, val variant: String)
}
