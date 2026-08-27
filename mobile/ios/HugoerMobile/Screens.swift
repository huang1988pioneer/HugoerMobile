import SwiftUI

struct OverviewView: View {
    @EnvironmentObject private var store: HugoerStore
    let onSelectArticles: () -> Void
    let onSelectDeploy: () -> Void
    let onOpenArticle: (Article) -> Void

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 20) {
                    SiteIdentity(site: store.site)
                    DispatchBoard(onSelectDeploy: onSelectDeploy)
                    QuickActions(onEdit: onSelectArticles, onPreview: {}, onPublish: onSelectDeploy)
                    SectionHeading(title: "最近活動")
                        .padding(.top, 2)
                    VStack(spacing: 0) {
                        ActivityRow(
                            title: "草稿已更新",
                            detail: "把 Hugo 站帶在身上",
                            time: "今天 09:42",
                            systemImage: "pencil.circle.fill",
                            onTap: { if let article = store.articles.first { onOpenArticle(article) } }
                        )
                        Divider().padding(.leading, 54)
                        ActivityRow(
                            title: "Pages 部署完成",
                            detail: "#184 · 41 秒",
                            time: "今天 09:48",
                            systemImage: "checkmark.circle.fill",
                            onTap: onSelectDeploy
                        )
                    }
                    .background(HugoerPalette.surface, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 28)
            }
            .navigationTitle("站點總覽")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        Task { await store.refresh() }
                    } label: {
                        if store.isRefreshing {
                            ProgressView()
                        } else {
                            Image(systemName: "arrow.triangle.2.circlepath")
                        }
                    }
                    .disabled(store.isRefreshing)
                    .accessibilityLabel("同步站點")
                }
            }
            .refreshable { await store.refresh() }
        }
    }
}

private struct DispatchBoard: View {
    @EnvironmentObject private var store: HugoerStore
    let onSelectDeploy: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(alignment: .top, spacing: 12) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("發布路線")
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(HugoerPalette.cyan)
                    Text("版本正在航行")
                        .font(.title2.bold())
                    Text(store.latestDeploymentMessage)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
                Spacer(minLength: 10)
                SitePill(label: "\(store.site.lastCommit) · main", systemImage: "chevron.left.forwardslash.chevron.right")
            }
            DispatchRail(currentIndex: 3)
            HStack(alignment: .firstTextBaseline) {
                VStack(alignment: .leading, spacing: 3) {
                    Text("GitHub Pages")
                        .font(.subheadline.weight(.semibold))
                    Text(store.site.pagesURL)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }
                Spacer()
                Button("查看記錄", action: onSelectDeploy)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(HugoerPalette.cyan)
            }
        }
        .hugoerCard()
    }
}

private struct QuickActions: View {
    let onEdit: () -> Void
    let onPreview: () -> Void
    let onPublish: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionHeading(title: "下一步")
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                Button(action: onEdit) {
                    Label("編輯文章", systemImage: "pencil")
                }
                .buttonStyle(.borderedProminent)
                .tint(HugoerPalette.cyan)
                Button(action: onPreview) {
                    Label("預覽網站", systemImage: "eye")
                }
                .buttonStyle(.bordered)
                Button(action: onPublish) {
                    Label("發布", systemImage: "paperplane.fill")
                }
                .buttonStyle(.bordered)
                .tint(HugoerPalette.amber)
                }
            }
            .labelStyle(.titleAndIcon)
            .controlSize(.regular)
        }
    }
}

struct ArticlesView: View {
    @EnvironmentObject private var store: HugoerStore
    @State private var query = ""
    @State private var filter: ArticleStatus? = nil
    let onOpenArticle: (Article) -> Void
    let onCreateArticle: () -> Void

    private var filteredArticles: [Article] {
        store.articles.filter { article in
            let matchesQuery = query.isEmpty || article.title.localizedCaseInsensitiveContains(query) || article.path.localizedCaseInsensitiveContains(query)
            let matchesFilter = filter == nil || article.status == filter
            return matchesQuery && matchesFilter
        }
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 12) {
                    Text("\(store.articles.count) 篇 · repository content")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                    HStack(spacing: 8) {
                        ForEach([ArticleStatus?](arrayLiteral: nil) + ArticleStatus.allCases.map(Optional.some), id: \.self) { option in
                            FilterChip(
                                title: option?.rawValue ?? "全部",
                                isSelected: filter == option,
                                action: { filter = option }
                            )
                        }
                    }
                    .padding(.vertical, 4)
                    SectionHeading(title: "內容清單")
                    if filteredArticles.isEmpty {
                        ContentUnavailableView(
                            "找不到符合條件的文章",
                            systemImage: "doc.text.magnifyingglass",
                            description: Text("試試看其他關鍵字，或新增一篇草稿。")
                        )
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 42)
                    } else {
                        ForEach(filteredArticles) { article in
                            ArticleRow(article: article, onTap: { onOpenArticle(article) })
                        }
                    }
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 28)
            }
            .navigationTitle("文章")
            .searchable(text: $query, placement: .navigationBarDrawer(displayMode: .always), prompt: "搜尋文章")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button(action: onCreateArticle) { Image(systemName: "plus") }
                        .accessibilityLabel("新增文章")
                }
            }
            .refreshable { await store.refresh() }
        }
    }
}

private struct FilterChip: View {
    let title: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(isSelected ? .white : .primary)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(isSelected ? HugoerPalette.cyan : HugoerPalette.surface, in: Capsule())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}

struct DeployView: View {
    @EnvironmentObject private var store: HugoerStore
    let onOpenArticle: (Article) -> Void
    @State private var showPublishConfirmation = false

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 16) {
                    VStack(alignment: .leading, spacing: 14) {
                        Label("準備發布 \(store.site.branch)", systemImage: "icloud.and.arrow.up")
                            .font(.title3.bold())
                        Text("手機端只觸發既有的 GitHub Actions，不在裝置上執行 Hugo。發布前請確認草稿內容與 repository。")
                            .font(.subheadline)
                        Button {
                            showPublishConfirmation = true
                        } label: {
                            HStack {
                                if store.isDeploying { ProgressView().tint(.white) }
                                Text(store.isDeploying ? "正在排入佇列…" : "確認並發布")
                                Spacer()
                                Image(systemName: "paperplane.fill")
                            }
                            .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(HugoerPalette.cyan)
                        .disabled(store.isDeploying)
                    }
                    .foregroundStyle(Color.primary)
                    .padding(20)
                    .background(HugoerPalette.cyan.opacity(0.18), in: RoundedRectangle(cornerRadius: 20, style: .continuous))
                    DispatchBoard(onSelectDeploy: {})
                    SectionHeading(title: "部署記錄")
                    ForEach(store.deployments) { deployment in
                        DeploymentRow(deployment: deployment)
                    }
                    SectionHeading(title: "最近變更", action: "查看文章", onAction: {
                        if let article = store.articles.first { onOpenArticle(article) }
                    })
                    if let article = store.articles.first {
                        ActivityRow(
                            title: article.title,
                            detail: "\(store.site.branch) · \(store.site.lastCommit)",
                            time: "未提交",
                            systemImage: "pencil.circle.fill",
                            onTap: { onOpenArticle(article) }
                        )
                        .background(HugoerPalette.surface, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                    }
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 28)
            }
            .navigationTitle("發布")
            .alert("發布到 GitHub Pages？", isPresented: $showPublishConfirmation) {
                Button("確認發布") {
                    Task { await store.triggerDeployment() }
                }
                Button("先不要", role: .cancel) {}
            } message: {
                Text("這會把目前 main 分支交給既有的 GitHub Actions 建置。你可以在發布分頁追蹤結果。")
            }
        }
    }
}

struct MoreView: View {
    let onAction: (String) -> Void

    var body: some View {
        NavigationStack {
            List {
                Section("站點工具") {
                    MoreActionRow(systemImage: "gearshape", title: "設定檔", detail: "網站基本欄位、params 與原始 TOML", onTap: { onAction("設定檔已加入接續清單") })
                    MoreActionRow(systemImage: "paintpalette", title: "主題", detail: "安裝或切換 Stack 等 Hugo themes", onTap: { onAction("主題管理需要桌面 Hugoer") })
                    MoreActionRow(systemImage: "list.bullet", title: "選單", detail: "編輯 menu.main、menu.social 與頁面連結", onTap: { onAction("選單編輯已加入接續清單") })
                    MoreActionRow(systemImage: "arrow.left.arrow.right", title: "遷移", detail: "Hexo／Jekyll 與 Hugo 的雙向轉換", onTap: { onAction("遷移工作會在桌面端執行") })
                }
                Section("連線") {
                    VStack(alignment: .leading, spacing: 10) {
                        Label("GitHub 連線", systemImage: "link")
                            .font(.headline)
                        Text("目前使用示範 repository adapter。接入 OAuth 後，token 只保存在系統安全儲存。")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                        Button("連接 GitHub") { onAction("GitHub OAuth 連線尚未設定") }
                            .buttonStyle(.bordered)
                    }
                    .padding(.vertical, 6)
                }
                Section("桌面接續") {
                    HStack(alignment: .top, spacing: 12) {
                        Image(systemName: "desktopcomputer")
                            .foregroundStyle(HugoerPalette.amber)
                        VStack(alignment: .leading, spacing: 5) {
                            Text("需要完整 Hugo 工具鏈？")
                                .font(.headline)
                            Text("在桌面 Hugoer 執行本機預覽、安裝 Hugo 或大量遷移。")
                                .font(.subheadline)
                        }
                    }
                    .padding(.vertical, 6)
                }
            }
            .navigationTitle("更多")
        }
    }
}

private struct MoreActionRow: View {
    let systemImage: String
    let title: String
    let detail: String
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 12) {
                Image(systemName: systemImage)
                    .frame(width: 30, height: 30)
                    .foregroundStyle(HugoerPalette.cyan)
                VStack(alignment: .leading, spacing: 3) {
                    Text(title).font(.body.weight(.medium)).foregroundStyle(.primary)
                    Text(detail).font(.subheadline).foregroundStyle(.secondary)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(.tertiary)
            }
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Original five-tab surface

/// These views intentionally mirror the original mobile information architecture:
/// a large native title, a quiet grouped background, and five persistent tabs.
struct ReferenceSitesView: View {
    @EnvironmentObject private var store: HugoerStore
    @State private var showSettings = false

    var body: some View {
        NavigationStack {
            ZStack(alignment: .bottomTrailing) {
                ScrollView {
                    VStack(alignment: .leading, spacing: 18) {
                        Text("已連結的網站")
                            .font(.title2)
                        ReferenceSiteCard(site: store.site)
                        Text("左右滑動網站列即可移除本機連結（不會刪除 GitHub 內容）。")
                            .font(.body)
                            .padding(.top, 170)
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 22)
                    .padding(.bottom, 110)
                }
                Button { } label: {
                    Image(systemName: "plus")
                        .font(.system(size: 30, weight: .regular))
                        .frame(width: 76, height: 76)
                }
                .buttonStyle(.borderedProminent)
                .tint(HugoerPalette.cyanSoft)
                .foregroundStyle(HugoerPalette.cyan)
                .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
                .padding(.trailing, 20)
                .padding(.bottom, 24)
            }
            .background(Color(uiColor: .systemGroupedBackground).ignoresSafeArea())
            .navigationTitle("站台")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button { showSettings = true } label: {
                        Image(systemName: "gearshape.fill")
                    }
                    .accessibilityLabel("設定")
                }
            }
            .alert("設定", isPresented: $showSettings) {
                Button("完成", role: .cancel) { }
            } message: {
                Text("目前網站：\(store.site.repository)")
            }
        }
    }
}

private struct ReferenceSiteCard: View {
    let site: Site

    var body: some View {
        let owner = site.repository.split(separator: "/", maxSplits: 1).first.map(String.init) ?? site.repository
        let repository = site.repository.split(separator: "/", maxSplits: 1).dropFirst().first.map(String.init) ?? site.repository
        HStack(spacing: 18) {
            Image(systemName: "cloud.fill")
                .font(.system(size: 42))
                .foregroundStyle(HugoerPalette.cyan)
            VStack(alignment: .leading, spacing: 2) {
                Text("\(owner)/")
                    .font(.title2.bold())
                    .foregroundStyle(HugoerPalette.cyan)
                Text(repository)
                    .font(.title2.bold())
                    .foregroundStyle(HugoerPalette.cyan)
                    .lineLimit(1)
                Text("目前作用中 · \(site.branch)")
                    .font(.subheadline)
                    .foregroundStyle(.primary)
            }
            Spacer(minLength: 4)
            Image(systemName: "checkmark")
                .font(.system(size: 30, weight: .semibold))
                .foregroundStyle(HugoerPalette.cyan)
        }
        .padding(24)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(HugoerPalette.cyanSoft.opacity(0.62), in: RoundedRectangle(cornerRadius: 24, style: .continuous))
    }
}

struct ReferenceConfigView: View {
    @State private var segment = 0
    @State private var baseURL = "https://fenghuanghsin.github.io/"
    @State private var title = "星空下微風輕拂黃光閃耀的世界三"
    @State private var languageCode = ""
    @State private var theme = "stack"
    let onMessage: (String) -> Void

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    HStack(spacing: 10) {
                        ReferenceSegmentButton(title: "基本", selected: segment == 0) { segment = 0 }
                        ReferenceSegmentButton(title: "Params\n表單", selected: segment == 1) { segment = 1 }
                        ReferenceSegmentButton(title: "原始\nTOML", selected: segment == 2) { segment = 2 }
                    }
                    .padding(.top, 4)
                    if segment == 0 {
                        Text("fenghuanghsin/fenghuanghsin.github.io · hugo.toml")
                            .font(.body)
                            .lineLimit(1)
                        ReferenceField("baseURL", text: $baseURL)
                        ReferenceField("title", text: $title)
                        ReferenceField("languageCode", text: $languageCode)
                        ReferenceField("theme", text: $theme)
                        Button {
                            onMessage("已儲存基本設定")
                        } label: {
                            Label("儲存基本設定", systemImage: "externaldrive.fill")
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(HugoerPalette.cyan)
                        .controlSize(.large)
                    } else if segment == 1 {
                        Text("常用 Hugo / Stack 參數").font(.title2)
                        Text("關閉開關或清空欄位後儲存，會移除該參數。")
                            .foregroundStyle(.secondary)
                        ReferenceField("description", text: .constant("桌面版 Hugoer 預設參數"))
                        Button("儲存 Params") { onMessage("已儲存 Params") }
                            .buttonStyle(.borderedProminent)
                            .tint(HugoerPalette.cyan)
                    } else {
                        Text("原始 TOML").font(.title2)
                        TextEditor(text: .constant("baseURL = \\\"https://fenghuanghsin.github.io/\\\"\ntitle = \\\"星空下微風輕拂黃光閃耀的世界三\\\"\ntheme = \\\"stack\\\""))
                            .frame(minHeight: 300)
                            .padding(8)
                            .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color.secondary.opacity(0.35)))
                        Button("儲存原始 TOML") { onMessage("已儲存原始 TOML") }
                            .buttonStyle(.borderedProminent)
                            .tint(HugoerPalette.cyan)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 28)
            }
            .background(Color(uiColor: .systemGroupedBackground).ignoresSafeArea())
            .navigationTitle("設定檔")
            .toolbar { ReferenceSettingsToolbar() }
        }
    }
}

private struct ReferenceSegmentButton: View {
    let title: String
    let selected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.title3)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity, minHeight: 68)
        }
        .buttonStyle(.borderedProminent)
        .tint(selected ? HugoerPalette.cyan : .clear)
        .foregroundStyle(selected ? Color.white : HugoerPalette.cyan)
        .overlay {
            if !selected {
                RoundedRectangle(cornerRadius: 34, style: .continuous)
                    .stroke(HugoerPalette.cyan.opacity(0.65), lineWidth: 1.5)
            }
        }
    }
}

private struct ReferenceField: View {
    let label: String
    @Binding var text: String

    init(_ label: String, text: Binding<String>) {
        self.label = label
        _text = text
    }

    var body: some View {
        TextField(label, text: $text)
            .textFieldStyle(.roundedBorder)
            .font(.body)
    }
}

struct ReferenceThemesView: View {
    @State private var currentTheme = "stack"
    let onMessage: (String) -> Void

    private let themes = [
        ReferenceTheme(name: "Stack", description: "桌面版 Hugoer 預設一鍵安裝的卡片式部落格主題，支援側欄、留言、Mermaid、圖片處理等豐富 params。", url: "https://github.com/CaiJimmy/hugo-theme-stack"),
        ReferenceTheme(name: "PaperMod", description: "簡潔快速、SEO 友善，適合技術部落格與筆記型網站。", url: "https://github.com/adityatelange/hugo-PaperMod")
    ]

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    VStack(alignment: .leading, spacing: 6) {
                        Text("目前主題").font(.title3)
                        Text(currentTheme).font(.largeTitle.bold())
                        Text("套用只會更新 theme 值；主題檔案需先存在 repo。")
                    }
                    .padding(24)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(HugoerPalette.cyanSoft.opacity(0.62), in: RoundedRectangle(cornerRadius: 24, style: .continuous))
                    ForEach(themes) { theme in
                        VStack(alignment: .leading, spacing: 12) {
                            Label(theme.name, systemImage: "paintpalette.fill")
                                .font(.title2.bold())
                                .foregroundStyle(.primary)
                            Text(theme.description).font(.body)
                            Text(theme.url).foregroundStyle(HugoerPalette.cyan)
                            Button("一鍵套用") {
                                currentTheme = theme.name == "Stack" ? "stack" : "PaperMod"
                                onMessage("已套用 \(theme.name)")
                            }
                            .buttonStyle(.bordered)
                            .tint(HugoerPalette.cyan)
                            .controlSize(.large)
                            .frame(maxWidth: .infinity)
                        }
                        .padding(24)
                        .background(HugoerPalette.surface, in: RoundedRectangle(cornerRadius: 24, style: .continuous))
                    }
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 28)
            }
            .background(Color(uiColor: .systemGroupedBackground).ignoresSafeArea())
            .navigationTitle("主題")
            .toolbar { ReferenceSettingsToolbar() }
        }
    }
}

private struct ReferenceTheme: Identifiable {
    let name: String
    let description: String
    let url: String

    var id: String { name }
}

struct ReferenceContentView: View {
    @EnvironmentObject private var store: HugoerStore
    @State private var query = ""
    let onOpenArticle: (Article) -> Void
    let onCreateArticle: () -> Void
    let onRefresh: () -> Void

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    HStack(spacing: 8) {
                        Image(systemName: "chevron.left")
                            .font(.title2)
                        VStack(alignment: .leading, spacing: 2) {
                            Text("content/post").font(.title2.bold())
                            Text("向下滑動即可重新整理").font(.subheadline)
                        }
                        Spacer()
                        Button(action: onRefresh) { Image(systemName: "arrow.clockwise").font(.title2) }
                        Button(action: onCreateArticle) { Image(systemName: "plus").font(.title2) }
                    }
                    TextField("搜尋檔案", text: $query)
                        .textFieldStyle(.roundedBorder)
                    if let article = store.articles.first(where: { query.isEmpty || $0.path.localizedCaseInsensitiveContains(query) }) {
                        Button { onOpenArticle(article) } label: {
                            HStack(spacing: 16) {
                                Image(systemName: "doc.text.fill")
                                    .font(.system(size: 36))
                                    .foregroundStyle(HugoerPalette.cyan)
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(article.path.split(separator: "/").last.map(String.init) ?? "無標題.md")
                                        .font(.title2.bold())
                                    Text("Markdown · 374 bytes").font(.body)
                                }
                                Spacer()
                                Image(systemName: "trash.fill")
                                    .font(.title2)
                                    .foregroundStyle(.red)
                            }
                            .padding(24)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(HugoerPalette.surface, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
                        }
                        .buttonStyle(.plain)
                    } else {
                        Text("找不到符合條件的檔案").foregroundStyle(.secondary)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 18)
                .padding(.bottom, 28)
            }
            .background(Color(uiColor: .systemGroupedBackground).ignoresSafeArea())
            .navigationTitle("內容")
            .toolbar { ReferenceSettingsToolbar() }
        }
    }
}

struct ReferenceGitHubView: View {
    @EnvironmentObject private var store: HugoerStore
    let onDeploy: () -> Void
    let onRefresh: () -> Void

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    HStack {
                        Text("部署與 GitHub 狀態").font(.title2)
                        Spacer()
                        Button(action: onRefresh) { Image(systemName: "arrow.clockwise").font(.title2) }
                    }
                    ReferenceStatusCard(title: "Repository 權限", value: "管理員權限", icon: "cloud.fill")
                    VStack(alignment: .leading, spacing: 12) {
                        Text("GitHub Pages").font(.title2.bold())
                        Text("網站已成功建置並上線。").font(.title3)
                        Text("建置來源：workflow")
                        Button("開啟已發佈網站") { }
                            .buttonStyle(.bordered)
                            .tint(HugoerPalette.cyan)
                            .controlSize(.large)
                            .frame(maxWidth: .infinity)
                    }
                    .padding(24)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(HugoerPalette.cyanSoft.opacity(0.62), in: RoundedRectangle(cornerRadius: 22, style: .continuous))
                    Button(action: onDeploy) {
                        Label(store.isDeploying ? "部署中…" : "立即部署", systemImage: "arrow.triangle.2.circlepath")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(HugoerPalette.cyan)
                    .controlSize(.large)
                    .disabled(store.isDeploying)
                    Text("Actions 執行紀錄").font(.title2)
                    ForEach(store.deployments) { deployment in
                        ReferenceActionCard(deployment: deployment)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 18)
                .padding(.bottom, 28)
            }
            .background(Color(uiColor: .systemGroupedBackground).ignoresSafeArea())
            .navigationTitle("GitHub")
            .toolbar { ReferenceSettingsToolbar() }
        }
    }
}

private struct ReferenceStatusCard: View {
    let title: String
    let value: String
    let icon: String

    var body: some View {
        HStack(spacing: 16) {
            Image(systemName: icon)
                .font(.system(size: 34))
                .foregroundStyle(HugoerPalette.cyan)
            VStack(alignment: .leading, spacing: 3) {
                Text(title).font(.headline)
                Text(value).font(.title2.bold())
            }
            Spacer()
        }
        .padding(24)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(HugoerPalette.surface, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
    }
}

private struct ReferenceActionCard: View {
    let deployment: Deployment

    var body: some View {
        HStack(spacing: 14) {
            Image(systemName: deployment.status == .live ? "checkmark" : "arrow.clockwise")
                .font(.title2)
                .foregroundStyle(deployment.status == .live ? HugoerPalette.cyan : .orange)
            VStack(alignment: .leading, spacing: 3) {
                Text("Deploy Hugo site to Pages").font(.headline)
                Text("\(deployment.status.rawValue) · \(deployment.time)").font(.subheadline)
            }
            Spacer()
            Image(systemName: "arrow.up.right.square")
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(HugoerPalette.surface, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
    }
}

private struct ReferenceSettingsToolbar: ToolbarContent {
    var body: some ToolbarContent {
        ToolbarItem(placement: .topBarTrailing) {
            Button { } label: {
                Image(systemName: "gearshape.fill")
            }
            .accessibilityLabel("設定")
        }
    }
}
