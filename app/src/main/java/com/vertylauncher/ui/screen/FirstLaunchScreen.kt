package com.vertylauncher.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vertylauncher.feature.download.DownloadProgress
import com.vertylauncher.feature.runtime.JavaRuntimeManager
import com.vertylauncher.feature.version.VersionManager
import kotlinx.coroutines.launch
import org.koin.androidx.compose.get

@Composable
fun FirstLaunchScreen(
    versionId: String,
    onComplete: () -> Unit
) {
    val versionManager: VersionManager = get()
    val runtimeManager: JavaRuntimeManager = get()
    val scope = rememberCoroutineScope()

    var currentStep by remember { mutableStateOf("") }
    var progress by remember { mutableStateOf(0f) }
    var isComplete by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                currentStep = "Загрузка Java Runtime..."
                val javaVersion = runtimeManager.getRequiredJavaVersion(versionId)
                runtimeManager.getJavaHome(javaVersion) { downloadProgress ->
                    progress = downloadProgress.percentage / 100f * 0.4f
                }.getOrThrow()

                currentStep = "Загрузка информации о версии..."
                progress = 0.4f
                val manifest = versionManager.fetchVersionManifest().getOrThrow()
                val versionInfo = manifest.versions.find { it.id == versionId }
                    ?: throw IllegalStateException("Version not found")
                val versionJson = versionManager.fetchVersionJson(versionInfo).getOrThrow()

                currentStep = "Загрузка Minecraft client.jar..."
                versionManager.downloadClientJar(versionJson) { downloadProgress ->
                    progress = 0.5f + (downloadProgress.percentage / 100f * 0.2f)
                }.getOrThrow()

                currentStep = "Загрузка asset index..."
                versionManager.downloadAssetIndex(versionJson) { downloadProgress ->
                    progress = 0.7f + (downloadProgress.percentage / 100f * 0.1f)
                }.getOrThrow()

                currentStep = "Готово!"
                progress = 1f
                isComplete = true
                onComplete()

            } catch (e: Exception) {
                error = e.message ?: "Unknown error"
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (error != null) {
            Text("Ошибка при подготовке", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.error)
            Text(error ?: "", style = MaterialTheme.typography.bodyMedium)
            Button(onClick = { onComplete() }) { Text("Пропустить") }
        } else {
            Text("Подготовка к первому запуску", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))
            Text(currentStep, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(24.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
            if (isComplete) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onComplete) { Text("Начать игру") }
            }
        }
    }
}
