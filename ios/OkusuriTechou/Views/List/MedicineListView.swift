import SwiftUI

/// 薬リスト画面。
/// - 新規登録(ツールバー)
/// - 各行: 更新/削除(確認ダイアログ)/QRコード(スタブ。今後ZXing相当のCoreImage実装を追加)
/// - 有効/無効をその場でトグル
struct MedicineListView: View {
    @State private var medicines: [Medicine] = []
    @State private var pendingDelete: Medicine?
    @State private var errorMessage: String?
    @State private var showForm = false
    @State private var editingMedicineId: String?
    @State private var showQr = false
    @State private var qrMedicineId: String?

    var body: some View {
        List {
            ForEach(medicines) { medicine in
                VStack(alignment: .leading, spacing: 4) {
                    HStack {
                        Text(medicine.name).font(.headline)
                        Spacer()
                        Toggle("", isOn: Binding(
                            get: { medicine.isActive },
                            set: { newValue in toggleActive(medicine, isActive: newValue) }
                        ))
                        .labelsHidden()
                    }
                    Text(frequencyLabel(medicine))
                    Text(timingLabel(medicine))
                    Text("1回量: \(formatAmount(medicine.doseAmount)) \(unitLabel(medicine.doseForm))")
                    Text("残量: \(formatAmount(medicine.remainingQty)) \(unitLabel(medicine.doseForm))")
                        .foregroundColor(.secondary)

                    HStack {
                        Spacer()
                        Button {
                            qrMedicineId = medicine.id
                            showQr = true
                        } label: {
                            Image(systemName: "qrcode")
                        }
                        Button {
                            editingMedicineId = medicine.id
                            showForm = true
                        } label: {
                            Image(systemName: "square.and.pencil")
                        }
                        Button(role: .destructive) {
                            pendingDelete = medicine
                        } label: {
                            Image(systemName: "trash")
                        }
                    }
                    .buttonStyle(.borderless)
                }
                .padding(.vertical, 4)
            }
        }
        .listStyle(.plain)
        .navigationTitle("薬リスト")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                NavigationLink {
                    OcrCaptureView(onDone: { reload() })
                } label: {
                    Image(systemName: "camera.viewfinder")
                }
            }
            ToolbarItem(placement: .navigationBarTrailing) {
                Button {
                    editingMedicineId = nil
                    showForm = true
                } label: {
                    Image(systemName: "plus")
                }
            }
        }
        .onAppear { reload() }
        .alert("削除確認", isPresented: Binding(
            get: { pendingDelete != nil },
            set: { if !$0 { pendingDelete = nil } }
        )) {
            Button("キャンセル", role: .cancel) { pendingDelete = nil }
            Button("OK", role: .destructive) {
                if let target = pendingDelete { delete(target) }
            }
        } message: {
            Text("「\(pendingDelete?.name ?? "")」を削除します。よろしいですか？")
        }
        .sheet(isPresented: $showForm) {
            NavigationStack {
                MedicineFormView(medicineId: editingMedicineId) {
                    showForm = false
                    reload()
                }
            }
        }
        .sheet(isPresented: $showQr) {
            NavigationStack {
                if let id = qrMedicineId {
                    QrDisplayView(medicineId: id)
                }
            }
        }
        .overlay {
            if let errorMessage {
                Text(errorMessage).foregroundColor(.red).padding()
            }
        }
    }

    private func reload() {
        do {
            medicines = try Medicine.all()
        } catch {
            errorMessage = "読み込みに失敗しました: \(error.localizedDescription)"
        }
    }

    private func toggleActive(_ medicine: Medicine, isActive: Bool) {
        do {
            var copy = medicine
            copy.isActive = isActive
            try copy.save()
            reload()
            NotificationScheduler.rescheduleAll()
            WidgetUpdater.reloadAll()
        } catch {
            errorMessage = "更新に失敗しました: \(error.localizedDescription)"
        }
    }

    private func delete(_ medicine: Medicine) {
        do {
            try medicine.destroy()
            pendingDelete = nil
            reload()
            NotificationScheduler.rescheduleAll()
            WidgetUpdater.reloadAll()
        } catch {
            errorMessage = "削除に失敗しました: \(error.localizedDescription)"
        }
    }
}

private func frequencyLabel(_ m: Medicine) -> String {
    switch m.frequencyType {
    case .daily: return "毎日"
    case .interval: return "\(m.intervalDays ?? 1)日に1回"
    }
}

private func timingLabel(_ m: Medicine) -> String {
    var parts: [String] = []
    if m.timingMorning { parts.append("朝") }
    if m.timingNoon { parts.append("昼") }
    if m.timingNight { parts.append("晩") }
    return parts.isEmpty ? "タイミング未設定" : parts.joined(separator: "・")
}

private func unitLabel(_ form: DoseForm) -> String {
    switch form {
    case .tablet: return "錠"
    case .injection: return "目盛"
    case .other: return "単位"
    }
}

private func formatAmount(_ value: Double) -> String {
    value == value.rounded() ? String(Int(value)) : String(value)
}

#Preview {
    NavigationStack { MedicineListView() }
}
