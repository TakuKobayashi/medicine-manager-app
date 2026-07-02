# okusuri-techou (iOS)

## 実行手順
1. [XcodeGen](https://github.com/yonaskolb/XcodeGen) をインストール（初回のみ）
   ```
   brew install xcodegen
   ```
2. このディレクトリ(`ios/`)で以下を実行し、`.xcodeproj` を生成
   ```
   cd ios
   xcodegen generate
   ```
3. 生成された `OkusuriTechou.xcodeproj` を Xcode で開く
4. 初回ビルド時にSwift Package Manager が GRDB.swift を自動取得します（ネット接続が必要）
5. シミュレータ/実機を選択して ▶ Run

`project.yml` を編集した場合は再度 `xcodegen generate` を実行してください。
`.xcodeproj`自体はリポジトリに含めず、`project.yml` から都度生成する運用を想定しています
（チーム開発時のコンフリクト防止のため）。`.gitignore`に`*.xcodeproj`を追加することを推奨します。

## 現状の実装範囲
- DBスキーマ（GRDB.swift, ActiveRecordパターン）一式
- トップ画面（有効な薬のチェックリスト・服用記録）
- 薬リスト画面（一覧・有効/無効トグル・更新/削除(確認ダイアログ)/QR導線/シール読み取り導線）
- 登録/更新画面（バリデーション付き、シート表示）
- シールOCR読み取り画面（AVFoundation + Vision。自動検出→Before/After確認ダイアログ→残量加算 or 新規登録）
- QRスキャン画面（AVCaptureMetadataOutput。読み取った薬を即座に服用記録）
- カレンダー画面（月表示。服用すべき回数に満たない日を薄い赤セルで表示）
- 通知設定画面（朝/昼/晩の時刻・文言・有効無効をUNUserNotificationCenterに即時反映）
- ホーム画面ウィジェット2種（WidgetKit、`OkusuriTechouWidget`ターゲット）
  - 服用チェックウィジェット: 有効な薬と当日の服用状況を表示（iOS16互換のためタップでアプリを開く方式。
    iOS17+のApp Intentsボタンでウィジェット上から直接チェックする拡張が可能）
  - 薬リストウィジェット: 登録されている薬の一覧を読み取り専用表示
  - App Group(`group.com.phantomcatworks.okusuritechou`)経由でアプリ本体とSQLiteを共有しています。
    Apple Developer Portal側で同名のApp Groupを作成し、`DEVELOPMENT_TEAM`を設定してから実行してください。
- QRコード表示画面（CoreImageで画像生成・写真ライブラリへの保存に対応。印刷ボタンはグレーアウト/Coming soon表示でスタブ関数のみ用意）

## QRコードのデータ形式
QRコードにはMessagePackでエンコードしたバイナリをBase64文字列化した内容を埋め込みます(`QR/QrPayload.swift`。Android版と同一スキーマ)。

```
{
  "type": 1,              // 1 = INTAKE(服用記録)。将来「QRから薬を新規登録」等を見越した列挙体
  "version": 1,            // ペイロードスキーマのバージョン
  "data": {
    "medicine_id": "...",        // ローカルDBの Medicine.id（照合に最優先で使用）
    "mst_medicine_id": null,     // MedicineMaster.id（将来のマスター同期用、現状null許容）
    "medicine_name": "..."       // フォールバック照合用 / 可読性のため
  }
}
```
スキャン時は `medicine_id` で検索し、見つからなければ `medicine_name` で再検索します。
MessagePackエンコードには[MessagePack.swift](https://github.com/a2/MessagePack.swift)をSPMで導入しています。

## 通知について
`UNCalendarNotificationTrigger(repeats: true)`で毎日指定時刻に繰り返し通知します。
**重要な制約**: iOSのローカル通知は登録時点の内容で確定するため、Androidのように
「発火直前にDBを見て該当薬の有無を判定する」ことはできません。代わりに、
薬の登録/更新/削除/有効切替のタイミングと、アプリがフォアグラウンドに復帰するたびに
`NotificationScheduler.rescheduleAll()`を呼び、その時点の状況に応じて通知の登録/解除を行うことで近似しています。
そのため、アプリをしばらく開かないまま薬の状態が変わった場合、次にアプリを開くまで通知内容の反映が遅れる可能性があります。

## OCR解析について
お薬シールの書式は薬局により差異が大きいため、`Data/Ocr/OcrTextParser.swift` は正規表現によるベストエフォートの抽出です。
「n日分」「1回n錠」「朝/昼/夕」等の表記を想定しています。解析結果は必ず確認ダイアログ(Before/After)で人間が確認してから保存されます。
- 通知設定/ウィジェットは実装済みです。Apple Developer側でApp Group(`group.com.phantomcatworks.okusuritechou`)を作成してください
