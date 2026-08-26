namespace HugoerMobile.Models;

/// <summary>Parsed YAML front matter + Markdown body of a Hugo content file.</summary>
public sealed class FrontMatterDocument
{
    public Dictionary<string, string> Fields { get; init; } = new(StringComparer.OrdinalIgnoreCase);
    public string Body { get; set; } = string.Empty;

    public string Title
    {
        get => Fields.TryGetValue("title", out var v) ? v : string.Empty;
        set => Fields["title"] = value;
    }

    public string Date
    {
        get => Fields.TryGetValue("date", out var v) ? v : string.Empty;
        set => Fields["date"] = value;
    }

    public string Tags
    {
        get => Fields.TryGetValue("tags", out var v) ? v : string.Empty;
        set => Fields["tags"] = value;
    }

    public string Categories
    {
        get => Fields.TryGetValue("categories", out var v) ? v : string.Empty;
        set => Fields["categories"] = value;
    }

    public bool Draft
    {
        get => Fields.TryGetValue("draft", out var v) && v.Equals("true", StringComparison.OrdinalIgnoreCase);
        set => Fields["draft"] = value ? "true" : "false";
    }
}
