import CoreImage
import CoreImage.CIFilterBuiltins
import UIKit

/// 薬のqrToken文字列からQRコードのUIImageを生成する。
enum QrCodeGenerator {
    static func generate(from content: String, sizePoints: CGFloat = 512) -> UIImage? {
        let filter = CIFilter.qrCodeGenerator()
        filter.message = Data(content.utf8)
        filter.correctionLevel = "M"

        guard let outputImage = filter.outputImage else { return nil }

        let scale = sizePoints / outputImage.extent.width
        let transformed = outputImage.transformed(by: CGAffineTransform(scaleX: scale, y: scale))

        let context = CIContext()
        guard let cgImage = context.createCGImage(transformed, from: transformed.extent) else { return nil }
        return UIImage(cgImage: cgImage)
    }
}
