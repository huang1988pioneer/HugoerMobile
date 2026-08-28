import SwiftUI

struct RootTabView: View {
    @EnvironmentObject private var appState: AppState

    var body: some View {
        TabView {
            SitesView()
                .tabItem { Label("站台", systemImage: "cloud.fill") }

            ConfigView()
                .tabItem { Label("設定檔", systemImage: "slider.horizontal.3") }

            ThemesView()
                .tabItem { Label("主題", systemImage: "paintpalette.fill") }

            ContentBrowserView()
                .tabItem { Label("內容", systemImage: "doc.text.fill") }

            GitHubView()
                .tabItem { Label("GitHub", systemImage: "chevron.left.forwardslash.chevron.right") }
        }
    }
}

#Preview {
    RootTabView().environmentObject(AppState())
}
