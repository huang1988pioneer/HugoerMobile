package com.hugoer.mobile.ui.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hugoer.mobile.AppUiState
import com.hugoer.mobile.AppViewModel
import com.hugoer.mobile.EditorState
import com.hugoer.mobile.ui.ArticlePreview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditorScreen(editor: EditorState, state: AppUiState, vm: AppViewModel, onBack: () -> Unit) {
    var dark by remember { mutableStateOf(false) }
    LaunchedEffect(editor.body, editor.isPreview) { vm.refreshPreview(dark) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editor.isNew) "新增文章" else "編輯文章") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                actions = {
                    IconButton(onClick = vm::togglePreview) {
                        Icon(if (editor.isPreview) Icons.Default.Edit else Icons.Default.Visibility, "切換預覽")
                    }
                }
            )
        },
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).navigationBarsPadding().imePadding().padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(onClick = vm::saveEditor, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Save, null); Spacer(Modifier.width(6.dp)); Text("儲存")
                }
                if (!editor.isNew) {
                    OutlinedButton(onClick = vm::deleteEditor) {
                        Icon(Icons.Default.Delete, null); Text("刪除")
                    }
                }
            }
        }
    ) { padding ->
        if (editor.isPreview) {
            Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ArticlePreview(
                    title = editor.title,
                    markdown = editor.body,
                    baseUrl = state.baseUrl.ifBlank {
                        state.sites.firstOrNull { it.id == state.activeSiteId }?.pagesUrl.orEmpty()
                    },
                    documentPath = editor.path,
                )
                Spacer(Modifier.height(72.dp))
            }
        } else {
            Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).imePadding().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(editor.path, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(editor.title, { vm.updateEditor(title = it) }, Modifier.fillMaxWidth(), label = { Text("標題") }, singleLine = true)
                OutlinedTextField(editor.date, { vm.updateEditor(date = it) }, Modifier.fillMaxWidth(), label = { Text("日期") }, singleLine = true)
                OutlinedTextField(editor.tags, { vm.updateEditor(tags = it) }, Modifier.fillMaxWidth(), label = { Text("標籤（逗號分隔）") }, singleLine = true)
                OutlinedTextField(editor.categories, { vm.updateEditor(categories = it) }, Modifier.fillMaxWidth(), label = { Text("分類（逗號分隔）") }, singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("草稿", Modifier.weight(1f)); Switch(editor.draft, { vm.updateEditor(draft = it) })
                }
                OutlinedTextField(editor.body, { vm.updateEditor(body = it) }, Modifier.fillMaxWidth().height(360.dp), label = { Text("Markdown 內文") }, minLines = 14)
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}
