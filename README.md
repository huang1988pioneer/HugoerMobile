# HugoerMobile

[Hugoer](https://github.com/huang1988pioneer/Hugoer) 桌面 Hugo 工作台的行動版副手 — 以 **.NET MAUI** 打造，可同時建置 **Android** 與 **iOS**，透過 GitHub REST API 管理 Hugo 網站的內容、設定與部署，介面依行動裝置操作習慣重新設計。

## 為什麼手機版長得不一樣

桌面版會呼叫本機 `hugo` / `git` / `gh` 執行檔並直接讀寫本機資料夾；這些在 iOS 上完全不可行，在 Android 上也不可靠。因此手機版定位為「Hugo 網站的隨身管理副手」：改以 **GitHub Contents / Actions / Pages REST API** 直接讀寫 repo，真正的 `hugo build` 交給桌面版已內建的 GitHub Actions workflow 執行。

完整設計理念、資訊架構圖、桌面／手機功能對照表，請見 [`DESIGN.md`](DESIGN.md)。

## 功能（對應桌面版分頁）

| 分頁 | 能力 |
| --- | --- |
| **站台** | 連結 GitHub repo（owner/repo + PAT）、管理多個已連結網站、切換目前作用中網站 |
| **設定檔** | 基本欄位（baseURL / title / theme）、圖形化 **Params 表單**、原始 TOML 編輯，皆透過 Contents API 直接提交 |
| **主題** | 顯示目前 `theme` 設定、精選主題目錄，一鍵切換 `theme` 值並推送 |
| **內容** | 瀏覽 `content/`、新增／刪除文章、前置資料（front matter）表單 + Markdown 即時預覽 |
| **GitHub** | 權限檢查、Actions 執行紀錄、手動觸發部署、GitHub Pages 狀態與啟用 |

## 技術棧

- **.NET MAUI**（`net10.0-android` / `net10.0-ios`），單一 C# 專案
- `CommunityToolkit.Mvvm`、`CommunityToolkit.Maui`
- `Markdig`（文章即時預覽）、`Tomlyn`（TOML 設定，與桌面版共用邏輯）
- GitHub REST API + `SecureStorage`（PAT 安全儲存於 Keychain / Keystore）

## 專案結構

```
HugoerMobile/
  HugoerMobile.csproj
  MauiProgram.cs / App.xaml / AppShell.xaml
  Platforms/Android, Platforms/iOS
  Models/        資料模型
  Services/      GitHubApiService、TomlParamsService、FrontMatterService、MarkdownPreviewService…
  ViewModels/    CommunityToolkit.Mvvm ObservableObject + RelayCommand
  Views/         Shell 分頁與子頁面 XAML
  Converters/    XAML 值轉換器
  Resources/     圖示、樣式（Colors.xaml / Styles.xaml）
```

## 建置

```bash
dotnet workload install maui
cd HugoerMobile
dotnet build -f net10.0-android
# iOS 需在 macOS 安裝 Xcode
dotnet build -f net10.0-ios
```

## GitHub Personal Access Token 權限

建議使用 Fine-grained PAT，限定目標 repository，並授予：

- Contents：Read and write
- Actions：Read and write
- Pages：Read and write
- Metadata：Read-only

## 授權

本專案為 [Hugoer](https://github.com/huang1988pioneer/Hugoer) 桌面工作台的配套行動應用；Hugo 與各主題請遵循其各自授權條款。
