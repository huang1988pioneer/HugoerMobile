using HugoerMobile.Views;

namespace HugoerMobile;

public partial class AppShell : Shell
{
    public AppShell()
    {
        InitializeComponent();

        // Routes for pages pushed on top of the tabs (not part of the bottom bar itself).
        Routing.RegisterRoute(nameof(ConnectSitePage), typeof(ConnectSitePage));
        Routing.RegisterRoute(nameof(ContentEditorPage), typeof(ContentEditorPage));
        Routing.RegisterRoute(nameof(SettingsPage), typeof(SettingsPage));
    }
}
