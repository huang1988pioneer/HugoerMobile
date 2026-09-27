import SwiftUI
import UniformTypeIdentifiers
import WebKit

/// Rich Markdown editor backed by the bundled Quill page in `editor/` (shared/web/editor).
///
/// The page is served from a private `hugoer-editor://` scheme so its CSP can use `'self'`,
/// every other navigation is cancelled, and Markdown flows in and out as plain strings.
struct QuillMarkdownEditor: UIViewRepresentable {
    @Binding var markdown: String
    var baseURL: String
    var documentPath: String

    static let scheme = "hugoer-editor"
    private static let messageName = "hugoer"

    func makeCoordinator() -> Coordinator {
        Coordinator(parent: self)
    }

    func makeUIView(context: Context) -> WKWebView {
        let configuration = WKWebViewConfiguration()
        configuration.setURLSchemeHandler(EditorAssetSchemeHandler(), forURLScheme: Self.scheme)
        configuration.userContentController.add(WeakScriptMessageHandler(context.coordinator), name: Self.messageName)
        configuration.websiteDataStore = .nonPersistent()

        let webView = WKWebView(frame: .zero, configuration: configuration)
        webView.navigationDelegate = context.coordinator
        webView.isOpaque = false
        webView.backgroundColor = .systemBackground
        webView.scrollView.keyboardDismissMode = .interactive
        #if DEBUG
        if #available(iOS 16.4, *) { webView.isInspectable = true }
        #endif
        context.coordinator.webView = webView
        if let url = URL(string: "\(Self.scheme)://editor/index.html") {
            webView.load(URLRequest(url: url))
        }
        return webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {
        context.coordinator.parent = self
        context.coordinator.pushContentIfNeeded()
    }

    static func dismantleUIView(_ webView: WKWebView, coordinator: Coordinator) {
        webView.configuration.userContentController.removeScriptMessageHandler(forName: messageName)
        webView.navigationDelegate = nil
    }

    final class Coordinator: NSObject, WKNavigationDelegate, WKScriptMessageHandler {
        var parent: QuillMarkdownEditor
        weak var webView: WKWebView?
        private var isReady = false
        /// The Markdown the page currently holds, to avoid echoing its own edits back.
        private var syncedMarkdown: String?

        init(parent: QuillMarkdownEditor) {
            self.parent = parent
        }

        func pushContentIfNeeded() {
            guard isReady, let webView, parent.markdown != syncedMarkdown else { return }
            syncedMarkdown = parent.markdown
            let payload: [String: Any] = [
                "markdown": parent.markdown,
                "baseURL": parent.baseURL,
                "documentPath": parent.documentPath,
            ]
            guard let data = try? JSONSerialization.data(withJSONObject: payload),
                  let json = String(data: data, encoding: .utf8) else { return }
            webView.evaluateJavaScript("window.HugoerEditor.setContent(\(json));")
        }

        func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
            guard message.frameInfo.isMainFrame,
                  message.frameInfo.securityOrigin.protocol == QuillMarkdownEditor.scheme,
                  let body = message.body as? String,
                  let data = body.data(using: .utf8),
                  let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                  let type = object["type"] as? String else { return }

            switch type {
            case "ready":
                isReady = true
                pushContentIfNeeded()
            case "change":
                guard let markdown = object["markdown"] as? String else { return }
                syncedMarkdown = markdown
                if parent.markdown != markdown { parent.markdown = markdown }
            case "error":
                #if DEBUG
                print("QuillMarkdownEditor:", object["message"] ?? "")
                #endif
            default:
                break
            }
        }

        func webView(
            _ webView: WKWebView,
            decidePolicyFor navigationAction: WKNavigationAction,
            decisionHandler: @escaping (WKNavigationActionPolicy) -> Void
        ) {
            // Only the bundled editor page may load; links inside articles never navigate.
            let allowed = navigationAction.request.url?.scheme == QuillMarkdownEditor.scheme
                && navigationAction.navigationType != .linkActivated
            decisionHandler(allowed ? .allow : .cancel)
        }

        func webViewWebContentProcessDidTerminate(_ webView: WKWebView) {
            isReady = false
            syncedMarkdown = nil
            webView.reload()
        }
    }
}

/// Avoids the retain cycle between `WKUserContentController` and the coordinator.
private final class WeakScriptMessageHandler: NSObject, WKScriptMessageHandler {
    weak var target: WKScriptMessageHandler?

    init(_ target: WKScriptMessageHandler) {
        self.target = target
    }

    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        target?.userContentController(userContentController, didReceive: message)
    }
}

/// Serves files from the app bundle's `editor/` folder reference.
private final class EditorAssetSchemeHandler: NSObject, WKURLSchemeHandler {
    private let root = Bundle.main.resourceURL?.appendingPathComponent("editor", isDirectory: true).standardizedFileURL

    func webView(_ webView: WKWebView, start urlSchemeTask: WKURLSchemeTask) {
        guard let url = urlSchemeTask.request.url,
              let root,
              let fileURL = resolve(url.path, in: root),
              let data = try? Data(contentsOf: fileURL) else {
            urlSchemeTask.didFailWithError(URLError(.fileDoesNotExist))
            return
        }
        let mimeType = UTType(filenameExtension: fileURL.pathExtension)?.preferredMIMEType ?? "application/octet-stream"
        let response = HTTPURLResponse(
            url: url,
            statusCode: 200,
            httpVersion: "HTTP/1.1",
            headerFields: [
                "Content-Type": mimeType.hasPrefix("text/") || mimeType.hasSuffix("javascript") ? "\(mimeType); charset=utf-8" : mimeType,
                "Content-Length": String(data.count),
                "Cache-Control": "no-store",
            ]
        )
        urlSchemeTask.didReceive(response ?? URLResponse(url: url, mimeType: mimeType, expectedContentLength: data.count, textEncodingName: "utf-8"))
        urlSchemeTask.didReceive(data)
        urlSchemeTask.didFinish()
    }

    func webView(_ webView: WKWebView, stop urlSchemeTask: WKURLSchemeTask) {}

    private func resolve(_ path: String, in root: URL) -> URL? {
        let relative = path.hasPrefix("/") ? String(path.dropFirst()) : path
        guard !relative.isEmpty else { return nil }
        let candidate = root.appendingPathComponent(relative).standardizedFileURL
        // Refuse anything that escapes the editor folder (e.g. "../").
        guard candidate.path.hasPrefix(root.path + "/") else { return nil }
        return candidate
    }
}
