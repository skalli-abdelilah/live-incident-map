package com.livemap.incidents.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.livemap.incidents.ui.detail.IncidentDetailScreen
import com.livemap.incidents.ui.list.IncidentListScreen
import com.livemap.incidents.ui.map.MapScreen
import kotlin.reflect.KClass

private data class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    val label: String,
    val icon: ImageVector,
)

private val topLevelDestinations = listOf(
    TopLevelDestination(MapRoute, MapRoute::class, "Map", Icons.Filled.Map),
    TopLevelDestination(ListRoute, ListRoute::class, "List", Icons.AutoMirrored.Filled.List),
)

@Composable
fun IncidentNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    // The bottom bar belongs to the two top-level screens; detail is a pushed screen.
    val showBottomBar = topLevelDestinations.any { destination ->
        currentDestination?.hasRoute(destination.routeClass) == true
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    topLevelDestinations.forEach { destination ->
                        val selected =
                            currentDestination?.hasRoute(destination.routeClass) == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    // Keep a single copy of each top-level screen on the stack
                                    // and preserve its state when switching tabs.
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = MapRoute,
            modifier = Modifier.padding(padding),
        ) {
            composable<MapRoute> {
                MapScreen(
                    onIncidentClick = { id -> navController.navigate(DetailRoute(id)) },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            composable<ListRoute> {
                IncidentListScreen(
                    onIncidentClick = { id -> navController.navigate(DetailRoute(id)) },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            composable<DetailRoute> {
                IncidentDetailScreen(onBack = navController::popBackStack)
            }
        }
    }
}
