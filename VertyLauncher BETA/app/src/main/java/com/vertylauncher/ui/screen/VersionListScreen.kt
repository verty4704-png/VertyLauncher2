package com.vertylauncher.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersionListScreen(navController: NavController) {
    val viewModel: VersionListViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Версии Minecraft") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Text("←")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadVersions() }) {
                        Text("↻")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                uiState.error != null -> Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Ошибка: ${uiState.error}", color = MaterialTheme.colorScheme.error)
                    Button(onClick = { viewModel.loadVersions() }) { Text("Повторить") }
                }
                else -> LazyColumn {
                    item {
                        if (uiState.latestRelease.isNotBlank()) {
                            ListItem(
                                headlineContent = { Text("Последний релиз: ${uiState.latestRelease}") },
                                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            )
                        }
                    }
                    items(uiState.versions) { version ->
                        ListItem(
                            headlineContent = { Text(version.id) },
                            supportingContent = { Text(version.type) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            trailingContent = {
                                Button(onClick = { viewModel.selectVersion(version.id) }) {
                                    Text("Выбрать")
                                }
                            }
                        )
                        Divider()
                    }
                }
            }
        }
    }
}
