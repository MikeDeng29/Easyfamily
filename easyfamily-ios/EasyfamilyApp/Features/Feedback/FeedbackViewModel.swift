import Foundation

@Observable
final class FeedbackViewModel {
    var title: String = ""
    var description: String = ""
    var email: String = ""
    var isSubmitting: Bool = false
    var submitResult: SubmitResult? = nil

    enum SubmitResult: Equatable {
        case success
        case failure(String)
    }

    func submit(token: String) async {
        guard !description.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            submitResult = .failure("请填写问题描述")
            return
        }
        isSubmitting = true
        defer { isSubmitting = false }
        do {
            let trimmedTitle = title.trimmingCharacters(in: .whitespacesAndNewlines)
            let trimmedEmail = email.trimmingCharacters(in: .whitespacesAndNewlines)
            try await APIService.submitFeedback(
                token: token,
                title: trimmedTitle.isEmpty ? nil : trimmedTitle,
                description: description.trimmingCharacters(in: .whitespacesAndNewlines),
                email: trimmedEmail.isEmpty ? nil : trimmedEmail
            )
            submitResult = .success
        } catch {
            submitResult = .failure(error.localizedDescription)
        }
    }

    func loadDefaultEmail(token: String) async {
        do {
            let profile = try await APIService.getUserProfile(token: token)
            if let profileEmail = profile.email, !profileEmail.isEmpty {
                email = profileEmail
            }
        } catch {
            // 静默失败，用户可手动输入
        }
    }
}
