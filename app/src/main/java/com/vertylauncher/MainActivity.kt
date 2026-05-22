package com.vertylauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vertylauncher.ui.screen.HomeScreen
import com.vertylauncher.ui.screen.LoginScreen
import com.vertylauncher.ui.screen.SettingsScreen
import com.vertylauncher.ui.screen.VersionListScreen
import com.vertylauncher.ui.theme.VertyLauncherTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VertyLauncherTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") { HomeScreen(navController) }
                        composable("versions") { VersionListScreen(navController) }
                        composable("settings") { SettingsScreen(navController) }
                        composable("login") { LoginScreen(navController) }
                    }
                }
            }
        }
    }
}
