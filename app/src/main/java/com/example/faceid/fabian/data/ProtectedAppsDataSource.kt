package com.example.faceid.fabian.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Fuente de verdad de Fabian: set de packageNames protegidos.
 * Persistencia simple con SharedPreferences (decisión del equipo).
 * Usado por AppsViewModel y por HomeViewModel (protectedAppsCount).
 */
class ProtectedAppsDataSource(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun getProtectedPackages(): Set<String> {
        return prefs.getStringSet(KEY_PROTECTED, emptySet())?.toSet() ?: emptySet()
    }

    fun isProtected(packageName: String): Boolean {
        return getProtectedPackages().contains(packageName)
    }

    fun setProtected(packageName: String, protect: Boolean) {
        val current = getProtectedPackages().toMutableSet()
        if (protect) current.add(packageName) else current.remove(packageName)
        prefs.edit { putStringSet(KEY_PROTECTED, current) }
    }

    /** Alterna y retorna el nuevo estado. */
    fun toggleProtected(packageName: String): Boolean {
        val nowProtected = !isProtected(packageName)
        setProtected(packageName, nowProtected)
        return nowProtected
    }

    fun getCount(): Int = getProtectedPackages().size

    fun clear() {
        prefs.edit { remove(KEY_PROTECTED) }
    }

    companion object {
        private const val PREFS_NAME = "fabian_protected_apps"
        private const val KEY_PROTECTED = "protected_set"
    }
}
