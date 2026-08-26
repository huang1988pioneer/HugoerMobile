import Foundation

enum ParamFieldKind: String, Codable {
    case string
    case bool
    case number
    case array
    case nested

    var label: String {
        switch self {
        case .bool: return "布林"
        case .number: return "數字"
        case .array: return "陣列"
        case .nested: return "巢狀"
        case .string: return "文字"
        }
    }
}

/// One row in the "Params 表單" section of the 設定檔 tab.
struct ParamFieldItem: Identifiable, Equatable {
    var key: String
    var path: String
    var displayName: String = ""
    var description: String = ""
    var kind: ParamFieldKind
    var stringValue: String = ""
    var boolValue: Bool = false
    var isKnown: Bool = false
    var isConfigured: Bool = false

    var id: String { path }

    var isEditable: Bool { kind != .nested }
    var isBool: Bool { kind == .bool }
    var isText: Bool { kind != .bool }
    var kindLabel: String { kind.label }
}
