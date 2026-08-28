using System.Net;
using System.Net.Http.Headers;
using System.Net.Http.Json;
using System.Text;
using System.Text.Json;
using System.Text.Json.Nodes;
using HugoerMobile.Models;

namespace HugoerMobile.Services;

public sealed class GitHubApiService : IGitHubApiService
{
    private readonly HttpClient _http;

    public GitHubApiService(HttpClient http) => _http = http;

    public async Task<string?> GetAuthenticatedUserLoginAsync(string token, CancellationToken ct = default)
    {
        using var response = await SendAsync(HttpMethod.Get, "user", token, ct: ct);
        if (!response.IsSuccessStatusCode) return null;

        var json = await response.Content.ReadFromJsonAsync<JsonObject>(cancellationToken: ct);
        return json?["login"]?.GetValue<string>();
    }

    public async Task<GitHubRepoInfo?> GetRepoInfoAsync(string owner, string repo, string token, CancellationToken ct = default)
    {
        using var response = await SendAsync(HttpMethod.Get, $"repos/{owner}/{repo}", token, ct: ct);
        if (!response.IsSuccessStatusCode) return null;

        var json = await response.Content.ReadFromJsonAsync<JsonObject>(cancellationToken: ct);
        if (json is null) return null;

        var permissions = json["permissions"] as JsonObject;
        return new GitHubRepoInfo
        {
            Owner = owner,
            Name = repo,
            DefaultBranch = json["default_branch"]?.GetValue<string>() ?? "main",
            Private = json["private"]?.GetValue<bool>() ?? false,
            HtmlUrl = json["html_url"]?.GetValue<string>() ?? $"https://github.com/{owner}/{repo}",
            Description = json["description"]?.GetValue<string>(),
            Permissions = new RepoPermissions
            {
                Pull = permissions?["pull"]?.GetValue<bool>() ?? false,
                Push = permissions?["push"]?.GetValue<bool>() ?? false,
                Admin = permissions?["admin"]?.GetValue<bool>() ?? false
            }
        };
    }

    public async Task<GitHubFileContent?> TryGetFileAsync(
        string owner, string repo, string path, string branch, string token, CancellationToken ct = default)
    {
        var url = $"repos/{owner}/{repo}/contents/{Uri.EscapeDataString(path)}?ref={Uri.EscapeDataString(branch)}";
        using var response = await SendAsync(HttpMethod.Get, url, token, ct: ct);
        if (response.StatusCode == HttpStatusCode.NotFound) return null;
        await EnsureSuccessOrThrowAsync(response, ct);

        var json = await response.Content.ReadFromJsonAsync<JsonObject>(cancellationToken: ct);
        if (json is null || json["type"]?.GetValue<string>() != "file") return null;

        var sha = json["sha"]!.GetValue<string>();
        var base64 = json["content"]?.GetValue<string>() ?? string.Empty;
        var text = DecodeBase64(base64);
        return new GitHubFileContent { Path = path, Sha = sha, TextContent = text };
    }

    public async Task<IReadOnlyList<ContentItem>> ListDirectoryAsync(
        string owner, string repo, string path, string branch, string token, CancellationToken ct = default)
    {
        var url = $"repos/{owner}/{repo}/contents/{Uri.EscapeDataString(path)}?ref={Uri.EscapeDataString(branch)}";
        using var response = await SendAsync(HttpMethod.Get, url, token, ct: ct);
        if (response.StatusCode == HttpStatusCode.NotFound) return [];
        await EnsureSuccessOrThrowAsync(response, ct);

        var json = await response.Content.ReadFromJsonAsync<JsonArray>(cancellationToken: ct);
        if (json is null) return [];

        var items = new List<ContentItem>();
        foreach (var node in json)
        {
            if (node is not JsonObject obj) continue;
            var type = obj["type"]?.GetValue<string>();
            items.Add(new ContentItem
            {
                Name = obj["name"]?.GetValue<string>() ?? string.Empty,
                Path = obj["path"]?.GetValue<string>() ?? string.Empty,
                IsDirectory = type == "dir",
                Sha = obj["sha"]?.GetValue<string>(),
                Size = obj["size"]?.GetValue<long>() ?? 0
            });
        }

        return items
            .OrderByDescending(i => i.IsDirectory)
            .ThenBy(i => i.Name, StringComparer.OrdinalIgnoreCase)
            .ToList();
    }

    public async Task<GitHubFileContent> PutFileAsync(
        string owner, string repo, string path, string textContent, string commitMessage,
        string? sha, string branch, string token, CancellationToken ct = default)
    {
        var url = $"repos/{owner}/{repo}/contents/{Uri.EscapeDataString(path)}";
        var body = new JsonObject
        {
            ["message"] = commitMessage,
            ["content"] = EncodeBase64(textContent),
            ["branch"] = branch
        };
        if (!string.IsNullOrEmpty(sha))
            body["sha"] = sha;

        using var response = await SendAsync(HttpMethod.Put, url, token, body, ct);
        await EnsureSuccessOrThrowAsync(response, ct);

        var json = await response.Content.ReadFromJsonAsync<JsonObject>(cancellationToken: ct);
        var newSha = json?["content"]?["sha"]?.GetValue<string>() ?? sha ?? string.Empty;
        return new GitHubFileContent { Path = path, Sha = newSha, TextContent = textContent };
    }

    public async Task DeleteFileAsync(
        string owner, string repo, string path, string sha, string commitMessage,
        string branch, string token, CancellationToken ct = default)
    {
        var url = $"repos/{owner}/{repo}/contents/{Uri.EscapeDataString(path)}";
        var body = new JsonObject
        {
            ["message"] = commitMessage,
            ["sha"] = sha,
            ["branch"] = branch
        };

        using var request = new HttpRequestMessage(HttpMethod.Delete, url)
        {
            Content = JsonContent.Create(body)
        };
        ApplyAuth(request, token);
        using var response = await _http.SendAsync(request, ct);
        await EnsureSuccessOrThrowAsync(response, ct);
    }

    public async Task<IReadOnlyList<GitHubWorkflowInfo>> ListWorkflowsAsync(
        string owner, string repo, string token, CancellationToken ct = default)
    {
        using var response = await SendAsync(HttpMethod.Get, $"repos/{owner}/{repo}/actions/workflows", token, ct: ct);
        if (!response.IsSuccessStatusCode) return [];

        var json = await response.Content.ReadFromJsonAsync<JsonObject>(cancellationToken: ct);
        var array = json?["workflows"] as JsonArray;
        if (array is null) return [];

        return array.OfType<JsonObject>().Select(obj => new GitHubWorkflowInfo
        {
            Id = obj["id"]?.GetValue<long>() ?? 0,
            Name = obj["name"]?.GetValue<string>() ?? string.Empty,
            Path = obj["path"]?.GetValue<string>() ?? string.Empty
        }).ToList();
    }

    public async Task<IReadOnlyList<WorkflowRunInfo>> ListWorkflowRunsAsync(
        string owner, string repo, string token, int take = 10, CancellationToken ct = default)
    {
        using var response = await SendAsync(
            HttpMethod.Get, $"repos/{owner}/{repo}/actions/runs?per_page={take}", token, ct: ct);
        if (!response.IsSuccessStatusCode) return [];

        var json = await response.Content.ReadFromJsonAsync<JsonObject>(cancellationToken: ct);
        var array = json?["workflow_runs"] as JsonArray;
        if (array is null) return [];

        return array.OfType<JsonObject>().Select(MapRun).ToList();
    }

    public async Task DispatchWorkflowAsync(
        string owner, string repo, long workflowId, string branch, string token, CancellationToken ct = default)
    {
        var url = $"repos/{owner}/{repo}/actions/workflows/{workflowId}/dispatches";
        var body = new JsonObject { ["ref"] = branch };
        using var response = await SendAsync(HttpMethod.Post, url, token, body, ct);
        await EnsureSuccessOrThrowAsync(response, ct);
    }

    public async Task<GitHubPagesStatus> GetPagesStatusAsync(
        string owner, string repo, string token, CancellationToken ct = default)
    {
        using var response = await SendAsync(HttpMethod.Get, $"repos/{owner}/{repo}/pages", token, ct: ct);
        if (response.StatusCode == HttpStatusCode.NotFound)
            return new GitHubPagesStatus { Enabled = false, Message = "GitHub Pages 尚未啟用。" };

        if (!response.IsSuccessStatusCode)
            return new GitHubPagesStatus { Enabled = false, Message = await ReadErrorAsync(response, ct) };

        var json = await response.Content.ReadFromJsonAsync<JsonObject>(cancellationToken: ct);
        if (json is null)
            return new GitHubPagesStatus { Enabled = false, Message = "無法解析 Pages 回應。" };

        var status = json["status"]?.GetValue<string>();
        var source = json["source"] as JsonObject;

        return new GitHubPagesStatus
        {
            Enabled = true,
            Status = status,
            HtmlUrl = json["html_url"]?.GetValue<string>(),
            SourceBranch = source?["branch"]?.GetValue<string>(),
            SourcePath = source?["path"]?.GetValue<string>(),
            BuildType = json["build_type"]?.GetValue<string>(),
            Message = status switch
            {
                "built" => "網站已成功建置並上線。",
                "building" => "正在建置中…",
                "errored" => "建置發生錯誤，請至 Actions 檢查日誌。",
                null => "GitHub Pages 已啟用。",
                _ => $"狀態：{status}"
            }
        };
    }

    public async Task<(bool Success, string Message)> EnablePagesFromActionsAsync(
        string owner, string repo, string token, CancellationToken ct = default)
    {
        using var existing = await SendAsync(HttpMethod.Get, $"repos/{owner}/{repo}/pages", token, ct: ct);
        var pagesExist = existing.IsSuccessStatusCode;

        var method = pagesExist ? HttpMethod.Put : HttpMethod.Post;
        var body = new JsonObject { ["build_type"] = "workflow" };
        using var response = await SendAsync(method, $"repos/{owner}/{repo}/pages", token, body, ct);

        if (response.IsSuccessStatusCode)
        {
            return (true, pagesExist
                ? "已將 GitHub Pages 建置來源更新為 GitHub Actions。"
                : "已啟用 GitHub Pages（GitHub Actions）。");
        }

        return (false, await ReadErrorAsync(response, ct));
    }

    public async Task EnsureDeployWorkflowAsync(
        string owner, string repo, string branch, string token, CancellationToken ct = default)
    {
        const string path = ".github/workflows/hugo.yml";
        var existing = await TryGetFileAsync(owner, repo, path, branch, token, ct);
        if (existing is not null) return;

        await PutFileAsync(owner, repo, path, DefaultHugoPagesWorkflow, "Add Hugo Pages workflow via Hugoer Mobile",
            sha: null, branch, token, ct);
    }

    private static WorkflowRunInfo MapRun(JsonObject obj)
    {
        var status = obj["status"]?.GetValue<string>() switch
        {
            "queued" => WorkflowRunStatus.Queued,
            "in_progress" => WorkflowRunStatus.InProgress,
            "completed" => WorkflowRunStatus.Completed,
            _ => WorkflowRunStatus.Unknown
        };

        var conclusion = obj["conclusion"]?.GetValue<string>() switch
        {
            "success" => WorkflowRunConclusion.Success,
            "failure" => WorkflowRunConclusion.Failure,
            "cancelled" => WorkflowRunConclusion.Cancelled,
            "skipped" => WorkflowRunConclusion.Skipped,
            null => WorkflowRunConclusion.None,
            _ => WorkflowRunConclusion.Other
        };

        return new WorkflowRunInfo
        {
            Id = obj["id"]?.GetValue<long>() ?? 0,
            Name = obj["name"]?.GetValue<string>() ?? obj["display_title"]?.GetValue<string>() ?? "workflow",
            Status = status,
            Conclusion = conclusion,
            HeadBranch = obj["head_branch"]?.GetValue<string>() ?? string.Empty,
            Event = obj["event"]?.GetValue<string>() ?? string.Empty,
            HtmlUrl = obj["html_url"]?.GetValue<string>() ?? string.Empty,
            CreatedAt = obj["created_at"]?.GetValue<DateTimeOffset>() ?? default
        };
    }

    private async Task<HttpResponseMessage> SendAsync(
        HttpMethod method, string url, string token, JsonObject? body = null, CancellationToken ct = default)
    {
        using var request = new HttpRequestMessage(method, url);
        ApplyAuth(request, token);
        if (body is not null)
            request.Content = JsonContent.Create(body);

        return await _http.SendAsync(request, ct);
    }

    private static void ApplyAuth(HttpRequestMessage request, string token)
    {
        request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", token);
    }

    private static async Task EnsureSuccessOrThrowAsync(HttpResponseMessage response, CancellationToken ct)
    {
        if (response.IsSuccessStatusCode) return;
        throw new GitHubApiException(response.StatusCode, await ReadErrorAsync(response, ct));
    }

    private static async Task<string> ReadErrorAsync(HttpResponseMessage response, CancellationToken ct)
    {
        try
        {
            var json = await response.Content.ReadFromJsonAsync<JsonObject>(cancellationToken: ct);
            var message = json?["message"]?.GetValue<string>();
            return string.IsNullOrWhiteSpace(message)
                ? $"GitHub API 回應 {(int)response.StatusCode} {response.ReasonPhrase}"
                : message;
        }
        catch (JsonException)
        {
            return $"GitHub API 回應 {(int)response.StatusCode} {response.ReasonPhrase}";
        }
    }

    private static string DecodeBase64(string base64)
    {
        var cleaned = base64.Replace("\n", string.Empty).Replace("\r", string.Empty);
        var bytes = Convert.FromBase64String(cleaned);
        return Encoding.UTF8.GetString(bytes);
    }

    private static string EncodeBase64(string text) => Convert.ToBase64String(Encoding.UTF8.GetBytes(text));

    private const string DefaultHugoPagesWorkflow = """
        # Generated by Hugoer Mobile
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
                  hugo-version: ${{ env.HUGO_VERSION }}
                  extended: true

              - name: Setup Pages
                id: pages
                uses: actions/configure-pages@v5

              - name: Build with Hugo
                env:
                  HUGO_ENVIRONMENT: production
                run: |
                  hugo --gc --minify --baseURL "${{ steps.pages.outputs.base_url }}/"

              - name: Upload artifact
                uses: actions/upload-pages-artifact@v3
                with:
                  path: ./public

          deploy:
            environment:
              name: github-pages
              url: ${{ steps.deployment.outputs.page_url }}
            runs-on: ubuntu-latest
            needs: build
            steps:
              - name: Deploy to GitHub Pages
                id: deployment
                uses: actions/deploy-pages@v4
        """;
}

public sealed class GitHubApiException(HttpStatusCode statusCode, string message) : Exception(message)
{
    public HttpStatusCode StatusCode { get; } = statusCode;
}
