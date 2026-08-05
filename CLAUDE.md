# TheFairValue — 適正株価評価アプリ

企業の財務データから DCF などの評価モデルで「適正株価（理論株価）」を算出し、
現在株価と比較して割安/割高を判定する Spring Boot Web アプリ。

## 技術スタック

| 領域 | 使用技術 |
|---|---|
| 言語 / ビルド | Java 21 / Maven（ラッパーなし、`mvn` を直接使う） |
| フレームワーク | Spring Boot 3.2.4（web, thymeleaf, devtools） |
| DB アクセス | Doma 2.53.1（`doma-spring-boot-starter` 1.7.0）＋ アノテーションプロセッサ |
| DB | PostgreSQL `localhost:5432/db4thefairvalueC2` |
| View | Thymeleaf + jQuery（Ajax）＋ 素の CSS |
| JS テスト | Vitest（`src/test/javascript/**/*.test.js`） |
| フォーマッタ | Prettier（`prettier-plugin-tailwindcss`, `prettier-plugin-classnames`） |
| エントリポイント | `org.example.TheFairValue` |

## よく使うコマンド

```powershell
mvn clean compile        # ビルド（Doma の APT が走り SQL ファイルを検証する）
mvn spring-boot:run      # 起動 → http://localhost:8080/top
npx vitest run           # JS テストを 1 回実行
npx vitest               # JS テストを watch 実行
npx prettier --write "src/main/resources/static/js/**/*.js"
```

Java のユニットテストは現状存在しない（`src/test/java` なし）。
サーバ側の変更は `mvn clean compile` と実際の画面での動作確認で検証する。

## ディレクトリ構成と責務

```
src/main/java/org/example/web/
├── dao/                       ... Doma @Dao。★機能別ではなくここに集約
├── entity/                    ... Doma @Entity。★テーブルと 1:1、ここに集約
├── exception/NotFoundException.java
└── stock/
    ├── common/service/CIMapper.java   ... code → id 変換の共通処理
    └── <feature>/                     ... 機能単位（country, currency, industry,
        ├── controller/                    stockList, stockDetail, valuationmodel）
        ├── domain/                    ... Form（入力）/ ResponseDto（出力）
        └── service/                   ... インタフェース ＋ Impl

src/main/resources/
├── META-INF/org/example/web/dao/<DaoName>/<method>.sql   ... @Select 用 SQL
├── templates/<feature>-list/<feature>-list.html
├── templates/stock-detail/, templates/modals/
└── static/js/, static/css/
```

**重要な構造上のルール**

- `dao` と `entity` は機能パッケージの外（`web.dao` / `web.entity`）に置く。機能パッケージ側には作らない。
- `domain` パッケージには Form（`record`）と ResponseDto（`final` フィールド＋getter のクラス）を置く。Entity を画面まで漏らさない。
- Service は必ずインタフェース＋`Impl` の 2 ファイル。DI はコンストラクタインジェクション（`@Autowired` は書かない）。
  - 例外は `StockDetailServiceImpl` でフィールド `@Autowired` を使っている箇所。新規コードでは真似しない。

## 命名規約（`country` 機能が基準実装）

| 種別 | 規約 | 例 |
|---|---|---|
| 画面 Controller | `<Feature>Controller`, `@RequestMapping("/<複数形>")`, `@GetMapping("")` で `ModelAndView` | `CountryController` → `/countries` |
| REST Controller | `<Feature>RestController`, `@RequestMapping("/rest_<複数形>")` | `CountryRestController` → `/rest_countries` |
| REST エンドポイント | `POST /insert`, `POST /update`, `DELETE /delete/{id}` | |

画面 URL は kebab-case、REST URL は `rest_` ＋ snake_case（例 `/valuation-models` と `/rest_valuation_models`）。
銘柄一覧だけ例外で `/top` ＋ `/rest_stock_list`、銘柄詳細は `/stock-detail/{code}`（パス変数）。
| Service メソッド | `initialDispAll()`, `insert<Feature>Info()`, `update<Feature>Info()`, `delete<Feature>InfoById()` | |
| モデル属性 | 一覧データは必ず `items` | `mav.addObject("items", ...)` |
| テンプレート | `templates/<feature>-list/<feature>-list.html` | |
| JS | `static/js/<feature>.js` | |
| Entity | `<Feature>Entity` + `@Entity(immutable = false)` + `@Table(name = "<snake_case 複数形>")` | `CountryEntity` → `countries` |

## Doma の注意点

- `@Select` メソッドは対応する SQL ファイルが必須。パスは
  `src/main/resources/META-INF/<DAOの完全修飾名をディレクトリ化>/<メソッド名>.sql`。
  カラム列挙は `/*%expand*/*` を使う。
- `@Insert` / `@Update` / `@Delete` は SQL ファイル不要。
- VS Code で `[DOMA4019] ... .sql is not found in the classpath` が出た場合は
  `docs/doma4019-vscode.md` の手順に従う（原因は APT とリソースコピー順）。
- DAO には `@Dao` と `@ConfigAutowireable` の両方を付ける。

## 適正株価の算出

- 計算ロジックはフロント側 `static/js/pv_calculator.js` の `pv_calculator` クラスにある（DCF ほか）。
  ここを変更したら必ず `src/test/javascript/pv_calculator.test.js` を更新し `npx vitest run` を通す。
- 評価モデルとパラメータはハードコードせず DB で管理する：
  `valuation_models` / `valuation_parameters` / `company_valuation_models` / `company_valuation_parameter_defaults`。
- サーバ側 `StockDetailServiceImpl.getComprehensiveAnalysis(code)` が
  企業情報・市場指標・モデル既定パラメータ・5 年分の分析指標を集約して `StockAnalysisResponse` を返す。

## 既知の課題（触るときは注意）

- `application.properties` に DB パスワードが平文で入っている。値を新たに追加するときは環境変数化を検討する。
- `stock-list.html` の編集/削除リンクは `data-code` のみで `data-id` を持たないため ID 解決が不整合。
- マスタ一覧の検索 / クリア / Excel 出力ボタンは UI のみでサーバ実装がない。

## `.claude` の構成

```
.claude/
├── settings.json              ... 共有設定（権限など）
├── settings.local.json        ... 個人設定（コミット対象外にしたい設定）
├── commands/verify.md         ... /verify : ビルド＋テスト＋診断
└── skills/
    ├── master-crud-page/      ... マスタ一覧＋CRUD 画面の追加
    ├── doma-dao/              ... DAO / SQL ファイルの追加・修正
    ├── valuation-model/       ... 評価モデル・パラメータの追加
    └── run-app/               ... アプリ起動と動作確認
```
