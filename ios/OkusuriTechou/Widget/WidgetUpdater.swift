import Foundation
import WidgetKit

/// 薬や服用記録が変更された際に、ホーム画面ウィジェットのタイムラインを更新するためのヘルパー。
enum WidgetUpdater {
    static func reloadAll() {
        WidgetCenter.shared.reloadAllTimelines()
    }
}
