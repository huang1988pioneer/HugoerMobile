package com.hugoer.mobile.ui.sites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.hugoer.mobile.AppUiState
import com.hugoer.mobile.AppViewModel
import com.hugoer.mobile.ui.common.ErrorText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConnectScreen(state: AppUiState, vm: AppViewModel, onBack: () -> Unit) {
    var repository by rememberSaveable { mutableStateOf("") }
    var token by rememberSaveable { mutableStateOf("") }
    var tokenVisible by rememberSaveable { mutableStateOf(false) }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("新增網站") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }
        )
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).imePadding().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("連結 GitHub Hugo 網站", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("支援 owner/repo 或完整 GitHub 網址。PAT 僅會以 Android Keystore 加密保存。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                repository,
                { repository = it },
                Modifier.fillMaxWidth(),
                label = { Text("GitHub repository") },
                placeholder = { Text("owner/repo") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Link, null) }
            )
            OutlinedTextField(
                token,
                { token = it },
                Modifier.fillMaxWidth(),
                label = { Text("GitHub Personal Access Token") },
                singleLine = true,
                visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { tokenVisible = !tokenVisible }) {
                        Icon(if (tokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, "顯示權杖")
                    }
                }
            )
            Text("需要 Contents、Actions、Pages 讀寫權限，以及 Metadata 唯讀權限。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { vm.verifySite(repository, token) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("驗證連線") }
            if (state.verificationSummary.isNotBlank()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(state.verificationSummary) }
                }
                Button(onClick = { vm.saveVerifiedSite(); onBack() }, modifier = Modifier.fillMaxWidth()) { Text("儲存並連結") }
            }
            state.error?.let { ErrorText(it) }
        }
    }
}
