import SwiftUI

struct RootTabView: View {
    @EnvironmentObject private var appState: AppState

    var body: some View {
        TabView {
            SitesView()
                .tabItem { Label("站台", systemImage: "server.rack") }

            ConfigView()
                .tabItem { Label("設定檔", systemImage: "gearshape.2") }

            ThemesView()
                .tabItem { Label("主題", systemImage: "paintbrush") }

            ContentBrowserView()
                .tabItem { Label("內容", systemImage: "doc.text") }

            GitHubView()
                .tabItem { Label("GitHub", systemImage: "arrow.triangle.2.circlepath") }
        }
    }
}

#Preview {
    RootTabView().environmentObject(AppState())
}
