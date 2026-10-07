using System.Text.Encodings.Web;
using System.Text.Json;
using Avalonia;
using Avalonia.Controls;
using Avalonia.Data;
using Avalonia.Media;
using Avalonia.Platform;
using Avalonia.Styling;
using Avalonia.Threading;

namespace Hugoer.Controls;

/// <summary>
/// Desktop rich-text editor. Hosts the shared Quill page (<c>shared/web/editor</c>) in
/// <see cref="NativeWebView"/> and exchanges Markdown with the page. Opening a document
/// does not re-serialise it; Markdown changes only after the user edits.
/// </summary>
public sealed class QuillMarkdownEditor : UserControl
{
    public static readonly StyledProperty<string?> MarkdownProperty =
        AvaloniaProperty.Register<QuillMarkdownEditor, string?>(
            nameof(Markdown), defaultBindingMode: BindingMode.TwoWay);

    public static readonly StyledProperty<string?> BaseUrlProperty =
        AvaloniaProperty.Register<QuillMarkdownEditor, string?>(nameof(BaseUrl));

    public static readonly StyledProperty<string?> DocumentPathProperty =
        AvaloniaProperty.Register<QuillMarkdownEditor, string?>(nameof(DocumentPath));

    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        Encoder = JavaScriptEncoder.Default
    };

    private readonly NativeWebView _webView;
    private EditorAssetServer? _server;
    private bool _ready;
    private string? _syncedMarkdown;
    private string? _syncedBaseUrl;
    private string? _syncedDocumentPath;
    private bool? _syncedDark;
    private int _pushGeneration;

    public QuillMarkdownEditor()
    {
        _webView = new NativeWebView
        {
            Background = new SolidColorBrush(Color.Parse("#1C1B1F"))
        };
        _webView.EnvironmentRequested += OnEnvironmentRequested;
        _webView.NavigationStarted += OnNavigationStarted;
        _webView.NavigationCompleted += OnNavigationCompleted;
        _webView.NewWindowRequested += (_, args) => args.Handled = true;
        _webView.WebMessageReceived += OnWebMessageReceived;
        Content = _webView;
    }

    public string? Markdown
    {
        get => GetValue(MarkdownProperty);
        set => SetValue(MarkdownProperty, value);
    }

    public string? BaseUrl
    {
        get => GetValue(BaseUrlProperty);
        set => SetValue(BaseUrlProperty, value);
    }

    public string? DocumentPath
    {
        get => GetValue(DocumentPathProperty);
        set => SetValue(DocumentPathProperty, value);
    }

    public event EventHandler? SaveRequested;

    protected override void OnAttachedToVisualTree(VisualTreeAttachmentEventArgs e)
    {
        base.OnAttachedToVisualTree(e);
        if (Application.Current is { } app)
            app.ActualThemeVariantChanged += OnThemeChanged;
        EnsureLoaded();
    }

    protected override void OnDetachedFromVisualTree(VisualTreeAttachmentEventArgs e)
    {
        if (Application.Current is { } app)
            app.ActualThemeVariantChanged -= OnThemeChanged;
        _ready = false;
        _server?.Dispose();
        _server = null;
        base.OnDetachedFromVisualTree(e);
    }

    protected override void OnPropertyChanged(AvaloniaPropertyChangedEventArgs change)
    {
        base.OnPropertyChanged(change);
        if (change.Property == MarkdownProperty
            || change.Property == BaseUrlProperty
            || change.Property == DocumentPathProperty)
            Push();
    }

    private void OnThemeChanged(object? sender, EventArgs e) => Push();

    private void EnsureLoaded()
    {
        if (_server is not null)
            return;

        var root = FindEditorRoot();
        if (root is null)
        {
            Content = new TextBlock
            {
                Text = "找不到 Quill 編輯器資源（editor/index.html）。",
                TextWrapping = TextWrapping.Wrap,
                Margin = new Thickness(16),
                Opacity = 0.8
            };
            return;
        }

        _server = new EditorAssetServer(root);
        _webView.Navigate(new Uri(_server.BaseUri, "index.html"));
    }

    private void OnEnvironmentRequested(object? sender, WebViewEnvironmentRequestedEventArgs e)
    {
        e.EnableDevTools = false;
        if (e is AppleWKWebViewEnvironmentRequestedEventArgs apple)
        {
            apple.NonPersistentDataStore = true;
            apple.UpgradeKnownHostsToHTTPS = false;
        }
    }

    private void OnNavigationStarted(object? sender, WebViewNavigationStartingEventArgs e)
    {
        if (e.Request is null || !IsEditorUrl(e.Request))
            e.Cancel = true;
    }

    private void OnNavigationCompleted(object? sender, WebViewNavigationCompletedEventArgs e)
    {
        if (e.Request is null || !IsEditorUrl(e.Request) || !e.IsSuccess)
            return;

        _ready = true;
        _syncedMarkdown = null;
        Push();
    }

    private void OnWebMessageReceived(object? sender, WebMessageReceivedEventArgs e)
    {
        if (string.IsNullOrWhiteSpace(e.Body))
            return;

        try
        {
            using var document = JsonDocument.Parse(e.Body);
            var root = document.RootElement;
            var type = root.TryGetProperty("type", out var typeElement) ? typeElement.GetString() : null;
            switch (type)
            {
                case "ready":
                    _ready = true;
                    _syncedMarkdown = null;
                    Push();
                    break;
                case "change":
                    if (!root.TryGetProperty("markdown", out var markdownElement))
                        return;
                    var markdown = markdownElement.GetString() ?? string.Empty;
                    _syncedMarkdown = markdown;
                    if (!string.Equals(Markdown, markdown, StringComparison.Ordinal))
                        Markdown = markdown;
                    break;
                case "save":
                    SaveRequested?.Invoke(this, EventArgs.Empty);
                    break;
            }
        }
        catch (JsonException)
        {
            // Ignore a malformed host message.
        }
    }

    private bool IsEditorUrl(Uri uri)
    {
        if (!uri.IsAbsoluteUri)
            return false;
        if (uri.AbsoluteUri == "about:blank")
            return true;
        return _server is not null
               && uri.Scheme == "http"
               && uri.Host == "127.0.0.1"
               && uri.Port == _server.Port;
    }

    private void Push()
    {
        if (!_ready || _server is null)
            return;

        var markdown = Markdown ?? string.Empty;
        var baseUrl = BaseUrl ?? string.Empty;
        var documentPath = DocumentPath ?? string.Empty;
        var dark = IsDarkTheme();
        var markdownChanged = !string.Equals(markdown, _syncedMarkdown, StringComparison.Ordinal);
        var locationChanged = !string.Equals(baseUrl, _syncedBaseUrl, StringComparison.Ordinal)
                              || !string.Equals(documentPath, _syncedDocumentPath, StringComparison.Ordinal);
        var themeChanged = _syncedDark != dark;
        if (!markdownChanged && !locationChanged && !themeChanged)
            return;

        var generation = ++_pushGeneration;
        var payload = JsonSerializer.Serialize(new
        {
            markdown,
            baseURL = baseUrl,
            documentPath,
            dark
        }, JsonOptions);
        // setContent rebuilds the document so relative images pick up a new base URL or path.
        // Theme-only updates keep the caret where it is.
        var script = markdownChanged || locationChanged
            ? $"window.HugoerEditor&&window.HugoerEditor.setContent({payload});"
            : $"window.HugoerEditor&&window.HugoerEditor.setContext({payload});";

        _ = PushAsync(script, generation, markdown, baseUrl, documentPath, dark);
    }

    private async Task PushAsync(
        string script,
        int generation,
        string markdown,
        string baseUrl,
        string documentPath,
        bool dark)
    {
        try
        {
            await _webView.InvokeScript(script);
            if (generation != _pushGeneration)
                return;

            await Dispatcher.UIThread.InvokeAsync(() =>
            {
                if (generation != _pushGeneration)
                    return;
                _syncedMarkdown = markdown;
                _syncedBaseUrl = baseUrl;
                _syncedDocumentPath = documentPath;
                _syncedDark = dark;
            });
        }
        catch (Exception)
        {
            // The page is navigating or the WebView was released.
        }
    }

    private static bool IsDarkTheme() =>
        Application.Current?.ActualThemeVariant == ThemeVariant.Dark;

    private static string? FindEditorRoot()
    {
        var candidates = new[]
        {
            Path.Combine(AppContext.BaseDirectory, "editor"),
            Path.Combine(AppContext.BaseDirectory, "shared", "web", "editor"),
            Path.GetFullPath(Path.Combine(AppContext.BaseDirectory, "..", "..", "..", "shared", "web", "editor"))
        };

        return candidates.FirstOrDefault(dir => File.Exists(Path.Combine(dir, "index.html")));
    }
}
