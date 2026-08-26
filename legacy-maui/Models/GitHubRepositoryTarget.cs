using System.Text.RegularExpressions;

namespace HugoerMobile.Models;

/// <summary>
/// Parses user-entered repository references ("owner/repo" or a full GitHub URL).
/// Ported from the desktop app's <c>GitHubService.ParseRepositoryTarget</c>.
/// </summary>
public sealed partial class GitHubRepositoryTarget
{
    public bool IsValid { get; init; }
    public string Owner { get; init; } = string.Empty;
    public string Repository { get; init; } = string.Empty;
    public string PagesUrl { get; init; } = string.Empty;
    public bool IsUserOrOrganizationSite { get; init; }
    public string ErrorMessage { get; init; } = string.Empty;

    public static GitHubRepositoryTarget Parse(string? input)
    {
        var value = input?.Trim() ?? string.Empty;
        if (string.IsNullOrWhiteSpace(value))
            return Invalid("請輸入 GitHub repository，例如 owner/repo 或完整網址。");

        if (!value.Contains("://", StringComparison.Ordinal))
        {
            value = value.StartsWith("github.com/", StringComparison.OrdinalIgnoreCase)
                ? $"https://{value}"
                : $"https://github.com/{value.TrimStart('/')}";
        }

        if (!Uri.TryCreate(value, UriKind.Absolute, out var uri)
            || !uri.Scheme.Equals(Uri.UriSchemeHttps, StringComparison.OrdinalIgnoreCase)
            || !uri.Host.Equals("github.com", StringComparison.OrdinalIgnoreCase))
        {
            return Invalid("僅支援 owner/repo 或 https://github.com/owner/repository 格式。");
        }

        var segments = uri.AbsolutePath.Split('/', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries);
        if (segments.Length < 2)
            return Invalid("請提供完整的 owner 與 repository 名稱。");

        var owner = Uri.UnescapeDataString(segments[0]);
        var repository = Uri.UnescapeDataString(segments[1]);
        if (repository.EndsWith(".git", StringComparison.OrdinalIgnoreCase))
            repository = repository[..^4];

        if (!OwnerRegex().IsMatch(owner) || !RepoRegex().IsMatch(repository))
            return Invalid("GitHub owner 或 repository 名稱格式無效。");

        var userSite = repository.Equals($"{owner}.github.io", StringComparison.OrdinalIgnoreCase);
        var pagesUrl = userSite
            ? $"https://{owner.ToLowerInvariant()}.github.io/"
            : $"https://{owner.ToLowerInvariant()}.github.io/{repository}/";

        return new GitHubRepositoryTarget
        {
            IsValid = true,
            Owner = owner,
            Repository = repository,
            PagesUrl = pagesUrl,
            IsUserOrOrganizationSite = userSite
        };
    }

    private static GitHubRepositoryTarget Invalid(string message) => new()
    {
        IsValid = false,
        ErrorMessage = message
    };

    [GeneratedRegex(@"^[A-Za-z0-9](?:[A-Za-z0-9-]{0,38})$")]
    private static partial Regex OwnerRegex();

    [GeneratedRegex(@"^[A-Za-z0-9._-]{1,100}$")]
    private static partial Regex RepoRegex();
}
