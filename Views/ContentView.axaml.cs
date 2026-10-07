using Avalonia.Controls;
using Avalonia.Input;
using Hugoer.ViewModels;

namespace Hugoer.Views;

public partial class ContentView : UserControl
{
    public ContentView()
    {
        InitializeComponent();
    }

    private void ContentView_OnKeyDown(object? sender, KeyEventArgs e)
    {
        var save = e.Key == Key.S
                   && (e.KeyModifiers.HasFlag(KeyModifiers.Control) || e.KeyModifiers.HasFlag(KeyModifiers.Meta));
        if (!save)
            return;

        SaveCurrent();
        e.Handled = true;
    }

    private void Editor_OnSaveRequested(object? sender, EventArgs e) => SaveCurrent();

    private void SaveCurrent()
    {
        if (DataContext is ContentViewModel viewModel && viewModel.SaveCommand.CanExecute(null))
            viewModel.SaveCommand.Execute(null);
    }
}
