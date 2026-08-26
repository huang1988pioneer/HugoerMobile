import Foundation

/// Handles the YAML front matter Hugo uses by default. Ported from the desktop app's
/// `FrontMatterService.cs` — pure string processing, no OS dependency.
enum FrontMatterService {
    private static let orderedKeys = ["title", "date", "slug", "categories", "tags", "image", "description", "draft"]

    static func parse(_ text: String) -> FrontMatterDocument {
        guard let match = matchFrontMatterBlock(text) else {
            return FrontMatterDocument(body: text)
        }

        var fields: [String: String] = [:]
        for line in match.frontMatter.components(separatedBy: "\n") {
            guard let colonIndex = line.firstIndex(of: ":") else { continue }
            let key = String(line[line.startIndex..<colonIndex]).trimmingCharacters(in: .whitespaces)
            if key.isEmpty { continue }
            if line.trimmingCharacters(in: .whitespaces).hasPrefix("#") { continue }
            let value = String(line[line.index(after: colonIndex)...]).trimmingCharacters(in: .whitespaces)
            fields[key] = unquote(value)
        }

        var body = String(text[match.bodyStart...])
        while let first = body.first, first == "\r" || first == "\n" {
            body.removeFirst()
        }

        return FrontMatterDocument(fields: fields, body: body)
    }

    static func write(_ document: FrontMatterDocument) -> String {
        var emitted = Set<String>()
        var output = "---\n"

        for key in orderedKeys {
            appendField(&output, fields: document.fields, key: key, emitted: &emitted)
        }

        for key in document.fields.keys.sorted() {
            appendField(&output, fields: document.fields, key: key, emitted: &emitted)
        }

        output += "---\n\n"
        var body = document.body
        while let first = body.first, first == "\r" || first == "\n" {
            body.removeFirst()
        }
        output += body
        return output
    }

    private static func appendField(_ output: inout String, fields: [String: String], key: String, emitted: inout Set<String>) {
        let actualKey = fields.keys.first { $0.caseInsensitiveCompare(key) == .orderedSame } ?? key
        guard let value = fields[actualKey], !value.trimmingCharacters(in: .whitespaces).isEmpty else { return }
        let lowerKey = key.lowercased()
        if emitted.contains(lowerKey) { return }
        emitted.insert(lowerKey)

        if lowerKey == "draft" {
            output += "draft: \(value.lowercased())\n"
            return
        }

        if lowerKey == "categories" || lowerKey == "tags" {
            let values = value
                .split(separator: ",")
                .map { $0.trimmingCharacters(in: .whitespaces) }
                .filter { !$0.isEmpty }
                .map(quote)
            output += "\(lowerKey): [\(values.joined(separator: ", "))]\n"
            return
        }

        if lowerKey == "date", isParsableDate(value) {
            output += "date: \(value)\n"
            return
        }

        output += "\(lowerKey): \(quote(value))\n"
    }

    private static func isParsableDate(_ value: String) -> Bool {
        let formatters: [DateFormatter] = [isoDateTimeFormatter, isoDateFormatter]
        return formatters.contains { $0.date(from: value) != nil }
    }

    private static let isoDateTimeFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd'T'HH:mm:ssZZZZZ"
        f.locale = Locale(identifier: "en_US_POSIX")
        return f
    }()

    private static let isoDateFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd"
        f.locale = Locale(identifier: "en_US_POSIX")
        return f
    }()

    private static func quote(_ value: String) -> String {
        if value.hasPrefix("[") && value.hasSuffix("]") { return value }
        let escaped = value.replacingOccurrences(of: "\\", with: "\\\\").replacingOccurrences(of: "\"", with: "\\\"")
        return "\"\(escaped)\""
    }

    private static func unquote(_ raw: String) -> String {
        let value = raw.trimmingCharacters(in: .whitespaces)
        if value.hasPrefix("[") && value.hasSuffix("]") {
            let inner = String(value.dropFirst().dropLast())
            let items = inner
                .split(separator: ",")
                .map { $0.trimmingCharacters(in: .whitespaces) }
                .filter { !$0.isEmpty }
                .map(unquote)
            return items.joined(separator: ", ")
        }
        if value.count >= 2 {
            if (value.hasPrefix("\"") && value.hasSuffix("\"")) || (value.hasPrefix("'") && value.hasSuffix("'")) {
                let inner = String(value.dropFirst().dropLast())
                return inner.replacingOccurrences(of: "\\\"", with: "\"").replacingOccurrences(of: "\\\\", with: "\\")
            }
        }
        return value
    }

    private static func matchFrontMatterBlock(_ text: String) -> (frontMatter: String, bodyStart: String.Index)? {
        guard text.hasPrefix("---") else { return nil }

        // Split into lines while keeping track of index offsets so we can slice the
        // original string precisely once we find the closing "---" line.
        var lineStartIndex = text.startIndex
        var isFirstLine = true
        var frontMatterLines: [String] = []

        while lineStartIndex < text.endIndex {
            let newlineIndex = text[lineStartIndex...].firstIndex(of: "\n") ?? text.endIndex
            let line = String(text[lineStartIndex..<newlineIndex])
            let bodyStart = newlineIndex < text.endIndex ? text.index(after: newlineIndex) : text.endIndex

            if isFirstLine {
                isFirstLine = false
                lineStartIndex = bodyStart
                continue
            }

            if line.trimmingCharacters(in: .whitespaces) == "---" {
                return (frontMatterLines.joined(separator: "\n"), bodyStart)
            }

            frontMatterLines.append(line)
            lineStartIndex = bodyStart
        }

        return nil
    }
}
