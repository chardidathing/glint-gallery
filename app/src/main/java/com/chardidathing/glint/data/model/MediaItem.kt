package com.chardidathing.glint.data.model

import android.net.Uri

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val dateAdded: Long,
    val dateTaken: Long,
    val mimeType: String,
    val duration: Long = 0,
    val width: Int = 0,
    val height: Int = 0,
    val bucketId: Long = 0,
    val bucketName: String = "",
) {
    val isVideo: Boolean get() = mimeType.startsWith("video/")
    val effectiveDate: Long get() = if (dateTaken > 0) dateTaken else dateAdded
}
