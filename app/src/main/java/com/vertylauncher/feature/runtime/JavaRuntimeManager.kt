package com.vertylauncher.feature.runtime

import android.content.Context
import com.vertylauncher.feature.download.DownloadManager
import com.vertylauncher.feature.download.DownloadProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class JavaRuntimeManager(
    private val context: Context,
    private val downloadManager: DownloadManager
) {

    companion object {
        const val RUNTIME_DIR = "jre"
        val RUNTIME_DOWNLOAD_URLS = mapOf(
            8 to "https://github.com/PojavLauncherTeam/android-openjdk-build/releases/download/jre8-arm64-202309/jre8-arm64-202309.tar.xz",
            17 to "https://github.com/PojavLauncherTeam/android-openjdk-build/releases/download/jre17-arm64-202309/jre17-arm64-202309.tar.xz",
            21 to "https://github.com/PojavLauncherTeam/android-openjdk-build/releases/download/jre21-arm64-202309/jre21-arm64-202309.tar.xz"
        )
    }

    private val runtimeRootDir: File
        get() = File(context.filesDir, RUNTIME_DIR).apply { mkdirs() }

    fun getRequiredJavaVersion(mcVersion: String): Int {
        return when {
            mcVersion >= "1.21" -> 21
            mcVersion >= "1.18" -> 17
            mcVersion >= "1.17" -> 17
            else -> 8
        }
    }

    suspend fun getJavaHome(version: Int, onProgress: (DownloadProgress) -> Unit): Result<File> = withContext(Dispatchers.IO) {
        val javaDir = File(runtimeRootDir, "java$version")
        if (javaDir.exists() && javaDir.resolve("bin/java").exists()) {
            return@withContext Result.success(javaDir)
        }

        val url = RUNTIME_DOWNLOAD_URLS[version]
            ?: return@withContext Result.failure(IllegalArgumentException("Unsupported Java version: $version"))

        try {
            val downloadDir = File(runtimeRootDir, "downloads").apply { mkdirs() }
            val tarFile = File(downloadDir, "java$version.tar.xz")

            downloadManager.downloadFile(url, tarFile) { downloaded, total ->
                onProgress(DownloadProgress(downloaded, total, if (total > 0) (downloaded * 100 / total).toInt() else 0))
            }.getOrThrow()

            extractTarXz(tarFile, javaDir)
            makeExecutable(javaDir)
            tarFile.delete()

            Result.success(javaDir)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractTarXz(tarFile: File, destDir: File) {
        destDir.mkdirs()
        try {
            val process = Runtime.getRuntime().exec(arrayOf("tar", "xf", tarFile.absolutePath, "-C", destDir.absolutePath))
            process.waitFor()
        } catch (e: Exception) {
            tarFile.copyTo(File(destDir, "runtime.tar.xz"), overwrite = true)
        }
    }

    private fun makeExecutable(javaDir: File) {
        File(javaDir, "bin/java").apply { if (exists()) setExecutable(true, false) }
        File(javaDir, "bin/javac").apply { if (exists()) setExecutable(true, false) }
    }

    fun getJavaExecutable(version: Int): File = File(runtimeRootDir, "java$version/bin/java")
    fun isRuntimeAvailable(version: Int): Boolean = File(runtimeRootDir, "java$version/bin/java").exists()
}
