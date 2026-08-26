import Foundation

enum ConfigEditMode: String, CaseIterable, Identifiable {
    case basic = "基本"
    case paramsForm = "Params 表單"
    case rawToml = "原始 TOML"

    var id: String { rawValue }
}

@MainActor
final class ConfigViewModel: ObservableObject {
    @Published var isLoading = false
    @Published var isSaving = false
    @Published var errorMessage: String?
    @Published var successMessage: String?

    @Published var rawText: String = ""
    @Published var filePath: String = ConfigLocator.defaultPath
    @Published var fileSha: String?

    @Published var baseUrl: String = ""
    @Published var title: String = ""
    @Published var languageCode: String = ""
    @Published var theme: String = ""

    @Published var paramFields: [ParamFieldItem] = []

    private var site: SiteProfile?
    private var token: String?

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
            refreshDerivedState()
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func refreshDerivedState() {
        let basic = TomlParamsService.readBasicFields(rawText)
        baseUrl = basic.baseUrl
        title = basic.title
        languageCode = basic.languageCode
        theme = basic.theme
        paramFields = TomlParamsService.loadParamsForm(rawText)
    }

    func saveBasicFields() async {
        rawText = TomlParamsService.upsertSimpleRootKeys(rawText, rootKeys: [
            "baseURL": baseUrl,
            "title": title,
            "languageCode": languageCode,
            "theme": theme,
        ])
        await saveRaw()
    }

    func saveParamsForm() async {
        rawText = TomlParamsService.applyParamsToToml(rawText, fields: paramFields)
        await saveRaw()
        refreshDerivedState()
    }

    func saveRawTomlEditsDirectly() async {
        await saveRaw()
        refreshDerivedState()
    }

    private func saveRaw() async {
        guard let site, let token else { return }
        isSaving = true
        errorMessage = nil
        successMessage = nil
        defer { isSaving = false }

        do {
            let result = try await GitHubApiService.putFile(
                owner: site.owner, repo: site.repository, path: filePath, textContent: rawText,
                commitMessage: "Update \(filePath) via Hugoer Mobile", sha: fileSha, branch: site.defaultBranch, token: token
            )
            fileSha = result.sha
            successMessage = "已成功儲存並推送到 GitHub。"
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
