namespace HugoerMobile.Models;

/// <summary>A file or folder inside the repo's content directory (GitHub Contents API item).</summary>
public sealed class ContentItem
{
    public required string Name { get; init; }

    /// <summary>Full repo-relative path, e.g. "content/posts/hello-world.md".</summary>
    public required string Path { get; init; }

    public bool IsDirectory { get; init; }

    /// <summary>Git blob SHA — required by the Contents API when updating/deleting a file.</summary>
    public string? Sha { get; init; }

    public long Size { get; init; }

    public bool IsMarkdown =>
        !IsDirectory &&
        (Name.EndsWith(".md", StringComparison.OrdinalIgnoreCase) ||
         Name.EndsWith(".markdown", StringComparison.OrdinalIgnoreCase));
}
