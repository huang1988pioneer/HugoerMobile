using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Hugoer.Services;

namespace Hugoer.ViewModels;

public partial class MainViewModel : ViewModelBase
{
    public MainViewModel()
    {
        SetupPage = new SetupViewModel();
        ConfigPage = new ConfigViewModel();
        ThemesPage = new ThemesViewModel();
        ContentPage = new ContentViewModel();
        GitHubPage = new GitHubViewModel();

        NavItems =
        [
            new NavItem("環境", "安裝 Hugo、建立新站或開啟既有網站，並在本機預覽與建置。", SetupPage),
            new NavItem("設定檔", "網站基本欄位、Hugo 進階設定、主題 params，以及原始 TOML。", ConfigPage),
            new NavItem("主題", "安裝熱門主題（推薦 Stack），並編輯主題與站點設定。", ThemesPage),
            new NavItem("內容", "瀏覽 content/，以 Quill 撰寫內文並即時預覽。儲存仍是 Markdown。", ContentPage),
            new NavItem("GitHub", "推上 GitHub，用 Actions 部署 Pages，並查看上線狀態。", GitHubPage),
        ];

        SelectedNav = NavItems[0];
        AppServices.Instance.SiteChanged += (_, _) => UpdateSiteBanner();
        UpdateSiteBanner();
        _ = SelectedNav.Page.OnNavigatedToAsync();
    }

    public ObservableCollection<NavItem> NavItems { get; }

    public SetupViewModel SetupPage { get; }
    public ConfigViewModel ConfigPage { get; }
    public ThemesViewModel ThemesPage { get; }
    public ContentViewModel ContentPage { get; }
    public GitHubViewModel GitHubPage { get; }

    [ObservableProperty]
    public partial NavItem? SelectedNav { get; set; }

    [ObservableProperty]
    public partial PageViewModelBase? CurrentPage { get; set; }

    [ObservableProperty]
    public partial string SiteBanner { get; set; } = "尚未選擇網站";

    [ObservableProperty]
    public partial string AppStatus { get; set; } = "就緒";

    partial void OnSelectedNavChanged(NavItem? value)
    {
        if (value is null) return;
        CurrentPage = value.Page;
        AppStatus = value.Page.StatusMessage;
        _ = value.Page.OnNavigatedToAsync();
    }

    [RelayCommand]
    private void GoToSetup() => SelectedNav = NavItems[0];

    private void UpdateSiteBanner()
    {
        var path = AppServices.Instance.CurrentSitePath;
        SiteBanner = string.IsNullOrWhiteSpace(path)
            ? "尚未選擇。請到「環境」開啟或建立。"
            : path;
    }
}

public sealed class NavItem(string title, string description, PageViewModelBase page)
{
    public string Title { get; } = title;
    public string Description { get; } = description;
    public PageViewModelBase Page { get; } = page;
}
