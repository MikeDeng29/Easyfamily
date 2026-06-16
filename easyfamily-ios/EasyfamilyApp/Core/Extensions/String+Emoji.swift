import Foundation

extension String {
    /// Strips Unicode Supplementary Multilingual Plane characters (U+10000 and above),
    /// which includes most emoji. Prevents [?] tofu boxes on platforms that lack
    /// SMP glyph support. BMP symbols (✓ ～ etc.) are preserved.
    var strippingSMPEmoji: String {
        unicodeScalars
            .filter { $0.value < 0x10000 && $0.value != 0xFFFD }
            .reduce(into: "") { $0 += String($1) }
    }
}
