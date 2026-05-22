package com.vertylauncher.feature.modloader

import com.vertylauncher.feature.version.VersionJson
import java.io.File

interface ModLoaderInstaller {
    val name: String
    val id: String
    suspend fun install(gameDir: File, versionJson: VersionJson, loaderVersion: String): Result<VersionJson>
    suspend fun fetchVersions(mcVersion: String): Result<List<String>>
}
