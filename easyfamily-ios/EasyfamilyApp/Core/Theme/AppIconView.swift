import SwiftUI

/// Easyfamily app icon — "环抱之家".
/// Use AppIconView(size: 1024) to render the production asset.
struct AppIconView: View {
    let size: CGFloat

    private var arc: CGFloat { size * 0.72 }
    private var lineW: CGFloat { size * 0.06 }
    private var houseW: CGFloat { size * 0.34 }

    var body: some View {
        ZStack {
            // 1. Background gradient
            RoundedRectangle(cornerRadius: size * 0.2237, style: .continuous)
                .fill(
                    LinearGradient(
                        stops: [
                            .init(color: Color(red: 1.00, green: 0.48, blue: 0.36), location: 0.00),
                            .init(color: Color(red: 0.77, green: 0.42, blue: 0.80), location: 0.55),
                            .init(color: Color(red: 0.55, green: 0.36, blue: 0.96), location: 1.00)
                        ],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )

            // 2. Left "J"
            Text("J")
                .font(.system(size: size * 0.38, weight: .black, design: .rounded))
                .foregroundStyle(Color.white.opacity(0.88))
                .rotationEffect(.degrees(20))
                .offset(x: -(size * 0.30), y: size * 0.02)

            // 3. Right "J" — mirrored
            Text("J")
                .font(.system(size: size * 0.38, weight: .black, design: .rounded))
                .foregroundStyle(Color.white.opacity(0.88))
                .scaleEffect(x: -1, y: 1)
                .rotationEffect(.degrees(-20))
                .offset(x: size * 0.30, y: size * 0.02)

            // 4. House — centered
            Image(systemName: "house.fill")
                .resizable()
                .scaledToFit()
                .frame(width: houseW, height: houseW)
                .foregroundStyle(Color.white.opacity(0.95))
        }
        .frame(width: size, height: size)
    }
}

#Preview {
    AppIconView(size: 256)
        .padding(20)
        .background(Color.gray.opacity(0.2))
}
