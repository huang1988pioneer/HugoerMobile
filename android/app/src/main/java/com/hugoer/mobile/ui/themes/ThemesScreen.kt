package com.hugoer.mobile.ui.themes

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
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.hugoer.mobile.data.model.ThemeInfo
import com.hugoer.mobile.ui.common.EmptyState

@Composable
internal fun ThemesScreen(state: AppUiState, vm: AppViewModel) {
    if (state.activeSiteId == null) {
        EmptyState("請先連結網站", "主題套用會更新 GitHub 上的 theme 設定值。", null, {})
        return
    }
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
        AlertDialog(
            onDismissRequest = { pendingTheme = null },
            title = { Text("套用主題") },
            text = { Text("將 theme 設定改為「${theme.themeConfigValue}」並推送到 GitHub？\n請確認此主題檔案已存在於 repo，否則網站建置會失敗。") },
            confirmButton = { TextButton(onClick = { pendingTheme = null; vm.applyTheme(theme) }) { Text("套用") } },
            dismissButton = { TextButton(onClick = { pendingTheme = null }) { Text("取消") } }
        )
    }
}

@Composable
private fun ThemeCard(theme: ThemeInfo, selected: Boolean, onApply: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Palette, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text(theme.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (selected) {
                    Spacer(Modifier.weight(1f))
                    AssistChip(onClick = {}, label = { Text("目前") })
                }
            }
            Text(theme.description, style = MaterialTheme.typography.bodyMedium)
            Text(theme.repoUrl, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            OutlinedButton(onClick = onApply, modifier = Modifier.fillMaxWidth()) { Text(if (selected) "重新套用" else "一鍵套用") }
        }
    }
}
