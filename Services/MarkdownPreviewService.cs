using System.Text;
using System.Text.RegularExpressions;
using Markdig;

namespace Hugoer.Services;

public static partial class MarkdownPreviewService
{
    private static readonly MarkdownPipeline Pipeline = new MarkdownPipelineBuilder()
        .UseAdvancedExtensions()
        .UseEmojiAndSmiley()
        .Build();

    public static string StripFrontMatter(string markdown)
    {
        if (string.IsNullOrEmpty(markdown))
            return string.Empty;

        var m = FrontMatterRegex().Match(markdown);
        return m.Success ? markdown[m.Length..] : markdown;
    }

    public static string ExtractFrontMatter(string markdown)
    {
        if (string.IsNullOrEmpty(markdown))
            return string.Empty;

        var m = FrontMatterRegex().Match(markdown);
        return m.Success ? m.Groups[1].Value.Trim() : string.Empty;
    }

    public static string ToHtmlDocument(string markdown, string? title = null)
    {
        var bodyMd = StripFrontMatter(markdown);
        var bodyHtml = Markdown.ToHtml(bodyMd, Pipeline);
        var pageTitle = string.IsNullOrWhiteSpace(title) ? "Preview" : title;

        var sb = new StringBuilder();
        sb.AppendLine("<!DOCTYPE html>");
        sb.AppendLine("<html lang=\"zh-Hant\"><head><meta charset=\"utf-8\"/>");
        sb.AppendLine($"<title>{System.Net.WebUtility.HtmlEncode(pageTitle)}</title>");
        sb.AppendLine("<style>");
        sb.AppendLine(DarkPreviewCss);
        sb.AppendLine("</style></head><body>");
        sb.AppendLine("<article class=\"markdown-body\">");
        sb.AppendLine(bodyHtml);
        sb.AppendLine("</article></body></html>");
        return sb.ToString();
    }

    public static string ToPlainPreviewHint(string markdown)
    {
        var body = StripFrontMatter(markdown).Trim();
        if (body.Length == 0)
            return "（空白預覽）";
        return body.Length > 2000 ? body[..2000] + "\n…" : body;
    }

    private const string DarkPreviewCss = """
:root { color-scheme: dark; }
html, body {
  margin: 0; padding: 0;
  background: #161310;
  color: #f6f0e8;
  font-family: "Segoe UI", "Microsoft JhengHei", sans-serif;
  font-size: 15px;
  line-height: 1.65;
}
.markdown-body { padding: 20px 24px 40px; max-width: 820px; }
h1, h2, h3, h4, h5, h6 { color: #e8a15a; margin-top: 1.4em; margin-bottom: 0.5em; font-weight: 650; }
h1 { font-size: 1.9em; border-bottom: 1px solid #3a312a; padding-bottom: 0.25em; }
h2 { font-size: 1.5em; border-bottom: 1px solid #3a312a; padding-bottom: 0.2em; }
h3 { font-size: 1.25em; }
p { margin: 0.75em 0; }
a { color: #e8a15a; text-decoration: none; }
a:hover { text-decoration: underline; }
code {
  font-family: Consolas, "Cascadia Mono", monospace;
  background: #2a241c;
  padding: 0.15em 0.4em;
  border-radius: 4px;
  font-size: 0.92em;
}
pre {
  background: #1c1814;
  border: 1px solid #3a312a;
  border-radius: 8px;
  padding: 12px 14px;
  overflow: auto;
}
pre code { background: transparent; padding: 0; }
blockquote {
  margin: 1em 0;
  padding: 0.4em 1em;
  border-left: 4px solid #c47a32;
  background: #221c18;
  color: #e7d9cc;
}
table { border-collapse: collapse; width: 100%; margin: 1em 0; }
th, td { border: 1px solid #3a312a; padding: 8px 10px; }
th { background: #2a241c; }
img { max-width: 100%; border-radius: 6px; }
hr { border: none; border-top: 1px solid #3a312a; margin: 1.5em 0; }
ul, ol { padding-left: 1.4em; }
li { margin: 0.25em 0; }
strong { color: #fff; }
""";

    [GeneratedRegex(@"^---\r?\n(.*?)\r?\n---\r?\n?", RegexOptions.Singleline)]
    private static partial Regex FrontMatterRegex();
}
