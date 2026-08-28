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
    fun parsesEmbedShortcodesUsedByPublishedHugoSites() {
        val blocks = ArticlePreviewParser.parse(
            """
            {{< embed-video src="/videos/demo.mp4" width="100%" >}}

            ![](/images/demo.png)

            {{< embed-pdf url="/pdfs/guide.pdf" >}}
            """.trimIndent(),
        )

        assertEquals(3, blocks.size)
        assertEquals(ArticlePreviewBlock.Video("/videos/demo.mp4"), blocks[0])
        assertEquals(ArticlePreviewBlock.Image("", "/images/demo.png"), blocks[1])
        assertEquals(ArticlePreviewBlock.Pdf("/pdfs/guide.pdf"), blocks[2])
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
    fun resolvesRootRelativeMediaFromUserPagesSite() {
        assertEquals(
            "https://fenghuanghsin.github.io/images/demo.png",
            resolvePreviewSource(
                source = "/images/demo.png",
                baseUrl = "https://fenghuanghsin.github.io/",
                documentPath = "content/post/無標題.md",
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
