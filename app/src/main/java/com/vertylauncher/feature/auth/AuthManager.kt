package com.vertylauncher.feature.auth

import android.content.Context
import com.vertylauncher.feature.auth.elyby.ElyByAuth
import com.vertylauncher.feature.auth.microsoft.MicrosoftAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthManager(
    private val context: Context,
    private val microsoftAuth: MicrosoftAuth,
    private val elyByAuth: ElyByAuth
) {

    fun createOfflineProfile(username: String, versionId: String, assetsIndex: String): AuthProfile {
        val offlineUuid = java.util.UUID.nameUUIDFromBytes("OfflinePlayer:$username".toByteArray()).toString()
        return AuthProfile(
            username = username,
            uuid = offlineUuid.replace("-", ""),
            accessToken = "0",
            userType = "legacy",
            versionId = versionId,
            assetsIndex = assetsIndex
        )
    }

    suspend fun loginMicrosoft(): Result<AuthProfile> = withContext(Dispatchers.IO) {
        microsoftAuth.authenticate()
    }

    suspend fun loginElyBy(username: String, password: String): Result<AuthProfile> = withContext(Dispatchers.IO) {
        elyByAuth.authenticate(username, password)
    }

    suspend fun refreshElyByToken(refreshToken: String): Result<AuthProfile> = withContext(Dispatchers.IO) {
        elyByAuth.refresh(refreshToken)
    }
}
