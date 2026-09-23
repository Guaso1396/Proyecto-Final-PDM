package com.example.faceid.ganan.manager

import android.content.Context
import com.example.faceid.fabian.data.ProtectedAppsDataSource

/**
 * Fachada de Ganan sobre la fuente de verdad de Fabian.
 * Decide si una app debe mostrar [com.example.faceid.ganan.lock.LockScreen].
 *
 * El desbloqueo es de sesión: dura mientras el usuario permanezca en la
 * app (sin límite de tiempo) y solo se revoca al salir, vía [lockNow].
 * El set se guarda en el companion (compartida por proceso) para que el
 * servicio, la activity de bloqueo y la lock screen vean el mismo estado.
 */
class LockManager(context: Context) {

    private val protectedApps = ProtectedAppsDataSource(context.applicationContext)

    fun isProtected(packageName: String): Boolean {
        if (packageName.isBlank()) return false
        return protectedApps.isProtected(packageName)
    }

    @Synchronized
    fun shouldLock(packageName: String): Boolean {
        if (!isProtected(packageName)) return false
        return packageName !in unlockedSessions
    }

    @Synchronized
    fun markUnlocked(packageName: String) {
        if (packageName.isBlank()) return
        unlockedSessions.add(packageName)
    }

    @Synchronized
    fun lockNow(packageName: String) {
        unlockedSessions.remove(packageName)
    }

    @Synchronized
    fun clearAll() {
        unlockedSessions.clear()
    }

    companion object {
        /** packageNames desbloqueados mientras el usuario siga en ellos. */
        private val unlockedSessions = mutableSetOf<String>()
    }
}
