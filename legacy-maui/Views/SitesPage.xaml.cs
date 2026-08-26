using HugoerMobile.ViewModels;

namespace HugoerMobile.Views;

public partial class SitesPage : ContentPage
{
    private readonly SitesViewModel _viewModel;

    public SitesPage(SitesViewModel viewModel)
    {
        InitializeComponent();
        BindingContext = _viewModel = viewModel;
    }

    protected override void OnAppearing()
    {
        base.OnAppearing();
        _viewModel.AppearingCommand.Execute(null);
    }
}
