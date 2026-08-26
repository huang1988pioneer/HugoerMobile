package com.hugoer.mobile.data.service

import com.hugoer.mobile.data.model.ThemeInfo

/**
 * Curated theme catalog shown in the 主題 tab. Actually *installing* a theme (adding a git
 * submodule, vendoring files, running `hugo mod`) stays a desktop job; the mobile app focuses
 * on quickly switching the active `theme` value for a site that already has the theme available.
 */
class ThemeCatalogService {

    fun getCatalog(): List<ThemeInfo> = listOf(
        ThemeInfo(
            name = "Stack",
            themeConfigValue = "hugo-theme-stack",
            repoUrl = "https://github.com/CaiJimmy/hugo-theme-stack",
            description = "桌面版 Hugoer 預設一鍵安裝的卡片式部落格主題，支援側欄、留言、Mermaid、圖片處理等豐富 params。",
            recommendedForStackParams = true
        ),
        ThemeInfo(
            name = "PaperMod",
            themeConfigValue = "PaperMod",
            repoUrl = "https://github.com/adityatelange/hugo-PaperMod",
            description = "簡潔快速、SEO 友善，適合技術部落格與筆記型網站。"
        ),
        ThemeInfo(
            name = "Congo",
            themeConfigValue = "congo",
            repoUrl = "https://github.com/jpanther/congo",
            description = "多功能個人網站主題，內建多種色彩配置與版面配置。"
        ),
        ThemeInfo(
            name = "Blowfish",
            themeConfigValue = "blowfish",
            repoUrl = "https://github.com/nunocoracao/blowfish",
            description = "功能齊全的現代化主題，適合作品集與技術文章。"
        )
    )
}
