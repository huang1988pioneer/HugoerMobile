namespace HugoerMobile.Models;

public sealed class GitHubFileContent
{
    public required string Path { get; init; }
    public required string Sha { get; init; }
    public string TextContent { get; init; } = string.Empty;
}

public sealed class RepoPermissions
{
    public bool Pull { get; init; }
    public bool Push { get; init; }
    public bool Admin { get; init; }
}

public sealed class GitHubRepoInfo
{
    public required string Owner { get; init; }
    public required string Name { get; init; }
    public string DefaultBranch { get; init; } = "main";
    public bool Private { get; init; }
    public string HtmlUrl { get; init; } = string.Empty;
    public string? Description { get; init; }
    public RepoPermissions Permissions { get; init; } = new();
}

public sealed class GitHubPagesStatus
{
    public bool Enabled { get; init; }
    public string? Status { get; init; }
    public string? HtmlUrl { get; init; }
    public string? SourceBranch { get; init; }
    public string? SourcePath { get; init; }
    public string? BuildType { get; init; }
    public string Message { get; init; } = string.Empty;
}

public enum WorkflowRunStatus
{
    Unknown,
    Queued,
    InProgress,
    Completed
}

public enum WorkflowRunConclusion
{
    None,
    Success,
    Failure,
    Cancelled,
    Skipped,
    Other
}

public sealed class WorkflowRunInfo
{
    public long Id { get; init; }
    public string Name { get; init; } = string.Empty;
    public WorkflowRunStatus Status { get; init; }
    public WorkflowRunConclusion Conclusion { get; init; }
    public string HeadBranch { get; init; } = string.Empty;
    public string Event { get; init; } = string.Empty;
    public string HtmlUrl { get; init; } = string.Empty;
    public DateTimeOffset CreatedAt { get; init; }

    public string StatusLabel => Status switch
    {
        WorkflowRunStatus.Queued => "排隊中",
        WorkflowRunStatus.InProgress => "建置中…",
        WorkflowRunStatus.Completed => Conclusion switch
        {
            WorkflowRunConclusion.Success => "成功",
            WorkflowRunConclusion.Failure => "失敗",
            WorkflowRunConclusion.Cancelled => "已取消",
            WorkflowRunConclusion.Skipped => "已略過",
            _ => "已完成"
        },
        _ => "未知"
    };
}

public sealed class GitHubWorkflowInfo
{
    public long Id { get; init; }
    public string Name { get; init; } = string.Empty;
    public string Path { get; init; } = string.Empty;
}
