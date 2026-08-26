import Foundation

/// A curated theme entry shown in the 主題 (Themes) tab.
struct ThemeInfo: Identifiable, Equatable {
    var name: String
    var themeConfigValue: String
    var repoUrl: String
    var description: String = ""
    var recommendedForStackParams: Bool = false

    var id: String { themeConfigValue }
}
