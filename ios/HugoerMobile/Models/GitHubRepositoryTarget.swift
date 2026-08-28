import Foundation

/// Parses user-entered repository references ("owner/repo" or a full GitHub URL).
struct GitHubRepositoryTarget {
    var isValid: Bool = false
    var owner: String = ""
    var repository: String = ""
    var pagesUrl: String = ""
    var isUserOrOrganizationSite: Bool = false
    var errorMessage: String = ""

    private static let ownerRegex = try! NSRegularExpression(pattern: "^[A-Za-z0-9](?:[A-Za-z0-9-]{0,38})$")
    private static let repoRegex = try! NSRegularExpression(pattern: "^[A-Za-z0-9._-]{1,100}$")

    static func parse(_ input: String?) -> GitHubRepositoryTarget {
        var value = (input ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
        if value.isEmpty {
            return invalid("請輸入 GitHub repository，例如 owner/repo 或完整網址。")
        }

        if !value.contains("://") {
            if value.lowercased().hasPrefix("github.com/") {
                value = "https://\(value)"
            } else {
                let trimmed = value.hasPrefix("/") ? String(value.dropFirst()) : value
                value = "https://github.com/\(trimmed)"
            }
        }

        guard let url = URL(string: value),
              url.scheme?.lowercased() == "https",
              url.host?.lowercased() == "github.com" else {
            return invalid("僅支援 owner/repo 或 https://github.com/owner/repository 格式。")
        }

        let segments = url.path.split(separator: "/").map(String.init)
        guard segments.count >= 2 else {
            return invalid("請提供完整的 owner 與 repository 名稱。")
        }

        let owner = segments[0].removingPercentEncoding ?? segments[0]
        var repository = segments[1].removingPercentEncoding ?? segments[1]
        if repository.lowercased().hasSuffix(".git") {
            repository = String(repository.dropLast(4))
        }

        guard matches(ownerRegex, owner), matches(repoRegex, repository) else {
            return invalid("GitHub owner 或 repository 名稱格式無效。")
        }

        let userSite = repository.lowercased() == "\(owner.lowercased()).github.io"
        let pagesUrl = userSite
            ? "https://\(owner.lowercased()).github.io/"
            : "https://\(owner.lowercased()).github.io/\(repository)/"

        return GitHubRepositoryTarget(
            isValid: true,
            owner: owner,
            repository: repository,
            pagesUrl: pagesUrl,
            isUserOrOrganizationSite: userSite
        )
    }

    private static func matches(_ regex: NSRegularExpression, _ value: String) -> Bool {
        let range = NSRange(value.startIndex..., in: value)
        return regex.firstMatch(in: value, range: range) != nil
    }

    private static func invalid(_ message: String) -> GitHubRepositoryTarget {
        GitHubRepositoryTarget(isValid: false, errorMessage: message)
    }
}
