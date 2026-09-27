package com.hugoer.mobile.ui.content

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TextFields
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.hugoer.mobile.AppUiState
import com.hugoer.mobile.AppViewModel
import com.hugoer.mobile.EditorState
import com.hugoer.mobile.ui.ArticlePreview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditorScreen(editor: EditorState, state: AppUiState, vm: AppViewModel, onBack: () -> Unit) {
    var dark by remember { mutableStateOf(false) }
    var sourceMode by rememberSaveable { mutableStateOf(false) }
    var frontMatterExpanded by rememberSaveable(editor.path) { mutableStateOf(editor.isNew) }
    val baseUrl = state.baseUrl.ifBlank {
        state.sites.firstOrNull { it.id == state.activeSiteId }?.pagesUrl.orEmpty()
    }
    LaunchedEffect(editor.body, editor.isPreview) { vm.refreshPreview(dark) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editor.isNew) "新增文章" else "編輯文章") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                actions = {
                    if (!editor.isPreview) {
                        IconButton(onClick = { sourceMode = !sourceMode }) {
                            Icon(
                                if (sourceMode) Icons.Default.TextFields else Icons.Default.Code,
                                if (sourceMode) "切換為所見即所得編輯" else "切換為 Markdown 原始碼"
                            )
                        }
                    }
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
                    baseUrl = baseUrl,
                    documentPath = editor.path,
                )
                Spacer(Modifier.height(72.dp))
            }
        } else {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(editor.path, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    Modifier.fillMaxWidth().clickable { frontMatterExpanded = !frontMatterExpanded }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("前置資料（Front Matter）", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                    Icon(if (frontMatterExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, if (frontMatterExpanded) "收合" else "展開")
                }
                if (frontMatterExpanded) {
                    Column(
                        Modifier.fillMaxWidth().heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(editor.title, { vm.updateEditor(title = it) }, Modifier.fillMaxWidth(), label = { Text("標題") }, singleLine = true)
                        OutlinedTextField(editor.date, { vm.updateEditor(date = it) }, Modifier.fillMaxWidth(), label = { Text("日期") }, singleLine = true)
                        OutlinedTextField(editor.tags, { vm.updateEditor(tags = it) }, Modifier.fillMaxWidth(), label = { Text("標籤（逗號分隔）") }, singleLine = true)
                        OutlinedTextField(editor.categories, { vm.updateEditor(categories = it) }, Modifier.fillMaxWidth(), label = { Text("分類（逗號分隔）") }, singleLine = true)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("草稿", Modifier.weight(1f)); Switch(editor.draft, { vm.updateEditor(draft = it) })
                        }
                    }
                }
                if (sourceMode) {
                    OutlinedTextField(editor.body, { vm.updateEditor(body = it) }, Modifier.fillMaxWidth().weight(1f), label = { Text("Markdown 內文") })
                } else {
                    Text("內文（所見即所得，儲存為 Markdown）", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val shape = RoundedCornerShape(8.dp)
                    QuillMarkdownEditor(
                        markdown = editor.body,
                        onMarkdownChange = { vm.updateEditor(body = it) },
                        baseUrl = baseUrl,
                        documentPath = editor.path,
                        modifier = Modifier.fillMaxWidth().weight(1f).clip(shape)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
                    )
                }
            }
        }
    }
}
