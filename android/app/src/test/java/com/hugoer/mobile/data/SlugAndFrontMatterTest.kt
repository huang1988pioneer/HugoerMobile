package com.hugoer.mobile.data

import com.hugoer.mobile.data.model.FrontMatterDocument
import com.hugoer.mobile.data.service.FrontMatterService
import com.hugoer.mobile.data.service.Slug
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SlugAndFrontMatterTest {
    @Test
    fun slugifiesTitleAndCollapsesDashes() {
        assertEquals("hello-world", Slug.fromTitle("Hello World"))
        assertEquals("my-post", Slug.fromTitle("  My   Post  "))
        assertEquals("hello-world", Slug.fromTitle("Hello, World!!!"))
        assertEquals("fallback", Slug.fromTitle("!!!", fallback = { "fallback" }))
    }

    @Test
    fun roundTripsFrontMatterFields() {
        val service = FrontMatterService()
        val original = FrontMatterService().parse(
            """
            ---
            title: "Hello"
            date: 2026-09-01T00:00:00Z
            tags: [mobile, hugo]
            draft: true
            ---

            Body text.
            """.trimIndent()
        )
        original.categories = "notes"
        val written = service.write(original)
        val parsed = service.parse(written)
        assertEquals("Hello", parsed.title)
        assertEquals("2026-09-01T00:00:00Z", parsed.date)
        assertTrue(parsed.draft)
        assertEquals("mobile, hugo", parsed.tags)
        assertEquals("notes", parsed.categories)
        assertEquals("Body text.", parsed.body.trim())
        assertFalse(written.contains("draft: \"true\""))
    }
}
