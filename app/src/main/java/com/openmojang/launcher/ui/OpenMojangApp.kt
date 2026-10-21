package com.openmojang.launcher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.openmojang.launcher.data.SettingsStore
import com.openmojang.launcher.ui.screens.*

private data class Tab(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun OpenMojangApp() {
    val appContext = LocalContext.current
    val setupStore = remember { SettingsStore(appContext) }
    var setupDone by remember { mutableStateOf(setupStore.setupComplete) }
    if (!setupDone) {
        SetupScreen(onFinished = {
            setupStore.setupComplete = true
            setupDone = true
        })
        return
    }
    val nav = rememberNavController()
    val tabs = listOf(
        Tab("home", "Home", Icons.Outlined.Home),
        Tab("versions", "Versions", Icons.Outlined.Download),
        Tab("modify", "Modify", Icons.Outlined.Tune),
        Tab("library", "Library", Icons.Outlined.Inventory2),
        Tab("more", "More", Icons.Outlined.MoreHoriz)
    )
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route ?: "home"
    val showBar = current in tabs.map { it.route }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBar,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut()
            ) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = current == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {
            composable("home") {
                HomeScreen(
                    onVersions = {
                        nav.navigate("versions") {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onAccount = { nav.navigate("account") },
                    onSkins = { nav.navigate("skins") },
                    onSettings = { nav.navigate("settings") },
                    onLibrary = {
                        nav.navigate("library") {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable("versions") {
                VersionManagerScreen(onBack = { nav.popBackStack() })
            }
            composable("modify") { ModifyScreen() }
            composable("library") { LibraryScreen() }
            composable("more") {
                MoreScreen(
                    onAccount = { nav.navigate("account") },
                    onSkins = { nav.navigate("skins") },
                    onSettings = { nav.navigate("settings") }
                )
            }
            composable("account") { AccountScreen(onBack = { nav.popBackStack() }) }
            composable("skins") { SkinScreen(onBack = { nav.popBackStack() }) }
            composable("settings") { SettingsScreen(onBack = { nav.popBackStack() }) }
        }
    }
}
