import SwiftUI

@main
struct OkusuriTechouApp: App {
    @Environment(\.scenePhase) private var scenePhase

    init() {
        try? NotificationSetting.ensureDefaults()
        NotificationScheduler.requestAuthorizationIfNeeded()
        NotificationScheduler.rescheduleAll()
    }

    var body: some Scene {
        WindowGroup {
            TopView()
        }
        .onChange(of: scenePhase) { phase in
            if phase == .active {
                // フォアグラウンド復帰のたびに、薬の登録状況に合わせて通知を再評価する
                NotificationScheduler.rescheduleAll()
            }
        }
    }
}
