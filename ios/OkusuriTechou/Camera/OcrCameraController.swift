import AVFoundation
import UIKit
import Vision

/// シールのテキストをライブカメラから検出するためのコントローラ。
/// それらしいテキストが取れたタイミングで onTextRecognized を1回だけ呼ぶ。
final class OcrCameraController: NSObject, AVCaptureVideoDataOutputSampleBufferDelegate {
    let session = AVCaptureSession()
    private let videoOutput = AVCaptureVideoDataOutput()
    private let queue = DispatchQueue(label: "ocr.camera.queue")
    private var hasResult = false
    private var lastAnalysisTime = Date.distantPast

    var onTextRecognized: ((String) -> Void)?

    func start() {
        guard let device = AVCaptureDevice.default(for: .video),
              let input = try? AVCaptureDeviceInput(device: device) else { return }

        session.beginConfiguration()
        session.sessionPreset = .high
        if session.canAddInput(input) { session.addInput(input) }
        videoOutput.setSampleBufferDelegate(self, queue: queue)
        if session.canAddOutput(videoOutput) { session.addOutput(videoOutput) }
        session.commitConfiguration()

        queue.async { [weak self] in
            self?.session.startRunning()
        }
    }

    func stop() {
        queue.async { [weak self] in
            self?.session.stopRunning()
        }
    }

    func captureOutput(
        _ output: AVCaptureOutput,
        didOutput sampleBuffer: CMSampleBuffer,
        from connection: AVCaptureConnection
    ) {
        guard !hasResult else { return }
        // 連続解析による負荷を抑えるため0.4秒間隔に間引く
        let now = Date()
        guard now.timeIntervalSince(lastAnalysisTime) > 0.4 else { return }
        lastAnalysisTime = now

        guard let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else { return }

        let request = VNRecognizeTextRequest { [weak self] request, _ in
            guard let self, !self.hasResult else { return }
            guard let results = request.results as? [VNRecognizedTextObservation] else { return }
            let text = results.compactMap { $0.topCandidates(1).first?.string }.joined(separator: "\n")
            guard !text.isEmpty else { return }

            let parsed = OcrTextParser.parse(text)
            if parsed.isConfident {
                self.hasResult = true
                DispatchQueue.main.async {
                    self.onTextRecognized?(text)
                }
            }
        }
        request.recognitionLevel = .accurate
        request.recognitionLanguages = ["ja-JP", "en-US"]
        request.usesLanguageCorrection = true

        let handler = VNImageRequestHandler(cvPixelBuffer: pixelBuffer, orientation: .right)
        try? handler.perform([request])
    }
}
