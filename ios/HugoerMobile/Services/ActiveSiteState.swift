import Foundation

/// Holds the currently selected Hugo site so the 設定檔/主題/內容/GitHub tabs can operate on it
/// without threading a "current site" parameter through every navigation push.
@MainActor
final class ActiveSiteState: ObservableObject {
    @Published private(set) var current: SiteProfile?

    func set(_ site: SiteProfile?) {
        current = site
    }
}
