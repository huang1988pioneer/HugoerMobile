namespace HugoerMobile.Models;

/// <summary>A curated theme entry shown in the 主題 (Themes) tab.</summary>
public sealed class ThemeInfo
{
    public required string Name { get; init; }
    public required string ThemeConfigValue { get; init; }
    public required string RepoUrl { get; init; }
    public string Description { get; init; } = string.Empty;
    public bool RecommendedForStackParams { get; init; }
}
