import SwiftUI

private enum EditorMode: String, CaseIterable, Identifiable {
    case edit = "編輯"
    case preview = "預覽"
    var id: String { rawValue }
}

struct ContentEditorView: View {
    @StateObject private var viewModel: ContentEditorViewModel
    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var colorScheme
    @State private var mode: EditorMode = .edit
    @State private var showDeleteConfirm = false
    @State private var frontMatterExpanded = true

    private let site: SiteProfile
    private let token: String

    init(path: String, isNewFile: Bool, initialTitle: String = "", site: SiteProfile, token: String) {
        _viewModel = StateObject(wrappedValue: ContentEditorViewModel(path: path, isNewFile: isNewFile, initialTitle: initialTitle))
        self.site = site
        self.token = token
    }

    var body: some View {
        VStack(spacing: 0) {
            Picker("模式", selection: $mode) {
                ForEach(EditorMode.allCases) { m in
                    Text(m.rawValue).tag(m)
                }
            }
            .pickerStyle(.segmented)
            .padding()

            if viewModel.isLoading {
                ProgressView("載入中…")
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if mode == .edit {
                editForm
            } else {
                MarkdownWebView(htmlString: viewModel.renderPreviewHtml(isDarkMode: colorScheme == .dark))
            }
        }
        .navigationTitle(viewModel.path)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if !viewModel.isNewFile {
                ToolbarItem(placement: .topBarTrailing) {
                    Button(role: .destructive) {
                        showDeleteConfirm = true
                    } label: {
                        Image(systemName: "trash")
                    }
                }
            }
        }
        .safeAreaInset(edge: .bottom) {
            VStack(spacing: 8) {
                if let error = viewModel.errorMessage {
                    Text(error).foregroundStyle(.red).font(.caption)
                }
                if let success = viewModel.successMessage {
                    Text(success).foregroundStyle(.green).font(.caption)
                }
                Button {
                    Task { await viewModel.save() }
                } label: {
                    if viewModel.isSaving {
                        ProgressView().frame(maxWidth: .infinity)
                    } else {
                        Text("儲存並推送到 GitHub")
                            .font(.headline)
                            .frame(maxWidth: .infinity)
                    }
                }
                .buttonStyle(.borderedProminent)
                .disabled(viewModel.isSaving)
            }
            .padding()
            .background(.bar)
        }
        .task {
            viewModel.configure(site: site, token: token)
            await viewModel.load()
        }
        .onChange(of: viewModel.didDelete) { _, didDelete in
            if didDelete { dismiss() }
        }
        .alert("確定要刪除這篇文章嗎？", isPresented: $showDeleteConfirm) {
            Button("取消", role: .cancel) {}
            Button("刪除", role: .destructive) {
                Task { await viewModel.delete() }
            }
        } message: {
            Text("此操作會透過 GitHub API 刪除該檔案，且無法復原。")
        }
    }

    private var editForm: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                DisclosureGroup("前置資料 (Front Matter)", isExpanded: $frontMatterExpanded) {
                    VStack(alignment: .leading, spacing: 12) {
                        LabeledField(label: "標題") {
                            TextField("文章標題", text: $viewModel.title)
                        }
                        LabeledField(label: "日期") {
                            TextField("YYYY-MM-DDTHH:mm:ssZ", text: $viewModel.dateText)
                                .textInputAutocapitalization(.never)
                        }
                        LabeledField(label: "標籤 (逗號分隔)") {
                            TextField("tag1, tag2", text: $viewModel.tags)
                                .textInputAutocapitalization(.never)
                        }
                        LabeledField(label: "分類 (逗號分隔)") {
                            TextField("category1, category2", text: $viewModel.categories)
                                .textInputAutocapitalization(.never)
                        }
                        Toggle("草稿 (draft)", isOn: $viewModel.draft)
                    }
                    .padding(.top, 8)
                }
                .padding()
                .background(Color(.secondarySystemBackground))
                .clipShape(RoundedRectangle(cornerRadius: 12))

                Text("內文 (Markdown)")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                TextEditor(text: $viewModel.body)
                    .frame(minHeight: 320)
                    .font(.system(.body, design: .monospaced))
                    .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.secondary.opacity(0.3)))
            }
            .padding()
        }
    }
}

private struct LabeledField<Content: View>: View {
    let label: String
    @ViewBuilder let content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label).font(.caption).foregroundStyle(.secondary)
            content.textFieldStyle(.roundedBorder)
        }
    }
}
