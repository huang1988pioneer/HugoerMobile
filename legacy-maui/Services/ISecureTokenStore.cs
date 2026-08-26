namespace HugoerMobile.Services;

/// <summary>Persists a GitHub Personal Access Token per site using the platform keychain/keystore.</summary>
public interface ISecureTokenStore
{
    Task SaveTokenAsync(string key, string token);
    Task<string?> GetTokenAsync(string key);
    Task RemoveTokenAsync(string key);
}
