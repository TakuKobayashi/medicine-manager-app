# okusuri-techou (Android)

## 実行手順
1. Android Studio (Hedgehog以降) で `android/` ディレクトリを開く
2. 初回open時に「Gradle Wrapper jarがありません」と出た場合は、Android Studioの提案に従い
   `Sync Project with Gradle Files` を実行すると自動取得されます。
   （手動の場合: ターミナルで `gradle wrapper --gradle-version 8.7` を実行）
3. JDK 17 / Android Gradle Plugin 8.5.2 を使用します（Android Studio標準のJDKでOK）
4. 上部の ▶ Run ボタンでエミュレータ/実機にインストールして起動できます

## 現状の実装範囲
- DBスキーマ（Room, ActiveRecordパターン）一式
- トップ画面（有効な薬のチェックリスト・服用記録）
- 薬リスト画面（一覧・有効/無効トグル・更新/削除(確認ダイアログ)/QR導線/シール読み取り導線）
- 登録/更新画面（バリデーション付き）
- シールOCR読み取り画面（CameraX + ML Kit日本語テキスト認識。自動検出→Before/After確認ダイアログ→残量加算 or 新規登録）
- QRスキャン画面（CameraX + ML Kit バーコード認識。読み取った薬を即座に服用記録）
- カレンダー画面（月表示。服用すべき回数に満たない日を薄い赤セルで表示）
- 通知設定画面（朝/昼/晩の時刻・文言・有効無効をAlarmManagerに即時反映。端末再起動後も自動で再登録）
- ホーム画面ウィジェット2種（Glance for Compose）
  - 服用チェックウィジェット: 有効な薬を一覧表示し、タップで即座に服用記録（服用済みはグレーアウトしタップ不可）
  - 薬リストウィジェット: 登録されている薬の一覧を読み取り専用表示
- QRコード表示画面（ZXingで画像生成・MediaStoreへの保存に対応。印刷ボタンはグレーアウト/Coming soon表示でスタブ関数のみ用意）

## QRコードのデータ形式
QRコードにはMessagePackでエンコードしたバイナリをBase64文字列化した内容を埋め込みます(`data/qr/QrPayload.kt`)。

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

## 通知について
- AlarmManager(`setExactAndAllowWhileIdle`)で指定時刻に`AlarmReceiver`を起動し、発火直前にDBを参照して
  「該当タイミングの有効な薬が無ければ通知を出さない」判定を行います(アプリがバックグラウンド/未起動でも動作)。
  通知後は自動で翌日分を再登録します。
- 端末再起動時は`BootReceiver`(`BOOT_COMPLETED`)が全件再登録します。
- Android 12+ では正確なアラームの権限(`SCHEDULE_EXACT_ALARM`)が必要な場合があります。許可されない端末では
  `AlarmManager.set()`へフォールバックします(数分程度のずれが生じる可能性があります)。

## OCR解析について
お薬シールの書式は薬局により差異が大きいため、`data/ocr/OcrTextParser.kt` は正規表現によるベストエフォートの抽出です。
「n日分」「1回n錠」「朝/昼/夕」等の表記を想定しています。解析結果は必ず確認ダイアログ(Before/After)で人間が確認してから保存されます。
