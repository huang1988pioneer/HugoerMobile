package com.hugoer.mobile.data.service

/** URL-safe slug used when creating a new Hugo post from a title. */
object Slug {
    fun fromTitle(title: String, fallback: () -> String = { (System.currentTimeMillis() / 1000).toString() }): String {
        var slug = title.trim().lowercase().map { c ->
            when {
                c.isLetterOrDigit() -> c
                c == ' ' || c == '-' || c == '_' -> '-'
                else -> null
            }
        }.filterNotNull().joinToString("")
        while ("--" in slug) slug = slug.replace("--", "-")
        return slug.trim('-').ifEmpty { fallback() }
    }
}
