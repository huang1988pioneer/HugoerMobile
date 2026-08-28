package com.hugoer.mobile.data.toml

/**
 * A minimal, dependency-free TOML reader/writer sufficient for hugo.toml: flat + nested
 * `[section.subsection]` tables, string/bool/int/double/string-array values. This is a
 * pragmatic subset of the TOML spec (not a full implementation), replacing the desktop
 * app's use of the Tomlyn library.
 *
 * Values are represented as: String, Boolean, Long, Double, List<Any?> (flat scalar array),
 * or LinkedHashMap<String, Any?> (nested table).
 */
class TomlTable : LinkedHashMap<String, Any?>()

object TomlParser {

    fun parse(text: String): TomlTable {
        val root = TomlTable()
        var current: TomlTable = root

        val lines = preprocessLines(text)
        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) continue

            if (line.startsWith("[")) {
                val headerEnd = line.indexOf(']')
                if (headerEnd < 0) continue
                val header = line.substring(1, headerEnd).trim()
                current = navigateCreate(root, header)
                continue
            }

            val eq = findTopLevelEquals(line) ?: continue
            val rawKey = line.substring(0, eq).trim()
            val rawValue = line.substring(eq + 1).trim()
            val key = unquoteKey(rawKey)
            val value = parseValue(rawValue)

            if (key.contains('.')) {
                val parts = key.split('.').map { it.trim() }.filter { it.isNotEmpty() }
                var table = current
                for (i in 0 until parts.size - 1) {
                    val next = table[parts[i]]
                    table = if (next is TomlTable) next else {
                        val newTable = TomlTable()
                        table[parts[i]] = newTable
                        newTable
                    }
                }
                table[parts.last()] = value
            } else {
                current[key] = value
            }
        }

        return root
    }

    /** Joins lines so that multi-line arrays (`[` ... `]` spanning lines) become one logical line. */
    private fun preprocessLines(text: String): List<String> {
        val result = mutableListOf<String>()
        val rawLines = text.replace("\r\n", "\n").split("\n")
        var i = 0
        while (i < rawLines.size) {
            var line = rawLines[i]
            val trimmed = line.trim()
            if (!trimmed.startsWith("#") && !trimmed.startsWith("[") &&
                trimmed.contains('=') && countUnmatched(line, '[', ']') > 0
            ) {
                val sb = StringBuilder(line)
                var depth = countUnmatched(line, '[', ']')
                i++
                while (depth > 0 && i < rawLines.size) {
                    sb.append(" ").append(rawLines[i])
                    depth += countUnmatched(rawLines[i], '[', ']')
                    i++
                }
                result.add(sb.toString())
                continue
            }
            result.add(line)
            i++
        }
        return result
    }

    private fun countUnmatched(line: String, open: Char, close: Char): Int {
        var depth = 0
        var inQuotes = false
        var quoteChar = ' '
        for (c in line) {
            if (inQuotes) {
                if (c == quoteChar) inQuotes = false
            } else when (c) {
                '"', '\'' -> { inQuotes = true; quoteChar = c }
                open -> depth++
                close -> depth--
            }
        }
        return depth
    }

    private fun navigateCreate(root: TomlTable, header: String): TomlTable {
        val parts = header.split('.').map { unquoteKey(it.trim()) }.filter { it.isNotEmpty() }
        var current = root
        for (part in parts) {
            val next = current[part]
            current = if (next is TomlTable) next else {
                val newTable = TomlTable()
                current[part] = newTable
                newTable
            }
        }
        return current
    }

    private fun unquoteKey(key: String): String {
        var k = key.trim()
        if (k.length >= 2 && ((k.first() == '"' && k.last() == '"') || (k.first() == '\'' && k.last() == '\''))) {
            k = k.substring(1, k.length - 1)
        }
        return k
    }

    private fun findTopLevelEquals(line: String): Int? {
        var inQuotes = false
        var quoteChar = ' '
        for (i in line.indices) {
            val c = line[i]
            if (inQuotes) {
                if (c == quoteChar) inQuotes = false
            } else when (c) {
                '"', '\'' -> { inQuotes = true; quoteChar = c }
                '=' -> return i
            }
        }
        return null
    }

    private fun parseValue(raw: String): Any? {
        var value = raw.trim()
        // strip trailing inline comment (naive: only if not inside quotes)
        value = stripTrailingComment(value)

        if (value.isEmpty()) return ""

        if (value.startsWith("[") && value.endsWith("]")) {
            val inner = value.substring(1, value.length - 1)
            return splitArrayItems(inner).map { parseScalar(it.trim()) }
        }

        return parseScalar(value)
    }

    private fun stripTrailingComment(value: String): String {
        var inQuotes = false
        var quoteChar = ' '
        for (i in value.indices) {
            val c = value[i]
            if (inQuotes) {
                if (c == quoteChar) inQuotes = false
            } else when {
                c == '"' || c == '\'' -> { inQuotes = true; quoteChar = c }
                c == '#' -> return value.substring(0, i).trim()
            }
        }
        return value
    }

    private fun splitArrayItems(inner: String): List<String> {
        val items = mutableListOf<String>()
        var depth = 0
        var inQuotes = false
        var quoteChar = ' '
        val sb = StringBuilder()
        for (c in inner) {
            when {
                inQuotes -> {
                    sb.append(c)
                    if (c == quoteChar) inQuotes = false
                }
                c == '"' || c == '\'' -> { inQuotes = true; quoteChar = c; sb.append(c) }
                c == '[' -> { depth++; sb.append(c) }
                c == ']' -> { depth--; sb.append(c) }
                c == ',' && depth == 0 -> { items.add(sb.toString()); sb.clear() }
                else -> sb.append(c)
            }
        }
        if (sb.toString().isNotBlank()) items.add(sb.toString())
        return items
    }

    private fun parseScalar(raw: String): Any? {
        val value = raw.trim()
        if (value.isEmpty()) return ""

        if (value.length >= 2 && value.first() == '"' && value.last() == '"') {
            return unescapeString(value.substring(1, value.length - 1))
        }
        if (value.length >= 2 && value.first() == '\'' && value.last() == '\'') {
            return value.substring(1, value.length - 1)
        }
        if (value == "true") return true
        if (value == "false") return false

        value.toLongOrNull()?.let { return it }
        value.toDoubleOrNull()?.let { return it }

        // Bare/unquoted string fallback (lenient beyond strict TOML).
        return value
    }

    private fun unescapeString(value: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < value.length) {
            val c = value[i]
            if (c == '\\' && i + 1 < value.length) {
                when (value[i + 1]) {
                    'n' -> { sb.append('\n'); i += 2 }
                    't' -> { sb.append('\t'); i += 2 }
                    'r' -> { sb.append('\r'); i += 2 }
                    '"' -> { sb.append('"'); i += 2 }
                    '\\' -> { sb.append('\\'); i += 2 }
                    else -> { sb.append(c); i++ }
                }
            } else {
                sb.append(c); i++
            }
        }
        return sb.toString()
    }
}

object TomlSerializer {

    fun serialize(root: TomlTable): String {
        val sb = StringBuilder()
        writeTable(sb, root, "")
        return sb.toString().trimEnd('\n') + "\n"
    }

    private fun writeTable(sb: StringBuilder, table: TomlTable, prefix: String) {
        val simpleEntries = table.entries.filter { it.value !is TomlTable }
        for ((key, value) in simpleEntries) {
            sb.append(quoteKeyIfNeeded(key)).append(" = ").append(formatValue(value)).append("\n")
        }
        if (simpleEntries.isNotEmpty()) sb.append("\n")

        val tableEntries = table.entries.filter { it.value is TomlTable }
        for ((key, value) in tableEntries) {
            val path = if (prefix.isEmpty()) key else "$prefix.$key"
            @Suppress("UNCHECKED_CAST")
            val nested = value as TomlTable
            sb.append("[").append(path).append("]\n")
            writeTable(sb, nested, path)
        }
    }

    private fun quoteKeyIfNeeded(key: String): String {
        val plainKeyRegex = Regex("^[A-Za-z0-9_-]+$")
        return if (plainKeyRegex.matches(key)) key else "\"${escapeString(key)}\""
    }

    private fun formatValue(value: Any?): String = when (value) {
        null -> "\"\""
        is String -> "\"${escapeString(value)}\""
        is Boolean -> if (value) "true" else "false"
        is Long -> value.toString()
        is Int -> value.toString()
        is Double -> formatDouble(value)
        is List<*> -> "[" + value.joinToString(", ") { formatScalar(it) } + "]"
        else -> "\"${escapeString(value.toString())}\""
    }

    private fun formatScalar(value: Any?): String = when (value) {
        null -> "\"\""
        is String -> "\"${escapeString(value)}\""
        is Boolean -> if (value) "true" else "false"
        is Long -> value.toString()
        is Int -> value.toString()
        is Double -> formatDouble(value)
        else -> "\"${escapeString(value.toString())}\""
    }

    private fun formatDouble(d: Double): String =
        if (d == d.toLong().toDouble()) d.toLong().toString() + ".0" else d.toString()

    private fun escapeString(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "")
}
