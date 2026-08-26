import SwiftUI

struct SitesView: View {
    @EnvironmentObject private var appState: AppState
    @State private var showConnectSheet = false
    @State private var showSettings = false
    @State private var siteToRemove: SiteProfile?

    var body: some View {
        NavigationStack {
            List {
                if appState.sites.isEmpty {
                    Section {
                        VStack(spacing: 12) {
                            Image(systemName: "server.rack")
                                .font(.system(size: 40))
                                .foregroundStyle(.secondary)
                            Text("尚未連結任何 GitHub 網站")
                                .font(.headline)
                            Text("點擊下方按鈕，連結你的第一個 Hugo 網站 repo。")
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                                .multilineTextAlignment(.center)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 32)
                        .listRowSeparator(.hidden)
                    }
                } else {
                    Section("已連結網站") {
                        ForEach(appState.sites) { site in
                            SiteRow(site: site, isActive: site.id == appState.activeSiteId)
                                .contentShape(Rectangle())
                                .onTapGesture {
                                    appState.setActive(site)
                                }
                                .swipeActions {
                                    Button("移除", role: .destructive) {
                                        siteToRemove = site
                                    }
                                }
                        }
                    }
                }

                Section {
                    Button {
                        showConnectSheet = true
                    } label: {
                        Label("＋ 新增網站", systemImage: "plus.circle.fill")
                            .font(.headline)
                    }
                }
            }
            .navigationTitle("站台")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        showSettings = true
                    } label: {
                        Image(systemName: "gearshape")
                    }
                }
            }
            .sheet(isPresented: $showConnectSheet) {
                NavigationStack {
                    ConnectSiteView(isPresented: $showConnectSheet)
                }
            }
            .sheet(isPresented: $showSettings) {
                NavigationStack {
                    SettingsView()
                }
            }
            .alert("移除網站", isPresented: Binding(
                get: { siteToRemove != nil },
                set: { if !$0 { siteToRemove = nil } }
            )) {
                Button("取消", role: .cancel) { siteToRemove = nil }
                Button("移除", role: .destructive) {
                    if let site = siteToRemove { appState.removeSite(site) }
                    siteToRemove = nil
                }
            } message: {
                Text("只會移除此裝置上的連結與權杖，不會刪除 GitHub 上的內容。")
            }
        }
    }
}

private struct SiteRow: View {
    let site: SiteProfile
    let isActive: Bool

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(site.fullName)
                    .font(.headline)
                Text("分支：\(site.defaultBranch)")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                if !site.pagesUrl.isEmpty {
                    Text(site.pagesUrl)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }
            }
            Spacer()
            if isActive {
                Image(systemName: "checkmark.circle.fill")
                    .foregroundStyle(.green)
                    .font(.title3)
            }
        }
        .padding(.vertical, 6)
    }
}

#Preview {
    SitesView().environmentObject(AppState())
}
