import Foundation

@MainActor
final class ContentEditorViewModel: ObservableObject {
    @Published var title: String = ""
    @Published var dateText: String = ""
    @Published var tags: String = ""
    @Published var categories: String = ""
    @Published var draft: Bool = false
    @Published var body: String = ""

    @Published var isLoading = false
    @Published var isSaving = false
    @Published var isDeleting = false
    @Published var errorMessage: String?
    @Published var successMessage: String?
    @Published var didDelete = false

    var path: String
    private var sha: String?
    private var site: SiteProfile?
    private var token: String?
    private(set) var isNewFile: Bool

    init(path: String, isNewFile: Bool, initialTitle: String = "") {
        self.path = path
        self.isNewFile = isNewFile
        if isNewFile {
            title = initialTitle
            draft = true
        }
    }

    func configure(site: SiteProfile, token: String) {
        self.site = site
        self.token = token
    }

    func load() async {
        guard let site, let token, !isNewFile else {
            if isNewFile {
                let formatter = ISO8601DateFormatter()
                dateText = formatter.string(from: Date())
                if body.isEmpty { body = "開始寫作吧。" }
            }
            return
        }
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }

        do {
            if let file = try await GitHubApiService.tryGetFile(owner: site.owner, repo: site.repository, path: path, branch: site.defaultBranch, token: token) {
                sha = file.sha
                let doc = FrontMatterService.parse(file.textContent)
                title = doc.title
                dateText = doc.date
                tags = doc.tags
                categories = doc.categories
                draft = doc.draft
                body = doc.body
            }
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func save() async {
        guard let site, let token else { return }
        isSaving = true
        errorMessage = nil
        successMessage = nil
        defer { isSaving = false }

        var doc = FrontMatterDocument()
        doc.title = title
        doc.date = dateText
        doc.tags = tags
        doc.categories = categories
        doc.draft = draft
        doc.body = body

        let text = FrontMatterService.write(doc)
        let commitMessage = isNewFile ? "Add \(path) via Hugoer Mobile" : "Update \(path) via Hugoer Mobile"

        do {
            let result = try await GitHubApiService.putFile(
                owner: site.owner, repo: site.repository, path: path, textContent: text,
                commitMessage: commitMessage, sha: sha, branch: site.defaultBranch, token: token
            )
            sha = result.sha
            isNewFile = false
            successMessage = "已成功儲存並推送到 GitHub。"
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func delete() async {
        guard let site, let token, let sha else { return }
        isDeleting = true
        errorMessage = nil
        defer { isDeleting = false }

        do {
            try await GitHubApiService.deleteFile(
                owner: site.owner, repo: site.repository, path: path, sha: sha,
                commitMessage: "Delete \(path) via Hugoer Mobile", branch: site.defaultBranch, token: token
            )
            didDelete = true
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func renderPreviewHtml(isDarkMode: Bool) -> String {
        MarkdownPreviewService.renderToHtmlDocument(body, isDarkMode: isDarkMode)
    }
}
