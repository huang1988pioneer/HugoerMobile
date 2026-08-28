package com.hugoer.mobile.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArticlePreviewParserTest {
    @Test
    fun parsesPositionalHugoShortcodeAndMediaLinks() {
        val blocks = ArticlePreviewParser.parse(
            """
            {{< figure "images/cover.jpg" >}}

            [Watch](clips/intro.mp4)
            [Guide](files/guide.pdf)
            """.trimIndent(),
        )

        assertEquals(3, blocks.size)
        assertEquals(ArticlePreviewBlock.Image("文章圖片", "images/cover.jpg"), blocks[0])
        assertEquals(ArticlePreviewBlock.Video("clips/intro.mp4"), blocks[1])
        assertEquals(ArticlePreviewBlock.Pdf("files/guide.pdf"), blocks[2])
    }

    @Test
    fun resolvesContentRelativeMediaFromPublishedSiteRoot() {
        assertEquals(
            "https://example.com/posts/images/cover.png",
            resolvePreviewSource(
                source = "images/cover.png",
                baseUrl = "https://example.com/",
                documentPath = "content/posts/hello.md",
            ),
        )
    }

    @Test
    fun rejectsUnsupportedMediaSchemes() {
        assertEquals(
            "",
            resolvePreviewSource(
                source = "javascript:alert(1)",
                baseUrl = "https://example.com/",
                documentPath = "content/posts/hello.md",
            ),
        )
        assertTrue(ArticlePreviewParser.mediaKind("movie.webm") == PreviewMediaKind.VIDEO)
    }
}
