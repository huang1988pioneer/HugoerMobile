package com.hugoer.mobile.data.service

/**
 * Renders a single Markdown article body to a mobile-friendly HTML preview document.
 * Uses the dependency-free [MarkdownToHtml] converter instead of Markdig, since only the
 * article body (not the whole themed site) needs to be previewed on mobile.
 */
class MarkdownPreviewService {

    fun renderToHtmlDocument(markdown: String, isDarkMode: Boolean): String {
        val body = MarkdownToHtml.convert(markdown)
        val bg = if (isDarkMode) "#141318" else "#F6F5FA"
        val fg = if (isDarkMode) "#F2F1F7" else "#1B1B1F"
        val codeBg = if (isDarkMode) "#26242E" else "#EFEDF5"
        val link = "#6E56CF"

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
                  background: $bg;
                  color: $fg;
                  font-family: -apple-system, Roboto, "Noto Sans TC", sans-serif;
                  font-size: 16px;
                  line-height: 1.6;
                  word-wrap: break-word;
                }
                img { max-width: 100%; border-radius: 8px; }
                a { color: $link; }
                pre { background: $codeBg; padding: 12px; border-radius: 8px; overflow-x: auto; }
                code { background: $codeBg; padding: 2px 4px; border-radius: 4px; }
                pre code { background: none; padding: 0; }
                blockquote { border-left: 4px solid $link; margin: 0; padding-left: 12px; opacity: 0.85; }
                table { border-collapse: collapse; width: 100%; }
                th, td { border: 1px solid $codeBg; padding: 6px 8px; }
                h1, h2, h3 { line-height: 1.3; }
              </style>
            </head>
            <body>
            $body
            </body>
            </html>
        """.trimIndent()
    }
}
