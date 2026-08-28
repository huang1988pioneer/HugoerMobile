using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using HugoerMobile.Models;
using HugoerMobile.Services;
using HugoerMobile.Views;

namespace HugoerMobile.ViewModels;

/// <summary>
/// Mobile 內容 tab: browses the repo's content/ directory via the GitHub Contents API,
/// mirroring the desktop app's <c>ContentService</c> but without a local filesystem.
/// </summary>
public sealed partial class ContentListViewModel : BaseViewModel
{
    private const string ContentRoot = "content";

    private readonly IGitHubApiService _api;
    private readonly ISecureTokenStore _tokenStore;
    private readonly ActiveSiteState _activeSite;

    public ContentListViewModel(IGitHubApiService api, ISecureTokenStore tokenStore, ActiveSiteState activeSite)
    {
        _api = api;
        _tokenStore = tokenStore;
        _activeSite = activeSite;
        _activeSite.Changed += async (_, _) =>
        {
            CurrentPath = ContentRoot;
            await LoadAsync();
        };
        currentPath = ContentRoot;
    }

    public bool HasActiveSite => _activeSite.Current is not null;

    [ObservableProperty] private string currentPath;
    [ObservableProperty] private string searchText = string.Empty;
    [ObservableProperty] private bool isEmpty;

    public ObservableCollection<ContentItem> Items { get; } = [];

    public bool CanGoUp => CurrentPath != ContentRoot;

    [RelayCommand]
    private async Task AppearingAsync() => await LoadAsync();

    [RelayCommand]
    private Task RefreshAsync() => LoadAsync();

    private async Task LoadAsync()
    {
        OnPropertyChanged(nameof(HasActiveSite));
        var site = _activeSite.Current;
        if (site is null)
        {
            Items.Clear();
            IsEmpty = true;
            return;
        }

        await RunBusyAsync(async () =>
        {
            var token = await _tokenStore.GetTokenAsync(site.TokenKey);
            if (string.IsNullOrEmpty(token))
            {
                ErrorMessage = "找不到此網站的權杖，請於「站台」重新連結。";
                return;
            }

            var items = await _api.ListDirectoryAsync(site.Owner, site.Repository, CurrentPath, site.DefaultBranch, token);
            var filtered = string.IsNullOrWhiteSpace(SearchText)
                ? items
                : items.Where(i => i.Name.Contains(SearchText, StringComparison.OrdinalIgnoreCase)).ToList();

            Items.Clear();
            foreach (var item in filtered.Where(i => i.IsDirectory || i.IsMarkdown))
                Items.Add(item);

            IsEmpty = Items.Count == 0;
            OnPropertyChanged(nameof(CanGoUp));
        });
    }

    partial void OnSearchTextChanged(string value) => _ = LoadAsync();

    [RelayCommand]
    private async Task OpenAsync(ContentItem item)
    {
        if (item.IsDirectory)
        {
            CurrentPath = item.Path;
            await LoadAsync();
            return;
        }

        await Shell.Current.GoToAsync(nameof(ContentEditorPage), new Dictionary<string, object>
        {
            [nameof(ContentEditorViewModel.Path)] = item.Path,
            [nameof(ContentEditorViewModel.Sha)] = item.Sha ?? string.Empty,
            [nameof(ContentEditorViewModel.IsNew)] = false
        });
    }

    [RelayCommand]
    private async Task GoUpAsync()
    {
        if (!CanGoUp) return;
        var parent = CurrentPath.Contains('/') ? CurrentPath[..CurrentPath.LastIndexOf('/')] : ContentRoot;
        CurrentPath = string.IsNullOrEmpty(parent) ? ContentRoot : parent;
        await LoadAsync();
    }

    [RelayCommand]
    private async Task AddPostAsync()
    {
        var page = Shell.Current.CurrentPage;
        var title = await page.DisplayPromptAsync("新增文章", "文章標題", "建立", "取消", placeholder: "我的第一篇文章");
        if (string.IsNullOrWhiteSpace(title)) return;

        var slug = Slugify(title);
        var targetDir = CurrentPath == ContentRoot ? $"{ContentRoot}/posts" : CurrentPath;
        var path = $"{targetDir}/{slug}.md";

        await Shell.Current.GoToAsync(nameof(ContentEditorPage), new Dictionary<string, object>
        {
            [nameof(ContentEditorViewModel.Path)] = path,
            [nameof(ContentEditorViewModel.Sha)] = string.Empty,
            [nameof(ContentEditorViewModel.IsNew)] = true,
            [nameof(ContentEditorViewModel.NewTitle)] = title
        });
    }

    [RelayCommand]
    private async Task DeleteAsync(ContentItem item)
    {
        var site = _activeSite.Current;
        if (site is null || item.IsDirectory || item.Sha is null) return;

        var page = Shell.Current.CurrentPage;
        var confirmed = await page.DisplayAlert("刪除文章", $"確定要刪除「{item.Name}」嗎？此動作會直接提交到 GitHub。", "刪除", "取消");
        if (!confirmed) return;

        await RunBusyAsync(async () =>
        {
            var token = await _tokenStore.GetTokenAsync(site.TokenKey);
            if (string.IsNullOrEmpty(token))
            {
                ErrorMessage = "找不到此網站的權杖，請於「站台」重新連結。";
                return;
            }

            await _api.DeleteFileAsync(
                site.Owner, site.Repository, item.Path, item.Sha!,
                $"Delete {item.Path} via Hugoer Mobile", site.DefaultBranch, token);
            Items.Remove(item);
            IsEmpty = Items.Count == 0;
        });
    }

    private static string Slugify(string title)
    {
        var lower = title.Trim().ToLowerInvariant();
        var chars = lower.Select(c => char.IsLetterOrDigit(c) ? c : (c is ' ' or '-' or '_' ? '-' : '\0'))
            .Where(c => c != '\0')
            .ToArray();
        var slug = new string(chars);
        while (slug.Contains("--")) slug = slug.Replace("--", "-");
        slug = slug.Trim('-');
        return string.IsNullOrEmpty(slug) ? DateTimeOffset.UtcNow.ToUnixTimeSeconds().ToString() : slug;
    }
}
