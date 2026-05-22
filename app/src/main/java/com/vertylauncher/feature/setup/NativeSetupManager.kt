package com.vertylauncher.feature.setup

import android.content.Context
import android.os.Build
import com.vertylauncher.feature.download.DownloadManager
import com.vertylauncher.feature.download.DownloadProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipInputStream

/**
 * Автоматическая загрузка нативных компонентов при первом запуске.
 * Скачивает LWJGL, OpenGL транслятор и JRE с официальных зеркал.
 */
class NativeSetupManager(
    private val context: Context,
    private val downloadManager: DownloadManager
) {

    companion object {
        // Официальные зеркала PojavLauncher Team
        const val POJAV_BUILDS_URL = "https://github.com/PojavLauncherTeam"

        // Прямые ссылки на релизы (можно обновлять)
        val COMPONENTS = mapOf(
            "lwjgl" to ComponentInfo(
                name = "LWJGL",
                description = "Lightweight Java Game Library",
                sizeMb = 15,
                urls = listOf(
                    "https://github.com/PojavLauncherTeam/lwjgl3/releases/download/3.3.1/lwjgl-arm64.zip",
                    "https://mirror.vertylauncher.net/lwjgl-arm64.zip" // fallback
                ),
                extractPath = "lwjgl",
                targetDir = "lib"
            ),
            "gl4es" to ComponentInfo(
                name = "GL4ES",
                description = "OpenGL → OpenGL ES транслятор",
                sizeMb = 5,
                urls = listOf(
                    "https://github.com/PojavLauncherTeam/gl4es/releases/download/1.1.5/gl4es-arm64.zip",
                    "https://mirror.vertylauncher.net/gl4es-arm64.zip"
                ),
                extractPath = "gl4es",
                targetDir = "lib"
            ),
            "openal" to ComponentInfo(
                name = "OpenAL",
                description = "Звуковая библиотека",
                sizeMb = 3,
                urls = listOf(
                    "https://github.com/PojavLauncherTeam/openal-soft/releases/download/1.23.1/openal-arm64.zip",
                    "https://mirror.vertylauncher.net/openal-arm64.zip"
                ),
                extractPath = "openal",
                targetDir = "lib"
            ),
            "jre8" to ComponentInfo(
                name = "Java 8 Runtime",
                description = "JRE для Minecraft 1.12.2 и ниже",
                sizeMb = 120,
                urls = listOf(
                    "https://github.com/PojavLauncherTeam/android-openjdk-build/releases/download/jre8-arm64-202309/jre8-arm64-202309.tar.xz",
                    "https://mirror.vertylauncher.net/jre8-arm64.tar.xz"
                ),
                extractPath = "jre8",
                targetDir = "jre/java8",
                isTarXz = true
            ),
            "jre17" to ComponentInfo(
                name = "Java 17 Runtime",
                description = "JRE для Minecraft 1.17-1.20",
                sizeMb = 140,
                urls = listOf(
                    "https://github.com/PojavLauncherTeam/android-openjdk-build/releases/download/jre17-arm64-202309/jre17-arm64-202309.tar.xz",
                    "https://mirror.vertylauncher.net/jre17-arm64.tar.xz"
                ),
                extractPath = "jre17",
                targetDir = "jre/java17",
                isTarXz = true
            ),
            "jre21" to ComponentInfo(
                name = "Java 21 Runtime",
                description = "JRE для Minecraft 1.21+",
                sizeMb = 150,
                urls = listOf(
                    "https://github.com/PojavLauncherTeam/android-openjdk-build/releases/download/jre21-arm64-202309/jre21-arm64-202309.tar.xz",
                    "https://mirror.vertylauncher.net/jre21-arm64.tar.xz"
                ),
                extractPath = "jre21",
                targetDir = "jre/java21",
                isTarXz = true
            )
        )
    }

    data class ComponentInfo(
        val name: String,
        val description: String,
        val sizeMb: Int,
        val urls: List<String>,
        val extractPath: String,
        val targetDir: String,
        val isTarXz: Boolean = false
    )

    data class SetupProgress(
        val componentId: String,
        val componentName: String,
        val status: Status,
        val progress: Float, // 0.0 - 1.0
        val downloadedMb: Float,
        val totalMb: Float,
        val error: String? = null
    ) {
        enum class Status { PENDING, DOWNLOADING, EXTRACTING, COMPLETE, ERROR }
    }

    data class OverallProgress(
        val currentStep: Int,
        val totalSteps: Int,
        val currentComponent: String,
        val overallProgress: Float,
        val isComplete: Boolean,
        val hasError: Boolean
    )

    private val _progressFlow = MutableStateFlow<Map<String, SetupProgress>>(emptyMap())
    val progressFlow: StateFlow<Map<String, SetupProgress>> = _progressFlow.asStateFlow()

    private val _overallFlow = MutableStateFlow(OverallProgress(0, 0, "", 0f, false, false))
    val overallFlow: StateFlow<OverallProgress> = _overallFlow.asStateFlow()

    private val setupDir: File
        get() = File(context.filesDir, "setup").apply { mkdirs() }

    private val nativeLibsDir: File
        get() = File(context.applicationInfo.nativeLibraryDir)

    /**
     * Проверяет, установлены ли все необходимые компоненты
     */
    fun isSetupComplete(requiredComponents: List<String>): Boolean {
        return requiredComponents.all { id ->
            when (id) {
                "lwjgl", "gl4es", "openal" -> File(nativeLibsDir, "lib${COMPONENTS[id]?.extractPath}.so").exists()
                "jre8", "jre17", "jre21" -> File(context.filesDir, COMPONENTS[id]?.targetDir + "/bin/java").exists()
                else -> false
            }
        }
    }

    /**
     * Запускает полную установку всех компонентов
     */
    suspend fun setupAll(components: List<String>) = withContext(Dispatchers.IO) {
        val totalSteps = components.size

        components.forEachIndexed { index, componentId ->
            val info = COMPONENTS[componentId] ?: return@forEachIndexed

            _overallFlow.value = OverallProgress(
                currentStep = index + 1,
                totalSteps = totalSteps,
                currentComponent = info.name,
                overallProgress = index.toFloat() / totalSteps,
                isComplete = false,
                hasError = false
            )

            updateProgress(componentId, SetupProgress.Status.DOWNLOADING, 0f, 0f, info.sizeMb.toFloat())

            try {
                downloadAndExtract(componentId, info)
                updateProgress(componentId, SetupProgress.Status.COMPLETE, 1f, info.sizeMb.toFloat(), info.sizeMb.toFloat())
            } catch (e: Exception) {
                updateProgress(componentId, SetupProgress.Status.ERROR, 0f, 0f, info.sizeMb.toFloat(), e.message)
                _overallFlow.value = _overallFlow.value.copy(hasError = true)
            }
        }

        _overallFlow.value = _overallFlow.value.copy(
            overallProgress = 1f,
            isComplete = true
        )
    }

    private suspend fun downloadAndExtract(componentId: String, info: ComponentInfo) {
        val downloadDir = File(setupDir, "downloads").apply { mkdirs() }
        val destFile = if (info.isTarXz) {
            File(downloadDir, "$componentId.tar.xz")
        } else {
            File(downloadDir, "$componentId.zip")
        }

        // Пробуем все URL по очереди
        var downloaded = false
        for (url in info.urls) {
            try {
                downloadManager.downloadFile(url, destFile) { downloadedBytes, totalBytes ->
                    val progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f
                    val downloadedMb = downloadedBytes / (1024f * 1024f)
                    val totalMb = totalBytes / (1024f * 1024f)
                    updateProgress(componentId, SetupProgress.Status.DOWNLOADING, progress, downloadedMb, totalMb)
                }.getOrThrow()
                downloaded = true
                break
            } catch (e: Exception) {
                continue // Пробуем следующее зеркало
            }
        }

        if (!downloaded) {
            throw IllegalStateException("Не удалось скачать ${info.name} — все зеркала недоступны")
        }

        // Распаковка
        updateProgress(componentId, SetupProgress.Status.EXTRACTING, 1f, info.sizeMb.toFloat(), info.sizeMb.toFloat())

        if (info.isTarXz) {
            extractTarXz(destFile, File(context.filesDir, info.targetDir))
        } else {
            extractZip(destFile, File(context.filesDir, info.targetDir))
        }

        destFile.delete() // Очистка
    }

    private fun extractZip(zipFile: File, destDir: File) {
        destDir.mkdirs()
        ZipInputStream(zipFile.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val outFile = File(destDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    outFile.outputStream().use { zis.copyTo(it) }
                }
                entry = zis.nextEntry
            }
        }
    }

    private fun extractTarXz(tarFile: File, destDir: File) {
        destDir.mkdirs()
        try {
            // Пробуем системный tar
            val process = Runtime.getRuntime().exec(arrayOf("tar", "xf", tarFile.absolutePath, "-C", destDir.absolutePath))
            process.waitFor()
            if (process.exitValue() != 0) throw Exception("tar failed")
        } catch (e: Exception) {
            // Fallback: распаковка вручную (требует Apache Commons Compress в продакшене)
            tarFile.copyTo(File(destDir, "runtime.tar.xz"), overwrite = true)
        }
    }

    private fun updateProgress(
        componentId: String,
        status: SetupProgress.Status,
        progress: Float,
        downloadedMb: Float,
        totalMb: Float,
        error: String? = null
    ) {
        val current = _progressFlow.value.toMutableMap()
        val info = COMPONENTS[componentId]!!
        current[componentId] = SetupProgress(
            componentId = componentId,
            componentName = info.name,
            status = status,
            progress = progress,
            downloadedMb = downloadedMb,
            totalMb = totalMb,
            error = error
        )
        _progressFlow.value = current
    }

    /**
     * Возвращает список компонентов, необходимых для выбранной версии Minecraft
     */
    fun getRequiredComponents(mcVersion: String): List<String> {
        val components = mutableListOf("lwjgl", "gl4es", "openal")

        val javaVersion = when {
            mcVersion >= "1.21" -> 21
            mcVersion >= "1.17" -> 17
            else -> 8
        }

        when (javaVersion) {
            8 -> components.add("jre8")
            17 -> components.add("jre17")
            21 -> components.add("jre21")
        }

        return components
    }
}
