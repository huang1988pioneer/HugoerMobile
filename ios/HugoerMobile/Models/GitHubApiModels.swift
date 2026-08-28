import Foundation

struct GitHubFileContent {
    var path: String
    var sha: String
    var textContent: String = ""
}

struct RepoPermissions: Equatable {
    var pull: Bool = false
    var push: Bool = false
    var admin: Bool = false
}

struct GitHubRepoInfo {
    var owner: String
    var name: String
    var defaultBranch: String = "main"
    var isPrivate: Bool = false
    var htmlUrl: String = ""
    var description: String?
    var permissions: RepoPermissions = RepoPermissions()
}

struct GitHubPagesStatus {
    var enabled: Bool = false
    var status: String?
    var htmlUrl: String?
    var sourceBranch: String?
    var sourcePath: String?
    var buildType: String?
    var message: String = ""
}

enum WorkflowRunStatus {
    case unknown
    case queued
    case inProgress
    case completed
}

enum WorkflowRunConclusion {
    case none
    case success
    case failure
    case cancelled
    case skipped
    case other
}

struct WorkflowRunInfo: Identifiable {
    var id: Int64
    var name: String = ""
    var status: WorkflowRunStatus = .unknown
    var conclusion: WorkflowRunConclusion = .none
    var headBranch: String = ""
    var event: String = ""
    var htmlUrl: String = ""
    var createdAt: Date = Date()

    var statusLabel: String {
        switch status {
        case .queued: return "排隊中"
        case .inProgress: return "建置中…"
        case .completed:
            switch conclusion {
            case .success: return "成功"
            case .failure: return "失敗"
            case .cancelled: return "已取消"
            case .skipped: return "已略過"
            default: return "已完成"
            }
        default: return "未知"
        }
    }
}

struct GitHubWorkflowInfo {
    var id: Int64
    var name: String = ""
    var path: String = ""
}

struct GitHubApiError: LocalizedError {
    let statusCode: Int
    let message: String
    var errorDescription: String? { message }
}
