package com.vertylauncher.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vertylauncher.feature.settings.SettingsRepository
import com.vertylauncher.feature.version.VersionInfo
import com.vertylauncher.feature.version.VersionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VersionListViewModel(
    private val versionManager: VersionManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VersionListUiState())
    val uiState: StateFlow<VersionListUiState> = _uiState.asStateFlow()

    init {
        loadVersions()
    }

    fun loadVersions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            versionManager.fetchVersionManifest().fold(
                onSuccess = { manifest ->
                    _uiState.value = _uiState.value.copy(
                        versions = manifest.versions,
                        latestRelease = manifest.latest.release,
                        latestSnapshot = manifest.latest.snapshot,
                        isLoading = false
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Unknown error"
                    )
                }
            )
        }
    }

    fun selectVersion(versionId: String) {
        viewModelScope.launch {
            settingsRepository.updateSelectedVersion(versionId)
        }
    }

    data class VersionListUiState(
        val versions: List<VersionInfo> = emptyList(),
        val latestRelease: String = "",
        val latestSnapshot: String = "",
        val isLoading: Boolean = false,
        val error: String? = null
    )
}
