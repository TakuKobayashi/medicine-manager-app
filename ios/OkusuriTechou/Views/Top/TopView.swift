import SwiftUI

/// トップ画面: 現在有効な薬をチェックリスト表示し、服用記録を行う最小実装。
/// 薬リスト/カレンダー/通知設定/QRスキャン画面は今後 NavigationStack で接続する。
struct TopView: View {
    @State private var medicines: [Medicine] = []
    @State private var takenMedicineIds: Set<String> = []
    @State private var errorMessage: String?

    private var today: String {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd"
        return f.string(from: Date())
    }

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading) {
                HStack {
                    NavigationLink("薬リスト") { MedicineListView() }
                    Spacer()
                    NavigationLink("カレンダー") { CalendarView() }
                    Spacer()
                    NavigationLink("QR読取") { QrScanView(onDone: { reload() }) }
                }
                .padding(.horizontal)

                HStack {
                    Spacer()
                    NavigationLink("通知設定") { NotificationSettingsView() }
                }
                .padding(.horizontal)

                if let errorMessage {
                    Text(errorMessage).foregroundColor(.red).padding(.horizontal)
                }

                if medicines.isEmpty {
                    Text("現在有効な薬は登録されていません。")
                        .foregroundColor(.secondary)
                        .padding()
                }

                List(medicines) { medicine in
                    let taken = takenMedicineIds.contains(medicine.id)
                    HStack {
                        VStack(alignment: .leading) {
                            Text(medicine.name)
                            Text(taken ? "服用済み" : "未服用")
                                .font(.caption)
                                .foregroundColor(taken ? .gray : .secondary)
                        }
                        Spacer()
                        Button {
                            recordTaken(medicine)
                        } label: {
                            Image(systemName: taken ? "checkmark.square.fill" : "square")
                        }
                        .disabled(taken)
                    }
                }
                .listStyle(.plain)
            }
            .navigationTitle("お薬手帳")
            .onAppear { reload() }
        }
    }

    private func reload() {
        do {
            let actives = try Medicine.active()
            medicines = actives
            var taken: Set<String> = []
            for m in actives {
                if try !IntakeLog.forMedicine(m.id, date: today).isEmpty {
                    taken.insert(m.id)
                }
            }
            takenMedicineIds = taken
        } catch {
            errorMessage = "読み込みに失敗しました: \(error.localizedDescription)"
        }
    }

    private func recordTaken(_ medicine: Medicine) {
        do {
            try IntakeLog.record(medicine: medicine, slot: currentSlot())
            reload()
            WidgetUpdater.reloadAll()
        } catch {
            errorMessage = "記録に失敗しました: \(error.localizedDescription)"
        }
    }

    /// 現在時刻から朝/昼/晩のタイミングを簡易判定する（後で通知設定の時刻と連動させる）
    private func currentSlot() -> TimingSlot {
        let hour = Calendar.current.component(.hour, from: Date())
        switch hour {
        case ..<12: return .morning
        case 12..<18: return .noon
        default: return .night
        }
    }
}

#Preview {
    TopView()
}
