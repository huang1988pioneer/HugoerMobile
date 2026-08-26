using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using HugoerMobile.Models;
using HugoerMobile.Services;

namespace HugoerMobile.ViewModels;

/// <summary>
/// Mobile 主題 tab. Installing a theme (submodule/vendoring) stays a desktop job; this tab
/// focuses on showing what's configured and letting you flip the `theme` value quickly,
/// e.g. to try a theme you already vendored from the desktop app.
/// </summary>
public sealed partial class ThemesViewModel : BaseViewModel
{
    private readonly IGitHubApiService _api;
    private readonly ISecureTokenStore _tokenStore;
    private readonly ActiveSiteState _activeSite;
    private readonly TomlParamsService _tomlParams;
    private readonly ConfigLocator _configLocator;
    private readonly ThemeCatalogService _catalog;

    private string? _configPath;
    private string? _sha;
    private string _rawToml = string.Empty;

    public ThemesViewModel(
        IGitHubApiService api,
        ISecureTokenStore tokenStore,
        ActiveSiteState activeSite,
        TomlParamsService tomlParams,
        ConfigLocator configLocator,
        ThemeCatalogService catalog)
    {
        _api = api;
        _tokenStore = tokenStore;
        _activeSite = activeSite;
        _tomlParams = tomlParams;
        _configLocator = configLocator;
        _catalog = catalog;
        _activeSite.Changed += async (_, _) => await LoadAsync();

        foreach (var theme in _catalog.GetCatalog())
            Catalog.Add(theme);
    }

    public bool HasActiveSite => _activeSite.Current is not null;

    [ObservableProperty] private string currentTheme = string.Empty;

    public ObservableCollection<ThemeInfo> Catalog { get; } = [];

    [RelayCommand]
    private async Task AppearingAsync() => await LoadAsync();

    private async Task LoadAsync()
    {
        OnPropertyChanged(nameof(HasActiveSite));
        var site = _activeSite.Current;
        if (site is null)
        {
            CurrentTheme = string.Empty;
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

            var file = await _configLocator.FindAsync(site, token);
            _configPath = file?.Path ?? _configLocator.DefaultPath;
            _sha = file?.Sha;
            _rawToml = file?.TextContent ?? string.Empty;

            var (_, _, _, theme) = _tomlParams.ReadBasicFields(_rawToml);
            CurrentTheme = theme;
        });
    }

    [RelayCommand]
    private async Task ApplyThemeAsync(ThemeInfo theme) => await RunBusyAsync(async () =>
    {
        var site = _activeSite.Current;
        if (site is null || _configPath is null) return;

        var confirmed = await Shell.Current.CurrentPage.DisplayAlert(
            "套用主題",
            $"將 theme 設定改為「{theme.ThemeConfigValue}」並推送到 GitHub？\n" +
            "請確認此主題的檔案已存在於 repo（例如已用桌面版 Hugoer 安裝），否則網站建置會失敗。",
            "套用", "取消");
        if (!confirmed) return;

        var token = await _tokenStore.GetTokenAsync(site.TokenKey);
        if (string.IsNullOrEmpty(token))
        {
            ErrorMessage = "找不到此網站的權杖，請於「站台」重新連結。";
            return;
        }

        var updated = _tomlParams.UpsertSimpleRootKeys(_rawToml, new Dictionary<string, string>
        {
            ["theme"] = theme.ThemeConfigValue
        });

        var result = await _api.PutFileAsync(
            site.Owner, site.Repository, _configPath, updated,
            $"Switch theme to {theme.ThemeConfigValue} via Hugoer Mobile", _sha, site.DefaultBranch, token);

        _sha = result.Sha;
        _rawToml = updated;
        CurrentTheme = theme.ThemeConfigValue;
        StatusMessage = $"已套用主題 {theme.ThemeConfigValue} 並推送到 GitHub。";
    });

    [RelayCommand]
    private static async Task OpenThemeRepoAsync(ThemeInfo theme) =>
        await Launcher.Default.OpenAsync(theme.RepoUrl);
}
