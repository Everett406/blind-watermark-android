package com.everett.blindwatermark.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.io.InputStream

/**
 * Image picker helper for Compose
 */
class ImagePicker(
    private val onImagePicked: (Bitmap) -> Unit
) {
    private lateinit var launcher: androidx.activity.result.ActivityResultLauncher<String>

    fun pickImage() {
        launcher.launch("image/*")
    }

    internal fun setLauncher(l: androidx.activity.result.ActivityResultLauncher<String>) {
        launcher = l
    }
}

@Composable
fun rememberImagePicker(onImagePicked: (Bitmap) -> Unit): ImagePicker {
    val picker = remember { ImagePicker(onImagePicked) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // Bitmap will be loaded when needed
        }
    }

    picker.setLauncher(launcher)
    return picker
}

/**
 * Load bitmap from URI with size limiting to prevent OOM
 * Max dimension: 1024px (sufficient for watermarking, prevents OOM)
 */
fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        val contentResolver = context.contentResolver

        // First decode bounds only
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }

        val maxDimension = 1024
        val scale = calculateInSampleSize(options, maxDimension, maxDimension)

        // Decode with scaling
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = scale
            // Prefer RGB_565 to save memory (no alpha needed for watermarking)
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        }
    } catch (e: Throwable) {
        Log.e("ImagePicker", "加载图片失败", e)
        null
    }
}

private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val height = options.outHeight
    val width = options.outWidth
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2

        // Calculate the largest inSampleSize value that is a power of 2 and keeps
        // both height and width larger than the requested height and width.
        while (halfHeight / inSampleSize >= reqHeight || halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}
