import SwiftUI

struct GitHubView: View {
    @EnvironmentObject private var appState: AppState
    @StateObject private var viewModel = GitHubViewModel()
    @Environment(\.openURL) private var openURL

    var body: some View {
        NavigationStack {
            Group {
                if let site = appState.activeSite {
                    content(for: site)
                } else {
                    NoActiveSiteView()
                }
            }
            .navigationTitle("GitHub")
        }
    }

    @ViewBuilder
    private func content(for site: SiteProfile) -> some View {
        List {
            if viewModel.isLoading {
                ProgressView()
            }

            if let error = viewModel.errorMessage {
                Text(error).foregroundStyle(.red)
            }
            if let success = viewModel.successMessage {
                Text(success).foregroundStyle(.green)
            }

            if let repo = viewModel.repoInfo {
                Section("儲存庫權限") {
                    LabeledContent("推送", value: repo.permissions.push ? "有" : "無")
                    LabeledContent("管理", value: repo.permissions.admin ? "有" : "無")
                    LabeledContent("預設分支", value: repo.defaultBranch)
                }
            }

            Section("GitHub Pages") {
                if let pages = viewModel.pagesStatus {
                    HStack {
                        Circle()
                            .fill(pagesColor(pages))
                            .frame(width: 12, height: 12)
                        Text(pages.message)
                    }
                    if let htmlUrl = pages.htmlUrl {
                        Text(htmlUrl).font(.caption).foregroundStyle(.secondary)
                    }
                    if !pages.enabled {
                        Button {
                            Task { await viewModel.enablePages() }
                        } label: {
                            if viewModel.isEnablingPages {
                                ProgressView()
                            } else {
                                Text("啟用 Pages")
                            }
                        }
                    }
                } else {
                    Text("尚未讀取 Pages 狀態")
                        .foregroundStyle(.secondary)
                }
            }

            Section {
                Button {
                    Task { await viewModel.deployNow() }
                } label: {
                    if viewModel.isDeploying {
                        ProgressView().frame(maxWidth: .infinity)
                    } else {
                        Label("🚀 立即部署", systemImage: "bolt.fill")
                            .font(.headline)
                            .frame(maxWidth: .infinity)
                    }
                }
                .disabled(viewModel.isDeploying)

                if !site.pagesUrl.isEmpty, let url = URL(string: site.pagesUrl) {
                    Button("開啟網站") {
                        openURL(url)
                    }
                }
            }

            Section("最近的執行紀錄") {
                if viewModel.workflowRuns.isEmpty {
                    Text("尚無執行紀錄")
                        .foregroundStyle(.secondary)
                } else {
                    ForEach(viewModel.workflowRuns) { run in
                        Button {
                            if let url = URL(string: run.htmlUrl) {
                                openURL(url)
                            }
                        } label: {
                            WorkflowRunRow(run: run)
                        }
                        .buttonStyle(.plain)
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
    }

    private func pagesColor(_ pages: GitHubPagesStatus) -> Color {
        switch pages.status {
        case "built": return .green
        case "building": return .orange
        case "errored": return .red
        default: return pages.enabled ? .blue : .gray
        }
    }
}

private struct WorkflowRunRow: View {
    let run: WorkflowRunInfo

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(run.name).font(.subheadline).foregroundStyle(.primary)
                Text("\(run.headBranch) · \(run.event)")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Spacer()
            Text(run.statusLabel)
                .font(.caption)
                .padding(.horizontal, 8)
                .padding(.vertical, 3)
                .background(badgeColor.opacity(0.2))
                .foregroundStyle(badgeColor)
                .clipShape(Capsule())
        }
    }

    private var badgeColor: Color {
        switch run.conclusion {
        case .success: return .green
        case .failure: return .red
        case .cancelled, .skipped: return .gray
        default:
            return run.status == .inProgress ? .orange : .blue
        }
    }
}

#Preview {
    GitHubView().environmentObject(AppState())
}
