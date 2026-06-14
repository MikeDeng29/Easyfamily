import Foundation

enum Config {
    /// Same production backend the Android debug build points at.
    /// Switch to an HTTPS domain (and tighten ATS in Info.plist) once one exists.
    static let apiBaseURL = "http://47.102.126.67:8080"
}
