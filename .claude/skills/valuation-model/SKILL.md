---
name: valuation-model
description: 適正株価（理論株価）の評価モデルを追加・修正するときに使う。DCF・PER 倍率法・配当割引モデル（DDM）・残余利益モデルなどの計算ロジック、pv_calculator.js への実装、Vitest テスト、valuation_models / valuation_parameters テーブルへのモデル登録、パラメータ既定値の扱いを扱う。「評価モデルを追加」「DCF の計算を直す」「理論株価の算出ロジック」「パラメータを増やす」などで発動する。
---

# 評価モデルの追加・修正

## このアプリの評価モデルの構造

計算ロジックとメタデータが分離されている。片方だけ変えると画面が動かないので必ず両方揃える。

| 層 | 場所 | 役割 |
|---|---|---|
| 計算ロジック | `src/main/resources/static/js/pv_calculator.js` の `pv_calculator` クラス | 実際の理論株価計算。ブラウザ上で実行 |
| テスト | `src/test/javascript/pv_calculator.test.js` | Vitest。正常系＋異常系 |
| モデル定義 | `valuation_models` テーブル | `model_name`, `formula_description` |
| パラメータ定義 | `valuation_parameters` テーブル | `valuation_model_id`, `parameter_code`, `parameter_name`, `display_order` |
| 企業ごとの採用モデル | `company_valuation_models` テーブル | どの企業でどのモデルを使うか |
| 企業ごとの既定値 | `company_valuation_parameter_defaults` テーブル | パラメータの初期表示値 |
| サーバ側集約 | `StockDetailServiceImpl.getComprehensiveAnalysis(code)` | 上記を読んで `StockAnalysisResponse` に詰める |
| 画面 | `templates/stock-detail/stock-detail.html` ＋ `static/js/stock-detail.js` | パラメータ入力 → `pv_calculator` 呼び出し → 表示 |

`parameter_code` は JS 側の引数と対応する。**命名を勝手に変えると画面とロジックが繋がらなくなる**。
既存の DCF は `fcf`, `growth_ratio`, `discount_ratio`, `perpetual_growth_ratio` を使っている。

## 計算ロジックの実装規約

`dcf_calc` が唯一の完成例。新しいモデルも同じ形に揃える。

```js
class pv_calculator {
  // <モデル名>を用いて適正現在株価を予測するロジック
  <model>_calc(param1, param2, ..., financial_data) {
    logger.info("<model>_calc method started.");

    // 0. 前提条件が崩れるケースは早期に日本語メッセージで throw
    if (/* 数式が成立しない条件 */) {
      logger.error("...");
      throw new Error("<ユーザーに見せる日本語メッセージ>");
    }

    // 1..n. 計算ステップごとに日本語コメントを付ける

    logger.info("<model>_calc method ended.");
    return fair_price;   // 1 株あたりの適正株価を返す
  }
}
```

必須事項：

- メソッド名は `<snake_case のモデル略称>_calc`。
- 戻り値は必ず **1 株あたりの適正株価（数値）**。株主価値や事業価値を返さない。
- 率は小数で受け取る（5% → `0.05`）。パーセント値を受け取らない。
- ゼロ除算・負の分母・`shares_outstanding <= 0` は必ず `throw new Error()` で弾く。
  メッセージは画面にそのまま出るので日本語で書く。
- `logger` はファイル先頭で定義済みのものを使う（Vitest でも動くダミー付き）。
- ファイル末尾の `global.pv_calculator` エクスポートは消さない。Vitest がこれに依存している。
- 財務データは `financial_data` オブジェクトで受け取る。既存の DCF は
  `non_operating_assets`（各項目の合計を取る）、`interest_bearing_debt`（同）、`shares_outstanding` を参照する。

## 手順

1. **数式を確定する。** どのパラメータをユーザー入力にし、どれを財務データから取るかを先に決める。
   会計上の定義が曖昧な項目（FCF の算出方法など）は推測せずユーザーに確認する。
2. **`pv_calculator.js` にメソッドを追加。** 既存メソッドは変更しない。
3. **`pv_calculator.test.js` にテストを追加。**
   - 正常系：手計算した期待値を `toBeCloseTo(期待値, 2)` で検証。コメントに計算根拠を書く。
   - 異常系：throw する条件をすべて `toThrow("メッセージ")` で検証。
   - `beforeEach` で `new global.pv_calculator()` する既存の書き方に合わせる。
4. **`npx vitest run` を通す。** 失敗したまま先に進まない。
5. **DB にモデルとパラメータを登録する SQL を提示する。**（実行はユーザーに委ねる）

   ```sql
   INSERT INTO valuation_models (model_name, formula_description)
   VALUES ('配当割引モデル', '来期予想配当 ÷ (要求収益率 - 配当成長率)');

   INSERT INTO valuation_parameters (valuation_model_id, parameter_code, parameter_name, display_order)
   VALUES
     (<上で採番された id>, 'expected_dividend', '来期予想配当',  1),
     (<同>,                'required_return',   '要求収益率',    2),
     (<同>,                'dividend_growth',   '配当成長率',    3);
   ```

   企業ごとに使うモデル・既定値も必要なら `company_valuation_models` /
   `company_valuation_parameter_defaults` への INSERT も併せて提示する。
6. **画面側の結線を確認する。** `stock-detail.js` が `parameter_code` からどうやって
   計算メソッドを呼んでいるかを読み、新モデルの分岐が必要なら追加する。
7. **`mvn clean compile` → `mvn spring-boot:run`** で銘柄詳細画面から実際に計算させて確認する
   （`run-app` スキル参照）。

## 既存ロジックを直すとき

- `dcf_calc` は 5 年予測＋ゴードン成長モデルのターミナルバリューという構成。
  予測年数は `const term = 5` にハードコードされている。可変にする依頼なら引数追加＋テスト追加。
- 数値の期待値が変わる修正では、テストの期待値を「動いたから合わせる」ではなく
  **手計算し直した根拠をコメントに残す**。
- `npx prettier --write "src/main/resources/static/js/pv_calculator.js"` で整形して終える。

## やらないこと

- 計算ロジックをサーバ側（Java）に移植しない。現状の設計はフロント計算。
- モデル名やパラメータをコードにハードコードしない。DB 管理が原則。
- テストを書かずに計算ロジックを変更しない。
