namespace HugoerMobile.Models;

/// <summary>
/// A Hugo site the user has connected to this device. Mirrors what the desktop app
/// keeps as "an opened site folder", except here the site lives in a GitHub repository
/// and is only ever touched through the GitHub REST API.
/// </summary>
public sealed class SiteProfile
{
    public string Id { get; init; } = Guid.NewGuid().ToString("N");
    public required string Owner { get; init; }
    public required string Repository { get; init; }
    public string DisplayName { get; set; } = string.Empty;
    public string DefaultBranch { get; set; } = "main";
    public string PagesUrl { get; set; } = string.Empty;
    public bool IsUserOrOrganizationSite { get; set; }
    public DateTimeOffset AddedAtUtc { get; init; } = DateTimeOffset.UtcNow;

    public string FullName => $"{Owner}/{Repository}";

    /// <summary>Key used to look up this site's PAT in <see cref="Services.ISecureTokenStore"/>.</summary>
    public string TokenKey => $"gh_pat::{Id}";
}
