package com.everett.blindwatermark.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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
 * Load bitmap from URI
 */
fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        BitmapFactory.decodeStream(inputStream)
    } catch (_: Exception) {
        null
    }
}
