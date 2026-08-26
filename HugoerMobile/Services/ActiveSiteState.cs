using HugoerMobile.Models;

namespace HugoerMobile.Services;

/// <summary>
/// Holds the currently selected Hugo site so the 設定檔/主題/內容/GitHub tabs can operate on it
/// without threading a "current site" parameter through every Shell navigation.
/// </summary>
public sealed class ActiveSiteState
{
    public SiteProfile? Current { get; private set; }

    public event EventHandler? Changed;

    public void Set(SiteProfile? site)
    {
        Current = site;
        Changed?.Invoke(this, EventArgs.Empty);
    }
}
