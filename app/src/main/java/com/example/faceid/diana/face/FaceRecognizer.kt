package com.example.faceid.diana.face

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class FaceRecognizer(private val context: Context) {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .build()
    )

    @Volatile
    private var cachedInterpreter: Interpreter? = null

    /**
     * Embebe usando el bounding box ya detectado en la cámara
     * (misma detección que dibuja el óvalo). Evita una 2ª pasada de ML Kit
     * y garantiza que alta y lock usen exactamente el mismo recorte.
     * Devuelve null si la cara es muy chica o el crop falla.
     */
    fun embedFaceBox(bitmap: Bitmap, box: Rect, minFacePx: Int = MIN_FACE_PX): FloatArray? {
        if (box.width() < minFacePx || box.height() < minFacePx) {
            Log.d(TAG, "embedFaceBox reject small ${box.width()}x${box.height()} min=$minFacePx")
            return null
        }
        val cropped = cropAndEmbed(bitmap, box) ?: return null
        return cropped
    }

    /**
     * Extrae el embedding del rostro desde el [bitmap] con una 2ª detección
     * de ML Kit. [requireFaceDetected]=true → solo si hay cara y tamaño ok.
     */
    fun embedFromBitmap(
        bitmap: Bitmap,
        requireFaceDetected: Boolean = false,
        minFacePx: Int = MIN_FACE_PX
    ): FloatArray? {
        return try {
            val input = InputImage.fromBitmap(bitmap, 0)
            val latch = CountDownLatch(1)
            var embedding: FloatArray? = null

            detector.process(input)
                .addOnSuccessListener { faces ->
                    val face = faces.maxByOrNull {
                        it.boundingBox.width() * it.boundingBox.height()
                    }
                    embedding = when {
                        face == null -> {
                            if (requireFaceDetected) null else embedFaceCrop(bitmap)
                        }
                        else -> {
                            val sizeOk = face.boundingBox.width() >= minFacePx &&
                                face.boundingBox.height() >= minFacePx
                            if (requireFaceDetected && !sizeOk) {
                                null
                            } else {
                                cropAndEmbed(bitmap, face.boundingBox)
                                    ?: embedFaceCrop(bitmap)
                            }
                        }
                    }
                    latch.countDown()
                }
                .addOnFailureListener {
                    embedding = if (requireFaceDetected) null else embedFaceCrop(bitmap)
                    latch.countDown()
                }

            if (!latch.await(3, TimeUnit.SECONDS)) {
                return embedding ?: if (requireFaceDetected) null else embedFaceCrop(bitmap)
            }
            embedding ?: if (requireFaceDetected) null else embedFaceCrop(bitmap)
        } catch (_: Exception) {
            if (requireFaceDetected) null else embedFaceCrop(bitmap)
        }
    }

    fun matchFromCamera(
        stored: FloatArray,
        lifecycleOwner: LifecycleOwner? = null
    ): Boolean {
        val owner = lifecycleOwner ?: return false
        return try {
            val provider = ProcessCameraProvider.getInstance(context).get(3, TimeUnit.SECONDS)
            val executor = Executors.newSingleThreadExecutor()
            val latch = CountDownLatch(1)
            var matched = false

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(executor) { image ->
                if (latch.count == 0L) {
                    image.close()
                    return@setAnalyzer
                }
                val rotation = image.imageInfo.rotationDegrees
                val mediaImg = image.image
                if (mediaImg == null) {
                    image.close()
                    return@setAnalyzer
                }
                val input = InputImage.fromMediaImage(mediaImg, rotation)
                detector.process(input)
                    .addOnSuccessListener { faces ->
                        val face = faces.maxByOrNull {
                            it.boundingBox.width() * it.boundingBox.height()
                        }
                        if (face != null) {
                            val bmp = FrameConverter.toBitmap(image, rotation)
                            if (bmp != null) {
                                val emb = cropAndEmbed(bmp, face.boundingBox)
                                    ?: embedFaceCrop(bmp)
                                if (emb != null) {
                                    matched = cosine(stored, emb) >= STRICT_THRESHOLD
                                }
                            }
                        }
                        image.close()
                        latch.countDown()
                    }
                    .addOnFailureListener {
                        image.close()
                        latch.countDown()
                    }
            }

            val preview = Preview.Builder().build()
            provider.unbindAll()
            provider.bindToLifecycle(
                owner,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                preview,
                analysis
            )
            val got = latch.await(CAPTURE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            provider.unbindAll()
            executor.shutdown()
            got && matched
        } catch (_: Exception) {
            false
        }
    }

    fun matchFromCameraBlocking(stored: FloatArray): Boolean = matchFromCamera(stored, null)

    private fun cropAndEmbed(bitmap: Bitmap, box: Rect): FloatArray? {
        val padX = (box.width() * 0.10f).toInt()
        val padY = (box.height() * 0.10f).toInt()

        val left = (box.left - padX).coerceIn(0, bitmap.width - 1)
        val top = (box.top - padY).coerceIn(0, bitmap.height - 1)
        val right = (box.right + padX).coerceIn(left + 1, bitmap.width)
        val bottom = (box.bottom + padY).coerceIn(top + 1, bitmap.height)

        val width = right - left
        val height = bottom - top
        if (width <= 0 || height <= 0) return null

        return try {
            val faceBmp = Bitmap.createBitmap(bitmap, left, top, width, height)
            embedFaceCrop(faceBmp)
        } catch (_: Exception) {
            null
        }
    }

    private fun embedFaceCrop(face: Bitmap): FloatArray? {
        val interpreter = loadInterpreter()
        if (interpreter == null) {
            Log.e(TAG, "embedFaceCrop: interpreter null (model load failed)")
            return null
        }
        return try {
            val inputSize = INPUT_SIZE
            val scaled = Bitmap.createScaledBitmap(face, inputSize, inputSize, true)
            val input = ByteBuffer.allocateDirect(inputSize * inputSize * 3 * 4)
                .order(ByteOrder.nativeOrder())
            val pixels = IntArray(inputSize * inputSize)
            scaled.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
            for (p in pixels) {
                input.putFloat(((p shr 16 and 0xFF) - 127.5f) / 127.5f)
                input.putFloat(((p shr 8 and 0xFF) - 127.5f) / 127.5f)
                input.putFloat(((p and 0xFF) - 127.5f) / 127.5f)
            }
            input.rewind()

            val outShape = interpreter.getOutputTensor(0).shape()
            val outSize = outShape.lastOrNull()?.takeIf { it > 0 } ?: EMBEDDING_SIZE
            val output = Array(1) { FloatArray(outSize) }
            interpreter.run(input, output)

            val raw = output[0]
            if (raw.all { it == 0f || it.isNaN() }) {
                Log.e(TAG, "embedFaceCrop: all-zero/NaN output shape=${outShape.toList()}")
                return null
            }
            normalize(raw)
        } catch (e: Exception) {
            Log.e(TAG, "embedFaceCrop failed", e)
            null
        }
    }

    fun hasModel(): Boolean {
        return try {
            context.assets.openFd(MODEL_ASSET).close()
            true
        } catch (_: Exception) {
            try {
                context.assets.open(MODEL_ASSET).use { it.read(ByteArray(4)) }
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    private fun loadInterpreter(): Interpreter? {
        cachedInterpreter?.let { return it }
        synchronized(this) {
            cachedInterpreter?.let { return it }
            val options = Interpreter.Options().apply { setNumThreads(2) }

            // 1) Asset sin comprimir (noCompress tflite)
            try {
                val afd = context.assets.openFd(MODEL_ASSET)
                val stream = FileInputStream(afd.fileDescriptor)
                val buffer = ByteBuffer.allocateDirect(afd.declaredLength.toInt())
                    .order(ByteOrder.nativeOrder())
                stream.channel.read(buffer)
                buffer.rewind()
                val interp = Interpreter(buffer, options)
                cachedInterpreter = interp
                Log.d(TAG, "model loaded from fd size=${afd.declaredLength}")
                return interp
            } catch (e: Exception) {
                Log.w(TAG, "openFd failed: ${e.message}")
            }

            // 2) Leer el asset completo por stream (si viene comprimido)
            return try {
                val bytes = context.assets.open(MODEL_ASSET).use { it.readBytes() }
                val buffer = ByteBuffer.allocateDirect(bytes.size)
                    .order(ByteOrder.nativeOrder())
                buffer.put(bytes)
                buffer.rewind()
                val interp = Interpreter(buffer, options)
                cachedInterpreter = interp
                Log.d(TAG, "model loaded from stream size=${bytes.size}")
                interp
            } catch (e: Exception) {
                Log.e(TAG, "model load failed", e)
                null
            }
        }
    }

    private fun normalize(v: FloatArray): FloatArray {
        var sum = 0f
        for (x in v) sum += x * x
        val norm = kotlin.math.sqrt(sum.toDouble()).toFloat().coerceAtLeast(1e-6f)
        for (i in v.indices) v[i] /= norm
        return v
    }

    private fun cosine(a: FloatArray, b: FloatArray): Float {
        val n = minOf(a.size, b.size)
        var dot = 0f
        for (i in 0 until n) dot += a[i] * b[i]
        return dot
    }

    companion object {
        const val TAG = "FaceRecognizer"
        const val MODEL_ASSET = "model.tflite"
        const val INPUT_SIZE = 112
        const val EMBEDDING_SIZE = 192

        /** Umbral laxo: cualquier rostro válido desbloquea (versión original). */
        const val THRESHOLD = 0.55f

        /** Reservado para el modo estricto (no usado en el lock actual). */
        const val STRICT_THRESHOLD = 0.90f

        /** Lado mínimo del bounding box en lock (igual que el alta). */
        const val MIN_FACE_PX = 50

        const val CAPTURE_TIMEOUT_MS = 8_000L
    }
}
