using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using HugoerMobile.Models;
using HugoerMobile.Services;
using HugoerMobile.Views;

namespace HugoerMobile.ViewModels;

public sealed partial class SiteCardViewModel(SiteProfile profile) : ObservableObject
{
    public SiteProfile Profile { get; } = profile;

    public string FullName => Profile.FullName;
    public string PagesUrl => Profile.PagesUrl;

    [ObservableProperty]
    private bool isActive;
}

/// <summary>
/// Mobile equivalent of the desktop "環境" tab: instead of installing Hugo locally,
/// this manages the list of GitHub-backed Hugo sites connected to the device and which
/// one is currently active for the 設定檔/主題/內容/GitHub tabs.
/// </summary>
public sealed partial class SitesViewModel : BaseViewModel
{
    private readonly ISiteProfileStore _siteStore;
    private readonly ActiveSiteState _activeSite;

    public ObservableCollection<SiteCardViewModel> Sites { get; } = [];

    [ObservableProperty]
    private bool hasNoSites;

    public SitesViewModel(ISiteProfileStore siteStore, ActiveSiteState activeSite)
    {
        _siteStore = siteStore;
        _activeSite = activeSite;
        _activeSite.Changed += (_, _) => RefreshActiveMarkers();
    }

    [RelayCommand]
    private async Task AppearingAsync() => await RunBusyAsync(async () =>
    {
        var profiles = await _siteStore.LoadAsync();
        Sites.Clear();
        foreach (var profile in profiles)
            Sites.Add(new SiteCardViewModel(profile));

        HasNoSites = Sites.Count == 0;
        RefreshActiveMarkers();

        // Auto-activate the only site, or keep the previous selection if still present.
        if (_activeSite.Current is null && Sites.Count > 0)
            SelectSite(Sites[0]);
    });

    [RelayCommand]
    private void SelectSite(SiteCardViewModel site)
    {
        _activeSite.Set(site.Profile);
        RefreshActiveMarkers();
    }

    [RelayCommand]
    private async Task AddSiteAsync() => await Shell.Current.GoToAsync(nameof(ConnectSitePage));

    [RelayCommand]
    private async Task RemoveSiteAsync(SiteCardViewModel site)
    {
        var confirmed = await Shell.Current.CurrentPage.DisplayAlert(
            "移除網站", $"確定要移除「{site.FullName}」嗎？（不會刪除 GitHub 上的內容，僅移除本機連結與已儲存的權杖）", "移除", "取消");
        if (!confirmed) return;

        await RunBusyAsync(async () =>
        {
            var profiles = await _siteStore.LoadAsync();
            profiles.RemoveAll(p => p.Id == site.Profile.Id);
            await _siteStore.SaveAsync(profiles);
            Sites.Remove(site);
            HasNoSites = Sites.Count == 0;

            if (_activeSite.Current?.Id == site.Profile.Id)
                _activeSite.Set(Sites.Count > 0 ? Sites[0].Profile : null);
        });
    }

    [RelayCommand]
    private async Task OpenSettingsAsync() => await Shell.Current.GoToAsync(nameof(SettingsPage));

    private void RefreshActiveMarkers()
    {
        foreach (var site in Sites)
            site.IsActive = _activeSite.Current?.Id == site.Profile.Id;
    }
}
