package com.vertylauncher.feature.auth

data class AuthProfile(
    val username: String,
    val uuid: String,
    val accessToken: String,
    val userType: String = "mojang",
    val versionId: String,
    val assetsIndex: String,
    val refreshToken: String? = null
)
