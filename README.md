# medicine-manager-app(お薬手帳アプリ) モノレポ

```
okusuri-techou/
├── docs/architecture.md   # 全体設計（DBスキーマ・画面一覧・技術選定）
├── android/                # Android Studioでそのまま開いてRunできるGradleプロジェクト
└── ios/                    # xcodegen generate → Xcodeで開いてRunできるプロジェクト
```

各ディレクトリの `README.md` に実行手順があります。
- Android: `android/README.md`
- iOS: `ios/README.md`

## 現状のスコープ
両OSとも「DBスキーマ（SQLite, ActiveRecordパターン）」＋「トップ画面（有効薬チェックリストと服用記録）」のみ実装済みです。
薬リスト/登録・更新/OCRシール読取/QR/カレンダー/通知設定/ウィジェットは未実装で、`docs/architecture.md` に設計のみ記載しています。
次のステップとして画面を1つずつ追加していくことを想定しています。
