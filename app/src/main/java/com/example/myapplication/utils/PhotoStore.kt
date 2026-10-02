package com.example.myapplication.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class PhotoStore(private val context: Context) {
    private val directory get() = File(context.filesDir, "progress").apply { mkdirs() }

    suspend fun import(uri: Uri): String = withContext(Dispatchers.IO) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Selecciona una imagen válida." }
        val options = BitmapFactory.Options().apply {
            inSampleSize = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / inSampleSize > 2048) inSampleSize *= 2
        }
        val bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: error("No se pudo abrir la imagen.")
        val orientation = context.contentResolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
        val matrix = Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> { postRotate(90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_TRANSVERSE -> { postRotate(270f); postScale(-1f, 1f) }
            }
        }
        val oriented = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        val file = File(directory, UUID.randomUUID().toString() + ".jpg")
        try {
            file.outputStream().use { check(oriented.compress(Bitmap.CompressFormat.JPEG, 88, it)) }
            // Re-encoding also avoids retaining GPS EXIF from imported photos.
            file.absolutePath
        } catch (e: Exception) {
            file.delete()
            throw e
        } finally {
            if (oriented !== bitmap) oriented.recycle()
            bitmap.recycle()
        }
    }

    suspend fun delete(path: String) = withContext(Dispatchers.IO) {
        val file = File(path).canonicalFile
        require(file.parentFile == directory.canonicalFile)
        check(!file.exists() || file.delete()) { "No se pudo eliminar la foto." }
    }
}
