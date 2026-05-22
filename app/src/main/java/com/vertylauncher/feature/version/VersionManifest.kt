package com.vertylauncher.feature.version

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class VersionManifest(
    val latest: LatestVersions,
    val versions: List<VersionInfo>
) {
    @Serializable
    data class LatestVersions(
        val release: String,
        val snapshot: String
    )
}

@Serializable
data class VersionInfo(
    val id: String,
    val type: String,
    val url: String,
    val time: String,
    val releaseTime: String
)

@Serializable
data class VersionJson(
    val id: String,
    val type: String,
    val assets: String,
    val assetIndex: AssetIndex,
    val downloads: Downloads,
    val libraries: List<Library>,
    val mainClass: String,
    val minecraftArguments: String? = null,
    val arguments: Arguments? = null,
    val javaVersion: JavaVersion? = null,
    val logging: JsonObject? = null
) {
    @Serializable
    data class AssetIndex(
        val id: String,
        val sha1: String,
        val size: Long,
        val totalSize: Long,
        val url: String
    )

    @Serializable
    data class Downloads(
        val client: DownloadInfo,
        val client_mappings: DownloadInfo? = null,
        val server: DownloadInfo? = null,
        val server_mappings: DownloadInfo? = null
    )

    @Serializable
    data class DownloadInfo(
        val sha1: String,
        val size: Long,
        val url: String
    )

    @Serializable
    data class Library(
        val downloads: LibraryDownloads,
        val name: String,
        val rules: List<Rule>? = null,
        val natives: Map<String, String>? = null,
        val extract: Extract? = null
    )

    @Serializable
    data class LibraryDownloads(
        val artifact: Artifact? = null,
        val classifiers: Map<String, Artifact>? = null
    )

    @Serializable
    data class Artifact(
        val path: String,
        val sha1: String,
        val size: Long,
        val url: String
    )

    @Serializable
    data class Rule(
        val action: String,
        val os: OsRule? = null
    )

    @Serializable
    data class OsRule(
        val name: String? = null,
        val arch: String? = null
    )

    @Serializable
    data class Extract(
        val exclude: List<String>? = null
    )

    @Serializable
    data class Arguments(
        val game: List<ArgumentElement> = emptyList(),
        val jvm: List<ArgumentElement> = emptyList()
    )

    @Serializable
    data class ArgumentElement(
        val rules: List<Rule>? = null,
        val value: JsonElement
    )

    @Serializable
    data class JavaVersion(
        val component: String,
        val majorVersion: Int
    )
}
