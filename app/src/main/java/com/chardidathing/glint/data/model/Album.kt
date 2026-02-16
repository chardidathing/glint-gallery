package com.chardidathing.glint.data.model

import android.net.Uri

data class Album(
    val bucketId: Long,
    val name: String,
    val coverUri: Uri,
    val count: Int,
)
