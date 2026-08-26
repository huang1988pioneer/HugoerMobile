package com.hugoer.mobile.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.hugoer.mobile.AppTab
import com.hugoer.mobile.AppViewModel
import com.hugoer.mobile.AppUiState
import com.hugoer.mobile.EditorState
import com.hugoer.mobile.HugoerApplication
import com.hugoer.mobile.data.model.ContentItem
import com.hugoer.mobile.data.model.ParamFieldItem
import com.hugoer.mobile.data.model.ParamFieldKind
import com.hugoer.mobile.data.model.SiteProfile
import com.hugoer.mobile.data.model.ThemeInfo
import com.hugoer.mobile.ui.theme.HugoerTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels {
        AppViewModel.Factory((application as HugoerApplication).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { HugoerTheme { HugoerApp(viewModel) } }
    }
}

@Composable
private fun HugoerApp(viewModel: AppViewModel) {
    val state by viewModel.state.collectAsState()
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var connectOpen by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error, state.status) {
        val message = state.error ?: state.status
        if (!message.isNullOrBlank()) snackbarHostState.showSnackbar(message)
    }

    when {
        settingsOpen -> SettingsScreen(state, viewModel, onBack = { settingsOpen = false })
        connectOpen -> ConnectScreen(state, viewModel, onBack = { connectOpen = false })
        state.editor != null -> EditorScreen(state.editor!!, state, viewModel, onBack = viewModel::closeEditor)
        else -> MainShell(state, viewModel, onSettings = { settingsOpen = true }, onConnect = { connectOpen = true }, snackbarHostState)
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
    AppTab.CONTENT -> Icons.Default.Article
    AppTab.GITHUB -> Icons.Default.Code
}

@Composable
private fun SitesScreen(state: AppUiState, vm: AppViewModel, onConnect: () -> Unit) {
    if (state.sites.isEmpty()) {
        EmptyState("尚未連結任何 Hugo 網站", "新增一個 GitHub repository，開始管理你的網站。", "新增網站", onConnect)
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("已連結的網站", style = MaterialTheme.typography.titleMedium) }
        items(state.sites, key = { it.id }) { site ->
            SwipeSiteRow(site, state.activeSiteId == site.id, vm)
        }
        item {
            Spacer(Modifier.height(70.dp))
            Text("左右滑動網站列即可移除本機連結（不會刪除 GitHub 內容）。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeSiteRow(site: SiteProfile, active: Boolean, vm: AppViewModel) {
    var confirmDelete by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(confirmValueChange = { value ->
        if (value == SwipeToDismissBoxValue.EndToStart || value == SwipeToDismissBoxValue.StartToEnd) {
            confirmDelete = true
        }
        false
    })
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(16.dp)).padding(horizontal = 20.dp), contentAlignment = Alignment.CenterEnd) {
                Icon(Icons.Default.Delete, "移除", tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    ) {
        Card(
            onClick = { vm.selectSite(site) },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Cloud, null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(site.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(if (active) "目前作用中 · ${site.defaultBranch}" else "${site.defaultBranch} · Pages ${site.pagesUrl}", style = MaterialTheme.typography.bodySmall)
                }
                if (active) Icon(Icons.Default.Check, "目前作用中", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("移除網站") },
            text = { Text("確定要移除「${site.fullName}」嗎？（不會刪除 GitHub 上的內容，僅移除本機連結與已儲存的權杖）") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; vm.removeSite(site) }) { Text("移除") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConnectScreen(state: AppUiState, vm: AppViewModel, onBack: () -> Unit) {
    var repository by rememberSaveable { mutableStateOf("") }
    var token by rememberSaveable { mutableStateOf("") }
    var tokenVisible by rememberSaveable { mutableStateOf(false) }
    Scaffold(topBar = {
        TopAppBar(title = { Text("新增網站") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") } })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).imePadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("連結 GitHub Hugo 網站", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("支援 owner/repo 或完整 GitHub 網址。PAT 僅會以 Android Keystore 加密保存。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(repository, { repository = it }, Modifier.fillMaxWidth(), label = { Text("GitHub repository") }, placeholder = { Text("owner/repo") }, singleLine = true, leadingIcon = { Icon(Icons.Default.Link, null) })
            OutlinedTextField(token, { token = it }, Modifier.fillMaxWidth(), label = { Text("GitHub Personal Access Token") }, singleLine = true, visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), trailingIcon = { IconButton(onClick = { tokenVisible = !tokenVisible }) { Icon(if (tokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, "顯示權杖") } })
            Text("需要 Contents、Actions、Pages 讀寫權限，以及 Metadata 唯讀權限。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { vm.verifySite(repository, token) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("驗證連線") }
            if (state.verificationSummary.isNotBlank()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(state.verificationSummary) } }
                Button(onClick = { vm.saveVerifiedSite(); onBack() }, modifier = Modifier.fillMaxWidth()) { Text("儲存並連結") }
            }
            state.error?.let { ErrorText(it) }
        }
    }
}

@Composable
private fun ConfigScreen(state: AppUiState, vm: AppViewModel) {
    if (state.activeSiteId == null) { EmptyState("請先連結網站", "設定檔會套用到目前作用中的 GitHub Hugo 網站。", null, {}) ; return }
    var segment by rememberSaveable { mutableStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("基本", "Params 表單", "原始 TOML").forEachIndexed { index, label ->
                if (segment == index) Button(onClick = { segment = index }, modifier = Modifier.weight(1f)) { Text(label) }
                else OutlinedButton(onClick = { segment = index }, modifier = Modifier.weight(1f)) { Text(label) }
            }
        }
        when (segment) {
            0 -> BasicConfig(state, vm)
            1 -> ParamsConfig(state, vm)
            else -> RawConfig(state, vm)
        }
    }
}

@Composable
private fun BasicConfig(state: AppUiState, vm: AppViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("${state.sites.firstOrNull { it.id == state.activeSiteId }?.fullName.orEmpty()} · ${state.configPath ?: "hugo.toml"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ConfigField("baseURL", state.baseUrl, { vm.updateBasic(baseUrl = it) }, KeyboardType.Uri)
        ConfigField("title", state.siteTitle, { vm.updateBasic(title = it) })
        ConfigField("languageCode", state.languageCode, { vm.updateBasic(language = it) }, KeyboardType.Ascii)
        ConfigField("theme", state.themeName, { vm.updateBasic(theme = it) })
        Button(onClick = vm::saveBasic, modifier = Modifier.fillMaxWidth(), enabled = !state.busy) { Icon(Icons.Default.Save, null); Spacer(Modifier.width(8.dp)); Text("儲存基本設定") }
    }
}

@Composable
private fun ParamsConfig(state: AppUiState, vm: AppViewModel) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("常用 Hugo / Stack 參數", style = MaterialTheme.typography.titleMedium); Text("關閉開關或清空欄位後儲存，會移除該參數。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(state.paramFields, key = { it.path }) { field -> ParamFieldRow(field, vm) }
        item { Button(onClick = vm::saveParams, modifier = Modifier.fillMaxWidth(), enabled = !state.busy) { Icon(Icons.Default.Save, null); Spacer(Modifier.width(8.dp)); Text("儲存 Params") } }
        item { Spacer(Modifier.height(60.dp)) }
    }
}

@Composable
private fun ParamFieldRow(field: ParamFieldItem, vm: AppViewModel) {
    Card {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(field.displayName, fontWeight = FontWeight.Bold)
                    Text("${field.path} · ${field.kindLabel}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text(field.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (field.isBool) Switch(checked = field.boolValue, onCheckedChange = { vm.updateParam(field.path, boolValue = it, configured = true) })
                else Switch(checked = field.isConfigured, onCheckedChange = { vm.updateParam(field.path, configured = it) })
            }
            if (field.isEditable && !field.isBool && field.isConfigured) {
                OutlinedTextField(field.stringValue, { vm.updateParam(field.path, stringValue = it) }, Modifier.fillMaxWidth(), label = { Text("值") }, singleLine = true)
            }
        }
    }
}

@Composable
private fun RawConfig(state: AppUiState, vm: AppViewModel) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("原始 TOML", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(state.rawToml, vm::updateRawToml, Modifier.fillMaxWidth().weight(1f), label = { Text(state.configPath ?: "hugo.toml") }, minLines = 16)
        Button(onClick = vm::saveRaw, modifier = Modifier.fillMaxWidth(), enabled = !state.busy) { Icon(Icons.Default.Save, null); Spacer(Modifier.width(8.dp)); Text("儲存原始 TOML") }
    }
}

@Composable
private fun ThemesScreen(state: AppUiState, vm: AppViewModel) {
    if (state.activeSiteId == null) { EmptyState("請先連結網站", "主題套用會更新 GitHub 上的 theme 設定值。", null, {}); return }
    var pendingTheme by remember { mutableStateOf<ThemeInfo?>(null) }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("目前主題", style = MaterialTheme.typography.labelLarge)
                    Text(state.currentTheme.ifBlank { "尚未設定" }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("套用只會更新 theme 值；主題檔案需先存在 repo。", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        items(vm.themeCatalog, key = { it.themeConfigValue }) { theme ->
            ThemeCard(theme, state.currentTheme == theme.themeConfigValue, onApply = { pendingTheme = theme })
        }
        item { Spacer(Modifier.height(60.dp)) }
    }
    pendingTheme?.let { theme ->
        AlertDialog(onDismissRequest = { pendingTheme = null }, title = { Text("套用主題") }, text = { Text("將 theme 設定改為「${theme.themeConfigValue}」並推送到 GitHub？\n請確認此主題檔案已存在於 repo，否則網站建置會失敗。") }, confirmButton = { TextButton(onClick = { pendingTheme = null; vm.applyTheme(theme) }) { Text("套用") } }, dismissButton = { TextButton(onClick = { pendingTheme = null }) { Text("取消") } })
    }
}

@Composable
private fun ThemeCard(theme: ThemeInfo, selected: Boolean, onApply: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Palette, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(10.dp)); Text(theme.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); if (selected) { Spacer(Modifier.weight(1f)); AssistChip(onClick = {}, label = { Text("目前") }) } }
            Text(theme.description, style = MaterialTheme.typography.bodyMedium)
            Text(theme.repoUrl, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            OutlinedButton(onClick = onApply, modifier = Modifier.fillMaxWidth()) { Text(if (selected) "重新套用" else "一鍵套用") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContentScreen(state: AppUiState, vm: AppViewModel) {
    var showNewPost by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<ContentItem?>(null) }
    if (state.activeSiteId == null) { EmptyState("請先連結網站", "文章會直接讀寫 GitHub repository 的 content/。", null, {}); return }
    PullToRefreshBox(
        isRefreshing = state.busy,
        onRefresh = vm::loadContent,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (state.contentPath != "content") IconButton(onClick = vm::goUpContent) { Icon(Icons.Default.ArrowBack, "上一層") }
                Column(Modifier.weight(1f)) { Text(state.contentPath, fontWeight = FontWeight.Bold); Text("向下滑動即可重新整理", style = MaterialTheme.typography.labelSmall) }
                IconButton(onClick = vm::loadContent) { Icon(Icons.Default.Refresh, "重新整理") }
                IconButton(onClick = { showNewPost = true }) { Icon(Icons.Default.Add, "新增文章") }
            }
            OutlinedTextField(state.contentSearch, vm::setContentSearch, Modifier.fillMaxWidth().padding(horizontal = 12.dp), label = { Text("搜尋檔案") }, singleLine = true)
            if (state.contentItems.isEmpty()) EmptyState("這個目錄是空的", "新增文章會建立 Markdown 檔案並直接提交。", "新增文章", { showNewPost = true })
            else LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.contentItems, key = { it.path }) { item ->
                    ContentRow(item, onOpen = { if (item.isDirectory) vm.enterContentDirectory(item) else vm.openExistingPost(item) }, onDelete = { pendingDelete = item })
                }
            }
        }
    }
    if (showNewPost) AlertDialog(onDismissRequest = { showNewPost = false }, title = { Text("新增文章") }, text = { OutlinedTextField(newTitle, { newTitle = it }, label = { Text("文章標題") }, placeholder = { Text("我的第一篇文章") }) }, confirmButton = { TextButton(onClick = { showNewPost = false; vm.startNewPost(newTitle); newTitle = "" }) { Text("建立") } }, dismissButton = { TextButton(onClick = { showNewPost = false }) { Text("取消") } })
    pendingDelete?.let { item -> AlertDialog(onDismissRequest = { pendingDelete = null }, title = { Text("刪除文章") }, text = { Text("確定要刪除「${item.name}」嗎？此動作會直接提交到 GitHub。") }, confirmButton = { TextButton(onClick = { pendingDelete = null; vm.deleteContentItem(item) }) { Text("刪除") } }, dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消") } }) }
}

@Composable
private fun ContentRow(item: ContentItem, onOpen: () -> Unit, onDelete: () -> Unit) {
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth()) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(if (item.isDirectory) Icons.Default.Folder else Icons.Default.Description, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(item.name, fontWeight = FontWeight.Medium); Text(if (item.isDirectory) "資料夾" else "Markdown · ${item.size} bytes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; if (!item.isDirectory) IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "刪除", tint = MaterialTheme.colorScheme.error) } } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorScreen(editor: EditorState, state: AppUiState, vm: AppViewModel, onBack: () -> Unit) {
    var dark by remember { mutableStateOf(false) }
    LaunchedEffect(editor.body, editor.isPreview) { vm.refreshPreview(dark) }
    Scaffold(topBar = { TopAppBar(title = { Text(if (editor.isNew) "新增文章" else "編輯文章") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") } }, actions = { IconButton(onClick = vm::togglePreview) { Icon(if (editor.isPreview) Icons.Default.Edit else Icons.Default.Visibility, "切換預覽") } }) }, bottomBar = { Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).navigationBarsPadding().imePadding().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) { Button(onClick = vm::saveEditor, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Save, null); Spacer(Modifier.width(6.dp)); Text("儲存") }; if (!editor.isNew) OutlinedButton(onClick = vm::deleteEditor) { Icon(Icons.Default.Delete, null); Text("刪除") } } }) { padding ->
        if (editor.isPreview) {
            AndroidView(factory = { context -> WebView(context).apply { webViewClient = WebViewClient(); settings.javaScriptEnabled = false } }, update = { web -> web.loadDataWithBaseURL(null, editor.previewHtml, "text/html", "UTF-8", null) }, modifier = Modifier.fillMaxSize().padding(padding))
        } else {
            Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).imePadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(editor.path, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(editor.title, { vm.updateEditor(title = it) }, Modifier.fillMaxWidth(), label = { Text("標題") }, singleLine = true)
                OutlinedTextField(editor.date, { vm.updateEditor(date = it) }, Modifier.fillMaxWidth(), label = { Text("日期") }, singleLine = true)
                OutlinedTextField(editor.tags, { vm.updateEditor(tags = it) }, Modifier.fillMaxWidth(), label = { Text("標籤（逗號分隔）") }, singleLine = true)
                OutlinedTextField(editor.categories, { vm.updateEditor(categories = it) }, Modifier.fillMaxWidth(), label = { Text("分類（逗號分隔）") }, singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically) { Text("草稿", Modifier.weight(1f)); Switch(editor.draft, { vm.updateEditor(draft = it) }) }
                OutlinedTextField(editor.body, { vm.updateEditor(body = it) }, Modifier.fillMaxWidth().height(360.dp), label = { Text("Markdown 內文") }, minLines = 14)
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun GithubScreen(state: AppUiState, vm: AppViewModel) {
    val context = LocalContext.current
    if (state.activeSiteId == null) { EmptyState("請先連結網站", "在這裡查看 repository 權限、Pages 與 Actions。", null, {}); return }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { Text("部署與 GitHub 狀態", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f)); IconButton(onClick = vm::loadGithub) { Icon(Icons.Default.Refresh, "重新整理") } } }
        item { Card { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Cloud, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(12.dp)); Column { Text("Repository 權限", style = MaterialTheme.typography.labelLarge); Text(state.permissionSummary.ifBlank { "讀取中…" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) } } } }
        item { Card(colors = CardDefaults.cardColors(containerColor = if (state.pages?.enabled == true) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("GitHub Pages", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text(state.pages?.message ?: "尚未讀取"); if (state.pages?.enabled == true) { Text("建置來源：${state.pages.buildType ?: "workflow"}"); state.pages.htmlUrl?.let { url -> OutlinedButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.OpenInBrowser, null); Spacer(Modifier.width(8.dp)); Text("開啟已發佈網站") } } } else Button(onClick = vm::enablePages, modifier = Modifier.fillMaxWidth()) { Text("啟用 GitHub Pages") } } } }
        item { Button(onClick = vm::deployNow, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Sync, null); Spacer(Modifier.width(8.dp)); Text("立即部署") } }
        item { Text("Actions 執行紀錄", style = MaterialTheme.typography.titleMedium) }
        if (state.runs.isEmpty()) item { Text("尚無執行紀錄。", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(state.runs, key = { it.id }) { run -> Card { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(if (run.statusLabel == "成功") Icons.Default.Check else Icons.Default.Sync, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(run.name, fontWeight = FontWeight.Medium); Text("${run.statusLabel} · ${run.headBranch} · ${formatDate(run.createdAt)}", style = MaterialTheme.typography.bodySmall) }; if (run.htmlUrl.isNotBlank()) Icon(Icons.Default.OpenInBrowser, "開啟") } } }
        item { Spacer(Modifier.height(60.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(state: AppUiState, vm: AppViewModel, onBack: () -> Unit) {
    var confirmClear by remember { mutableStateOf(false) }
    Scaffold(topBar = { TopAppBar(title = { Text("設定") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("連結的網站", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (state.sites.isEmpty()) Text("目前沒有已連結的網站。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            else state.sites.forEach { Text("• ${it.fullName}") }
            Divider()
            OutlinedButton(onClick = { confirmClear = true }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Delete, null); Spacer(Modifier.width(8.dp)); Text("清除所有網站與權杖") }
            Text("GitHub PAT 以 EncryptedSharedPreferences（Android Keystore）保存；網站清單不含權杖。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Divider()
            Text("關於", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Hugoer Mobile", style = MaterialTheme.typography.titleLarge)
            Text("版本 1.0.0\n透過 GitHub REST API 管理 Hugo 網站。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (confirmClear) AlertDialog(onDismissRequest = { confirmClear = false }, title = { Text("清除所有已連結網站") }, text = { Text("將移除本機所有 Hugo 網站連結與已儲存的 GitHub 權杖。此動作無法復原。") }, confirmButton = { TextButton(onClick = { confirmClear = false; vm.clearAllSites(); onBack() }) { Text("清除") } }, dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("取消") } })
}

@Composable
private fun ConfigField(label: String, value: String, onValueChange: (String) -> Unit, keyboard: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(value, onValueChange, Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = keyboard))
}

@Composable
private fun EmptyState(title: String, body: String, action: String?, onAction: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.MenuBook, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp)); Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp)); Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); if (action != null) { Spacer(Modifier.height(20.dp)); Button(onClick = onAction) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text(action) } }
    }
}

@Composable
private fun ErrorText(message: String) = Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)

private fun formatDate(time: Long): String = if (time == 0L) "" else SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.TAIWAN).format(Date(time))

