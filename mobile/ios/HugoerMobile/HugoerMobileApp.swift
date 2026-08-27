import SwiftUI

@main
@MainActor
struct HugoerMobileApp: App {
    @StateObject private var store = HugoerStore()
    @State private var selection = 0
    @State private var editorArticle: Article?
    @State private var toastMessage: String?

    var body: some Scene {
        WindowGroup {
            TabView(selection: $selection) {
                ReferenceSitesView()
                .tabItem { Label("站台", systemImage: "cloud.fill") }
                .tag(0)

                ReferenceConfigView(onMessage: { toastMessage = $0 })
                .tabItem { Label("設定檔", systemImage: "slider.horizontal.3") }
                .tag(1)

                ReferenceThemesView(onMessage: { toastMessage = $0 })
                    .tabItem { Label("主題", systemImage: "paintpalette.fill") }
                    .tag(2)

                ReferenceContentView(
                    onOpenArticle: { editorArticle = $0 },
                    onCreateArticle: { editorArticle = store.createArticle() },
                    onRefresh: { Task { await store.refresh() } }
                )
                    .tabItem { Label("內容", systemImage: "doc.text.fill") }
                    .tag(3)

                ReferenceGitHubView(
                    onDeploy: { Task { await store.triggerDeployment() } },
                    onRefresh: { Task { await store.refresh() } }
                )
                    .tabItem { Label("GitHub", systemImage: "arrow.triangle.2.circlepath") }
                    .tag(4)
            }
            .tint(HugoerPalette.cyan)
            .environmentObject(store)
            .sheet(item: $editorArticle) { article in
                ArticleEditorView(article: article, previewBaseURL: store.site.pagesURL) { title, body in
                    if store.save(articleID: article.id, title: title, body: body) {
                        toastMessage = "草稿已儲存"
                    }
                }
            }
            .overlay(alignment: .top) {
                if let toastMessage {
                    Text(toastMessage)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(.primary)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 11)
                        .background(.regularMaterial, in: Capsule())
                        .padding(.top, 8)
                        .transition(.move(edge: .top).combined(with: .opacity))
                        .task {
                            try? await Task.sleep(for: .seconds(2))
                            withAnimation { self.toastMessage = nil }
                        }
                }
            }
            .alert(
                "操作未完成",
                isPresented: Binding(
                    get: { store.errorMessage != nil },
                    set: { if !$0 { store.clearError() } }
                )
            ) {
                Button("知道了", role: .cancel) { store.clearError() }
            } message: {
                Text(store.errorMessage ?? "請稍後重試。")
            }
        }
    }
}
