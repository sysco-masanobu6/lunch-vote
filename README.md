# 🗳️ lunch-vote — SAP BTP ランチ投票アプリ

SAP BTP（Cloud Foundry）での **開発 → 自動デプロイ（CI/CD）** を練習するためのサンプルアプリです。
「今日のランチどこ行く？」をみんなで投票して決めます 🍜🍛🍣🥗

## 技術スタック
- **CAP (Cloud Application Programming Model) / Java** — SapMachine JDK 25
- **OData V4** サービス + **Fiori elements** 画面（アノテーション駆動）
- **SAP HANA Cloud**（HDI コンテナ）
- **XSUAA**（認証）+ **Application Router**
- デプロイ形態：**MTA**（Multi-Target Application）
- CI/CD：**SAP Continuous Integration & Delivery** サービス（GitHub 連携）

## データモデル（`db/schema.cds`）
| エンティティ | 役割 |
|---|---|
| `Polls` | 1回の投票（タイトル・締切・クローズ状態） |
| `Options` | 選択肢（店名/メニュー・得票数） |
| `Votes` | 1票（投票者・対象選択肢） |

## 業務ロジック（`VoteServiceHandler.java`）
- `Options.vote()` … 締切・重複投票をチェックして1票を記録、得票数を +1
- `Polls.close()` … 投票を締め切る
- Polls 読み取り時に総投票数 `totalVotes` を集計

## ローカル実行
```bash
# 前提: JDK 25 / Maven 3.9.14+ / @sap/cds-dk
mvn spring-boot:run -f srv/pom.xml
# → http://localhost:8080  (OData: /odata/v4/vote/Polls)
```

投票の例:
```bash
curl -X POST "http://localhost:8080/odata/v4/vote/Options(aaaaaaaa-0000-0000-0000-000000000001)/VoteService.vote"
```

## BTP へのデプロイ（手動）
```bash
cf login    # BTP の CF エンドポイントへ
mbt build   # → mta_archives/lunch-vote_*.mtar を生成
cf deploy mta_archives/lunch-vote_1.0.0-SNAPSHOT.mtar
```

## 自動デプロイ
`main` への push を SAP CI/CD サービスが検知し、`mta.yaml` を元にビルド → CF へデプロイします。
（設定手順は BTP コックピットの Continuous Integration & Delivery を参照）
