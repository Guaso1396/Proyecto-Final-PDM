package com.example.faceid.diana.authentication

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

data class AuthUiState(
    val pin: String = "",
    val confirmPin: String = "",
    val currentPin: String = "",
    val result: AuthResult = AuthResult.Idle,
    val biometricsEnabled: Boolean = false,
    val canUseBiometrics: Boolean = false
)

/**
 * ViewModel de Diana (mismo patrón que AppsViewModel de Fabian:
 * AndroidViewModel + uiState mutableStateOf).
 */
class AuthenticationViewModel(application: Application) : AndroidViewModel(application) {

    private val security = SecurityManager(application)
    private val appContext = application.applicationContext

    var uiState by mutableStateOf(AuthUiState())
        private set

    init {
        refreshPrefs()
    }

    fun refreshPrefs() {
        uiState = uiState.copy(
            biometricsEnabled = security.isBiometricsEnabled(),
            canUseBiometrics = security.isBiometricsEnabled() &&
                BiometricAuthenticator.canAuthenticate(appContext)
        )
    }

    fun onPinChange(value: String) {
        uiState = uiState.copy(
            pin = value.filter { it.isDigit() }.take(PasswordManager.PIN_LENGTH),
            result = AuthResult.Idle
        )
    }

    fun onConfirmChange(value: String) {
        uiState = uiState.copy(
            confirmPin = value.filter { it.isDigit() }.take(PasswordManager.PIN_LENGTH),
            result = AuthResult.Idle
        )
    }

    fun onCurrentPinChange(value: String) {
        uiState = uiState.copy(
            currentPin = value.filter { it.isDigit() }.take(PasswordManager.PIN_LENGTH),
            result = AuthResult.Idle
        )
    }

    fun resetResult() {
        uiState = uiState.copy(result = AuthResult.Idle)
    }

    /** EnterPin: verifica contra el hash guardado. */
    fun verifyPin() {
        val pin = uiState.pin
        if (!PasswordManager.isValidFormat(pin)) {
            uiState = uiState.copy(result = AuthResult.Error("Ingresa un PIN de 4 dígitos"))
            return
        }
        uiState = uiState.copy(result = AuthResult.Loading)
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { security.verifyPin(pin) }
            uiState = uiState.copy(
                result = if (ok) AuthResult.Success
                else AuthResult.Error("PIN incorrecto")
            )
        }
    }

    /** CreatePin: valida coincidencia y guarda el hash. */
    fun createPin() {
        val pin = uiState.pin
        val confirm = uiState.confirmPin
        if (!PasswordManager.isValidFormat(pin)) {
            uiState = uiState.copy(result = AuthResult.Error("El PIN debe tener 4 dígitos"))
            return
        }
        if (pin != confirm) {
            uiState = uiState.copy(result = AuthResult.Error("Los PIN no coinciden"))
            return
        }
        uiState = uiState.copy(result = AuthResult.Loading)
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { security.savePin(pin) }
            uiState = uiState.copy(
                result = if (ok) AuthResult.Success
                else AuthResult.Error("No se pudo guardar el PIN")
            )
        }
    }

    /** ChangePin: valida el actual y guarda el nuevo. */
    fun changePin() {
        val current = uiState.currentPin
        val pin = uiState.pin
        val confirm = uiState.confirmPin
        if (!PasswordManager.isValidFormat(current) || !PasswordManager.isValidFormat(pin)) {
            uiState = uiState.copy(result = AuthResult.Error("PIN de 4 dígitos requerido"))
            return
        }
        if (pin != confirm) {
            uiState = uiState.copy(result = AuthResult.Error("El PIN nuevo no coincide"))
            return
        }
        if (current == pin) {
            uiState = uiState.copy(result = AuthResult.Error("El nuevo PIN debe ser distinto"))
            return
        }
        uiState = uiState.copy(result = AuthResult.Loading)
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { security.changePin(current, pin) }
            uiState = uiState.copy(
                result = if (ok) AuthResult.Success
                else AuthResult.Error("El PIN actual es incorrecto")
            )
        }
    }

    /** Llamado desde biometric Success para entrar sin PIN. */
    fun onBiometricSuccess() {
        uiState = uiState.copy(result = AuthResult.Success)
    }

    fun onBiometricFailed(message: String) {
        uiState = uiState.copy(result = AuthResult.Error(message))
    }
}
