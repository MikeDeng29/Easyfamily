import SwiftUI

/// Drag-to-fit slider captcha. Mirrors the Android `SlideCaptchaWidget`:
/// fetch a challenge with a hidden target X (encoded in an SVG `cx` attribute),
/// let the user drag a knob to that position, and report the trajectory
/// (`tracks`) plus final offset/time to `/auth/captcha/slide/verify`.
struct SlideCaptchaView: View {
    var onSuccess: (String) -> Void

    private enum Status {
        case loading, ready, dragging, verifying, success, error
    }

    @State private var status: Status = .loading
    @State private var info: String = "正在加载验证组件..."
    @State private var challengeId: String = ""
    @State private var targetSvgX: CGFloat = 0
    @State private var currentOffsetPx: CGFloat = 0
    @State private var trackWidthPx: CGFloat = 0
    @State private var tracks: [SlideTrackPoint] = []
    @State private var dragStartTime: Date?

    private let svgWidth: CGFloat = 320
    private let knobSize: CGFloat = 44

    private var maxDrag: CGFloat { max(trackWidthPx - knobSize, 1) }
    private var currentSvgX: CGFloat { (currentOffsetPx / maxDrag) * svgWidth }
    private var targetOffsetPx: CGFloat { (targetSvgX / svgWidth) * maxDrag }

    var body: some View {
        VStack(spacing: 8) {
            ZStack(alignment: .leading) {
                LinearGradient(
                    colors: [Color(hex: 0xEEF3FF), Color(hex: 0xDCE7FF)],
                    startPoint: .topLeading, endPoint: .bottomTrailing
                )
                .cornerRadius(12)

                Text("拖动滑块完成验证")
                    .font(.caption)
                    .foregroundColor(AppPalette.textSecondary)
                    .padding(.leading, 14)
                    .padding(.top, 8)
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)

                // Target indicator
                Circle()
                    .fill(Color(hex: 0xB9C7EA).opacity(0.75))
                    .frame(width: 40, height: 40)
                    .offset(x: targetOffsetPx + knobSize / 2 - 20, y: 0)
                    .frame(maxHeight: .infinity, alignment: .center)

                // Current knob indicator
                Circle()
                    .fill(status == .success ? AppPalette.success : Color(hex: 0x6E86C8))
                    .frame(width: 40, height: 40)
                    .offset(x: currentOffsetPx + knobSize / 2 - 20, y: 0)
                    .frame(maxHeight: .infinity, alignment: .center)
            }
            .frame(height: 100)

            // Draggable track
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 22)
                        .fill(Color(hex: 0xE8ECF4))

                    Circle()
                        .fill(knobColor)
                        .frame(width: knobSize, height: knobSize)
                        .overlay(Text(knobLabel).foregroundColor(.white).font(.headline))
                        .offset(x: currentOffsetPx)
                        .gesture(dragGesture)
                }
                .onAppear { trackWidthPx = geo.size.width }
                .onChange(of: geo.size.width) { trackWidthPx = $0 }
            }
            .frame(height: 44)

            HStack {
                Text(info)
                    .font(.caption)
                    .foregroundColor(status == .error ? AppPalette.error : AppPalette.textSecondary)
                Spacer()
                if status == .error {
                    Button("重新加载") { Task { await loadChallenge() } }
                        .font(.caption)
                }
            }
        }
        .task { await loadChallenge() }
    }

    private var knobColor: Color {
        switch status {
        case .verifying: return .gray
        case .success: return AppPalette.success
        case .error: return AppPalette.error
        default: return AppPalette.coral
        }
    }

    private var knobLabel: String {
        switch status {
        case .verifying: return "…"
        case .success: return "✓"
        default: return "→"
        }
    }

    private var dragGesture: some Gesture {
        DragGesture(minimumDistance: 0)
            .onChanged { value in
                guard status == .ready || status == .dragging else { return }
                if dragStartTime == nil {
                    dragStartTime = Date()
                    tracks = [SlideTrackPoint(x: 0, y: 0, t: 0)]
                    status = .dragging
                }
                let newOffset = min(max(value.translation.width, 0), maxDrag)
                currentOffsetPx = newOffset
                if let start = dragStartTime {
                    let t = Int(Date().timeIntervalSince(start) * 1000)
                    tracks.append(SlideTrackPoint(x: Int(currentSvgX), y: 0, t: t))
                }
            }
            .onEnded { _ in
                guard status == .dragging, let start = dragStartTime else { return }
                let totalTimeMs = Int(Date().timeIntervalSince(start) * 1000)
                let finalX = Int(currentSvgX)
                tracks.append(SlideTrackPoint(x: finalX, y: 0, t: totalTimeMs))
                status = .verifying
                info = "验证中..."
                Task { await verify(offsetX: finalX, totalTimeMs: totalTimeMs) }
            }
    }

    private func loadChallenge() async {
        status = .loading
        info = "正在加载验证组件..."
        currentOffsetPx = 0
        tracks = []
        dragStartTime = nil
        do {
            let result = try await APIService.slideCaptchaInit()
            challengeId = result.challengeId
            targetSvgX = CGFloat(parseTargetX(from: result.backgroundImageUrl) ?? 0)
            status = .ready
            info = "向右拖动滑块完成验证"
        } catch {
            status = .error
            info = "加载失败：\(error.localizedDescription)"
        }
    }

    private func verify(offsetX: Int, totalTimeMs: Int) async {
        do {
            let token = try await APIService.slideCaptchaVerify(
                challengeId: challengeId, offsetX: offsetX, totalTimeMs: totalTimeMs, tracks: tracks
            )
            status = .success
            info = "✓ 安全校验已通过"
            onSuccess(token)
        } catch {
            status = .error
            info = "验证失败，请重试"
            try? await Task.sleep(nanoseconds: 1_500_000_000)
            await loadChallenge()
        }
    }

    /// Extracts the first `cx='NNN'` value from the (base64-encoded) SVG data URL —
    /// that circle marks the hidden target position.
    private func parseTargetX(from dataUrl: String) -> Int? {
        guard let base64Start = dataUrl.range(of: "base64,") else { return nil }
        let base64 = String(dataUrl[base64Start.upperBound...])
        guard let data = Data(base64Encoded: base64), let svg = String(data: data, encoding: .utf8) else { return nil }
        guard let regex = try? NSRegularExpression(pattern: "cx='(\\d+)'") else { return nil }
        let range = NSRange(svg.startIndex..<svg.endIndex, in: svg)
        guard let match = regex.firstMatch(in: svg, range: range), let cxRange = Range(match.range(at: 1), in: svg) else { return nil }
        return Int(svg[cxRange])
    }
}
