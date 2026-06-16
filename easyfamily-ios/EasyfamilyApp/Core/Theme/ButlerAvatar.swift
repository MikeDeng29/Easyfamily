import SwiftUI

/// Maps the 8 predefined butler avatar IDs (1-8, from the backend's
/// `butlerAvatarId`) to SF Symbols and accent colors for display.
enum ButlerAvatar {
    static let allIds: [Int] = Array(1...8)

    static func icon(for id: Int) -> String {
        switch id {
        case 1: return "sparkles"
        case 2: return "heart.fill"
        case 3: return "star.fill"
        case 4: return "leaf.fill"
        case 5: return "moon.stars.fill"
        case 6: return "sun.max.fill"
        case 7: return "pawprint.fill"
        case 8: return "crown.fill"
        default: return "sparkles"
        }
    }

    static func color(for id: Int) -> Color {
        switch id {
        case 1: return AppPalette.violet
        case 2: return AppPalette.coral
        case 3: return AppPalette.amber
        case 4: return Color(hex: 0x4CAF50)
        case 5: return Color(hex: 0x5C6BC0)
        case 6: return Color(hex: 0xFF9800)
        case 7: return Color(hex: 0x8D6E63)
        case 8: return Color(hex: 0xEC407A)
        default: return AppPalette.violet
        }
    }
}

/// Maps `butlerPersona` values to display labels and descriptions.
enum ButlerPersona {
    static let all: [String] = ["warm", "strict", "humorous"]

    static func label(for persona: String) -> String {
        switch persona {
        case "strict": return "严谨高效"
        case "humorous": return "幽默风趣"
        default: return "温暖贴心"
        }
    }

    static func description(for persona: String) -> String {
        switch persona {
        case "strict": return "回复简洁有条理，专注效率"
        case "humorous": return "聊天轻松活泼，偶尔卖个萌"
        default: return "语气友好温暖，适合家庭场景"
        }
    }
}
