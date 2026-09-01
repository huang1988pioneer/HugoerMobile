import Foundation
import SwiftUI

/// Holds the list of connected sites and which one is currently active, so every tab
/// can operate on "the active site" without threading it through navigation.
@MainActor
final class AppState: ObservableObject {
    @Published var sites: [SiteProfile] = []
    let activeSiteState = ActiveSiteState()

    @Published var activeSiteId: String? {
        didSet {
            UserDefaults.standard.set(activeSiteId, forKey: Self.activeSiteKey)
        }
    }

    private static let activeSiteKey = "hugoer.activeSiteId"

    var activeSite: SiteProfile? {
        guard let activeSiteId else { return nil }
        return sites.first { $0.id == activeSiteId }
    }

    init() {
        sites = SiteProfileStore.load()
        activeSiteId = UserDefaults.standard.string(forKey: Self.activeSiteKey)
        if activeSiteId == nil || !sites.contains(where: { $0.id == activeSiteId }) {
            activeSiteId = sites.first?.id
        }
        activeSiteState.set(activeSite)
    }

    func addSite(_ site: SiteProfile, token: String) {
        var profile = site
        if let index = sites.firstIndex(where: {
            $0.owner.caseInsensitiveCompare(site.owner) == .orderedSame &&
            $0.repository.caseInsensitiveCompare(site.repository) == .orderedSame
        }) {
            profile.id = sites[index].id
            profile.addedAtUtc = sites[index].addedAtUtc
            sites[index] = profile
        } else {
            sites.append(profile)
        }
        KeychainTokenStore.saveToken(key: profile.tokenKey, token: token)
        persist()
        activeSiteId = profile.id
        activeSiteState.set(profile)
    }

    func removeSite(_ site: SiteProfile) {
        sites.removeAll { $0.id == site.id }
        KeychainTokenStore.removeToken(key: site.tokenKey)
        persist()
        if activeSiteId == site.id {
            activeSiteId = sites.first?.id
        }
        activeSiteState.set(activeSite)
    }

    func setActive(_ site: SiteProfile) {
        activeSiteId = site.id
        activeSiteState.set(site)
    }

    func token(for site: SiteProfile) -> String? {
        KeychainTokenStore.getToken(key: site.tokenKey)
    }

    func clearAll() {
        for site in sites {
            KeychainTokenStore.removeToken(key: site.tokenKey)
        }
        sites.removeAll()
        activeSiteId = nil
        activeSiteState.set(nil)
        persist()
    }

    private func persist() {
        SiteProfileStore.save(sites)
    }
}
