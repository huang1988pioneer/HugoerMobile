package com.hugoer.mobile.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.hugoer.mobile.AppTab
import com.hugoer.mobile.AppUiState
import com.hugoer.mobile.AppViewModel
import com.hugoer.mobile.ui.config.ConfigScreen
import com.hugoer.mobile.ui.content.ContentScreen
import com.hugoer.mobile.ui.content.EditorScreen
import com.hugoer.mobile.ui.github.GithubScreen
import com.hugoer.mobile.ui.settings.SettingsScreen
import com.hugoer.mobile.ui.sites.ConnectScreen
import com.hugoer.mobile.ui.sites.SitesScreen
import com.hugoer.mobile.ui.themes.ThemesScreen

@Composable
internal fun HugoerApp(viewModel: AppViewModel) {
    val state by viewModel.state.collectAsState()
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var connectOpen by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error, state.status) {
        val message = state.error ?: state.status
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    when {
        settingsOpen -> SettingsScreen(state, viewModel, onBack = { settingsOpen = false })
        connectOpen -> ConnectScreen(state, viewModel, onBack = { connectOpen = false })
        state.editor != null -> EditorScreen(state.editor!!, state, viewModel, onBack = viewModel::closeEditor)
        else -> MainShell(
            state,
            viewModel,
            onSettings = { settingsOpen = true },
            onConnect = { connectOpen = true },
            snackbarHostState
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainShell(
    state: AppUiState,
    vm: AppViewModel,
    onSettings: () -> Unit,
    onConnect: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.selectedTab.label, fontWeight = FontWeight.Bold) },
                actions = { IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "設定") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = state.selectedTab == tab,
                        onClick = { vm.selectTab(tab) },
                        icon = { Icon(tabIcon(tab), contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (state.selectedTab == AppTab.SITES) {
                FloatingActionButton(onClick = onConnect) {
                    Icon(Icons.Default.Add, "新增網站")
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (state.selectedTab) {
                AppTab.SITES -> SitesScreen(state, vm, onConnect)
                AppTab.CONFIG -> ConfigScreen(state, vm)
                AppTab.THEMES -> ThemesScreen(state, vm)
                AppTab.CONTENT -> ContentScreen(state, vm)
                AppTab.GITHUB -> GithubScreen(state, vm)
            }
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
        }
    }
}

private fun tabIcon(tab: AppTab) = when (tab) {
    AppTab.SITES -> Icons.Default.Cloud
    AppTab.CONFIG -> Icons.Default.Tune
    AppTab.THEMES -> Icons.Default.Palette
    AppTab.CONTENT -> Icons.AutoMirrored.Filled.Article
    AppTab.GITHUB -> Icons.Default.Code
}
