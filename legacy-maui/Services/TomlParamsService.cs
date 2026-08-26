using System.Collections.ObjectModel;
using System.Globalization;
using HugoerMobile.Models;
using Tomlyn;
using Tomlyn.Model;

namespace HugoerMobile.Services;

/// <summary>
/// Structured "Params 表單" editor for hugo.toml's [params] table, ported from the
/// desktop app. Depends only on Tomlyn, so it is identical on mobile.
/// </summary>
public sealed class TomlParamsService
{
    /// <summary>Common Hugo + Stack theme params with labels.</summary>
    public static IReadOnlyList<(string Key, string Label, string Description, ParamFieldKind Kind)> KnownParams { get; } =
    [
        ("description", "網站描述", "SEO / 首頁摘要", ParamFieldKind.String),
        ("mainSections", "主要區塊", "例如 post（逗號分隔）", ParamFieldKind.Array),
        ("colorScheme.default", "色彩模式", "auto / light / dark", ParamFieldKind.String),
        ("colorScheme.toggle", "允許切換色彩模式", "顯示明暗模式切換", ParamFieldKind.Bool),
        ("favicon", "網站圖示", "Stack v4 assets 路徑，例如 img/favicon.png", ParamFieldKind.String),
        ("footer.since", "Footer 起始年", "版權起始年份", ParamFieldKind.Number),
        ("footer.customText", "Footer 文字", "自訂頁尾文字", ParamFieldKind.String),
        ("sidebar.subtitle", "側欄副標", "個人簡介副標", ParamFieldKind.String),
        ("sidebar.avatar", "側欄頭像", "Stack v4 單一 assets 路徑；空白代表停用", ParamFieldKind.String),
        ("article.toc", "文章目錄", "Stack v4 顯示目錄", ParamFieldKind.Bool),
        ("article.readingTime", "閱讀時間", "Stack v4 顯示預估閱讀時間", ParamFieldKind.Bool),
        ("article.list.showTags", "列表顯示標籤", "Stack v4 文章列表顯示 tags", ParamFieldKind.Bool),
        ("comments.enabled", "啟用留言", "Stack v4 留言總開關", ParamFieldKind.Bool),
        ("comments.provider", "留言服務", "giscus / utterances / waline 等", ParamFieldKind.String),
    ];

    public ObservableCollection<ParamFieldItem> LoadParamsForm(string tomlText)
    {
        var result = new ObservableCollection<ParamFieldItem>();
        var root = TryParse(tomlText);

        var flat = new Dictionary<string, (ParamFieldKind Kind, string Value, bool Bool)>(StringComparer.OrdinalIgnoreCase);
        if (root is not null)
        {
            var paramsTable = GetOrNullTable(root, "params");
            if (paramsTable is not null)
                FlattenTable(paramsTable, "", flat);
        }

        var used = new HashSet<string>(StringComparer.OrdinalIgnoreCase);

        foreach (var (key, label, desc, kind) in KnownParams)
        {
            used.Add(key);
            flat.TryGetValue(key, out var existing);
            result.Add(new ParamFieldItem
            {
                Key = key.Contains('.') ? key.Split('.')[^1] : key,
                Path = key,
                DisplayName = label,
                Description = desc,
                Kind = kind,
                StringValue = existing.Value ?? string.Empty,
                BoolValue = existing.Bool,
                IsConfigured = flat.ContainsKey(key),
                IsKnown = true
            });
        }

        foreach (var (path, val) in flat.OrderBy(x => x.Key, StringComparer.OrdinalIgnoreCase))
        {
            if (used.Contains(path)) continue;
            result.Add(new ParamFieldItem
            {
                Key = path.Contains('.') ? path.Split('.')[^1] : path,
                Path = path,
                DisplayName = path,
                Description = "自訂參數",
                Kind = val.Kind,
                StringValue = val.Value,
                BoolValue = val.Bool,
                IsConfigured = true,
                IsKnown = false
            });
        }

        return result;
    }

    public string ApplyParamsToToml(string tomlText, IEnumerable<ParamFieldItem> fields)
    {
        var root = TryParse(tomlText) ?? new TomlTable();

        if (!root.TryGetValue("params", out var paramsObj) || paramsObj is not TomlTable paramsTable)
        {
            paramsTable = new TomlTable();
            root["params"] = paramsTable;
        }

        foreach (var field in fields)
        {
            if (string.IsNullOrWhiteSpace(field.Path)) continue;
            if (!field.IsEditable) continue;

            if (!field.IsConfigured)
            {
                RemovePath(paramsTable, field.Path);
                continue;
            }

            if (!field.IsKnown && field.Kind != ParamFieldKind.Bool && string.IsNullOrWhiteSpace(field.StringValue))
                continue;

            SetPathValue(paramsTable, field.Path, field);
        }

        return TomlSerializer.Serialize(root);
    }

    public string UpsertSimpleRootKeys(string tomlText, IDictionary<string, string> rootKeys)
    {
        var root = TryParse(tomlText) ?? new TomlTable();
        foreach (var (k, v) in rootKeys)
        {
            if (string.IsNullOrWhiteSpace(k)) continue;
            root[k] = v ?? string.Empty;
        }

        return TomlSerializer.Serialize(root);
    }

    public (string BaseUrl, string Title, string LanguageCode, string Theme) ReadBasicFields(string tomlText)
    {
        var root = TryParse(tomlText) ?? new TomlTable();
        return (
            GetString(root, "baseURL"),
            GetString(root, "title"),
            GetString(root, "languageCode"),
            GetString(root, "theme"));
    }

    private static string GetString(TomlTable root, string key) =>
        root.TryGetValue(key, out var value) ? value?.ToString() ?? string.Empty : string.Empty;

    private static TomlTable? TryParse(string tomlText)
    {
        if (string.IsNullOrWhiteSpace(tomlText))
            return new TomlTable();
        try
        {
            return TomlSerializer.Deserialize<TomlTable>(tomlText);
        }
        catch
        {
            return null;
        }
    }

    private static TomlTable? GetOrNullTable(TomlTable root, string key) =>
        root.TryGetValue(key, out var obj) && obj is TomlTable t ? t : null;

    private static void FlattenTable(
        TomlTable table,
        string prefix,
        Dictionary<string, (ParamFieldKind Kind, string Value, bool Bool)> flat)
    {
        foreach (var kv in table)
        {
            var path = string.IsNullOrEmpty(prefix) ? kv.Key : $"{prefix}.{kv.Key}";
            switch (kv.Value)
            {
                case string s:
                    flat[path] = (ParamFieldKind.String, s, false);
                    break;
                case bool b:
                    flat[path] = (ParamFieldKind.Bool, b ? "true" : "false", b);
                    break;
                case long l:
                    flat[path] = (ParamFieldKind.Number, l.ToString(CultureInfo.InvariantCulture), false);
                    break;
                case int i:
                    flat[path] = (ParamFieldKind.Number, i.ToString(CultureInfo.InvariantCulture), false);
                    break;
                case double d:
                    flat[path] = (ParamFieldKind.Number, d.ToString(CultureInfo.InvariantCulture), false);
                    break;
                case TomlArray arr when arr.All(item => item is not TomlTable):
                    flat[path] = (ParamFieldKind.Array, string.Join(", ", arr.Select(FormatScalar)), false);
                    break;
                case TomlArray:
                    flat[path] = (ParamFieldKind.Nested, "複雜物件陣列；請用「原始 TOML」編輯", false);
                    break;
                case TomlTable nested:
                    FlattenTable(nested, path, flat);
                    break;
                default:
                    if (kv.Value is not null)
                        flat[path] = (ParamFieldKind.String, kv.Value.ToString() ?? "", false);
                    break;
            }
        }
    }

    private static string FormatScalar(object? o) => o switch
    {
        null => "",
        string s => s,
        bool b => b ? "true" : "false",
        IFormattable f => f.ToString(null, CultureInfo.InvariantCulture) ?? "",
        _ => o.ToString() ?? ""
    };

    private static void SetPathValue(TomlTable rootParams, string path, ParamFieldItem field)
    {
        var parts = path.Split('.', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries);
        if (parts.Length == 0) return;

        var current = rootParams;
        for (var i = 0; i < parts.Length - 1; i++)
        {
            if (!current.TryGetValue(parts[i], out var next) || next is not TomlTable nested)
            {
                nested = new TomlTable();
                current[parts[i]] = nested;
            }
            current = nested;
        }

        var leaf = parts[^1];
        current[leaf] = field.Kind switch
        {
            ParamFieldKind.Bool => field.BoolValue,
            ParamFieldKind.Number when long.TryParse(field.StringValue, NumberStyles.Integer, CultureInfo.InvariantCulture, out var l) => l,
            ParamFieldKind.Number when double.TryParse(field.StringValue, NumberStyles.Float, CultureInfo.InvariantCulture, out var d) => d,
            ParamFieldKind.Array => ParseArray(field.StringValue),
            _ => field.StringValue
        };
    }

    private static void RemovePath(TomlTable root, string path)
    {
        var parts = path.Split('.', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries);
        if (parts.Length == 0) return;
        var parents = new List<(TomlTable Parent, string Key)>();
        var current = root;
        for (var index = 0; index < parts.Length - 1; index++)
        {
            if (!current.TryGetValue(parts[index], out var value) || value is not TomlTable nested) return;
            parents.Add((current, parts[index]));
            current = nested;
        }

        current.Remove(parts[^1]);
        for (var index = parents.Count - 1; index >= 0; index--)
        {
            var (parent, key) = parents[index];
            if (parent[key] is TomlTable table && table.Count == 0)
                parent.Remove(key);
            else
                break;
        }
    }

    private static TomlArray ParseArray(string value)
    {
        var arr = new TomlArray();
        if (string.IsNullOrWhiteSpace(value)) return arr;
        foreach (var part in value.Split(',', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries))
            arr.Add(part);
        return arr;
    }
}
