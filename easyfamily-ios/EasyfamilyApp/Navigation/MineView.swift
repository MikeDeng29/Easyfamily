import SwiftUI

struct MineView: View {
    @EnvironmentObject private var session: AuthSession
    @State private var showLogoutConfirm = false
    @State private var showFeedback = false
    @State private var showProfileEdit = false
    @State private var phoneCount: Int?
    @State private var familyCount: Int?
    @State private var financeRole: String = "none"
    @State private var userProfile: UserProfile? = nil

    private struct Destination: Identifiable {
        let id: String
        let title: String
        let icon: String
        let color: Color
        let background: Color
    }

    private var visibleDestinations: [Destination] {
        var list: [Destination] = [
            Destination(id: "family", title: "大家庭", icon: "house.fill", color: AppPalette.coral, background: AppPalette.softCoral),
            Destination(id: "phone", title: "手机号", icon: "phone.fill", color: AppPalette.violet, background: AppPalette.softViolet),
            Destination(id: "query", title: "查询", icon: "magnifyingglass", color: AppPalette.coral, background: AppPalette.softCoral),
            Destination(id: "vehicle", title: "车辆", icon: "car.fill", color: AppPalette.amber, background: AppPalette.softAmber),
            Destination(id: "bill", title: "账单", icon: "yensign.circle.fill", color: AppPalette.violet, background: AppPalette.softViolet),
        ]
        if financeRole == "head" || financeRole == "viewer" {
            list.append(Destination(id: "finance", title: "财务健康", icon: "chart.xyaxis.line", color: AppPalette.violet, background: AppPalette.softViolet))
            list.append(Destination(id: "assets", title: "家庭资产", icon: "chart.pie.fill", color: Color(hex: 0x2E7D32), background: Color(hex: 0xE8F5E9)))
            list.append(Destination(id: "liabilities", title: "家庭负债", icon: "creditcard.fill", color: AppPalette.coral, background: AppPalette.softCoral))
        }
        if financeRole == "head" {
            list.append(Destination(id: "finance_permissions", title: "财务授权", icon: "person.badge.key.fill", color: AppPalette.violet, background: AppPalette.softViolet))
        }
        return list
    }

    private let feedbackDestination = Destination(id: "feedback", title: "问题反馈", icon: "exclamationmark.bubble.fill", color: AppPalette.coral, background: AppPalette.softCoral)

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 24) {
                    profileCard

                    if financeRole == "viewer" {
                        Text("你正在查看家庭财务数据")
                            .font(.caption)
                            .foregroundColor(AppPalette.textSecondary)
                            .padding(.horizontal, 16)
                    }

                    VStack(spacing: 0) {
                        ForEach(visibleDestinations) { destination in
                            NavigationLink(value: destination.id) {
                                row(for: destination)
                            }
                            Divider().padding(.leading, 60)
                        }
                        Button {
                            showFeedback = true
                        } label: {
                            row(for: feedbackDestination)
                        }
                    }
                    .background(AppPalette.surface)
                    .cornerRadius(16)
                    .padding(.horizontal, 16)

                    Button("退出登录") {
                        showLogoutConfirm = true
                    }
                    .font(.subheadline)
                    .foregroundColor(AppPalette.error)
                    .padding(.top, 16)

                    Spacer(minLength: 40)
                }
                .padding(.top, 8)
            }
            .background(AppPalette.background)
            .navigationTitle("我的")
            .sheet(isPresented: $showFeedback) {
                FeedbackView()
                    .environmentObject(session)
            }
            .navigationDestination(for: String.self) { id in
                switch id {
                case "family": FamilyView()
                case "phone": PhoneView()
                case "query": QueryView()
                case "vehicle": VehicleListView()
                case "bill": BillListView()
                case "finance": FinancialHealthView()
                case "assets": AssetListView()
                case "liabilities": LiabilityListView()
                case "finance_permissions": FinancePermissionView()
                default: EmptyView()
                }
            }
            .confirmationDialog("确定要退出登录吗？", isPresented: $showLogoutConfirm, titleVisibility: .visible) {
                Button("退出登录", role: .destructive) { session.logout() }
                Button("取消", role: .cancel) {}
            }
            .task {
                guard let token = session.accessToken else { return }
                async let phones = try? APIService.listMyPhones(token: token)
                async let family = try? APIService.listFamilyMembers(token: token)
                async let roleResult = try? APIService.getFinanceRole(token: token)
                async let profile = try? APIService.getUserProfile(token: token)
                phoneCount = await phones?.count
                familyCount = await family?.count
                financeRole = await roleResult?.role ?? "none"
                userProfile = await profile
            }
            .sheet(isPresented: $showProfileEdit) {
                ProfileEditView(profile: userProfile) { updated in
                    userProfile = updated
                }
                .environmentObject(session)
            }
        }
    }

    private var profileCard: some View {
        Button { showProfileEdit = true } label: {
            HStack(spacing: 14) {
                Image(systemName: "person.fill")
                    .font(.system(size: 26, weight: .semibold))
                    .foregroundColor(.white)
                    .frame(width: 56, height: 56)
                    .background(
                        LinearGradient(colors: [AppPalette.coral, AppPalette.violet], startPoint: .topLeading, endPoint: .bottomTrailing)
                    )
                    .clipShape(Circle())

                VStack(alignment: .leading, spacing: 6) {
                    Text(userProfile?.nickname ?? "青鸟管家用户")
                        .font(.headline)
                        .foregroundColor(AppPalette.textPrimary)

                    if let phone = userProfile?.phone {
                        Label(phone, systemImage: "phone.fill")
                            .font(.caption)
                            .foregroundColor(AppPalette.textSecondary)
                    }

                    if let email = userProfile?.email, !email.isEmpty {
                        Label(email, systemImage: "envelope.fill")
                            .font(.caption)
                            .foregroundColor(AppPalette.textSecondary)
                    }

                    if let phoneCount, let familyCount {
                        Text("已绑定 \(phoneCount) 个手机号 · 家庭成员 \(familyCount) 人")
                            .font(.caption2)
                            .foregroundColor(AppPalette.textSecondary.opacity(0.7))
                    }
                }

                Spacer()

                Image(systemName: "chevron.right")
                    .font(.caption)
                    .foregroundColor(AppPalette.textSecondary)
            }
            .padding(16)
            .background(AppPalette.surface)
            .cornerRadius(16)
            .padding(.horizontal, 16)
        }
        .buttonStyle(.plain)
    }

    private func row(for destination: Destination) -> some View {
        HStack(spacing: 14) {
            Image(systemName: destination.icon)
                .font(.system(size: 16, weight: .medium))
                .foregroundColor(destination.color)
                .frame(width: 32, height: 32)
                .background(destination.background)
                .clipShape(RoundedRectangle(cornerRadius: 8))

            Text(destination.title)
                .foregroundColor(AppPalette.textPrimary)

            Spacer()

            Image(systemName: "chevron.right")
                .font(.caption)
                .foregroundColor(AppPalette.textSecondary)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .contentShape(Rectangle())
    }
}
