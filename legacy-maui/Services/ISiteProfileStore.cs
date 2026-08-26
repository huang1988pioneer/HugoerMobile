using HugoerMobile.Models;

namespace HugoerMobile.Services;

/// <summary>Persists the (non-secret) list of Hugo sites the user has connected on this device.</summary>
public interface ISiteProfileStore
{
    Task<List<SiteProfile>> LoadAsync();
    Task SaveAsync(List<SiteProfile> profiles);
}
