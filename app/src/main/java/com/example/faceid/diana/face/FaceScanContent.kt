package com.example.faceid.diana.face

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as GeometrySize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.faceid.diana.authentication.AuthHero
import com.example.faceid.kevin.components.AppButton
import com.example.faceid.kevin.components.ErrorBanner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.Executors

@Composable
fun FaceScanContent(
    statusText: String,
    errorText: String?,
    scanning: Boolean,
    onFaceDetected: (Bitmap, Rect) -> Unit,
    onRequestCamera: () -> Unit,
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AuthHero(
            icon = Icons.Default.Face,
            title = "Registra tu rostro",
            subtitle = "Coloca tu cara dentro del óvalo y mantén quieta"
        )

        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        if (errorText != null) {
            ErrorBanner(message = errorText)
        }

        if (!hasPermission) {
            AppButton(
                text = "Permitir cámara",
                onClick = { launcher.launch(Manifest.permission.CAMERA) },
                enabled = true
            )
        } else {
            FaceCameraPreview(
                scanning = scanning,
                onFaceDetected = onFaceDetected,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.Black)
            )
        }

        if (errorText != null) {
            AppButton(text = "Reintentar", onClick = onRetry, enabled = true, isPrimary = false)
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Tu rostro se procesa solo en este dispositivo",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Cámara frontal con detección de rostro para la pantalla de bloqueo.
 * Pide permiso de cámara si hace falta y emite cada fotograma detectado.
 */
@Composable
fun FaceCameraPreview(
    scanning: Boolean,
    onFaceDetected: (Bitmap, Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    if (hasPermission) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(28.dp))
                .background(Color.Black)
        ) {
            CameraPreviewWithFace(
                scanning = scanning,
                onFaceDetected = onFaceDetected
            )
        }
    } else {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            AppButton(
                text = "Permitir cámara",
                onClick = { launcher.launch(Manifest.permission.CAMERA) },
                enabled = true
            )
        }
    }
}

@Composable
private fun CameraPreviewWithFace(
    scanning: Boolean,
    onFaceDetected: (Bitmap, Rect) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val detector = remember {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .build()
        )
    }
    var faceBox by remember { mutableStateOf<Rect?>(null) }
    var lastFaceAt by remember { mutableStateOf(0L) }
    val scanningRef = remember { mutableStateOf(scanning) }
    LaunchedEffect(scanning) { scanningRef.value = scanning }

    // Mantén el óvalo verde ~700ms tras la última detección para que no titilee.
    val showFaceHint = remember(faceBox, lastFaceAt) {
        faceBox != null && (System.currentTimeMillis() - lastFaceAt) < FACE_HOLD_MS
    }
    LaunchedEffect(faceBox) {
        if (faceBox != null) {
            lastFaceAt = System.currentTimeMillis()
        }
    }

    DisposableEffect(Unit) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                val provider = future.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(640, 480))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(executor) { image ->
                    analyzeFrame(image, detector, scanningRef.value) { rect, bmp ->
                        if (rect != null) {
                            faceBox = rect
                            lastFaceAt = System.currentTimeMillis()
                        }
                        if (bmp != null && rect != null) onFaceDetected(bmp, rect)
                    }
                }
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    preview,
                    analysis
                )
                Log.d(TAG, "camera bound")
            } catch (e: Exception) {
                Log.e(TAG, "camera bind failed", e)
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            try {
                ProcessCameraProvider.getInstance(context).get().unbindAll()
            } catch (_: Exception) {
            }
            executor.shutdown()
            detector.close()
        }
    }

    // Refresco periódico para expirar el hint verde sin depender de nuevos frames.
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(300)
            if (faceBox != null && System.currentTimeMillis() - lastFaceAt >= FACE_HOLD_MS) {
                faceBox = null
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidPreview(previewView = previewView)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val ovalW = w * 0.62f
            val ovalH = h * 0.78f
            val left = (w - ovalW) / 2f
            val top = (h - ovalH) / 2f
            val path = Path().apply {
                addOval(
                    androidx.compose.ui.geometry.Rect(
                        Offset(left, top),
                        GeometrySize(ovalW, ovalH)
                    )
                )
            }
            drawPath(
                path = path,
                color = if (faceBox != null) Color(0xFF10B981) else Color.White.copy(alpha = 0.85f),
                style = Stroke(width = 6f)
            )
        }
    }
}

private const val FACE_HOLD_MS = 700L

@Composable
private fun AndroidPreview(previewView: PreviewView) {
    AndroidView(factory = { previewView })
}

private fun analyzeFrame(
    image: ImageProxy,
    detector: com.google.mlkit.vision.face.FaceDetector,
    scanning: Boolean,
    onResult: (Rect?, Bitmap?) -> Unit
) {
    if (!scanning) {
        image.close()
        onResult(null, null)
        return
    }
    if (image.image == null) {
        image.close()
        onResult(null, null)
        return
    }
    val rotation = image.imageInfo.rotationDegrees
    // 1) Bitmap ya rotado (mismo espacio que el preview embebido).
    // 2) Detección SOBRE ese bitmap → el box coincide 1:1 con los píxeles
    //    que se recortan (evita crops de fondo y falsos positivos en el lock).
    val bmp = FrameConverter.toBitmap(image, rotation)
    image.close()
    if (bmp == null) {
        onResult(null, null)
        return
    }
    detector.process(InputImage.fromBitmap(bmp, 0))
        .addOnSuccessListener { faces ->
            val face = faces.maxByOrNull {
                it.boundingBox.width() * it.boundingBox.height()
            }
            if (face == null) {
                onResult(null, null)
            } else {
                onResult(face.boundingBox, bmp)
            }
        }
        .addOnFailureListener {
            onResult(null, null)
        }
}

private const val TAG = "FaceCamera"