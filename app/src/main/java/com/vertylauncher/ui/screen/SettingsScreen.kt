package com.vertylauncher.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val viewModel: SettingsViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Text("←")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.username,
                onValueChange = { viewModel.updateUsername(it) },
                label = { Text("Никнейм") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Рендерер", style = MaterialTheme.typography.titleMedium)
            listOf("angle" to "ANGLE (Vulkan)", "zink" to "Zink (Vulkan)", "virgl" to "VirGL").forEach { (id, name) ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(name)
                    RadioButton(selected = uiState.renderer == id, onClick = { viewModel.updateRenderer(id) })
                }
            }

            Divider()
            Text("Память: ${uiState.maxMemory} МБ", style = MaterialTheme.typography.titleMedium)
            Slider(
                value = uiState.maxMemory.toFloat(),
                onValueChange = { viewModel.updateMaxMemory(it.toInt()) },
                valueRange = 512f..4096f,
                steps = 7
            )
        }
    }
}
