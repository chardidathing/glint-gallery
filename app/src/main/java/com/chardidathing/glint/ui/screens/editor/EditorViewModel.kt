package com.chardidathing.glint.ui.screens.editor

import android.app.Application
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val _bitmap = MutableStateFlow<Bitmap?>(null)
    val bitmap: StateFlow<Bitmap?> = _bitmap.asStateFlow()

    private val _rotation = MutableStateFlow(0f)
    val rotation: StateFlow<Float> = _rotation.asStateFlow()

    private val _flipH = MutableStateFlow(false)
    val flipH: StateFlow<Boolean> = _flipH.asStateFlow()

    private val _flipV = MutableStateFlow(false)
    val flipV: StateFlow<Boolean> = _flipV.asStateFlow()

    private val _cropRect = MutableStateFlow<Rect?>(null)
    val cropRect: StateFlow<Rect?> = _cropRect.asStateFlow()

    private val _isCropping = MutableStateFlow(false)
    val isCropping: StateFlow<Boolean> = _isCropping.asStateFlow()

    fun loadImage(uri: Uri) {
        val context = getApplication<Application>()
        context.contentResolver.openInputStream(uri)?.use { stream ->
            _bitmap.value = BitmapFactory.decodeStream(stream)
        }
        _rotation.value = 0f
        _flipH.value = false
        _flipV.value = false
        _cropRect.value = null
    }

    fun rotateLeft() {
        _rotation.value = (_rotation.value - 90f) % 360f
    }

    fun rotateRight() {
        _rotation.value = (_rotation.value + 90f) % 360f
    }

    fun toggleFlipH() {
        _flipH.value = !_flipH.value
    }

    fun toggleFlipV() {
        _flipV.value = !_flipV.value
    }

    fun toggleCropMode() {
        _isCropping.value = !_isCropping.value
        if (!_isCropping.value) {
            _cropRect.value = null
        }
    }

    fun updateCropRect(rect: Rect) {
        _cropRect.value = rect
    }

    fun applyCrop(displayedRect: Rect) {
        val bmp = _bitmap.value ?: return
        val rect = _cropRect.value ?: return

        val scaleX = bmp.width.toFloat() / displayedRect.width
        val scaleY = bmp.height.toFloat() / displayedRect.height

        val x = ((rect.left - displayedRect.left) * scaleX).toInt().coerceIn(0, bmp.width - 1)
        val y = ((rect.top - displayedRect.top) * scaleY).toInt().coerceIn(0, bmp.height - 1)
        val w = (rect.width * scaleX).toInt().coerceAtMost(bmp.width - x)
        val h = (rect.height * scaleY).toInt().coerceAtMost(bmp.height - y)

        if (w > 0 && h > 0) {
            _bitmap.value = Bitmap.createBitmap(bmp, x, y, w, h)
        }
        _cropRect.value = null
        _isCropping.value = false
    }

    fun save(): Uri? {
        val bmp = _bitmap.value ?: return null
        val matrix = Matrix().apply {
            postRotate(_rotation.value)
            postScale(
                if (_flipH.value) -1f else 1f,
                if (_flipV.value) -1f else 1f,
            )
        }
        val transformed = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)

        val context = getApplication<Application>()
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "Glint_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Glint")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return null

        context.contentResolver.openOutputStream(uri)?.use { out ->
            transformed.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }

        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        context.contentResolver.update(uri, values, null, null)

        return uri
    }
}
