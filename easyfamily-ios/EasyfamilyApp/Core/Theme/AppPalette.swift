import SwiftUI

enum AppPalette {
    static let coral = Color(hex: 0xFF7B5C)
    static let coralDark = Color(hex: 0xE8684A)
    static let violet = Color(hex: 0x8B5CF6)
    static let violetDark = Color(hex: 0x7C3AED)
    static let amber = Color(hex: 0xFFBF4D)

    static let background = Color(hex: 0xFFFBFA)
    static let surface = Color(hex: 0xFFFFFF)
    static let softCoral = Color(hex: 0xFFF0EB)
    static let softViolet = Color(hex: 0xF3EEFF)
    static let softAmber = Color(hex: 0xFFF6E3)
    static let disabledSurface = Color(hex: 0xF0EDF2)

    static let textPrimary = Color(hex: 0x2D2535)
    static let textSecondary = Color(hex: 0x8A7F92)
    static let textOnPrimary = Color(hex: 0xFFFFFF)

    static let green = Color(hex: 0x2E7D32)
    static let softGreen = Color(hex: 0xE8F5E9)

    static let success = Color(hex: 0x4CAF50)
    static let error = Color(hex: 0xE53935)

    static let bubbleUser = Color(hex: 0xF3EEFF)
    static let bubbleAi = Color(hex: 0xFFF0EB)
}

extension Color {
    init(hex: UInt32) {
        let r = Double((hex >> 16) & 0xFF) / 255.0
        let g = Double((hex >> 8) & 0xFF) / 255.0
        let b = Double(hex & 0xFF) / 255.0
        self.init(red: r, green: g, blue: b)
    }
}
