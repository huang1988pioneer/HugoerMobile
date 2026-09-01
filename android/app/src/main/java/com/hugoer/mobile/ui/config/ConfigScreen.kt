package com.hugoer.mobile.ui.config

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hugoer.mobile.AppUiState
import com.hugoer.mobile.AppViewModel
import com.hugoer.mobile.data.model.ParamFieldItem
import com.hugoer.mobile.ui.common.ConfigField
import com.hugoer.mobile.ui.common.EmptyState

@Composable
internal fun ConfigScreen(state: AppUiState, vm: AppViewModel) {
    if (state.activeSiteId == null) {
        EmptyState("請先連結網站", "設定檔會套用到目前作用中的 GitHub Hugo 網站。", null, {})
        return
    }
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
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "${state.sites.firstOrNull { it.id == state.activeSiteId }?.fullName.orEmpty()} · ${state.configPath ?: "hugo.toml"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ConfigField("baseURL", state.baseUrl, { vm.updateBasic(baseUrl = it) }, KeyboardType.Uri)
        ConfigField("title", state.siteTitle, { vm.updateBasic(title = it) })
        ConfigField("languageCode", state.languageCode, { vm.updateBasic(language = it) }, KeyboardType.Ascii)
        ConfigField("theme", state.themeName, { vm.updateBasic(theme = it) })
        Button(onClick = vm::saveBasic, modifier = Modifier.fillMaxWidth(), enabled = !state.busy) {
            Icon(Icons.Default.Save, null); Spacer(Modifier.width(8.dp)); Text("儲存基本設定")
        }
    }
}

@Composable
private fun ParamsConfig(state: AppUiState, vm: AppViewModel) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("常用 Hugo / Stack 參數", style = MaterialTheme.typography.titleMedium)
            Text("關閉開關或清空欄位後儲存，會移除該參數。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(state.paramFields, key = { it.path }) { field -> ParamFieldRow(field, vm) }
        item {
            Button(onClick = vm::saveParams, modifier = Modifier.fillMaxWidth(), enabled = !state.busy) {
                Icon(Icons.Default.Save, null); Spacer(Modifier.width(8.dp)); Text("儲存 Params")
            }
        }
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
        Button(onClick = vm::saveRaw, modifier = Modifier.fillMaxWidth(), enabled = !state.busy) {
            Icon(Icons.Default.Save, null); Spacer(Modifier.width(8.dp)); Text("儲存原始 TOML")
        }
    }
}
