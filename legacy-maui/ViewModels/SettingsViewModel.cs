using System.Collections.ObjectModel;
using System.Reflection;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using HugoerMobile.Services;

namespace HugoerMobile.ViewModels;

public sealed partial class SettingsViewModel : BaseViewModel
{
    private readonly ISiteProfileStore _siteStore;
    private readonly ISecureTokenStore _tokenStore;
    private readonly ActiveSiteState _activeSite;

    public SettingsViewModel(ISiteProfileStore siteStore, ISecureTokenStore tokenStore, ActiveSiteState activeSite)
    {
        _siteStore = siteStore;
        _tokenStore = tokenStore;
        _activeSite = activeSite;
    }

    public ObservableCollection<string> ConnectedSites { get; } = [];

    public string AppVersion =>
        Assembly.GetExecutingAssembly().GetName().Version?.ToString(3) ?? "1.0.0";

    [RelayCommand]
    private async Task AppearingAsync() => await RunBusyAsync(async () =>
    {
        var profiles = await _siteStore.LoadAsync();
        ConnectedSites.Clear();
        foreach (var p in profiles)
            ConnectedSites.Add(p.FullName);
    });

    [RelayCommand]
    private async Task ClearAllTokensAsync()
    {
        var confirmed = await Shell.Current.CurrentPage.DisplayAlert(
            "清除所有已連結網站", "將移除本機所有 Hugo 網站連結與已儲存的 GitHub 權杖。此動作無法復原。", "清除", "取消");
        if (!confirmed) return;

        await RunBusyAsync(async () =>
        {
            var profiles = await _siteStore.LoadAsync();
            foreach (var profile in profiles)
                await _tokenStore.RemoveTokenAsync(profile.TokenKey);

            await _siteStore.SaveAsync([]);
            _activeSite.Set(null);
            ConnectedSites.Clear();
            StatusMessage = "已清除所有網站與權杖。";
        });
    }
}
