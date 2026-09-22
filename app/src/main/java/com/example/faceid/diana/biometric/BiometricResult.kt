package com.example.faceid.diana.biometric

/** Resultado del prompt biométrico de Diana. */
sealed interface BiometricResult {
    data object Success : BiometricResult
    data object Failed : BiometricResult
    data object Cancelled : BiometricResult
    data class Error(val code: Int, val message: String) : BiometricResult
    data class NotAvailable(val reason: String) : BiometricResult
}
