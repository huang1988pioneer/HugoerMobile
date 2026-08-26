import SwiftUI

struct ConfigView: View {
    @EnvironmentObject private var appState: AppState
    @StateObject private var viewModel = ConfigViewModel()
    @State private var mode: ConfigEditMode = .basic

    var body: some View {
        NavigationStack {
            Group {
                if let site = appState.activeSite {
                    content(for: site)
                } else {
                    NoActiveSiteView()
                }
            }
            .navigationTitle("設定檔")
        }
    }

    @ViewBuilder
    private func content(for site: SiteProfile) -> some View {
        VStack(spacing: 0) {
            Picker("模式", selection: $mode) {
                ForEach(ConfigEditMode.allCases) { m in
                    Text(m.rawValue).tag(m)
                }
            }
            .pickerStyle(.segmented)
            .padding()

            if viewModel.isLoading {
                ProgressView("載入設定檔中…")
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        if let error = viewModel.errorMessage {
                            Text(error).foregroundStyle(.red)
                        }
                        if let success = viewModel.successMessage {
                            Text(success).foregroundStyle(.green)
                        }

                        switch mode {
                        case .basic:
                            basicForm
                        case .paramsForm:
                            paramsForm
                        case .rawToml:
                            rawEditor
                        }
                    }
                    .padding()
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

    private var basicForm: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("路徑：\(viewModel.filePath)")
                .font(.caption)
                .foregroundStyle(.secondary)

            LabeledField(label: "baseURL") {
                TextField("https://example.com/", text: $viewModel.baseUrl)
                    .keyboardType(.URL)
                    .textInputAutocapitalization(.never)
            }
            LabeledField(label: "title") {
                TextField("網站標題", text: $viewModel.title)
            }
            LabeledField(label: "languageCode") {
                TextField("例如 zh-tw", text: $viewModel.languageCode)
                    .textInputAutocapitalization(.never)
            }
            LabeledField(label: "theme") {
                TextField("主題資料夾名稱", text: $viewModel.theme)
                    .textInputAutocapitalization(.never)
            }

            SaveButton(isSaving: viewModel.isSaving, title: "儲存並推送到 GitHub") {
                Task { await viewModel.saveBasicFields() }
            }
        }
        .cardStyle()
    }

    private var paramsForm: some View {
        VStack(alignment: .leading, spacing: 12) {
            ForEach($viewModel.paramFields) { $field in
                ParamFieldRow(field: $field)
                Divider()
            }

            SaveButton(isSaving: viewModel.isSaving, title: "儲存並推送到 GitHub") {
                Task { await viewModel.saveParamsForm() }
            }
        }
        .cardStyle()
    }

    private var rawEditor: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("路徑：\(viewModel.filePath)")
                .font(.caption)
                .foregroundStyle(.secondary)

            TextEditor(text: $viewModel.rawText)
                .frame(minHeight: 400)
                .font(.system(.body, design: .monospaced))
                .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.secondary.opacity(0.3)))

            SaveButton(isSaving: viewModel.isSaving, title: "儲存並推送到 GitHub") {
                Task { await viewModel.saveRawTomlEditsDirectly() }
            }
        }
        .cardStyle()
    }
}

private struct LabeledField<Content: View>: View {
    let label: String
    @ViewBuilder let content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label).font(.caption).foregroundStyle(.secondary)
            content
                .textFieldStyle(.roundedBorder)
        }
    }
}

private struct ParamFieldRow: View {
    @Binding var field: ParamFieldItem

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                VStack(alignment: .leading) {
                    Text(field.displayName).font(.subheadline).bold()
                    Text(field.description).font(.caption).foregroundStyle(.secondary)
                }
                Spacer()
                Toggle("", isOn: $field.isConfigured)
                    .labelsHidden()
            }

            if field.isConfigured {
                switch field.kind {
                case .bool:
                    Toggle(field.displayName, isOn: $field.boolValue)
                        .labelsHidden()
                case .number, .string, .array:
                    TextField(field.kind == .array ? "以逗號分隔" : "值", text: $field.stringValue)
                        .textFieldStyle(.roundedBorder)
                        .keyboardType(field.kind == .number ? .decimalPad : .default)
                case .nested:
                    Text(field.stringValue).font(.caption).foregroundStyle(.secondary)
                }
            }
        }
        .padding(.vertical, 4)
    }
}

private struct SaveButton: View {
    let isSaving: Bool
    let title: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            if isSaving {
                ProgressView()
                    .frame(maxWidth: .infinity)
            } else {
                Text(title)
                    .font(.headline)
                    .frame(maxWidth: .infinity)
            }
        }
        .buttonStyle(.borderedProminent)
        .disabled(isSaving)
        .padding(.top, 8)
    }
}

private extension View {
    func cardStyle() -> some View {
        self
            .padding()
            .background(Color(.secondarySystemBackground))
            .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}

#Preview {
    ConfigView().environmentObject(AppState())
}
