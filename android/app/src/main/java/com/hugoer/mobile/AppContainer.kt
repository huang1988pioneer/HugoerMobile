package com.hugoer.mobile

import android.content.Context
import com.hugoer.mobile.data.local.SecureTokenStore
import com.hugoer.mobile.data.local.SiteProfileStore
import com.hugoer.mobile.data.remote.GitHubApiService
import com.hugoer.mobile.data.service.ConfigLocator
import com.hugoer.mobile.data.service.FrontMatterService
import com.hugoer.mobile.data.service.MarkdownPreviewService
import com.hugoer.mobile.data.service.ThemeCatalogService
import com.hugoer.mobile.data.service.TomlParamsService
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** Small manual dependency container; deliberately avoids Hilt/Koin/Dagger. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    val githubApi = GitHubApiService(httpClient)
    val siteStore = SiteProfileStore(appContext)
    val tokenStore = SecureTokenStore(appContext)
    val tomlParams = TomlParamsService()
    val frontMatter = FrontMatterService()
    val markdownPreview = MarkdownPreviewService()
    val themeCatalog = ThemeCatalogService()
    val configLocator = ConfigLocator(githubApi)
}

class HugoerApplication : android.app.Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
