package com.chardidathing.glint.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.chardidathing.glint.data.model.DateGroup
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun TimelineScroller(
    gridState: LazyGridState,
    dateGroups: List<DateGroup>,
    modifier: Modifier = Modifier,
    onLeft: Boolean = false,
) {
    val totalItemCount = remember(dateGroups) {
        dateGroups.sumOf { 1 + it.items.size }
    }

    if (totalItemCount == 0) return

    val headerPositions = remember(dateGroups) {
        buildList {
            var index = 0
            val fmt = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            for (group in dateGroups) {
                val date = group.items.firstOrNull()?.effectiveDate ?: 0L
                add(index to fmt.format(Date(date)))
                index += 1 + group.items.size
            }
        }
    }

    val scrollFraction by remember {
        derivedStateOf {
            if (totalItemCount <= 1) 0f
            else gridState.firstVisibleItemIndex.toFloat() / (totalItemCount - 1)
        }
    }

    val currentLabel by remember {
        derivedStateOf {
            val idx = gridState.firstVisibleItemIndex
            headerPositions.lastOrNull { it.first <= idx }?.second ?: ""
        }
    }

    var isDragging by remember { mutableStateOf(false) }
    val isScrolling = gridState.isScrollInProgress
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(isScrolling, isDragging) {
        if (isScrolling || isDragging) {
            visible = true
        } else {
            delay(1500)
            visible = false
        }
    }

    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val thumbHeightDp = 48.dp
    val thumbHeightPx = with(density) { thumbHeightDp.toPx() }

    val sideAlignment = if (onLeft) Alignment.TopStart else Alignment.TopEnd
    val bubbleOriginX = if (onLeft) 0f else 1f

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(150)),
        exit = fadeOut(tween(400)),
        modifier = modifier,
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val trackHeightPx = constraints.maxHeight.toFloat()
            val usableHeightPx = trackHeightPx - thumbHeightPx
            val thumbOffsetPx = (scrollFraction * usableHeightPx).coerceIn(0f, usableHeightPx)

            // Touch target on the edge
            Box(
                Modifier
                    .width(48.dp)
                    .fillMaxHeight()
                    .align(sideAlignment)
                    .pointerInput(totalItemCount) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            isDragging = true
                            val frac =
                                ((down.position.y - thumbHeightPx / 2) / usableHeightPx).coerceIn(
                                    0f, 1f
                                )
                            scope.launch {
                                gridState.scrollToItem((frac * (totalItemCount - 1)).toInt())
                            }

                            do {
                                val event = awaitPointerEvent()
                                val change = event.changes.first()
                                change.consume()
                                val dragFrac =
                                    ((change.position.y - thumbHeightPx / 2) / usableHeightPx).coerceIn(
                                        0f, 1f
                                    )
                                scope.launch {
                                    gridState.scrollToItem(
                                        (dragFrac * (totalItemCount - 1)).toInt()
                                    )
                                }
                            } while (event.changes.any { it.pressed })

                            isDragging = false
                        }
                    }
            ) {
                val trackAlignment = if (onLeft) Alignment.CenterStart else Alignment.CenterEnd
                val thumbAlignment = if (onLeft) Alignment.TopStart else Alignment.TopEnd
                val trackPadding = if (onLeft) Modifier.padding(start = 4.dp)
                    else Modifier.padding(end = 4.dp)
                val thumbPadding = if (onLeft) Modifier.padding(start = 2.dp)
                    else Modifier.padding(end = 2.dp)

                // Thin visual track
                Box(
                    Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .padding(vertical = thumbHeightDp / 2)
                        .align(trackAlignment)
                        .then(trackPadding)
                        .background(
                            Color.White.copy(alpha = 0.12f),
                            RoundedCornerShape(1.5.dp)
                        )
                )

                // Thumb
                Box(
                    Modifier
                        .offset { IntOffset(0, thumbOffsetPx.roundToInt()) }
                        .size(width = 6.dp, height = thumbHeightDp)
                        .align(thumbAlignment)
                        .then(thumbPadding)
                        .background(
                            if (isDragging) MaterialTheme.colorScheme.primary
                            else Color.White.copy(alpha = 0.5f),
                            RoundedCornerShape(3.dp)
                        )
                )
            }

            // Date bubble
            val bubblePadding = if (onLeft) Modifier.padding(start = 56.dp)
                else Modifier.padding(end = 56.dp)

            AnimatedVisibility(
                visible = isDragging,
                enter = fadeIn(tween(100)) + scaleIn(
                    initialScale = 0.8f,
                    transformOrigin = TransformOrigin(bubbleOriginX, 0.5f),
                ),
                exit = fadeOut(tween(150)) + scaleOut(
                    targetScale = 0.8f,
                    transformOrigin = TransformOrigin(bubbleOriginX, 0.5f),
                ),
                modifier = Modifier
                    .align(sideAlignment)
                    .then(bubblePadding)
                    .offset {
                        IntOffset(
                            0,
                            (thumbOffsetPx + thumbHeightPx / 2 - with(density) { 18.dp.toPx() }).roundToInt()
                        )
                    }
            ) {
                Text(
                    text = currentLabel,
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}
