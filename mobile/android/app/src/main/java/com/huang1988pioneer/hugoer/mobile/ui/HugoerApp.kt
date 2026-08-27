package com.huang1988pioneer.hugoer.mobile.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.huang1988pioneer.hugoer.mobile.data.DemoHugoerRepository
import com.huang1988pioneer.hugoer.mobile.presentation.HugoerEvent
import com.huang1988pioneer.hugoer.mobile.presentation.HugoerViewModel
import kotlinx.coroutines.launch

enum class Destination(
    val label: String,
    val icon: ImageVector,
) {
    Sites("站台", Icons.Filled.Cloud),
    Config("設定檔", Icons.Filled.Tune),
    Themes("主題", Icons.Filled.Palette),
    Content("內容", Icons.Filled.Article),
    GitHub("GitHub", Icons.Filled.Code),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HugoerApp() {
    val repository = remember { DemoHugoerRepository() }
    val viewModel: HugoerViewModel = viewModel(factory = HugoerViewModel.Factory(repository))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var destinationIndex by rememberSaveable { mutableStateOf(0) }
    var editorArticleId by rememberSaveable { mutableStateOf<String?>(null) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val destination = Destination.entries[destinationIndex.coerceIn(0, Destination.entries.lastIndex)]

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val expanded = maxWidth >= 600.dp
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            floatingActionButton = {
                if (destination == Destination.Sites) {
                    ReferenceFloatingActionButton(
                        contentDescription = "新增網站",
                        onClick = {
                            scope.launch { snackbarHostState.showSnackbar("新增網站功能即將推出") }
                        },
                    )
                }
            },
            bottomBar = {
                if (!expanded) {
                    AppNavigationBar(
                        selected = destination,
                        onSelect = { destinationIndex = it.ordinal },
                    )
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (expanded) {
                    AppNavigationRail(
                        selected = destination,
                        onSelect = { destinationIndex = it.ordinal },
                    )
                }
                Surface(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                    when (destination) {
                        Destination.Sites -> ReferenceSitesScreen(
                            state = state,
                            onSettings = { showSettings = true },
                        )
                        Destination.Config -> ReferenceConfigScreen(
                            state = state,
                            onSave = { message -> scope.launch { snackbarHostState.showSnackbar(message) } },
                            onSettings = { showSettings = true },
                        )
                        Destination.Themes -> ReferenceThemesScreen(
                            onApply = { theme -> scope.launch { snackbarHostState.showSnackbar("已套用 $theme") } },
                            onSettings = { showSettings = true },
                        )
                        Destination.Content -> ReferenceContentScreen(
                            state = state,
                            onOpenArticle = { editorArticleId = it.id },
                            onCreateArticle = viewModel::createArticle,
                            onSync = viewModel::refresh,
                            onSettings = { showSettings = true },
                        )
                        Destination.GitHub -> ReferenceGitHubScreen(
                            state = state,
                            onDeploy = { viewModel.triggerDeployment() },
                            onSync = viewModel::refresh,
                            onSettings = { showSettings = true },
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is HugoerEvent.Snackbar -> snackbarHostState.showSnackbar(event.message)
                is HugoerEvent.OpenEditor -> editorArticleId = event.articleId
            }
        }
    }

    val editorArticle = state.articles.firstOrNull { it.id == editorArticleId }
    if (editorArticle != null) {
        ArticleEditorSheet(
            article = editorArticle,
            previewBaseUrl = state.site.pagesUrl,
            onDismiss = { editorArticleId = null },
            onSave = { title, body ->
                viewModel.saveArticle(editorArticle.id, title, body)
                editorArticleId = null
            },
        )
    }

    if (showSettings) {
        ReferenceSettingsDialog(
            site = state.site,
            onDismiss = { showSettings = false },
            onAction = { message ->
                showSettings = false
                scope.launch { snackbarHostState.showSnackbar(message) }
            },
        )
    }
}
