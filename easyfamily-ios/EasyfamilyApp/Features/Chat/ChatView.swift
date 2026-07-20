import SwiftUI

struct ChatView: View {
    @EnvironmentObject private var session: AuthSession
    @StateObject private var viewModel = ChatViewModel()
    @StateObject private var voiceManager = VoiceInputManager()
    @FocusState private var inputFocused: Bool
    @FocusState private var nicknameFocused: Bool
    @State private var micPulse = false

    private let suggestions: [(icon: String, text: String)] = [
        ("yensign.circle.fill", "帮我记一笔账"),
        ("car.fill", "查看车辆保养记录"),
        ("person.2.fill", "添加家庭成员")
    ]

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                ScrollView {
                    ScrollViewReader { proxy in
                        VStack(alignment: .leading, spacing: 16) {
                            if viewModel.messages.isEmpty {
                                if !viewModel.isProfileLoaded {
                                    ProgressView()
                                        .frame(maxWidth: .infinity, alignment: .center)
                                        .padding(.top, 60)
                                } else if viewModel.nickname == nil {
                                    nicknameOnboardingView
                                } else {
                                    welcomeView
                                }
                            }
                            ForEach(viewModel.messages) { message in
                                bubble(for: message)
                                    .id(message.id)
                            }
                            if let action = viewModel.pendingBillAction {
                                billActionCard(action)
                            }
                            if let action = viewModel.pendingFamilyAction {
                                familyActionCard(action)
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
            .navigationTitle(viewModel.butlerName)
            .task {
                if let token = session.accessToken {
                    await viewModel.loadProfile(token: token)
                }
            }
            .sheet(isPresented: $viewModel.showButlerSettings) {
                butlerSettingsSheet
            }
        }
    }

    private var welcomeView: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 12) {
                Image(systemName: ButlerAvatar.icon(for: viewModel.butlerAvatarId))
                    .font(.system(size: 22, weight: .semibold))
                    .foregroundColor(.white)
                    .frame(width: 44, height: 44)
                    .background(ButlerAvatar.color(for: viewModel.butlerAvatarId))
                    .clipShape(RoundedRectangle(cornerRadius: 14))

                VStack(alignment: .leading, spacing: 2) {
                    Text("嗨，\(viewModel.nickname ?? "")！").font(.headline)
                    Text("我是\(viewModel.butlerName)，可以帮你记账、管理家庭成员和车辆")
                        .font(.caption)
                        .foregroundColor(AppPalette.textSecondary)
                }

                Spacer()

                Button {
                    viewModel.resetNickname()
                } label: {
                    Image(systemName: "square.and.pencil")
                        .font(.system(size: 18))
                        .foregroundColor(AppPalette.textSecondary)
                }

                Button {
                    viewModel.beginButlerSetup()
                } label: {
                    Image(systemName: "slider.horizontal.3")
                        .font(.system(size: 18))
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
                .padding(.trailing, 24)
            }
            .mask(
                LinearGradient(
                    stops: [
                        .init(color: .black, location: 0.92),
                        .init(color: .clear, location: 1.0)
                    ],
                    startPoint: .leading, endPoint: .trailing
                )
            )
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var nicknameOnboardingView: some View {
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
                    Text("先认识一下，我该怎么称呼你")
                        .font(.caption)
                        .foregroundColor(AppPalette.textSecondary)
                }
            }

            HStack(spacing: 10) {
                TextField("输入姓名或给自己起个昵称", text: $viewModel.nicknameInput)
                    .focused($nicknameFocused)
                    .padding(12)
                    .background(AppPalette.surface)
                    .cornerRadius(10)
                    .overlay(
                        RoundedRectangle(cornerRadius: 10)
                            .strokeBorder(nicknameFocused ? AppPalette.coral : .clear, lineWidth: 1.5)
                    )
                    .onSubmit {
                        if let token = session.accessToken { viewModel.saveNickname(token: token) }
                    }

                Button("确定") {
                    if let token = session.accessToken { viewModel.saveNickname(token: token) }
                }
                .font(.subheadline)
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .background(viewModel.canSubmitNickname ? AppPalette.coral : AppPalette.disabledSurface)
                .foregroundColor(viewModel.canSubmitNickname ? .white : AppPalette.textSecondary)
                .cornerRadius(10)
                .disabled(!viewModel.canSubmitNickname)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .onAppear { nicknameFocused = true }
    }

    private var butlerOnboardingView: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 12) {
                Image(systemName: ButlerAvatar.icon(for: viewModel.butlerAvatarIdInput))
                    .font(.system(size: 22, weight: .semibold))
                    .foregroundColor(.white)
                    .frame(width: 44, height: 44)
                    .background(ButlerAvatar.color(for: viewModel.butlerAvatarIdInput))
                    .clipShape(RoundedRectangle(cornerRadius: 14))

                VStack(alignment: .leading, spacing: 2) {
                    Text("认识一下你的专属管家").font(.headline)
                    Text("给它起个名字、选个形象和性格吧")
                        .font(.caption)
                        .foregroundColor(AppPalette.textSecondary)
                }
            }

            butlerSetupForm

            HStack {
                Button("暂不设置，先用默认") {
                    viewModel.cancelButlerSetup()
                }
                .font(.footnote)
                .foregroundColor(AppPalette.textSecondary)

                Spacer()

                Button("完成") {
                    if let token = session.accessToken { viewModel.saveButlerSetup(token: token) }
                }
                .font(.subheadline)
                .padding(.horizontal, 20)
                .padding(.vertical, 10)
                .background(AppPalette.coral)
                .foregroundColor(.white)
                .cornerRadius(10)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .onAppear {
            viewModel.primeButlerSetupDraft()
        }
    }

    private var butlerSetupForm: some View {
        VStack(alignment: .leading, spacing: 14) {
            VStack(alignment: .leading, spacing: 8) {
                Text("形象").font(.subheadline.bold()).foregroundColor(AppPalette.textSecondary)
                HStack(spacing: 10) {
                    ForEach(ButlerAvatar.allIds, id: \.self) { avatarId in
                        Button {
                            viewModel.butlerAvatarIdInput = avatarId
                        } label: {
                            Image(systemName: ButlerAvatar.icon(for: avatarId))
                                .font(.system(size: 16, weight: .semibold))
                                .foregroundColor(.white)
                                .frame(width: 36, height: 36)
                                .background(ButlerAvatar.color(for: avatarId))
                                .clipShape(Circle())
                                .overlay(
                                    Circle()
                                        .strokeBorder(AppPalette.textPrimary, lineWidth: viewModel.butlerAvatarIdInput == avatarId ? 2 : 0)
                                        .padding(-2)
                                )
                        }
                    }
                }
            }

            VStack(alignment: .leading, spacing: 8) {
                Text("名字").font(.subheadline.bold()).foregroundColor(AppPalette.textSecondary)
                TextField("青鸟管家", text: $viewModel.butlerNameInput)
                    .padding(12)
                    .background(AppPalette.surface)
                    .cornerRadius(10)
            }

            VStack(alignment: .leading, spacing: 8) {
                Text("性格").font(.subheadline.bold()).foregroundColor(AppPalette.textSecondary)
                VStack(spacing: 8) {
                    ForEach(ButlerPersona.all, id: \.self) { persona in
                        Button {
                            viewModel.butlerPersonaInput = persona
                        } label: {
                            HStack {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(ButlerPersona.label(for: persona)).font(.subheadline.bold())
                                    Text(ButlerPersona.description(for: persona))
                                        .font(.caption)
                                        .foregroundColor(AppPalette.textSecondary)
                                }
                                Spacer()
                                if viewModel.butlerPersonaInput == persona {
                                    Image(systemName: "checkmark.circle.fill")
                                        .foregroundColor(AppPalette.coral)
                                }
                            }
                            .padding(12)
                            .background(viewModel.butlerPersonaInput == persona ? AppPalette.softCoral : AppPalette.surface)
                            .cornerRadius(10)
                        }
                        .foregroundColor(AppPalette.textPrimary)
                    }
                }
            }
        }
    }

    private var butlerSettingsSheet: some View {
        NavigationStack {
            ScrollView {
                butlerSetupForm
                    .padding(16)
            }
            .background(AppPalette.background)
            .navigationTitle("管家设置")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消") { viewModel.showButlerSettings = false }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("保存") {
                        if let token = session.accessToken { viewModel.saveButlerSetup(token: token) }
                    }
                }
            }
        }
    }

    @ViewBuilder
    private func bubble(for message: ChatMessage) -> some View {
        HStack(alignment: .top, spacing: 8) {
            if message.role == "user" { Spacer(minLength: 40) }

            if message.role == "ai" {
                avatar(systemImage: ButlerAvatar.icon(for: viewModel.butlerAvatarId), color: ButlerAvatar.color(for: viewModel.butlerAvatarId))
            }

            VStack(alignment: .leading) {
                Text(message.content.strippingSMPEmoji)
                if message.isStreaming {
                    ProgressView().scaleEffect(0.7)
                }
            }
            .padding(12)
            .background(message.role == "user" ? AppPalette.bubbleUser : AppPalette.bubbleAi)
            .cornerRadius(12)
            .contextMenu {
                Button {
                    UIPasteboard.general.string = message.content.strippingSMPEmoji
                } label: {
                    Label("复制", systemImage: "doc.on.doc")
                }
            }

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
        let isIncome = action.direction == "income"
        let accentColor = isIncome ? Color(red: 0.2, green: 0.7, blue: 0.4) : AppPalette.coral
        return VStack(alignment: .leading, spacing: 6) {
            Text(isIncome ? "记录收入？" : "记录支出？")
                .font(.subheadline.bold())
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
                .tint(accentColor)
            }
        }
        .padding(12)
        .background(accentColor.opacity(0.1))
        .cornerRadius(14)
    }

    private func familyActionCard(_ action: FamilyActionData) -> some View {
        let isAdd = action.action == "add"
        let accentColor = isAdd ? Color(red: 0.2, green: 0.7, blue: 0.4) : AppPalette.coral
        let icon = isAdd ? "person.badge.plus" : "person.badge.minus"
        return VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 6) {
                Image(systemName: icon)
                    .foregroundColor(accentColor)
                Text(isAdd ? "添加家庭成员？" : "移除家庭成员？")
                    .font(.subheadline.bold())
            }
            Text("姓名：\(action.name)　关系：\(action.relation)")
                .font(.footnote)
            if isAdd, let phone = action.phone {
                Text("手机号：\(phone)")
                    .font(.footnote)
            }
            HStack {
                Button("取消") { viewModel.dismissFamilyAction() }
                    .buttonStyle(.plain)
                    .foregroundColor(AppPalette.textSecondary)
                Spacer()
                Button(isAdd ? "确认添加" : "确认移除") {
                    if let token = session.accessToken {
                        viewModel.confirmFamilyAction(token: token)
                    }
                }
                .buttonStyle(.borderedProminent)
                .tint(accentColor)
            }
        }
        .padding(12)
        .background(accentColor.opacity(0.1))
        .cornerRadius(14)
    }

    private var inputBar: some View {
        HStack(spacing: 10) {
            // Mic button
            Button {
                if voiceManager.isRecording {
                    voiceManager.stop()
                } else {
                    inputFocused = false
                    voiceManager.toggle { transcript in
                        viewModel.input = transcript
                    }
                }
            } label: {
                ZStack {
                    if voiceManager.isRecording {
                        Circle()
                            .fill(AppPalette.coral.opacity(0.2))
                            .frame(width: 40, height: 40)
                            .scaleEffect(micPulse ? 1.4 : 1.0)
                    }
                    Image(systemName: voiceManager.isRecording ? "waveform" : "mic.fill")
                        .font(.system(size: 20))
                        .foregroundColor(voiceManager.isRecording ? AppPalette.coral : AppPalette.softCoral)
                        .frame(width: 40, height: 40)
                }
            }
            .onChange(of: voiceManager.isRecording) { _, recording in
                if recording {
                    withAnimation(.easeInOut(duration: 0.65).repeatForever(autoreverses: true)) {
                        micPulse = true
                    }
                } else {
                    withAnimation(.default) { micPulse = false }
                }
            }

            TextField("输入或说出你的问题...", text: $viewModel.input)
                .focused($inputFocused)
                .padding(12)
                .background(AppPalette.surface)
                .cornerRadius(20)
                .overlay(
                    RoundedRectangle(cornerRadius: 20)
                        .strokeBorder(
                            voiceManager.isRecording ? AppPalette.coral :
                            (inputFocused ? AppPalette.coral : .clear),
                            lineWidth: 1.5
                        )
                )
                .onChange(of: inputFocused) { _, focused in
                    if focused && voiceManager.isRecording { voiceManager.stop() }
                }

            Button {
                if let token = session.accessToken {
                    viewModel.sendMessage(token: token, session: session)
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
