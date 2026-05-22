package com.vertylauncher.feature.modloader

import android.content.Context
import com.vertylauncher.feature.download.DownloadManager
import com.vertylauncher.feature.version.VersionJson
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.util.zip.ZipInputStream

class ForgeInstaller(
    private val context: Context,
    private val downloadManager: DownloadManager
) : ModLoaderInstaller {
    override val name = "Forge"
    override val id = "forge"

    private val client = HttpClient(Android)
    private val cacheDir = File(context.cacheDir, "forge_installers").apply { mkdirs() }

    companion object {
        const val FORGE_MAVEN = "https://maven.minecraftforge.net"
        const val NEOFORGE_MAVEN = "https://maven.neoforged.net/releases"
    }

    override suspend fun fetchVersions(mcVersion: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val url = if (mcVersion >= "1.20.1") {
                "$NEOFORGE_MAVEN/net/neoforged/neoforge/maven-metadata.xml"
            } else {
                "$FORGE_MAVEN/net/minecraftforge/forge/maven-metadata.xml"
            }
            val response = client.get(url).bodyAsText()
            val versions = parseMavenMetadata(response, mcVersion)
            Result.success(versions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun install(gameDir: File, versionJson: VersionJson, loaderVersion: String): Result<VersionJson> = withContext(Dispatchers.IO) {
        try {
            val isNeoForge = loaderVersion.contains("neoforge") || versionJson.id >= "1.20.1"
            val installProfile = downloadInstallProfile(loaderVersion, isNeoForge)
            val profileJson = Json.parseToJsonElement(installProfile).jsonObject

            val versionInfo = profileJson["versionInfo"]?.jsonObject
                ?: profileJson["install"]?.jsonObject
                ?: return@withContext Result.failure(Exception("Invalid install profile"))

            val mainClass = versionInfo["mainClass"]?.jsonPrimitive?.content
                ?: "net.minecraft.launchwrapper.Launch"

            val forgeLibraries = parseLibraries(versionInfo["libraries"]?.jsonArray)
            val mergedLibraries = versionJson.libraries.toMutableList()
            mergedLibraries.addAll(forgeLibraries)

            val modifiedVersionJson = versionJson.copy(
                id = if (isNeoForge) "neoforge-$loaderVersion" else "forge-$loaderVersion",
                mainClass = mainClass,
                libraries = mergedLibraries,
                minecraftArguments = versionInfo["minecraftArguments"]?.jsonPrimitive?.content
                    ?: versionJson.minecraftArguments
            )

            Result.success(modifiedVersionJson)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun downloadInstallProfile(version: String, isNeoForge: Boolean): String {
        val url = if (isNeoForge) {
            "$NEOFORGE_MAVEN/net/neoforged/neoforge/$version/neoforge-$version-installer.jar"
        } else {
            "$FORGE_MAVEN/net/minecraftforge/forge/$version/forge-$version-installer.jar"
        }

        val jarFile = File(cacheDir, "forge-$version-installer.jar")
        downloadManager.downloadFile(url, jarFile).getOrThrow()

        ZipInputStream(jarFile.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (entry.name == "install_profile.json" || entry.name == "version.json") {
                    return zis.bufferedReader().use { it.readText() }
                }
                entry = zis.nextEntry
            }
        }
        throw IllegalStateException("Install profile not found in installer JAR")
    }

    private fun parseMavenMetadata(xml: String, mcVersion: String): List<String> {
        return xml.split("<version>")
            .drop(1)
            .map { it.substringBefore("</version>") }
            .filter { it.startsWith(mcVersion) || it.contains(mcVersion.replace(".", "_")) }
    }

    private fun parseLibraries(array: kotlinx.serialization.json.JsonArray?): List<VersionJson.Library> {
        if (array == null) return emptyList()
        return array.map { element ->
            val obj = element.jsonObject
            val name = obj["name"]?.jsonPrimitive?.content ?: ""
            val url = obj["url"]?.jsonPrimitive?.content ?: "https://libraries.minecraft.net/"
            val downloads = if (obj["downloads"] != null) {
                Json.decodeFromString(VersionJson.LibraryDownloads.serializer(), obj["downloads"]!!.toString())
            } else {
                val parts = name.split(":")
                val path = if (parts.size >= 3) {
                    parts[0].replace(".", "/") + "/" + parts[1] + "/" + parts[2] + "/" + parts[1] + "-" + parts[2] + ".jar"
                } else ""
                VersionJson.LibraryDownloads(
                    artifact = VersionJson.Artifact(path = path, sha1 = "", size = 0, url = url)
                )
            }
            VersionJson.Library(downloads = downloads, name = name)
        }
    }
}
