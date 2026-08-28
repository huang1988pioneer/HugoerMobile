import SwiftUI

struct ContentBrowserView: View {
    @EnvironmentObject private var appState: AppState
    @StateObject private var viewModel = ContentViewModel()
    @State private var showNewPostAlert = false
    @State private var newPostTitle = ""
    @State private var navigateToEditorPath: String?
    @State private var navigateToEditorIsNew = false
    @State private var itemToDelete: ContentItem?

    var body: some View {
        NavigationStack {
            Group {
                if let site = appState.activeSite {
                    content(for: site)
                } else {
                    NoActiveSiteView()
                }
            }
            .navigationTitle("內容")
            .navigationDestination(isPresented: Binding(
                get: { navigateToEditorPath != nil },
                set: { if !$0 { navigateToEditorPath = nil } }
            )) {
                if let path = navigateToEditorPath, let site = appState.activeSite, let token = appState.token(for: site) {
                    ContentEditorView(path: path, isNewFile: navigateToEditorIsNew, initialTitle: newPostTitle, site: site, token: token)
                }
            }
        }
    }

    @ViewBuilder
    private func content(for site: SiteProfile) -> some View {
        VStack(spacing: 0) {
            HStack {
                if viewModel.canGoUp {
                    Button {
                        Task { await viewModel.goUp() }
                    } label: {
                        Label("上一層", systemImage: "chevron.up")
                    }
                }
                Spacer()
                Text(viewModel.breadcrumb)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }
            .padding(.horizontal)
            .padding(.top, 8)

            List {
                if viewModel.isLoading {
                    ProgressView()
                } else if let error = viewModel.errorMessage {
                    Text(error).foregroundStyle(.red)
                } else {
                    ForEach(viewModel.filteredItems) { item in
                        ContentRow(item: item)
                            .contentShape(Rectangle())
                            .onTapGesture {
                                Task {
                                    if item.isDirectory {
                                        await viewModel.enterDirectory(item)
                                    } else if item.isMarkdown {
                                        navigateToEditorIsNew = false
                                        navigateToEditorPath = item.path
                                    }
                                }
                            }
                            .swipeActions {
                                Button("刪除", role: .destructive) {
                                    itemToDelete = item
                                }
                            }
                    }
                }
            }
            .listStyle(.plain)
            .searchable(text: $viewModel.searchText, prompt: "搜尋檔名")
            .refreshable {
                await viewModel.load()
            }
        }
        .safeAreaInset(edge: .bottom) {
            Button {
                newPostTitle = ""
                showNewPostAlert = true
            } label: {
                Label("＋ 新增文章", systemImage: "square.and.pencil")
                    .font(.headline)
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .padding()
        }
        .onAppear {
            viewModel.configure(site: site, token: appState.token(for: site) ?? "")
        }
        .task(id: site.id) {
            viewModel.configure(site: site, token: appState.token(for: site) ?? "")
            await viewModel.load()
        }
        .alert("新增文章", isPresented: $showNewPostAlert) {
            TextField("文章標題", text: $newPostTitle)
            Button("取消", role: .cancel) {}
            Button("建立") {
                let title = newPostTitle.isEmpty ? "未命名文章" : newPostTitle
                let path = viewModel.newPostPath(title: title)
                self.newPostTitle = title
                navigateToEditorIsNew = true
                navigateToEditorPath = path
            }
        } message: {
            Text("請輸入新文章標題，將自動產生檔名 slug。")
        }
        .alert("刪除文章", isPresented: Binding(
            get: { itemToDelete != nil },
            set: { if !$0 { itemToDelete = nil } }
        )) {
            Button("取消", role: .cancel) { itemToDelete = nil }
            Button("刪除", role: .destructive) {
                if let item = itemToDelete {
                    Task { await viewModel.delete(item) }
                }
                itemToDelete = nil
            }
        } message: {
            Text("此動作會直接提交到 GitHub，且無法復原。")
        }
    }
}

private struct ContentRow: View {
    let item: ContentItem

    var body: some View {
        HStack {
            Image(systemName: item.isDirectory ? "folder.fill" : (item.isMarkdown ? "doc.text.fill" : "doc"))
                .foregroundStyle(item.isDirectory ? .blue : .secondary)
            VStack(alignment: .leading) {
                Text(item.name)
                if !item.isDirectory {
                    Text("\(item.size) 位元組")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
            }
            Spacer()
            if item.isDirectory {
                Image(systemName: "chevron.right")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(.vertical, 4)
    }
}

#Preview {
    ContentBrowserView().environmentObject(AppState())
}
