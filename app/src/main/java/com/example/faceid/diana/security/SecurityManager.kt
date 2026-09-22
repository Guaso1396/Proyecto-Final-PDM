package com.example.faceid.diana.security

import android.content.Context

/**
 * Fachada de Diana para Kevin (HomeViewModel) y Ganan (LockManager).
 * Uso:
 *   val security = SecurityManager(context)
 *   security.savePin("1234") / security.verifyPin("1234")
 */
class SecurityManager(context: Context) {

    private val prefs = SecurityPreferences(context.applicationContext)

    fun isPinConfigured(): Boolean = prefs.isPinConfigured()

    fun isBiometricsEnabled(): Boolean = prefs.isBiometricsEnabled()

    fun setBiometricsEnabled(enabled: Boolean) {
        prefs.setBiometricsEnabled(enabled)
    }

    /** Verifica un PIN contra el hash guardado. */
    fun verifyPin(pin: String): Boolean {
        val hash = prefs.getPinHash() ?: return false
        return PasswordManager.verify(pin, hash)
    }

    /** Guarda un PIN nuevo (lo hashea). Retorna false si el formato es inválido. */
    fun savePin(pin: String): Boolean {
        if (!PasswordManager.isValidFormat(pin)) return false
        prefs.savePinHash(PasswordManager.hashPin(pin))
        return true
    }

    /** Cambia el PIN validando el anterior. Retorna false si falla. */
    fun changePin(currentPin: String, newPin: String): Boolean {
        if (!verifyPin(currentPin)) return false
        return savePin(newPin)
    }

    fun clearPin() {
        prefs.clearPin()
    }
}
