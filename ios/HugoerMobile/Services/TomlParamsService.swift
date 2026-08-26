import Foundation

/// Structured "Params 表單" editor for hugo.toml's [params] table, ported from the
/// desktop app's `TomlParamsService.cs`. Built on top of `TomlLite`.
enum TomlParamsService {
    /// Common Hugo + Stack theme params with labels.
    static let knownParams: [(key: String, label: String, description: String, kind: ParamFieldKind)] = [
        ("description", "網站描述", "SEO / 首頁摘要", .string),
        ("mainSections", "主要區塊", "例如 post（逗號分隔）", .array),
        ("colorScheme.default", "色彩模式", "auto / light / dark", .string),
        ("colorScheme.toggle", "允許切換色彩模式", "顯示明暗模式切換", .bool),
        ("favicon", "網站圖示", "Stack v4 assets 路徑，例如 img/favicon.png", .string),
        ("footer.since", "Footer 起始年", "版權起始年份", .number),
        ("footer.customText", "Footer 文字", "自訂頁尾文字", .string),
        ("sidebar.subtitle", "側欄副標", "個人簡介副標", .string),
        ("sidebar.avatar", "側欄頭像", "Stack v4 單一 assets 路徑；空白代表停用", .string),
        ("article.toc", "文章目錄", "Stack v4 顯示目錄", .bool),
        ("article.readingTime", "閱讀時間", "Stack v4 顯示預估閱讀時間", .bool),
        ("article.list.showTags", "列表顯示標籤", "Stack v4 文章列表顯示 tags", .bool),
        ("comments.enabled", "啟用留言", "Stack v4 留言總開關", .bool),
        ("comments.provider", "留言服務", "giscus / utterances / waline 等", .string),
    ]

    private struct FlatValue {
        var kind: ParamFieldKind
        var value: String
        var bool: Bool
    }

    static func loadParamsForm(_ tomlText: String) -> [ParamFieldItem] {
        var result: [ParamFieldItem] = []
        let root = TomlLite.parse(tomlText)
        var flat: [String: FlatValue] = [:] // keyed by lowercased path
        var displayPaths: [String: String] = [:] // lowercased path -> original-case path

        if let paramsTable = root.table("params") {
            flattenTable(paramsTable, prefix: "", into: &flat, displayPaths: &displayPaths)
        }

        var used = Set<String>()

        for entry in knownParams {
            let lowerKey = entry.key.lowercased()
            used.insert(lowerKey)
            let existing = flat[lowerKey]
            result.append(ParamFieldItem(
                key: entry.key.contains(".") ? String(entry.key.split(separator: ".").last!) : entry.key,
                path: entry.key,
                displayName: entry.label,
                description: entry.description,
                kind: entry.kind,
                stringValue: existing?.value ?? "",
                boolValue: existing?.bool ?? false,
                isKnown: true,
                isConfigured: existing != nil
            ))
        }

        for (lowerPath, val) in flat.sorted(by: { $0.key < $1.key }) {
            if used.contains(lowerPath) { continue }
            let path = displayPaths[lowerPath] ?? lowerPath
            result.append(ParamFieldItem(
                key: path.contains(".") ? String(path.split(separator: ".").last!) : path,
                path: path,
                displayName: path,
                description: "自訂參數",
                kind: val.kind,
                stringValue: val.value,
                boolValue: val.bool,
                isKnown: false,
                isConfigured: true
            ))
        }

        return result
    }

    static func applyParamsToToml(_ tomlText: String, fields: [ParamFieldItem]) -> String {
        let root = TomlLite.parse(tomlText)
        let paramsTable = root.orCreateTable("params")

        for field in fields {
            if field.path.trimmingCharacters(in: .whitespaces).isEmpty { continue }
            if !field.isEditable { continue }

            if !field.isConfigured {
                removePath(paramsTable, path: field.path)
                continue
            }

            if !field.isKnown && field.kind != .bool && field.stringValue.trimmingCharacters(in: .whitespaces).isEmpty {
                continue
            }

            setPathValue(paramsTable, path: field.path, field: field)
        }

        return TomlLite.serialize(root)
    }

    static func upsertSimpleRootKeys(_ tomlText: String, rootKeys: [String: String]) -> String {
        let root = TomlLite.parse(tomlText)
        for (k, v) in rootKeys {
            if k.trimmingCharacters(in: .whitespaces).isEmpty { continue }
            root[k] = .string(v)
        }
        return TomlLite.serialize(root)
    }

    static func readBasicFields(_ tomlText: String) -> (baseUrl: String, title: String, languageCode: String, theme: String) {
        let root = TomlLite.parse(tomlText)
        return (
            getString(root, "baseURL"),
            getString(root, "title"),
            getString(root, "languageCode"),
            getString(root, "theme")
        )
    }

    private static func getString(_ root: TomlTable, _ key: String) -> String {
        guard let value = root[key] else { return "" }
        switch value {
        case let .string(s): return s
        case let .bool(b): return b ? "true" : "false"
        case let .int(i): return String(i)
        case let .double(d): return String(d)
        default: return ""
        }
    }

    private static func flattenTable(
        _ table: TomlTable,
        prefix: String,
        into flat: inout [String: FlatValue],
        displayPaths: inout [String: String]
    ) {
        for key in table.keys {
            guard let value = table[key] else { continue }
            let path = prefix.isEmpty ? key : "\(prefix).\(key)"
            let lowerPath = path.lowercased()
            displayPaths[lowerPath] = path
            switch value {
            case let .string(s):
                flat[lowerPath] = FlatValue(kind: .string, value: s, bool: false)
            case let .bool(b):
                flat[lowerPath] = FlatValue(kind: .bool, value: b ? "true" : "false", bool: b)
            case let .int(i):
                flat[lowerPath] = FlatValue(kind: .number, value: String(i), bool: false)
            case let .double(d):
                flat[lowerPath] = FlatValue(kind: .number, value: String(d), bool: false)
            case let .array(items):
                flat[lowerPath] = FlatValue(kind: .array, value: items.joined(separator: ", "), bool: false)
            case let .table(nested):
                flattenTable(nested, prefix: path, into: &flat, displayPaths: &displayPaths)
            }
        }
    }

    private static func setPathValue(_ rootParams: TomlTable, path: String, field: ParamFieldItem) {
        let parts = path.split(separator: ".").map(String.init).filter { !$0.isEmpty }
        guard !parts.isEmpty else { return }

        var current = rootParams
        for i in 0..<(parts.count - 1) {
            current = current.orCreateTable(parts[i])
        }

        let leaf = parts[parts.count - 1]
        switch field.kind {
        case .bool:
            current[leaf] = .bool(field.boolValue)
        case .number:
            if let l = Int64(field.stringValue) {
                current[leaf] = .int(l)
            } else if let d = Double(field.stringValue) {
                current[leaf] = .double(d)
            } else {
                current[leaf] = .string(field.stringValue)
            }
        case .array:
            let items = field.stringValue
                .split(separator: ",")
                .map { $0.trimmingCharacters(in: .whitespaces) }
                .filter { !$0.isEmpty }
            current[leaf] = .array(items)
        default:
            current[leaf] = .string(field.stringValue)
        }
    }

    private static func removePath(_ root: TomlTable, path: String) {
        let parts = path.split(separator: ".").map(String.init).filter { !$0.isEmpty }
        guard !parts.isEmpty else { return }
        var parents: [(TomlTable, String)] = []
        var current = root
        for i in 0..<(parts.count - 1) {
            guard let nested = current.table(parts[i]) else { return }
            parents.append((current, parts[i]))
            current = nested
        }

        current.remove(parts[parts.count - 1])

        for (parent, key) in parents.reversed() {
            if let table = parent.table(key), table.count == 0 {
                parent.remove(key)
            } else {
                break
            }
        }
    }
}
