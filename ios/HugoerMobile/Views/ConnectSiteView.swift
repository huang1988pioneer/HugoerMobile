import SwiftUI

struct ConnectSiteView: View {
    @EnvironmentObject private var appState: AppState
    @StateObject private var viewModel = ConnectSiteViewModel()
    @Binding var isPresented: Bool

    var body: some View {
        Form {
            Section("GitHub 儲存庫") {
                TextField("owner/repo 或 https://github.com/owner/repo", text: $viewModel.repositoryInput)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .keyboardType(.URL)
            }

            Section("GitHub 個人存取權杖 (PAT)") {
                SecureField("GitHub PAT", text: $viewModel.token)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                Text("建議使用 fine-grained PAT，僅授權此 repo 的 Contents、Actions、Pages（讀寫）與 Metadata（唯讀）。")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            Section {
                Button {
                    Task { await viewModel.verify() }
                } label: {
                    if viewModel.isVerifying {
                        ProgressView()
                    } else {
                        Text("驗證連線")
                    }
                }
                .disabled(!viewModel.canVerify || viewModel.isVerifying)
            }

            if let errorMessage = viewModel.errorMessage {
                Section {
                    Text(errorMessage)
                        .foregroundStyle(.red)
                }
            }

            if let repo = viewModel.verifiedRepo, let login = viewModel.verifiedLogin {
                Section("連線摘要") {
                    LabeledContent("GitHub 帳號", value: login)
                    LabeledContent("儲存庫", value: "\(repo.owner)/\(repo.name)")
                    LabeledContent("預設分支", value: repo.defaultBranch)
                    LabeledContent("私有 repo", value: repo.isPrivate ? "是" : "否")
                    LabeledContent("推送權限", value: repo.permissions.push ? "有" : "無")
                    LabeledContent("管理權限", value: repo.permissions.admin ? "有" : "無")
                }

                Section {
                    Button {
                        if let profile = viewModel.buildProfile() {
                            appState.addSite(profile, token: viewModel.token)
                            isPresented = false
                        }
                    } label: {
                        Text("儲存並開始使用")
                            .font(.headline)
                    }
                    .disabled(!viewModel.canSave)
                }
            }
        }
        .navigationTitle("新增網站")
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("取消") { isPresented = false }
            }
        }
    }
}

#Preview {
    NavigationStack {
        ConnectSiteView(isPresented: .constant(true))
    }
    .environmentObject(AppState())
}
