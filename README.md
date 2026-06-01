# DopWatch

DopWatch is an Android screen-time monitor that helps people notice and reduce compulsive app usage.

Androidスマホの使用時間を端末内で集計し、制限を超えたらローカル通知、LINE通知、Beeminder連携で行動を戻すためのオープンソースアプリです。

## Project Status

このリポジトリは初期公開版です。個人利用を前提に動く状態まで実装済みで、今後はインストール手順、テスト、リリース運用、権限まわりの安定性を継続的に整備します。

## Why DopWatch Exists

スマホの使用時間を減らすツールは多くありますが、実際に行動を変えるには「見える化」だけでなく、使いすぎた瞬間に自分や信頼できる相手へ通知し、外部のコミットメントに接続する導線が必要です。

DopWatch は次の3つを重視しています。

- 端末内の使用時間を自分で管理できること
- LINE通知で第三者チェックやグループ運用につなげられること
- Beeminder に送信して、行動改善を日々の約束として扱えること

## Features

- UsageStats API によるアプリ別スクリーンタイム集計
- 総使用時間、SNS、動画カテゴリ別の制限
- 制限超過時のローカル通知
- LINE Messaging API による通知
- 1日2回のハートビート通知による監視稼働確認
- Beeminder へのスクリーンタイム自動送信
- Google Drive JSON からのリモート設定読み込み
- WorkManager による15分間隔の定期チェック
- 端末内 Room DB への使用履歴保存
- localhost のみで参照できる使用状況API

## Tech Stack

- Kotlin
- Jetpack Compose
- Hilt
- Room
- DataStore
- WorkManager
- OkHttp
- NanoHTTPD
- Android UsageStats API

## Requirements

- Android Studio Hedgehog 以降
- JDK 17
- Android SDK 35
- Android 8.0 以降の実機またはエミュレータ
- LINE Developers アカウント（LINE通知を使う場合）
- Beeminder アカウント（Beeminder連携を使う場合）

## Build

```bash
./gradlew assembleDebug
```

APKの出力先:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Install on a Device

USBデバッグを有効化したAndroid端末を接続してから実行します。

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

APKファイルをGoogle DriveやSlack経由で端末に転送してインストールすることもできます。その場合は、Android側で提供元不明のアプリのインストールを許可してください。

## First-Time Setup

### 1. 使用統計アクセス権限

アプリ起動時に許可画面が表示されます。

「設定を開く」から DopWatch を探し、使用統計アクセスを有効にしてください。

### 2. 通知権限

Android 13 以降では通知権限の許可が必要です。これを許可しないと、制限超過時のローカル通知が表示されません。

### 3. バッテリー最適化除外

ダッシュボード上部に表示されるバナーから、バッテリー最適化の除外を許可してください。除外しない場合、OSにより定期チェックが止まることがあります。

### 4. LINE Messaging API

LINE通知を使う場合のみ設定します。

1. [LINE Developers](https://developers.line.biz/) にログイン
2. プロバイダーを作成
3. Messaging API チャネルを作成
4. Channel Access Token を発行
5. DopWatch用のLINE公式アカウントを通知先グループに招待
6. Webhookイベントなどから Group ID を取得
7. DopWatch の設定画面に Channel Access Token と Group ID を入力

### 5. Beeminder

Beeminder連携を使う場合のみ設定します。

1. [Beeminder](https://www.beeminder.com/) でアカウントを作成
2. `screentime` などの Do Less ゴールを作成
3. Apps & API から Personal Auth Token を取得
4. DopWatch の設定画面に Username、Auth Token、Goal Slug を入力

## Test

```bash
./gradlew test
```

実機またはエミュレータを使うUIテスト:

```bash
./gradlew connectedDebugAndroidTest
```

## Privacy and Security

- スクリーンタイム履歴は端末内の Room DB に保存されます。
- LINE と Beeminder への送信は、ユーザーが設定画面で認証情報を入力した場合のみ実行されます。
- LINE / Beeminder のトークンは Android DataStore に保存されます。
- 使用状況APIは `127.0.0.1:8080` のみにバインドし、同じネットワーク上の別端末から直接参照できないようにしています。
- 認証情報や個人の利用履歴をIssueに貼らないでください。脆弱性の報告は [SECURITY.md](SECURITY.md) を確認してください。

## Architecture

```text
app/src/main/java/com/sora/dopwatch/
├── MainActivity.kt             # エントリポイント、Navigation
├── DopWatchApp.kt              # Hilt Application、WorkManager起動
├── data/
│   ├── AppDatabase.kt          # Room DB
│   ├── AppUsageDao.kt          # DAO
│   ├── AppUsageEntity.kt       # Entity
│   ├── UsageRepository.kt      # UsageStats APIから使用時間を集計
│   └── SettingsRepository.kt   # DataStore Preferences
├── domain/
│   ├── ThresholdConfig.kt      # 制限値の設定モデル
│   └── CheckUsageUseCase.kt    # 閾値チェックとアラート生成
├── api/
│   ├── LineMessagingClient.kt  # LINE Messaging API
│   ├── BeeminderClient.kt      # Beeminder API
│   ├── RemoteConfigClient.kt   # Google Drive JSON設定
│   └── LocalApiServer.kt       # localhost usage API
├── worker/
│   └── UsageCheckWorker.kt     # 15分間隔の定期チェック
└── ui/
    ├── dashboard/
    ├── settings/
    └── theme/
```

## Roadmap

- v0.1.0 のGitHub Release作成
- READMEへのスクリーンショット追加
- Android権限エラー時の案内改善
- Unit test と UI test の拡充
- GitHub Actions の connected test 対応
- Local API のオン/オフ設定
- 通知テンプレートのカスタマイズ

## Contributing

Issue、改善提案、ドキュメント修正を歓迎します。開発の進め方は [CONTRIBUTING.md](CONTRIBUTING.md) を確認してください。

## License

MIT License. See [LICENSE](LICENSE).
