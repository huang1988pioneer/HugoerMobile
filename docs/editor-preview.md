# 編輯器

## Markdown 富文字編輯（Quill）

Android 與 iOS 的「編輯」模式共用 [`shared/web/editor/`](../shared/web/editor) 的 [Quill](https://quilljs.com/) 2 編輯器，以所見即所得方式編輯內文，儲存時仍寫回 Markdown：

- 載入：`marked` lexer 把 Markdown 直接轉成 Quill Delta（不經 HTML）；儲存：Delta 直接序列化回 Markdown。轉換邏輯在 `markdown-delta.js`，測試在 `shared/tests/`（`node --test shared/tests/*.test.cjs`）。
- 支援標題、粗體、斜體、刪除線、行內程式碼、連結、圖片、項目／編號／待辦清單（含巢狀）、引言、程式碼區塊（保留語言）與分隔線；工具列另有復原／重做與插入原始 Markdown。
- Quill 無法表示的內容（Hugo shortcode、表格、HTML、參考連結定義、多段落清單、含清單的引言等）會以「原始碼」區塊原封保留，點一下即可編輯或刪除，儲存時逐字寫回。
- 只有使用者實際編輯後才會重新序列化；單純開啟再儲存不會改動內文。編輯後，軟換行會合併為同一行，`*` 清單記號會正規化為 `-`，寬鬆清單會變成緊湊清單。
- 需要逐字控制時，可切換到「Markdown」原始碼模式（iOS 分段選單；Android 頂列 `</>` 按鈕）。
- 編輯器頁面與函式庫都打包在 App 內（iOS 以 `hugoer-editor://` scheme、Android 以 `WebViewAssetLoader` 提供），有 CSP 限制、不載入任何遠端腳本，並阻擋所有頁面導覽；遠端圖片僅以 HTTPS 顯示。

## 預覽

Android 與 iOS 的文章編輯器都使用原生預覽元件，避免在 WebView 中執行未信任的文章 HTML。預覽會處理：

- Markdown 標題、段落、引言、清單與 fenced code block
- Markdown 圖片／連結，以及 `<img>`、`<video>`、`<iframe>` 等常見媒體標記
- Hugo `figure`、`image`、`img`、`video`、`pdf`，以及常見自訂 `embed-image`／`embed-video`／`embed-pdf` shortcode（含第一個引號位置參數與 `src`／`url`／`data`／`file` 屬性）
- 圖片、影片與 PDF 的 data URL、遠端 URL，以及相對於網站與文章路徑的媒體 URL

相對網址會以設定檔的 `baseURL`（沒有 scheme 時預設為 HTTPS）解析；只允許 `http`、`https`、`file`（Android 另支援 `content`）scheme，`javascript:` 等不受支援的 scheme 會顯示為不可用媒體。網路載入在背景執行，失敗時可針對單一媒體重試；Android 會對大圖取樣，避免預覽時耗盡記憶體。

這是單篇文章的快速檢查，不是完整 Hugo 主題渲染；真正的網站輸出仍由 GitHub Actions 的 Hugo build 產生。
