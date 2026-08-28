package com.hugoer.mobile.data.remote

import com.hugoer.mobile.data.model.ContentItem
import com.hugoer.mobile.data.model.GitHubFileContent
import com.hugoer.mobile.data.model.GitHubPagesStatus
import com.hugoer.mobile.data.model.GitHubRepoInfo
import com.hugoer.mobile.data.model.GitHubWorkflowInfo
import com.hugoer.mobile.data.model.RepoPermissions
import com.hugoer.mobile.data.model.WorkflowRunConclusion
import com.hugoer.mobile.data.model.WorkflowRunInfo
import com.hugoer.mobile.data.model.WorkflowRunStatus
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.time.OffsetDateTime

class GitHubApiException(val statusCode: Int, message: String) : IOException(message)

/** Everything the app needs from GitHub, implemented over REST API v3. */
class GitHubApiService(private val http: OkHttpClient) {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun getAuthenticatedUserLogin(token: String): String? {
        val response = send("GET", "user", token)
        return if (response.code in 200..299) response.body?.getString("login") else null
    }

    fun getRepoInfo(owner: String, repo: String, token: String): GitHubRepoInfo? {
        val response = send("GET", "repos/$owner/$repo", token)
        if (response.code !in 200..299) return null
        val json = response.body ?: return null
        val permissions = json.optJSONObject("permissions")
        return GitHubRepoInfo(
            owner = owner,
            name = repo,
            defaultBranch = json.optString("default_branch", "main"),
            private = json.optBoolean("private", false),
            htmlUrl = json.optString("html_url", "https://github.com/$owner/$repo"),
            description = json.optStringOrNull("description"),
            permissions = RepoPermissions(
                pull = permissions?.optBoolean("pull", false) ?: false,
                push = permissions?.optBoolean("push", false) ?: false,
                admin = permissions?.optBoolean("admin", false) ?: false
            )
        )
    }

    fun tryGetFile(owner: String, repo: String, path: String, branch: String, token: String): GitHubFileContent? {
        val response = send("GET", "repos/$owner/$repo/contents/${encode(path)}?ref=${encode(branch)}", token)
        if (response.code == 404) return null
        ensureSuccess(response)
        val json = response.body ?: return null
        if (json.optString("type") != "file") return null
        val sha = json.optString("sha")
        val base64 = json.optString("content", "").replace("\n", "").replace("\r", "")
        val text = try {
            String(android.util.Base64.decode(base64, android.util.Base64.DEFAULT), Charsets.UTF_8)
        } catch (_: IllegalArgumentException) {
            ""
        }
        return GitHubFileContent(path, sha, text)
    }

    fun listDirectory(owner: String, repo: String, path: String, branch: String, token: String): List<ContentItem> {
        val response = send("GET", "repos/$owner/$repo/contents/${encode(path)}?ref=${encode(branch)}", token)
        if (response.code == 404) return emptyList()
        ensureSuccess(response)
        val array = response.array ?: return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                add(ContentItem(
                    name = obj.optString("name"),
                    path = obj.optString("path"),
                    isDirectory = obj.optString("type") == "dir",
                    sha = obj.optStringOrNull("sha"),
                    size = obj.optLong("size", 0)
                ))
            }
        }.sortedWith(compareByDescending<ContentItem> { it.isDirectory }.thenBy { it.name.lowercase() })
    }

    fun putFile(
        owner: String,
        repo: String,
        path: String,
        textContent: String,
        commitMessage: String,
        sha: String?,
        branch: String,
        token: String
    ): GitHubFileContent {
        val body = JSONObject()
            .put("message", commitMessage)
            .put("content", android.util.Base64.encodeToString(textContent.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP))
            .put("branch", branch)
        if (!sha.isNullOrEmpty()) body.put("sha", sha)

        val response = send("PUT", "repos/$owner/$repo/contents/${encode(path)}", token, body)
        ensureSuccess(response)
        val json = response.body
        val newSha = json?.optJSONObject("content")?.optStringOrNull("sha") ?: sha.orEmpty()
        return GitHubFileContent(path, newSha, textContent)
    }

    fun deleteFile(owner: String, repo: String, path: String, sha: String, commitMessage: String, branch: String, token: String) {
        val body = JSONObject()
            .put("message", commitMessage)
            .put("sha", sha)
            .put("branch", branch)
        val response = send("DELETE", "repos/$owner/$repo/contents/${encode(path)}", token, body)
        ensureSuccess(response)
    }

    fun listWorkflows(owner: String, repo: String, token: String): List<GitHubWorkflowInfo> {
        val response = send("GET", "repos/$owner/$repo/actions/workflows", token)
        if (response.code !in 200..299) return emptyList()
        val array = response.body?.optJSONArray("workflows") ?: return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                add(GitHubWorkflowInfo(obj.optLong("id"), obj.optString("name"), obj.optString("path")))
            }
        }
    }

    fun listWorkflowRuns(owner: String, repo: String, token: String, take: Int = 10): List<WorkflowRunInfo> {
        val response = send("GET", "repos/$owner/$repo/actions/runs?per_page=$take", token)
        if (response.code !in 200..299) return emptyList()
        val array = response.body?.optJSONArray("workflow_runs") ?: return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                array.optJSONObject(i)?.let { add(mapRun(it)) }
            }
        }
    }

    fun dispatchWorkflow(owner: String, repo: String, workflowId: Long, branch: String, token: String) {
        val response = send("POST", "repos/$owner/$repo/actions/workflows/$workflowId/dispatches", token, JSONObject().put("ref", branch))
        ensureSuccess(response)
    }

    fun getPagesStatus(owner: String, repo: String, token: String): GitHubPagesStatus {
        val response = send("GET", "repos/$owner/$repo/pages", token)
        if (response.code == 404) return GitHubPagesStatus(message = "GitHub Pages 尚未啟用。")
        if (response.code !in 200..299) return GitHubPagesStatus(message = response.errorMessage())
        val json = response.body ?: return GitHubPagesStatus(message = "無法解析 Pages 回應。")
        val status = json.optStringOrNull("status")
        val source = json.optJSONObject("source")
        val message = when (status) {
            "built" -> "網站已成功建置並上線。"
            "building" -> "正在建置中…"
            "errored" -> "建置發生錯誤，請至 Actions 檢查日誌。"
            null -> "GitHub Pages 已啟用。"
            else -> "狀態：$status"
        }
        return GitHubPagesStatus(
            enabled = true,
            status = status,
            htmlUrl = json.optStringOrNull("html_url"),
            sourceBranch = source?.optStringOrNull("branch"),
            sourcePath = source?.optStringOrNull("path"),
            buildType = json.optStringOrNull("build_type"),
            message = message
        )
    }

    fun enablePagesFromActions(owner: String, repo: String, token: String): Pair<Boolean, String> {
        val existing = send("GET", "repos/$owner/$repo/pages", token)
        val method = if (existing.code in 200..299) "PUT" else "POST"
        val response = send(method, "repos/$owner/$repo/pages", token, JSONObject().put("build_type", "workflow"))
        return if (response.code in 200..299) {
            true to if (method == "PUT") "已將 GitHub Pages 建置來源更新為 GitHub Actions。" else "已啟用 GitHub Pages（GitHub Actions）。"
        } else {
            false to response.errorMessage()
        }
    }

    fun ensureDeployWorkflow(owner: String, repo: String, branch: String, token: String) {
        val path = ".github/workflows/hugo.yml"
        if (tryGetFile(owner, repo, path, branch, token) != null) return
        putFile(owner, repo, path, DEFAULT_HUGO_PAGES_WORKFLOW, "Add Hugo Pages workflow via Hugoer Mobile", null, branch, token)
    }

    private fun mapRun(obj: JSONObject): WorkflowRunInfo {
        val status = when (obj.optString("status")) {
            "queued" -> WorkflowRunStatus.QUEUED
            "in_progress" -> WorkflowRunStatus.IN_PROGRESS
            "completed" -> WorkflowRunStatus.COMPLETED
            else -> WorkflowRunStatus.UNKNOWN
        }
        val conclusion = when (obj.optStringOrNull("conclusion")) {
            "success" -> WorkflowRunConclusion.SUCCESS
            "failure" -> WorkflowRunConclusion.FAILURE
            "cancelled" -> WorkflowRunConclusion.CANCELLED
            "skipped" -> WorkflowRunConclusion.SKIPPED
            null -> WorkflowRunConclusion.NONE
            else -> WorkflowRunConclusion.OTHER
        }
        val createdAt = try { OffsetDateTime.parse(obj.optString("created_at")).toInstant().toEpochMilli() } catch (_: Exception) { 0L }
        return WorkflowRunInfo(
            id = obj.optLong("id"),
            name = obj.optString("name").ifBlank { obj.optString("display_title", "workflow") },
            status = status,
            conclusion = conclusion,
            headBranch = obj.optString("head_branch"),
            event = obj.optString("event"),
            htmlUrl = obj.optString("html_url"),
            createdAt = createdAt
        )
    }

    private data class Response(val code: Int, val body: JSONObject?, val array: JSONArray?, val rawError: String?) {
        fun errorMessage(): String {
            val apiMessage = body?.optStringOrNull("message")
            return apiMessage ?: "GitHub API 回應 $code"
        }
    }

    private fun send(method: String, endpoint: String, token: String, body: JSONObject? = null): Response {
        val builder = Request.Builder()
            .url("https://api.github.com/$endpoint")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("Authorization", "Bearer $token")
        if (body != null) builder.method(method, body.toString().toRequestBody(jsonMediaType))
        else builder.method(method, null)

        val response = http.newCall(builder.build()).execute()
        response.use {
            val text = it.body?.string().orEmpty()
            val json = try { if (text.trimStart().startsWith("[")) null else JSONObject(text) } catch (_: Exception) { null }
            val array = try { if (text.trimStart().startsWith("[")) JSONArray(text) else null } catch (_: Exception) { null }
            return Response(it.code, json, array, text)
        }
    }

    private fun ensureSuccess(response: Response) {
        if (response.code !in 200..299) throw GitHubApiException(response.code, response.errorMessage())
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    companion object {
        private val DEFAULT_HUGO_PAGES_WORKFLOW = """# Generated by Hugoer Mobile
name: Deploy Hugo site to Pages

on:
  push:
    branches: ["main", "master"]
  workflow_dispatch:

permissions:
  contents: read
  pages: write
  id-token: write

concurrency:
  group: "pages"
  cancel-in-progress: false

defaults:
  run:
    shell: bash

jobs:
  build:
    runs-on: ubuntu-latest
    env:
      HUGO_VERSION: 0.164.0
    steps:
      - name: Checkout
        uses: actions/checkout@v4
        with:
          submodules: recursive
          fetch-depth: 0

      - name: Setup Hugo
        uses: peaceiris/actions-hugo@v3
        with:
          hugo-version: ${'$'}{{ env.HUGO_VERSION }}
          extended: true

      - name: Setup Pages
        id: pages
        uses: actions/configure-pages@v5

      - name: Build with Hugo
        env:
          HUGO_ENVIRONMENT: production
        run: |
          hugo --gc --minify --baseURL "${'$'}{{ steps.pages.outputs.base_url }}/"

      - name: Upload artifact
        uses: actions/upload-pages-artifact@v3
        with:
          path: ./public

  deploy:
    environment:
      name: github-pages
      url: ${'$'}{{ steps.deployment.outputs.page_url }}
    runs-on: ubuntu-latest
    needs: build
    steps:
      - name: Deploy to GitHub Pages
        id: deployment
        uses: actions/deploy-pages@v4
"""
    }
}

private fun JSONObject.optStringOrNull(key: String): String? =
    if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotEmpty() }
