package com.example.faceid.diana.security

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Persistencia de Diana (espejo de ProtectedAppsDataSource de Fabian).
 * SharedPreferences "diana_security": decisión del equipo.
 * Guarda solo el hash del PIN, nunca el PIN en claro.
 */
class SecurityPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun getPinHash(): String? {
        return prefs.getString(KEY_PIN_HASH, null)
    }

    fun savePinHash(hash: String) {
        prefs.edit { putString(KEY_PIN_HASH, hash) }
    }

    fun clearPin() {
        prefs.edit { remove(KEY_PIN_HASH) }
    }

    fun isPinConfigured(): Boolean {
        return !getPinHash().isNullOrBlank()
    }

    fun isBiometricsEnabled(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRICS, false)
    }

    fun setBiometricsEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_BIOMETRICS, enabled) }
    }

    /** Vector de embedding del rostro registrado (CSV de floats), o null. */
    fun getFaceEmbedding(): String? {
        return prefs.getString(KEY_FACE_EMBEDDING, null)
    }

    fun saveFaceEmbedding(value: String) {
        prefs.edit(commit = true) { putString(KEY_FACE_EMBEDDING, value) }
    }

    fun clearFaceEmbedding() {
        prefs.edit(commit = true) { remove(KEY_FACE_EMBEDDING) }
    }

    companion object {
        private const val PREFS_NAME = "diana_security"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_BIOMETRICS = "biometrics_enabled"
        private const val KEY_FACE_EMBEDDING = "face_embedding"
    }
}
