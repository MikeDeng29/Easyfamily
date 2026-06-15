import SwiftUI

struct ChatView: View {
    @EnvironmentObject private var session: AuthSession
    @StateObject private var viewModel = ChatViewModel()
    @FocusState private var inputFocused: Bool

    private let suggestions: [(icon: String, text: String)] = [
        ("magnifyingglass", "查一下手机号"),
        ("chart.bar.fill", "查询今日配额"),
        ("person.2.fill", "添加家庭成员")
    ]

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                ScrollView {
                    ScrollViewReader { proxy in
                        VStack(alignment: .leading, spacing: 16) {
                            if viewModel.messages.isEmpty {
                                welcomeView
                            }
                            ForEach(viewModel.messages) { message in
                                bubble(for: message)
                                    .id(message.id)
                            }
                            if let action = viewModel.pendingBillAction {
                                billActionCard(action)
                            }
                        }
                        .padding(16)
                        .onChange(of: viewModel.messages.count) {
                            if let last = viewModel.messages.last {
                                proxy.scrollTo(last.id, anchor: .bottom)
                            }
                        }
                    }
                }

                inputBar
            }
            .background(AppPalette.background)
            .navigationTitle("easyfamily")
        }
    }

    private var welcomeView: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 12) {
                Image(systemName: "sparkles")
                    .font(.system(size: 22, weight: .semibold))
                    .foregroundColor(.white)
                    .frame(width: 44, height: 44)
                    .background(
                        LinearGradient(colors: [AppPalette.coral, AppPalette.violet], startPoint: .topLeading, endPoint: .bottomTrailing)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 14))

                VStack(alignment: .leading, spacing: 2) {
                    Text("AI 智能助手").font(.headline)
                    Text("我可以帮你查手机号绑定、记账、查配额")
                        .font(.caption)
                        .foregroundColor(AppPalette.textSecondary)
                }
            }

            Text("快速开始").font(.subheadline.bold()).foregroundColor(AppPalette.textSecondary)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    ForEach(suggestions, id: \.text) { suggestion in
                        Button {
                            viewModel.input = suggestion.text
                        } label: {
                            HStack(spacing: 6) {
                                Image(systemName: suggestion.icon)
                                Text(suggestion.text)
                            }
                            .font(.subheadline)
                            .foregroundColor(AppPalette.violetDark)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 10)
                            .background(AppPalette.softViolet)
                            .cornerRadius(20)
                        }
                    }
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    @ViewBuilder
    private func bubble(for message: ChatMessage) -> some View {
        HStack(alignment: .top, spacing: 8) {
            if message.role == "user" { Spacer(minLength: 40) }

            if message.role == "ai" {
                avatar(systemImage: "sparkles", color: AppPalette.violet)
            }

            VStack(alignment: .leading) {
                Text(message.content)
                if message.isStreaming {
                    ProgressView().scaleEffect(0.7)
                }
            }
            .padding(12)
            .background(message.role == "user" ? AppPalette.bubbleUser : AppPalette.bubbleAi)
            .cornerRadius(12)

            if message.role == "user" { avatar(systemImage: "person.fill", color: AppPalette.coral) }
            if message.role != "user" { Spacer(minLength: 40) }
        }
    }

    private func avatar(systemImage: String, color: Color) -> some View {
        Image(systemName: systemImage)
            .font(.system(size: 14, weight: .semibold))
            .foregroundColor(.white)
            .frame(width: 28, height: 28)
            .background(color)
            .clipShape(Circle())
    }

    private func billActionCard(_ action: BillActionData) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("💰 记录支出？").font(.subheadline.bold())
            Text("分类：\(action.category) · 金额：¥\(String(format: "%.2f", action.amount)) · 日期：\(action.date)")
                .font(.footnote)
            if let note = action.note, !note.isEmpty {
                Text("备注：\(note)").font(.footnote)
            }
            HStack {
                Button("取消") { viewModel.dismissBillAction() }
                    .buttonStyle(.plain)
                    .foregroundColor(AppPalette.textSecondary)
                Spacer()
                Button("确认记录") {
                    if let token = session.accessToken {
                        viewModel.confirmBillAction(token: token)
                    }
                }
                .buttonStyle(.borderedProminent)
                .tint(AppPalette.coral)
            }
        }
        .padding(12)
        .background(AppPalette.softCoral)
        .cornerRadius(14)
    }

    private var inputBar: some View {
        HStack(spacing: 10) {
            TextField("输入或说出你的问题...", text: $viewModel.input)
                .focused($inputFocused)
                .padding(12)
                .background(AppPalette.surface)
                .cornerRadius(20)
                .overlay(
                    RoundedRectangle(cornerRadius: 20)
                        .strokeBorder(inputFocused ? AppPalette.coral : .clear, lineWidth: 1.5)
                )

            Button {
                if let token = session.accessToken {
                    viewModel.sendMessage(token: token)
                }
            } label: {
                if viewModel.loading {
                    ProgressView().frame(width: 40, height: 40)
                } else {
                    Image(systemName: "arrow.up.circle.fill")
                        .font(.system(size: 28))
                        .foregroundColor(viewModel.input.isEmpty ? AppPalette.softCoral : AppPalette.coral)
                        .scaleEffect(viewModel.input.isEmpty ? 1 : 1.05)
                        .animation(.spring(response: 0.25, dampingFraction: 0.6), value: viewModel.input.isEmpty)
                }
            }
            .disabled(viewModel.loading || viewModel.input.isEmpty)
        }
        .padding(12)
        .background(AppPalette.background)
    }
}
