using System.Globalization;

namespace HugoerMobile.Converters;

public sealed class HtmlToWebViewSourceConverter : IValueConverter
{
    public object Convert(object? value, Type targetType, object? parameter, CultureInfo culture) =>
        new HtmlWebViewSource { Html = value as string ?? string.Empty };

    public object ConvertBack(object? value, Type targetType, object? parameter, CultureInfo culture) =>
        throw new NotSupportedException();
}
