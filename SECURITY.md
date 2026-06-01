# Security Policy

## Supported Versions

DopWatch は初期公開段階のため、現時点では `main` ブランチのみをサポート対象とします。GitHub Release を作成した後は、最新の安定版をサポート対象にします。

## Reporting a Vulnerability

脆弱性を見つけた場合は、公開Issueではなく GitHub Security Advisory から報告してください。

公開Issueには次の情報を貼らないでください。

- LINE Channel Access Token
- LINE Group ID
- Beeminder Auth Token
- Google Drive File ID
- 個人のスクリーンタイム履歴
- 端末内データベースの中身

## Security Notes

- DopWatch は Android の UsageStats API を使うため、使用統計アクセス権限が必要です。
- 外部送信は、ユーザーが LINE または Beeminder を設定した場合のみ行われます。
- ローカル使用状況APIは `127.0.0.1:8080` のみで待ち受けます。
- トークンは Android DataStore に保存されます。端末自体が侵害された場合の保護は保証できません。

## Response

報告を受け取ったら内容を確認し、再現性、影響範囲、修正方針を整理します。重大な問題は修正後にリリースノートで告知します。
