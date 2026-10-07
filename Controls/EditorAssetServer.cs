using System.Net;
using System.Net.Sockets;
using System.Text;

namespace Hugoer.Controls;

/// <summary>
/// Serves the bundled Quill page on 127.0.0.1 so every platform WebView can load it
/// as one origin. Only files under the editor directory are readable.
/// </summary>
internal sealed class EditorAssetServer : IDisposable
{
    private static readonly Dictionary<string, string> ContentTypes = new(StringComparer.OrdinalIgnoreCase)
    {
        [".html"] = "text/html; charset=utf-8",
        [".css"] = "text/css; charset=utf-8",
        [".js"] = "text/javascript; charset=utf-8",
        [".map"] = "application/json; charset=utf-8",
        [".txt"] = "text/plain; charset=utf-8",
        [".svg"] = "image/svg+xml",
        [".png"] = "image/png",
        [".jpg"] = "image/jpeg",
        [".jpeg"] = "image/jpeg",
        [".webp"] = "image/webp",
    };

    private readonly string _root;
    private readonly TcpListener _listener;
    private readonly CancellationTokenSource _cts = new();

    public EditorAssetServer(string editorRoot)
    {
        _root = Path.GetFullPath(editorRoot);
        _listener = new TcpListener(IPAddress.Loopback, 0);
        _listener.Start();
        var port = ((IPEndPoint)_listener.LocalEndpoint).Port;
        Port = port;
        BaseUri = new Uri($"http://127.0.0.1:{port}/");
        _ = Task.Run(AcceptLoop);
    }

    public int Port { get; }
    public Uri BaseUri { get; }

    public void Dispose()
    {
        _cts.Cancel();
        _listener.Stop();
    }

    private void AcceptLoop()
    {
        while (!_cts.IsCancellationRequested)
        {
            TcpClient client;
            try
            {
                client = _listener.AcceptTcpClient();
            }
            catch (Exception) when (_cts.IsCancellationRequested)
            {
                break;
            }
            catch (SocketException)
            {
                break;
            }

            ThreadPool.QueueUserWorkItem(static state =>
            {
                var (server, accepted) = ((EditorAssetServer, TcpClient))state!;
                server.Serve(accepted);
            }, (this, client));
        }
    }

    private void Serve(TcpClient client)
    {
        using (client)
        {
            client.ReceiveTimeout = 3000;
            client.SendTimeout = 5000;
            try
            {
                using var stream = client.GetStream();
                var request = ReadRequestLine(stream);
                if (request is null || !request.StartsWith("GET ", StringComparison.Ordinal))
                {
                    WriteStatus(stream, 405, "Method Not Allowed", "text/plain; charset=utf-8", "Method Not Allowed"u8);
                    return;
                }

                var path = request[4..];
                var space = path.IndexOf(' ');
                if (space >= 0)
                    path = path[..space];
                var query = path.IndexOf('?');
                if (query >= 0)
                    path = path[..query];

                if (!TryResolve(path, out var file))
                {
                    WriteStatus(stream, 404, "Not Found", "text/plain; charset=utf-8", "Not Found"u8);
                    return;
                }

                var bytes = File.ReadAllBytes(file);
                var contentType = ContentTypes.GetValueOrDefault(Path.GetExtension(file), "application/octet-stream");
                WriteStatus(stream, 200, "OK", contentType, bytes);
            }
            catch (Exception)
            {
                // The WebView closed the socket or the request was malformed.
            }
        }
    }

    private bool TryResolve(string requestPath, out string file)
    {
        file = string.Empty;
        var relative = Uri.UnescapeDataString(requestPath).Replace('\\', '/').TrimStart('/');
        if (relative.Length == 0)
            relative = "index.html";

        if (relative.Contains('\0', StringComparison.Ordinal) ||
            relative.Split('/').Any(segment => segment is ".." or ""))
            return false;

        var candidate = Path.GetFullPath(Path.Combine(_root, relative.Replace('/', Path.DirectorySeparatorChar)));
        var rootPrefix = _root.EndsWith(Path.DirectorySeparatorChar) ? _root : _root + Path.DirectorySeparatorChar;
        if (!candidate.StartsWith(rootPrefix, StringComparison.OrdinalIgnoreCase) || !File.Exists(candidate))
            return false;

        file = candidate;
        return true;
    }

    private static string? ReadRequestLine(NetworkStream stream)
    {
        var buffer = new byte[2048];
        var read = 0;
        while (read < buffer.Length)
        {
            var n = stream.Read(buffer, read, buffer.Length - read);
            if (n == 0)
                break;
            read += n;
            for (var i = 0; i < read - 1; i++)
            {
                if (buffer[i] == '\r' && buffer[i + 1] == '\n')
                    return Encoding.ASCII.GetString(buffer, 0, i);
            }
        }

        return null;
    }

    private static void WriteStatus(Stream stream, int status, string reason, string contentType, ReadOnlySpan<byte> body)
    {
        var header = Encoding.ASCII.GetBytes(
            $"HTTP/1.1 {status} {reason}\r\n" +
            $"Content-Type: {contentType}\r\n" +
            $"Content-Length: {body.Length}\r\n" +
            "Cache-Control: no-store\r\n" +
            "Connection: close\r\n" +
            "\r\n");
        stream.Write(header);
        stream.Write(body);
    }
}
