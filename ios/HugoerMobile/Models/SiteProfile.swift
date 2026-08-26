import Foundation

/// A Hugo site the user has connected to this device. The site lives in a GitHub
/// repository and is only ever touched through the GitHub REST API.
struct SiteProfile: Codable, Identifiable, Equatable {
    var id: String = UUID().uuidString
    var owner: String
    var repository: String
    var displayName: String = ""
    var defaultBranch: String = "main"
    var pagesUrl: String = ""
    var isUserOrOrganizationSite: Bool = false
    var addedAtUtc: Date = Date()

    var fullName: String { "\(owner)/\(repository)" }

    /// Key used to look up this site's PAT in the Keychain.
    var tokenKey: String { "gh_pat::\(id)" }
}
