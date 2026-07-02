import SwiftUI

private struct DayResult: Identifiable {
    let date: Date
    let dayNumber: Int
    let requiredCount: Int
    let actualCount: Int

    var id: Int { dayNumber }

    /// 必要回数が設定されているのに実績が満たない日。必要回数0(その日飲む薬がない)はOK扱い。
    var isShortfall: Bool { requiredCount > 0 && actualCount < requiredCount }
}

/// カレンダー画面。
/// 各日について「その日服用すべき回数(有効な薬のうち該当日にdueなものの朝昼晩タイミング合計)」と
/// 「実際に記録された服用回数」を比較し、不足している日のセルを薄い赤で表示する。
struct CalendarView: View {
    @State private var monthOffset = 0
    @State private var days: [DayResult] = []
    @State private var errorMessage: String?

    private let calendar = Calendar.current

    private var displayedMonth: Date {
        calendar.date(byAdding: .month, value: monthOffset, to: Date()) ?? Date()
    }

    private let weekdaySymbols = ["日", "月", "火", "水", "木", "金", "土"]
    private let columns = Array(repeating: GridItem(.flexible()), count: 7)

    var body: some View {
        VStack {
            HStack {
                Button { monthOffset -= 1 } label: { Image(systemName: "chevron.left") }
                Spacer()
                Text(monthTitle)
                    .font(.headline)
                Spacer()
                Button { monthOffset += 1 } label: { Image(systemName: "chevron.right") }
            }
            .padding(.horizontal)

            HStack {
                ForEach(weekdaySymbols, id: \.self) { symbol in
                    Text(symbol).frame(maxWidth: .infinity)
                }
            }
            .font(.caption)
            .foregroundColor(.secondary)

            if let errorMessage {
                Text(errorMessage).foregroundColor(.red)
            }

            LazyVGrid(columns: columns, spacing: 4) {
                ForEach(0..<leadingBlankCount, id: \.self) { _ in
                    Color.clear.frame(height: 48)
                }
                ForEach(days) { day in
                    dayCell(day)
                }
            }
            .padding(.horizontal, 4)

            Spacer()
        }
        .navigationTitle("カレンダー")
        .onAppear { reload() }
        .onChange(of: monthOffset) { _ in reload() }
    }

    private var monthTitle: String {
        let f = DateFormatter()
        f.dateFormat = "yyyy年 M月"
        f.locale = Locale(identifier: "ja_JP")
        return f.string(from: displayedMonth)
    }

    private var leadingBlankCount: Int {
        guard let firstDay = calendar.date(from: calendar.dateComponents([.year, .month], from: displayedMonth)) else { return 0 }
        // weekday: 1=日曜 ... 7=土曜 → 先頭の空セル数はそのまま (weekday - 1)
        return calendar.component(.weekday, from: firstDay) - 1
    }

    @ViewBuilder
    private func dayCell(_ day: DayResult) -> some View {
        VStack(spacing: 2) {
            Text("\(day.dayNumber)")
            if day.requiredCount > 0 {
                Text("\(day.actualCount)/\(day.requiredCount)")
                    .font(.caption2)
            }
        }
        .frame(maxWidth: .infinity, minHeight: 48)
        .background(day.isShortfall ? Color.red.opacity(0.2) : Color.clear)
        .cornerRadius(4)
    }

    private func reload() {
        do {
            days = try computeMonth(displayedMonth)
            errorMessage = nil
        } catch {
            errorMessage = "読み込みに失敗しました: \(error.localizedDescription)"
        }
    }

    private func computeMonth(_ month: Date) throws -> [DayResult] {
        let allMedicines = try Medicine.all()
        guard let range = calendar.range(of: .day, in: .month, for: month),
              let firstDay = calendar.date(from: calendar.dateComponents([.year, .month], from: month)) else {
            return []
        }

        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyy-MM-dd"

        var results: [DayResult] = []
        for dayNumber in range {
            guard let date = calendar.date(byAdding: .day, value: dayNumber - 1, to: firstDay) else { continue }
            let dateStr = dateFormatter.string(from: date)

            let required = allMedicines.reduce(0) { sum, medicine in
                sum + (medicine.isDue(on: date, calendar: calendar) ? medicine.requiredTimingsCount() : 0)
            }
            let actual = try IntakeLog.forDate(dateStr).count

            results.append(DayResult(date: date, dayNumber: dayNumber, requiredCount: required, actualCount: actual))
        }
        return results
    }
}

#Preview {
    NavigationStack { CalendarView() }
}
