using System.ComponentModel;
using Avalonia;
using Avalonia.Controls;
using Avalonia.Input;
using Avalonia.Interactivity;
using Avalonia.Threading;
using Hugoer.Services;
using Hugoer.ViewModels;

namespace Hugoer.Views;

public partial class ContentView : UserControl
{
    private bool _syncingFromWysiwyg;

    public ContentView()
    {
        InitializeComponent();
        WysiwygEditor.MarkdownChanged += Wysiwyg_OnMarkdownChanged;
        WysiwygEditor.SaveRequested += Wysiwyg_OnSaveRequested;
        WysiwygEditor.ToggleModeRequested += Wysiwyg_OnToggleModeRequested;
        WysiwygEditor.EditorFailed += Wysiwyg_OnEditorFailed;
    }

    private ContentViewModel? ViewModel => DataContext as ContentViewModel;

    private bool IsWysiwyg => ViewModel is { IsWysiwygMode: true };

    protected override void OnPropertyChanged(AvaloniaPropertyChangedEventArgs change)
    {
        base.OnPropertyChanged(change);
        if (change.Property != DataContextProperty) return;
        if (change.OldValue is ContentViewModel oldViewModel)
            oldViewModel.PropertyChanged -= OnViewModelPropertyChanged;
        if (change.NewValue is ContentViewModel newViewModel)
        {
            newViewModel.PropertyChanged += OnViewModelPropertyChanged;
            SyncWysiwygFromViewModel();
        }
    }

    private void OnViewModelPropertyChanged(object? sender, PropertyChangedEventArgs e)
    {
        if (e.PropertyName == nameof(ContentViewModel.EditorText))
            SyncWysiwygFromViewModel();
        else if (e.PropertyName == nameof(ContentViewModel.IsWysiwygMode))
            _ = ApplyEditorModeAsync();
    }

    private async Task ApplyEditorModeAsync()
    {
        if (IsWysiwyg)
        {
            SyncWysiwygFromViewModel();
            await WysiwygEditor.FocusEditorAsync();
            return;
        }

        // Leaving the visual editor: collect edits still waiting in its debounce.
        await WysiwygEditor.FlushAsync();
        MarkdownEditor.Focus();
    }

    /// <summary>The visual editor only ever sees the body; front matter stays in the form above.</summary>
    private void SyncWysiwygFromViewModel()
    {
        if (_syncingFromWysiwyg || ViewModel is not { } viewModel) return;
        WysiwygEditor.SitePath = viewModel.CurrentSitePath;
        var body = viewModel.FrontMatter.GetBody(viewModel.EditorText);
        if (!string.Equals(WysiwygEditor.Markdown, body, StringComparison.Ordinal))
            WysiwygEditor.Markdown = body;
    }

    private void Wysiwyg_OnMarkdownChanged(object? sender, EventArgs e)
    {
        if (ViewModel is not { } viewModel) return;
        _syncingFromWysiwyg = true;
        try
        {
            viewModel.EditorText = viewModel.FrontMatter.ReplaceBody(
                viewModel.EditorText,
                WysiwygEditor.Markdown ?? string.Empty);
        }
        finally
        {
            _syncingFromWysiwyg = false;
        }
    }

    private void Wysiwyg_OnSaveRequested(object? sender, EventArgs e) => ExecuteSave();

    private void Wysiwyg_OnToggleModeRequested(object? sender, EventArgs e) =>
        ViewModel?.ToggleEditorModeCommand.Execute(null);

    private void Wysiwyg_OnEditorFailed(object? sender, string detail)
    {
        if (ViewModel is not { } viewModel) return;
        viewModel.IsWysiwygMode = false;
        viewModel.StatusMessage = "視覺編輯器無法使用（需要系統 WebView），已切換為 Markdown 原始碼模式。";
    }

    private async void Save_OnClick(object? sender, RoutedEventArgs e)
    {
        if (IsWysiwyg)
            await WysiwygEditor.FlushAsync();
        ExecuteSave();
    }

    private void ExecuteSave()
    {
        if (ViewModel is { } viewModel && viewModel.SaveCommand.CanExecute(null))
            viewModel.SaveCommand.Execute(null);
    }

    private void Bold_OnClick(object? sender, RoutedEventArgs e) =>
        RunEditorCommand("bold", (text, start, length) => MarkdownEditingService.Wrap(text, start, length, "**", "**", "粗體文字"));

    private void Italic_OnClick(object? sender, RoutedEventArgs e) =>
        RunEditorCommand("italic", (text, start, length) => MarkdownEditingService.Wrap(text, start, length, "*", "*", "斜體文字"));

    private void Strike_OnClick(object? sender, RoutedEventArgs e) =>
        RunEditorCommand("strike", (text, start, length) => MarkdownEditingService.Wrap(text, start, length, "~~", "~~", "刪除文字"));

    private void InlineCode_OnClick(object? sender, RoutedEventArgs e) =>
        RunEditorCommand("inlineCode", (text, start, length) => MarkdownEditingService.Wrap(text, start, length, "`", "`", "程式碼"));

    private void Link_OnClick(object? sender, RoutedEventArgs e) =>
        RunEditorCommand("link", (text, start, length) => MarkdownEditingService.Link(text, start, length, image: false));

    private void Image_OnClick(object? sender, RoutedEventArgs e) =>
        RunEditorCommand("image", (text, start, length) => MarkdownEditingService.Link(text, start, length, image: true));

    private void Heading1_OnClick(object? sender, RoutedEventArgs e) => ApplyHeading(1);
    private void Heading2_OnClick(object? sender, RoutedEventArgs e) => ApplyHeading(2);
    private void Heading3_OnClick(object? sender, RoutedEventArgs e) => ApplyHeading(3);

    private void ApplyHeading(int level) =>
        RunEditorCommand("heading", (text, start, length) => MarkdownEditingService.Heading(text, start, length, level),
            level.ToString());

    private void Quote_OnClick(object? sender, RoutedEventArgs e) =>
        RunEditorCommand("quote", (text, start, length) => MarkdownEditingService.PrefixLines(text, start, length, "> "));

    private void BulletList_OnClick(object? sender, RoutedEventArgs e) =>
        RunEditorCommand("bulletList", (text, start, length) => MarkdownEditingService.PrefixLines(text, start, length, "- "));

    private void TaskList_OnClick(object? sender, RoutedEventArgs e) =>
        RunEditorCommand("taskList", (text, start, length) => MarkdownEditingService.PrefixLines(text, start, length, "- [ ] "));

    private void OrderedList_OnClick(object? sender, RoutedEventArgs e) =>
        RunEditorCommand("orderedList", MarkdownEditingService.OrderedList);

    private void CodeBlock_OnClick(object? sender, RoutedEventArgs e) =>
        RunEditorCommand("codeBlock", MarkdownEditingService.CodeBlock);

    private void Table_OnClick(object? sender, RoutedEventArgs e) =>
        RunEditorCommand("table", MarkdownEditingService.InsertTable);

    private void HorizontalRule_OnClick(object? sender, RoutedEventArgs e) =>
        RunEditorCommand("hr", MarkdownEditingService.HorizontalRule);

    /// <summary>Toolbar buttons drive Quill in visual mode and edit the Markdown text in source mode.</summary>
    private void RunEditorCommand(
        string wysiwygCommand,
        Func<string, int, int, MarkdownEditResult> sourceOperation,
        string? argument = null)
    {
        if (IsWysiwyg)
        {
            _ = WysiwygEditor.ExecAsync(wysiwygCommand, argument);
            return;
        }

        ApplyEdit(sourceOperation);
    }

    private void MarkdownEditor_OnKeyDown(object? sender, KeyEventArgs e)
    {
        if (!e.KeyModifiers.HasFlag(KeyModifiers.Control)) return;
        switch (e.Key)
        {
            case Key.B:
                Bold_OnClick(sender, e);
                e.Handled = true;
                break;
            case Key.I:
                Italic_OnClick(sender, e);
                e.Handled = true;
                break;
            case Key.K:
                Link_OnClick(sender, e);
                e.Handled = true;
                break;
            case Key.M when e.KeyModifiers.HasFlag(KeyModifiers.Shift):
                ViewModel?.ToggleEditorModeCommand.Execute(null);
                e.Handled = true;
                break;
            case Key.S:
                ExecuteSave();
                e.Handled = true;
                break;
        }
    }

    private void ApplyEdit(Func<string, int, int, MarkdownEditResult> operation)
    {
        var text = MarkdownEditor.Text ?? string.Empty;
        var start = Math.Min(MarkdownEditor.SelectionStart, MarkdownEditor.SelectionEnd);
        var length = Math.Abs(MarkdownEditor.SelectionEnd - MarkdownEditor.SelectionStart);
        if (MarkdownEditingService.IsInsideFrontMatter(text, start))
        {
            if (DataContext is ContentViewModel viewModel)
                viewModel.StatusMessage = "請使用上方欄位編輯 front matter；格式工具只作用於 Markdown 正文。";
            return;
        }

        var result = operation(text, start, length);
        if (DataContext is ContentViewModel editorViewModel)
            editorViewModel.EditorText = result.Text;
        else
            MarkdownEditor.Text = result.Text;
        Dispatcher.UIThread.Post(() =>
        {
            MarkdownEditor.Focus();
            MarkdownEditor.SelectionStart = result.SelectionStart;
            MarkdownEditor.SelectionEnd = result.SelectionStart + result.SelectionLength;
        });
    }
}
