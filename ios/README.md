# Hugoer Mobile — iOS

原生 iOS App（Swift + SwiftUI，iOS 17+），透過 **GitHub REST API** 管理與桌面版 [Hugoer](https://github.com/huang1988pioneer/Hugoer) 相同的 Hugo 網站（同一個 GitHub repository）。手機上無法執行 `git`/`gh`/`hugo` 這些本機命令列工具，因此所有「儲存」都是透過 GitHub Contents API 直接對 repo 建立真正的 git commit；建置則交給 GitHub Actions，部署狀態透過 Pages API 查詢。

詳細產品設計請參考專案根目錄的 `DESIGN.md`；本 App 的行為是從 `legacy-maui/`（.NET MAUI 原型）依邏輯移植而來的道地 Swift 實作。

## 專案結構

```
ios/
  project.yml                    xcodegen 專案描述
  HugoerMobile/
    Info.plist
    App/HugoerMobileApp.swift    App 進入點
    Models/                      Codable/純資料模型
    Services/                    GitHubApiService、TomlLite、TomlParamsService、
                                  FrontMatterService、MiniMarkdown、MarkdownPreviewService、ArticlePreview、
                                  KeychainTokenStore、SiteProfileStore、ThemeCatalogService、ConfigLocator
    ViewModels/                  MVVM ObservableObject（async/await）
    Views/                       SwiftUI 畫面（zh-TW 介面文字）
```

## 建置方式

本機已安裝 Xcode 與 Homebrew 版 `xcodegen`：

```bash
cd ios
xcodegen generate
xcodebuild -project HugoerMobile.xcodeproj -scheme HugoerMobile \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' build
```

## GitHub Personal Access Token 權限建議

在「站台 → 新增網站」畫面連結網站時，建議使用 **fine-grained personal access token**，並僅限定目標 repository，授權下列權限：

| 權限 | 存取層級 | 用途 |
| --- | --- | --- |
| Contents | Read and write | 讀寫 `hugo.toml`、`content/` 下的文章等 |
| Actions | Read and write | 查詢與手動觸發 GitHub Actions workflow |
| Pages | Read and write | 查詢 / 啟用 GitHub Pages |
| Metadata | Read-only | GitHub API 的必要基本權限 |

App 會在驗證連線時呼叫 `GET /user` 與 `GET /repos/{owner}/{repo}`，顯示目前帳號對該 repo 的 push / admin 權限，比照桌面版 `CheckPushAccessAsync` 的使用者體驗。PAT 一律只存放於 iOS Keychain（`KeychainTokenStore`），絕不寫入 `UserDefaults` 或明碼檔案。

## 已知限制 / 與桌面版（MAUI 原型）的差異

- **TomlLite** 是手寫的極簡 TOML 讀寫器，只涵蓋 hugo.toml 實際會用到的語法（純量、字串陣列、`[section]` / `[section.sub]` 巢狀表格）；不支援 inline table、多行字串、array-of-tables、單行 dotted key 等進階語法。若原始檔案使用這些語法，「原始 TOML」分頁仍可原樣編輯文字，但「基本」與「Params 表單」分頁的解析結果可能不完整。
- **ArticlePreview** 是原生 SwiftUI 預覽器，解析常見 Markdown／Hugo 媒體形式並以 AsyncImage、AVKit、PDFKit 顯示圖片、影片與 PDF；它不是完整的 CommonMark 或 Hugo 主題渲染器（例如不支援表格、巢狀清單縮排、HTML 混排等）。
- 主題套用僅更新 `theme` 設定值並推送 commit，並不會安裝主題檔案（新增 git submodule 等）本身；App 會在套用前顯示確認提示，說明主題檔案需已存在於 repo 中。
- 內容瀏覽的搜尋為前端（client-side）依檔名過濾目前資料夾內容，不會遞迴搜尋子資料夾。
- 未實作生物辨識鎖定、推播通知等 DESIGN.md「後續可擴充方向」中列出的項目。
