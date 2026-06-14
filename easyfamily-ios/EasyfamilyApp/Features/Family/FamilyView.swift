import SwiftUI

struct FamilyView: View {
    @EnvironmentObject private var session: AuthSession
    @StateObject private var viewModel = FamilyViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                Text("大家庭")
                    .font(.title3.bold())
                Text("本人与家人信息统一展示，便于后续查询和关怀。")
                    .font(.subheadline)
                    .foregroundColor(AppPalette.textSecondary)

                if viewModel.loading {
                    ProgressView().padding(.top, 8)
                } else {
                    ForEach(viewModel.members) { member in
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(member.name)
                                    .font(.headline)
                                    .foregroundColor(AppPalette.textPrimary)
                                Text(member.maskedPhone)
                                    .font(.subheadline)
                                    .foregroundColor(AppPalette.textSecondary)
                            }
                            Spacer()
                            Text(member.relation)
                                .font(.caption)
                                .padding(.horizontal, 10)
                                .padding(.vertical, 5)
                                .background(member.isOwner ? AppPalette.softCoral : AppPalette.background)
                                .foregroundColor(member.isOwner ? AppPalette.coral : AppPalette.textPrimary)
                                .cornerRadius(8)
                        }
                        .padding(14)
                        .background(AppPalette.surface)
                        .cornerRadius(14)
                    }

                    if !viewModel.info.isEmpty {
                        Text(viewModel.info)
                            .font(.caption)
                            .foregroundColor(AppPalette.textSecondary)
                    }
                }
            }
            .padding(16)
        }
        .background(AppPalette.background)
        .navigationTitle("大家庭")
        .task {
            if let token = session.accessToken {
                await viewModel.load(token: token)
            }
        }
    }
}
