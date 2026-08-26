import Foundation

/// A tiny, dependency-free TOML reader/writer sufficient for hugo.toml's actual usage:
/// top-level `key = value` pairs, `[section]` / `[section.sub]` tables, string/bool/
/// integer/float/array-of-strings values. It preserves unknown keys and does not attempt
/// to be a fully spec-compliant TOML parser (no inline tables, no multi-line strings,
/// no array-of-tables, no dotted keys on a single line).
enum TomlValue: Equatable {
    case string(String)
    case bool(Bool)
    case int(Int64)
    case double(Double)
    case array([String]) // array-of-strings only, per scope
    indirect case table(TomlTable)

    var asString: String? {
        if case let .string(s) = self { return s }
        return nil
    }

    var asBool: Bool? {
        if case let .bool(b) = self { return b }
        return nil
    }

    var asTable: TomlTable? {
        if case let .table(t) = self { return t }
        return nil
    }
}

/// Ordered key-value table, preserving insertion order so round-tripped files stay stable.
final class TomlTable: Equatable {
    private(set) var keys: [String] = []
    private var storage: [String: TomlValue] = [:]

    init() {}

    static func == (lhs: TomlTable, rhs: TomlTable) -> Bool {
        lhs.keys == rhs.keys && lhs.storage == rhs.storage
    }

    subscript(key: String) -> TomlValue? {
        get { storage[key] }
        set {
            if let newValue {
                if storage[key] == nil { keys.append(key) }
                storage[key] = newValue
            } else {
                remove(key)
            }
        }
    }

    func remove(_ key: String) {
        if storage.removeValue(forKey: key) != nil {
            keys.removeAll { $0 == key }
        }
    }

    var count: Int { keys.count }

    func table(_ key: String) -> TomlTable? {
        self[key]?.asTable
    }

    func orCreateTable(_ key: String) -> TomlTable {
        if let existing = table(key) { return existing }
        let t = TomlTable()
        self[key] = .table(t)
        return t
    }
}

enum TomlLite {
    // MARK: - Parsing

    static func parse(_ text: String) -> TomlTable {
        let root = TomlTable()
        var current = root
        // Map from dotted section path -> table, so [a.b] can find or create nested tables.
        var sectionPathToTable: [String: TomlTable] = ["": root]

        for rawLine in text.components(separatedBy: "\n") {
            let line = stripComment(rawLine).trimmingCharacters(in: .whitespaces)
            if line.isEmpty { continue }

            if line.hasPrefix("[") && line.hasSuffix("]") {
                let inner = String(line.dropFirst().dropLast()).trimmingCharacters(in: .whitespaces)
                if inner.hasPrefix("[") { continue } // array-of-tables, unsupported: skip
                let path = inner
                let parts = splitDotted(path)
                var parentPath = ""
                var parent = root
                var accumulated = ""
                for (idx, part) in parts.enumerated() {
                    accumulated = accumulated.isEmpty ? part : "\(accumulated).\(part)"
                    if let existing = sectionPathToTable[accumulated] {
                        parent = existing
                    } else {
                        let newTable = parent.orCreateTable(part)
                        sectionPathToTable[accumulated] = newTable
                        parent = newTable
                    }
                    if idx == parts.count - 1 {
                        current = parent
                    }
                    parentPath = accumulated
                }
                _ = parentPath
                continue
            }

            guard let eq = line.firstIndex(of: "=") else { continue }
            let key = String(line[line.startIndex..<eq]).trimmingCharacters(in: .whitespaces)
            let rawValue = String(line[line.index(after: eq)...]).trimmingCharacters(in: .whitespaces)
            if key.isEmpty { continue }
            current[unquoteKey(key)] = parseValue(rawValue)
        }

        return root
    }

    private static func stripComment(_ line: String) -> String {
        var inString = false
        var quoteChar: Character = "\""
        var result = ""
        for ch in line {
            if inString {
                result.append(ch)
                if ch == quoteChar { inString = false }
            } else if ch == "\"" || ch == "'" {
                inString = true
                quoteChar = ch
                result.append(ch)
            } else if ch == "#" {
                break
            } else {
                result.append(ch)
            }
        }
        return result
    }

    private static func splitDotted(_ s: String) -> [String] {
        s.split(separator: ".").map { $0.trimmingCharacters(in: .whitespaces) }
    }

    private static func unquoteKey(_ key: String) -> String {
        var k = key
        if k.count >= 2, (k.hasPrefix("\"") && k.hasSuffix("\"")) || (k.hasPrefix("'") && k.hasSuffix("'")) {
            k = String(k.dropFirst().dropLast())
        }
        return k
    }

    private static func parseValue(_ raw: String) -> TomlValue {
        let value = raw.trimmingCharacters(in: .whitespaces)
        if value.isEmpty { return .string("") }

        if value == "true" { return .bool(true) }
        if value == "false" { return .bool(false) }

        if value.hasPrefix("[") && value.hasSuffix("]") {
            let inner = String(value.dropFirst().dropLast())
            let items = splitArrayItems(inner).map { unquoteString($0) }
            return .array(items)
        }

        if (value.hasPrefix("\"") && value.hasSuffix("\"") && value.count >= 2)
            || (value.hasPrefix("'") && value.hasSuffix("'") && value.count >= 2) {
            return .string(unquoteString(value))
        }

        if let intVal = Int64(value) {
            return .int(intVal)
        }
        if let doubleVal = Double(value) {
            return .double(doubleVal)
        }

        // Bare word (e.g. a date or unquoted string) — keep as string.
        return .string(value)
    }

    private static func splitArrayItems(_ inner: String) -> [String] {
        var items: [String] = []
        var current = ""
        var inString = false
        var quoteChar: Character = "\""
        var depth = 0
        for ch in inner {
            if inString {
                current.append(ch)
                if ch == quoteChar { inString = false }
                continue
            }
            switch ch {
            case "\"", "'":
                inString = true
                quoteChar = ch
                current.append(ch)
            case "[":
                depth += 1
                current.append(ch)
            case "]":
                depth -= 1
                current.append(ch)
            case ",":
                if depth == 0 {
                    let trimmed = current.trimmingCharacters(in: .whitespaces)
                    if !trimmed.isEmpty { items.append(trimmed) }
                    current = ""
                } else {
                    current.append(ch)
                }
            default:
                current.append(ch)
            }
        }
        let trimmed = current.trimmingCharacters(in: .whitespaces)
        if !trimmed.isEmpty { items.append(trimmed) }
        return items
    }

    private static func unquoteString(_ value: String) -> String {
        var v = value.trimmingCharacters(in: .whitespaces)
        guard v.count >= 2 else { return v }
        if (v.hasPrefix("\"") && v.hasSuffix("\"")) || (v.hasPrefix("'") && v.hasSuffix("'")) {
            v = String(v.dropFirst().dropLast())
            v = v.replacingOccurrences(of: "\\\"", with: "\"")
            v = v.replacingOccurrences(of: "\\\\", with: "\\")
        }
        return v
    }

    // MARK: - Serialization

    static func serialize(_ root: TomlTable) -> String {
        var output = ""
        // Top-level scalar keys first.
        for key in root.keys {
            guard let value = root[key], value.asTable == nil else { continue }
            output += "\(quoteKeyIfNeeded(key)) = \(formatValue(value))\n"
        }
        output += serializeTables(root, prefix: [])
        return output
    }

    private static func serializeTables(_ table: TomlTable, prefix: [String]) -> String {
        var output = ""
        for key in table.keys {
            guard let value = table[key], let nested = value.asTable else { continue }
            let path = prefix + [key]
            output += "\n[\(path.map(quoteKeyIfNeeded).joined(separator: "."))]\n"
            for nestedKey in nested.keys {
                guard let nestedValue = nested[nestedKey], nestedValue.asTable == nil else { continue }
                output += "\(quoteKeyIfNeeded(nestedKey)) = \(formatValue(nestedValue))\n"
            }
            output += serializeTables(nested, prefix: path)
        }
        return output
    }

    private static func quoteKeyIfNeeded(_ key: String) -> String {
        let isBare = key.allSatisfy { $0.isLetter || $0.isNumber || $0 == "_" || $0 == "-" } && !key.isEmpty
        return isBare ? key : "\"\(key)\""
    }

    private static func formatValue(_ value: TomlValue) -> String {
        switch value {
        case let .string(s):
            return "\"\(escapeString(s))\""
        case let .bool(b):
            return b ? "true" : "false"
        case let .int(i):
            return String(i)
        case let .double(d):
            return String(d)
        case let .array(items):
            return "[" + items.map { "\"\(escapeString($0))\"" }.joined(separator: ", ") + "]"
        case .table:
            return ""
        }
    }

    private static func escapeString(_ s: String) -> String {
        s.replacingOccurrences(of: "\\", with: "\\\\").replacingOccurrences(of: "\"", with: "\\\"")
    }
}
