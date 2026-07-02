import SwiftUI
import Photos

/// QRコード表示画面。
/// 薬の情報をMessagePackでエンコードしたペイロード(QrPayloadCodec参照)からQRコード画像を生成して表示し、
/// 写真ライブラリへの保存ができる。印刷機能は今後実装予定のためボタンをグレーアウトし「Coming soon」を表示する。
struct QrDisplayView: View {
    let medicineId: String

    @State private var medicine: Medicine?
    @State private var qrImage: UIImage?
    @State private var saveMessage: String?

    var body: some View {
        VStack(spacing: 16) {
            if let medicine {
                Text(medicine.name).font(.headline)
                Text("このQRコードを薬(または袋)に貼ると、トップ画面の「QR読取」で服用記録ができます。")
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)

                if let qrImage {
                    Image(uiImage: qrImage)
                        .interpolation(.none)
                        .resizable()
                        .scaledToFit()
                        .frame(width: 240, height: 240)
                }

                HStack(spacing: 16) {
                    Button("保存") {
                        saveToPhotoLibrary()
                    }
                    .buttonStyle(.borderedProminent)

                    // 印刷機能は今後実装予定。ボタンはグレーアウトし押せないようにしておく。
                    Button("印刷 (Coming soon)") {
                        printQrCode()
                    }
                    .buttonStyle(.bordered)
                    .disabled(true)
                }

                if let saveMessage {
                    Text(saveMessage).font(.caption)
                }
            } else {
                Text("薬の情報が見つかりませんでした。")
            }
        }
        .padding()
        .navigationTitle("QRコード")
        .onAppear { load() }
    }

    private func load() {
        do {
            guard let m = try Medicine.find(medicineId) else { return }
            medicine = m
            let payload = QrPayloadCodec.encodeIntake(m)
            qrImage = QrCodeGenerator.generate(from: payload)
        } catch {
            saveMessage = "読み込みに失敗しました: \(error.localizedDescription)"
        }
    }

    private func saveToPhotoLibrary() {
        guard let qrImage else { return }
        PHPhotoLibrary.requestAuthorization(for: .addOnly) { status in
            DispatchQueue.main.async {
                guard status == .authorized || status == .limited else {
                    saveMessage = "写真ライブラリへのアクセスが許可されていません"
                    return
                }
                PHPhotoLibrary.shared().performChanges {
                    PHAssetChangeRequest.creationRequestForAsset(from: qrImage)
                } completionHandler: { success, error in
                    DispatchQueue.main.async {
                        saveMessage = success ? "画像を保存しました" : "保存に失敗しました: \(error?.localizedDescription ?? "")"
                    }
                }
            }
        }
    }

    /// QRコードの印刷機能。現状は未実装のスタブ。
    /// TODO: UIPrintInteractionController を使って実装する。
    private func printQrCode() {
        // TODO: 印刷処理を実装する
    }
}
