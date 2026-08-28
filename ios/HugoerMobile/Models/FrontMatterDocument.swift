import Foundation

/// Parsed YAML front matter + Markdown body of a Hugo content file.
struct FrontMatterDocument {
    var fields: [String: String] = [:]
    var body: String = ""

    private func fieldsKey(_ key: String) -> String? {
        fields.keys.first { $0.caseInsensitiveCompare(key) == .orderedSame }
    }

    var title: String {
        get { fieldsKey("title").flatMap { fields[$0] } ?? "" }
        set { fields["title"] = newValue }
    }

    var date: String {
        get { fieldsKey("date").flatMap { fields[$0] } ?? "" }
        set { fields["date"] = newValue }
    }

    var tags: String {
        get { fieldsKey("tags").flatMap { fields[$0] } ?? "" }
        set { fields["tags"] = newValue }
    }

    var categories: String {
        get { fieldsKey("categories").flatMap { fields[$0] } ?? "" }
        set { fields["categories"] = newValue }
    }

    var draft: Bool {
        get { (fieldsKey("draft").flatMap { fields[$0] } ?? "").caseInsensitiveCompare("true") == .orderedSame }
        set { fields["draft"] = newValue ? "true" : "false" }
    }
}
