package com.chardidathing.glint.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.chardidathing.glint.ui.screens.editor.EditorScreen
import com.chardidathing.glint.ui.screens.home.HomeScreen
import com.chardidathing.glint.ui.screens.settings.SettingsScreen
import com.chardidathing.glint.ui.screens.viewer.ViewerScreen
import com.chardidathing.glint.viewmodel.GalleryViewModel

@Composable
fun GlintNavHost(navController: NavHostController) {
    val galleryViewModel: GalleryViewModel = viewModel()
    val viewerState by galleryViewModel.viewerState.collectAsState()
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
    ) {
        NavHost(navController = navController, startDestination = Routes.Home.route) {
            composable(Routes.Home.route) {
                HomeScreen(
                    viewModel = galleryViewModel,
                    onMediaClick = { mediaId, offset ->
                        galleryViewModel.openViewer(mediaId, offset.x, offset.y)
                    },
                    onAlbumClick = { bucketId ->
                        navController.navigate(Routes.AlbumDetail.create(bucketId))
                    },
                    onSettings = {
                        navController.navigate(Routes.Settings.route)
                    }
                )
            }

            composable(
                route = Routes.AlbumDetail.route,
                arguments = listOf(navArgument("bucketId") { type = NavType.LongType })
            ) { backStackEntry ->
                val bucketId = backStackEntry.arguments?.getLong("bucketId") ?: 0L
                HomeScreen(
                    viewModel = galleryViewModel,
                    filterBucketId = bucketId,
                    onMediaClick = { mediaId, offset ->
                        galleryViewModel.openViewer(mediaId, offset.x, offset.y)
                    },
                    onAlbumClick = {},
                    onSettings = {
                        navController.navigate(Routes.Settings.route)
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.Settings.route,
                enterTransition = { slideInHorizontally(tween(300)) { it } },
                exitTransition = { slideOutHorizontally(tween(300)) { it } },
                popEnterTransition = { slideInHorizontally(tween(300)) { -it } },
                popExitTransition = { slideOutHorizontally(tween(300)) { it } },
            ) {
                SettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.Editor.route,
                arguments = listOf(navArgument("mediaId") { type = NavType.LongType })
            ) { backStackEntry ->
                val mediaId = backStackEntry.arguments?.getLong("mediaId") ?: 0L
                EditorScreen(
                    mediaId = mediaId,
                    viewModel = galleryViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        val originX = if (containerSize.width > 0)
            (viewerState.originX / containerSize.width).coerceIn(0f, 1f)
        else 0.5f
        val originY = if (containerSize.height > 0)
            (viewerState.originY / containerSize.height).coerceIn(0f, 1f)
        else 0.5f
        val origin = TransformOrigin(originX, originY)

        AnimatedVisibility(
            visible = viewerState.isOpen,
            enter = scaleIn(
                initialScale = 0.2f,
                transformOrigin = origin,
                animationSpec = tween(250),
            ) + fadeIn(tween(200)),
            exit = fadeOut(tween(150)),
        ) {
            ViewerScreen(
                viewModel = galleryViewModel,
                startMediaId = viewerState.startMediaId,
                onBack = { galleryViewModel.closeViewer() },
                onEdit = { mediaId ->
                    galleryViewModel.closeViewer()
                    navController.navigate(Routes.Editor.create(mediaId))
                }
            )
        }
    }
}
