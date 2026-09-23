package com.example.faceid.kevin.home

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.example.faceid.diana.face.FaceEmbeddingStore
import com.example.faceid.diana.security.SecurityManager
import com.example.faceid.fabian.data.ProtectedAppsDataSource
import com.example.faceid.ganan.service.AppMonitoringService

data class HomeUiState(
    val isLoading: Boolean = false,
    val pinConfigured: Boolean = false,
    val biometricsEnabled: Boolean = false,
    val faceEnrolled: Boolean = false,
    val protectedAppsCount: Int = 0,
    val lockActive: Boolean = false,
    val hasUsagePermission: Boolean = false,
    val hasOverlayPermission: Boolean = false
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val security = SecurityManager(appContext)
    private val faceStore = FaceEmbeddingStore(appContext)
    private val protectedApps = ProtectedAppsDataSource(appContext)

    var uiState by mutableStateOf(HomeUiState())
        private set

    init {
        refresh()
    }

    fun refresh() {
        uiState = uiState.copy(
            pinConfigured = security.isPinConfigured(),
            biometricsEnabled = security.isBiometricsEnabled(),
            faceEnrolled = faceStore.hasFace(),
            protectedAppsCount = protectedApps.getProtectedPackages().size,
            hasUsagePermission = AppMonitoringService.hasUsagePermission(appContext),
            hasOverlayPermission = Settings.canDrawOverlays(appContext),
            lockActive = uiState.lockActive
        )
    }

    fun onToggleBiometrics(enabled: Boolean) {
        security.setBiometricsEnabled(enabled)
        uiState = uiState.copy(biometricsEnabled = enabled)
    }

    fun onToggleLockActive(active: Boolean) {
        if (active) {
            if (!AppMonitoringService.hasUsagePermission(appContext)) {
                requestUsagePermission()
                return
            }
            // Sin superposición, Android bloquea el arranque de la pantalla
            // de bloqueo desde el servicio: el candado nunca aparecería.
            if (!Settings.canDrawOverlays(appContext)) {
                requestOverlayPermission()
                return
            }
            AppMonitoringService.start(appContext)
            uiState = uiState.copy(lockActive = true)
        } else {
            AppMonitoringService.stop(appContext)
            uiState = uiState.copy(lockActive = false)
        }
    }

    fun requestUsagePermission() {
        appContext.startActivity(
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun requestOverlayPermission() {
        appContext.startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${appContext.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, appContext.packageName)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
