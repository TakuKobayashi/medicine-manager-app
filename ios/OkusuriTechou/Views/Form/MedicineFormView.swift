import SwiftUI

/// 薬の登録/更新画面。
/// medicineId が nil なら新規登録、非nilなら既存薬を読み込んで更新する。
struct MedicineFormView: View {
    let medicineId: String?
    let onSaved: () -> Void

    @Environment(\.dismiss) private var dismiss

    @State private var existing: Medicine?
    @State private var name: String = ""
    @State private var doseForm: DoseForm = .tablet
    @State private var doseAmountText: String = "1"
    @State private var frequencyType: FrequencyType = .daily
    @State private var intervalDaysText: String = "2"
    @State private var timingMorning = false
    @State private var timingNoon = false
    @State private var timingNight = false
    @State private var remainingQtyText: String = "0"
    @State private var isActive = true
    @State private var errorMessage: String?

    private var isEditing: Bool { medicineId != nil }

    var body: some View {
        Form {
            Section("基本情報") {
                TextField("薬の名前", text: $name)

                Picker("剤形", selection: $doseForm) {
                    Text("錠剤").tag(DoseForm.tablet)
                    Text("注射").tag(DoseForm.injection)
                    Text("その他").tag(DoseForm.other)
                }

                TextField(
                    doseForm == .injection ? "1回服用量(目盛)" : "1回服用量(錠数)",
                    text: $doseAmountText
                )
                .keyboardType(.decimalPad)
            }

            Section("頻度") {
                Picker("頻度", selection: $frequencyType) {
                    Text("毎日").tag(FrequencyType.daily)
                    Text("n日に1回").tag(FrequencyType.interval)
                }
                .pickerStyle(.segmented)

                if frequencyType == .interval {
                    TextField("何日に1回か", text: $intervalDaysText)
                        .keyboardType(.numberPad)
                }
            }

            Section("服用タイミング") {
                Toggle("朝", isOn: $timingMorning)
                Toggle("昼", isOn: $timingNoon)
                Toggle("晩", isOn: $timingNight)
            }

            Section("在庫") {
                TextField("残量", text: $remainingQtyText)
                    .keyboardType(.decimalPad)
                Toggle("有効", isOn: $isActive)
            }

            if let errorMessage {
                Text(errorMessage).foregroundColor(.red)
            }
        }
        .navigationTitle(isEditing ? "薬の更新" : "薬の登録")
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("キャンセル") { dismiss() }
            }
            ToolbarItem(placement: .confirmationAction) {
                Button("保存") { save() }
            }
        }
        .onAppear { load() }
    }

    private func load() {
        guard let medicineId else { return }
        do {
            guard let m = try Medicine.find(medicineId) else { return }
            existing = m
            name = m.name
            doseForm = m.doseForm
            doseAmountText = formatNumber(m.doseAmount)
            frequencyType = m.frequencyType
            intervalDaysText = String(m.intervalDays ?? 2)
            timingMorning = m.timingMorning
            timingNoon = m.timingNoon
            timingNight = m.timingNight
            remainingQtyText = formatNumber(m.remainingQty)
            isActive = m.isActive
        } catch {
            errorMessage = "読み込みに失敗しました: \(error.localizedDescription)"
        }
    }

    private func save() {
        guard !name.trimmingCharacters(in: .whitespaces).isEmpty else {
            errorMessage = "薬の名前を入力してください"; return
        }
        guard let amount = Double(doseAmountText), amount > 0 else {
            errorMessage = "1回服用量を正しく入力してください"; return
        }
        guard let remaining = Double(remainingQtyText), remaining >= 0 else {
            errorMessage = "残量を正しく入力してください"; return
        }
        var interval: Int? = nil
        if frequencyType == .interval {
            guard let v = Int(intervalDaysText), v > 0 else {
                errorMessage = "服用間隔(日数)を正しく入力してください"; return
            }
            interval = v
        }
        guard timingMorning || timingNoon || timingNight else {
            errorMessage = "朝・昼・晩のうち少なくとも1つを選択してください"; return
        }
        errorMessage = nil

        do {
            if var current = existing {
                current.name = name
                current.doseForm = doseForm
                current.doseAmount = amount
                current.frequencyType = frequencyType
                current.intervalDays = interval
                current.timingMorning = timingMorning
                current.timingNoon = timingNoon
                current.timingNight = timingNight
                current.remainingQty = remaining
                current.isActive = isActive
                try current.save()
            } else {
                try Medicine.create(
                    name: name,
                    doseForm: doseForm,
                    doseAmount: amount,
                    frequencyType: frequencyType,
                    intervalDays: interval,
                    timingMorning: timingMorning,
                    timingNoon: timingNoon,
                    timingNight: timingNight,
                    remainingQty: remaining,
                    isActive: isActive
                )
            }
            onSaved()
            NotificationScheduler.rescheduleAll()
            WidgetUpdater.reloadAll()
            dismiss()
        } catch {
            errorMessage = "保存に失敗しました: \(error.localizedDescription)"
        }
    }
}

private func formatNumber(_ value: Double) -> String {
    value == value.rounded() ? String(Int(value)) : String(value)
}

#Preview {
    NavigationStack { MedicineFormView(medicineId: nil, onSaved: {}) }
}
