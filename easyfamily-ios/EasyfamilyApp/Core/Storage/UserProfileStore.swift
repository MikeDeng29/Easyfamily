import Foundation

/// Stores the user's chosen display nickname and AI butler identity locally
/// (UserDefaults), used to personalize the AI chat UI offline-first.
struct UserProfileStore {
    private let nicknameKey = "com.easyfamily.ios.nickname"
    private let butlerNameKey = "com.easyfamily.ios.butlerName"
    private let butlerAvatarIdKey = "com.easyfamily.ios.butlerAvatarId"
    private let butlerPersonaKey = "com.easyfamily.ios.butlerPersona"
    private let butlerSetupDoneKey = "com.easyfamily.ios.butlerSetupDone"

    func loadNickname() -> String? {
        UserDefaults.standard.string(forKey: nicknameKey)
    }

    func saveNickname(_ nickname: String) {
        UserDefaults.standard.set(nickname, forKey: nicknameKey)
    }

    func clearNickname() {
        UserDefaults.standard.removeObject(forKey: nicknameKey)
    }

    func loadButlerName() -> String? {
        UserDefaults.standard.string(forKey: butlerNameKey)
    }

    func loadButlerAvatarId() -> Int? {
        let value = UserDefaults.standard.integer(forKey: butlerAvatarIdKey)
        return value == 0 ? nil : value
    }

    func loadButlerPersona() -> String? {
        UserDefaults.standard.string(forKey: butlerPersonaKey)
    }

    func saveButlerIdentity(name: String, avatarId: Int, persona: String) {
        UserDefaults.standard.set(name, forKey: butlerNameKey)
        UserDefaults.standard.set(avatarId, forKey: butlerAvatarIdKey)
        UserDefaults.standard.set(persona, forKey: butlerPersonaKey)
    }

    func isButlerSetupDone() -> Bool {
        UserDefaults.standard.bool(forKey: butlerSetupDoneKey)
    }

    func markButlerSetupDone() {
        UserDefaults.standard.set(true, forKey: butlerSetupDoneKey)
    }
}
