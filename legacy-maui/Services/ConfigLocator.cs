using HugoerMobile.Models;

namespace HugoerMobile.Services;

/// <summary>
/// Finds the Hugo site configuration file inside the repo, trying the same common
/// locations the desktop app's <c>PathHelper.FindConfigFile</c> checks.
/// </summary>
public sealed class ConfigLocator
{
    private static readonly string[] CandidatePaths =
    [
        "hugo.toml",
        "config.toml",
        "config/_default/hugo.toml",
        "config/_default/config.toml"
    ];

    private readonly IGitHubApiService _api;

    public ConfigLocator(IGitHubApiService api) => _api = api;

    /// <summary>Returns the first existing config file, or the default candidate path if none exist yet.</summary>
    public async Task<GitHubFileContent?> FindAsync(SiteProfile site, string token, CancellationToken ct = default)
    {
        foreach (var path in CandidatePaths)
        {
            var file = await _api.TryGetFileAsync(site.Owner, site.Repository, path, site.DefaultBranch, token, ct);
            if (file is not null)
                return file;
        }

        return null;
    }

    public string DefaultPath => CandidatePaths[0];
}
