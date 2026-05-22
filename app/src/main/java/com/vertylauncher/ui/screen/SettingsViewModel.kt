package com.vertylauncher.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vertylauncher.feature.settings.LauncherSettings
import com.vertylauncher.feature.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                _uiState.value = SettingsUiState(
                    renderer = settings.renderer,
                    maxMemory = settings.maxMemory,
                    username = settings.username,
                    selectedVersion = settings.selectedVersion
                )
            }
        }
    }

    fun updateRenderer(value: String) {
        viewModelScope.launch { settingsRepository.updateRenderer(value) }
    }

    fun updateMaxMemory(value: Int) {
        viewModelScope.launch { settingsRepository.updateMaxMemory(value) }
    }

    fun updateUsername(value: String) {
        viewModelScope.launch { settingsRepository.updateUsername(value) }
    }

    data class SettingsUiState(
        val renderer: String = "angle",
        val maxMemory: Int = 2048,
        val username: String = "Player",
        val selectedVersion: String = ""
    )
}
