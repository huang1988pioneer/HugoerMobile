using Hugoer.Services;

// Markdown -> editor HTML -> Markdown round trips (the path the Quill editor sits on).
AssertContains(RoundTrip("# 標題\n\n段落文字"), "# 標題");
var emphasis = RoundTrip("這是 **粗體**、*斜體* 與 ~~刪除~~。");
AssertContains(emphasis, "**粗體**");
AssertContains(emphasis, "*斜體*");
AssertContains(emphasis, "~~刪除~~");
AssertContains(RoundTrip("- 蘋果\n- 香蕉\n\n1. 第一\n2. 第二"), "1. 第一");
var tasks = RoundTrip("- [ ] 未完成\n- [x] 已完成");
AssertContains(tasks, "[ ]");
AssertContains(tasks, "[x]");
AssertContains(RoundTrip("[Hugo](https://gohugo.io) 與 ![圖](cover.jpg)"), "![圖](cover.jpg)");
var code = RoundTrip("行內 `var x`\n\n```csharp\nConsole.WriteLine(1);\n```");
AssertContains(code, "`var x`");
AssertContains(code, "```csharp");
AssertContains(RoundTrip("| 欄位一 | 欄位二 |\n| --- | --- |\n| 內容 | 內容 |"), "| 欄位一 | 欄位二 |");
AssertContains(RoundTrip("上段\n\n---\n\n下段"), "---");
AssertContains(RoundTrip("開頭\n\n{{< figure src=\"a.jpg\" >}}\n\n結尾"), "{{< figure src=\"a.jpg\" >}}");

// HTML dialect emitted by the Quill editor's Delta serializer (Assets/editor/wysiwyg.html).
var quillHtml = "<h2>標題</h2><p>一行<br>換行 <strong>粗<em>斜</em></strong> <del>刪</del> <code>c</code> <a href=\"/post/x/\">連結</a></p>"
    + "<ul><li>甲<ul><li>乙</li></ul></li></ul>"
    + "<ul><li><input type=\"checkbox\"> 待辦</li><li><input type=\"checkbox\" checked> 完成</li></ul>"
    + "<pre><code class=\"language-go\">fmt.Println(\"&lt;x&gt;\")</code></pre>"
    + "<table><tbody><tr><td>H1</td><td>H2</td></tr><tr><td>a</td><td>b</td></tr></tbody></table>"
    + "<hr><p><img src=\"/image/a.png\" alt=\"a\" width=\"240\" data-hugoer-align=\"center\"></p>"
    + "<audio controls src=\"/music/a.mp3\"></audio>";
var fromQuill = MarkdownWysiwygConverter.FromEditableHtml(quillHtml);
AssertContains(fromQuill, "## 標題");
AssertContains(fromQuill, "一行  \n換行");
AssertContains(fromQuill, "**粗*斜***");
AssertContains(fromQuill, "- 甲\n  - 乙");
AssertContains(fromQuill, "- [ ] 待辦\n- [x] 完成");
AssertContains(fromQuill, "```go\nfmt.Println(\"<x>\")\n```");
AssertContains(fromQuill, "| H1 | H2 |\n| --- | --- |\n| a | b |");
AssertContains(fromQuill, "data-hugoer-align=\"center\"");
AssertContains(fromQuill, "<audio controls src=\"/music/a.mp3\"></audio>");

// The visual editor edits the body only; front matter must survive byte-for-byte.
var frontMatter = new FrontMatterService();
var yaml = "---\ntitle: Demo\ntags: [a, b]\n---\n\n舊的正文";
Assert(frontMatter.GetBody(yaml) == "舊的正文", frontMatter.GetBody(yaml));
Assert(frontMatter.ReplaceBody(yaml, "新的 **正文**") == "---\ntitle: Demo\ntags: [a, b]\n---\n\n新的 **正文**", frontMatter.ReplaceBody(yaml, "新的 **正文**"));
var toml = "+++\ntitle = \"Demo\"\ndraft = true\n+++\n\n舊的正文";
Assert(frontMatter.GetBody(toml) == "舊的正文", frontMatter.GetBody(toml));
Assert(frontMatter.ReplaceBody(toml, "新") == "+++\ntitle = \"Demo\"\ndraft = true\n+++\n\n新", frontMatter.ReplaceBody(toml, "新"));
Assert(frontMatter.ReplaceBody("沒有 front matter", "新") == "新", "Body-only files stay body-only.");

Console.WriteLine("MARKDOWN_WYSIWYG_HARNESS_OK");

static string RoundTrip(string markdown) =>
    MarkdownWysiwygConverter.FromEditableHtml(MarkdownWysiwygConverter.ToEditableHtml(markdown));

static void AssertContains(string actual, string expected)
{
    if (!actual.Contains(expected, StringComparison.Ordinal))
        throw new InvalidOperationException($"Expected to contain `{expected}` but got:\n{actual}");
}

static void Assert(bool condition, string message)
{
    if (!condition) throw new InvalidOperationException(message);
}
