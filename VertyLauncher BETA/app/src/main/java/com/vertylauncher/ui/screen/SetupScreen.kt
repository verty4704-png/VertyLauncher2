package com.vertylauncher.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vertylauncher.feature.setup.NativeSetupManager
import kotlinx.coroutines.launch
import org.koin.androidx.compose.get

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    versionId: String,
    onSetupComplete: () -> Unit
) {
    val setupManager: NativeSetupManager = get()
    val scope = rememberCoroutineScope()

    val progress by setupManager.progressFlow.collectAsState()
    val overall by setupManager.overallFlow.collectAsState()

    val requiredComponents = remember { setupManager.getRequiredComponents(versionId) }

    LaunchedEffect(Unit) {
        if (!setupManager.isSetupComplete(requiredComponents)) {
            scope.launch {
                setupManager.setupAll(requiredComponents)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Установка компонентов") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Overall progress
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Шаг ${overall.currentStep} из ${overall.totalSteps}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        overall.currentComponent,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { overall.overallProgress },
                        modifier = Modifier.fillMaxWidth().height(8.dp)
                    )
                    Text(
                        "${(overall.overallProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Components list
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(requiredComponents) { componentId ->
                    val componentProgress = progress[componentId]
                    ComponentCard(
                        componentId = componentId,
                        progress = componentProgress
                    )
                }
            }

            // Action buttons
            if (overall.isComplete) {
                Button(
                    onClick = onSetupComplete,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Начать игру")
                }
            } else if (overall.hasError) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                setupManager.setupAll(requiredComponents)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Повторить")
                    }
                    OutlinedButton(
                        onClick = onSetupComplete,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Пропустить")
                    }
                }
            }
        }
    }
}

@Composable
fun ComponentCard(
    componentId: String,
    progress: NativeSetupManager.SetupProgress?
) {
    val info = NativeSetupManager.COMPONENTS[componentId]

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (progress?.status) {
                NativeSetupManager.SetupProgress.Status.COMPLETE -> MaterialTheme.colorScheme.tertiaryContainer
                NativeSetupManager.SetupProgress.Status.ERROR -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        info?.name ?: componentId,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        info?.description ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusIcon(progress?.status)
            }

            if (progress != null && progress.status == NativeSetupManager.SetupProgress.Status.DOWNLOADING) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress.progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp)
                )
                Text(
                    "${progress.downloadedMb.toInt()} / ${progress.totalMb.toInt()} МБ",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (progress?.error != null) {
                Text(
                    progress.error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun StatusIcon(status: NativeSetupManager.SetupProgress.Status?) {
    when (status) {
        NativeSetupManager.SetupProgress.Status.COMPLETE -> Text("✓", color = MaterialTheme.colorScheme.primary)
        NativeSetupManager.SetupProgress.Status.ERROR -> Text("✗", color = MaterialTheme.colorScheme.error)
        NativeSetupManager.SetupProgress.Status.DOWNLOADING -> CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        NativeSetupManager.SetupProgress.Status.EXTRACTING -> CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        else -> Text("○", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
