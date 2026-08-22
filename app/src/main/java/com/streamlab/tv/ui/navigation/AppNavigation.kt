package com.streamlab.tv.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.streamlab.tv.ui.MainScreen
import com.streamlab.tv.ui.PlayerScreen
import com.streamlab.tv.ui.screens.settings.SettingsScreen

object Routes {
    const val MAIN = "main"
    const val PLAYER = "player/{channelUrl}"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.MAIN) {
        composable(Routes.MAIN) {
            MainScreen(
                onNavigateToPlayer = { channelUrl ->
                    val encodedUrl = java.net.URLEncoder.encode(channelUrl, "UTF-8")
                    navController.navigate("player/$encodedUrl")
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                }
            )
        }
        
        composable(Routes.PLAYER) { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString("channelUrl") ?: ""
            val url = java.net.URLDecoder.decode(encodedUrl, "UTF-8")
            PlayerScreen(
                channelUrl = url,
                onBackPressed = { navController.popBackStack() }
            )
        }
        
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBackPressed = { navController.popBackStack() }
            )
        }
    }
}
