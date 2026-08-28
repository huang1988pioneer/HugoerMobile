using HugoerMobile.ViewModels;

namespace HugoerMobile.Views;

public partial class ActionsPage : ContentPage
{
    private readonly ActionsViewModel _viewModel;

    public ActionsPage(ActionsViewModel viewModel)
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
