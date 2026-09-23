package com.example.faceid.ganan.service

import android.app.AppOpsManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Process
import com.example.faceid.ganan.lock.LockHostActivity
import com.example.faceid.ganan.manager.LockManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Servicio de Ganan: detecta la app en primer plano con UsageStatsManager
 * y lanza [LockHostActivity] cuando una app protegida (Fabian) necesita bloqueo.
 */
class AppMonitoringService : Service() {

    private lateinit var lockManager: LockManager
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var pollJob: Job? = null
    private var lockShowingFor: String? = null
    private var lastExternalForeground: String? = null

    override fun onCreate() {
        super.onCreate()
        lockManager = LockManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundInternal()
        if (pollJob?.isActive != true) {
            pollJob = scope.launch {
                while (isActive) {
                    try {
                        val foreground = getForegroundPackage()
                        handleForeground(foreground)
                    } catch (_: Exception) {
                        // Polling best-effort: nunca tumbar el servicio.
                    }
                    delay(POLL_INTERVAL_MS)
                }
            }
        }
        return START_STICKY
    }

    private fun handleForeground(foreground: String?) {
        if (foreground == null) return
        // Nuestra UI (Vault o LockHostActivity) está visible.
        // Si no hay lock en pantalla (el usuario abrió Vault), revoca el
        // desbloqueo de la última app externa para pedirlo al reingresar.
        if (foreground == packageName) {
            if (lockShowingFor == null) {
                lastExternalForeground?.let { lockManager.lockNow(it) }
                lastExternalForeground = null
            }
            return
        }
        // Cambió la app externa en primer plano: la anterior queda bloqueada otra vez.
        val prev = lastExternalForeground
        if (prev != null && prev != foreground) {
            lockManager.lockNow(prev)
        }
        lastExternalForeground = foreground
        if (lockManager.shouldLock(foreground)) {
            if (lockShowingFor != foreground) {
                lockShowingFor = foreground
                LockHostActivity.launch(this, foreground)
            }
        } else {
            // Mismo uso (sin salir): no relanzar el lock.
            lockShowingFor = null
        }
    }

    private fun startForegroundInternal() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val channelId = "app_monitor_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "Protección de apps",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setContentTitle("Vault activo")
            .setContentText("Vigilando apps protegidas")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .build()
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
        private const val NOTIFICATION_ID = 1001

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
