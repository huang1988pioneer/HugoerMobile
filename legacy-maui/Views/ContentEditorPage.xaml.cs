using HugoerMobile.ViewModels;

namespace HugoerMobile.Views;

public partial class ContentEditorPage : ContentPage
{
    public ContentEditorPage(ContentEditorViewModel viewModel)
    {
        InitializeComponent();
        BindingContext = viewModel;
    }
}
