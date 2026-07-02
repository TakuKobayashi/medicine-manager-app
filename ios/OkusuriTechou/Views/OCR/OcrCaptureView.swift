import SwiftUI
import AVFoundation

/// お薬シールをカメラで撮影し、自動でテキスト検出→解析→確認ダイアログに遷移する画面。
/// 「それらしい」情報が揃った時点で自動的に解析を停止する。
struct OcrCaptureView: View {
    let onDone: () -> Void

    @Environment(\.dismiss) private var dismiss
    @StateObject private var coordinator = OcrCaptureCoordinator()

    var body: some View {
        ZStack {
            if coordinator.cameraAuthorized {
                CameraPreviewView(session: coordinator.controller.session)
                    .ignoresSafeArea()
            } else {
                VStack(spacing: 16) {
                    Text("カメラの権限が必要です。")
                    Button("設定を開く") {
                        if let url = URL(string: UIApplication.openSettingsURLString) {
                            UIApplication.shared.open(url)
                        }
                    }
                }
            }
        }
        .navigationTitle("シール読み取り")
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("キャンセル") { dismiss() }
            }
        }
        .onAppear { coordinator.requestPermissionAndStart() }
        .onDisappear { coordinator.controller.stop() }
        .sheet(isPresented: $coordinator.showConfirm) {
            if let parsed = coordinator.parsedResult {
                OcrConfirmView(
                    result: parsed,
                    existing: coordinator.matchedExisting,
                    onCancel: { coordinator.retry() },
                    onConfirm: {
                        coordinator.apply()
                        onDone()
                        dismiss()
                    }
                )
            }
        }
    }
}

@MainActor
final class OcrCaptureCoordinator: ObservableObject {
    let controller = OcrCameraController()

    @Published var cameraAuthorized = false
    @Published var showConfirm = false
    @Published var parsedResult: OcrTextParser.ParsedSticker?
    @Published var matchedExisting: Medicine?

    init() {
        controller.onTextRecognized = { [weak self] rawText in
            self?.handleRecognized(rawText)
        }
    }

    func requestPermissionAndStart() {
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            cameraAuthorized = true
            controller.start()
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { granted in
                DispatchQueue.main.async {
                    self.cameraAuthorized = granted
                    if granted { self.controller.start() }
                }
            }
        default:
            cameraAuthorized = false
        }
    }

    private func handleRecognized(_ rawText: String) {
        let parsed = OcrTextParser.parse(rawText)
        parsedResult = parsed
        matchedExisting = parsed.name.flatMap { try? Medicine.findByName($0) } ?? nil
        controller.stop()
        showConfirm = true
    }

    func retry() {
        showConfirm = false
        parsedResult = nil
        matchedExisting = nil
        controller.start()
    }

    func apply() {
        guard let result = parsedResult, let name = result.name else { return }
        do {
            if var existing = matchedExisting {
                let addQty = result.computedAddQuantity ?? 0
                existing = existing.withAddedQuantity(addQty)
                try existing.save()
            } else {
                try Medicine.create(
                    name: name,
                    doseForm: OcrTextParser.guessDoseForm(),
                    doseAmount: result.doseAmount ?? 1,
                    frequencyType: OcrTextParser.guessFrequencyType(),
                    intervalDays: nil,
                    timingMorning: result.timingMorning,
                    timingNoon: result.timingNoon,
                    timingNight: result.timingNight,
                    remainingQty: result.computedAddQuantity ?? 0,
                    isActive: true
                )
            }
        } catch {
            // TODO: エラー表示
        }
    }
}

/// Before/Afterを表示する確認ダイアログ画面。
private struct OcrConfirmView: View {
    let result: OcrTextParser.ParsedSticker
    let existing: Medicine?
    let onCancel: () -> Void
    let onConfirm: () -> Void

    private var addQty: Double { result.computedAddQuantity ?? 0 }

    var body: some View {
        NavigationStack {
            Form {
                Section("Before") {
                    if let existing {
                        Text("\(existing.name): 残量 \(format(existing.remainingQty))")
                    } else {
                        Text("(新規登録)")
                    }
                }
                Section("After") {
                    Text("薬名: \(result.name ?? "(不明)")")
                    Text("1回量: \(result.doseAmount.map { format($0) } ?? "(不明)")")
                    Text("タイミング: \(timingSummary)")
                    Text("日数: \(result.days.map { "\($0)日分" } ?? "(不明)")")
                    if let existing {
                        Text("残量: \(format(existing.remainingQty)) → \(format(existing.remainingQty + addQty))")
                    } else {
                        Text("残量: \(format(addQty))")
                    }
                }
            }
            .navigationTitle(existing != nil ? "この内容で更新しますか？" : "この内容で新規登録しますか？")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("やり直す") { onCancel() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("OK") { onConfirm() }
                }
            }
        }
    }

    private var timingSummary: String {
        var parts: [String] = []
        if result.timingMorning { parts.append("朝") }
        if result.timingNoon { parts.append("昼") }
        if result.timingNight { parts.append("晩") }
        return parts.isEmpty ? "(不明)" : parts.joined(separator: "・")
    }

    private func format(_ value: Double) -> String {
        value == value.rounded() ? String(Int(value)) : String(value)
    }
}
