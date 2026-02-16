package com.chardidathing.glint.ui.navigation

sealed class Routes(val route: String) {
    data object Home : Routes("home")
    data object AlbumDetail : Routes("album/{bucketId}") {
        fun create(bucketId: Long) = "album/$bucketId"
    }
    data object Editor : Routes("editor/{mediaId}") {
        fun create(mediaId: Long) = "editor/$mediaId"
    }
    data object Settings : Routes("settings")
}
