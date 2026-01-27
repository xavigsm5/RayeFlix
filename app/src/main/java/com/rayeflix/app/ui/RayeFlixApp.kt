package com.rayeflix.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Person
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument // Import needed
import com.rayeflix.app.ui.screens.HomeScreen
import com.rayeflix.app.ui.screens.SearchScreen
import com.rayeflix.app.ui.screens.ProfileSelectionScreen
import com.rayeflix.app.ui.screens.PlayerScreen
import com.rayeflix.app.ui.theme.DarkSurface
import com.rayeflix.app.ui.theme.NetflixRed
import com.rayeflix.app.ui.theme.White
import com.rayeflix.app.ui.theme.GrayText
import com.rayeflix.app.viewmodel.AppViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Search : Screen("search", "Search", Icons.Default.Search)
    object MyNetflix : Screen("mynetflix", "My Netflix", Icons.Default.Person)
}

@Composable
fun RayeFlixApp() {
    val navController = rememberNavController()
    // In a real app, use Hilt or a Factory. Here we instantiate for simplicity.
    val viewModel: AppViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    
    val items = listOf(Screen.Home, Screen.Search, Screen.MyNetflix)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Scaffold with no bottom bar
    Scaffold { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "profile",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("profile") {
                ProfileSelectionScreen(
                    viewModel = viewModel,
                    onProfileClick = { profile ->
                        viewModel.onProfileSelected(profile)
                        navController.navigate(Screen.Home.route) {
                            popUpTo("profile") { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Home.route) { 
                HomeScreen(navController, viewModel) 
            }
            composable(Screen.Search.route) { SearchScreen() }
            composable(Screen.MyNetflix.route) { Text("My Netflix", color = White) }
            composable(
                route = "player?url={url}",
                arguments = listOf(navArgument("url") { defaultValue = "" })
            ) { backStackEntry ->
                val url = backStackEntry.arguments?.getString("url") ?: ""
                PlayerScreen(url)
            }
        }
    }
}
