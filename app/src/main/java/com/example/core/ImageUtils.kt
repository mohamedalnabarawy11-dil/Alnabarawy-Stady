package com.example.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import java.io.File
import kotlin.math.roundToInt

private const val TAG = "ImageUtils"

object ImageUtils {

    fun decodeUriToSafeBitmap(context: Context, uri: Uri, maxSide: Int = 1280): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxSide)
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inMutable = true
            }
            val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, opts)
            } ?: return null

            val rotation = readExifRotationFromUri(context, uri)
            normalizeBitmap(decoded, rotation, maxSide)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode URI bitmap", e)
            null
        }
    }

    fun decodeFileToSafeBitmap(file: File, maxSide: Int = 1280): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxSide)
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inMutable = true
            }
            val decoded = BitmapFactory.decodeFile(file.absolutePath, opts) ?: return null
            val rotation = readExifRotationFromFile(file.absolutePath)
            normalizeBitmap(decoded, rotation, maxSide)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode file bitmap", e)
            null
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxSide: Int): Int {
        var inSampleSize = 1
        var halfW = width
        var halfH = height
        while (halfW > maxSide || halfH > maxSide) {
            inSampleSize *= 2
            halfW /= 2
            halfH /= 2
        }
        return inSampleSize.coerceAtLeast(1)
    }

    private fun readExifRotationFromUri(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                exifOrientationToDegrees(
                    exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                )
            } ?: 0
        } catch (_: Exception) {
            0
        }
    }

    private fun readExifRotationFromFile(path: String): Int {
        return try {
            val exif = ExifInterface(path)
            exifOrientationToDegrees(
                exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            )
        } catch (_: Exception) {
            0
        }
    }

    private fun exifOrientationToDegrees(orientation: Int): Int = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }

    private fun normalizeBitmap(bitmap: Bitmap, rotationDegrees: Int, maxSide: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val scale = if (w > maxSide || h > maxSide) {
            minOf(maxSide.toFloat() / w, maxSide.toFloat() / h)
        } else 1f

        if (rotationDegrees == 0 && scale >= 0.99f) return bitmap

        val matrix = Matrix().apply {
            if (scale < 0.99f) postScale(scale, scale)
            if (rotationDegrees != 0) postRotate(rotationDegrees.toFloat())
        }
        val transformed = Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true)
        if (transformed !== bitmap) {
            bitmap.recycle()
        }
        return transformed
    }
}
