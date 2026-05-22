package com.vertylauncher.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vertylauncher.feature.settings.LauncherSettings
import com.vertylauncher.feature.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                _uiState.value = HomeUiState(
                    username = settings.username,
                    selectedVersion = settings.selectedVersion,
                    canLaunch = settings.selectedVersion.isNotBlank() && settings.username.isNotBlank(),
                    isFirstLaunch = settings.selectedVersion.isNotBlank() && !settings.hasLaunchedBefore
                )
            }
        }
    }

    data class HomeUiState(
        val username: String = "Player",
        val selectedVersion: String = "",
        val canLaunch: Boolean = false,
        val isFirstLaunch: Boolean = false
    )
}
