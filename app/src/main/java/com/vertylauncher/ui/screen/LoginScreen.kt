package com.vertylauncher.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.vertylauncher.feature.auth.AuthManager
import com.vertylauncher.feature.settings.SettingsRepository
import kotlinx.coroutines.launch
import org.koin.androidx.compose.get

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {
    val authManager: AuthManager = get()
    val settingsRepository: SettingsRepository = get()
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(0) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Аккаунт") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Text("←")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) { Text("Offline") }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) { Text("Ely.by") }
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) { Text("Microsoft") }
            }

            when (selectedTab) {
                0 -> OfflineTab(
                    username = username,
                    onUsernameChange = { username = it },
                    onLogin = {
                        scope.launch {
                            val profile = authManager.createOfflineProfile(username, "", "")
                            settingsRepository.updateUsername(profile.username)
                            navController.popBackStack()
                        }
                    }
                )
                1 -> ElyByTab(
                    username = username,
                    password = password,
                    onUsernameChange = { username = it },
                    onPasswordChange = { password = it },
                    isLoading = isLoading,
                    errorMessage = errorMessage,
                    onLogin = {
                        scope.launch {
                            isLoading = true
                            errorMessage = null
                            authManager.loginElyBy(username, password).fold(
                                onSuccess = { profile ->
                                    settingsRepository.updateUsername(profile.username)
                                    isLoading = false
                                    navController.popBackStack()
                                },
                                onFailure = { e ->
                                    isLoading = false
                                    errorMessage = e.message ?: "Ошибка входа"
                                }
                            )
                        }
                    }
                )
                2 -> MicrosoftTab(
                    onLogin = {
                        errorMessage = "Microsoft OAuth требует WebView реализации"
                    }
                )
            }
        }
    }
}

@Composable
fun OfflineTab(username: String, onUsernameChange: (String) -> Unit, onLogin: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = { Text("Никнейм") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = onLogin, modifier = Modifier.fillMaxWidth(), enabled = username.isNotBlank()) {
            Text("Играть Offline")
        }
        Text(
            "Offline режим позволяет играть без аккаунта. Скины и сервера с online-mode будут недоступны.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ElyByTab(
    username: String, password: String,
    onUsernameChange: (String) -> Unit, onPasswordChange: (String) -> Unit,
    isLoading: Boolean, errorMessage: String?, onLogin: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = { Text("E-mail или ник") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Пароль") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        if (errorMessage != null) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Button(
            onClick = onLogin,
            modifier = Modifier.fillMaxWidth(),
            enabled = username.isNotBlank() && password.isNotBlank() && !isLoading
        ) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text("Войти через Ely.by")
        }
        Text(
            "Ely.by — бесплатная система скинов и аккаунтов. Регистрация: https://ely.by",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MicrosoftTab(onLogin: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Microsoft Account", style = MaterialTheme.typography.titleMedium)
        Button(onClick = onLogin, modifier = Modifier.fillMaxWidth()) {
            Text("Войти через Microsoft")
        }
        Text(
            "Требуется лицензия Minecraft Java Edition. Поддерживает официальные сервера, Realms и скины.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
