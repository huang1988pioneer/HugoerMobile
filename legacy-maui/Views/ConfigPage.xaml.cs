using HugoerMobile.ViewModels;

namespace HugoerMobile.Views;

public partial class ConfigPage : ContentPage
{
    private readonly ConfigViewModel _viewModel;

    public ConfigPage(ConfigViewModel viewModel)
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
