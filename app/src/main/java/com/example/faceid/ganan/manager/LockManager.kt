package com.example.faceid.ganan.manager

import android.content.Context
import android.os.SystemClock
import com.example.faceid.fabian.data.ProtectedAppsDataSource

/**
 * Fachada de Ganan sobre la fuente de verdad de Fabian.
 * Decide si una app debe mostrar [com.example.faceid.ganan.lock.LockScreen].
 *
 * Mantiene una ventana de gracia en memoria (solo runtime, no persistida):
 * tras un desbloqueo exitoso no se vuelve a pedir PIN durante [UNLOCK_GRACE_MS].
 * Usa [SystemClock.elapsedRealtime] para no depender del reloj del sistema.
 *
 * Uso:
 * ```
 * val locks = LockManager(context)
 * if (locks.shouldLock(packageName)) { /* navegar a Routes.lock(packageName) */ }
 * else { /* abrir directo */ }
 * // tras LockResult.Success:
 * locks.markUnlocked(packageName)
 * ```
 */
class LockManager(context: Context) {

    private val protectedApps = ProtectedAppsDataSource(context.applicationContext)

    /** packageName -> elapsedRealtime hasta el que está desbloqueado. */
    private val unlockedUntil = mutableMapOf<String, Long>()

    /** True si la app está en el set protegido de Fabian. */
    fun isProtected(packageName: String): Boolean {
        if (packageName.isBlank()) return false
        return protectedApps.isProtected(packageName)
    }

    /**
     * True si hay que bloquear: está protegida y fuera de la ventana de gracia.
     * Las apps no protegidas nunca se bloquean.
     */
    @Synchronized
    fun shouldLock(packageName: String): Boolean {
        if (!isProtected(packageName)) return false
        val now = SystemClock.elapsedRealtime()
        val until = unlockedUntil[packageName] ?: 0L
        if (now < until) return false
        unlockedUntil.remove(packageName)
        return true
    }

    /** Marca la app como desbloqueada durante la ventana de gracia. */
    @Synchronized
    fun markUnlocked(packageName: String) {
        if (packageName.isBlank()) return
        unlockedUntil[packageName] = SystemClock.elapsedRealtime() + UNLOCK_GRACE_MS
    }

    /** Revoca la gracia (ej. la app pasó a segundo plano). */
    @Synchronized
    fun lockNow(packageName: String) {
        unlockedUntil.remove(packageName)
    }

    @Synchronized
    fun clearAll() {
        unlockedUntil.clear()
    }

    companion object {
        /** 2 minutos de gracia tras desbloquear, decisión local de Ganan. */
        const val UNLOCK_GRACE_MS: Long = 2 * 60 * 1000L
    }
}

