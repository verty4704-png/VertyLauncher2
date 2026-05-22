package com.vertylauncher.ui.screen

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.vertylauncher.feature.game.GameActivity
import com.vertylauncher.feature.setup.NativeSetupManager
import kotlinx.coroutines.launch
import org.koin.androidx.compose.get
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val viewModel: HomeViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val setupManager: NativeSetupManager = get()
    val scope = rememberCoroutineScope()

    var showSetup by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("VertyLauncher") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        if (showSetup && uiState.selectedVersion.isNotBlank()) {
            SetupScreen(
                versionId = uiState.selectedVersion,
                onSetupComplete = {
                    showSetup = false
                    val intent = Intent(context, GameActivity::class.java).apply {
                        putExtra("version_id", uiState.selectedVersion)
                        putExtra("username", uiState.username)
                    }
                    context.startActivity(intent)
                }
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
            ) {
                Text("Добро пожаловать, ${uiState.username}", style = MaterialTheme.typography.headlineMedium)
                if (uiState.selectedVersion.isNotBlank()) {
                    Text("Версия: ${uiState.selectedVersion}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                }

                Button(
                    onClick = {
                        if (uiState.selectedVersion.isBlank()) {
                            navController.navigate("versions")
                            return@Button
                        }

                        val required = setupManager.getRequiredComponents(uiState.selectedVersion)
                        if (!setupManager.isSetupComplete(required)) {
                            showSetup = true
                        } else {
                            val intent = Intent(context, GameActivity::class.java).apply {
                                putExtra("version_id", uiState.selectedVersion)
                                putExtra("username", uiState.username)
                            }
                            context.startActivity(intent)
                        }
                    },
                    enabled = uiState.canLaunch,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (uiState.canLaunch) "Играть" else "Выберите версию и ник")
                }

                OutlinedButton(onClick = { navController.navigate("versions") }, modifier = Modifier.fillMaxWidth()) { Text("Версии") }
                OutlinedButton(onClick = { navController.navigate("settings") }, modifier = Modifier.fillMaxWidth()) { Text("Настройки") }
                OutlinedButton(onClick = { navController.navigate("login") }, modifier = Modifier.fillMaxWidth()) { Text("Аккаунт") }
            }
        }
    }
}
