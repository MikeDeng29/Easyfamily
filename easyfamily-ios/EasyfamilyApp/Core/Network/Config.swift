import Foundation

enum Config {
    #if DEBUG
    static let apiBaseURL = "http://47.102.126.67:8080"
    #else
    static let apiBaseURL = "https://easyfamily.xin"
    #endif
}
