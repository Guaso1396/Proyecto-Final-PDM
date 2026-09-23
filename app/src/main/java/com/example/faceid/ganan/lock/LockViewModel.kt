package com.example.faceid.ganan.lock

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.faceid.diana.biometric.BiometricAuthenticator
import com.example.faceid.diana.face.FaceEmbeddingStore
import com.example.faceid.diana.face.FaceRecognizer
import com.example.faceid.diana.security.PasswordManager
import com.example.faceid.diana.security.SecurityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel de Ganan (mismo patrón que Fabian `AppsViewModel` y Diana
 * `AuthenticationViewModel`: AndroidViewModel + uiState mutableStateOf).
 *
 * Delega la verificación del PIN a [SecurityManager] de Diana y la
 * disponibilidad biométrica a [BiometricAuthenticator]. No duplica hashes
 * ni preferencias. El rostro se compara con el embedding registrado vía
 * [FaceEmbeddingStore] + [FaceRecognizer].
 */
class LockViewModel(application: Application) : AndroidViewModel(application) {

    private val security = SecurityManager(application)
    private val appContext = application.applicationContext
    private val faceStore = FaceEmbeddingStore(application.applicationContext)
    private val recognizer = FaceRecognizer(application.applicationContext)

    var uiState by mutableStateOf(LockUiState())
        private set

    private var isFaceVerifying = false
    private var lastFaceAttemptAt = 0L

    /** Ventana móvil de los últimos intentos: true = hubo match. */
    private val faceWindow = ArrayDeque<Boolean>()

    /** Fija la app objetivo y refresca flags (biometría / PIN / rostro). */
    fun setTargetApp(packageName: String, appLabel: String = "") {
        val resolvedLabel = appLabel.ifBlank { resolveLabel(packageName) }
        faceWindow.clear()
        lastFaceAttemptAt = 0L
        uiState = uiState.copy(
            packageName = packageName,
            appLabel = resolvedLabel,
            pinConfigured = security.isPinConfigured(),
            canUseBiometrics = security.isBiometricsEnabled() &&
                BiometricAuthenticator.canAuthenticate(appContext),
            faceEnrolled = faceStore.hasFace(),
            faceMatches = 0,
            result = LockResult.Idle
        )
    }

    fun onPinChange(value: String) {
        uiState = uiState.copy(
            pin = value.filter { it.isDigit() }.take(PasswordManager.PIN_LENGTH),
            result = LockResult.Idle
        )
    }

    fun resetResult() {
        uiState = uiState.copy(result = LockResult.Idle)
    }

    /** Verifica el PIN contra el hash guardado por Diana. */
    fun verifyPin() {
        val pin = uiState.pin
        if (!PasswordManager.isValidFormat(pin)) {
            uiState = uiState.copy(result = LockResult.Error("Ingresa un PIN de 4 dígitos"))
            return
        }
        uiState = uiState.copy(result = LockResult.Loading)
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { security.verifyPin(pin) }
            uiState = uiState.copy(
                result = if (ok) LockResult.Success
                else LockResult.Error("PIN incorrecto")
            )
        }
    }

    /** Llamado desde biometric Success para desbloquear sin PIN. */
    fun onBiometricSuccess() {
        uiState = uiState.copy(result = LockResult.Success)
    }

    fun onBiometricFailed(message: String) {
        uiState = uiState.copy(result = LockResult.Error(message))
    }

    /**
     * Solo el rostro registrado del alta multi-muestra.
     * Desbloquea con [MATCHES_REQUIRED] matches ≥ [FaceRecognizer.THRESHOLD]
     * dentro de una ventana de [WINDOW] intentos (un frame malo no reinicia
     * todo el progreso). Cualquier otro rostro → error (PIN/huella de respaldo).
     */
    fun processFaceScan(bitmap: Bitmap, faceBox: android.graphics.Rect) {
        if (isFaceVerifying) return
        val result = uiState.result
        if (result is LockResult.Success || result is LockResult.Loading) return
        if (uiState.pin.isNotEmpty()) return
        val now = System.currentTimeMillis()
        if (now - lastFaceAttemptAt < FACE_COOLDOWN_MS) return
        lastFaceAttemptAt = now
        val templates = faceStore.loadAll()
        if (templates.isEmpty()) return
        isFaceVerifying = true
        viewModelScope.launch {
            try {
                val probe = withContext(Dispatchers.Default) {
                    recognizer.embedFaceBox(
                        bitmap,
                        faceBox,
                        minFacePx = FaceRecognizer.MIN_FACE_PX
                    )
                }
                val scores = if (probe != null) {
                    withContext(Dispatchers.Default) {
                        templates.map { cosine(it, probe) }
                    }
                } else {
                    emptyList()
                }
                val best = scores.maxOrNull() ?: -1f
                val match = probe != null && best >= FaceRecognizer.THRESHOLD
                android.util.Log.d(
                    "FaceLock",
                    "probe=${probe != null} templates=${templates.size} " +
                        "best=${"%.3f".format(best)} match=$match " +
                        "scores=${scores.joinToString(",") { "%.3f".format(it) }}"
                )
                val current = uiState.result
                if (current !is LockResult.Success && current !is LockResult.Loading) {
                    if (faceWindow.size >= WINDOW) faceWindow.removeFirst()
                    faceWindow.addLast(match)
                    val matches = faceWindow.count { it }
                    when {
                        matches >= MATCHES_REQUIRED -> {
                            uiState = uiState.copy(
                                result = LockResult.Success,
                                faceMatches = MATCHES_REQUIRED
                            )
                        }
                        faceWindow.size >= ERROR_AFTER_ATTEMPTS && matches == 0 -> {
                            uiState = uiState.copy(
                                faceMatches = 0,
                                result = LockResult.Error(
                                    "Rostro no reconocido. Solo el rostro registrado puede entrar."
                                )
                            )
                        }
                        else -> {
                            uiState = uiState.copy(
                                faceMatches = matches,
                                result = if (current is LockResult.Error && matches > 0) {
                                    LockResult.Idle
                                } else {
                                    current
                                }
                            )
                        }
                    }
                }
            } finally {
                isFaceVerifying = false
            }
        }
    }

    private fun cosine(a: FloatArray, b: FloatArray): Float {
        val n = minOf(a.size, b.size)
        if (n == 0) return -1f
        var dot = 0f
        var na = 0f
        var nb = 0f
        for (i in 0 until n) {
            dot += a[i] * b[i]
            na += a[i] * a[i]
            nb += b[i] * b[i]
        }
        if (na <= 1e-6f || nb <= 1e-6f) return -1f
        return dot / (kotlin.math.sqrt(na) * kotlin.math.sqrt(nb))
    }

    private companion object {
        const val FACE_COOLDOWN_MS = 350L

        /** Matches necesarios para desbloquear. */
        const val MATCHES_REQUIRED = 3

        /** Tamaño de la ventana de intentos (frames malos no reinician todo). */
        const val WINDOW = 6

        /** Tras tantos intentos sin ningún match, mostrar error. */
        const val ERROR_AFTER_ATTEMPTS = 4
    }

    private fun resolveLabel(packageName: String): String {
        if (packageName.isBlank()) return ""
        return try {
            val pm = appContext.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info)?.toString() ?: packageName
        } catch (_: Exception) {
            packageName
        }
    }
}

