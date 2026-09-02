# 銘柄データ定期取得バッチ モジュール（メソッド）単位 IO 一覧

`docs/batch-market-data-sync-design.md`（設計ドラフト）に対応する実装コード
`src/main/java/org/example/web/batch/marketdata/` 配下と `org.example.MarketDataBatchApplication` を対象に、
モジュール（クラス）ごと・メソッド単位で入出力（Input/Output）を棚卸しした一覧。**2026-09-03 時点のソースコードを直接読んで作成**しており、
設計ドラフトの「想定」ではなく実装済みコードの実態を反映する。

## 凡例

| ステータス | 意味 |
| --- | --- |
| ✅ 実装済み | クラス定義・メソッド本体が実装済み（コンパイル可能） |
| 🚧 未着手 | ファイルは存在するが `package` 宣言と説明コメントのみで、クラス/インタフェース定義自体が存在しない（設計メモの状態）。IO 欄は `batch-market-data-sync-design.md` の該当節から拾った**想定**であり、確定ではない |

対応する Java ユニットテストが存在するクラスには備考欄に記載する。

---

## 1. `client` パッケージ — 外部 API クライアント（✅ 実装済み）

`src/main/java/org/example/web/batch/marketdata/client/`

### 1.1 DTO（入出力の形）

| ファイル | 型 | フィールド（＝IOの形） |
| --- | --- | --- |
| `client/dto/StockQuoteData.java` | record | `date, openPrice, highPrice, lowPrice, closePrice, volume, marketCap, sharesOutstanding` — `StockPriceProvider.fetchLatestQuote()` の戻り値 |
| `client/dto/FinancialStatementData.java` | record | `fiscalYear, fiscalQuarter, endDate, revenue, operatingIncome, netIncome, eps, bps, operatingCashFlow, investingCashFlow, freeCashFlow, totalAssets, totalDebt, cashAndEquivalents, grossProfit, sgAndA, ebit, totalEquity, inventory, accountsReceivable, financingCf, interestExpense, dividendsPaid`（`company_id` は持たない） — `FinancialDataProvider.fetchLatestStatement()` 等の戻り値 |
| `client/dto/EdinetDocumentListItem.java` | record | `docId, secCode, docTypeCode, periodEnd` — `EdinetFinancialDataProviderImpl.fetchDocumentList()` の戻り値要素 |
| `client/dto/EdinetMatchedDocument.java` | record | `companyId, docId, docTypeCode, periodEnd` — `secCode` 突合後の一致結果（Tasklet→chunk Step 間の受け渡し用、未着手の `EdinetDocumentListTasklet` が生成する想定） |

### 1.2 `StockPriceProvider.java`（interface）

| メソッド | 入力 | 出力 |
| --- | --- | --- |
| `StockQuoteData fetchLatestQuote(String code)` | `code`（銘柄コード） | `StockQuoteData`（取得不可時は `null`）。429時は `RateLimitException` を throw |

### 1.3 `FmpStockPriceProviderImpl.java`（`StockPriceProvider` 米国株実装）

| メソッド | 入力 | 外部 I/O | 出力 |
| --- | --- | --- | --- |
| `fetchLatestQuote(String code)` | `code`（例 `AAPL`） | `GET {fmp.baseUrl}/quote/{symbol}?apikey=...`（FMP） | `StockQuoteData`。レスポンス0件/404は `null`。429は `RateLimitException` |

### 1.4 `YahooFinanceStockPriceProviderImpl.java`（`StockPriceProvider` 日本株実装）

| メソッド | 入力 | 外部 I/O | 出力 |
| --- | --- | --- | --- |
| `fetchLatestQuote(String code)` | `code`（4桁証券コード、`.T`付与はPython側） | `GET {yfinanceServiceBaseUrl}/quotes/{code}`（yfinance-service） | `StockQuoteData`。404は `null`。429は `RateLimitException` |
| `isHealthy()` | なし | `GET {yfinanceServiceBaseUrl}/health` | `boolean`（例外時は `false`） |

### 1.5 `FinancialDataProvider.java`（interface）

| メソッド | 入力 | 出力 |
| --- | --- | --- |
| `FinancialStatementData fetchLatestStatement(String code)` | `code` | `FinancialStatementData`（取得不可時は `null`）。429時は `RateLimitException` |

### 1.6 `FmpFinancialDataProviderImpl.java`（`FinancialDataProvider` 米国株実装）

| メソッド | 入力 | 外部 I/O | 出力 |
| --- | --- | --- | --- |
| `fetchLatestStatement(String code)` | `code` | `GET /income-statement/{symbol}`, `GET /balance-sheet-statement/{symbol}`, `GET /cash-flow-statement/{symbol}`（いずれも `period=quarter&limit=1&apikey=...`、FMP。計最大3リクエスト） | `FinancialStatementData`。income未取得なら `null`。429は `RateLimitException`、404は `null` |
| `private <T> T fetchFirst(String path, String code, Class<T[]> arrayType)` | `path`, `code`, 配列型 | 上記いずれか1本のGET | 配列先頭要素 or `null`（内部ヘルパー） |

### 1.7 `EdinetFinancialDataProviderImpl.java`（`FinancialDataProvider` 日本株実装）

EDINETは銘柄コード単発取得APIを持たないため、インタフェースの `fetchLatestStatement` は未使用（例外throw）。実際は2段構成の独自メソッドで対応する。

| メソッド | 入力 | 外部 I/O | 出力 |
| --- | --- | --- | --- |
| `fetchLatestStatement(String code)` | `code` | なし | 常に `UnsupportedOperationException` を throw（未実装ではなく意図的な非対応） |
| `fetchDocumentList(LocalDate date)` | `date`（対象日） | `GET {edinet.baseUrl}/documents.json?date=...&type=2&Subscription-Key=...`（EDINET、その日1回のみ想定） | `List<EdinetDocumentListItem>`（`docTypeCode` が有報`120`/半報`140,160`のみに絞込済み）。429は `RateLimitException` |
| `fetchStatementDocument(String docId, String docTypeCode, LocalDate periodEnd)` | `docId, docTypeCode, periodEnd`（`fetchDocumentList` の結果由来） | `GET {edinet.baseUrl}/documents/{docId}?type=5&Subscription-Key=...`（ZIP形式バイナリ） | `FinancialStatementData`（ZIP空/取得不可時は `null`）。429は `RateLimitException` |
| `private Map<String,String> extractElementValues(byte[] zipBytes)` | ZIPバイト列 | ZIP展開（CSVエントリのみ） | 要素ID→値のMap（内部ヘルパー） |
| `private void parseCsvBytes(byte[] csvBytes, Map values, Map priorities)` | CSVバイト列（UTF-16LE） | なし | `values`/`priorities` Mapを更新（当期・連結を優先する仕分けロジック、内部ヘルパー） |
| `private FinancialStatementData toFinancialStatementData(Map v, String docTypeCode, LocalDate periodEnd)` | 要素値Map, `docTypeCode`, `periodEnd` | なし | `FinancialStatementData`（タクソノミ要素名→フィールドへのマッピング、内部ヘルパー） |
| `private static BigDecimal decimal(Map v, String... elementIdCandidates)` | 要素値Map, 候補キー列 | なし | `BigDecimal` or `null`（内部ヘルパー） |

備考: `src/test/java/org/example/web/batch/marketdata/client/EdinetFinancialDataProviderImplTest.java` が存在。

### 1.8 `RateLimitException.java`

| メソッド | 入力 | 出力 |
| --- | --- | --- |
| `RateLimitException(String message)` / `RateLimitException(String message, Throwable cause)` | メッセージ（＋原因例外） | 例外インスタンス。各Providerが429検知時にthrowし、Step側の `faultTolerant().retry()`（未着手）が捕捉する想定 |

備考: `RateLimitExceptionTest.java` あり。`FmpStockPriceProviderImplTest.java` / `FmpFinancialDataProviderImplTest.java` / `YahooFinanceStockPriceProviderImplTest.java` も存在。

---

## 2. `config` パッケージ — 設定・Bean 定義

`src/main/java/org/example/web/batch/marketdata/config/`

### 2.1 `MarketDataApiProperties.java`（✅ 実装済み）

`@ConfigurationProperties(prefix = "marketdata.api")`。`application.properties` の以下の行経由で環境変数から注入される。

```properties
marketdata.api.edinet.api-key=${EDINET_API_KEY:}
marketdata.api.edinet.base-url=${EDINET_BASE_URL:https://api.edinet-fsa.go.jp/api/v2}
marketdata.api.fmp.api-key=${FMP_API_KEY:}
marketdata.api.fmp.base-url=${FMP_BASE_URL:https://financialmodelingprep.com/api/v3}
```

| メソッド | 入力 | 出力 |
| --- | --- | --- |
| `getEdinet()` / `getFmp()` | なし | `Edinet` / `Fmp`（内部static class）インスタンス |
| `Edinet#getApiKey()/getBaseUrl()`, `Fmp#getApiKey()/getBaseUrl()` | なし | `String`（既定値: EDINET baseUrl は `https://api.edinet-fsa.go.jp/api/v2`、FMP baseUrl は `https://financialmodelingprep.com/api/v3`、apiKeyは既定 `""`） |
| 各 `set*()` | `String` | Spring Boot のプロパティバインディングから呼ばれる（アプリコードから直接呼ぶことは想定しない） |

呼び出し元: `EdinetFinancialDataProviderImpl`, `FmpFinancialDataProviderImpl`, `FmpStockPriceProviderImpl`（コンストラクタでbaseUrl/apiKeyを読む）。

### 2.2 `YfinanceServiceProperties.java`（✅ 実装済み）

`@ConfigurationProperties(prefix = "marketdata.yfinance-service")`。`marketdata.yfinance-service.base-url=${YFINANCE_SERVICE_BASE_URL:http://localhost:8081}`。

| メソッド | 入力 | 出力 |
| --- | --- | --- |
| `getBaseUrl()` | なし | `String`（既定 `http://localhost:8081`） |
| `setBaseUrl(String)` | `String` | プロパティバインディング用 |

呼び出し元: `YahooFinanceStockPriceProviderImpl`。

### 2.3 `RestClientConfig.java`（✅ 実装済み）

| メソッド | 入力 | 出力 |
| --- | --- | --- |
| `@Bean marketDataRestClientBuilder()` | なし（`ClientHttpRequestFactorySettings.DEFAULTS` に connectTimeout=5秒・readTimeout=20秒をハードコードして適用） | `RestClient.Builder` Bean。各 Provider 実装がコンストラクタで `clone().baseUrl(...)` して利用する |

備考: 429時の再試行はここでは行わず、Step 側（`faultTolerant().retry()`、未着手）に委ねる設計（コメント記載）。

### 2.4 `MarketDataScheduleProperties.java`（🚧 未着手）

ファイルは `package` 宣言とコメントのみで、クラス定義が存在しない。

- 想定IO（設計書6章・ファイルコメントより）: `marketdata.schedule.*` 系プロパティ（`MARKETDATA_SCHEDULER_ENABLED`, `MARKETDATA_QUOTE_CRON_JP`, `MARKETDATA_QUOTE_CRON_US`, `MARKETDATA_FINANCIAL_CRON` 等の環境変数）を `@ConfigurationProperties` で束ね、`MarketDataSyncScheduler`（同じく未着手）が読む想定。

---

## 3. `controller` パッケージ（🚧 未着手）

`src/main/java/org/example/web/batch/marketdata/controller/MarketDataSyncRestController.java`

ファイルは `package` 宣言とコメントのみ。クラス定義なし。

- 想定IO（設計書 3.4・4.4）:

| エンドポイント（想定） | 入力 | 出力 |
| --- | --- | --- |
| `POST /rest_market_data_sync/quote` | HTTPリクエスト（ボディなし想定） | `BatchExecutionLockService.tryLock()` 経由で `JobLauncher.run(dailyQuoteSyncJob, jobParameters{triggerType=MANUAL})` を呼び出し、実行結果（成功/失敗件数）をレスポンスとして返す。ロック失敗時はHTTP 409相当 |
| `POST /rest_market_data_sync/financial-statement` | 同上 | `JobLauncher.run(financialStatementSyncJob, ...)` を呼び出す。同上のロック制御・レスポンス |

---

## 4. `job` パッケージ（🚧 未着手）

`src/main/java/org/example/web/batch/marketdata/job/`

いずれもファイルは `package` 宣言とコメントのみ。

### 4.1 `DailyQuoteSyncJobConfig.java`

- 想定IO（設計書4.2）: `dailyQuoteSyncJob` / `dailyQuoteSyncStep`（chunk指向、単一Step）のBean定義。入力＝`CompanyItemReader` → `DailyQuoteItemProcessor` → `DailyQuoteItemWriter` の連結。`RateLimitException` に対する `.faultTolerant().retry()` 設定を持つ。出力＝Spring Batchの `Job`/`Step` Bean。

### 4.2 `FinancialStatementSyncJobConfig.java`

- 想定IO（設計書4.3）: `financialStatementSyncJob` のBean定義。`usFinancialStatementStep` → `edinetDocumentListStep` → `jpFinancialStatementStep` を単一Job内で直列実行する構成。出力＝`Job` Bean。

---

## 5. `tasklet` パッケージ（🚧 未着手）

`src/main/java/org/example/web/batch/marketdata/tasklet/EdinetDocumentListTasklet.java`

- 想定IO（設計書4.3.2）:

| 項目 | 内容 |
| --- | --- |
| 入力 | 実行日（当日日付） |
| 外部I/O | `EdinetFinancialDataProviderImpl.fetchDocumentList(date)` を1回呼び出し（内部でEDINET `documents.json` 取得）。`companies`（`country=JP`）をDBから取得しての突合 |
| 出力 | `docTypeCode` 絞込＋`secCode`（末尾0除去）と `companies.code` の突合済み `(companyId, docID)` リストを `EdinetMatchedDocument` として `ExecutionContextPromotionListener` 経由で Job の `ExecutionContext` へ格納 |

---

## 6. `reader` パッケージ（🚧 未着手）

`src/main/java/org/example/web/batch/marketdata/reader/`

| ファイル | 想定型 | 入力 | 出力 |
| --- | --- | --- | --- |
| `CompanyItemReader.java` | `ItemReader<CompanyEntity>` | `companies`テーブル（`delete_flg = 0`） | `CompanyEntity` を1件ずつ返す。`dailyQuoteSyncStep` 用（設計書4.2） |
| `UsCompanyItemReader.java` | `ItemReader<CompanyEntity>` | `companies`テーブル（米国株のみに絞込） | `CompanyEntity` を1件ずつ返す。`usFinancialStatementStep` 用（設計書4.3.1） |
| `EdinetMatchedDocumentItemReader.java` | `ItemReader<EdinetMatchedDocument>` | `edinetDocumentListStep` が格納した `ExecutionContext` 内のリスト | `EdinetMatchedDocument` を1件ずつ返す。`jpFinancialStatementStep` 用（設計書4.3.2） |

---

## 7. `processor` パッケージ（🚧 未着手）

`src/main/java/org/example/web/batch/marketdata/processor/`

| ファイル | 想定型 | 入力 | 外部I/O | 出力 |
| --- | --- | --- | --- | --- |
| `DailyQuoteItemProcessor.java` | `ItemProcessor<CompanyEntity, DailyQuoteEntity>` | `CompanyEntity`（Readerから） | `StockPriceProvider.fetchLatestQuote(code)`（国コードに応じFMP/yfinance-serviceへ振分） | `DailyQuoteEntity`。取得失敗はログ記録＋`null`（chunkから除外）。429は `RateLimitException` throw（設計書4.2） |
| `UsFinancialStatementItemProcessor.java` | `ItemProcessor<CompanyEntity, FinancialStatementEntity>` | `CompanyEntity`（Readerから） | `financial_statements`から最新`(fiscal_year, fiscal_quarter)`取得＋`FmpFinancialDataProviderImpl.fetchLatestStatement(code)` | API側が新しければ `FinancialStatementEntity`、差分なしは `null`（除外）（設計書4.3.1） |
| `JpFinancialStatementItemProcessor.java` | `ItemProcessor<EdinetMatchedDocument, FinancialStatementEntity>` | `EdinetMatchedDocument`（Readerから） | `EdinetFinancialDataProviderImpl.fetchStatementDocument(docId, docTypeCode, periodEnd)` | `FinancialStatementEntity`（勘定科目タクソノミをパースして組立、設計書4.3.2） |

---

## 8. `writer` パッケージ（🚧 未着手）

`src/main/java/org/example/web/batch/marketdata/writer/`

| ファイル | 想定型 | 入力 | 外部I/O | 出力 |
| --- | --- | --- | --- | --- |
| `DailyQuoteItemWriter.java` | `ItemWriter<DailyQuoteEntity>` | chunk内の `DailyQuoteEntity` リスト | `daily_quotes` へ `(company_id, date)` キーでUPSERT | なし（DB書き込みのみ、設計書4.2） |
| `FinancialStatementItemWriter.java` | `ItemWriter<FinancialStatementEntity>` | chunk内の `FinancialStatementEntity` リスト | `financial_statements` へINSERT。INSERTした各行について `AnalysisIndicatorRecalcService.recalc(companyId)` を呼出し | なし（DB書き込み＋指標再計算のトリガ。`usFinancialStatementStep`/`jpFinancialStatementStep`で共用、設計書4.3.1〜4.3.3） |

---

## 9. `service` パッケージ（🚧 未着手）

`src/main/java/org/example/web/batch/marketdata/service/`

| ファイル | 想定メソッド | 入力 | 外部I/O | 出力 |
| --- | --- | --- | --- | --- |
| `AnalysisIndicatorRecalcService.java`（interface）/ `AnalysisIndicatorRecalcServiceImpl.java` | `recalc(companyId)` 相当 | `companyId`（`FinancialStatementItemWriter`から） | 評価モデル機能側（`web.stock.valuationmodel`等）のROE/PER算出ロジックを呼出し | 算出結果を `analysis_indicators` へINSERT。算出式自体は実装しないオーケストレーション役（設計書4.3.3） |
| `BatchExecutionLockService.java`（interface）/ `BatchExecutionLockServiceImpl.java` | `tryLock(batchType)` / `unlock(batchType)` 相当 | `batchType`（日次株価=lockKey 1、財務諸表=lockKey 2） | PostgreSQL `pg_try_advisory_lock(lockKey)` / `pg_advisory_unlock(lockKey)` | `tryLock`は`boolean`（取得可否）、`unlock`は戻り値なし。`Runner`/`Scheduler`/`RestController`が起動前後に呼ぶ（設計書3.4） |

---

## 10. `runner` / `scheduler` パッケージ（🚧 未着手）

| ファイル | 想定メソッド | 入力 | 外部I/O | 出力 |
| --- | --- | --- | --- | --- |
| `runner/MarketDataBatchRunner.java` | `CommandLineRunner#run(String... args)` | `--job=dailyQuote` / `--job=financialStatement` 起動引数 | `BatchExecutionLockService.tryLock()` → `JobLauncher.run(job, jobParameters{triggerType=SCHEDULED_EXTERNAL})` | 処理後 `unlock()`。将来 `org.example.MarketDataBatchApplication` から呼ばれプロセス終了（設計書3.1・3.2） |
| `scheduler/MarketDataSyncScheduler.java` | `@Scheduled` メソッド（cronは`MarketDataScheduleProperties`から） | なし（スケジューラトリガ） | `BatchExecutionLockService.tryLock()` → `JobLauncher.run(job, jobParameters{triggerType=SCHEDULED_INTERNAL})` | 処理後 `unlock()`。`MARKETDATA_SCHEDULER_ENABLED=true` の時のみBean登録（`@ConditionalOnProperty`、設計書3.1・3.4） |

---

## 11. `org.example.MarketDataBatchApplication`（🚧 未着手）

`src/main/java/org/example/MarketDataBatchApplication.java`

ファイルは `package org.example;` とコメントのみ。クラス定義なし（`web.batch`配下ではなく`org.example`直下、既存の`TheFairValue`と同格に置く設計）。

- 想定IO（設計書3.1）:

| メソッド | 入力 | 出力 |
| --- | --- | --- |
| `main(String[] args)` | コマンドライン引数（`--job=...`は`MarketDataBatchRunner`側が解釈） | `SpringApplicationBuilder(...).web(WebApplicationType.NONE).run(args)` でTomcat非起動のコンテキストを生成し、Job完了後 `System.exit(SpringApplication.exit(ctx))` でプロセスを終了 |

---

## 12. 実装状況サマリ

| パッケージ | 状態 |
| --- | --- |
| `client`（Provider実装＋DTO＋例外） | ✅ 実装済み（対応するユニットテストも一部あり） |
| `config` | ✅ 実装済み（`MarketDataApiProperties`, `YfinanceServiceProperties`, `RestClientConfig`）。`MarketDataScheduleProperties` のみ 🚧 未着手 |
| `controller`, `job`, `tasklet`, `reader`, `processor`, `writer`, `service`, `runner`, `scheduler` | 🚧 未着手（全ファイルが `package`＋コメントのみ） |
| `org.example.MarketDataBatchApplication` | 🚧 未着手 |
| `pom.xml` の `spring-boot-starter-batch` 追加、Spring Batchメタデータテーブル | 未確認（本一覧の対象外。着手時に別途確認要） |

このサマリは `memory/thefairvalue_batch_client_impl_2026-08.md` の記録（client package実装済み、job/step/reader/writer/tasklet/Spring Batch依存はスタブ）と一致する。
