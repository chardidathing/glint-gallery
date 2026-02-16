package com.chardidathing.glint.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chardidathing.glint.data.model.Album
import com.chardidathing.glint.data.model.DateGroup
import com.chardidathing.glint.data.model.MediaItem
import com.chardidathing.glint.data.repository.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ViewerState(
    val isOpen: Boolean = false,
    val startMediaId: Long = 0L,
    val originX: Float = 0f,
    val originY: Float = 0f,
)

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaRepository(application)

    val allMedia: StateFlow<List<MediaItem>> = repository.observeMedia()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dateGroups: StateFlow<List<DateGroup>> = allMedia
        .map { MediaRepository.groupByDate(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albums: StateFlow<List<Album>> = allMedia
        .map { MediaRepository.groupByAlbum(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun mediaForAlbum(bucketId: Long): List<MediaItem> {
        return allMedia.value.filter { it.bucketId == bucketId }
    }

    fun findMediaById(id: Long): MediaItem? {
        return allMedia.value.find { it.id == id }
    }

    private val _viewerState = MutableStateFlow(ViewerState())
    val viewerState: StateFlow<ViewerState> = _viewerState.asStateFlow()

    fun openViewer(mediaId: Long, originX: Float, originY: Float) {
        _viewerState.value = ViewerState(
            isOpen = true,
            startMediaId = mediaId,
            originX = originX,
            originY = originY,
        )
    }

    fun closeViewer() {
        _viewerState.value = _viewerState.value.copy(isOpen = false)
    }
}
