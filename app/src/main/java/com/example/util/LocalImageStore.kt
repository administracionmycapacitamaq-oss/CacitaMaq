package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object LocalImageStore {

    suspend fun copyPickedImageToInternalStorage(
        context: Context,
        sourceUri: Uri,
        slotKey: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, "images").apply { mkdirs() }
            val inputStream = context.contentResolver.openInputStream(sourceUri)
                ?: return@withContext Result.failure(IllegalStateException("No se pudo leer la imagen seleccionada."))

            val decoded = inputStream.use { BitmapFactory.decodeStream(it) }
                ?: return@withContext Result.failure(IllegalStateException("Formato de imagen no soportado."))

            val scaled = scaleBitmapIfNeeded(decoded, maxDimension = 1024)
            val outFile = File(dir, "${slotKey}_${UUID.randomUUID().toString().take(8)}.jpg")
            FileOutputStream(outFile).use { fos ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 88, fos)
            }
            if (scaled !== decoded) {
                decoded.recycle()
            }
            Result.success(outFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun decodeImageFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun scaleBitmapIfNeeded(source: Bitmap, maxDimension: Int): Bitmap {
        val maxSide = maxOf(source.width, source.height)
        if (maxSide <= maxDimension) return source
        val scale = maxDimension.toFloat() / maxSide.toFloat()
        return Bitmap.createScaledBitmap(
            source,
            (source.width * scale).toInt().coerceAtLeast(1),
            (source.height * scale).toInt().coerceAtLeast(1),
            true
        )
    }
}
