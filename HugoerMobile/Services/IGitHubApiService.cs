using HugoerMobile.Models;

namespace HugoerMobile.Services;

/// <summary>
/// Everything the mobile app needs from GitHub, implemented purely over the REST API.
/// This is the mobile replacement for the desktop app's <c>GitHubService</c> +
/// <c>ProcessRunner</c>-based `git`/`gh` calls.
/// </summary>
public interface IGitHubApiService
{
    Task<string?> GetAuthenticatedUserLoginAsync(string token, CancellationToken ct = default);

    Task<GitHubRepoInfo?> GetRepoInfoAsync(string owner, string repo, string token, CancellationToken ct = default);

    Task<GitHubFileContent?> TryGetFileAsync(
        string owner, string repo, string path, string branch, string token, CancellationToken ct = default);

    Task<IReadOnlyList<ContentItem>> ListDirectoryAsync(
        string owner, string repo, string path, string branch, string token, CancellationToken ct = default);

    Task<GitHubFileContent> PutFileAsync(
        string owner, string repo, string path, string textContent, string commitMessage,
        string? sha, string branch, string token, CancellationToken ct = default);

    Task DeleteFileAsync(
        string owner, string repo, string path, string sha, string commitMessage,
        string branch, string token, CancellationToken ct = default);

    Task<IReadOnlyList<GitHubWorkflowInfo>> ListWorkflowsAsync(
        string owner, string repo, string token, CancellationToken ct = default);

    Task<IReadOnlyList<WorkflowRunInfo>> ListWorkflowRunsAsync(
        string owner, string repo, string token, int take = 10, CancellationToken ct = default);

    Task DispatchWorkflowAsync(
        string owner, string repo, long workflowId, string branch, string token, CancellationToken ct = default);

    Task<GitHubPagesStatus> GetPagesStatusAsync(
        string owner, string repo, string token, CancellationToken ct = default);

    Task<(bool Success, string Message)> EnablePagesFromActionsAsync(
        string owner, string repo, string token, CancellationToken ct = default);

    Task EnsureDeployWorkflowAsync(
        string owner, string repo, string branch, string token, CancellationToken ct = default);
}
