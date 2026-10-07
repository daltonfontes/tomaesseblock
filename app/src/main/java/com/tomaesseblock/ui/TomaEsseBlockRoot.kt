package com.tomaesseblock.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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

internal enum class Tab(val route: String, val label: String) {
    HOME("home", "Hoje"),
    HISTORY("history", "Registro"),
    BLOCKLIST("blocklist", "Listas"),
    LOOKUP("lookup", "Buscar"),
    SETTINGS("settings", "Ajustes"),
}

@Composable
fun TomaEsseBlockRoot(vm: MainViewModel = viewModel(factory = MainViewModel.Factory)) {
    val nav = rememberNavController()
    val context = LocalContext.current
    LaunchedEffect(vm) {
        vm.messages.collect { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
    }
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
        bottomBar = { TabBar(currentRoute, onTabSelected) },
        content = content,
    )
}

/** Barra inferior de texto: a aba atual fica preenchida com a cor da tinta. */
@Composable
private fun TabBar(currentRoute: String?, onTabSelected: (Tab) -> Unit) {
    val ink = MaterialTheme.colorScheme.onSurface
    Column(Modifier.background(MaterialTheme.colorScheme.surface).navigationBarsPadding()) {
        HorizontalDivider(thickness = 2.dp, color = ink)
        Row(Modifier.fillMaxWidth().selectableGroup()) {
            Tab.entries.forEach { tab ->
                val selected = currentRoute == tab.route
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 60.dp)
                        .background(if (selected) ink else MaterialTheme.colorScheme.surface)
                        .selectable(selected = selected, role = Role.Tab, onClick = { onTabSelected(tab) }),
                ) {
                    Text(
                        tab.label.uppercase(),
                        color = if (selected) MaterialTheme.colorScheme.surface else ink,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        ),
                    )
                }
            }
        }
    }
}
