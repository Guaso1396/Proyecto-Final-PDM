package com.example.faceid.kevin.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

data class HomeUiState(
    val isLoading: Boolean = false,
    val pinConfigured: Boolean = false,
    val biometricsEnabled: Boolean = false,
    val protectedAppsCount: Int = 0,
    val lockActive: Boolean = true
)

class HomeViewModel : ViewModel() {
    var uiState by mutableStateOf(HomeUiState())
        private set

    // TODO(diana): leer pinConfigured y biometricsEnabled desde SecurityManager
    // TODO(fabian): leer protectedAppsCount desde ProtectedAppsDataSource

    fun onToggleBiometrics(enabled: Boolean) {
        uiState = uiState.copy(biometricsEnabled = enabled)
        // TODO(diana): persistir preferencia real de biometría
    }

    fun onToggleLockActive(active: Boolean) {
        uiState = uiState.copy(lockActive = active)
    }
}
