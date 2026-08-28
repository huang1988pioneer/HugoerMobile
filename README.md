# HugoerMobile

[Hugoer](https://github.com/huang1988pioneer/Hugoer) 桌面 Hugo 工作台的行動版副手，採用 **Android 原生 Kotlin + Jetpack Compose** 與 **iOS 原生 Swift + SwiftUI** 分別實作，透過 GitHub REST API 管理 Hugo 網站內容、設定與部署。

## 為什麼手機版長得不一樣

桌面版會呼叫本機 `hugo` / `git` / `gh` 執行檔並直接讀寫本機資料夾；這些在 iOS 上完全不可行，在 Android 上也不可靠。因此手機版定位為「Hugo 網站的隨身管理副手」：改以 **GitHub Contents / Actions / Pages REST API** 直接讀寫 repo，真正的 `hugo build` 交給 GitHub Actions 執行。

完整設計理念、資訊架構圖、桌面／手機功能對照表，請見 [`DESIGN.md`](DESIGN.md)。

## 功能（對應桌面版分頁）

| 分頁 | 能力 |
| --- | --- |
| **站台** | 連結 GitHub repo（owner/repo + PAT）、管理多個已連結網站、切換目前作用中網站 |
| **設定檔** | 基本欄位（baseURL / title / theme）、圖形化 **Params 表單**、原始 TOML 編輯，皆透過 Contents API 直接提交 |
| **主題** | 顯示目前 `theme` 設定、精選主題目錄，一鍵切換 `theme` 值並推送 |
| **內容** | 瀏覽 `content/`、新增／刪除文章、前置資料（front matter）表單 + 原生 Markdown／圖片／影片／PDF 即時預覽 |
| **GitHub** | 權限檢查、Actions 執行紀錄、手動觸發部署、GitHub Pages 狀態與啟用 |

## 技術棧

### Android 原生

- Kotlin
- Jetpack Compose + Material 3
- Android ViewModel + StateFlow
- OkHttp + Android `org.json`
- `EncryptedSharedPreferences`（Android Keystore）儲存 GitHub PAT
- Android 原生 Compose 顯示 Markdown、圖片、影片與 PDF 預覽

### iOS 原生

- Swift
- SwiftUI + `TabView`
- `ObservableObject` + `@Published` + Swift Concurrency (`async` / `await`)
- `URLSession` + `Codable`
- Keychain Services 儲存 GitHub PAT
- SwiftUI 原生顯示 Markdown、圖片、影片與 PDF 預覽

兩個原生版本都使用無第三方依賴的簡化 TOML 與 Markdown parser，並保留桌面版 Hugoer 的 Hugo/Stack 常用設定與操作流程。底部五個分頁的圖示與資訊架構以 v1.0.0 版面為基準；編輯器預覽額外支援常見 Hugo shortcode、相對媒體網址與不受信任 scheme 的安全拒絕。

## 專案結構

```
HugoerMobile/
  android/                 Android 原生 Kotlin + Jetpack Compose
    app/src/main/java/com/hugoer/mobile/
  ios/                     iOS 原生 Swift + SwiftUI
    HugoerMobile/
  legacy-maui/             第一版 .NET MAUI 原型，僅供邏輯參考
  DESIGN.md                完整產品與行動 UI 設計
```

## 建置 Android

```bash
cd android
JAVA_HOME=/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home \
ANDROID_HOME=/opt/homebrew/share/android-commandlinetools \
ANDROID_SDK_ROOT=/opt/homebrew/share/android-commandlinetools \
gradle :app:assembleDebug :app:assembleRelease
```

產物：

```
android/app/build/outputs/apk/debug/app-debug.apk
android/app/build/outputs/apk/release/app-release.apk
```

目前 release APK 使用本機測試用自簽名 keystore，適合 sideload／測試，不是 Google Play production signing key。正式發布前請改用自己的 keystore，並透過環境變數提供簽署設定。

## 建置 iOS

產生 Xcode project 並建置 Simulator 版本：

```bash
cd ios
xcodegen generate
xcodebuild build \
  -project HugoerMobile.xcodeproj \
  -scheme HugoerMobile \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' \
  -sdk iphonesimulator \
  CODE_SIGNING_ALLOWED=NO
```

目前已驗證 iOS Simulator build 成功。GitHub Actions 在有 Apple signing secrets 時產生可發佈的簽名 IPA；沒有 secrets 時仍會封裝明確標示的 unsigned IPA，供下游以自己的憑證重新簽署，不能直接安裝到實機。

正式 IPA 建置需要設定：

- Apple Developer Team ID
- Distribution certificate
- Provisioning profile
- App Store Connect／TestFlight 發布權限

## GitHub Personal Access Token 權限

建議使用 Fine-grained PAT，限定目標 repository，並授予：

- Contents：Read and write
- Actions：Read and write
- Pages：Read and write
- Metadata：Read-only

## 發布狀態

- Android signed APK：需 Android signing secrets；未設定時 Release 會附上可安裝的 QA preview-signed APK 與明確標示的 unsigned APK
- iOS native source：已提交，Simulator build 已驗證
- iOS signed IPA：需 Apple Developer signing credentials；未設定時 Release 會附上 unsigned IPA

## 手動發布

在 GitHub CLI 已登入且具備 repository `Contents: write` 權限的環境執行：

```powershell
Set-Location D:\codex\HugoerMobile
gh workflow run release.yml --repo huang1988pioneer/HugoerMobile --ref main -f version=1.1.1 -f publish=true
gh run watch --repo huang1988pioneer/HugoerMobile
```

若要從既有 tag 建置，另外傳入 `-f release_tag=v1.1.0`；此時 workflow 會以該 tag 的 commit checkout，不會誤用觸發分支。

## 授權

本專案為 [Hugoer](https://github.com/huang1988pioneer/Hugoer) 桌面工作台的配套行動應用；Hugo 與各主題請遵循其各自授權條款。
