package com.example.faceid.kevin.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.faceid.diana.authentication.ChangePinScreen
import com.example.faceid.diana.authentication.CreatePinScreen
import com.example.faceid.diana.authentication.EnterPinScreen
import com.example.faceid.fabian.apps.AppsScreen
import com.example.faceid.kevin.home.HomeScreen

object Routes {
    const val HOME = "home"
    const val APPS = "apps"
    const val ENTER_PIN = "enterPin"
    const val CREATE_PIN = "createPin"
    const val CHANGE_PIN = "changePin"
    const val LOCK_WITH_APP = "lock/{packageName}"

    fun lock(packageName: String) = "lock/$packageName"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(onNavigate = { route -> navController.navigate(route) })
        }
        composable(Routes.APPS) {
            AppsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.ENTER_PIN) {
            EnterPinScreen(
                onSuccess = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.CREATE_PIN) {
            CreatePinScreen(
                onSuccess = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.CHANGE_PIN) {
            ChangePinScreen(
                onSuccess = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.LOCK_WITH_APP,
            arguments = listOf(navArgument("packageName") { type = NavType.StringType })
        ) {
            PlaceholderScreen(title = "Pantalla de bloqueo")
            // TODO(ganan): reemplazar con LockScreen
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = title)
    }
}
