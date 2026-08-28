import Foundation

@MainActor
final class ThemesViewModel: ObservableObject {
    @Published var isLoading = false
    @Published var isApplying = false
    @Published var errorMessage: String?
    @Published var successMessage: String?
    @Published var currentTheme: String = ""

    let catalog = ThemeCatalogService.getCatalog()

    private var site: SiteProfile?
    private var token: String?
    private var filePath: String = ConfigLocator.defaultPath
    private var fileSha: String?
    private var rawText: String = ""

    func load(site: SiteProfile, token: String) async {
        self.site = site
        self.token = token
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }

        do {
            if let file = try await ConfigLocator.find(site: site, token: token) {
                rawText = file.textContent
                filePath = file.path
                fileSha = file.sha
            } else {
                rawText = ""
                filePath = ConfigLocator.defaultPath
                fileSha = nil
            }
            currentTheme = TomlParamsService.readBasicFields(rawText).theme
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func apply(theme: ThemeInfo) async {
        guard let site, let token else { return }
        isApplying = true
        errorMessage = nil
        successMessage = nil
        defer { isApplying = false }

        do {
            let updatedText = TomlParamsService.upsertSimpleRootKeys(rawText, rootKeys: ["theme": theme.themeConfigValue])
            let result = try await GitHubApiService.putFile(
                owner: site.owner, repo: site.repository, path: filePath, textContent: updatedText,
                commitMessage: "Set theme to \(theme.themeConfigValue) via Hugoer Mobile", sha: fileSha, branch: site.defaultBranch, token: token
            )
            rawText = updatedText
            fileSha = result.sha
            currentTheme = theme.themeConfigValue
            successMessage = "已套用主題「\(theme.name)」，請確認 repo 內已有對應主題檔案。"
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
