package com.example.faceid.diana.face

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FaceUiState(
    val status: String = "Buscando tu rostro…",
    val error: String? = null,
    val scanning: Boolean = true,
    val enrolled: Boolean = false,
    val done: Boolean = false
)

class FaceViewModel(application: Application) : AndroidViewModel(application) {

    private val recognizer = FaceRecognizer(application.applicationContext)
    private val store = FaceEmbeddingStore(application.applicationContext)

    var uiState by mutableStateOf(FaceUiState())
        private set

    private var isProcessingFrame = false
    private var lastAttemptAt = 0L
    private var consecutiveFailures = 0
    private val samples = mutableListOf<FloatArray>()

    init {
        checkEnrollment()
        if (!uiState.enrolled && !recognizer.hasModel()) {
            uiState = uiState.copy(
                status = "Buscando tu rostro…",
                error = "Aviso: modelo facial no detectado. Reinstala si falla el registro."
            )
        }
        Log.d(TAG, "init enrolled=${uiState.enrolled} model=${recognizer.hasModel()}")
    }

    fun checkEnrollment() {
        if (samples.isNotEmpty() && !uiState.enrolled && uiState.scanning) {
            // No pisar un alta en curso (LaunchedEffect al reentrar).
            return
        }
        samples.clear()
        val hasEnrolled = store.hasFace()
        Log.d(TAG, "checkEnrollment hasEnrolled=$hasEnrolled")
        uiState = if (hasEnrolled) {
            uiState.copy(
                enrolled = true,
                done = false,
                scanning = false,
                status = "Rostro registrado",
                error = null
            )
        } else {
            uiState.copy(
                enrolled = false,
                done = false,
                scanning = true,
                status = "Buscando tu rostro…",
                error = null
            )
        }
    }

    /** ¿Hay plantilla guardada en disco? (fuente de verdad del menú). */
    fun hasStoredFace(): Boolean = store.hasFace()

    /**
     * Alta multi-muestra: [ENROLL_SAMPLES] embeddings consistentes entre sí.
     * Umbral de acuerdo más laxo que el de lock: solo evita mezclar caras
     * distintas en el mismo alta, sin bloquear el registro por ruido de crop.
     */
    fun onFaceFrame(bitmap: Bitmap, faceBox: Rect) {
        if (isProcessingFrame || uiState.enrolled || !uiState.scanning) return
        val now = System.currentTimeMillis()
        if (now - lastAttemptAt < COOLDOWN_MS) return
        lastAttemptAt = now
        isProcessingFrame = true

        viewModelScope.launch {
            try {
                val boxSide = minOf(faceBox.width(), faceBox.height())
                val embedding = withContext(Dispatchers.Default) {
                    recognizer.embedFaceBox(bitmap, faceBox, minFacePx = ENROLL_MIN_FACE_PX)
                }

                if (embedding == null) {
                    consecutiveFailures++
                    Log.d(TAG, "embed=null box=${boxSide}px fail=$consecutiveFailures samples=${samples.size}")
                    uiState = uiState.copy(
                        status = progressText(samples.size),
                        error = when {
                            consecutiveFailures >= MIN_FAILURES_TO_HINT && boxSide < ENROLL_MIN_FACE_PX ->
                                "Acerca un poco más el rostro a la cámara."
                            consecutiveFailures >= MIN_FAILURES_TO_HINT ->
                                "No se reconoce el rostro. Buena luz y mira a la cámara."
                            else -> null
                        },
                        scanning = true
                    )
                    return@launch
                }

                val reference = samples.firstOrNull()
                val agreeWith = reference?.let { cosine(it, embedding) } ?: 1f
                val agrees = reference == null || agreeWith >= SAMPLE_AGREE
                if (!agrees) {
                    consecutiveFailures++
                    Log.d(TAG, "disagree=$agreeWith samples=${samples.size}")
                    uiState = uiState.copy(
                        status = progressText(samples.size),
                        error = "Mantén el mismo rostro hasta terminar el registro.",
                        scanning = true
                    )
                    return@launch
                }

                samples.add(embedding)
                consecutiveFailures = 0
                Log.d(TAG, "sample=${samples.size}/$ENROLL_SAMPLES box=${boxSide}px agree=$agreeWith")

                if (samples.size >= ENROLL_SAMPLES) {
                    val saved = withContext(Dispatchers.IO) {
                        store.saveAll(samples.toList())
                        store.hasFace()
                    }
                    Log.d(TAG, "saveAll saved=$saved")
                    if (saved) {
                        samples.clear()
                        uiState = uiState.copy(
                            status = "¡Rostro registrado con éxito!",
                            scanning = false,
                            enrolled = true,
                            done = true,
                            error = null
                        )
                    } else {
                        samples.clear()
                        uiState = uiState.copy(
                            status = "Buscando tu rostro…",
                            error = "No se pudo guardar. Inténtalo de nuevo.",
                            scanning = true
                        )
                    }
                } else {
                    uiState = uiState.copy(
                        status = progressText(samples.size),
                        error = null,
                        scanning = true
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "onFaceFrame error", e)
                uiState = uiState.copy(
                    status = progressText(samples.size),
                    error = "Error al procesar. Inténtalo de nuevo.",
                    scanning = true
                )
            } finally {
                isProcessingFrame = false
            }
        }
    }

    fun resetError() {
        consecutiveFailures = 0
        lastAttemptAt = 0L
        isProcessingFrame = false
        // No borrar muestras parciales salvo que no haya alta en curso.
        if (!uiState.enrolled && samples.isEmpty()) {
            samples.clear()
        }
        uiState = uiState.copy(
            error = null,
            status = if (uiState.enrolled) "Rostro registrado" else progressText(samples.size),
            scanning = !uiState.enrolled,
            enrolled = uiState.enrolled,
            done = false
        )
        if (!uiState.enrolled) {
            uiState = uiState.copy(scanning = true)
        }
    }

    fun clearEnrollment() {
        store.clear()
        samples.clear()
        consecutiveFailures = 0
        lastAttemptAt = 0L
        isProcessingFrame = false
        // Relee disco tras el clear: si aún quedara plantilla, no reabrir cámara.
        val stillThere = store.hasFace()
        Log.d(TAG, "clearEnrollment stillThere=$stillThere")
        uiState = if (stillThere) {
            uiState.copy(
                enrolled = true,
                done = false,
                scanning = false,
                status = "No se pudo eliminar. Inténtalo de nuevo.",
                error = "No se pudo eliminar el rostro guardado."
            )
        } else {
            uiState.copy(
                enrolled = false,
                done = false,
                scanning = true,
                status = "Buscando tu rostro…",
                error = null
            )
        }
    }

    fun updateEnrollment() {
        clearEnrollment()
    }

    private fun progressText(captured: Int): String {
        return "Registrando rostro… $captured/$ENROLL_SAMPLES"
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
        return dot / (kotlin.math.sqrt(na.toDouble()) * kotlin.math.sqrt(nb.toDouble())).toFloat()
    }

    private companion object {
        const val TAG = "FaceEnroll"
        const val COOLDOWN_MS = 200L
        const val MIN_FAILURES_TO_HINT = 3
        /**
         * 3 muestras del mismo rostro: al desbloquear se compara contra la
         * MEJOR de ellas, así el match es mucho más estable que con 1 sola.
         */
        const val ENROLL_SAMPLES = 3
        /** Misma cara entre frames suele superar 0.6; 0.4 deja margen de luz/ángulo. */
        const val SAMPLE_AGREE = 0.40f
        const val ENROLL_MIN_FACE_PX = 40
    }
}
