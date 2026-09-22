package com.example.faceid.diana.security

import java.security.MessageDigest

/**
 * Lógica pura de Diana: validación y hash del PIN.
 * Sin dependencias Android, testeable con JUnit.
 */
object PasswordManager {

    const val PIN_LENGTH = 4

    /** PIN válido: exactamente 4 dígitos. */
    fun isValidFormat(pin: String): Boolean {
        return pin.length == PIN_LENGTH && pin.all { it.isDigit() }
    }

    /** Hash SHA-256 hex del PIN. Nunca se guarda el PIN en claro. */
    fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /** Compara un PIN en claro contra un hash guardado. */
    fun verify(pin: String, hash: String): Boolean {
        if (!isValidFormat(pin) || hash.isBlank()) return false
        return hashPin(pin) == hash
    }
}
