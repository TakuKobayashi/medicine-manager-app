import SwiftUI

private struct SlotUiState: Identifiable {
    var setting: NotificationSetting
    var hasNoApplicableMedicine: Bool
    var id: String { setting.slot.rawValue }
}

/// 通知設定画面。
/// 朝・昼・晩それぞれの通知時刻・文言・有効/無効を編集できる。
/// 現在有効な薬の中にそのタイミングに該当するものが無い場合はグレーアウト表示するが、選択自体は可能。
/// 保存時にUNUserNotificationCenterへ即座に反映する。
struct NotificationSettingsView: View {
    @State private var slotStates: [SlotUiState] = []
    @State private var errorMessage: String?

    var body: some View {
        Form {
            if let errorMessage {
                Text(errorMessage).foregroundColor(.red)
            }
            ForEach($slotStates) { $state in
                SlotSection(state: $state, onChanged: { updated in
                    save(updated)
                })
            }
        }
        .navigationTitle("通知設定")
        .onAppear { reload() }
    }

    private func reload() {
        do {
            let settings = try NotificationSetting.all().sorted {
                TimingSlot.allCases.firstIndex(of: $0.slot)! < TimingSlot.allCases.firstIndex(of: $1.slot)!
            }
            slotStates = try settings.map { setting in
                SlotUiState(setting: setting, hasNoApplicableMedicine: try setting.hasNoApplicableMedicine())
            }
            errorMessage = nil
        } catch {
            errorMessage = "読み込みに失敗しました: \(error.localizedDescription)"
        }
    }

    private func save(_ setting: NotificationSetting) {
        do {
            try setting.save()
            try NotificationScheduler.scheduleOrCancel(setting)
            reload()
        } catch {
            errorMessage = "保存に失敗しました: \(error.localizedDescription)"
        }
    }
}

private struct SlotSection: View {
    @Binding var state: SlotUiState
    let onChanged: (NotificationSetting) -> Void

    @State private var message: String = ""
    @State private var time: Date = Date()

    var body: some View {
        Section {
            HStack {
                Text(slotLabel(state.setting.slot))
                Spacer()
                Toggle("", isOn: Binding(
                    get: { state.setting.enabled },
                    set: { newValue in
                        var updated = state.setting
                        updated.enabled = newValue
                        onChanged(updated)
                    }
                ))
                .labelsHidden()
            }

            if state.hasNoApplicableMedicine {
                Text("現在有効な薬の中に該当タイミングのものがないため通知されません")
                    .font(.caption)
                    .foregroundColor(.gray)
            }

            DatePicker("時刻", selection: $time, displayedComponents: .hourAndMinute)
                .onChange(of: time) { newValue in
                    let comps = Calendar.current.dateComponents([.hour, .minute], from: newValue)
                    var updated = state.setting
                    updated.hour = comps.hour ?? updated.hour
                    updated.minute = comps.minute ?? updated.minute
                    onChanged(updated)
                }

            TextField("通知文", text: $message)
            Button("文言を保存") {
                var updated = state.setting
                updated.message = message
                onChanged(updated)
            }
        }
        .listRowBackground(state.hasNoApplicableMedicine ? Color(.systemGray5) : nil)
        .onAppear {
            message = state.setting.message
            time = timeFromComponents(hour: state.setting.hour, minute: state.setting.minute)
        }
        .onChange(of: state.setting.hour) { _ in
            time = timeFromComponents(hour: state.setting.hour, minute: state.setting.minute)
        }
    }

    private func timeFromComponents(hour: Int, minute: Int) -> Date {
        var comps = DateComponents()
        comps.hour = hour
        comps.minute = minute
        return Calendar.current.date(from: comps) ?? Date()
    }
}

private func slotLabel(_ slot: TimingSlot) -> String {
    switch slot {
    case .morning: return "朝"
    case .noon: return "昼"
    case .night: return "晩"
    }
}

#Preview {
    NavigationStack { NotificationSettingsView() }
}
