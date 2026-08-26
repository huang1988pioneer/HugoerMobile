using HugoerMobile.ViewModels;

namespace HugoerMobile.Views;

public partial class ThemesPage : ContentPage
{
    private readonly ThemesViewModel _viewModel;

    public ThemesPage(ThemesViewModel viewModel)
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
