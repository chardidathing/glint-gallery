package com.chardidathing.glint.ui.screens.viewer

import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun ZoomableImage(
    uri: Uri,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    onTap: () -> Unit = {},
    onZoomChange: (Boolean) -> Unit = {},
    onVerticalDrag: (Float) -> Unit = {},
    onVerticalDragEnd: (Float) -> Unit = {},
) {
    val scaleAnim = remember { Animatable(1f) }
    val offsetXAnim = remember { Animatable(0f) }
    val offsetYAnim = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(scaleAnim.value) {
        onZoomChange(scaleAnim.value > 1.01f)
    }

    AsyncImage(
        model = uri,
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                val touchSlop = viewConfiguration.touchSlop
                var lastTapTime = 0L
                var pendingTapJob: Job? = null

                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var totalDragX = 0f
                    var totalDragY = 0f
                    var directionDecided = false
                    var isVerticalDrag = false
                    var wasPinching = false
                    var hasDragged = false

                    do {
                        val event = awaitPointerEvent()
                        if (event.changes.size >= 2) {
                            wasPinching = true
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val newScale = (scaleAnim.value * zoom).coerceIn(1f, 5f)
                            val maxX = (newScale - 1f) * size.width / 2f
                            val maxY = (newScale - 1f) * size.height / 2f
                            scope.launch {
                                scaleAnim.snapTo(newScale)
                                if (newScale == 1f) {
                                    offsetXAnim.snapTo(0f)
                                    offsetYAnim.snapTo(0f)
                                } else {
                                    offsetXAnim.snapTo(
                                        (offsetXAnim.value + pan.x).coerceIn(-maxX, maxX)
                                    )
                                    offsetYAnim.snapTo(
                                        (offsetYAnim.value + pan.y).coerceIn(-maxY, maxY)
                                    )
                                }
                            }
                            event.changes.forEach { it.consume() }
                        } else if (scaleAnim.value > 1.01f) {
                            val pan = event.calculatePan()
                            if (pan != Offset.Zero) hasDragged = true
                            val maxX = (scaleAnim.value - 1f) * size.width / 2f
                            val maxY = (scaleAnim.value - 1f) * size.height / 2f
                            scope.launch {
                                offsetXAnim.snapTo(
                                    (offsetXAnim.value + pan.x).coerceIn(-maxX, maxX)
                                )
                                offsetYAnim.snapTo(
                                    (offsetYAnim.value + pan.y).coerceIn(-maxY, maxY)
                                )
                            }
                            event.changes.forEach { it.consume() }
                        } else {
                            val pan = event.calculatePan()
                            totalDragX += pan.x
                            totalDragY += pan.y

                            if (!directionDecided) {
                                if (abs(totalDragX) > touchSlop || abs(totalDragY) > touchSlop) {
                                    directionDecided = true
                                    isVerticalDrag = abs(totalDragY) > abs(totalDragX)
                                    hasDragged = true
                                }
                            }

                            if (directionDecided && isVerticalDrag) {
                                event.changes.forEach { it.consume() }
                                onVerticalDrag(totalDragY)
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    if (directionDecided && isVerticalDrag) {
                        onVerticalDragEnd(totalDragY)
                    } else if (!hasDragged && !wasPinching) {
                        val now = System.currentTimeMillis()
                        if (now - lastTapTime < 300) {
                            // Double tap
                            pendingTapJob?.cancel()
                            lastTapTime = 0L
                            scope.launch {
                                if (scaleAnim.value > 1.01f) {
                                    launch { scaleAnim.animateTo(1f) }
                                    launch { offsetXAnim.animateTo(0f) }
                                    launch { offsetYAnim.animateTo(0f) }
                                } else {
                                    scaleAnim.animateTo(2.5f)
                                }
                            }
                        } else {
                            lastTapTime = now
                            pendingTapJob = scope.launch {
                                delay(300)
                                onTap()
                            }
                        }
                    }
                }
            }
            .graphicsLayer(
                scaleX = scaleAnim.value,
                scaleY = scaleAnim.value,
                translationX = offsetXAnim.value,
                translationY = offsetYAnim.value,
            )
    )
}
