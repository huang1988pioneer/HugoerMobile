import Foundation

/// Finds the Hugo site configuration file inside the repo, trying the same common
/// locations the desktop app's `PathHelper.FindConfigFile` checks.
enum ConfigLocator {
    static let candidatePaths = [
        "hugo.toml",
        "config.toml",
        "config/_default/hugo.toml",
        "config/_default/config.toml",
    ]

    static var defaultPath: String { candidatePaths[0] }

    /// Returns the first existing config file, or `nil` if none exist yet.
    static func find(site: SiteProfile, token: String) async throws -> GitHubFileContent? {
        for path in candidatePaths {
            if let file = try await GitHubApiService.tryGetFile(
                owner: site.owner, repo: site.repository, path: path,
                branch: site.defaultBranch, token: token
            ) {
                return file
            }
        }
        return nil
    }
}
