# お薬手帳アプリ 設計ドキュメント

## 0. 方針
- ローカルファースト。薬の情報・服用記録はサーバに一切送信しない。
- Android: Kotlin + Jetpack Compose + Room（ActiveRecordパターンのラッパー）
- iOS: SwiftUI + GRDB.swift（ActiveRecordパターンのラッパー）、プロジェクトはXcodeGen(`project.yml`)で生成
- モノレポ構成。`android/`と`ios/`はそれぞれ単独でAndroid Studio / Xcodeから実行可能。

## 1. DBスキーマ（両OS共通の論理設計）

### medicine_master（将来の薬データベース同期用マスター）
id(TEXT PK) / name / genericName / manufacturer / defaultUnit / source / syncedAt / createdAt / updatedAt

### medicines（ユーザー登録薬）
id(TEXT PK) / masterId(FK NULL) / name / doseForm(tablet/injection/other) / doseAmount /
frequencyType(daily/interval) / intervalDays(NULL) / timingMorning,timingNoon,timingNight(bool) /
remainingQty / isActive(bool) / qrToken(UNIQUE) / createdAt / updatedAt

### intake_logs（服用記録）
id(TEXT PK) / medicineId(FK) / timingSlot(morning/noon/night) / takenAt / takenDate("YYYY-MM-DD") /
source(checkbox/qr) / createdAt

### notification_settings
slot(PK: morning/noon/night) / hour / minute / message / enabled
デフォルト: 朝8:00 / 昼12:00 / 夜18:00

## 2. 画面一覧
1. トップ画面（有効薬チェックリスト、QR読取、各画面への導線）
2. 薬リスト画面（一覧・登録・更新・削除・QR表示・シールOCR起動）
3. 登録/更新画面
4. OCRカメラ画面（シール自動検出→Before/After確認ダイアログ→残量加算 or 新規登録）
5. QRスキャン画面（トップから。読み取ったら服用記録）
6. QRコード表示画面（薬リストから。保存/印刷）
7. カレンダー画面（必要回数に満たない日を薄赤セル表示）
8. 通知設定画面（朝昼晩の時刻・文言、該当薬が無い枠はグレーアウト＋注記）
9. ウィジェット（服用チェック／薬リスト）

## 3. 技術選定
- Android: CameraX + ML Kit(Text Recognition / Barcode Scanning) + ZXing(QR生成)、通知はAlarmManager、ウィジェットはGlance
- iOS: Vision/VisionKit(OCR・バーコード) + CoreImage(QR生成)、通知はUNUserNotificationCenter、ウィジェットはWidgetKit
- ウィジェット共有: Android=同一プロセスのRoomインスタンス共有 / iOS=App Group共有コンテナのSQLiteファイル

## 4. 今回の納品スコープ
データ層（DBスキーマ・ActiveRecordパターン）に加え、**両OSともそのままAndroid Studio / Xcodeでビルド・実行できる最小動作プロジェクト**（トップ画面のみ実装、他画面はスタブ）を提供。
画面の肉付け（OCR/QR/カレンダー/通知/ウィジェット）は次のステップで順次追加していく前提。
