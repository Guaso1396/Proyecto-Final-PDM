package com.example.faceid.kevin.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.faceid.diana.authentication.ChangePinScreen
import com.example.faceid.diana.authentication.CreatePinScreen
import com.example.faceid.diana.authentication.EnterPinScreen
import com.example.faceid.fabian.apps.AppsScreen
import com.example.faceid.ganan.lock.LockScreen
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

private const val ANIM_MS = 280

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it / 4 },
                animationSpec = tween(ANIM_MS)
            ) + fadeIn(tween(ANIM_MS))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it / 4 },
                animationSpec = tween(ANIM_MS)
            ) + fadeOut(tween(ANIM_MS))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it / 4 },
                animationSpec = tween(ANIM_MS)
            ) + fadeIn(tween(ANIM_MS))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it / 4 },
                animationSpec = tween(ANIM_MS)
            ) + fadeOut(tween(ANIM_MS))
        }
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
        ) { backStackEntry ->
            val pkg = backStackEntry.arguments?.getString("packageName").orEmpty()
            LockScreen(
                packageName = pkg,
                onUnlocked = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
