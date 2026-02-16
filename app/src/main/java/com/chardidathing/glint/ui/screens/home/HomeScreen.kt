package com.chardidathing.glint.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.chardidathing.glint.data.preferences.ScrollerPosition
import com.chardidathing.glint.data.preferences.ThemePreferences
import com.chardidathing.glint.data.repository.MediaRepository
import com.chardidathing.glint.viewmodel.GalleryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: GalleryViewModel,
    onMediaClick: (Long, Offset) -> Unit,
    onAlbumClick: (Long) -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    filterBucketId: Long? = null,
    onBack: (() -> Unit)? = null,
) {
    val allMedia by viewModel.allMedia.collectAsState()
    val dateGroups by viewModel.dateGroups.collectAsState()
    val albums by viewModel.albums.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val picturesGridState = rememberLazyGridState()
    val albumsGridState = rememberLazyGridState()
    val isAtTop by remember {
        derivedStateOf {
            val state = if (filterBucketId != null || selectedTab == 0) picturesGridState else albumsGridState
            state.firstVisibleItemIndex == 0 && state.firstVisibleItemScrollOffset == 0
        }
    }

    val displayMedia = if (filterBucketId != null) {
        allMedia.filter { it.bucketId == filterBucketId }
    } else {
        allMedia
    }
    val displayGroups = if (filterBucketId != null) {
        MediaRepository.groupByDate(displayMedia)
    } else {
        dateGroups
    }

    val albumName = if (filterBucketId != null) {
        albums.find { it.bucketId == filterBucketId }?.name ?: "Album"
    } else null

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AnimatedVisibility(
                visible = isAtTop,
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut(),
            ) {
                TopAppBar(
                    title = {
                        Text(albumName ?: "Glint")
                    },
                    navigationIcon = {
                        if (onBack != null) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = onSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                    )
                )
            }
        },
        bottomBar = {
            if (filterBucketId == null) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Photo, contentDescription = null) },
                        label = { Text("Pictures") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.PhotoAlbum, contentDescription = null) },
                        label = { Text("Albums") }
                    )
                }
            }
        }
    ) { innerPadding ->
        when {
            filterBucketId != null || selectedTab == 0 -> {
                PicturesTab(
                    dateGroups = displayGroups,
                    onMediaClick = onMediaClick,
                    gridState = picturesGridState,
                    modifier = Modifier.padding(innerPadding),
                    showTimelineScroller = ThemePreferences.scrollerPosition != ScrollerPosition.Off,
                    timelineScrollerOnLeft = ThemePreferences.scrollerPosition == ScrollerPosition.Left,
                )
            }
            selectedTab == 1 -> {
                AlbumsTab(
                    albums = albums,
                    onAlbumClick = onAlbumClick,
                    gridState = albumsGridState,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}
