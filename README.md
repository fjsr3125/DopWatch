# DopWatch

Androidスマホの使用時間を監視し、制限を超えたらLINE通知 + Beeminderにデータを送信するアプリ。
ドーパミン中毒（Dopamine + Watch）を自覚するためのツール。

## 機能

- アプリ別のスクリーンタイム集計（UsageStats API）
- 総使用時間 / SNS / 動画のカテゴリ別制限
- 制限超過時にローカル通知 + LINE通知
- 1日2回（朝・夜）のハートビート通知で監視稼働確認
- Beeminderにスクリーンタイムを自動送信（コミットメント契約）
- WorkManagerによる15分間隔の定期チェック

## 前提条件

- Android Studio (Hedgehog以降)
- Android SDK 36 (compileSdk)
- Android 8.0+ (minSdk 26) の実機またはエミュレータ
- LINE Developers アカウント（LINE通知を使う場合）
- Beeminder アカウント（コミットメント機能を使う場合）

## ビルド

```bash
./gradlew assembleDebug
```

APKの出力先: `app/build/outputs/apk/debug/app-debug.apk`

## 実機インストール

```bash
# USBデバッグを有効化した端末を接続して
adb install app/build/outputs/apk/debug/app-debug.apk
```

APKファイルをGoogle DriveやSlack経由で端末に転送してインストールも可能。
（設定 > 提供元不明のアプリ を許可する必要あり）

## 初回セットアップ

### 1. 使用統計アクセス権限

アプリ起動時に許可画面が表示される。
「設定を開く」→ DopWatch を探して ON にする。

### 2. 通知権限（Android 13+）

使用統計を許可した後、通知権限のダイアログが表示される。
「許可」を選択。これがないと制限超過の通知が表示されない。

### 3. バッテリー最適化除外

ダッシュボード上部にバナーが表示される。
「許可する」をタップしてバッテリー最適化から除外する。
除外しないとWorkManagerの定期チェックがOSに停止される。

### 4. LINE Messaging API セットアップ

1. [LINE Developers](https://developers.line.biz/) にログイン
2. プロバイダーを作成（なければ）
3. 「Messaging API」チャネルを作成
4. チャネル設定 > Messaging API > **Channel Access Token** を発行
5. DopWatchの公式アカウントをLINEグループに招待
6. **Group ID** を取得する方法:
   - Webhook URL を設定して、グループに招待されたときのイベントから `groupId` を取得
   - または [LINE Bot Designer](https://developers.line.biz/ja/services/bot-designer/) 等で確認
7. アプリの設定画面に Channel Access Token と Group ID を入力

### 5. Beeminder セットアップ

1. [Beeminder](https://www.beeminder.com/) でアカウント作成
2. 「screentime」ゴールを作成（Do Less タイプ、単位: hours）
3. Settings > Apps & API > **Personal Auth Token** をコピー
4. アプリの設定画面に Username / Auth Token / Goal Slug を入力

### 6. アプリ内設定

設定画面（右上の歯車アイコン）で以下を設定:
- **制限時間**: 総使用時間 / SNS / 動画 それぞれのスライダー
- **LINE Messaging API**: Channel Access Token, Group ID
- **Beeminder**: Username, Auth Token, Goal Slug

## テスト

```bash
# ユニットテスト
./gradlew test

# E2Eテスト（エミュレータまたは実機が必要）
./gradlew connectedDebugAndroidTest
```

## アーキテクチャ

```
app/src/main/java/com/sora/dopwatch/
├── MainActivity.kt          # エントリポイント、Navigation
├── DopWatchApp.kt           # Hilt Application
├── data/
│   ├── AppDatabase.kt       # Room DB
│   ├── AppUsageDao.kt       # DAO
│   ├── AppUsageEntity.kt    # Entity
│   ├── UsageRepository.kt   # UsageStats API → Room
│   └── SettingsRepository.kt # DataStore Preferences
├── domain/
│   ├── ThresholdConfig.kt   # 制限値の設定モデル
│   └── CheckUsageUseCase.kt # 閾値チェック + アラート生成
├── api/
│   ├── LineMessagingClient.kt  # LINE Messaging API
│   └── BeeminderClient.kt     # Beeminder API
├── worker/
│   └── UsageCheckWorker.kt  # 15分間隔の定期チェック
├── ui/
│   ├── theme/Theme.kt
│   ├── dashboard/
│   │   ├── DashboardScreen.kt    # メイン画面
│   │   └── DashboardViewModel.kt
│   └── settings/
│       ├── SettingsScreen.kt     # 設定画面
│       └── SettingsViewModel.kt
```

**技術スタック**: Kotlin, Jetpack Compose, Hilt, Room, WorkManager, DataStore, OkHttp
