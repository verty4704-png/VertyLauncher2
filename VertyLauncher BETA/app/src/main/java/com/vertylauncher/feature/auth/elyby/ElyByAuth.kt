package com.vertylauncher.feature.auth.elyby

import android.content.Context
import com.vertylauncher.feature.auth.AuthProfile
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class ElyByAuth(private val context: Context) {

    companion object {
        const val BASE_URL = "https://authserver.ely.by"
    }

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun authenticate(username: String, password: String): Result<AuthProfile> {
        return try {
            val authResponse = client.post("$BASE_URL/auth/authenticate") {
                contentType(ContentType.Application.Json)
                setBody(ElyByAuthRequest(
                    username = username,
                    password = password,
                    clientToken = getClientToken()
                ))
            }.body<ElyByAuthResponse>()

            Result.success(AuthProfile(
                username = authResponse.selectedProfile.name,
                uuid = authResponse.selectedProfile.id,
                accessToken = authResponse.accessToken,
                userType = "mojang",
                versionId = "",
                assetsIndex = "",
                refreshToken = authResponse.refreshToken
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refresh(refreshToken: String): Result<AuthProfile> {
        return try {
            val response = client.post("$BASE_URL/auth/refresh") {
                contentType(ContentType.Application.Json)
                setBody(ElyByRefreshRequest(
                    accessToken = "",
                    clientToken = getClientToken(),
                    refreshToken = refreshToken
                ))
            }.body<ElyByAuthResponse>()

            Result.success(AuthProfile(
                username = response.selectedProfile.name,
                uuid = response.selectedProfile.id,
                accessToken = response.accessToken,
                userType = "mojang",
                versionId = "",
                assetsIndex = "",
                refreshToken = response.refreshToken
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getClientToken(): String {
        return "vertylauncher-android-client"
    }

    @Serializable
    data class ElyByAuthRequest(
        val username: String,
        val password: String,
        @SerialName("clientToken") val clientToken: String,
        val requestUser: Boolean = true
    )

    @Serializable
    data class ElyByRefreshRequest(
        @SerialName("accessToken") val accessToken: String,
        @SerialName("clientToken") val clientToken: String,
        @SerialName("refreshToken") val refreshToken: String
    )

    @Serializable
    data class ElyByAuthResponse(
        @SerialName("accessToken") val accessToken: String,
        @SerialName("refreshToken") val refreshToken: String? = null,
        @SerialName("clientToken") val clientToken: String,
        val selectedProfile: ElyByProfile,
        val user: ElyByUser? = null
    )

    @Serializable
    data class ElyByProfile(val id: String, val name: String)

    @Serializable
    data class ElyByUser(
        val id: String,
        val username: String,
        val properties: List<ElyByProperty>? = null
    )

    @Serializable
    data class ElyByProperty(val name: String, val value: String)
}
