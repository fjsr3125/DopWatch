# Contributing to DopWatch

DopWatch は初期公開段階の Android アプリです。小さな修正、再現手順の共有、README改善、テスト追加を歓迎します。

## 開発環境

- Android Studio Hedgehog 以降
- JDK 17
- Android SDK 35
- Android 8.0 以降の実機またはエミュレータ

## セットアップ

```bash
git clone https://github.com/fjsr3125/DopWatch.git
cd DopWatch
./gradlew assembleDebug
```

## 変更前の確認

```bash
./gradlew test
```

UIや権限まわりを触る場合は、実機またはエミュレータで次も確認してください。

```bash
./gradlew connectedDebugAndroidTest
```

## Issue の書き方

不具合報告には次を含めてください。

- Android バージョン
- 端末名またはエミュレータ名
- DopWatch のバージョンまたは commit
- 再現手順
- 期待した動作
- 実際の動作
- 関係するログ

認証情報、LINEの Group ID、Beeminder token、個人の利用履歴は貼らないでください。

## Pull Request の方針

- 1つのPRでは1つの目的に絞ってください。
- 既存の構成を尊重し、大きな設計変更は先にIssueで相談してください。
- UI変更はスクリーンショットを添えてください。
- 権限、認証情報、外部API送信に関わる変更は、READMEまたはSECURITYも更新してください。

## 優先してほしい貢献

- READMEの改善
- Android権限まわりの不具合修正
- WorkManagerの安定性改善
- テスト追加
- GitHub Actions の整備
- LINE / Beeminder 連携のエラーハンドリング改善
