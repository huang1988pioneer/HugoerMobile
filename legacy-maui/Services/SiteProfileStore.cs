using System.Text.Json;
using HugoerMobile.Models;

namespace HugoerMobile.Services;

/// <summary>
/// Stores connected <see cref="SiteProfile"/> entries as a small JSON file in the app's
/// sandboxed data directory. PATs themselves never go here — see <see cref="ISecureTokenStore"/>.
/// </summary>
public sealed class SiteProfileStore : ISiteProfileStore
{
    private static string FilePath => Path.Combine(FileSystem.AppDataDirectory, "sites.json");
    private static readonly JsonSerializerOptions JsonOptions = new() { WriteIndented = true };

    public async Task<List<SiteProfile>> LoadAsync()
    {
        try
        {
            if (!File.Exists(FilePath))
                return [];

            await using var stream = File.OpenRead(FilePath);
            var profiles = await JsonSerializer.DeserializeAsync<List<SiteProfile>>(stream);
            return profiles ?? [];
        }
        catch
        {
            return [];
        }
    }

    public async Task SaveAsync(List<SiteProfile> profiles)
    {
        await using var stream = File.Create(FilePath);
        await JsonSerializer.SerializeAsync(stream, profiles, JsonOptions);
    }
}
