import Speech
import AVFoundation

@MainActor
final class VoiceInputManager: ObservableObject {
    @Published var isRecording = false
    @Published var authStatus: SFSpeechRecognizerAuthorizationStatus = .notDetermined

    private let recognizer = SFSpeechRecognizer(locale: Locale(identifier: "zh-CN"))
    private var audioEngine = AVAudioEngine()
    private var request: SFSpeechAudioBufferRecognitionRequest?
    private var task: SFSpeechRecognitionTask?

    init() {
        authStatus = SFSpeechRecognizer.authorizationStatus()
    }

    func toggle(onTranscript: @escaping (String) -> Void) {
        if isRecording {
            stop()
        } else {
            requestPermissionsAndStart(onTranscript: onTranscript)
        }
    }

    private func requestPermissionsAndStart(onTranscript: @escaping (String) -> Void) {
        SFSpeechRecognizer.requestAuthorization { [weak self] status in
            Task { @MainActor [weak self] in
                guard let self else { return }
                self.authStatus = status
                guard status == .authorized else { return }
                AVAudioApplication.requestRecordPermission { granted in
                    Task { @MainActor [weak self] in
                        guard granted else { return }
                        self?.start(onTranscript: onTranscript)
                    }
                }
            }
        }
    }

    private func start(onTranscript: @escaping (String) -> Void) {
        guard recognizer?.isAvailable == true else { return }

        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.record, mode: .measurement, options: .duckOthers)
            try session.setActive(true, options: .notifyOthersOnDeactivation)
        } catch {
            return
        }

        request = SFSpeechAudioBufferRecognitionRequest()
        guard let request else { return }
        request.shouldReportPartialResults = true
        request.requiresOnDeviceRecognition = false

        let inputNode = audioEngine.inputNode
        let format = inputNode.outputFormat(forBus: 0)
        inputNode.installTap(onBus: 0, bufferSize: 1024, format: format) { buffer, _ in
            request.append(buffer)
        }

        task = recognizer?.recognitionTask(with: request) { [weak self] result, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                if let text = result?.bestTranscription.formattedString {
                    onTranscript(text)
                }
                if error != nil || result?.isFinal == true {
                    self.stop()
                }
            }
        }

        do {
            audioEngine.prepare()
            try audioEngine.start()
            isRecording = true
        } catch {
            stop()
        }
    }

    func stop() {
        audioEngine.stop()
        audioEngine.inputNode.removeTap(onBus: 0)
        request?.endAudio()
        request = nil
        task?.cancel()
        task = nil
        isRecording = false
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
    }
}
