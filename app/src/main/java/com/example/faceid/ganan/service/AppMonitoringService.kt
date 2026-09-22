package com.example.faceid.ganan.service

import android.app.AppOpsManager
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Process
import com.example.faceid.ganan.manager.LockManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Servicio de Ganan: detecta la app en primer plano con UsageStatsManager
 * y avisa cuando una app protegida (Fabian) necesita bloqueo.
 *
 * Solo detecta; la navegación a `Routes.lock(package)` la hace quien observe
 * [onProtectedAppDetected] (ej. MainActivity). No muestra overlays.
 *
 * Integración pendiente (la aplica el equipo, fuera de `ganan/`):
 * ```xml
 * <!-- AndroidManifest.xml dentro de <application> -->
 * <service android:name=".ganan.service.AppMonitoringService"
 *     android:foregroundServiceType="dataSync" />
 * ```
 * Permisos ya declarados: PACKAGE_USAGE_STATS + FOREGROUND_SERVICE.
 * El usuario debe conceder acceso a uso en Ajustes; ver [hasUsagePermission].
 */
class AppMonitoringService : Service() {

    private lateinit var lockManager: LockManager
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var pollJob: Job? = null
    private var lastNotifiedPackage: String? = null

    override fun onCreate() {
        super.onCreate()
        lockManager = LockManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (pollJob?.isActive != true) {
            pollJob = scope.launch {
                while (isActive) {
                    try {
                        val foreground = getForegroundPackage()
                        if (foreground != null &&
                            foreground != packageName &&
                            lockManager.shouldLock(foreground)
                        ) {
                            // Evita notificar en bucle la misma app.
                            if (foreground != lastNotifiedPackage) {
                                lastNotifiedPackage = foreground
                                onProtectedAppDetected?.invoke(foreground)
                            }
                        } else if (foreground != null && foreground != lastNotifiedPackage) {
                            // Cambió la app visible: permite notificar de nuevo.
                            if (foreground != packageName) lastNotifiedPackage = null
                        }
                    } catch (_: Exception) {
                        // Polling best-effort: nunca tumbar el servicio.
                    }
                    delay(POLL_INTERVAL_MS)
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        pollJob?.cancel()
        super.onDestroy()
    }

    /** App en primer plano según UsageStats (ventana de 10s). Null si sin permiso. */
    private fun getForegroundPackage(): String? {
        if (!hasUsagePermission(this)) return null
        val usage = getSystemService(USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return null
        val now = System.currentTimeMillis()
        val stats = try {
            usage.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                now - QUERY_WINDOW_MS,
                now
            )
        } catch (_: Exception) {
            null
        } ?: return null
        return stats.maxByOrNull { it.lastTimeUsed }?.packageName
    }

    companion object {
        const val POLL_INTERVAL_MS: Long = 1500L
        private const val QUERY_WINDOW_MS: Long = 10_000L

        /** Callback en hilo de fondo con el package protegido detectado. */
        @Volatile
        var onProtectedAppDetected: ((packageName: String) -> Unit)? = null

        fun start(context: Context) {
            val intent = Intent(context, AppMonitoringService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AppMonitoringService::class.java))
        }

        /** True si el usuario concedió acceso a datos de uso (Ajustes). */
        @Suppress("DEPRECATION")
        fun hasUsagePermission(context: Context): Boolean {
            return try {
                val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
                val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    appOps.unsafeCheckOpNoThrow(
                        AppOpsManager.OPSTR_GET_USAGE_STATS,
                        Process.myUid(),
                        context.packageName
                    )
                } else {
                    @Suppress("DEPRECATION")
                    appOps.checkOpNoThrow(
                        AppOpsManager.OPSTR_GET_USAGE_STATS,
                        Process.myUid(),
                        context.packageName
                    )
                }
                mode == AppOpsManager.MODE_ALLOWED
            } catch (_: Exception) {
                false
            }
        }
    }
}

