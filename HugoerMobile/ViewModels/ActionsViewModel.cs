using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using HugoerMobile.Models;
using HugoerMobile.Services;

namespace HugoerMobile.ViewModels;

/// <summary>
/// Mobile GitHub tab: repo permissions, Actions run history + manual "立即部署" trigger,
/// and GitHub Pages status — the mobile replacement for desktop's `gh`-CLI-driven GitHub tab.
/// </summary>
public sealed partial class ActionsViewModel : BaseViewModel
{
    private readonly IGitHubApiService _api;
    private readonly ISecureTokenStore _tokenStore;
    private readonly ActiveSiteState _activeSite;

    public ActionsViewModel(IGitHubApiService api, ISecureTokenStore tokenStore, ActiveSiteState activeSite)
    {
        _api = api;
        _tokenStore = tokenStore;
        _activeSite = activeSite;
        _activeSite.Changed += async (_, _) => await LoadAsync();
    }

    public bool HasActiveSite => _activeSite.Current is not null;

    [ObservableProperty] private string permissionSummary = string.Empty;
    [ObservableProperty] private string pagesMessage = string.Empty;
    [ObservableProperty] private string? pagesUrl;
    [ObservableProperty] private bool pagesEnabled;

    public ObservableCollection<WorkflowRunInfo> Runs { get; } = [];

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
            Runs.Clear();
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

            var repoInfo = await _api.GetRepoInfoAsync(site.Owner, site.Repository, token);
            PermissionSummary = repoInfo is null
                ? "無法讀取權限"
                : repoInfo.Permissions.Admin ? "管理員權限" : repoInfo.Permissions.Push ? "可推送內容" : "唯讀權限";

            var pages = await _api.GetPagesStatusAsync(site.Owner, site.Repository, token);
            PagesEnabled = pages.Enabled;
            PagesMessage = pages.Message;
            PagesUrl = pages.HtmlUrl ?? site.PagesUrl;

            var runs = await _api.ListWorkflowRunsAsync(site.Owner, site.Repository, token);
            Runs.Clear();
            foreach (var run in runs)
                Runs.Add(run);
        });
    }

    [RelayCommand]
    private async Task DeployNowAsync() => await RunBusyAsync(async () =>
    {
        var site = _activeSite.Current;
        if (site is null) return;

        var token = await _tokenStore.GetTokenAsync(site.TokenKey);
        if (string.IsNullOrEmpty(token))
        {
            ErrorMessage = "找不到此網站的權杖，請於「站台」重新連結。";
            return;
        }

        await _api.EnsureDeployWorkflowAsync(site.Owner, site.Repository, site.DefaultBranch, token);

        var workflows = await _api.ListWorkflowsAsync(site.Owner, site.Repository, token);
        var deployWorkflow = workflows.FirstOrDefault(w =>
            w.Path.EndsWith("hugo.yml", StringComparison.OrdinalIgnoreCase)) ?? workflows.FirstOrDefault();

        if (deployWorkflow is null)
        {
            ErrorMessage = "找不到可觸發的 GitHub Actions workflow。";
            return;
        }

        await _api.DispatchWorkflowAsync(site.Owner, site.Repository, deployWorkflow.Id, site.DefaultBranch, token);
        StatusMessage = "已觸發部署，稍後下拉重新整理查看進度。";
        await Task.Delay(1500);
        await LoadAsync();
    });

    [RelayCommand]
    private async Task EnablePagesAsync() => await RunBusyAsync(async () =>
    {
        var site = _activeSite.Current;
        if (site is null) return;

        var token = await _tokenStore.GetTokenAsync(site.TokenKey);
        if (string.IsNullOrEmpty(token)) return;

        var (success, message) = await _api.EnablePagesFromActionsAsync(site.Owner, site.Repository, token);
        StatusMessage = message;
        if (success)
            await LoadAsync();
        else
            ErrorMessage = message;
    });

    [RelayCommand]
    private async Task OpenSiteAsync()
    {
        if (!string.IsNullOrWhiteSpace(PagesUrl))
            await Launcher.Default.OpenAsync(PagesUrl);
    }

    [RelayCommand]
    private async Task OpenRunAsync(WorkflowRunInfo run)
    {
        if (!string.IsNullOrWhiteSpace(run.HtmlUrl))
            await Launcher.Default.OpenAsync(run.HtmlUrl);
    }
}
