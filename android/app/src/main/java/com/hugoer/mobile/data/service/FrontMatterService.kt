package com.hugoer.mobile.data.service

import com.hugoer.mobile.data.model.FrontMatterDocument

/**
 * Handles the YAML front matter Hugo uses by default. Ported from the desktop app
 * almost verbatim — pure string processing, no OS dependency.
 */
class FrontMatterService {

    private val yamlBlockRegex = Regex(
        "\\A---\\s*\\r?\\n(?<frontMatter>[\\s\\S]*?)\\r?\\n---\\s*(?:\\r?\\n|\\z)"
    )

    fun parse(text: String): FrontMatterDocument {
        val safeText = text
        val match = yamlBlockRegex.find(safeText)
            ?: return FrontMatterDocument(body = safeText)

        val fields = LinkedHashMap<String, String>()
        val frontMatterBlock = match.groups["frontMatter"]?.value ?: ""
        for (line in frontMatterBlock.split("\n")) {
            val separator = line.indexOf(':')
            if (separator <= 0 || line.trimStart().startsWith("#")) continue

            val key = line.substring(0, separator).trim()
            val value = line.substring(separator + 1).trim()
            fields[key] = unquote(value)
        }

        val body = safeText.substring(match.range.last + 1).trimStart('\r', '\n')
        return FrontMatterDocument(fields, body)
    }

    fun write(document: FrontMatterDocument): String {
        val fields = document.fields
        val orderedKeys = listOf("title", "date", "slug", "categories", "tags", "image", "description", "draft")
        val emitted = mutableSetOf<String>()
        val output = StringBuilder("---\n")

        for (key in orderedKeys) {
            appendField(output, fields, key, emitted)
        }

        for (key in fields.keys.sortedBy { it.lowercase() }) {
            appendField(output, fields, key, emitted)
        }

        output.append("---\n\n")
        output.append(document.body.trimStart('\r', '\n'))
        return output.toString()
    }

    private fun appendField(
        output: StringBuilder,
        fields: Map<String, String>,
        key: String,
        emitted: MutableSet<String>
    ) {
        val existingKey = fields.keys.firstOrNull { it.equals(key, ignoreCase = true) } ?: return
        val value = fields[existingKey]
        if (value.isNullOrBlank() || !emitted.add(existingKey.lowercase())) return

        if (key.equals("draft", ignoreCase = true)) {
            output.append("draft: ${value.lowercase()}\n")
            return
        }

        if (key.equals("categories", ignoreCase = true) || key.equals("tags", ignoreCase = true)) {
            val values = value.split(',').map { it.trim() }.filter { it.isNotEmpty() }.map { quote(it) }
            output.append("$key: [${values.joinToString(", ")}]\n")
            return
        }

        if (key.equals("date", ignoreCase = true) && looksLikeDate(value)) {
            output.append("date: $value\n")
            return
        }

        output.append("$key: ${quote(value)}\n")
    }

    private fun looksLikeDate(value: String): Boolean {
        return Regex("^\\d{4}-\\d{2}-\\d{2}").containsMatchIn(value)
    }

    private fun quote(value: String): String {
        if (value.startsWith('[') && value.endsWith(']')) return value
        return "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
    }

    private fun unquote(raw: String): String {
        val value = raw.trim()
        if (value.startsWith('[') && value.endsWith(']')) {
            return value.substring(1, value.length - 1)
                .split(',')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .joinToString(", ") { unquote(it) }
        }
        if (value.length >= 2 && ((value.first() == '"' && value.last() == '"') || (value.first() == '\'' && value.last() == '\''))) {
            return value.substring(1, value.length - 1).replace("\\\"", "\"").replace("\\\\", "\\")
        }
        return value
    }
}
