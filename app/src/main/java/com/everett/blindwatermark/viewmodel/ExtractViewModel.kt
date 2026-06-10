package com.everett.blindwatermark.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everett.blindwatermark.data.algorithm.WatermarkEngine
import com.everett.blindwatermark.utils.loadBitmapFromUri
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ExtractViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(ExtractUiState())
    val uiState: StateFlow<ExtractUiState> = _uiState

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

    fun onPasswordChanged(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
    }

    fun extractWatermark() {
        val currentState = _uiState.value
        val bitmap = currentState.selectedBitmap ?: return

        viewModelScope.launch {
            _uiState.value = currentState.copy(isProcessing = true, errorMessage = null, extractedText = null)

            try {
                val password = currentState.password.takeIf { it.isNotBlank() } ?: ""

                // Move heavy computation to background thread
                val extracted = withContext(Dispatchers.Default) {
                    WatermarkEngine.extract(bitmap, password)
                }

                _uiState.value = currentState.copy(
                    isProcessing = false,
                    extractedText = extracted,
                    isSuccess = extracted != null,
                    errorMessage = if (extracted == null) "未检测到水印或密码错误" else null
                )
            } catch (e: Throwable) {
                Log.e("ExtractViewModel", "提取失败", e)
                _uiState.value = currentState.copy(
                    isProcessing = false,
                    errorMessage = "提取失败: ${e.message}"
                )
            }
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

data class ExtractUiState(
    val selectedImageUri: Uri? = null,
    val selectedBitmap: Bitmap? = null,
    val password: String = "",
    val isProcessing: Boolean = false,
    val extractedText: String? = null,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)
