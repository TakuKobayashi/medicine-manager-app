import WidgetKit
import SwiftUI

/// 薬リストウィジェット。
/// 登録されている薬(有効/無効問わず)を簡易表示する読み取り専用ウィジェット。
struct ListWidgetEntry: TimelineEntry {
    let date: Date
    let items: [ListItem]
    let errorMessage: String?

    struct ListItem: Identifiable {
        let id: String
        let name: String
        let isActive: Bool
        let summary: String
    }
}

struct ListWidgetProvider: TimelineProvider {
    func placeholder(in context: Context) -> ListWidgetEntry {
        ListWidgetEntry(date: Date(), items: [
            ListWidgetEntry.ListItem(id: "1", name: "サンプル薬", isActive: true, summary: "毎日 / 残量10錠")
        ], errorMessage: nil)
    }

    func getSnapshot(in context: Context, completion: @escaping (ListWidgetEntry) -> Void) {
        completion(buildEntry())
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<ListWidgetEntry>) -> Void) {
        let entry = buildEntry()
        let nextUpdate = Calendar.current.date(byAdding: .hour, value: 1, to: Date()) ?? Date()
        completion(Timeline(entries: [entry], policy: .after(nextUpdate)))
    }

    private func buildEntry() -> ListWidgetEntry {
        do {
            let all = try Medicine.all()
            let items = all.map { medicine in
                ListWidgetEntry.ListItem(
                    id: medicine.id,
                    name: medicine.name,
                    isActive: medicine.isActive,
                    summary: summary(for: medicine)
                )
            }
            return ListWidgetEntry(date: Date(), items: items, errorMessage: nil)
        } catch {
            return ListWidgetEntry(date: Date(), items: [], errorMessage: "読み込みエラー")
        }
    }

    private func summary(for medicine: Medicine) -> String {
        let freq: String
        switch medicine.frequencyType {
        case .daily: freq = "毎日"
        case .interval: freq = "\(medicine.intervalDays ?? 1)日に1回"
        }
        let unit: String
        switch medicine.doseForm {
        case .tablet: unit = "錠"
        case .injection: unit = "目盛"
        case .other: unit = "単位"
        }
        let qty = medicine.remainingQty == medicine.remainingQty.rounded()
            ? String(Int(medicine.remainingQty))
            : String(medicine.remainingQty)
        return "\(freq) / 残量\(qty)\(unit)"
    }
}

struct ListWidgetEntryView: View {
    var entry: ListWidgetEntry

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("薬リスト").font(.headline)
            if let errorMessage = entry.errorMessage {
                Text(errorMessage).font(.caption).foregroundColor(.red)
            } else if entry.items.isEmpty {
                Text("登録されている薬はありません").font(.caption).foregroundColor(.secondary)
            } else {
                ForEach(entry.items.prefix(4)) { item in
                    VStack(alignment: .leading, spacing: 0) {
                        Text(item.name + (item.isActive ? "" : "（無効）"))
                            .font(.caption)
                            .foregroundColor(item.isActive ? .primary : .secondary)
                        Text(item.summary)
                            .font(.caption2)
                            .foregroundColor(.secondary)
                    }
                }
            }
        }
        .padding()
        .widgetURL(URL(string: "okusuritechou://list"))
    }
}

struct ListWidget: Widget {
    let kind = "ListWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: ListWidgetProvider()) { entry in
            ListWidgetEntryView(entry: entry)
        }
        .configurationDisplayName("薬リスト")
        .description("登録されている薬の一覧を表示します。")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}
