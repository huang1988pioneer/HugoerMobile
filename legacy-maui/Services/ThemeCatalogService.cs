using HugoerMobile.Models;

namespace HugoerMobile.Services;

/// <summary>
/// Curated theme catalog shown in the 主題 tab. Actually *installing* a theme (adding a git
/// submodule, vendoring files, running `hugo mod`) is a heavier, occasional operation better
/// suited to the desktop app; the mobile app focuses on quickly switching the active `theme`
/// value for a site that already has the theme available.
/// </summary>
public sealed class ThemeCatalogService
{
    public IReadOnlyList<ThemeInfo> GetCatalog() =>
    [
        new ThemeInfo
        {
            Name = "Stack",
            ThemeConfigValue = "hugo-theme-stack",
            RepoUrl = "https://github.com/CaiJimmy/hugo-theme-stack",
            Description = "桌面版 Hugoer 預設一鍵安裝的卡片式部落格主題，支援側欄、留言、Mermaid、圖片處理等豐富 params。",
            RecommendedForStackParams = true
        },
        new ThemeInfo
        {
            Name = "PaperMod",
            ThemeConfigValue = "PaperMod",
            RepoUrl = "https://github.com/adityatelange/hugo-PaperMod",
            Description = "簡潔快速、SEO 友善，適合技術部落格與筆記型網站。"
        },
        new ThemeInfo
        {
            Name = "Congo",
            ThemeConfigValue = "congo",
            RepoUrl = "https://github.com/jpanther/congo",
            Description = "多功能個人網站主題，內建多種色彩配置與版面配置。"
        },
        new ThemeInfo
        {
            Name = "Blowfish",
            ThemeConfigValue = "blowfish",
            RepoUrl = "https://github.com/nunocoracao/blowfish",
            Description = "功能齊全的現代化主題，適合作品集與技術文章。"
        }
    ];
}
