package com.example.faceid.diana.face

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

/**
 * Convierte frames YUV_420_888 de CameraX a Bitmap listo para ML Kit/TFLite.
 * Respeta rowStride/pixelStride de cada plano (la conversión ingenua con
 * buffers.contiguous produce imágenes corruptas en muchos dispositivos y
 * ML Kit deja de detectar el rostro).
 */
object FrameConverter {

    /** Devuelve el frame como Bitmap ya rotado a orientación de pantalla. */
    fun toBitmap(proxy: ImageProxy, rotationDegrees: Int): Bitmap? {
        return try {
            val nv21 = yuv420ToNv21(proxy)
            val yuv = YuvImage(nv21, ImageFormat.NV21, proxy.width, proxy.height, null)
            val out = ByteArrayOutputStream()
            yuv.compressToJpeg(Rect(0, 0, proxy.width, proxy.height), 85, out)
            val bytes = out.toByteArray()
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            if (rotationDegrees % 360 == 0) {
                bmp
            } else {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun yuv420ToNv21(proxy: ImageProxy): ByteArray {
        val width = proxy.width
        val height = proxy.height
        val out = ByteArray(width * height * 3 / 2)

        val yPlane = proxy.planes[0]
        val yBuf = yPlane.buffer
        val yRow = yPlane.rowStride
        val yPix = yPlane.pixelStride
        var pos = 0
        for (row in 0 until height) {
            val base = row * yRow
            if (yPix == 1) {
                for (col in 0 until width) {
                    out[pos++] = yBuf.get(base + col)
                }
            } else {
                for (col in 0 until width) {
                    out[pos++] = yBuf.get(base + col * yPix)
                }
            }
        }

        val uPlane = proxy.planes[1]
        val vPlane = proxy.planes[2]
        val uBuf = uPlane.buffer
        val vBuf = vPlane.buffer
        val uRow = uPlane.rowStride
        val uPix = uPlane.pixelStride
        val vRow = vPlane.rowStride
        val vPix = vPlane.pixelStride
        val halfW = width / 2
        val halfH = height / 2
        for (row in 0 until halfH) {
            val uBase = row * uRow
            val vBase = row * vRow
            for (col in 0 until halfW) {
                out[pos++] = vBuf.get(vBase + col * vPix)
                out[pos++] = uBuf.get(uBase + col * uPix)
            }
        }
        return out
    }
}
