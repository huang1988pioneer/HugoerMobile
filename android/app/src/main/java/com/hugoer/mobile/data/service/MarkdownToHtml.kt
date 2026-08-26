package com.hugoer.mobile.data.service

/**
 * A small, dependency-free Markdown → HTML converter. Not a full CommonMark implementation,
 * but covers headings, bold/italic, links/images, inline & fenced code blocks, ordered/unordered
 * lists, blockquotes and paragraphs — good enough for previewing a single article body.
 */
object MarkdownToHtml {

    fun convert(markdown: String): String {
        val source = markdown.replace("\r\n", "\n")
        val lines = source.split("\n")

        val html = StringBuilder()
        var i = 0

        var inUnorderedList = false
        var inOrderedList = false
        var inBlockquote = false
        val paragraphBuffer = StringBuilder()

        fun closeLists() {
            if (inUnorderedList) { html.append("</ul>\n"); inUnorderedList = false }
            if (inOrderedList) { html.append("</ol>\n"); inOrderedList = false }
        }

        fun closeBlockquote() {
            if (inBlockquote) { html.append("</blockquote>\n"); inBlockquote = false }
        }

        fun flushParagraph() {
            if (paragraphBuffer.isNotEmpty()) {
                html.append("<p>").append(inlineToHtml(paragraphBuffer.toString().trim())).append("</p>\n")
                paragraphBuffer.clear()
            }
        }

        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()

            // Fenced code block ```lang ... ```
            if (trimmed.startsWith("```")) {
                flushParagraph(); closeLists(); closeBlockquote()
                val lang = trimmed.removePrefix("```").trim()
                val code = StringBuilder()
                i++
                while (i < lines.size && !lines[i].trim().startsWith("```")) {
                    code.append(escapeHtml(lines[i])).append("\n")
                    i++
                }
                i++ // skip closing fence
                val langClass = if (lang.isNotEmpty()) " class=\"language-${escapeHtml(lang)}\"" else ""
                html.append("<pre><code$langClass>").append(code).append("</code></pre>\n")
                continue
            }

            // Blank line
            if (trimmed.isEmpty()) {
                flushParagraph(); closeLists(); closeBlockquote()
                i++
                continue
            }

            // Headings
            val headingMatch = Regex("^(#{1,6})\\s+(.*)$").find(trimmed)
            if (headingMatch != null) {
                flushParagraph(); closeLists(); closeBlockquote()
                val level = headingMatch.groupValues[1].length
                val text = headingMatch.groupValues[2].trim()
                html.append("<h$level>").append(inlineToHtml(text)).append("</h$level>\n")
                i++
                continue
            }

            // Horizontal rule
            if (Regex("^(-{3,}|\\*{3,}|_{3,})$").matches(trimmed)) {
                flushParagraph(); closeLists(); closeBlockquote()
                html.append("<hr/>\n")
                i++
                continue
            }

            // Blockquote
            if (trimmed.startsWith(">")) {
                flushParagraph(); closeLists()
                if (!inBlockquote) { html.append("<blockquote>\n"); inBlockquote = true }
                val content = trimmed.removePrefix(">").trim()
                html.append("<p>").append(inlineToHtml(content)).append("</p>\n")
                i++
                continue
            }

            // Unordered list
            val ulMatch = Regex("^[-*+]\\s+(.*)$").find(trimmed)
            if (ulMatch != null) {
                flushParagraph(); closeBlockquote()
                if (inOrderedList) { html.append("</ol>\n"); inOrderedList = false }
                if (!inUnorderedList) { html.append("<ul>\n"); inUnorderedList = true }
                html.append("<li>").append(inlineToHtml(ulMatch.groupValues[1])).append("</li>\n")
                i++
                continue
            }

            // Ordered list
            val olMatch = Regex("^\\d+\\.\\s+(.*)$").find(trimmed)
            if (olMatch != null) {
                flushParagraph(); closeBlockquote()
                if (inUnorderedList) { html.append("</ul>\n"); inUnorderedList = false }
                if (!inOrderedList) { html.append("<ol>\n"); inOrderedList = true }
                html.append("<li>").append(inlineToHtml(olMatch.groupValues[1])).append("</li>\n")
                i++
                continue
            }

            // Default: accumulate into paragraph (soft line breaks become <br/>)
            closeLists(); closeBlockquote()
            if (paragraphBuffer.isNotEmpty()) paragraphBuffer.append("\n")
            paragraphBuffer.append(line.trim())
            i++
        }

        flushParagraph(); closeLists(); closeBlockquote()
        return html.toString()
    }

    private fun inlineToHtml(textIn: String): String {
        var text = escapeHtml(textIn)

        // Images: ![alt](url)
        text = Regex("!\\[([^\\]]*)]\\(([^)\\s]+)(?:\\s+\"[^\"]*\")?\\)").replace(text) { m ->
            "<img alt=\"${m.groupValues[1]}\" src=\"${m.groupValues[2]}\" />"
        }
        // Links: [text](url)
        text = Regex("\\[([^\\]]*)]\\(([^)\\s]+)(?:\\s+\"[^\"]*\")?\\)").replace(text) { m ->
            "<a href=\"${m.groupValues[2]}\">${m.groupValues[1]}</a>"
        }
        // Inline code: `code`
        text = Regex("`([^`]+)`").replace(text) { m -> "<code>${m.groupValues[1]}</code>" }
        // Bold: **text** or __text__
        text = Regex("\\*\\*([^*]+)\\*\\*").replace(text) { m -> "<strong>${m.groupValues[1]}</strong>" }
        text = Regex("__([^_]+)__").replace(text) { m -> "<strong>${m.groupValues[1]}</strong>" }
        // Italic: *text* or _text_
        text = Regex("\\*([^*]+)\\*").replace(text) { m -> "<em>${m.groupValues[1]}</em>" }
        text = Regex("(?<![A-Za-z0-9])_([^_]+)_(?![A-Za-z0-9])").replace(text) { m -> "<em>${m.groupValues[1]}</em>" }
        // Soft line breaks
        text = text.replace("\n", "<br/>\n")

        return text
    }

    private fun escapeHtml(text: String): String =
        text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
