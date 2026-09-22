package com.example.faceid.ganan.lock

/**
 * Estado de Ganan para la pantalla de bloqueo.
 * Espejo del patrón de Diana ([com.example.faceid.diana.authentication.AuthResult])
 * y de Fabian (`AppsUiState`), para mantener el estilo del equipo.
 */
data class LockUiState(
    val packageName: String = "",
    val appLabel: String = "",
    val pin: String = "",
    val result: LockResult = LockResult.Idle,
    val canUseBiometrics: Boolean = false,
    val pinConfigured: Boolean = true
)

/** Resultado del intento de desbloqueo de una app protegida. */
sealed interface LockResult {
    data object Idle : LockResult
    data object Loading : LockResult
    data object Success : LockResult
    data class Error(val message: String) : LockResult
}

