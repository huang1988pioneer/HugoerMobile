import SwiftUI

struct SettingsView: View {
    @EnvironmentObject private var appState: AppState
    @Environment(\.dismiss) private var dismiss
    @State private var showClearConfirm = false

    var body: some View {
        Form {
            Section("已連結網站") {
                if appState.sites.isEmpty {
                    Text("尚未連結任何網站")
                        .foregroundStyle(.secondary)
                } else {
                    ForEach(appState.sites) { site in
                        VStack(alignment: .leading) {
                            Text(site.fullName).font(.headline)
                            Text(site.pagesUrl).font(.caption).foregroundStyle(.secondary)
                        }
                    }
                }
            }

            Section {
                Button(role: .destructive) {
                    showClearConfirm = true
                } label: {
                    Text("清除所有已連結網站與權杖")
                }
            }

            Section("關於") {
                LabeledContent("App 版本", value: appVersion)
                Text("Hugoer Mobile 是桌面版 Hugoer 的行動副手，透過 GitHub REST API 管理你的 Hugo 網站內容、設定檔、主題與部署狀態。")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
        }
        .navigationTitle("設定")
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("關閉") { dismiss() }
            }
        }
        .alert("確定要清除所有網站與權杖嗎？", isPresented: $showClearConfirm) {
            Button("取消", role: .cancel) {}
            Button("清除", role: .destructive) {
                appState.clearAll()
            }
        } message: {
            Text("此操作將移除所有已連結的網站設定與 Keychain 中儲存的 GitHub 權杖，且無法復原。")
        }
    }

    private var appVersion: String {
        let version = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0"
        let build = Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "1"
        return "\(version) (\(build))"
    }
}

#Preview {
    NavigationStack {
        SettingsView()
    }
    .environmentObject(AppState())
}
