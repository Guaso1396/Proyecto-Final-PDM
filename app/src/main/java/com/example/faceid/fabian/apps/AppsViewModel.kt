package com.example.faceid.fabian.apps

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.faceid.fabian.data.InstalledAppsDataSource
import com.example.faceid.fabian.data.ProtectedAppsDataSource
import com.example.faceid.fabian.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppsUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    val apps: List<AppInfo> = emptyList(),
    val error: String? = null
) {
    val protectedCount: Int get() = apps.count { it.isProtected }

    val filteredApps: List<AppInfo>
        get() {
            if (query.isBlank()) return apps
            val q = query.trim().lowercase()
            return apps.filter {
                it.label.lowercase().contains(q) ||
                    it.packageName.lowercase().contains(q)
            }
        }
}

class AppsViewModel(application: Application) : AndroidViewModel(application) {

    private val installedAppsDataSource = InstalledAppsDataSource(application)
    private val protectedAppsDataSource = ProtectedAppsDataSource(application)

    var uiState by mutableStateOf(AppsUiState())
        private set

    init {
        loadApps()
    }

    fun loadApps() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)
            try {
                val apps = withContext(Dispatchers.IO) {
                    val installed = installedAppsDataSource.getInstalledApps()
                    val protected = protectedAppsDataSource.getProtectedPackages()
                    installed.map { it.copy(isProtected = protected.contains(it.packageName)) }
                }
                uiState = uiState.copy(isLoading = false, apps = apps)
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isLoading = false,
                    error = e.message ?: "No se pudieron cargar las apps"
                )
            }
        }
    }

    fun onQueryChange(query: String) {
        uiState = uiState.copy(query = query)
    }

    fun onToggleProtect(packageName: String, protect: Boolean) {
        // Persistencia inmediata (SharedPreferences es rápido, va en principal).
        protectedAppsDataSource.setProtected(packageName, protect)
        uiState = uiState.copy(
            apps = uiState.apps.map {
                if (it.packageName == packageName) it.copy(isProtected = protect) else it
            }
        )
    }

    /** Expuesto para HomeViewModel / LockManager si necesitan el conteo. */
    fun getProtectedCount(): Int = protectedAppsDataSource.getCount()
}
