package com.example.ecozaschitnik.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

object PhotoPreparer {

    private const val MAX_SIDE_PX = 1280
    private const val JPEG_QUALITY = 82

    suspend fun prepareForUpload(context: Context, sourceUri: Uri): File =
        withContext(Dispatchers.IO) {
            val cacheDir = cacheDir(context)
            val rawTemp = File(cacheDir, "raw_${UUID.randomUUID()}.tmp")
            val target = File(cacheDir, "${UUID.randomUUID()}.jpg")

            try {
                context.contentResolver.openInputStream(sourceUri).use { input ->
                    requireNotNull(input) { "Не удалось открыть файл фото" }
                    rawTemp.outputStream().use { output -> input.copyTo(output) }
                }
                compressFile(rawTemp, target)
                target
            } finally {
                deleteQuietly(rawTemp)
            }
        }

    fun deleteQuietly(file: File?) {
        if (file == null) return
        runCatching { if (file.exists()) file.delete() }
    }

    private fun cacheDir(context: Context): File =
        File(context.cacheDir, "report_photos").apply { mkdirs() }

    private fun compressFile(sourceFile: File, target: File) {
        val rotation = readRotationDegrees(sourceFile)
        var decoded = decodeSampledBitmap(sourceFile, MAX_SIDE_PX)
            ?: throw IllegalStateException("Не удалось прочитать изображение")
        decoded = scaleDownIfNeeded(decoded, MAX_SIDE_PX)
        if (rotation != 0) {
            val rotated = rotateBitmap(decoded, rotation)
            if (rotated !== decoded) {
                decoded.recycle()
            }
            decoded = rotated
        }
        try {
            FileOutputStream(target).use { output ->
                if (!decoded.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)) {
                    throw IllegalStateException("Не удалось сохранить сжатое фото")
                }
            }
        } finally {
            decoded.recycle()
        }
    }

    private fun decodeSampledBitmap(file: File, maxSidePx: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, maxSidePx)
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        return BitmapFactory.decodeFile(file.absolutePath, options)
    }

    private fun calculateSampleSize(width: Int, height: Int, maxSidePx: Int): Int {
        var sampleSize = 1
        val longest = max(width, height)
        while (longest / sampleSize > maxSidePx) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxSidePx: Int): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= maxSidePx) return bitmap
        val scale = maxSidePx.toFloat() / longest
        val width = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
        val height = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(bitmap, width, height, true)
        if (scaled !== bitmap) {
            bitmap.recycle()
        }
        return scaled
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun readRotationDegrees(sourceFile: File): Int =
        runCatching {
            when (
                ExifInterface(sourceFile.absolutePath).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            ) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        }.getOrDefault(0)
}
