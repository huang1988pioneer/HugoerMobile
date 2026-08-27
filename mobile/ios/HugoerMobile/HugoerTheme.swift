import SwiftUI

enum HugoerPalette {
    static let cyan = Color(red: 0.22, green: 0.48, blue: 1.0)
    static let cyanSoft = Color(red: 0.75, green: 0.82, blue: 1.0)
    static let amber = Color(red: 0.98, green: 0.68, blue: 0.30)
    static let graphite = Color(uiColor: .systemBackground)
    static let surface = Color(uiColor: .secondarySystemBackground)
    static let surfaceElevated = Color(uiColor: .tertiarySystemBackground)
}

struct HugoerCardModifier: ViewModifier {
    func body(content: Content) -> some View {
        content
            .padding(16)
            .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
    }
}

extension View {
    func hugoerCard() -> some View { modifier(HugoerCardModifier()) }
}
