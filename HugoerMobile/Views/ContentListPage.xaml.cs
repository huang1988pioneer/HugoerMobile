using HugoerMobile.ViewModels;

namespace HugoerMobile.Views;

public partial class ContentListPage : ContentPage
{
    private readonly ContentListViewModel _viewModel;

    public ContentListPage(ContentListViewModel viewModel)
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
