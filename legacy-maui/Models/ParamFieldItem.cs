namespace HugoerMobile.Models;

public enum ParamFieldKind
{
    String,
    Bool,
    Number,
    Array,
    Nested
}

/// <summary>One row in the "Params 表單" section of the 設定檔 tab. Ported from the desktop app.</summary>
public sealed class ParamFieldItem
{
    public required string Key { get; init; }
    public required string Path { get; init; }
    public string DisplayName { get; init; } = string.Empty;
    public string Description { get; init; } = string.Empty;
    public ParamFieldKind Kind { get; init; }
    public string StringValue { get; set; } = string.Empty;
    public bool BoolValue { get; set; }
    public bool IsKnown { get; init; }
    public bool IsConfigured { get; set; }
    public bool IsEditable => Kind != ParamFieldKind.Nested;
    public bool IsBool => Kind == ParamFieldKind.Bool;
    public bool IsText => Kind != ParamFieldKind.Bool;

    public string KindLabel => Kind switch
    {
        ParamFieldKind.Bool => "布林",
        ParamFieldKind.Number => "數字",
        ParamFieldKind.Array => "陣列",
        ParamFieldKind.Nested => "巢狀",
        _ => "文字"
    };
}
