package com.uncode.app.data

import android.content.Context
import com.google.mediapipe.tasks.components.containers.Embedding
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder.TextEmbedderOptions
import java.util.concurrent.ConcurrentHashMap

object SemanticEmbeddingEngine {

    @Volatile
    private var embedder: TextEmbedder? = null

    @Volatile
    private var isInitialized = false

    private val vectorCache = ConcurrentHashMap<String, FloatArray>(256)

    fun init(context: Context) {
        if (isInitialized && embedder != null) return

        synchronized(this) {
            if (isInitialized && embedder != null) return

            try {
                val baseOptions = BaseOptions.builder()
                    .setModelAssetPath("models/universal_sentence_encoder.tflite")
                    .build()

                val options = TextEmbedderOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setL2Normalize(true)
                    .build()

                embedder = TextEmbedder.createFromOptions(context.applicationContext, options)
                isInitialized = true
            } catch (e: Exception) {
                embedder = null
                isInitialized = false
            }
        }
    }

    fun getEmbedding(text: String): Embedding? {
        val emb = embedder ?: return null
        return try {
            val result = emb.embed(text)
            result.embeddingResult().embeddings().firstOrNull()
        } catch (e: Exception) {
            null
        }
    }

    fun getVector(text: String): FloatArray? {
        val clean = text.trim().lowercase()
        if (clean.isBlank()) return null
        vectorCache[clean]?.let { return it }
        val emb = embedder ?: return null
        return try {
            val result = emb.embed(clean)
            val arr = result.embeddingResult().embeddings().firstOrNull()?.floatEmbedding()
            if (arr != null) {
                vectorCache[clean] = arr
            }
            arr
        } catch (e: Exception) {
            null
        }
    }

    fun computeSimilarity(text1: String, text2: String): Float {
        val clean1 = text1.trim()
        val clean2 = text2.trim()
        if (clean1.isBlank() || clean2.isBlank()) return 0f
        if (clean1.equals(clean2, ignoreCase = true)) return 1.0f
        if (embedder == null) return -1f

        val vec1 = getVector(clean1) ?: return -1f
        val vec2 = getVector(clean2) ?: return -1f
        val len = minOf(vec1.size, vec2.size)
        var dot = 0f
        for (i in 0 until len) {
            dot += vec1[i] * vec2[i]
        }
        return dot
    }

    fun isAvailable(): Boolean = embedder != null
}

