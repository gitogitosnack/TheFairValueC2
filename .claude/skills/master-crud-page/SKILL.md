---
name: master-crud-page
description: TheFairValue のマスタ一覧＋CRUD 画面を新規追加・修正するときに使う。country / currency / industry / valuation-model と同じ「Controller + RestController + Service + Impl + ResponseDto + Form + Doma DAO + Entity + SQL + Thymeleaf + jQuery」構成を規約どおりに揃える。「〇〇マスタの画面を追加」「一覧ページを作って」「CRUD を実装して」「新規登録モーダルを付けて」などで発動する。
---

# マスタ一覧＋CRUD 画面の追加

## 基準実装

`country` 機能が完成形のリファレンス。迷ったら必ず以下を読んでから書く。

- `src/main/java/org/example/web/stock/country/` 配下すべて
- `src/main/java/org/example/web/dao/CountryDao.java`
- `src/main/java/org/example/web/entity/CountryEntity.java`
- `src/main/resources/META-INF/org/example/web/dao/CountryDao/selectAll.sql`
- `src/main/resources/templates/country-list/country-list.html`
- `src/main/resources/static/js/country.js`

設計背景は `docs/master-list-page-architecture.md` にある。

## 最初に決めること

作業前にこの 4 つを確定させる。ユーザーの指示に無ければ推測せず確認する。

1. **機能名（単数 PascalCase）** — 例 `Sector` → 以下 `{{Feature}}`
2. **パッケージ / テンプレート名（単数 lowerCamel・kebab）** — 例 `sector` → 以下 `{{feature}}`
3. **URL 複数形** — 例 `sectors` → 以下 `{{features}}`
   - 画面 URL は kebab-case（`/valuation-models`）、REST URL は `rest_` ＋ snake_case（`/rest_valuation_models`）。
     単語が 2 語以上になる機能ではこの 2 つが別表記になるので、両方書き出しておく。
4. **テーブル名と列** — 例 `sectors(id, code, name)` → 以下 `{{table}}`

既存テーブルを使うのか新規作成が必要なのかも確認する。新規テーブルが必要なら
`CREATE TABLE` 文を提示し、実行はユーザーに任せる（このリポジトリにマイグレーションの仕組みはない）。

## 作成ファイル一覧（11 個）

| # | パス | 内容 |
|---|---|---|
| 1 | `src/main/java/org/example/web/entity/{{Feature}}Entity.java` | Doma Entity |
| 2 | `src/main/java/org/example/web/dao/{{Feature}}Dao.java` | Doma DAO |
| 3 | `src/main/resources/META-INF/org/example/web/dao/{{Feature}}Dao/selectAll.sql` | 全件取得 SQL |
| 4 | `src/main/resources/META-INF/org/example/web/dao/{{Feature}}Dao/selectById.sql` | ID 取得 SQL |
| 5 | `.../stock/{{feature}}/domain/{{Feature}}ResponseDto.java` | 画面表示用 DTO |
| 6 | `.../stock/{{feature}}/domain/{{Feature}}Form.java` | 入力用 record |
| 7 | `.../stock/{{feature}}/service/{{Feature}}Service.java` | インタフェース |
| 8 | `.../stock/{{feature}}/service/{{Feature}}ServiceImpl.java` | 実装 |
| 9 | `.../stock/{{feature}}/controller/{{Feature}}Controller.java` | 画面 GET |
| 10 | `.../stock/{{feature}}/controller/{{Feature}}RestController.java` | CRUD REST |
| 11 | `src/main/resources/templates/{{feature}}-list/{{feature}}-list.html` ＋ `static/js/{{feature}}.js` | 画面 |

（`.../` は `src/main/java/org/example/web/` の省略）

## 手順

1. **リファレンスを読む** — `country` の同種ファイルを開き、対象機能の列構成と差分を把握する。
2. **Entity → DAO → SQL → DTO/Form → Service → Controller → 画面** の順に作る。
   Java 側のコードは `references/java-templates.md` のテンプレートを使う。
3. **画面** は `references/frontend-templates.md` のテンプレートを使い、
   既存の `country-list.html` からレイアウト・CSS クラス名をコピーして見た目を揃える。
4. **ビルド確認** — `mvn clean compile`。
   `[DOMA4019]` が出たら SQL ファイルのパスとメソッド名の一致を疑う（`doma-dao` スキル参照）。
5. **動作確認** — `run-app` スキルに従い起動し `/{{features}}` を開いて一覧・登録・更新・削除を通す。

## 必ず守る規約

- 画面 Controller は `@RequestMapping("/{{features}}")`、REST は `@RequestMapping("/rest_{{features}}")`。
- モデル属性名は `items` 固定（テンプレートの `th:each` が依存している）。
- Service メソッド名は `initialDispAll` / `insert{{Feature}}Info` / `update{{Feature}}Info` / `delete{{Feature}}InfoById`。
- DI はコンストラクタインジェクション。`@Autowired` は書かない。
- `ServiceImpl` には `@Service` と `@Transactional`（`org.springframework.transaction.annotation`）。
- 更新・削除は `selectById` してから `orElseThrow(() -> new NotFoundException("..."))`。存在チェックを飛ばさない。
- Entity を Controller や画面に渡さない。必ず ResponseDto に変換する。

## やらないこと

- 既存の `country` / `currency` / `industry` のコードを「ついでにリファクタ」しない。
- Entity に業務ロジックを持たせない。
- 検索 / Excel 出力を勝手に実装しない（未実装が既知の状態。依頼があったときだけ）。
