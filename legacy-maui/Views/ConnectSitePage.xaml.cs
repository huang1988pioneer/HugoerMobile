using HugoerMobile.ViewModels;

namespace HugoerMobile.Views;

public partial class ConnectSitePage : ContentPage
{
    public ConnectSitePage(ConnectSiteViewModel viewModel)
    {
        InitializeComponent();
        BindingContext = viewModel;
    }
}
