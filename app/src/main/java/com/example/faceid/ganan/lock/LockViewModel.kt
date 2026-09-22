package com.example.faceid.ganan.lock

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.faceid.diana.biometric.BiometricAuthenticator
import com.example.faceid.diana.security.PasswordManager
import com.example.faceid.diana.security.SecurityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel de Ganan (mismo patrón que Fabian `AppsViewModel` y Diana
 * `AuthenticationViewModel`: AndroidViewModel + uiState mutableStateOf).
 *
 * Delega la verificación del PIN a [SecurityManager] de Diana y la
 * disponibilidad biométrica a [BiometricAuthenticator]. No duplica hashes
 * ni preferencias.
 */
class LockViewModel(application: Application) : AndroidViewModel(application) {

    private val security = SecurityManager(application)
    private val appContext = application.applicationContext

    var uiState by mutableStateOf(LockUiState())
        private set

    /** Fija la app objetivo y refresca flags (biometría / PIN configurado). */
    fun setTargetApp(packageName: String, appLabel: String = "") {
        val resolvedLabel = appLabel.ifBlank { resolveLabel(packageName) }
        uiState = uiState.copy(
            packageName = packageName,
            appLabel = resolvedLabel,
            pinConfigured = security.isPinConfigured(),
            canUseBiometrics = security.isBiometricsEnabled() &&
                BiometricAuthenticator.canAuthenticate(appContext)
        )
    }

    fun onPinChange(value: String) {
        uiState = uiState.copy(
            pin = value.filter { it.isDigit() }.take(PasswordManager.PIN_LENGTH),
            result = LockResult.Idle
        )
    }

    fun resetResult() {
        uiState = uiState.copy(result = LockResult.Idle)
    }

    /** Verifica el PIN contra el hash guardado por Diana. */
    fun verifyPin() {
        val pin = uiState.pin
        if (!PasswordManager.isValidFormat(pin)) {
            uiState = uiState.copy(result = LockResult.Error("Ingresa un PIN de 4 dígitos"))
            return
        }
        uiState = uiState.copy(result = LockResult.Loading)
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { security.verifyPin(pin) }
            uiState = uiState.copy(
                result = if (ok) LockResult.Success
                else LockResult.Error("PIN incorrecto")
            )
        }
    }

    /** Llamado desde biometric Success para desbloquear sin PIN. */
    fun onBiometricSuccess() {
        uiState = uiState.copy(result = LockResult.Success)
    }

    fun onBiometricFailed(message: String) {
        uiState = uiState.copy(result = LockResult.Error(message))
    }

    private fun resolveLabel(packageName: String): String {
        if (packageName.isBlank()) return ""
        return try {
            val pm = appContext.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info)?.toString() ?: packageName
        } catch (_: Exception) {
            packageName
        }
    }
}

