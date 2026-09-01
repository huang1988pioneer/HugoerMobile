package com.hugoer.mobile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hugoer.mobile.data.local.SecureTokenStore
import com.hugoer.mobile.data.local.SiteProfileStore
import com.hugoer.mobile.data.model.ContentItem
import com.hugoer.mobile.data.model.FrontMatterDocument
import com.hugoer.mobile.data.model.GitHubPagesStatus
import com.hugoer.mobile.data.model.GitHubRepoInfo
import com.hugoer.mobile.data.model.GitHubRepositoryTarget
import com.hugoer.mobile.data.model.ParamFieldItem
import com.hugoer.mobile.data.model.SiteProfile
import com.hugoer.mobile.data.model.ThemeInfo
import com.hugoer.mobile.data.model.WorkflowRunInfo
import com.hugoer.mobile.data.remote.GitHubApiException
import com.hugoer.mobile.data.remote.GitHubApiService
import com.hugoer.mobile.data.service.ConfigLocator
import com.hugoer.mobile.data.service.FrontMatterService
import com.hugoer.mobile.data.service.MarkdownPreviewService
import com.hugoer.mobile.data.service.Slug
import com.hugoer.mobile.data.service.ThemeCatalogService
import com.hugoer.mobile.data.service.TomlParamsService
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

enum class AppTab(val label: String) {
    SITES("站台"), CONFIG("設定檔"), THEMES("主題"), CONTENT("內容"), GITHUB("GitHub")
}

data class EditorState(
    val path: String,
    val sha: String = "",
    val isNew: Boolean = false,
    val title: String = "",
    val date: String = "",
    val tags: String = "",
    val categories: String = "",
    val draft: Boolean = true,
    val body: String = "",
    val isPreview: Boolean = false,
    val previewHtml: String = "",
    val dirty: Boolean = false
)

data class AppUiState(
    val sites: List<SiteProfile> = emptyList(),
    val activeSiteId: String? = null,
    val selectedTab: AppTab = AppTab.SITES,
    val busy: Boolean = false,
    val error: String? = null,
    val status: String? = null,
    val contentPath: String = "content",
    val contentSearch: String = "",
    val contentItems: List<ContentItem> = emptyList(),
    val configPath: String? = null,
    val configSha: String? = null,
    val baseUrl: String = "",
    val siteTitle: String = "",
    val languageCode: String = "",
    val themeName: String = "",
    val rawToml: String = "",
    val paramFields: List<ParamFieldItem> = emptyList(),
    val currentTheme: String = "",
    val pages: GitHubPagesStatus? = null,
    val permissionSummary: String = "",
    val runs: List<WorkflowRunInfo> = emptyList(),
    val editor: EditorState? = null,
    val verifiedSite: SiteProfile? = null,
    val verificationSummary: String = ""
)

/**
 * Main MVVM state holder. All remote work is dispatched off the main thread and all UI
 * observes a single StateFlow, making the tab screens easy to keep consistent after a site switch.
 */
class AppViewModel(
    private val api: GitHubApiService,
    private val siteStore: SiteProfileStore,
    private val tokenStore: SecureTokenStore,
    private val tomlParams: TomlParamsService,
    private val configLocator: ConfigLocator,
    private val frontMatter: FrontMatterService,
    private val markdownPreview: MarkdownPreviewService,
    private val themes: ThemeCatalogService
) : ViewModel() {
    private val _state = MutableStateFlow(AppUiState())
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    private var verifiedTarget: GitHubRepositoryTarget? = null
    private var verifiedRepo: GitHubRepoInfo? = null
    private var verifiedLogin: String? = null
    private var verifiedToken: String = ""
    private val workMutex = Mutex()
    private var searchJob: Job? = null

    init { loadSites() }

    val themeCatalog: List<ThemeInfo> get() = themes.getCatalog()

    fun selectTab(tab: AppTab) {
        _state.update { it.copy(selectedTab = tab, error = null, status = null) }
        when (tab) {
            AppTab.CONFIG -> loadConfig()
            AppTab.THEMES -> loadThemes()
            AppTab.CONTENT -> loadContent()
            AppTab.GITHUB -> loadGithub()
            AppTab.SITES -> loadSites()
        }
    }

    fun clearMessage() = _state.update { it.copy(error = null, status = null) }

    fun selectSite(site: SiteProfile) {
        _state.update { it.copy(activeSiteId = site.id, status = "已切換至 ${site.fullName}。", error = null) }
        persistActiveSite(site.id)
        when (_state.value.selectedTab) {
            AppTab.CONFIG -> loadConfig()
            AppTab.THEMES -> loadThemes()
            AppTab.CONTENT -> loadContent()
            AppTab.GITHUB -> loadGithub()
            else -> Unit
        }
    }

    fun loadSites() = launchWork {
        val profiles = withContext(Dispatchers.IO) { siteStore.load() }
        val savedActive = withContext(Dispatchers.IO) { siteStore.loadActiveSiteId() }
        val active = listOf(_state.value.activeSiteId, savedActive)
            .firstOrNull { id -> id != null && profiles.any { it.id == id } }
            ?: profiles.firstOrNull()?.id
        persistActiveSite(active)
        _state.update { it.copy(sites = profiles, activeSiteId = active) }
    }

    fun removeSite(site: SiteProfile) = launchWork {
        withContext(Dispatchers.IO) {
            tokenStore.remove(site.tokenKey)
            siteStore.save(_state.value.sites.filterNot { it.id == site.id })
        }
        val remaining = _state.value.sites.filterNot { it.id == site.id }
        val next = if (_state.value.activeSiteId == site.id) remaining.firstOrNull()?.id else _state.value.activeSiteId
        persistActiveSite(next)
        _state.update { it.copy(sites = remaining, activeSiteId = next, status = "已移除 ${site.fullName} 的本機連結。") }
    }

    fun clearAllSites() = launchWork {
        withContext(Dispatchers.IO) {
            _state.value.sites.forEach { tokenStore.remove(it.tokenKey) }
            siteStore.save(emptyList())
            tokenStore.clear()
            siteStore.saveActiveSiteId(null)
        }
        _state.update { it.copy(sites = emptyList(), activeSiteId = null, selectedTab = AppTab.SITES, status = "已清除所有網站與權杖。") }
    }

    fun verifySite(repositoryInput: String, token: String) = launchWork {
        verifiedTarget = null; verifiedRepo = null; verifiedLogin = null; verifiedToken = ""
        val target = GitHubRepositoryTarget.parse(repositoryInput)
        if (!target.isValid) throw UserFacingException(target.errorMessage)
        if (token.isBlank()) throw UserFacingException("請輸入 GitHub Personal Access Token。")

        val login = withContext(Dispatchers.IO) { api.getAuthenticatedUserLogin(token) }
            ?: throw UserFacingException("權杖無效或已過期，請重新產生後再試一次。")
        val repo = withContext(Dispatchers.IO) { api.getRepoInfo(target.owner, target.repository, token) }
            ?: throw UserFacingException("找不到 repository ${target.owner}/${target.repository}，或此權杖沒有存取權限。")

        verifiedTarget = target; verifiedRepo = repo; verifiedLogin = login; verifiedToken = token
        val summary = "已登入為 $login\n權限：${permissionLabel(repo)}\n預設分支：${repo.defaultBranch}\nPages 網址：${target.pagesUrl}"
        val profile = SiteProfile(
            owner = target.owner,
            repository = target.repository,
            displayName = target.repository,
            defaultBranch = repo.defaultBranch,
            pagesUrl = target.pagesUrl,
            isUserOrOrganizationSite = target.isUserOrOrganizationSite
        )
        _state.update { it.copy(verifiedSite = profile, verificationSummary = summary, status = "連線驗證成功。") }
    }

    fun saveVerifiedSite() = launchWork {
        val target = verifiedTarget ?: throw UserFacingException("請先驗證連線。")
        val repo = verifiedRepo ?: throw UserFacingException("請先驗證連線。")
        val token = verifiedToken
        val existing = _state.value.sites.firstOrNull {
            it.owner.equals(target.owner, ignoreCase = true) &&
                it.repository.equals(target.repository, ignoreCase = true)
        }
        val profile = SiteProfile(
            id = existing?.id ?: java.util.UUID.randomUUID().toString().replace("-", ""),
            owner = target.owner,
            repository = target.repository,
            displayName = target.repository,
            defaultBranch = repo.defaultBranch,
            pagesUrl = target.pagesUrl,
            isUserOrOrganizationSite = target.isUserOrOrganizationSite,
            addedAtUtc = existing?.addedAtUtc ?: System.currentTimeMillis()
        )
        val sites = _state.value.sites.filterNot { it.id == profile.id } + profile
        withContext(Dispatchers.IO) {
            tokenStore.save(profile.tokenKey, token)
            siteStore.save(sites)
            siteStore.saveActiveSiteId(profile.id)
        }
        _state.update {
            it.copy(
                sites = sites,
                activeSiteId = profile.id,
                verifiedSite = null,
                verificationSummary = "",
                selectedTab = AppTab.SITES,
                status = if (existing == null) "已新增並連結 ${profile.fullName}。" else "已更新 ${profile.fullName} 的連線。"
            )
        }
        verifiedTarget = null; verifiedRepo = null; verifiedToken = ""
    }

    fun loadConfig() = launchWork {
        val site = activeSiteOrNull() ?: return@launchWork clearConfig()
        val token = tokenFor(site) ?: return@launchWork
        val file = withContext(Dispatchers.IO) { configLocator.find(site, token) }
        val raw = file?.textContent.orEmpty()
        val basic = tomlParams.readBasicFields(raw)
        _state.update {
            it.copy(
                configPath = file?.path ?: configLocator.defaultPath,
                configSha = file?.sha,
                rawToml = raw,
                baseUrl = basic.baseUrl,
                siteTitle = basic.title,
                languageCode = basic.languageCode,
                themeName = basic.theme,
                paramFields = tomlParams.loadParamsForm(raw),
                status = if (file == null) "找不到現有設定檔，儲存後將建立新的 hugo.toml。" else "已載入 ${file.path}"
            )
        }
    }

    fun updateBasic(baseUrl: String? = null, title: String? = null, language: String? = null, theme: String? = null) {
        _state.update { it.copy(baseUrl = baseUrl ?: it.baseUrl, siteTitle = title ?: it.siteTitle, languageCode = language ?: it.languageCode, themeName = theme ?: it.themeName) }
    }

    fun updateRawToml(raw: String) = _state.update { it.copy(rawToml = raw) }

    fun updateParam(path: String, stringValue: String? = null, boolValue: Boolean? = null, configured: Boolean? = null) {
        _state.update { current ->
            current.copy(paramFields = current.paramFields.map { field ->
                if (field.path != path) field else field.copy(
                    stringValue = stringValue ?: field.stringValue,
                    boolValue = boolValue ?: field.boolValue,
                    isConfigured = configured ?: field.isConfigured
                )
            })
        }
    }

    fun saveBasic() = launchWork {
        val current = _state.value; val site = activeSiteOrNull() ?: return@launchWork
        val token = tokenFor(site) ?: return@launchWork
        val path = current.configPath ?: configLocator.defaultPath
        val updated = withContext(Dispatchers.Default) {
            tomlParams.upsertSimpleRootKeys(current.rawToml, mapOf("baseURL" to current.baseUrl, "title" to current.siteTitle, "languageCode" to current.languageCode, "theme" to current.themeName))
        }
        putConfig(site, token, path, current.configSha, updated)
    }

    fun saveParams() = launchWork {
        val current = _state.value; val site = activeSiteOrNull() ?: return@launchWork
        val token = tokenFor(site) ?: return@launchWork
        val path = current.configPath ?: configLocator.defaultPath
        val updated = withContext(Dispatchers.Default) { tomlParams.applyParamsToToml(current.rawToml, current.paramFields) }
        putConfig(site, token, path, current.configSha, updated)
    }

    fun saveRaw() = launchWork {
        val current = _state.value; val site = activeSiteOrNull() ?: return@launchWork
        val token = tokenFor(site) ?: return@launchWork
        putConfig(site, token, current.configPath ?: configLocator.defaultPath, current.configSha, current.rawToml)
    }

    private suspend fun putConfig(site: SiteProfile, token: String, path: String, sha: String?, updated: String) {
        val result = withContext(Dispatchers.IO) { api.putFile(site.owner, site.repository, path, updated, "Update Hugo config via Hugoer Mobile", sha, site.defaultBranch, token) }
        val basic = tomlParams.readBasicFields(updated)
        _state.update { it.copy(configPath = path, configSha = result.sha, rawToml = updated, baseUrl = basic.baseUrl, siteTitle = basic.title, languageCode = basic.languageCode, themeName = basic.theme, currentTheme = basic.theme, paramFields = tomlParams.loadParamsForm(updated), status = "已儲存並推送到 GitHub。") }
    }

    fun loadThemes() = launchWork {
        val site = activeSiteOrNull() ?: return@launchWork
        val token = tokenFor(site) ?: return@launchWork
        val file = withContext(Dispatchers.IO) { configLocator.find(site, token) }
        val raw = file?.textContent.orEmpty()
        _state.update { it.copy(configPath = file?.path ?: configLocator.defaultPath, configSha = file?.sha, rawToml = raw, currentTheme = tomlParams.readBasicFields(raw).theme) }
    }

    fun applyTheme(theme: ThemeInfo) = launchWork {
        val current = _state.value; val site = activeSiteOrNull() ?: return@launchWork
        val token = tokenFor(site) ?: return@launchWork
        val updated = withContext(Dispatchers.Default) { tomlParams.upsertSimpleRootKeys(current.rawToml, mapOf("theme" to theme.themeConfigValue)) }
        val result = withContext(Dispatchers.IO) { api.putFile(site.owner, site.repository, current.configPath ?: configLocator.defaultPath, updated, "Switch theme to ${theme.themeConfigValue} via Hugoer Mobile", current.configSha, site.defaultBranch, token) }
        _state.update { it.copy(configSha = result.sha, rawToml = updated, currentTheme = theme.themeConfigValue, themeName = theme.themeConfigValue, status = "已套用主題 ${theme.themeConfigValue} 並推送到 GitHub。") }
    }

    fun setContentSearch(value: String) {
        _state.update { it.copy(contentSearch = value) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            loadContent()
        }
    }

    fun loadContent() = launchWork {
        val site = activeSiteOrNull() ?: run { _state.update { it.copy(contentItems = emptyList()) }; return@launchWork }
        val token = tokenFor(site) ?: return@launchWork
        val items = withContext(Dispatchers.IO) { api.listDirectory(site.owner, site.repository, _state.value.contentPath, site.defaultBranch, token) }
        val search = _state.value.contentSearch
        _state.update { it.copy(contentItems = items.filter { item -> (item.isDirectory || item.isMarkdown) && (search.isBlank() || item.name.contains(search, ignoreCase = true)) }) }
    }

    fun enterContentDirectory(item: ContentItem) { if (item.isDirectory) { _state.update { it.copy(contentPath = item.path) }; loadContent() } }
    fun goUpContent() {
        val current = _state.value.contentPath
        if (current == "content") return
        val parent = current.substringBeforeLast('/', "content").ifBlank { "content" }
        _state.update { it.copy(contentPath = parent) }; loadContent()
    }

    fun startNewPost(title: String) {
        if (title.isBlank()) return
        val slug = Slug.fromTitle(title)
        val targetDir = if (_state.value.contentPath == "content") "content/posts" else _state.value.contentPath
        openEditor("$targetDir/$slug.md", isNew = true, newTitle = title)
    }

    fun openExistingPost(item: ContentItem) { openEditor(item.path, item.sha.orEmpty(), false, "") }

    private fun openEditor(path: String, sha: String = "", isNew: Boolean, newTitle: String) {
        _state.update { it.copy(editor = EditorState(path = path, sha = sha, isNew = isNew), error = null) }
        if (isNew) {
            val doc = FrontMatterDocument().apply { title = newTitle; date = java.time.OffsetDateTime.now().toString(); draft = true; body = "開始寫作吧。" }
            updateEditorFromDocument(doc, path, sha, true)
        } else loadEditor()
    }

    fun closeEditor() = _state.update { it.copy(editor = null) }

    private fun loadEditor() = launchWork {
        val editor = _state.value.editor ?: return@launchWork
        val site = activeSiteOrNull() ?: return@launchWork
        val token = tokenFor(site) ?: return@launchWork
        val file = withContext(Dispatchers.IO) { api.tryGetFile(site.owner, site.repository, editor.path, site.defaultBranch, token) }
            ?: throw UserFacingException("找不到這篇文章，可能已在其他裝置被刪除或搬移。")
        val doc = frontMatter.parse(file.textContent)
        updateEditorFromDocument(doc, file.path, file.sha, false)
    }

    private fun updateEditorFromDocument(doc: FrontMatterDocument, path: String, sha: String, isNew: Boolean) {
        _state.update { it.copy(editor = EditorState(path, sha, isNew, doc.title, doc.date, doc.tags, doc.categories, doc.draft, doc.body, false, markdownPreview.renderToHtmlDocument(doc.body, false), false)) }
    }

    fun updateEditor(title: String? = null, date: String? = null, tags: String? = null, categories: String? = null, draft: Boolean? = null, body: String? = null) {
        _state.update { current -> current.copy(editor = current.editor?.let { e -> e.copy(title = title ?: e.title, date = date ?: e.date, tags = tags ?: e.tags, categories = categories ?: e.categories, draft = draft ?: e.draft, body = body ?: e.body, dirty = true) }) }
    }

    fun togglePreview() = _state.update { current ->
            val editor = current.editor ?: return@update current
            current.copy(editor = editor.copy(isPreview = !editor.isPreview, previewHtml = markdownPreview.renderToHtmlDocument(editor.body, false)))
        }

    fun refreshPreview(isDark: Boolean) = _state.update { it.copy(editor = it.editor?.copy(previewHtml = markdownPreview.renderToHtmlDocument(it.editor.body, isDark))) }

    fun saveEditor() = launchWork {
        val current = _state.value; val editor = current.editor ?: return@launchWork; val site = activeSiteOrNull() ?: return@launchWork
        val token = tokenFor(site) ?: return@launchWork
        val doc = FrontMatterDocument().apply { title = editor.title; date = editor.date; tags = editor.tags; categories = editor.categories; draft = editor.draft; body = editor.body }
        val fullText = frontMatter.write(doc)
        val result = withContext(Dispatchers.IO) { api.putFile(site.owner, site.repository, editor.path, fullText, if (editor.isNew) "Add ${editor.path} via Hugoer Mobile" else "Update ${editor.path} via Hugoer Mobile", editor.sha.ifEmpty { null }, site.defaultBranch, token) }
        _state.update { it.copy(editor = editor.copy(sha = result.sha, isNew = false, dirty = false), status = "已儲存並推送到 GitHub。") }
    }

    fun deleteEditor() = launchWork {
        val editor = _state.value.editor ?: return@launchWork
        if (editor.sha.isBlank()) return@launchWork
        val site = activeSiteOrNull() ?: return@launchWork; val token = tokenFor(site) ?: return@launchWork
        withContext(Dispatchers.IO) { api.deleteFile(site.owner, site.repository, editor.path, editor.sha, "Delete ${editor.path} via Hugoer Mobile", site.defaultBranch, token) }
        _state.update { it.copy(editor = null, status = "已刪除文章並推送到 GitHub。") }
    }

    fun deleteContentItem(item: ContentItem) = launchWork {
        if (item.isDirectory || item.sha == null) return@launchWork
        val site = activeSiteOrNull() ?: return@launchWork; val token = tokenFor(site) ?: return@launchWork
        withContext(Dispatchers.IO) { api.deleteFile(site.owner, site.repository, item.path, item.sha, "Delete ${item.path} via Hugoer Mobile", site.defaultBranch, token) }
        _state.update { it.copy(contentItems = it.contentItems.filterNot { it.path == item.path }, status = "已刪除 ${item.name} 並推送到 GitHub。") }
    }

    fun loadGithub() = launchWork {
        val site = activeSiteOrNull() ?: return@launchWork
        val token = tokenFor(site) ?: return@launchWork
        refreshGithub(site, token)
    }

    private suspend fun refreshGithub(site: SiteProfile, token: String) {
        val (repo, pages, runs) = coroutineScope {
            val repoDeferred = async(Dispatchers.IO) { api.getRepoInfo(site.owner, site.repository, token) }
            val pagesDeferred = async(Dispatchers.IO) { api.getPagesStatus(site.owner, site.repository, token) }
            val runsDeferred = async(Dispatchers.IO) { api.listWorkflowRuns(site.owner, site.repository, token) }
            Triple(repoDeferred.await(), pagesDeferred.await(), runsDeferred.await())
        }
        _state.update { it.copy(permissionSummary = repo?.let(::permissionLabel) ?: "無法讀取權限", pages = pages, runs = runs) }
    }

    fun enablePages() = launchWork {
        val site = activeSiteOrNull() ?: return@launchWork; val token = tokenFor(site) ?: return@launchWork
        val result = withContext(Dispatchers.IO) { api.enablePagesFromActions(site.owner, site.repository, token) }
        if (!result.first) throw UserFacingException(result.second)
        _state.update { it.copy(status = result.second) }
        refreshGithub(site, token)
    }

    fun deployNow() = launchWork {
        val site = activeSiteOrNull() ?: return@launchWork; val token = tokenFor(site) ?: return@launchWork
        withContext(Dispatchers.IO) {
            api.ensureDeployWorkflow(site.owner, site.repository, site.defaultBranch, token)
            val workflows = api.listWorkflows(site.owner, site.repository, token)
            val workflow = workflows.firstOrNull { it.path.endsWith("hugo.yml", ignoreCase = true) } ?: workflows.firstOrNull()
                ?: throw UserFacingException("找不到可觸發的 GitHub Actions workflow。")
            api.dispatchWorkflow(site.owner, site.repository, workflow.id, site.defaultBranch, token)
        }
        _state.update { it.copy(status = "已觸發部署，稍後下拉重新整理查看進度。") }
        delay(1500)
        refreshGithub(site, token)
    }

    fun previewHtml(isDark: Boolean): String = _state.value.editor?.let { markdownPreview.renderToHtmlDocument(it.body, isDark) }.orEmpty()

    private fun clearConfig() = _state.update { it.copy(configPath = null, configSha = null, rawToml = "", paramFields = emptyList(), baseUrl = "", siteTitle = "", languageCode = "", themeName = "", currentTheme = "") }

    private fun activeSiteOrNull(): SiteProfile? = _state.value.sites.firstOrNull { it.id == _state.value.activeSiteId }

    private suspend fun tokenFor(site: SiteProfile): String? {
        val token = withContext(Dispatchers.IO) { tokenStore.get(site.tokenKey) }
        if (token.isNullOrEmpty()) {
            _state.update { it.copy(error = "找不到此網站的權杖，請於「站台」重新連結。") }
            return null
        }
        return token
    }

    private fun persistActiveSite(id: String?) {
        viewModelScope.launch(Dispatchers.IO) { siteStore.saveActiveSiteId(id) }
    }

    private fun permissionLabel(repo: GitHubRepoInfo): String = when {
        repo.permissions.admin -> "管理員權限"
        repo.permissions.push -> "可推送內容"
        else -> "唯讀權限"
    }

    private fun userFacingMessage(error: Exception): String = when (error) {
        is UserFacingException, is GitHubApiException -> error.message ?: "操作失敗。"
        is IOException -> "網路連線失敗，請稍後再試。"
        else -> error.message ?: "操作失敗。"
    }

    private fun launchWork(block: suspend () -> Unit) {
        viewModelScope.launch {
            workMutex.withLock {
                _state.update { it.copy(busy = true, error = null) }
                try {
                    block()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _state.update { it.copy(error = userFacingMessage(e)) }
                } finally {
                    _state.update { it.copy(busy = false) }
                }
            }
        }
    }

    class UserFacingException(message: String) : Exception(message)

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AppViewModel(
            container.githubApi, container.siteStore, container.tokenStore, container.tomlParams,
            container.configLocator, container.frontMatter, container.markdownPreview, container.themeCatalog
        ) as T
    }
}
