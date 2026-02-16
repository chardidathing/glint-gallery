package com.chardidathing.glint.ui.screens.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput

private const val HANDLE_SIZE = 40f
private const val MIN_CROP_SIZE = 80f

private enum class DragHandle {
    TopLeft, TopRight, BottomLeft, BottomRight, Body, None
}

@Composable
fun CropOverlay(
    cropRect: Rect,
    imageRect: Rect,
    onCropChange: (Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    var activeHandle by remember { mutableStateOf(DragHandle.None) }
    val currentCropRect by rememberUpdatedState(cropRect)
    val currentImageRect by rememberUpdatedState(imageRect)
    val currentOnCropChange by rememberUpdatedState(onCropChange)

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { pos ->
                        activeHandle = hitTest(pos, currentCropRect)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newRect = applyDrag(
                            activeHandle, currentCropRect, dragAmount, currentImageRect
                        )
                        currentOnCropChange(newRect)
                    },
                    onDragEnd = { activeHandle = DragHandle.None }
                )
            }
    ) {
        drawDimOverlay(cropRect)
        drawRect(
            color = Color.White,
            topLeft = Offset(cropRect.left, cropRect.top),
            size = Size(cropRect.width, cropRect.height),
            style = Stroke(width = 2f)
        )
        drawThirdsGrid(cropRect)
        drawCornerHandles(cropRect)
    }
}

private fun DrawScope.drawDimOverlay(crop: Rect) {
    val dimColor = Color.Black.copy(alpha = 0.5f)
    // Top strip
    drawRect(dimColor, Offset.Zero, Size(size.width, crop.top))
    // Bottom strip
    drawRect(dimColor, Offset(0f, crop.bottom), Size(size.width, size.height - crop.bottom))
    // Left strip (between top and bottom)
    drawRect(dimColor, Offset(0f, crop.top), Size(crop.left, crop.height))
    // Right strip (between top and bottom)
    drawRect(dimColor, Offset(crop.right, crop.top), Size(size.width - crop.right, crop.height))
}

private fun DrawScope.drawThirdsGrid(crop: Rect) {
    val thirdW = crop.width / 3f
    val thirdH = crop.height / 3f
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
    for (i in 1..2) {
        drawLine(
            Color.White.copy(alpha = 0.4f),
            Offset(crop.left + thirdW * i, crop.top),
            Offset(crop.left + thirdW * i, crop.bottom),
            strokeWidth = 1f,
            pathEffect = dashEffect,
        )
        drawLine(
            Color.White.copy(alpha = 0.4f),
            Offset(crop.left, crop.top + thirdH * i),
            Offset(crop.right, crop.top + thirdH * i),
            strokeWidth = 1f,
            pathEffect = dashEffect,
        )
    }
}

private fun DrawScope.drawCornerHandles(crop: Rect) {
    val handleLen = 24f
    val stroke = Stroke(width = 4f)
    val color = Color.White

    drawLine(color, Offset(crop.left, crop.top), Offset(crop.left + handleLen, crop.top), strokeWidth = stroke.width)
    drawLine(color, Offset(crop.left, crop.top), Offset(crop.left, crop.top + handleLen), strokeWidth = stroke.width)
    drawLine(color, Offset(crop.right, crop.top), Offset(crop.right - handleLen, crop.top), strokeWidth = stroke.width)
    drawLine(color, Offset(crop.right, crop.top), Offset(crop.right, crop.top + handleLen), strokeWidth = stroke.width)
    drawLine(color, Offset(crop.left, crop.bottom), Offset(crop.left + handleLen, crop.bottom), strokeWidth = stroke.width)
    drawLine(color, Offset(crop.left, crop.bottom), Offset(crop.left, crop.bottom - handleLen), strokeWidth = stroke.width)
    drawLine(color, Offset(crop.right, crop.bottom), Offset(crop.right - handleLen, crop.bottom), strokeWidth = stroke.width)
    drawLine(color, Offset(crop.right, crop.bottom), Offset(crop.right, crop.bottom - handleLen), strokeWidth = stroke.width)
}

private fun hitTest(pos: Offset, crop: Rect): DragHandle {
    val hs = HANDLE_SIZE
    if ((pos - Offset(crop.left, crop.top)).getDistance() < hs) return DragHandle.TopLeft
    if ((pos - Offset(crop.right, crop.top)).getDistance() < hs) return DragHandle.TopRight
    if ((pos - Offset(crop.left, crop.bottom)).getDistance() < hs) return DragHandle.BottomLeft
    if ((pos - Offset(crop.right, crop.bottom)).getDistance() < hs) return DragHandle.BottomRight
    if (crop.contains(pos)) return DragHandle.Body
    return DragHandle.None
}

private fun applyDrag(
    handle: DragHandle,
    crop: Rect,
    delta: Offset,
    bounds: Rect,
): Rect {
    return when (handle) {
        DragHandle.TopLeft -> Rect(
            left = (crop.left + delta.x).coerceIn(bounds.left, crop.right - MIN_CROP_SIZE),
            top = (crop.top + delta.y).coerceIn(bounds.top, crop.bottom - MIN_CROP_SIZE),
            right = crop.right,
            bottom = crop.bottom,
        )
        DragHandle.TopRight -> Rect(
            left = crop.left,
            top = (crop.top + delta.y).coerceIn(bounds.top, crop.bottom - MIN_CROP_SIZE),
            right = (crop.right + delta.x).coerceIn(crop.left + MIN_CROP_SIZE, bounds.right),
            bottom = crop.bottom,
        )
        DragHandle.BottomLeft -> Rect(
            left = (crop.left + delta.x).coerceIn(bounds.left, crop.right - MIN_CROP_SIZE),
            top = crop.top,
            right = crop.right,
            bottom = (crop.bottom + delta.y).coerceIn(crop.top + MIN_CROP_SIZE, bounds.bottom),
        )
        DragHandle.BottomRight -> Rect(
            left = crop.left,
            top = crop.top,
            right = (crop.right + delta.x).coerceIn(crop.left + MIN_CROP_SIZE, bounds.right),
            bottom = (crop.bottom + delta.y).coerceIn(crop.top + MIN_CROP_SIZE, bounds.bottom),
        )
        DragHandle.Body -> {
            val dx = delta.x.coerceIn(bounds.left - crop.left, bounds.right - crop.right)
            val dy = delta.y.coerceIn(bounds.top - crop.top, bounds.bottom - crop.bottom)
            crop.translate(dx, dy)
        }
        DragHandle.None -> crop
    }
}
