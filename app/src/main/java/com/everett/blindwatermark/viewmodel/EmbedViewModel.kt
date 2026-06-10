package com.everett.blindwatermark.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everett.blindwatermark.data.algorithm.WatermarkEngine
import com.everett.blindwatermark.utils.ImageSaver
import com.everett.blindwatermark.utils.loadBitmapFromUri
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmbedViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(EmbedUiState())
    val uiState: StateFlow<EmbedUiState> = _uiState

    fun onImageSelected(uri: Uri, context: Context) {
        viewModelScope.launch {
            val bitmap = loadBitmapFromUri(context, uri)
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

    fun onWatermarkTextChanged(text: String) {
        _uiState.value = _uiState.value.copy(watermarkText = text)
    }

    fun onPasswordChanged(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
    }

    fun embedWatermark(context: Context) {
        val currentState = _uiState.value
        val bitmap = currentState.selectedBitmap ?: return
        val text = currentState.watermarkText.trim()
        if (text.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = currentState.copy(isProcessing = true, errorMessage = null)

            try {
                val password = currentState.password.takeIf { it.isNotBlank() } ?: ""
                val watermarked = WatermarkEngine.embed(bitmap, text, password)

                val fileName = "yinyin_embedded_${System.currentTimeMillis()}.png"
                val saved = ImageSaver.saveToGallery(context, watermarked, fileName)

                _uiState.value = currentState.copy(
                    isProcessing = false,
                    resultBitmap = watermarked,
                    isSuccess = saved,
                    errorMessage = if (!saved) "水印已生成但保存失败" else null
                )
            } catch (e: Exception) {
                _uiState.value = currentState.copy(
                    isProcessing = false,
                    errorMessage = "嵌入失败: ${e.message}"
                )
            }
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
    val isProcessing: Boolean = false,
    val resultBitmap: Bitmap? = null,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)
