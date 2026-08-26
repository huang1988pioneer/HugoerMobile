import SwiftUI

struct ThemesView: View {
    @EnvironmentObject private var appState: AppState
    @StateObject private var viewModel = ThemesViewModel()
    @Environment(\.openURL) private var openURL
    @State private var pendingTheme: ThemeInfo?
    @State private var showApplyConfirm = false

    var body: some View {
        NavigationStack {
            Group {
                if let site = appState.activeSite {
                    content(for: site)
                } else {
                    NoActiveSiteView()
                }
            }
            .navigationTitle("主題")
        }
    }

    @ViewBuilder
    private func content(for site: SiteProfile) -> some View {
        List {
            Section("目前設定") {
                if viewModel.isLoading {
                    ProgressView()
                } else {
                    LabeledContent("theme", value: viewModel.currentTheme.isEmpty ? "（未設定）" : viewModel.currentTheme)
                }
                if let error = viewModel.errorMessage {
                    Text(error).foregroundStyle(.red)
                }
                if let success = viewModel.successMessage {
                    Text(success).foregroundStyle(.green)
                }
            }

            Section("精選主題目錄") {
                ForEach(viewModel.catalog) { theme in
                    ThemeCard(theme: theme, isCurrent: theme.themeConfigValue == viewModel.currentTheme) {
                        pendingTheme = theme
                        showApplyConfirm = true
                    } onViewSource: {
                        if let url = URL(string: theme.repoUrl) {
                            openURL(url)
                        }
                    }
                }
            }
        }
        .refreshable {
            if let token = appState.token(for: site) {
                await viewModel.load(site: site, token: token)
            }
        }
        .task(id: site.id) {
            if let token = appState.token(for: site) {
                await viewModel.load(site: site, token: token)
            }
        }
        .alert("套用主題「\(pendingTheme?.name ?? "")」？", isPresented: $showApplyConfirm) {
            Button("取消", role: .cancel) {}
            Button("套用") {
                if let theme = pendingTheme {
                    Task { await viewModel.apply(theme: theme) }
                }
            }
        } message: {
            Text("這只會更新 hugo.toml 的 theme 設定值，請確認 repo 內已存在對應的主題檔案（例如已安裝為 git submodule 或已放入 themes/ 資料夾），否則網站建置會失敗。")
        }
    }
}

private struct ThemeCard: View {
    let theme: ThemeInfo
    let isCurrent: Bool
    let onApply: () -> Void
    let onViewSource: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(theme.name).font(.headline)
                if isCurrent {
                    Text("使用中")
                        .font(.caption2)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 2)
                        .background(Color.green.opacity(0.2))
                        .foregroundStyle(.green)
                        .clipShape(Capsule())
                }
            }
            Text(theme.description)
                .font(.subheadline)
                .foregroundStyle(.secondary)

            HStack {
                Button("查看原始碼", action: onViewSource)
                    .buttonStyle(.bordered)
                Spacer()
                Button("套用", action: onApply)
                    .buttonStyle(.borderedProminent)
                    .disabled(isCurrent)
            }
        }
        .padding(.vertical, 8)
    }
}

#Preview {
    ThemesView().environmentObject(AppState())
}
