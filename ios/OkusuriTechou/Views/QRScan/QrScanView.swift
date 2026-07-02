import SwiftUI
import AVFoundation

/// トップ画面から起動するQRスキャン画面。
/// 薬(または薬袋)に貼られたQRシールを読み取り、その場で「服用した」として記録する。
struct QrScanView: View {
    let onDone: () -> Void

    @Environment(\.dismiss) private var dismiss
    @StateObject private var coordinator = QrScanCoordinator()

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
        .navigationTitle("QRコード読み取り")
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("キャンセル") { dismiss() }
            }
        }
        .onAppear { coordinator.requestPermissionAndStart() }
        .onDisappear { coordinator.controller.stop() }
        .alert("服用記録", isPresented: $coordinator.showConfirm) {
            Button("キャンセル", role: .cancel) { coordinator.retry() }
            Button("OK") {
                coordinator.record()
                onDone()
                dismiss()
            }
        } message: {
            Text("「\(coordinator.foundMedicine?.name ?? "")」を服用した記録を追加します。よろしいですか？")
        }
        .alert("見つかりません", isPresented: $coordinator.showNotFound) {
            Button("閉じる") { coordinator.retry() }
        } message: {
            Text("このQRコードに対応する薬が登録されていません。")
        }
    }
}

@MainActor
final class QrScanCoordinator: ObservableObject {
    let controller = QrCameraController()

    @Published var cameraAuthorized = false
    @Published var showConfirm = false
    @Published var showNotFound = false
    @Published var foundMedicine: Medicine?

    init() {
        controller.onCodeScanned = { [weak self] token in
            self?.handleScanned(token)
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

    private func handleScanned(_ raw: String) {
        controller.stop()
        guard let payload = QrPayloadCodec.decode(raw), payload.type == QrPayloadType.intake else {
            showNotFound = true
            return
        }
        do {
            if let medicine = try Medicine.find(payload.data.medicineId)
                ?? Medicine.findByName(payload.data.medicineName) {
                foundMedicine = medicine
                showConfirm = true
            } else {
                showNotFound = true
            }
        } catch {
            showNotFound = true
        }
    }

    func retry() {
        foundMedicine = nil
        controller.start()
    }

    func record() {
        guard let medicine = foundMedicine else { return }
        try? IntakeLog.record(medicine: medicine, slot: currentSlot(), source: .qr)
        WidgetUpdater.reloadAll()
    }

    private func currentSlot() -> TimingSlot {
        let hour = Calendar.current.component(.hour, from: Date())
        switch hour {
        case ..<12: return .morning
        case 12..<18: return .noon
        default: return .night
        }
    }
}
