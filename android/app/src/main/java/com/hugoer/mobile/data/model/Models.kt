package com.hugoer.mobile.data.model

import java.util.UUID
import java.util.regex.Pattern

/**
 * A Hugo site the user has connected to this device. Mirrors the desktop app's concept of
 * "an opened site folder", except here the site lives in a GitHub repository and is only
 * ever touched through the GitHub REST API.
 */
data class SiteProfile(
    val id: String = UUID.randomUUID().toString().replace("-", ""),
    val owner: String,
    val repository: String,
    var displayName: String = "",
    var defaultBranch: String = "main",
    var pagesUrl: String = "",
    var isUserOrOrganizationSite: Boolean = false,
    val addedAtUtc: Long = System.currentTimeMillis()
) {
    val fullName: String get() = "$owner/$repository"

    /** Key used to look up this site's PAT in the secure token store. */
    val tokenKey: String get() = "gh_pat::$id"
}

/** A file or folder inside the repo's content directory (GitHub Contents API item). */
data class ContentItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean = false,
    val sha: String? = null,
    val size: Long = 0
) {
    val isMarkdown: Boolean
        get() = !isDirectory &&
            (name.endsWith(".md", ignoreCase = true) || name.endsWith(".markdown", ignoreCase = true))
}

/** Parsed YAML front matter + Markdown body of a Hugo content file. */
class FrontMatterDocument(
    val fields: LinkedHashMap<String, String> = LinkedHashMap(),
    var body: String = ""
) {
    private fun get(key: String): String =
        fields.entries.firstOrNull { it.key.equals(key, ignoreCase = true) }?.value ?: ""

    private fun set(key: String, value: String) {
        val existingKey = fields.keys.firstOrNull { it.equals(key, ignoreCase = true) }
        fields[existingKey ?: key] = value
    }

    var title: String
        get() = get("title")
        set(value) = set("title", value)

    var date: String
        get() = get("date")
        set(value) = set("date", value)

    var tags: String
        get() = get("tags")
        set(value) = set("tags", value)

    var categories: String
        get() = get("categories")
        set(value) = set("categories", value)

    var draft: Boolean
        get() = get("draft").equals("true", ignoreCase = true)
        set(value) = set("draft", if (value) "true" else "false")
}

data class GitHubFileContent(
    val path: String,
    val sha: String,
    val textContent: String = ""
)

data class RepoPermissions(
    val pull: Boolean = false,
    val push: Boolean = false,
    val admin: Boolean = false
)

data class GitHubRepoInfo(
    val owner: String,
    val name: String,
    val defaultBranch: String = "main",
    val private: Boolean = false,
    val htmlUrl: String = "",
    val description: String? = null,
    val permissions: RepoPermissions = RepoPermissions()
)

data class GitHubPagesStatus(
    val enabled: Boolean = false,
    val status: String? = null,
    val htmlUrl: String? = null,
    val sourceBranch: String? = null,
    val sourcePath: String? = null,
    val buildType: String? = null,
    val message: String = ""
)

enum class WorkflowRunStatus { UNKNOWN, QUEUED, IN_PROGRESS, COMPLETED }

enum class WorkflowRunConclusion { NONE, SUCCESS, FAILURE, CANCELLED, SKIPPED, OTHER }

data class WorkflowRunInfo(
    val id: Long,
    val name: String,
    val status: WorkflowRunStatus,
    val conclusion: WorkflowRunConclusion,
    val headBranch: String,
    val event: String,
    val htmlUrl: String,
    val createdAt: Long
) {
    val statusLabel: String
        get() = when (status) {
            WorkflowRunStatus.QUEUED -> "排隊中"
            WorkflowRunStatus.IN_PROGRESS -> "建置中…"
            WorkflowRunStatus.COMPLETED -> when (conclusion) {
                WorkflowRunConclusion.SUCCESS -> "成功"
                WorkflowRunConclusion.FAILURE -> "失敗"
                WorkflowRunConclusion.CANCELLED -> "已取消"
                WorkflowRunConclusion.SKIPPED -> "已略過"
                else -> "已完成"
            }
            else -> "未知"
        }
}

data class GitHubWorkflowInfo(
    val id: Long,
    val name: String = "",
    val path: String = ""
)

enum class ParamFieldKind { STRING, BOOL, NUMBER, ARRAY, NESTED }

/** One row in the "Params 表單" section of the 設定檔 tab. Ported from the desktop app. */
data class ParamFieldItem(
    val key: String,
    val path: String,
    val displayName: String = "",
    val description: String = "",
    val kind: ParamFieldKind,
    var stringValue: String = "",
    var boolValue: Boolean = false,
    val isKnown: Boolean = false,
    var isConfigured: Boolean = false
) {
    val isEditable: Boolean get() = kind != ParamFieldKind.NESTED
    val isBool: Boolean get() = kind == ParamFieldKind.BOOL

    val kindLabel: String
        get() = when (kind) {
            ParamFieldKind.BOOL -> "布林"
            ParamFieldKind.NUMBER -> "數字"
            ParamFieldKind.ARRAY -> "陣列"
            ParamFieldKind.NESTED -> "巢狀"
            else -> "文字"
        }
}

/** A curated theme entry shown in the 主題 (Themes) tab. */
data class ThemeInfo(
    val name: String,
    val themeConfigValue: String,
    val repoUrl: String,
    val description: String = "",
    val recommendedForStackParams: Boolean = false
)

/**
 * Parses user-entered repository references ("owner/repo" or a full GitHub URL).
 * Ported from the desktop app's `GitHubService.ParseRepositoryTarget`.
 */
data class GitHubRepositoryTarget(
    val isValid: Boolean,
    val owner: String = "",
    val repository: String = "",
    val pagesUrl: String = "",
    val isUserOrOrganizationSite: Boolean = false,
    val errorMessage: String = ""
) {
    companion object {
        private val ownerRegex = Pattern.compile("^[A-Za-z0-9](?:[A-Za-z0-9-]{0,38})$")
        private val repoRegex = Pattern.compile("^[A-Za-z0-9._-]{1,100}$")

        fun parse(input: String?): GitHubRepositoryTarget {
            var value = input?.trim() ?: ""
            if (value.isBlank()) {
                return invalid("請輸入 GitHub repository，例如 owner/repo 或完整網址。")
            }

            if (!value.contains("://")) {
                value = if (value.startsWith("github.com/", ignoreCase = true)) {
                    "https://$value"
                } else {
                    "https://github.com/${value.trimStart('/')}"
                }
            }

            val uri = try {
                java.net.URI(value)
            } catch (e: Exception) {
                null
            }

            if (uri == null || !uri.scheme.equals("https", ignoreCase = true) ||
                uri.host == null || !uri.host.equals("github.com", ignoreCase = true)
            ) {
                return invalid("僅支援 owner/repo 或 https://github.com/owner/repository 格式。")
            }

            val segments = (uri.path ?: "").split('/').map { it.trim() }.filter { it.isNotEmpty() }
            if (segments.size < 2) {
                return invalid("請提供完整的 owner 與 repository 名稱。")
            }

            val owner = java.net.URLDecoder.decode(segments[0], "UTF-8")
            var repository = java.net.URLDecoder.decode(segments[1], "UTF-8")
            if (repository.endsWith(".git", ignoreCase = true)) {
                repository = repository.dropLast(4)
            }

            if (!ownerRegex.matcher(owner).matches() || !repoRegex.matcher(repository).matches()) {
                return invalid("GitHub owner 或 repository 名稱格式無效。")
            }

            val userSite = repository.equals("$owner.github.io", ignoreCase = true)
            val pagesUrl = if (userSite) {
                "https://${owner.lowercase()}.github.io/"
            } else {
                "https://${owner.lowercase()}.github.io/$repository/"
            }

            return GitHubRepositoryTarget(
                isValid = true,
                owner = owner,
                repository = repository,
                pagesUrl = pagesUrl,
                isUserOrOrganizationSite = userSite
            )
        }

        private fun invalid(message: String) = GitHubRepositoryTarget(isValid = false, errorMessage = message)
    }
}
