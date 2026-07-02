import Foundation
import UserNotifications

/// 通知設定(NotificationSetting)に基づいてUNUserNotificationCenterへ繰り返し通知を登録する。
///
/// 制約: iOSのローカル通知(UNCalendarNotificationTrigger)は登録時点の内容で確定するため、
/// Android(AlarmManager+BroadcastReceiver)のように「発火直前にDBを見て該当薬の有無を判定する」
/// ことはできない。そのため、薬の登録/更新/削除/有効切替のタイミングと、アプリがフォアグラウンドに
/// なるたびに `rescheduleAll()` を呼び、その時点の状況に応じて通知の登録/解除を行うことで近似する。
enum NotificationScheduler {

    static func requestAuthorizationIfNeeded() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { _, _ in }
    }

    /// 全スロットを現在のDB状態に合わせて再登録する。
    static func rescheduleAll() {
        do {
            let settings = try NotificationSetting.all()
            for setting in settings {
                try scheduleOrCancel(setting)
            }
        } catch {
            // 読み込み失敗時は何もしない(次回呼び出し時に再評価される)
        }
    }

    static func scheduleOrCancel(_ setting: NotificationSetting) throws {
        cancel(setting.slot)
        guard setting.enabled else { return }

        let noApplicable = try setting.hasNoApplicableMedicine()
        guard !noApplicable else { return }

        let content = UNMutableNotificationContent()
        content.title = "お薬手帳"
        content.body = setting.message
        content.sound = .default

        var dateComponents = DateComponents()
        dateComponents.hour = setting.hour
        dateComponents.minute = setting.minute

        let trigger = UNCalendarNotificationTrigger(dateMatching: dateComponents, repeats: true)
        let request = UNNotificationRequest(
            identifier: identifier(for: setting.slot),
            content: content,
            trigger: trigger
        )
        UNUserNotificationCenter.current().add(request)
    }

    static func cancel(_ slot: TimingSlot) {
        UNUserNotificationCenter.current()
            .removePendingNotificationRequests(withIdentifiers: [identifier(for: slot)])
    }

    private static func identifier(for slot: TimingSlot) -> String {
        "medication_reminder_\(slot.rawValue)"
    }
}
