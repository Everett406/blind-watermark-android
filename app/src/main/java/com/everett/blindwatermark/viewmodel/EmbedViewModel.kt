package com.everett.blindwatermark.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everett.blindwatermark.data.algorithm.WatermarkEngine
import com.everett.blindwatermark.utils.ImageSaver
import com.everett.blindwatermark.utils.loadBitmapFromUri
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class EmbedViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(EmbedUiState())
    val uiState: StateFlow<EmbedUiState> = _uiState

    fun onImageSelected(uri: Uri, context: Context) {
        viewModelScope.launch {
            val compress = _uiState.value.compressImage
            val bitmap = loadBitmapFromUri(context, uri, compress)
            bitmap?.let {
                _uiState.value = _uiState.value.copy(
                    selectedImageUri = uri,
                    selectedBitmap = it,
                    errorMessage = null
                )
            } ?: run {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "无法加载图片"
                )
            }
        }
    }

    fun onCompressChanged(compress: Boolean) {
        _uiState.value = _uiState.value.copy(compressImage = compress)
    }

    fun onWatermarkTextChanged(text: String) {
        _uiState.value = _uiState.value.copy(watermarkText = text)
    }

    fun onPasswordChanged(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
    }

    fun embedWatermark() {
        val currentState = _uiState.value
        val bitmap = currentState.selectedBitmap ?: return
        val text = currentState.watermarkText.trim()
        if (text.isEmpty()) return

        viewModelScope.launch {
            // Clear UI bitmap reference BEFORE processing to prevent Compose from
            // accessing a recycled bitmap during algorithm execution
            _uiState.value = currentState.copy(
                selectedBitmap = null,
                isProcessing = true,
                errorMessage = null
            )

            try {
                val password = currentState.password.takeIf { it.isNotBlank() } ?: ""

                // Move heavy computation to background thread
                val watermarked = withContext(Dispatchers.Default) {
                    WatermarkEngine.embed(bitmap, text, password)
                }

                // Now safe to recycle the original bitmap
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }

                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    resultBitmap = watermarked,
                    isSuccess = true,
                    errorMessage = null
                )
            } catch (e: Throwable) {
                Log.e("EmbedViewModel", "嵌入失败", e)
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    errorMessage = "嵌入失败: ${e.message}"
                )
            }
        }
    }

    fun saveResultToGallery(context: Context) {
        val currentState = _uiState.value
        val resultBitmap = currentState.resultBitmap ?: return

        viewModelScope.launch {
            val fileName = "yinyin_embedded_${System.currentTimeMillis()}.png"
            val saved = ImageSaver.saveToGallery(context, resultBitmap, fileName)
            _uiState.value = currentState.copy(
                isSuccess = saved,
                errorMessage = if (!saved) "保存到相册失败" else null
            )
        }
    }

    fun clearResult() {
        _uiState.value = _uiState.value.copy(
            resultBitmap = null,
            isSuccess = false,
            errorMessage = null
        )
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

data class EmbedUiState(
    val selectedImageUri: Uri? = null,
    val selectedBitmap: Bitmap? = null,
    val watermarkText: String = "",
    val password: String = "",
    val compressImage: Boolean = true,
    val isProcessing: Boolean = false,
    val resultBitmap: Bitmap? = null,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)
