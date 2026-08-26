import Foundation

/// Stores connected `SiteProfile` entries as a small JSON file in the app's sandboxed
/// Application Support directory. PATs themselves never go here — see `KeychainTokenStore`.
enum SiteProfileStore {
    private static var fileURL: URL {
        let dir = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        if !FileManager.default.fileExists(atPath: dir.path) {
            try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        }
        return dir.appendingPathComponent("sites.json")
    }

    static func load() -> [SiteProfile] {
        guard let data = try? Data(contentsOf: fileURL) else { return [] }
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        return (try? decoder.decode([SiteProfile].self, from: data)) ?? []
    }

    static func save(_ profiles: [SiteProfile]) {
        let encoder = JSONEncoder()
        encoder.outputFormatting = .prettyPrinted
        encoder.dateEncodingStrategy = .iso8601
        guard let data = try? encoder.encode(profiles) else { return }
        try? data.write(to: fileURL, options: .atomic)
    }
}
