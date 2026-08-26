import Foundation

@MainActor
final class ContentViewModel: ObservableObject {
    @Published var currentPath: String = "content"
    @Published var items: [ContentItem] = []
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var searchText: String = ""

    private(set) var site: SiteProfile?
    private(set) var token: String?

    var filteredItems: [ContentItem] {
        let visibleItems = items.filter { $0.isDirectory || $0.isMarkdown }
        guard !searchText.trimmingCharacters(in: .whitespaces).isEmpty else { return visibleItems }
        return visibleItems.filter { $0.name.localizedCaseInsensitiveContains(searchText) }
    }

    var canGoUp: Bool {
        currentPath != "content" && currentPath.contains("/")
    }

    var breadcrumb: String {
        currentPath
    }

    func configure(site: SiteProfile, token: String) {
        self.site = site
        self.token = token
    }

    func load() async {
        guard let site, let token else { return }
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }

        do {
            items = try await GitHubApiService.listDirectory(owner: site.owner, repo: site.repository, path: currentPath, branch: site.defaultBranch, token: token)
                .filter { $0.isDirectory || $0.isMarkdown }
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func enterDirectory(_ item: ContentItem) async {
        guard item.isDirectory else { return }
        currentPath = item.path
        searchText = ""
        await load()
    }

    func goUp() async {
        guard canGoUp else { return }
        let parts = currentPath.split(separator: "/").map(String.init)
        currentPath = parts.dropLast().joined(separator: "/")
        if currentPath.isEmpty { currentPath = "content" }
        searchText = ""
        await load()
    }

    func delete(_ item: ContentItem) async {
        guard let site, let token, let sha = item.sha else { return }
        do {
            try await GitHubApiService.deleteFile(
                owner: site.owner, repo: site.repository, path: item.path, sha: sha,
                commitMessage: "Delete \(item.path) via Hugoer Mobile", branch: site.defaultBranch, token: token
            )
            items.removeAll { $0.id == item.id }
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    /// Builds the target path for a brand-new post given a title, honoring whether we're
    /// already browsing inside `content/`.
    func newPostPath(title: String) -> String {
        let slug = Self.slugify(title)
        if currentPath == "content" {
            return "content/posts/\(slug).md"
        }
        if currentPath.hasPrefix("content/") {
            return "\(currentPath)/\(slug).md"
        }
        return "content/posts/\(slug).md"
    }

    static func slugify(_ title: String) -> String {
        let lowered = title.lowercased().trimmingCharacters(in: .whitespacesAndNewlines)
        var result = ""
        var lastWasDash = false
        for scalar in lowered.unicodeScalars {
            if CharacterSet.alphanumerics.contains(scalar) {
                result.unicodeScalars.append(scalar)
                lastWasDash = false
            } else if !lastWasDash {
                result.append("-")
                lastWasDash = true
            }
        }
        let trimmed = result.trimmingCharacters(in: CharacterSet(charactersIn: "-"))
        return trimmed.isEmpty ? "untitled-\(Int(Date().timeIntervalSince1970))" : trimmed
    }
}
