import SwiftUI
import UIKit

enum HugoerColor {
    /// Copper in dark mode, deeper copper in light mode. Same brand as desktop and Android.
    static let accent = Color(uiColor: UIColor { traits in
        traits.userInterfaceStyle == .dark
            ? UIColor(red: 232 / 255, green: 161 / 255, blue: 90 / 255, alpha: 1)
            : UIColor(red: 140 / 255, green: 78 / 255, blue: 26 / 255, alpha: 1)
    })
}

@main
struct HugoerMobileApp: App {
    @StateObject private var appState = AppState()

    var body: some Scene {
        WindowGroup {
            RootTabView()
                .environmentObject(appState)
                .tint(HugoerColor.accent)
        }
    }
}
