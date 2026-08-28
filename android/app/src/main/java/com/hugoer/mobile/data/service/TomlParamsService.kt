package com.hugoer.mobile.data.service

import com.hugoer.mobile.data.model.ParamFieldItem
import com.hugoer.mobile.data.model.ParamFieldKind
import com.hugoer.mobile.data.toml.TomlParser
import com.hugoer.mobile.data.toml.TomlSerializer
import com.hugoer.mobile.data.toml.TomlTable

/**
 * Structured "Params 表單" editor for hugo.toml's [params] table, ported field-for-field
 * from the desktop app's TomlParamsService.cs.
 */
class TomlParamsService {

    data class KnownParam(
        val key: String,
        val label: String,
        val description: String,
        val kind: ParamFieldKind
    )

    companion object {
        /** Common Hugo + Stack theme params with labels. */
        val KNOWN_PARAMS: List<KnownParam> = listOf(
            KnownParam("description", "網站描述", "SEO / 首頁摘要", ParamFieldKind.STRING),
            KnownParam("mainSections", "主要區塊", "例如 post（逗號分隔）", ParamFieldKind.ARRAY),
            KnownParam("colorScheme.default", "色彩模式", "auto / light / dark", ParamFieldKind.STRING),
            KnownParam("colorScheme.toggle", "允許切換色彩模式", "顯示明暗模式切換", ParamFieldKind.BOOL),
            KnownParam("favicon", "網站圖示", "Stack v4 assets 路徑，例如 img/favicon.png", ParamFieldKind.STRING),
            KnownParam("footer.since", "Footer 起始年", "版權起始年份", ParamFieldKind.NUMBER),
            KnownParam("footer.customText", "Footer 文字", "自訂頁尾文字", ParamFieldKind.STRING),
            KnownParam("sidebar.subtitle", "側欄副標", "個人簡介副標", ParamFieldKind.STRING),
            KnownParam("sidebar.avatar", "側欄頭像", "Stack v4 單一 assets 路徑；空白代表停用", ParamFieldKind.STRING),
            KnownParam("article.toc", "文章目錄", "Stack v4 顯示目錄", ParamFieldKind.BOOL),
            KnownParam("article.readingTime", "閱讀時間", "Stack v4 顯示預估閱讀時間", ParamFieldKind.BOOL),
            KnownParam("article.list.showTags", "列表顯示標籤", "Stack v4 文章列表顯示 tags", ParamFieldKind.BOOL),
            KnownParam("comments.enabled", "啟用留言", "Stack v4 留言總開關", ParamFieldKind.BOOL),
            KnownParam("comments.provider", "留言服務", "giscus / utterances / waline 等", ParamFieldKind.STRING)
        )
    }

    private data class FlatValue(val kind: ParamFieldKind, val value: String, val bool: Boolean)

    fun loadParamsForm(tomlText: String): List<ParamFieldItem> {
        val result = mutableListOf<ParamFieldItem>()
        val root = tryParse(tomlText)

        val flat = LinkedHashMap<String, FlatValue>()
        val paramsTable = root?.get("params") as? TomlTable
        if (paramsTable != null) {
            flattenTable(paramsTable, "", flat)
        }
        // Case-insensitive lookup helper
        fun findFlat(key: String): FlatValue? =
            flat.entries.firstOrNull { it.key.equals(key, ignoreCase = true) }?.value

        val used = mutableSetOf<String>()
        for (p in KNOWN_PARAMS) {
            used.add(p.key.lowercase())
            val existing = findFlat(p.key)
            result.add(
                ParamFieldItem(
                    key = if (p.key.contains('.')) p.key.substringAfterLast('.') else p.key,
                    path = p.key,
                    displayName = p.label,
                    description = p.description,
                    kind = p.kind,
                    stringValue = existing?.value ?: "",
                    boolValue = existing?.bool ?: false,
                    isConfigured = existing != null,
                    isKnown = true
                )
            )
        }

        for ((path, value) in flat.entries.sortedBy { it.key.lowercase() }) {
            if (used.contains(path.lowercase())) continue
            result.add(
                ParamFieldItem(
                    key = if (path.contains('.')) path.substringAfterLast('.') else path,
                    path = path,
                    displayName = path,
                    description = "自訂參數",
                    kind = value.kind,
                    stringValue = value.value,
                    boolValue = value.bool,
                    isConfigured = true,
                    isKnown = false
                )
            )
        }

        return result
    }

    fun applyParamsToToml(tomlText: String, fields: List<ParamFieldItem>): String {
        val root = tryParse(tomlText) ?: TomlTable()

        val paramsTable = (root["params"] as? TomlTable) ?: TomlTable().also { root["params"] = it }

        for (field in fields) {
            if (field.path.isBlank()) continue
            if (!field.isEditable) continue

            if (!field.isConfigured) {
                removePath(paramsTable, field.path)
                continue
            }

            if (!field.isKnown && field.kind != ParamFieldKind.BOOL && field.stringValue.isBlank()) {
                continue
            }

            setPathValue(paramsTable, field.path, field)
        }

        return TomlSerializer.serialize(root)
    }

    fun upsertSimpleRootKeys(tomlText: String, rootKeys: Map<String, String>): String {
        val root = tryParse(tomlText) ?: TomlTable()
        for ((k, v) in rootKeys) {
            if (k.isBlank()) continue
            root[k] = v
        }
        return TomlSerializer.serialize(root)
    }

    data class BasicFields(val baseUrl: String, val title: String, val languageCode: String, val theme: String)

    fun readBasicFields(tomlText: String): BasicFields {
        val root = tryParse(tomlText) ?: TomlTable()
        return BasicFields(
            baseUrl = getString(root, "baseURL"),
            title = getString(root, "title"),
            languageCode = getString(root, "languageCode"),
            theme = getString(root, "theme")
        )
    }

    private fun getString(root: TomlTable, key: String): String =
        root[key]?.toString() ?: ""

    private fun tryParse(tomlText: String): TomlTable? {
        if (tomlText.isBlank()) return TomlTable()
        return try {
            TomlParser.parse(tomlText)
        } catch (e: Exception) {
            null
        }
    }

    private fun flattenTable(table: TomlTable, prefix: String, flat: LinkedHashMap<String, FlatValue>) {
        for ((k, v) in table) {
            val path = if (prefix.isEmpty()) k else "$prefix.$k"
            when (v) {
                is String -> flat[path] = FlatValue(ParamFieldKind.STRING, v, false)
                is Boolean -> flat[path] = FlatValue(ParamFieldKind.BOOL, if (v) "true" else "false", v)
                is Long -> flat[path] = FlatValue(ParamFieldKind.NUMBER, v.toString(), false)
                is Int -> flat[path] = FlatValue(ParamFieldKind.NUMBER, v.toString(), false)
                is Double -> flat[path] = FlatValue(ParamFieldKind.NUMBER, v.toString(), false)
                is List<*> -> {
                    if (v.all { it !is TomlTable }) {
                        flat[path] = FlatValue(ParamFieldKind.ARRAY, v.joinToString(", ") { formatScalar(it) }, false)
                    } else {
                        flat[path] = FlatValue(ParamFieldKind.NESTED, "複雜物件陣列；請用「原始 TOML」編輯", false)
                    }
                }
                is TomlTable -> flattenTable(v, path, flat)
                null -> {}
                else -> flat[path] = FlatValue(ParamFieldKind.STRING, v.toString(), false)
            }
        }
    }

    private fun formatScalar(o: Any?): String = when (o) {
        null -> ""
        is String -> o
        is Boolean -> if (o) "true" else "false"
        else -> o.toString()
    }

    private fun setPathValue(rootParams: TomlTable, path: String, field: ParamFieldItem) {
        val parts = path.split('.').map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.isEmpty()) return

        var current = rootParams
        for (i in 0 until parts.size - 1) {
            val next = current[parts[i]]
            current = if (next is TomlTable) next else {
                val newTable = TomlTable()
                current[parts[i]] = newTable
                newTable
            }
        }

        val leaf = parts.last()
        current[leaf] = when (field.kind) {
            ParamFieldKind.BOOL -> field.boolValue
            ParamFieldKind.NUMBER -> field.stringValue.toLongOrNull()
                ?: field.stringValue.toDoubleOrNull()
                ?: field.stringValue
            ParamFieldKind.ARRAY -> parseArray(field.stringValue)
            else -> field.stringValue
        }
    }

    private fun removePath(root: TomlTable, path: String) {
        val parts = path.split('.').map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.isEmpty()) return

        val parents = mutableListOf<Pair<TomlTable, String>>()
        var current = root
        for (i in 0 until parts.size - 1) {
            val value = current[parts[i]]
            if (value !is TomlTable) return
            parents.add(current to parts[i])
            current = value
        }

        current.remove(parts.last())
        for (i in parents.indices.reversed()) {
            val (parent, key) = parents[i]
            val table = parent[key]
            if (table is TomlTable && table.isEmpty()) {
                parent.remove(key)
            } else {
                break
            }
        }
    }

    private fun parseArray(value: String): List<String> {
        if (value.isBlank()) return emptyList()
        return value.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    }
}
