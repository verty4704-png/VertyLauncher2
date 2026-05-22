package com.vertylauncher.feature.version

import android.content.Context
import com.vertylauncher.feature.auth.AuthProfile
import com.vertylauncher.feature.download.DownloadManager
import com.vertylauncher.feature.download.DownloadProgress
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

class VersionManager(
    private val context: Context,
    private val downloadManager: DownloadManager
) {

    companion object {
        const val VERSION_MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
        const val ASSETS_URL = "https://resources.download.minecraft.net"
        const val LIBRARIES_URL = "https://libraries.minecraft.net"
    }

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
    }

    private val versionsDir: File
        get() = File(context.getExternalFilesDir(null), "versions").apply { mkdirs() }

    private val librariesDir: File
        get() = File(context.getExternalFilesDir(null), "libraries").apply { mkdirs() }

    private val assetsDir: File
        get() = File(context.getExternalFilesDir(null), "assets").apply { mkdirs() }

    suspend fun fetchVersionManifest(): Result<VersionManifest> = withContext(Dispatchers.IO) {
        try {
            val manifest: VersionManifest = client.get(VERSION_MANIFEST_URL).body()
            Result.success(manifest)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchVersionJson(versionInfo: VersionInfo): Result<VersionJson> = withContext(Dispatchers.IO) {
        try {
            val versionJson: VersionJson = client.get(versionInfo.url).body()
            saveVersionJson(versionJson)
            Result.success(versionJson)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadClientJar(versionJson: VersionJson, onProgress: (DownloadProgress) -> Unit): Result<File> {
        val dest = getVersionJarFile(versionJson.id)
        return downloadManager.downloadFile(versionJson.downloads.client.url, dest) { downloaded, total ->
            onProgress(DownloadProgress(downloaded, total, if (total > 0) (downloaded * 100 / total).toInt() else 0))
        }
    }

    suspend fun downloadAssetIndex(versionJson: VersionJson, onProgress: (DownloadProgress) -> Unit): Result<File> {
        val dest = File(assetsDir, "indexes/${versionJson.assetIndex.id}.json")
        return downloadManager.downloadFile(versionJson.assetIndex.url, dest) { d, t ->
            onProgress(DownloadProgress(d, t, if (t > 0) (d * 100 / t).toInt() else 0))
        }
    }

    fun getVersionJsonFile(versionId: String): File = File(versionsDir, "$versionId/$versionId.json")
    fun getVersionJarFile(versionId: String): File = File(versionsDir, "$versionId/$versionId.jar")
    fun getLibraryFile(path: String): File = File(librariesDir, path)
    fun getAssetFile(hash: String): File {
        val prefix = hash.take(2)
        return File(assetsDir, "objects/$prefix/$hash")
    }

    private fun saveVersionJson(versionJson: VersionJson) {
        val file = getVersionJsonFile(versionJson.id)
        file.parentFile?.mkdirs()
        file.writeText(Json.encodeToString(VersionJson.serializer(), versionJson))
    }

    fun getClassPath(versionJson: VersionJson): String {
        val classpath = StringBuilder()
        classpath.append(getVersionJarFile(versionJson.id).absolutePath)

        versionJson.libraries.forEach { library ->
            if (isLibraryAllowed(library)) {
                library.downloads.artifact?.let { artifact ->
                    val libFile = getLibraryFile(artifact.path)
                    if (libFile.exists()) {
                        classpath.append(":${libFile.absolutePath}")
                    }
                }
            }
        }
        return classpath.toString()
    }

    private fun isLibraryAllowed(library: VersionJson.Library): Boolean {
        if (library.rules.isNullOrEmpty()) return true

        val currentOsName = "linux"
        var allowed = true
        var ruleMatched = false

        library.rules.forEach { rule ->
            val osMatch = when {
                rule.os == null -> true
                rule.os.name.equals(currentOsName, ignoreCase = true) -> true
                else -> false
            }

            if (osMatch) {
                ruleMatched = true
                allowed = rule.action == "allow"
            }
        }

        return if (ruleMatched) allowed else true
    }

    fun getGameArguments(versionJson: VersionJson, authProfile: AuthProfile, gameDir: File,
                         width: Int, height: Int): List<String> {
        val args = mutableListOf<String>()

        if (versionJson.minecraftArguments != null) {
            args.addAll(parseLegacyArguments(versionJson.minecraftArguments, authProfile, gameDir, width, height))
        } else {
            versionJson.arguments?.game?.forEach { arg ->
                val values = parseArgumentValue(arg.value)
                values.forEach { v ->
                    args.add(interpolateArgument(v, authProfile, gameDir, width, height))
                }
            }
        }
        return args
    }

    fun getJvmArguments(versionJson: VersionJson, nativesDir: File): List<String> {
        val args = mutableListOf<String>()
        versionJson.arguments?.jvm?.forEach { arg ->
            val values = parseArgumentValue(arg.value)
            values.forEach { v ->
                when {
                    v.contains("\${natives_directory}") -> args.add(v.replace("\${natives_directory}", nativesDir.absolutePath))
                    v.contains("\${launcher_name}") -> args.add(v.replace("\${launcher_name}", "vertylauncher"))
                    v.contains("\${launcher_version}") -> args.add(v.replace("\${launcher_version}", "1.0.0"))
                    else -> args.add(v)
                }
            }
        }
        return args
    }

    private fun parseLegacyArguments(args: String, authProfile: AuthProfile, gameDir: File,
                                     width: Int, height: Int): List<String> {
        return args.split(" ").map { interpolateArgument(it, authProfile, gameDir, width, height) }
    }

    private fun parseArgumentValue(element: JsonElement): List<String> {
        return when {
            element is JsonPrimitive -> listOf(element.content)
            else -> element.jsonArray.map { it.jsonPrimitive.content }
        }
    }

    private fun interpolateArgument(arg: String, authProfile: AuthProfile, gameDir: File,
                                    width: Int, height: Int): String {
        return arg
            .replace("\${auth_player_name}", authProfile.username)
            .replace("\${version_name}", authProfile.versionId)
            .replace("\${game_directory}", gameDir.absolutePath)
            .replace("\${assets_root}", assetsDir.absolutePath)
            .replace("\${assets_index_name}", authProfile.assetsIndex)
            .replace("\${auth_uuid}", authProfile.uuid)
            .replace("\${auth_access_token}", authProfile.accessToken)
            .replace("\${user_type}", authProfile.userType)
            .replace("\${version_type}", "VertyLauncher")
            .replace("\${resolution_width}", width.toString())
            .replace("\${resolution_height}", height.toString())
    }
}
