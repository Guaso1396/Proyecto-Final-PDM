package com.example.faceid.diana.authentication

/** Estado de resultado para los flujos de PIN de Diana. */
sealed interface AuthResult {
    data object Idle : AuthResult
    data object Loading : AuthResult
    data object Success : AuthResult
    data class Error(val message: String) : AuthResult
}
