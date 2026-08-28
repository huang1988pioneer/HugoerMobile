import Foundation

/// A file or folder inside the repo's content directory (GitHub Contents API item).
struct ContentItem: Identifiable, Equatable {
    var name: String
    /// Full repo-relative path, e.g. "content/posts/hello-world.md".
    var path: String
    var isDirectory: Bool = false
    /// Git blob SHA — required by the Contents API when updating/deleting a file.
    var sha: String?
    var size: Int64 = 0

    var id: String { path }

    var isMarkdown: Bool {
        !isDirectory &&
            (name.lowercased().hasSuffix(".md") || name.lowercased().hasSuffix(".markdown"))
    }
}
