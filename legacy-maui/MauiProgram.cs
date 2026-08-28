using CommunityToolkit.Maui;
using HugoerMobile.Services;
using HugoerMobile.ViewModels;
using HugoerMobile.Views;
using Microsoft.Extensions.Logging;

namespace HugoerMobile;

public static class MauiProgram
{
    public static MauiApp CreateMauiApp()
    {
        var builder = MauiApp.CreateBuilder();
        builder
            .UseMauiApp<App>()
            .UseMauiCommunityToolkit()
            .ConfigureFonts(fonts =>
            {
                fonts.AddFont("OpenSans-Regular.ttf", "OpenSansRegular");
                fonts.AddFont("OpenSans-Semibold.ttf", "OpenSansSemibold");
            });

#if DEBUG
        builder.Logging.AddDebug();
#endif

        // HttpClient used by GitHubApiService.
        builder.Services.AddSingleton(_ =>
        {
            var client = new HttpClient
            {
                BaseAddress = new Uri("https://api.github.com/"),
                Timeout = TimeSpan.FromSeconds(30)
            };
            client.DefaultRequestHeaders.UserAgent.ParseAdd("HugoerMobile/1.0");
            client.DefaultRequestHeaders.Accept.ParseAdd("application/vnd.github+json");
            client.DefaultRequestHeaders.Add("X-GitHub-Api-Version", "2022-11-28");
            return client;
        });

        // Cross-cutting services (mirrors desktop Services/ folder, adapted for mobile).
        builder.Services.AddSingleton<IGitHubApiService, GitHubApiService>();
        builder.Services.AddSingleton<ISecureTokenStore, SecureTokenStore>();
        builder.Services.AddSingleton<ISiteProfileStore, SiteProfileStore>();
        builder.Services.AddSingleton<ActiveSiteState>();
        builder.Services.AddSingleton<ThemeCatalogService>();
        builder.Services.AddSingleton<TomlParamsService>();
        builder.Services.AddSingleton<FrontMatterService>();
        builder.Services.AddSingleton<MarkdownPreviewService>();
        builder.Services.AddSingleton<ConfigLocator>();

        // ViewModels
        builder.Services.AddTransient<SitesViewModel>();
        builder.Services.AddTransient<ConnectSiteViewModel>();
        builder.Services.AddTransient<ConfigViewModel>();
        builder.Services.AddTransient<ThemesViewModel>();
        builder.Services.AddTransient<ContentListViewModel>();
        builder.Services.AddTransient<ContentEditorViewModel>();
        builder.Services.AddTransient<ActionsViewModel>();
        builder.Services.AddTransient<SettingsViewModel>();

        // Views
        builder.Services.AddTransient<SitesPage>();
        builder.Services.AddTransient<ConnectSitePage>();
        builder.Services.AddTransient<ConfigPage>();
        builder.Services.AddTransient<ThemesPage>();
        builder.Services.AddTransient<ContentListPage>();
        builder.Services.AddTransient<ContentEditorPage>();
        builder.Services.AddTransient<ActionsPage>();
        builder.Services.AddTransient<SettingsPage>();

        return builder.Build();
    }
}
