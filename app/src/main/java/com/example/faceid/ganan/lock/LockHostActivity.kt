package com.example.faceid.ganan.lock

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import com.example.faceid.ganan.manager.LockManager
import com.example.faceid.ui.theme.FACEIDTheme


/**
 * Activity de bloqueo lanzada por [com.example.faceid.ganan.service.AppMonitoringService]
 * cuando el usuario abre una app protegida. Se muestra sobre la app ajena
 * (exenta de Recents). Flujo: escaneo facial → PIN/huella.
 */
class LockHostActivity : FragmentActivity() {

    private var targetPackage by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        targetPackage = intent.getStringExtra(EXTRA_PACKAGE).orEmpty()
        setContent {
            FACEIDTheme {
                LockScreen(
                    packageName = targetPackage,
                    onUnlocked = {
                        LockManager(applicationContext).markUnlocked(targetPackage)
                        finish()
                    },
                    onBack = { /* Sin back: obliga a desbloquear */ }
                )
            }
        }
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        // Bloqueado: no se puede retroceder sin desbloquear.
    }

    companion object {
        private const val EXTRA_PACKAGE = "extra_package"

        /** Lanza la pantalla de bloqueo sobre [packageName]. */
        fun launch(context: Context, packageName: String) {
            val intent = Intent(context, LockHostActivity::class.java)
                .putExtra(EXTRA_PACKAGE, packageName)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            try {
                context.startActivity(intent)
            } catch (_: Exception) {
                // Si el sistema bloquea el arranque, no tumbar el servicio.
            }
        }
    }
}