import SwiftUI

struct ChatView: View {
    @EnvironmentObject private var session: AuthSession
    @StateObject private var viewModel = ChatViewModel()

    private let suggestions = ["查一下手机号", "查询今日配额", "添加家庭成员"]

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                ScrollView {
                    ScrollViewReader { proxy in
                        VStack(alignment: .leading, spacing: 12) {
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
        VStack(alignment: .leading, spacing: 10) {
            Text("🏠 AI 智能助手").font(.headline)
            Text("你可以试试：").font(.subheadline).foregroundColor(AppPalette.textSecondary)
            ForEach(suggestions, id: \.self) { suggestion in
                Button(suggestion) { viewModel.input = suggestion }
                    .font(.subheadline)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(AppPalette.softViolet)
                    .cornerRadius(10)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    @ViewBuilder
    private func bubble(for message: ChatMessage) -> some View {
        HStack(alignment: .top) {
            if message.role == "user" { Spacer(minLength: 40) }

            if message.role == "ai" {
                Text("🤖")
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

            if message.role == "user" { Text("👤") }
            if message.role != "user" { Spacer(minLength: 40) }
        }
    }

    private func billActionCard(_ action: BillActionData) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("💰 记录支出？").font(.headline)
            Text("分类：\(action.category)")
            Text("金额：¥\(String(format: "%.2f", action.amount))")
            if let note = action.note, !note.isEmpty {
                Text("备注：\(note)")
            }
            Text("日期：\(action.date)")
            HStack {
                Button("取消") { viewModel.dismissBillAction() }
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
        .font(.subheadline)
        .padding(14)
        .background(AppPalette.softCoral)
        .cornerRadius(14)
    }

    private var inputBar: some View {
        HStack(spacing: 10) {
            TextField("输入或说出你的问题...", text: $viewModel.input)
                .padding(12)
                .background(AppPalette.surface)
                .cornerRadius(20)

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
                }
            }
            .disabled(viewModel.loading || viewModel.input.isEmpty)
        }
        .padding(12)
        .background(AppPalette.background)
    }
}
