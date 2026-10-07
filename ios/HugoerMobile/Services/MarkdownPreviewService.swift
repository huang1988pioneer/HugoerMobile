import Foundation

/// Renders a single Markdown article body to a mobile-friendly HTML preview.
/// This only renders the article body itself — good enough to check formatting,
/// images, and code blocks while writing on the go (not a full Hugo theme render).
enum MarkdownPreviewService {
    static func renderToHtmlDocument(_ markdown: String, isDarkMode: Bool) -> String {
        let body = MiniMarkdown.toHtml(markdown)
        let bg = isDarkMode ? "#161310" : "#FBF7F2"
        let fg = isDarkMode ? "#F6F0E8" : "#1C1612"
        let codeBg = isDarkMode ? "#221C18" : "#F3EBE3"
        let link = isDarkMode ? "#E8A15A" : "#8C4E1A"

        return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1.0" />
          <style>
            body {
              margin: 0;
              padding: 16px;
              background: \(bg);
              color: \(fg);
              font-family: -apple-system, "Helvetica Neue", "PingFang TC", sans-serif;
              font-size: 16px;
              line-height: 1.6;
              word-wrap: break-word;
            }
            img { max-width: 100%; border-radius: 8px; }
            a { color: \(link); }
            pre { background: \(codeBg); padding: 12px; border-radius: 8px; overflow-x: auto; }
            code { background: \(codeBg); padding: 2px 4px; border-radius: 4px; }
            pre code { background: none; padding: 0; }
            blockquote { border-left: 4px solid \(link); margin: 0; padding-left: 12px; opacity: 0.85; }
            table { border-collapse: collapse; width: 100%; }
            th, td { border: 1px solid \(codeBg); padding: 6px 8px; }
            h1, h2, h3 { line-height: 1.3; }
          </style>
        </head>
        <body>
        \(body)
        </body>
        </html>
        """
    }
}
