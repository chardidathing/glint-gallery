package com.chardidathing.glint.ui.screens.editor

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chardidathing.glint.viewmodel.GalleryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    mediaId: Long,
    viewModel: GalleryViewModel,
    onBack: () -> Unit,
    editorViewModel: EditorViewModel = viewModel(),
) {
    val context = LocalContext.current
    val mediaItem = viewModel.findMediaById(mediaId)
    val bitmap by editorViewModel.bitmap.collectAsState()
    val rotation by editorViewModel.rotation.collectAsState()
    val flipH by editorViewModel.flipH.collectAsState()
    val flipV by editorViewModel.flipV.collectAsState()
    val cropRect by editorViewModel.cropRect.collectAsState()
    val isCropping by editorViewModel.isCropping.collectAsState()

    LaunchedEffect(mediaItem) {
        mediaItem?.let { editorViewModel.loadImage(it.uri) }
    }

    var imageSize by remember { mutableStateOf(IntSize.Zero) }

    // Compute where the image is actually rendered within the composable (ContentScale.Fit)
    val displayedImageRect = remember(imageSize, bitmap) {
        val bmp = bitmap ?: return@remember Rect.Zero
        if (imageSize == IntSize.Zero) return@remember Rect.Zero
        val compW = imageSize.width.toFloat()
        val compH = imageSize.height.toFloat()
        val scale = minOf(compW / bmp.width, compH / bmp.height)
        val renderedW = bmp.width * scale
        val renderedH = bmp.height * scale
        val offsetX = (compW - renderedW) / 2f
        val offsetY = (compH - renderedH) / 2f
        Rect(offsetX, offsetY, offsetX + renderedW, offsetY + renderedH)
    }

    // Initialize crop rect when entering crop mode
    LaunchedEffect(isCropping, displayedImageRect) {
        if (isCropping && cropRect == null && displayedImageRect != Rect.Zero) {
            editorViewModel.updateCropRect(displayedImageRect)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding()
    ) {
        TopAppBar(
            title = { Text("Edit") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                if (isCropping) {
                    IconButton(onClick = { editorViewModel.applyCrop(displayedImageRect) }) {
                        Icon(Icons.Default.Check, contentDescription = "Apply crop")
                    }
                }
                IconButton(onClick = {
                    val uri = editorViewModel.save()
                    if (uri != null) {
                        Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                        onBack()
                    } else {
                        Toast.makeText(context, "Save failed", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(Icons.Default.Save, contentDescription = "Save")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Black,
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White,
                actionIconContentColor = Color.White,
            )
        )

        // Image preview
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            bitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            rotationZ = rotation,
                            scaleX = if (flipH) -1f else 1f,
                            scaleY = if (flipV) -1f else 1f,
                        )
                        .onGloballyPositioned { coordinates ->
                            imageSize = coordinates.size
                        }
                )

                if (isCropping && cropRect != null) {
                    CropOverlay(
                        cropRect = cropRect!!,
                        imageRect = displayedImageRect,
                        onCropChange = { editorViewModel.updateCropRect(it) }
                    )
                }
            }
        }

        // Tool buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ToolButton(
                icon = Icons.Default.Crop,
                label = "Crop",
                isActive = isCropping,
                onClick = { editorViewModel.toggleCropMode() }
            )
            ToolButton(
                icon = Icons.AutoMirrored.Filled.RotateLeft,
                label = "Rotate L",
                onClick = { editorViewModel.rotateLeft() }
            )
            ToolButton(
                icon = Icons.AutoMirrored.Filled.RotateRight,
                label = "Rotate R",
                onClick = { editorViewModel.rotateRight() }
            )
            ToolButton(
                icon = Icons.Default.Flip,
                label = "Flip H",
                isActive = flipH,
                onClick = { editorViewModel.toggleFlipH() }
            )
            ToolButton(
                icon = Icons.Default.Flip,
                label = "Flip V",
                isActive = flipV,
                onClick = { editorViewModel.toggleFlipV() },
                iconRotation = 90f,
            )
        }
    }
}

@Composable
private fun ToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    isActive: Boolean = false,
    iconRotation: Float = 0f,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) MaterialTheme.colorScheme.primary else Color.White,
                modifier = Modifier.graphicsLayer(rotationZ = iconRotation)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) MaterialTheme.colorScheme.primary else Color.White,
        )
    }
}
