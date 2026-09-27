package com.hugoer.mobile.ui.content

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import org.json.JSONObject

private const val EDITOR_ORIGIN = "https://${WebViewAssetLoader.DEFAULT_DOMAIN}"
private const val EDITOR_URL = "$EDITOR_ORIGIN/assets/editor/index.html"
private const val BRIDGE_NAME = "hugoerBridge"

/**
 * Rich Markdown editor backed by the bundled Quill page (shared/web/editor, packaged as
 * assets/editor/). Markdown goes in via [markdown] and every user edit comes back through
 * [onMarkdownChange]; untouched documents are never re-serialised.
 */
@Composable
internal fun QuillMarkdownEditor(
    markdown: String,
    onMarkdownChange: (String) -> Unit,
    baseUrl: String,
    documentPath: String,
    modifier: Modifier = Modifier,
) {
    val dark = isSystemInDarkTheme()
    val latestOnChange by rememberUpdatedState(onMarkdownChange)
    val bridge = remember { EditorBridge { latestOnChange(it) } }
    bridge.markdown = markdown
    bridge.baseUrl = baseUrl
    bridge.documentPath = documentPath
    bridge.dark = dark

    AndroidView(
        modifier = modifier,
        factory = { context -> createEditorWebView(context, bridge) },
        update = { webView -> bridge.sync(webView) },
        onRelease = { webView ->
            bridge.webView = null
            webView.destroy()
        },
    )
}

private class EditorBridge(private val onChange: (String) -> Unit) {
    var markdown = ""
    var baseUrl = ""
    var documentPath = ""
    var dark = false
    var webView: WebView? = null

    private var ready = false
    /** The Markdown the page currently holds, so its own edits are not pushed back. */
    private var syncedMarkdown: String? = null
    private var appliedDark: Boolean? = null

    fun sync(view: WebView) {
        if (!ready) return
        if (markdown != syncedMarkdown) {
            syncedMarkdown = markdown
            appliedDark = dark
            val payload = JSONObject()
                .put("markdown", markdown)
                .put("baseURL", baseUrl)
                .put("documentPath", documentPath)
                .put("dark", dark)
            view.evaluateJavascript("window.HugoerEditor.setContent($payload);", null)
        } else if (dark != appliedDark) {
            appliedDark = dark
            view.evaluateJavascript("window.HugoerEditor.setTheme($dark);", null)
        }
    }

    /** Must be called on the main thread. */
    fun onMessage(json: String) {
        val message = runCatching { JSONObject(json) }.getOrNull() ?: return
        when (message.optString("type")) {
            "ready" -> {
                ready = true
                webView?.let(::sync)
            }
            "change" -> {
                val updated = message.optString("markdown")
                syncedMarkdown = updated
                if (updated != markdown) {
                    markdown = updated
                    onChange(updated)
                }
            }
            "error" -> Log.w("QuillMarkdownEditor", message.optString("message"))
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createEditorWebView(context: Context, bridge: EditorBridge): WebView {
    val assetLoader = WebViewAssetLoader.Builder()
        .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
        .build()

    return WebView(context).apply {
        bridge.webView = this
        setBackgroundColor(Color.TRANSPARENT)
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = false
            allowFileAccess = false
            allowContentAccess = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            setSupportMultipleWindows(false)
            javaScriptCanOpenWindowsAutomatically = false
        }
        webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                assetLoader.shouldInterceptRequest(request.url)

            // Only the bundled editor page loads; links inside articles never navigate.
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = true
        }

        if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            // The allowed-origin rule keeps the bridge away from anything but the bundled page.
            WebViewCompat.addWebMessageListener(this, BRIDGE_NAME, setOf(EDITOR_ORIGIN)) { _, message, _, isMainFrame, _ ->
                if (isMainFrame) message.data?.let(bridge::onMessage)
            }
        } else {
            // Older WebView providers: the page only ever loads bundled assets, so a JS interface is safe here.
            addJavascriptInterface(object {
                @JavascriptInterface
                fun postMessage(json: String) {
                    post { bridge.onMessage(json) }
                }
            }, BRIDGE_NAME)
        }

        loadUrl(EDITOR_URL)
    }
}
