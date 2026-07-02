import Foundation

/// お薬シールのOCR結果から薬名・1回服用量・服用タイミング・日数を抽出する簡易パーサー。
/// シールの書式は薬局によって差異が大きいため、これはベストエフォートの抽出であり、
/// 確認ダイアログで人間が最終確認することを前提とする。
enum OcrTextParser {

    struct ParsedSticker {
        var name: String?
        var doseAmount: Double?
        var timingMorning: Bool
        var timingNoon: Bool
        var timingNight: Bool
        var days: Int?
        var rawText: String

        /// 解析結果として「それらしい」情報が取れたか（自動遷移の判定に使用）
        var isConfident: Bool {
            guard let name, !name.isEmpty, doseAmount != nil else { return false }
            return timingMorning || timingNoon || timingNight
        }

        var requiredTimingsCount: Int {
            [timingMorning, timingNoon, timingNight].filter { $0 }.count
        }

        /// 加算すべき残量 = 1回量 × 1日のタイミング数 × 日数
        var computedAddQuantity: Double? {
            guard let days, let doseAmount else { return nil }
            let timings = requiredTimingsCount
            guard timings > 0 else { return nil }
            return doseAmount * Double(timings) * Double(days)
        }
    }

    private static let daysRegex = try! NSRegularExpression(pattern: #"(\d+)\s*日分"#)
    private static let perDoseRegex = try! NSRegularExpression(pattern: #"1回\s*(\d+(?:\.\d+)?)\s*(錠|カプセル|包|mL|ml|目盛)"#)
    private static let perDayCountRegex = try! NSRegularExpression(pattern: #"1日\s*(\d+)\s*回"#)
    private static let keywordPattern = try! NSRegularExpression(pattern: #"日分|1日|1回|錠|カプセル|包|mg|食後|食前|就寝"#)

    static func parse(_ rawText: String) -> ParsedSticker {
        let lines = rawText
            .components(separatedBy: .newlines)
            .map { $0.trimmingCharacters(in: .whitespaces) }
            .filter { !$0.isEmpty }

        let days = firstMatch(daysRegex, in: rawText, group: 1).flatMap { Int($0) }
        let doseAmount = firstMatch(perDoseRegex, in: rawText, group: 1).flatMap { Double($0) }

        let timingMorning = rawText.contains("朝")
        let timingNoon = rawText.contains("昼")
        let timingNight = rawText.contains("夕") || rawText.contains("晩") || rawText.contains("就寝")

        var finalMorning = timingMorning
        var finalNoon = timingNoon
        var finalNight = timingNight

        if !timingMorning, !timingNoon, !timingNight,
           let perDayCountStr = firstMatch(perDayCountRegex, in: rawText, group: 1),
           let perDayCount = Int(perDayCountStr) {
            switch perDayCount {
            case 1: finalMorning = true
            case 2: finalMorning = true; finalNight = true
            default: finalMorning = true; finalNoon = true; finalNight = true
            }
        }

        let nameCandidate = lines.first { line in
            keywordPattern.firstMatch(in: line, range: NSRange(line.startIndex..., in: line)) == nil
        }

        return ParsedSticker(
            name: nameCandidate,
            doseAmount: doseAmount,
            timingMorning: finalMorning,
            timingNoon: finalNoon,
            timingNight: finalNight,
            days: days,
            rawText: rawText
        )
    }

    static func guessDoseForm() -> DoseForm { .tablet }
    static func guessFrequencyType() -> FrequencyType { .daily }

    private static func firstMatch(_ regex: NSRegularExpression, in text: String, group: Int) -> String? {
        let range = NSRange(text.startIndex..., in: text)
        guard let match = regex.firstMatch(in: text, range: range),
              let r = Range(match.range(at: group), in: text) else { return nil }
        return String(text[r])
    }
}
