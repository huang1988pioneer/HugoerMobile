import Foundation

/// A small, dependency-free Markdown → HTML converter — "good enough for checking
/// formatting while writing," not a full CommonMark implementation. Supports:
/// headings, bold/italic, inline code, fenced code blocks, links, images,
/// unordered/ordered lists, blockquotes, and paragraphs.
enum MiniMarkdown {
    static func toHtml(_ markdown: String) -> String {
        let lines = markdown.replacingOccurrences(of: "\r\n", with: "\n").components(separatedBy: "\n")
        var html: [String] = []
        var index = 0

        var paragraphBuffer: [String] = []
        var listStack: [Character] = [] // 'u' or 'o'

        func flushParagraph() {
            guard !paragraphBuffer.isEmpty else { return }
            let joined = paragraphBuffer.joined(separator: "\n")
            html.append("<p>\(inline(joined))</p>")
            paragraphBuffer.removeAll()
        }

        func closeLists() {
            while let top = listStack.popLast() {
                html.append(top == "u" ? "</ul>" : "</ol>")
            }
        }

        while index < lines.count {
            let line = lines[index]
            let trimmed = line.trimmingCharacters(in: .whitespaces)

            // Fenced code block.
            if trimmed.hasPrefix("```") {
                flushParagraph()
                closeLists()
                let lang = String(trimmed.dropFirst(3)).trimmingCharacters(in: .whitespaces)
                var codeLines: [String] = []
                index += 1
                while index < lines.count && !lines[index].trimmingCharacters(in: .whitespaces).hasPrefix("```") {
                    codeLines.append(lines[index])
                    index += 1
                }
                let langClass = lang.isEmpty ? "" : " class=\"language-\(escapeHtml(lang))\""
                html.append("<pre><code\(langClass)>\(escapeHtml(codeLines.joined(separator: "\n")))</code></pre>")
                index += 1 // skip closing fence
                continue
            }

            // Blank line ends paragraph/list.
            if trimmed.isEmpty {
                flushParagraph()
                closeLists()
                index += 1
                continue
            }

            // Headings.
            if let headingMatch = headingLevel(trimmed) {
                flushParagraph()
                closeLists()
                html.append("<h\(headingMatch.level)>\(inline(headingMatch.text))</h\(headingMatch.level)>")
                index += 1
                continue
            }

            // Blockquote.
            if trimmed.hasPrefix(">") {
                flushParagraph()
                closeLists()
                var quoteLines: [String] = []
                while index < lines.count, lines[index].trimmingCharacters(in: .whitespaces).hasPrefix(">") {
                    let content = lines[index].trimmingCharacters(in: .whitespaces).dropFirst()
                    quoteLines.append(String(content).trimmingCharacters(in: .whitespaces))
                    index += 1
                }
                html.append("<blockquote><p>\(inline(quoteLines.joined(separator: "\n")))</p></blockquote>")
                continue
            }

            // Unordered list item.
            if let itemText = unorderedListItem(trimmed) {
                flushParagraph()
                if listStack.last != "u" {
                    closeLists()
                    html.append("<ul>")
                    listStack.append("u")
                }
                html.append("<li>\(inline(itemText))</li>")
                index += 1
                continue
            }

            // Ordered list item.
            if let itemText = orderedListItem(trimmed) {
                flushParagraph()
                if listStack.last != "o" {
                    closeLists()
                    html.append("<ol>")
                    listStack.append("o")
                }
                html.append("<li>\(inline(itemText))</li>")
                index += 1
                continue
            }

            // Otherwise, accumulate into paragraph.
            closeLists()
            paragraphBuffer.append(trimmed)
            index += 1
        }

        flushParagraph()
        closeLists()

        return html.joined(separator: "\n")
    }

    private static func headingLevel(_ line: String) -> (level: Int, text: String)? {
        guard line.hasPrefix("#") else { return nil }
        var level = 0
        var chars = Substring(line)
        while chars.first == "#" && level < 6 {
            level += 1
            chars = chars.dropFirst()
        }
        guard chars.first == " " || chars.isEmpty else { return nil }
        return (level, chars.trimmingCharacters(in: .whitespaces))
    }

    private static func unorderedListItem(_ line: String) -> String? {
        for marker in ["- ", "* ", "+ "] {
            if line.hasPrefix(marker) {
                return String(line.dropFirst(marker.count))
            }
        }
        return nil
    }

    private static func orderedListItem(_ line: String) -> String? {
        guard let dotIndex = line.firstIndex(of: ".") else { return nil }
        let prefix = line[line.startIndex..<dotIndex]
        guard !prefix.isEmpty, prefix.allSatisfy({ $0.isNumber }) else { return nil }
        let afterDot = line.index(after: dotIndex)
        guard afterDot < line.endIndex, line[afterDot] == " " else { return nil }
        return String(line[line.index(after: afterDot)...])
    }

    /// Handles inline formatting: images, links, bold, italic, inline code.
    private static func inline(_ text: String) -> String {
        var result = escapeHtml(text)

        // Inline code: `code`
        result = replaceRegex(result, pattern: "`([^`]+)`") { groups in
            "<code>\(groups[1])</code>"
        }

        // Images: ![alt](url)
        result = replaceRegex(result, pattern: "!\\[([^\\]]*)\\]\\(([^\\)]+)\\)") { groups in
            "<img src=\"\(groups[2])\" alt=\"\(groups[1])\" />"
        }

        // Links: [text](url)
        result = replaceRegex(result, pattern: "\\[([^\\]]*)\\]\\(([^\\)]+)\\)") { groups in
            "<a href=\"\(groups[2])\">\(groups[1])</a>"
        }

        // Bold: **text** or __text__
        result = replaceRegex(result, pattern: "\\*\\*([^*]+)\\*\\*") { groups in
            "<strong>\(groups[1])</strong>"
        }
        result = replaceRegex(result, pattern: "__([^_]+)__") { groups in
            "<strong>\(groups[1])</strong>"
        }

        // Italic: *text* or _text_
        result = replaceRegex(result, pattern: "\\*([^*]+)\\*") { groups in
            "<em>\(groups[1])</em>"
        }
        result = replaceRegex(result, pattern: "(?<![A-Za-z0-9])_([^_]+)_(?![A-Za-z0-9])") { groups in
            "<em>\(groups[1])</em>"
        }

        result = result.replacingOccurrences(of: "\n", with: "<br />")

        return result
    }

    private static func escapeHtml(_ s: String) -> String {
        s.replacingOccurrences(of: "&", with: "&amp;")
            .replacingOccurrences(of: "<", with: "&lt;")
            .replacingOccurrences(of: ">", with: "&gt;")
    }

    private static func replaceRegex(_ input: String, pattern: String, transform: ([String]) -> String) -> String {
        guard let regex = try? NSRegularExpression(pattern: pattern) else { return input }

        let mutable = NSMutableString(string: input)
        let matches = regex.matches(in: input, range: NSRange(location: 0, length: mutable.length))
        for match in matches.reversed() {
            var groups: [String] = []
            for i in 0..<match.numberOfRanges {
                let r = match.range(at: i)
                if r.location != NSNotFound {
                    groups.append(mutable.substring(with: r))
                } else {
                    groups.append("")
                }
            }
            let replacement = transform(groups)
            mutable.replaceCharacters(in: match.range, with: replacement)
        }
        return mutable as String
    }
}
