using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using HugoerMobile.Models;
using HugoerMobile.Services;

namespace HugoerMobile.ViewModels;

/// <summary>
/// Mobile 設定檔 tab: 基本欄位 / Params 表單 / 原始 TOML — same three views as the desktop app,
/// backed by the exact same <see cref="TomlParamsService"/> logic, but reading/writing the
/// file through the GitHub Contents API instead of the local filesystem.
/// </summary>
public sealed partial class ConfigViewModel : BaseViewModel
{
    private readonly IGitHubApiService _api;
    private readonly ISecureTokenStore _tokenStore;
    private readonly ActiveSiteState _activeSite;
    private readonly TomlParamsService _tomlParams;
    private readonly ConfigLocator _configLocator;

    private string? _configPath;
    private string? _sha;
    private SiteProfile? _loadedFor;

    public ConfigViewModel(
        IGitHubApiService api,
        ISecureTokenStore tokenStore,
        ActiveSiteState activeSite,
        TomlParamsService tomlParams,
        ConfigLocator configLocator)
    {
        _api = api;
        _tokenStore = tokenStore;
        _activeSite = activeSite;
        _tomlParams = tomlParams;
        _configLocator = configLocator;
        _activeSite.Changed += async (_, _) => await LoadAsync();
    }

    public bool HasActiveSite => _activeSite.Current is not null;
    public string ActiveSiteName => _activeSite.Current?.FullName ?? string.Empty;

    [ObservableProperty] private bool isBasicSegmentSelected = true;
    [ObservableProperty] private bool isParamsSegmentSelected;
    [ObservableProperty] private bool isRawSegmentSelected;

    [ObservableProperty] private string baseUrl = string.Empty;
    [ObservableProperty] private string siteTitle = string.Empty;
    [ObservableProperty] private string languageCode = string.Empty;
    [ObservableProperty] private string themeName = string.Empty;

    [ObservableProperty] private string rawToml = string.Empty;

    public ObservableCollection<ParamFieldItem> ParamFields { get; } = [];

    [RelayCommand]
    private void SelectSegment(string segment)
    {
        IsBasicSegmentSelected = segment == "basic";
        IsParamsSegmentSelected = segment == "params";
        IsRawSegmentSelected = segment == "raw";
    }

    [RelayCommand]
    private async Task AppearingAsync() => await LoadAsync();

    private async Task LoadAsync()
    {
        OnPropertyChanged(nameof(HasActiveSite));
        OnPropertyChanged(nameof(ActiveSiteName));

        var site = _activeSite.Current;
        if (site is null)
        {
            _configPath = null;
            _sha = null;
            RawToml = string.Empty;
            ParamFields.Clear();
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
            RawToml = file?.TextContent ?? string.Empty;
            _loadedFor = site;

            var (basicUrl, basicTitle, basicLang, basicTheme) = _tomlParams.ReadBasicFields(RawToml);
            BaseUrl = basicUrl;
            SiteTitle = basicTitle;
            LanguageCode = basicLang;
            ThemeName = basicTheme;

            ParamFields.Clear();
            foreach (var field in _tomlParams.LoadParamsForm(RawToml))
                ParamFields.Add(field);

            StatusMessage = file is null
                ? "找不到現有設定檔，儲存後將建立新的 hugo.toml。"
                : $"已載入 {_configPath}";
        });
    }

    [RelayCommand]
    private async Task SaveBasicAsync() => await SaveAsync(() =>
        _tomlParams.UpsertSimpleRootKeys(RawToml, new Dictionary<string, string>
        {
            ["baseURL"] = BaseUrl,
            ["title"] = SiteTitle,
            ["languageCode"] = LanguageCode,
            ["theme"] = ThemeName
        }));

    [RelayCommand]
    private async Task SaveParamsAsync() => await SaveAsync(() => _tomlParams.ApplyParamsToToml(RawToml, ParamFields));

    [RelayCommand]
    private async Task SaveRawAsync() => await SaveAsync(() => RawToml);

    private async Task SaveAsync(Func<string> buildToml) => await RunBusyAsync(async () =>
    {
        var site = _activeSite.Current;
        if (site is null || _configPath is null) return;

        var token = await _tokenStore.GetTokenAsync(site.TokenKey);
        if (string.IsNullOrEmpty(token))
        {
            ErrorMessage = "找不到此網站的權杖，請於「站台」重新連結。";
            return;
        }

        var updated = buildToml();
        var result = await _api.PutFileAsync(
            site.Owner, site.Repository, _configPath, updated,
            "Update Hugo config via Hugoer Mobile", _sha, site.DefaultBranch, token);

        _sha = result.Sha;
        RawToml = updated;
        ParamFields.Clear();
        foreach (var field in _tomlParams.LoadParamsForm(RawToml))
            ParamFields.Add(field);

        StatusMessage = "已儲存並推送到 GitHub。";
    });
}
