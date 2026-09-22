package com.example.faceid.diana.biometric

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Wrapper simple de Diana sobre androidx.biometric (sin CryptoObject/Keystore).
 * Coherente con SharedPreferences y con el biometric:1.2.0-alpha05 del proyecto.
 */
object BiometricAuthenticator {

    /** True si el dispositivo puede autenticar con biométricos. */
    fun canAuthenticate(context: Context): Boolean {
        val manager = BiometricManager.from(context)
        return manager.canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    /** Razón legible cuando [canAuthenticate] es false. */
    fun notAvailableReason(context: Context): String {
        return when (
            BiometricManager.from(context)
                .canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK)
        ) {
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "Sin hardware biométrico"
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Hardware no disponible"
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "Sin huella/rostro enrolado"
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> "Requiere actualización"
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> "No soportado"
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> "Estado desconocido"
            else -> "Biometría no disponible"
        }
    }

    /**
     * Lanza el prompt del sistema. Requiere FragmentActivity
     * (ver MainActivity : FragmentActivity).
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Desbloqueo biométrico",
        subtitle: String = "Usa tu huella o rostro",
        negativeButtonText: String = "Usar PIN",
        onResult: (BiometricResult) -> Unit
    ) {
        if (!canAuthenticate(activity)) {
            onResult(BiometricResult.NotAvailable(notAvailableReason(activity)))
            return
        }
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    onResult(BiometricResult.Success)
                }

                override fun onAuthenticationFailed() {
                    onResult(BiometricResult.Failed)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_CANCELED
                    ) {
                        onResult(BiometricResult.Cancelled)
                    } else {
                        onResult(BiometricResult.Error(errorCode, errString.toString()))
                    }
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK)
            .build()
        prompt.authenticate(info)
    }
}
