package com.druboni.absplayer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.druboni.absplayer.ui.bookdetail.BookDetailScreen
import com.druboni.absplayer.ui.library.LibraryScreen
import com.druboni.absplayer.ui.login.LoginScreen
import com.druboni.absplayer.ui.player.PlayerScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Library : Screen("library")
    object BookDetail : Screen("book/{itemId}") {
        fun createRoute(itemId: String) = "book/$itemId"
    }
    object Player : Screen("player/{itemId}") {
        fun createRoute(itemId: String) = "player/$itemId"
    }
}

@Composable
fun AppNavGraph(navViewModel: NavViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val token by navViewModel.prefs.token.collectAsState(initial = null)

    val startDestination = if (token != null) Screen.Library.route else Screen.Login.route

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Login.route) {
            LoginScreen(onLoginSuccess = {
                navController.navigate(Screen.Library.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            })
        }
        composable(Screen.Library.route) {
            LibraryScreen(
                onBookClick = { itemId -> navController.navigate(Screen.BookDetail.createRoute(itemId)) },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = Screen.BookDetail.route,
            arguments = listOf(navArgument("itemId") { type = NavType.StringType })
        ) { backStackEntry ->
            BookDetailScreen(
                itemId = backStackEntry.arguments?.getString("itemId") ?: "",
                onPlayClick = { itemId -> navController.navigate(Screen.Player.createRoute(itemId)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.Player.route,
            arguments = listOf(navArgument("itemId") { type = NavType.StringType })
        ) { backStackEntry ->
            PlayerScreen(
                itemId = backStackEntry.arguments?.getString("itemId") ?: "",
                onBack = { navController.popBackStack() }
            )
        }
    }
}
