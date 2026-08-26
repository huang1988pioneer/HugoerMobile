namespace HugoerMobile.Services;

/// <summary>
/// Wraps <see cref="SecureStorage"/> (Android Keystore / iOS Keychain) so GitHub PATs
/// are never written to plain preferences or disk files.
/// </summary>
public sealed class SecureTokenStore : ISecureTokenStore
{
    public Task SaveTokenAsync(string key, string token) => SecureStorage.Default.SetAsync(key, token);

    public Task<string?> GetTokenAsync(string key) => SecureStorage.Default.GetAsync(key);

    public Task RemoveTokenAsync(string key)
    {
        SecureStorage.Default.Remove(key);
        return Task.CompletedTask;
    }
}
