# 編輯器預覽

Android 與 iOS 的文章編輯器都使用原生預覽元件，避免在 WebView 中執行未信任的文章 HTML。預覽會處理：

- Markdown 標題、段落、引言、清單與 fenced code block
- Markdown 圖片／連結，以及 `<img>`、`<video>`、`<iframe>` 等常見媒體標記
- Hugo `figure`、`image`、`img`、`video`、`pdf`，以及常見自訂 `embed-image`／`embed-video`／`embed-pdf` shortcode（含第一個引號位置參數與 `src`／`url`／`data`／`file` 屬性）
- 圖片、影片與 PDF 的 data URL、遠端 URL，以及相對於網站與文章路徑的媒體 URL

相對網址會以設定檔的 `baseURL`（沒有 scheme 時預設為 HTTPS）解析；只允許 `http`、`https`、`file`（Android 另支援 `content`）scheme，`javascript:` 等不受支援的 scheme 會顯示為不可用媒體。網路載入在背景執行，失敗時可針對單一媒體重試；Android 會對大圖取樣，避免預覽時耗盡記憶體。

這是單篇文章的快速檢查，不是完整 Hugo 主題渲染；真正的網站輸出仍由 GitHub Actions 的 Hugo build 產生。
