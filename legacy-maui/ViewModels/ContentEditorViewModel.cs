using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using HugoerMobile.Models;
using HugoerMobile.Services;

namespace HugoerMobile.ViewModels;

/// <summary>
/// Front matter fields + Markdown body editor with live preview. Mirrors the desktop app's
/// content editor (FrontMatterService + MarkdownPreviewService) but persists through the
/// GitHub Contents API — every "save" is a real commit to the repo.
/// </summary>
[QueryProperty(nameof(Path), nameof(Path))]
[QueryProperty(nameof(Sha), nameof(Sha))]
[QueryProperty(nameof(IsNew), nameof(IsNew))]
[QueryProperty(nameof(NewTitle), nameof(NewTitle))]
public sealed partial class ContentEditorViewModel : BaseViewModel
{
    private readonly IGitHubApiService _api;
    private readonly ISecureTokenStore _tokenStore;
    private readonly ActiveSiteState _activeSite;
    private readonly FrontMatterService _frontMatter;
    private readonly MarkdownPreviewService _preview;

    public ContentEditorViewModel(
        IGitHubApiService api,
        ISecureTokenStore tokenStore,
        ActiveSiteState activeSite,
        FrontMatterService frontMatter,
        MarkdownPreviewService preview)
    {
        _api = api;
        _tokenStore = tokenStore;
        _activeSite = activeSite;
        _frontMatter = frontMatter;
        _preview = preview;
    }

    [ObservableProperty] private string path = string.Empty;
    [ObservableProperty] private string sha = string.Empty;
    [ObservableProperty] private bool isNew;
    [ObservableProperty] private string newTitle = string.Empty;

    [ObservableProperty] private string title = string.Empty;
    [ObservableProperty] private string date = string.Empty;
    [ObservableProperty] private string tags = string.Empty;
    [ObservableProperty] private string categories = string.Empty;
    [ObservableProperty] private bool draft = true;
    [ObservableProperty] private string body = string.Empty;

    [ObservableProperty] private bool isPreviewSelected;
    [ObservableProperty] private string previewHtml = string.Empty;
    [ObservableProperty] private bool isDirty;

    private FrontMatterDocument _document = new();

    partial void OnPathChanged(string value) => _ = LoadAsync();

    private async Task LoadAsync()
    {
        var site = _activeSite.Current;
        if (site is null || string.IsNullOrEmpty(Path)) return;

        await RunBusyAsync(async () =>
        {
            var token = await _tokenStore.GetTokenAsync(site.TokenKey);
            if (string.IsNullOrEmpty(token))
            {
                ErrorMessage = "找不到此網站的權杖，請於「站台」重新連結。";
                return;
            }

            if (IsNew)
            {
                _document = new FrontMatterDocument
                {
                    Title = NewTitle,
                    Date = DateTimeOffset.Now.ToString("yyyy-MM-ddTHH:mm:sszzz"),
                    Draft = true
                };
                _document.Body = "開始寫作吧。";
            }
            else
            {
                var file = await _api.TryGetFileAsync(site.Owner, site.Repository, Path, site.DefaultBranch, token);
                if (file is null)
                {
                    ErrorMessage = "找不到這篇文章，可能已在其他裝置被刪除或搬移。";
                    return;
                }

                Sha = file.Sha;
                _document = _frontMatter.Parse(file.TextContent);
            }

            Title = _document.Title;
            Date = _document.Date;
            Tags = _document.Tags;
            Categories = _document.Categories;
            Draft = _document.Draft;
            Body = _document.Body;
            IsDirty = false;
            UpdatePreview();
        });
    }

    partial void OnBodyChanged(string value) => UpdatePreview();

    [RelayCommand]
    private void TogglePreview() => IsPreviewSelected = !IsPreviewSelected;

    private void UpdatePreview() =>
        PreviewHtml = _preview.RenderToHtmlDocument(Body, Application.Current?.RequestedTheme == AppTheme.Dark);

    [RelayCommand]
    private async Task SaveAsync() => await RunBusyAsync(async () =>
    {
        var site = _activeSite.Current;
        if (site is null) return;

        var token = await _tokenStore.GetTokenAsync(site.TokenKey);
        if (string.IsNullOrEmpty(token))
        {
            ErrorMessage = "找不到此網站的權杖，請於「站台」重新連結。";
            return;
        }

        _document.Title = Title;
        _document.Date = Date;
        _document.Tags = Tags;
        _document.Categories = Categories;
        _document.Draft = Draft;
        _document.Body = Body;

        var fullText = _frontMatter.Write(_document);
        var commitMessage = IsNew ? $"Add {Path} via Hugoer Mobile" : $"Update {Path} via Hugoer Mobile";

        var result = await _api.PutFileAsync(
            site.Owner, site.Repository, Path, fullText, commitMessage,
            string.IsNullOrEmpty(Sha) ? null : Sha, site.DefaultBranch, token);

        Sha = result.Sha;
        IsNew = false;
        IsDirty = false;
        StatusMessage = "已儲存並推送到 GitHub。";
    });

    [RelayCommand]
    private async Task DeleteAsync()
    {
        var site = _activeSite.Current;
        if (site is null || string.IsNullOrEmpty(Sha)) return;

        var confirmed = await Shell.Current.CurrentPage.DisplayAlert("刪除文章", "確定要刪除這篇文章嗎？此動作會直接提交到 GitHub。", "刪除", "取消");
        if (!confirmed) return;

        await RunBusyAsync(async () =>
        {
            var token = await _tokenStore.GetTokenAsync(site.TokenKey);
            if (string.IsNullOrEmpty(token)) return;

            await _api.DeleteFileAsync(site.Owner, site.Repository, Path, Sha,
                $"Delete {Path} via Hugoer Mobile", site.DefaultBranch, token);
            await Shell.Current.GoToAsync("..");
        });
    }
}
