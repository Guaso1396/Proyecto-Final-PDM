package com.example.faceid.diana.face

import android.content.Context
import com.example.faceid.diana.security.SecurityPreferences

/**
 * Almacena las plantillas del rostro registrado (varios vectores 192 floats).
 * Formato: "f1,f2,...;f1,f2,..." — un solo vector si no hay ';'.
 */
class FaceEmbeddingStore(context: Context) {

    private val prefs = SecurityPreferences(context)

    fun save(embedding: FloatArray) {
        saveAll(listOf(embedding))
    }

    fun saveAll(embeddings: List<FloatArray>) {
        val valid = embeddings.filter { it.isNotEmpty() }
        if (valid.isEmpty()) return
        val string = valid.joinToString(";") { it.joinToString(",") }
        prefs.saveFaceEmbedding(string)
    }

    fun load(): FloatArray? {
        return loadAll().firstOrNull()
    }

    fun loadAll(): List<FloatArray> {
        val string = prefs.getFaceEmbedding() ?: return emptyList()
        if (string.isBlank()) return emptyList()
        return try {
            string.split(";").mapNotNull { chunk ->
                if (chunk.isBlank()) return@mapNotNull null
                val arr = chunk.split(",").map { it.trim().toFloat() }.toFloatArray()
                if (arr.size < MIN_DIM) null else arr
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun hasFace(): Boolean {
        return loadAll().isNotEmpty()
    }

    fun clear() {
        // commit=true en SecurityPreferences: el clear debe verse de inmediato
        // en el mismo proceso (Home, FaceEnrollment y Lock leen la misma key).
        prefs.clearFaceEmbedding()
    }

    private companion object {
        const val MIN_DIM = 32
    }
}
