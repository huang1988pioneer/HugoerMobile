using Markdig;

namespace HugoerMobile.Services;

/// <summary>
/// Renders a single Markdown article body to a mobile-friendly HTML preview.
/// Unlike the desktop app (which can shell out to `hugo server` and render the whole
/// site through the active theme), this only renders the article body itself —
/// good enough to check formatting, images, tables and code blocks while writing on the go.
/// </summary>
public sealed class MarkdownPreviewService
{
    private readonly MarkdownPipeline _pipeline = new MarkdownPipelineBuilder()
        .UseAdvancedExtensions()
        .UseEmphasisExtras()
        .UseSoftlineBreakAsHardlineBreak()
        .Build();

    public string RenderToHtmlDocument(string markdown, bool isDarkMode)
    {
        var body = Markdown.ToHtml(markdown ?? string.Empty, _pipeline);
        var bg = isDarkMode ? "#141318" : "#F6F5FA";
        var fg = isDarkMode ? "#F2F1F7" : "#1B1B1F";
        var codeBg = isDarkMode ? "#26242E" : "#EFEDF5";
        var link = "#6E56CF";

        return $$"""
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1.0" />
          <style>
            body {
              margin: 0;
              padding: 16px;
              background: {{bg}};
              color: {{fg}};
              font-family: -apple-system, Roboto, "Noto Sans TC", sans-serif;
              font-size: 16px;
              line-height: 1.6;
              word-wrap: break-word;
            }
            img { max-width: 100%; border-radius: 8px; }
            a { color: {{link}}; }
            pre { background: {{codeBg}}; padding: 12px; border-radius: 8px; overflow-x: auto; }
            code { background: {{codeBg}}; padding: 2px 4px; border-radius: 4px; }
            pre code { background: none; padding: 0; }
            blockquote { border-left: 4px solid {{link}}; margin: 0; padding-left: 12px; opacity: 0.85; }
            table { border-collapse: collapse; width: 100%; }
            th, td { border: 1px solid {{codeBg}}; padding: 6px 8px; }
            h1, h2, h3 { line-height: 1.3; }
          </style>
        </head>
        <body>
        {{body}}
        </body>
        </html>
        """;
    }
}
