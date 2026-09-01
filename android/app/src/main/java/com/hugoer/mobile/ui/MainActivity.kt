package com.hugoer.mobile.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.hugoer.mobile.AppViewModel
import com.hugoer.mobile.HugoerApplication
import com.hugoer.mobile.ui.nav.HugoerApp
import com.hugoer.mobile.ui.theme.HugoerTheme

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels {
        AppViewModel.Factory((application as HugoerApplication).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { HugoerTheme { HugoerApp(viewModel) } }
    }
}
