package com.vertylauncher.feature.modloader

import android.content.Context
import com.vertylauncher.feature.download.DownloadManager
import com.vertylauncher.feature.version.VersionJson
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

class FabricInstaller(
    private val context: Context,
    private val downloadManager: DownloadManager
) : ModLoaderInstaller {
    override val name = "Fabric"
    override val id = "fabric"

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    companion object {
        const val META_URL = "https://meta.fabricmc.net/v2"
    }

    override suspend fun fetchVersions(mcVersion: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val response: List<FabricLoaderVersion> = client.get("$META_URL/versions/loader/$mcVersion").body()
            Result.success(response.map { it.loader.version }.distinct())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun install(gameDir: File, versionJson: VersionJson, loaderVersion: String): Result<VersionJson> = withContext(Dispatchers.IO) {
        try {
            val mcVersion = versionJson.id
            val profileUrl = "$META_URL/versions/loader/$mcVersion/$loaderVersion/profile/json"
            val fabricProfile: FabricProfile = client.get(profileUrl).body()

            val mergedLibraries = versionJson.libraries.toMutableList()
            fabricProfile.libraries.forEach { lib ->
                val parts = lib.name.split(":")
                if (parts.size >= 3) {
                    val path = parts[0].replace(".", "/") + "/" + parts[1] + "/" + parts[2] + "/" + parts[1] + "-" + parts[2] + ".jar"
                    mergedLibraries.add(
                        VersionJson.Library(
                            downloads = VersionJson.LibraryDownloads(
                                artifact = VersionJson.Artifact(
                                    path = path,
                                    sha1 = "",
                                    size = 0,
                                    url = lib.url
                                )
                            ),
                            name = lib.name
                        )
                    )
                }
            }

            Result.success(versionJson.copy(
                id = "fabric-loader-$loaderVersion-$mcVersion",
                mainClass = fabricProfile.mainClass,
                libraries = mergedLibraries
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @Serializable
    data class FabricLoaderVersion(val loader: LoaderInfo, val intermediary: IntermediaryInfo)
    @Serializable
    data class LoaderInfo(val version: String, val stable: Boolean)
    @Serializable
    data class IntermediaryInfo(val version: String)
    @Serializable
    data class FabricProfile(val id: String, val mainClass: String, val libraries: List<FabricLibrary>)
    @Serializable
    data class FabricLibrary(val name: String, val url: String)
}
