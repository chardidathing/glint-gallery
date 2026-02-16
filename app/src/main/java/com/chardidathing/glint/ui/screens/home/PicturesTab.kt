package com.chardidathing.glint.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.chardidathing.glint.data.model.DateGroup
import com.chardidathing.glint.ui.components.DateHeader
import com.chardidathing.glint.ui.components.MediaGridItem
import com.chardidathing.glint.ui.components.TimelineScroller

@Composable
fun PicturesTab(
    dateGroups: List<DateGroup>,
    onMediaClick: (Long, Offset) -> Unit,
    gridState: LazyGridState,
    modifier: Modifier = Modifier,
    showTimelineScroller: Boolean = true,
    timelineScrollerOnLeft: Boolean = false,
) {
    Box(modifier = modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            dateGroups.forEach { group ->
                item(
                    key = "header_${group.label}",
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    DateHeader(label = group.label)
                }
                items(
                    items = group.items,
                    key = { it.id }
                ) { mediaItem ->
                    MediaGridItem(
                        item = mediaItem,
                        onClick = { offset -> onMediaClick(mediaItem.id, offset) }
                    )
                }
            }
        }

        if (showTimelineScroller) {
            TimelineScroller(
                gridState = gridState,
                dateGroups = dateGroups,
                onLeft = timelineScrollerOnLeft,
            )
        }
    }
}
