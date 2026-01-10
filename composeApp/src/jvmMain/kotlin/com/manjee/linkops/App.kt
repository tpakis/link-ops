package com.manjee.linkops

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.manjee.linkops.ui.navigation.*
import com.manjee.linkops.ui.screen.diagnostics.DiagnosticsScreen
import com.manjee.linkops.ui.screen.diagnostics.DiagnosticsViewModel
import com.manjee.linkops.ui.screen.main.MainScreen
import com.manjee.linkops.ui.screen.main.MainViewModel
import com.manjee.linkops.ui.screen.manifest.ManifestAnalyzerScreen
import com.manjee.linkops.ui.screen.manifest.ManifestAnalyzerViewModel
import com.manjee.linkops.ui.theme.LinkOpsTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Main Application Composable
 *
 * Entry point for the LinkOps desktop application.
 * Uses the new UI structure with:
 * - LinkOpsTheme for consistent styling
 * - NavigationController for screen navigation
 * - Sidebar navigation for main screens
 */
@Composable
@Preview
fun App() {
    val navController = rememberNavigationController()
    val mainViewModel = remember { MainViewModel() }
    val diagnosticsViewModel = remember { DiagnosticsViewModel() }
    val manifestAnalyzerViewModel = remember { ManifestAnalyzerViewModel() }

    // Cleanup ViewModels when composable leaves composition
    DisposableEffect(Unit) {
        onDispose {
            mainViewModel.onCleared()
            diagnosticsViewModel.onCleared()
            manifestAnalyzerViewModel.onCleared()
        }
    }

    LinkOpsTheme {
        ProvideNavigationController(navController) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Sidebar Navigation
                NavigationSidebar(
                    currentScreen = navController.currentScreen,
                    onNavigate = { screen -> navController.navigateTo(screen) }
                )

                // Main Content
                NavHost(
                    navController = navController,
                    modifier = Modifier.weight(1f)
                ) { screen ->
                    when (screen) {
                        Screen.Dashboard -> {
                            MainScreen(viewModel = mainViewModel)
                        }
                        Screen.DeviceSelection -> {
                            MainScreen(viewModel = mainViewModel)
                        }
                        Screen.Diagnostics -> {
                            DiagnosticsScreen(viewModel = diagnosticsViewModel)
                        }
                        Screen.ManifestAnalyzer -> {
                            val mainUiState by mainViewModel.uiState.collectAsState()
                            ManifestAnalyzerScreen(
                                viewModel = manifestAnalyzerViewModel,
                                devices = mainUiState.devices
                            )
                        }
                        Screen.Settings -> {
                            // TODO: Implement SettingsScreen
                            MainScreen(viewModel = mainViewModel)
                        }
                        is Screen.AppLinksDetail -> {
                            MainScreen(viewModel = mainViewModel)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Navigation sidebar with icons for main screens
 */
@Composable
private fun NavigationSidebar(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit
) {
    NavigationRail(
        modifier = Modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        NavigationRailItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
            label = { Text("Dashboard") },
            selected = currentScreen == Screen.Dashboard,
            onClick = { onNavigate(Screen.Dashboard) }
        )

        NavigationRailItem(
            icon = { Icon(Icons.Default.Search, contentDescription = "Diagnostics") },
            label = { Text("Diagnostics") },
            selected = currentScreen == Screen.Diagnostics,
            onClick = { onNavigate(Screen.Diagnostics) }
        )

        NavigationRailItem(
            icon = { Icon(Icons.Default.Description, contentDescription = "Manifest") },
            label = { Text("Manifest") },
            selected = currentScreen == Screen.ManifestAnalyzer,
            onClick = { onNavigate(Screen.ManifestAnalyzer) }
        )

        Spacer(modifier = Modifier.weight(1f))

        NavigationRailItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings") },
            selected = currentScreen == Screen.Settings,
            onClick = { onNavigate(Screen.Settings) }
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}
