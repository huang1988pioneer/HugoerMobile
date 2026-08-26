using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using HugoerMobile.Models;
using HugoerMobile.Services;

namespace HugoerMobile.ViewModels;

/// <summary>
/// "新增網站" wizard: parses an owner/repo (or GitHub URL) reference, validates the PAT
/// against the GitHub API (mirrors desktop's `CheckPushAccessAsync`), then persists the
/// site profile and stores the token securely.
/// </summary>
public sealed partial class ConnectSiteViewModel : BaseViewModel
{
    private readonly IGitHubApiService _api;
    private readonly ISiteProfileStore _siteStore;
    private readonly ISecureTokenStore _tokenStore;
    private readonly ActiveSiteState _activeSite;

    public ConnectSiteViewModel(
        IGitHubApiService api,
        ISiteProfileStore siteStore,
        ISecureTokenStore tokenStore,
        ActiveSiteState activeSite)
    {
        _api = api;
        _siteStore = siteStore;
        _tokenStore = tokenStore;
        _activeSite = activeSite;
    }

    [ObservableProperty]
    private string repositoryInput = string.Empty;

    [ObservableProperty]
    private string tokenInput = string.Empty;

    [ObservableProperty]
    private string verificationSummary = string.Empty;

    [ObservableProperty]
    private bool isVerified;

    private GitHubRepositoryTarget? _target;
    private GitHubRepoInfo? _repoInfo;
    private string? _login;

    [RelayCommand]
    private async Task VerifyAsync() => await RunBusyAsync(async () =>
    {
        IsVerified = false;
        VerificationSummary = string.Empty;

        var target = GitHubRepositoryTarget.Parse(RepositoryInput);
        if (!target.IsValid)
        {
            ErrorMessage = target.ErrorMessage;
            return;
        }

        if (string.IsNullOrWhiteSpace(TokenInput))
        {
            ErrorMessage = "請輸入 GitHub Personal Access Token。";
            return;
        }

        var login = await _api.GetAuthenticatedUserLoginAsync(TokenInput);
        if (login is null)
        {
            ErrorMessage = "權杖無效或已過期，請重新產生後再試一次。";
            return;
        }

        var repoInfo = await _api.GetRepoInfoAsync(target.Owner, target.Repository, TokenInput);
        if (repoInfo is null)
        {
            ErrorMessage = $"找不到 repository {target.Owner}/{target.Repository}，或此權杖沒有存取權限。";
            return;
        }

        _target = target;
        _repoInfo = repoInfo;
        _login = login;
        IsVerified = true;
        VerificationSummary =
            $"已登入為 {login}\n" +
            $"權限：{(repoInfo.Permissions.Admin ? "管理員" : repoInfo.Permissions.Push ? "可推送" : "唯讀")}\n" +
            $"預設分支：{repoInfo.DefaultBranch}\n" +
            $"Pages 網址：{target.PagesUrl}";
    });

    [RelayCommand]
    private async Task SaveAsync() => await RunBusyAsync(async () =>
    {
        if (!IsVerified || _target is null || _repoInfo is null)
        {
            ErrorMessage = "請先驗證連線。";
            return;
        }

        var profile = new SiteProfile
        {
            Owner = _target.Owner,
            Repository = _target.Repository,
            DisplayName = _target.Repository,
            DefaultBranch = _repoInfo.DefaultBranch,
            PagesUrl = _target.PagesUrl,
            IsUserOrOrganizationSite = _target.IsUserOrOrganizationSite
        };

        await _tokenStore.SaveTokenAsync(profile.TokenKey, TokenInput);

        var profiles = await _siteStore.LoadAsync();
        profiles.Add(profile);
        await _siteStore.SaveAsync(profiles);

        _activeSite.Set(profile);
        await Shell.Current.GoToAsync("..");
    });
}
