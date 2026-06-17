import Foundation

enum Config {
    #if DEBUG
    /// Debug: points to production server via HTTP (ATS exception active in Info.plist)
    static let apiBaseURL = "http://47.102.126.67:8080"
    #else
    /// Release: HTTPS domain (set before App Store submission)
    static let apiBaseURL = "https://REPLACE_WITH_YOUR_DOMAIN"
    #endif
}
