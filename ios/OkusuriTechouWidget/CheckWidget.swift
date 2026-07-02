import WidgetKit
import SwiftUI

/// 服用チェックウィジェット。
/// 現在有効な薬と、当日の服用状況(済み/未)を表示する。
/// iOS16互換のためタップはアプリを開く動作(widgetURL)とし、ウィジェット上での直接チェックは
/// iOS17+のApp Intentsボタンで今後拡張可能(現状はアプリへの導線として実装)。
struct CheckWidgetEntry: TimelineEntry {
    let date: Date
    let items: [CheckItem]
    let errorMessage: String?

    struct CheckItem: Identifiable {
        let id: String
        let name: String
        let taken: Bool
    }
}

struct CheckWidgetProvider: TimelineProvider {
    func placeholder(in context: Context) -> CheckWidgetEntry {
        CheckWidgetEntry(date: Date(), items: [
            CheckWidgetEntry.CheckItem(id: "1", name: "サンプル薬", taken: false)
        ], errorMessage: nil)
    }

    func getSnapshot(in context: Context, completion: @escaping (CheckWidgetEntry) -> Void) {
        completion(buildEntry())
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<CheckWidgetEntry>) -> Void) {
        let entry = buildEntry()
        // 15分ごとに再評価(時間帯の変化や服用記録の反映のため)
        let nextUpdate = Calendar.current.date(byAdding: .minute, value: 15, to: Date()) ?? Date()
        completion(Timeline(entries: [entry], policy: .after(nextUpdate)))
    }

    private func buildEntry() -> CheckWidgetEntry {
        do {
            let today = dateString(Date())
            let actives = try Medicine.active()
            let items = try actives.map { medicine -> CheckWidgetEntry.CheckItem in
                let taken = try !IntakeLog.forMedicine(medicine.id, date: today).isEmpty
                return CheckWidgetEntry.CheckItem(id: medicine.id, name: medicine.name, taken: taken)
            }
            return CheckWidgetEntry(date: Date(), items: items, errorMessage: nil)
        } catch {
            return CheckWidgetEntry(date: Date(), items: [], errorMessage: "読み込みエラー")
        }
    }

    private func dateString(_ date: Date) -> String {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd"
        return f.string(from: date)
    }
}

struct CheckWidgetEntryView: View {
    var entry: CheckWidgetEntry

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("お薬チェック").font(.headline)
            if let errorMessage = entry.errorMessage {
                Text(errorMessage).font(.caption).foregroundColor(.red)
            } else if entry.items.isEmpty {
                Text("有効な薬はありません").font(.caption).foregroundColor(.secondary)
            } else {
                ForEach(entry.items.prefix(4)) { item in
                    HStack {
                        Image(systemName: item.taken ? "checkmark.square.fill" : "square")
                            .foregroundColor(item.taken ? .green : .primary)
                        Text(item.name)
                            .font(.caption)
                            .strikethrough(item.taken)
                            .foregroundColor(item.taken ? .secondary : .primary)
                    }
                }
            }
        }
        .padding()
        .widgetURL(URL(string: "okusuritechou://top"))
    }
}

struct CheckWidget: Widget {
    let kind = "CheckWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: CheckWidgetProvider()) { entry in
            CheckWidgetEntryView(entry: entry)
        }
        .configurationDisplayName("お薬チェック")
        .description("今日の服用状況を確認できます。タップでアプリを開きます。")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}
