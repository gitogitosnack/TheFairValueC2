---
name: run-app
description: TheFairValue アプリをローカルで起動して画面の動作を確認するときに使う。ビルド、Spring Boot 起動、DB 接続確認、主要 URL、起動失敗時の切り分けを扱う。「アプリを起動」「動かして確認して」「画面を見たい」「起動できない」「ポートが使われている」などで発動する。
---

# アプリの起動と動作確認

## 前提の確認

起動前に PostgreSQL が動いていることを確認する。落ちていると Doma の DataSource 初期化で失敗する。

```powershell
Get-Service -Name "postgresql*" | Select-Object Name, Status
Test-NetConnection -ComputerName localhost -Port 5432 -InformationLevel Quiet
```

接続先は `src/main/resources/application.properties`：
`jdbc:postgresql://localhost:5432/db4thefairvalueC2`（ユーザー `postgres`）。

## 起動

```powershell
mvn clean compile        # 先にビルドを通す（Doma の APT エラーを早く見つける）
mvn spring-boot:run
```

`spring-boot-devtools` が入っているので、起動中に `mvn compile` を別途走らせるか
IDE がクラスを再コンパイルすれば自動再起動する。テンプレート / CSS / JS の変更はブラウザ再読み込みだけで反映される。

起動は**バックグラウンドで実行し、ログでポート待ち受けを確認してから**ブラウザ確認に進む。
`Tomcat started on port 8080` と `Started TheFairValue in ...` の 2 行が出れば起動成功。

## 主要 URL

| 画面 | URL |
|---|---|
| 銘柄一覧 | http://localhost:8080/top |
| 銘柄詳細 | http://localhost:8080/stock-detail/{証券コード} （例 `/stock-detail/7203`） |
| 国マスタ | http://localhost:8080/countries |
| 通貨マスタ | http://localhost:8080/currencies |
| 業種マスタ | http://localhost:8080/industries |
| 評価モデル | http://localhost:8080/valuation-models |

URL の正確な形は各 Controller の `@RequestMapping` / `@GetMapping` を読んで確認する
（マスタ系は `@GetMapping("")` だが `stock-detail` だけ `@GetMapping("/{code}")` でパス変数を取る）。

## 動作確認のやり方

1. 対象画面を開き、一覧にデータが出るか確認する。
2. CRUD を変更した場合は 登録 → 一覧反映 → 編集 → 削除 まで一通り通す。
3. 評価ロジックを変更した場合は銘柄詳細でパラメータを入れ、理論株価が期待値になるか見る。
4. サーバ側の SQL は `logging.level.org.seasar.doma.jdbc.UtilLoggingJdbcLogger=DEBUG` で
   コンソールに出るので、想定した SQL とバインド値になっているか確認する。
5. フロントのエラーはブラウザの DevTools コンソールを見る（jQuery Ajax の失敗は
   `console.error` に出るよう書かれている）。

ユーザーの目視確認が必要な場合は、開く URL と確認してほしい操作手順を具体的に伝える。

## 停止

起動をバックグラウンドで走らせている場合はそのタスクを停止する。手動で残った場合：

```powershell
Get-NetTCPConnection -LocalPort 8080 -State Listen | Select-Object OwningProcess
Stop-Process -Id <OwningProcess> -Confirm:$false
```

## 起動失敗の切り分け

| 症状 | 原因 / 対処 |
|---|---|
| `Web server failed to start. Port 8080 was already in use.` | 前回のプロセスが残っている。上記手順で停止するか `--server.port=8081` で起動 |
| `Connection to localhost:5432 refused` | PostgreSQL が起動していない |
| `password authentication failed for user "postgres"` | `application.properties` の資格情報とローカル DB の設定が不一致 |
| `[DOMA4019] ... .sql is not found` | SQL ファイルの配置ミス。`doma-dao` スキルと `docs/doma4019-vscode.md` を参照 |
| `NoSuchBeanDefinitionException` で DAO が見つからない | DAO に `@ConfigAutowireable` が付いていない |
| 画面が 404 | Controller の `@RequestMapping` と `setViewName` のテンプレートパスを確認 |
| 一覧が空 | DB にデータが無い / `mav.addObject` のキーが `items` になっていない |
| `hs_err_pid*.log` が増える | JVM クラッシュ。ログ先頭のスレッド情報を読み、再現条件を切り分ける |

## やらないこと

- `application.properties` の接続情報を勝手に書き換えない（ユーザーのローカル環境設定）。
- 確認のために DB のデータを削除・更新しない。参照だけで確認できる方法を優先する。
- 起動したまま放置しない。確認が終わったら停止する。
