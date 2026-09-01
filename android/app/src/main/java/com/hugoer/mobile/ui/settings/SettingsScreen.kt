package com.hugoer.mobile.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hugoer.mobile.AppUiState
import com.hugoer.mobile.AppViewModel
import com.hugoer.mobile.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(state: AppUiState, vm: AppViewModel, onBack: () -> Unit) {
    var confirmClear by remember { mutableStateOf(false) }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("設定") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }
        )
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("連結的網站", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (state.sites.isEmpty()) Text("目前沒有已連結的網站。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            else state.sites.forEach { Text("• ${it.fullName}") }
            HorizontalDivider()
            OutlinedButton(onClick = { confirmClear = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Delete, null); Spacer(Modifier.width(8.dp)); Text("清除所有網站與權杖")
            }
            Text(
                "GitHub PAT 以 EncryptedSharedPreferences（Android Keystore）保存；網站清單不含權杖。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider()
            Text("關於", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Hugoer Mobile", style = MaterialTheme.typography.titleLarge)
            Text(
                "版本 ${BuildConfig.VERSION_NAME}\n透過 GitHub REST API 管理 Hugo 網站。",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("清除所有已連結網站") },
            text = { Text("將移除本機所有 Hugo 網站連結與已儲存的 GitHub 權杖。此動作無法復原。") },
            confirmButton = { TextButton(onClick = { confirmClear = false; vm.clearAllSites(); onBack() }) { Text("清除") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("取消") } }
        )
    }
}
