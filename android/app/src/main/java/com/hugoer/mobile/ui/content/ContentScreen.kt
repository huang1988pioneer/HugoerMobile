package com.hugoer.mobile.ui.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hugoer.mobile.AppUiState
import com.hugoer.mobile.AppViewModel
import com.hugoer.mobile.data.model.ContentItem
import com.hugoer.mobile.ui.common.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ContentScreen(state: AppUiState, vm: AppViewModel) {
    var showNewPost by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<ContentItem?>(null) }
    if (state.activeSiteId == null) {
        EmptyState("請先連結網站", "文章會直接讀寫 GitHub repository 的 content/。", null, {})
        return
    }
    PullToRefreshBox(
        isRefreshing = state.busy,
        onRefresh = vm::loadContent,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (state.contentPath != "content") IconButton(onClick = vm::goUpContent) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "上一層") }
                Column(Modifier.weight(1f)) {
                    Text(state.contentPath, fontWeight = FontWeight.Bold)
                    Text("向下滑動即可重新整理", style = MaterialTheme.typography.labelSmall)
                }
                IconButton(onClick = vm::loadContent) { Icon(Icons.Default.Refresh, "重新整理") }
                IconButton(onClick = { showNewPost = true }) { Icon(Icons.Default.Add, "新增文章") }
            }
            OutlinedTextField(
                state.contentSearch,
                vm::setContentSearch,
                Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                label = { Text("搜尋檔案") },
                singleLine = true
            )
            if (state.contentItems.isEmpty()) {
                EmptyState("這個目錄是空的", "新增文章會建立 Markdown 檔案並直接提交。", "新增文章", { showNewPost = true })
            } else {
                LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.contentItems, key = { it.path }) { item ->
                        ContentRow(
                            item,
                            onOpen = { if (item.isDirectory) vm.enterContentDirectory(item) else vm.openExistingPost(item) },
                            onDelete = { pendingDelete = item }
                        )
                    }
                }
            }
        }
    }
    if (showNewPost) {
        AlertDialog(
            onDismissRequest = { showNewPost = false },
            title = { Text("新增文章") },
            text = { OutlinedTextField(newTitle, { newTitle = it }, label = { Text("文章標題") }, placeholder = { Text("我的第一篇文章") }) },
            confirmButton = { TextButton(onClick = { showNewPost = false; vm.startNewPost(newTitle); newTitle = "" }) { Text("建立") } },
            dismissButton = { TextButton(onClick = { showNewPost = false }) { Text("取消") } }
        )
    }
    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("刪除文章") },
            text = { Text("確定要刪除「${item.name}」嗎？此動作會直接提交到 GitHub。") },
            confirmButton = { TextButton(onClick = { pendingDelete = null; vm.deleteContentItem(item) }) { Text("刪除") } },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消") } }
        )
    }
}

@Composable
private fun ContentRow(item: ContentItem, onOpen: () -> Unit, onDelete: () -> Unit) {
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (item.isDirectory) Icons.Default.Folder else Icons.Default.Description, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.Medium)
                Text(
                    if (item.isDirectory) "資料夾" else "Markdown · ${item.size} bytes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!item.isDirectory) {
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "刪除", tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}
