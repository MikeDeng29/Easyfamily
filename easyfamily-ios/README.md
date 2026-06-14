# easyfamily-ios

Native SwiftUI client for the Easyfamily platform, mirroring `easyfamily-android`'s feature set:
login (slide captcha + SMS), real-name phone query, phone management, family overview,
AI chat with auto bill recording (SSE streaming), bill list/stats, and vehicle +
maintenance-record management/stats.

## Requirements

- macOS with **Xcode 15+** installed (full Xcode.app, not just Command Line Tools)
- [XcodeGen](https://github.com/yonaskolb/XcodeGen) — `brew install xcodegen`

> This project's `.xcodeproj` is generated from `project.yml`, not checked into git.

## Build & run

```bash
cd easyfamily-ios
xcodegen generate
open EasyfamilyApp.xcodeproj
```

Select a simulator (or your iPhone) and hit Run.

## Backend configuration

`EasyfamilyApp/Core/Network/Config.swift` points at the same production backend the
Android debug build uses:

```swift
static let apiBaseURL = "http://47.102.126.67:8080"
```

This is plain HTTP, so `Info.plist` sets `NSAllowsArbitraryLoads = true` (App Transport
Security exception) — the iOS equivalent of Android's `usesCleartextTraffic="true"`.
Once the backend has a real domain + HTTPS, update `apiBaseURL` and replace the ATS
exception with a scoped `NSExceptionDomains` entry (or remove it entirely).

## Architecture

- `Core/Network` — `APIClient` (generic URLSession JSON client matching the backend's
  `{ code, message, data }` envelope), `APIService` (one function per endpoint),
  `ChatStreamClient` (SSE line parsing for `/api/v1/chat/stream`), `APIModels` (DTOs).
- `Core/Storage/TokenStore` — Keychain-backed access-token persistence (replaces
  Android's DataStore).
- `Core/Session/AuthSession` — app-wide `ObservableObject` holding the logged-in state.
- `Navigation` — `RootView` (Login vs main app), `MainTabView` (对话/我的 tabs),
  `MineView` (大家庭/手机号/查询/车辆/账单 + 退出登录).
- `Features/*` — one folder per screen area, each with a SwiftUI `View` and an
  `ObservableObject` view model, following the same MVVM split as the Android app's
  Compose screens + ViewModels.

## Known gaps vs. Android

- The slide captcha renders a simplified `Canvas`-based puzzle (gradient track + target
  circle parsed from the backend's SVG `cx` attribute) rather than rendering the actual
  SVG image — SwiftUI has no built-in SVG renderer. The drag-tracking/verification logic
  (`tracks`, `offsetX`, `totalTimeMs`) is faithful to the Android implementation.
- Voice input (Android's speech-recognition mic button) is not yet wired up; the chat
  input is text-only for now.
