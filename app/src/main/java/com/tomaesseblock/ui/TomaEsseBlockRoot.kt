package com.tomaesseblock.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tomaesseblock.ui.screens.BlockListScreen
import com.tomaesseblock.ui.screens.HistoryScreen
import com.tomaesseblock.ui.screens.HomeScreen
import com.tomaesseblock.ui.screens.LookupScreen
import com.tomaesseblock.ui.screens.SettingsScreen

internal enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    HOME("home", "Início", Icons.Filled.Shield),
    BLOCKLIST("blocklist", "Bloqueios", Icons.Filled.Block),
    HISTORY("history", "Histórico", Icons.Filled.History),
    LOOKUP("lookup", "Buscar", Icons.Filled.Search),
    SETTINGS("settings", "Ajustes", Icons.Filled.Settings),
}

@Composable
fun TomaEsseBlockRoot(vm: MainViewModel = viewModel(factory = MainViewModel.Factory)) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    fun go(route: String) = nav.navigate(route) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    AppScaffold(currentRoute = currentRoute, onTabSelected = { go(it.route) }) { padding ->
        NavHost(nav, startDestination = Tab.HOME.route, modifier = Modifier.padding(padding)) {
            composable(Tab.HOME.route) { HomeScreen(vm, onOpenLookup = { go(Tab.LOOKUP.route) }) }
            composable(Tab.BLOCKLIST.route) { BlockListScreen(vm) }
            composable(Tab.HISTORY.route) {
                HistoryScreen(vm, onOpenNumber = { number ->
                    vm.lookup(number)
                    go(Tab.LOOKUP.route)
                })
            }
            composable(Tab.LOOKUP.route) { LookupScreen(vm) }
            composable(Tab.SETTINGS.route) { SettingsScreen(vm) }
        }
    }
}

/** Estrutura com a barra de navegação inferior; reaproveitada nas capturas de tela. */
@Composable
internal fun AppScaffold(
    currentRoute: String?,
    onTabSelected: (Tab) -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = { onTabSelected(tab) },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
        content = content,
    )
}
