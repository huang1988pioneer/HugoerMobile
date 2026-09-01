package com.hugoer.mobile.ui.github

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hugoer.mobile.AppUiState
import com.hugoer.mobile.AppViewModel
import com.hugoer.mobile.ui.common.EmptyState
import com.hugoer.mobile.ui.common.formatDate

@Composable
internal fun GithubScreen(state: AppUiState, vm: AppViewModel) {
    val context = LocalContext.current
    if (state.activeSiteId == null) {
        EmptyState("請先連結網站", "在這裡查看 repository 權限、Pages 與 Actions。", null, {})
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("部署與 GitHub 狀態", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = vm::loadGithub) { Icon(Icons.Default.Refresh, "重新整理") }
            }
        }
        item {
            Card {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Cloud, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Repository 權限", style = MaterialTheme.typography.labelLarge)
                        Text(state.permissionSummary.ifBlank { "讀取中…" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (state.pages?.enabled == true) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("GitHub Pages", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(state.pages?.message ?: "尚未讀取")
                    if (state.pages?.enabled == true) {
                        Text("建置來源：${state.pages.buildType ?: "workflow"}")
                        state.pages.htmlUrl?.let { url ->
                            OutlinedButton(
                                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.OpenInBrowser, null)
                                Spacer(Modifier.width(8.dp))
                                Text("開啟已發佈網站")
                            }
                        }
                    } else {
                        Button(onClick = vm::enablePages, modifier = Modifier.fillMaxWidth()) { Text("啟用 GitHub Pages") }
                    }
                }
            }
        }
        item {
            Button(onClick = vm::deployNow, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Sync, null)
                Spacer(Modifier.width(8.dp))
                Text("立即部署")
            }
        }
        item { Text("Actions 執行紀錄", style = MaterialTheme.typography.titleMedium) }
        if (state.runs.isEmpty()) item { Text("尚無執行紀錄。", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(state.runs, key = { it.id }) { run ->
            Card(
                onClick = {
                    if (run.htmlUrl.isNotBlank()) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(run.htmlUrl)))
                    }
                }
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (run.statusLabel == "成功") Icons.Default.Check else Icons.Default.Sync,
                        null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(run.name, fontWeight = FontWeight.Medium)
                        Text("${run.statusLabel} · ${run.headBranch} · ${formatDate(run.createdAt)}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (run.htmlUrl.isNotBlank()) Icon(Icons.Default.OpenInBrowser, "開啟")
                }
            }
        }
        item { Spacer(Modifier.height(60.dp)) }
    }
}
