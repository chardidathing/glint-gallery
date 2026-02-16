package com.chardidathing.glint

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.chardidathing.glint.data.preferences.ThemePreferences
import com.chardidathing.glint.ui.components.PermissionGate
import com.chardidathing.glint.ui.navigation.GlintNavHost
import com.chardidathing.glint.ui.theme.GlintTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ThemePreferences.init(this)
        setContent {
            GlintTheme {
                PermissionGate {
                    val navController = rememberNavController()
                    GlintNavHost(navController = navController)
                }
            }
        }
    }
}
