package com.huang1988pioneer.hugoer.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.huang1988pioneer.hugoer.mobile.domain.model.Article
import com.huang1988pioneer.hugoer.mobile.domain.model.Deployment
import com.huang1988pioneer.hugoer.mobile.domain.model.DeploymentStatus
import com.huang1988pioneer.hugoer.mobile.domain.model.Site
import com.huang1988pioneer.hugoer.mobile.presentation.HugoerUiState

/** The five-tab surface from the original Hugoer Mobile experience. */
@Composable
fun ReferenceTopBar(
    title: String,
    onSettings: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(96.dp)
                .padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = onSettings,
                modifier = Modifier.size(52.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "設定",
                    modifier = Modifier.size(34.dp),
                )
            }
        }
    }
}

@Composable
fun ReferenceFloatingActionButton(
    contentDescription: String,
    onClick: () -> Unit,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier.navigationBarsPadding(),
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.primary,
    ) {
        Icon(Icons.Filled.Add, contentDescription = contentDescription, modifier = Modifier.size(38.dp))
    }
}

@Composable
fun ReferenceSitesScreen(
    state: HugoerUiState,
    onSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        ReferenceTopBar(title = "站台", onSettings = onSettings)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Text(
                    "已連結的網站",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Normal,
                )
            }
            item {
                ReferenceSiteCard(
                    site = state.site,
                )
            }
            item {
                Text(
                    "左右滑動網站列即可移除本機連結（不會刪除 GitHub 內容）。",
                    modifier = Modifier.padding(top = 180.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun ReferenceSiteCard(
    site: Site,
) {
    val owner = site.repository.substringBefore('/', site.repository)
    val repository = site.repository.substringAfter('/', site.repository)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Icon(Icons.Filled.Cloud, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("$owner/", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text(repository, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("目前作用中 · ${site.branch}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Icon(Icons.Filled.Check, contentDescription = "目前作用中", modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun ReferenceConfigScreen(
    state: HugoerUiState,
    onSave: (String) -> Unit,
    onSettings: () -> Unit,
) {
    var segment by rememberSaveable { mutableStateOf(0) }
    var baseUrl by rememberSaveable { mutableStateOf("https://fenghuanghsin.github.io/") }
    var title by rememberSaveable { mutableStateOf("星空下微風輕拂黃光閃耀的世界三") }
    var languageCode by rememberSaveable { mutableStateOf("") }
    var theme by rememberSaveable { mutableStateOf("stack") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        ReferenceTopBar(title = "設定檔", onSettings = onSettings)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ReferenceSegment("基本", selected = segment == 0, onClick = { segment = 0 }, modifier = Modifier.weight(1f))
            ReferenceSegment("Params\n表單", selected = segment == 1, onClick = { segment = 1 }, modifier = Modifier.weight(1f))
            ReferenceSegment("原始\nTOML", selected = segment == 2, onClick = { segment = 2 }, modifier = Modifier.weight(1f))
        }
        when (segment) {
            0 -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    "${state.site.repository} · hugo.toml",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                OutlinedTextField(baseUrl, { baseUrl = it }, Modifier.fillMaxWidth(), label = { Text("baseURL") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri))
                OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("title") }, singleLine = true)
                OutlinedTextField(languageCode, { languageCode = it }, Modifier.fillMaxWidth(), placeholder = { Text("languageCode") }, singleLine = true)
                OutlinedTextField(theme, { theme = it }, Modifier.fillMaxWidth(), label = { Text("theme") }, singleLine = true)
                Button(
                    onClick = { onSave("已儲存基本設定") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(30.dp),
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("儲存基本設定", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(24.dp))
            }
            1 -> ReferenceParamsTab(onSave = onSave)
            else -> ReferenceRawTomlTab(onSave = onSave)
        }
    }
}

@Composable
private fun ReferenceSegment(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier.height(76.dp),
            shape = RoundedCornerShape(40.dp),
            contentPadding = PaddingValues(horizontal = 8.dp),
        ) { Text(label, textAlign = TextAlign.Center, style = MaterialTheme.typography.titleLarge) }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(76.dp),
            shape = RoundedCornerShape(40.dp),
            contentPadding = PaddingValues(horizontal = 8.dp),
        ) { Text(label, textAlign = TextAlign.Center, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
private fun ReferenceParamsTab(onSave: (String) -> Unit) {
    var description by rememberSaveable { mutableStateOf("桌面版 Hugoer 預設參數") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("常用 Hugo / Stack 參數", style = MaterialTheme.typography.headlineSmall)
        Text("關閉開關或清空欄位後儲存，會移除該參數。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(description, { description = it }, Modifier.fillMaxWidth(), label = { Text("description") }, minLines = 3)
        OutlinedTextField("true", {}, Modifier.fillMaxWidth(), label = { Text("showReadingTime") }, singleLine = true)
        Button(onClick = { onSave("已儲存 Params") }, modifier = Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(30.dp)) {
            Icon(Icons.Filled.Save, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Text("儲存 Params")
        }
    }
}

@Composable
private fun ReferenceRawTomlTab(onSave: (String) -> Unit) {
    var rawToml by rememberSaveable { mutableStateOf("baseURL = \\\"https://fenghuanghsin.github.io/\\\"\ntitle = \\\"星空下微風輕拂黃光閃耀的世界三\\\"\nlanguageCode = \\\"\\\"\ntheme = \\\"stack\\\"") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("原始 TOML", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(rawToml, { rawToml = it }, Modifier.fillMaxWidth().weight(1f), label = { Text("hugo.toml") }, minLines = 14)
        Button(onClick = { onSave("已儲存原始 TOML") }, modifier = Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(30.dp)) {
            Icon(Icons.Filled.Save, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Text("儲存原始 TOML")
        }
    }
}

@Composable
fun ReferenceThemesScreen(
    onApply: (String) -> Unit,
    onSettings: () -> Unit,
) {
    var currentTheme by rememberSaveable { mutableStateOf("stack") }
    val themes = listOf(
        ReferenceTheme("Stack", "stack", "桌面版 Hugoer 預設一鍵安裝的卡片式部落格主題，支援側欄、留言、Mermaid、圖片處理等豐富 params。", "https://github.com/CaiJimmy/hugo-theme-stack"),
        ReferenceTheme("PaperMod", "PaperMod", "簡潔快速、SEO 友善，適合技術部落格與筆記型網站。", "https://github.com/adityatelange/hugo-PaperMod"),
    )
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ReferenceTopBar(title = "主題", onSettings = onSettings)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, top = 26.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
                    Column(Modifier.fillMaxWidth().padding(26.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("目前主題", style = MaterialTheme.typography.titleLarge)
                        Text(currentTheme, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                        Text("套用只會更新 theme 值；主題檔案需先存在 repo。", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            items(themes, key = { it.configValue }) { theme ->
                ReferenceThemeCard(theme, selected = currentTheme.equals(theme.configValue, ignoreCase = true), onApply = {
                    currentTheme = theme.configValue
                    onApply(theme.name)
                })
            }
        }
    }
}

private data class ReferenceTheme(val name: String, val configValue: String, val description: String, val url: String)

@Composable
private fun ReferenceThemeCard(
    theme: ReferenceTheme,
    selected: Boolean,
    onApply: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.padding(26.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(Icons.Filled.Palette, contentDescription = null, modifier = Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary)
                Text(theme.name, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            }
            Text(theme.description, style = MaterialTheme.typography.bodyLarge)
            Text(theme.url, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            OutlinedButton(onClick = onApply, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(30.dp)) {
                Text(if (selected) "一鍵套用" else "一鍵套用", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferenceContentScreen(
    state: HugoerUiState,
    onOpenArticle: (Article) -> Unit,
    onCreateArticle: () -> Unit,
    onSync: () -> Unit,
    onSettings: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val article = state.articles.firstOrNull()
    val fileName = article?.path?.substringAfterLast('/') ?: "無標題.md"
    val visible = query.isBlank() || fileName.contains(query, ignoreCase = true)
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ReferenceTopBar(title = "內容", onSettings = onSettings)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, top = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = onSync, modifier = Modifier.size(52.dp)) { Icon(Icons.Filled.ArrowBack, contentDescription = "上一層", modifier = Modifier.size(34.dp)) }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("content/post", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("向下滑動即可重新整理", style = MaterialTheme.typography.bodyMedium)
                    }
                    IconButton(onClick = onSync, modifier = Modifier.size(52.dp)) { Icon(Icons.Filled.Refresh, contentDescription = "重新整理", modifier = Modifier.size(32.dp)) }
                    IconButton(onClick = onCreateArticle, modifier = Modifier.size(52.dp)) { Icon(Icons.Filled.Add, contentDescription = "新增文章", modifier = Modifier.size(34.dp)) }
                }
            }
            item {
                OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().height(76.dp), placeholder = { Text("搜尋檔案") }, singleLine = true)
            }
            if (visible && article != null) {
                item { ReferenceContentRow(fileName = fileName, onOpen = { onOpenArticle(article) }) }
            } else {
                item {
                    Text("找不到符合條件的檔案", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 20.dp))
                }
            }
        }
    }
}

@Composable
private fun ReferenceContentRow(
    fileName: String,
    onOpen: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 26.dp, vertical = 26.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Icon(Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(52.dp), tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(fileName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Markdown · 374 bytes", style = MaterialTheme.typography.titleMedium)
            }
            IconButton(onClick = {}, modifier = Modifier.size(52.dp)) { Icon(Icons.Filled.Delete, contentDescription = "刪除", modifier = Modifier.size(34.dp), tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
fun ReferenceGitHubScreen(
    state: HugoerUiState,
    onDeploy: () -> Unit,
    onSync: () -> Unit,
    onSettings: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ReferenceTopBar(title = "GitHub", onSettings = onSettings)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, top = 24.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("部署與 GitHub 狀態", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                    IconButton(onClick = onSync, modifier = Modifier.size(52.dp)) { Icon(Icons.Filled.Refresh, contentDescription = "重新整理", modifier = Modifier.size(34.dp)) }
                }
            }
            item {
                Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                    Row(Modifier.fillMaxWidth().padding(26.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Icon(Icons.Filled.Cloud, contentDescription = null, modifier = Modifier.size(52.dp), tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text("Repository 權限", style = MaterialTheme.typography.titleLarge)
                            Text("管理員權限", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            item {
                Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
                    Column(Modifier.fillMaxWidth().padding(26.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("GitHub Pages", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("網站已成功建置並上線。", style = MaterialTheme.typography.titleLarge)
                        Text("建置來源：workflow", style = MaterialTheme.typography.bodyLarge)
                        OutlinedButton(onClick = { }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(30.dp)) {
                            Icon(Icons.Filled.OpenInBrowser, contentDescription = null)
                            Spacer(Modifier.width(10.dp))
                            Text("開啟已發佈網站")
                        }
                    }
                }
            }
            item {
                Button(onClick = onDeploy, modifier = Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(30.dp), enabled = !state.isDeploying) {
                    if (state.isDeploying) CircularProgressIndicator(modifier = Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    else Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(if (state.isDeploying) "部署中…" else "立即部署", style = MaterialTheme.typography.titleMedium)
                }
            }
            item { Text("Actions 執行紀錄", style = MaterialTheme.typography.headlineSmall) }
            items(state.deployments, key = { it.id }) { deployment -> ReferenceActionRow(deployment) }
        }
    }
}

@Composable
private fun ReferenceActionRow(deployment: Deployment) {
    val success = deployment.status == DeploymentStatus.LIVE
    Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 22.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Icon(if (success) Icons.Filled.Check else Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(42.dp), tint = if (success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
            Column(modifier = Modifier.weight(1f)) {
                Text("Deploy Hugo site to Pages", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${if (success) "成功" else "失敗"} · ${deployment.time}", style = MaterialTheme.typography.bodyLarge)
            }
            Icon(Icons.Filled.OpenInBrowser, contentDescription = "開啟", modifier = Modifier.size(30.dp))
        }
    }
}

@Composable
fun ReferenceSettingsDialog(
    site: Site,
    onDismiss: () -> Unit,
    onAction: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
        title = { Text("設定") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("目前網站")
                Text(site.repository, fontWeight = FontWeight.Bold)
                Text("GitHub 權杖與網站連結會安全地保存在裝置上。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = { onAction("設定已儲存") }) { Text("完成") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
