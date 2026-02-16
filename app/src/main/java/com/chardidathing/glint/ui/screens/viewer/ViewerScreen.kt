package com.chardidathing.glint.ui.screens.viewer

import android.app.Activity
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.chardidathing.glint.viewmodel.GalleryViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun ViewerScreen(
    viewModel: GalleryViewModel,
    startMediaId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    val allMedia by viewModel.allMedia.collectAsState()
    var barsVisible by remember { mutableStateOf(true) }
    var isZoomed by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val dragOffsetY = remember { Animatable(0f) }
    val dismissThresholdPx = with(density) { 150.dp.toPx() }
    val isDragging = dragOffsetY.value != 0f
    val dragProgress = (abs(dragOffsetY.value) / dismissThresholdPx).coerceIn(0f, 1f)
    val bgAlpha = 1f - dragProgress * 0.6f
    val backgroundAlpha = 1f - dragProgress
    val dragScale = 1f - dragProgress * 0.1f

    val statusBarHeight = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
    val navBarHeight = with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }
    val animatedTopPadding by animateDpAsState(
        targetValue = if (barsVisible) statusBarHeight else 0.dp,
        label = "topPadding",
    )
    val animatedBottomPadding by animateDpAsState(
        targetValue = if (barsVisible) navBarHeight else 0.dp,
        label = "bottomPadding",
    )

    val window = (context as Activity).window
    val insetsController = remember {
        WindowCompat.getInsetsController(window, window.decorView)
    }

    LaunchedEffect(barsVisible) {
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (barsVisible) {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        } else {
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    BackHandler(onBack = onBack)

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            if (allMedia.size <= 1) {
                onBack()
            }
        }
    }

    if (allMedia.isEmpty()) return

    val startIndex = remember(startMediaId) {
        allMedia.indexOfFirst { it.id == startMediaId }.coerceAtLeast(0)
    }

    val pagerState = rememberPagerState(
        initialPage = startIndex,
        pageCount = { allMedia.size }
    )

    LaunchedEffect(pagerState.currentPage) {
        isZoomed = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = backgroundAlpha))
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = animatedTopPadding, bottom = animatedBottomPadding)
                .offset { IntOffset(0, dragOffsetY.value.roundToInt()) }
                .graphicsLayer {
                    scaleX = dragScale
                    scaleY = dragScale
                    alpha = bgAlpha
                },
            beyondViewportPageCount = 1,
            userScrollEnabled = !isZoomed,
        ) { page ->
            val item = allMedia[page]
            if (item.isVideo) {
                VideoPlayer(
                    uri = item.uri,
                    isCurrentPage = page == pagerState.currentPage,
                    controlsVisible = barsVisible,
                    onTap = { barsVisible = !barsVisible },
                    onVerticalDrag = { dragY ->
                        scope.launch { dragOffsetY.snapTo(dragY) }
                    },
                    onVerticalDragEnd = { dragY ->
                        if (abs(dragY) > dismissThresholdPx) {
                            onBack()
                        } else {
                            scope.launch { dragOffsetY.animateTo(0f, spring()) }
                        }
                    }
                )
            } else {
                ZoomableImage(
                    uri = item.uri,
                    contentDescription = item.displayName,
                    onTap = { barsVisible = !barsVisible },
                    onZoomChange = { zoomed ->
                        if (page == pagerState.currentPage) {
                            isZoomed = zoomed
                        }
                    },
                    onVerticalDrag = { dragY ->
                        scope.launch { dragOffsetY.snapTo(dragY) }
                    },
                    onVerticalDragEnd = { dragY ->
                        if (abs(dragY) > dismissThresholdPx) {
                            onBack()
                        } else {
                            scope.launch { dragOffsetY.animateTo(0f, spring()) }
                        }
                    }
                )
            }
        }

        AnimatedVisibility(
            visible = barsVisible && !isDragging,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = barsVisible && !isDragging,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (!allMedia[pagerState.currentPage].isVideo) {
                    IconButton(onClick = {
                        val currentItem = allMedia[pagerState.currentPage]
                        onEdit(currentItem.id)
                    }) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = Color.White
                        )
                    }
                }
                IconButton(onClick = {
                    val currentItem = allMedia[pagerState.currentPage]
                    val deleteRequest = MediaStore.createDeleteRequest(
                        context.contentResolver,
                        listOf(currentItem.uri)
                    )
                    deleteLauncher.launch(
                        IntentSenderRequest.Builder(deleteRequest.intentSender).build()
                    )
                }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
