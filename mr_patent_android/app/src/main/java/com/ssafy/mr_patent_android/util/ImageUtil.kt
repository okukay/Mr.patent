package com.ssafy.mr_patent_android.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class ImageUtil {
    fun rotateBitmapIfNeeded(context: Context, bitmap: Bitmap, imageUri: Uri): Bitmap {
        val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
        inputStream?.use {
            val exif = ExifInterface(it)
            val rotationAngle = getRotationAngle(exif)
            return rotateBitmap(bitmap, rotationAngle)
        }
        return bitmap
    }

    private fun getRotationAngle(exif: ExifInterface): Int {
        return when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, angle: Int): Bitmap {
        if (angle == 0) return bitmap

        val matrix = Matrix().apply {
            postRotate(angle.toFloat())
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun bitmapToUri(context: Context, bitmap: Bitmap, extension: String): Uri {
        val fileName = "rotated_${System.currentTimeMillis()}.$extension"
        val file = File(context.cacheDir, fileName)
        val outputStream = FileOutputStream(file)

        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)

        outputStream.flush()
        outputStream.close()

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height

        val scale = if (originalWidth > originalHeight) {
            maxWidth.toFloat() / originalWidth
        } else {
            maxHeight.toFloat() / originalHeight
        }

        val scaledWidth = (originalWidth * scale).toInt()
        val scaledHeight = (originalHeight * scale).toInt()

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
        return scaledBitmap
    }
}