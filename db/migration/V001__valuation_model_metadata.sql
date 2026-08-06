-- =====================================================================
-- 銘柄詳細画面「理論株価レンジ比較」「モデル別詳細カード」対応
--
-- 1. valuation_models に画面表示用の解説カラムと、JS の計算メソッドを
--    引き当てるための安定キー model_code を追加する。
-- 2. valuation_parameters にスライダーの単位・範囲・モデル共通既定値を追加する。
--    （単位が無いと「目標PER 23倍」が「23.0%」と表示されてしまうため）
-- 3. モデル 5〜10 のパラメータが「倍率を逆算する入力」になっていて理論株価を
--    算出できないため、「目標倍率／率」を入力する形に再定義する。
--
-- 追加カラムはすべて NULL 許容。既存機能への影響はない。
-- =====================================================================

BEGIN;

-- ---------------------------------------------------------------------
-- 1. valuation_models : model_code ＋ 解説 4 カラム
-- ---------------------------------------------------------------------
ALTER TABLE valuation_models ADD COLUMN IF NOT EXISTS model_code      varchar(30);
ALTER TABLE valuation_models ADD COLUMN IF NOT EXISTS characteristics text;
ALTER TABLE valuation_models ADD COLUMN IF NOT EXISTS use_case        text;
ALTER TABLE valuation_models ADD COLUMN IF NOT EXISTS advantages      text;
ALTER TABLE valuation_models ADD COLUMN IF NOT EXISTS disadvantages   text;

-- ---------------------------------------------------------------------
-- 2. valuation_parameters : 単位・範囲・モデル共通既定値
-- ---------------------------------------------------------------------
-- unit は 'PERCENT'（率／画面は %、JS へは小数で渡す）, 'TIMES'（倍率）,
-- 'YEARS'（年数）のいずれか。
ALTER TABLE valuation_parameters ADD COLUMN IF NOT EXISTS unit          varchar(10);
ALTER TABLE valuation_parameters ADD COLUMN IF NOT EXISTS min_value     numeric(15,4);
ALTER TABLE valuation_parameters ADD COLUMN IF NOT EXISTS max_value     numeric(15,4);
ALTER TABLE valuation_parameters ADD COLUMN IF NOT EXISTS step_value    numeric(10,4);
-- 企業固有の既定値（company_valuation_parameter_defaults）が無いときに使う
-- モデル共通のフォールバック値。
ALTER TABLE valuation_parameters ADD COLUMN IF NOT EXISTS default_value numeric(15,4);

-- ---------------------------------------------------------------------
-- 3. model_code の採番
-- ---------------------------------------------------------------------
UPDATE valuation_models SET model_code = 'DCF'                 WHERE id = 1;
UPDATE valuation_models SET model_code = 'PER_MULTIPLE'        WHERE id = 2;
UPDATE valuation_models SET model_code = 'DDM'                 WHERE id = 3;
UPDATE valuation_models SET model_code = 'EV_EBITDA'           WHERE id = 5;
UPDATE valuation_models SET model_code = 'PBR_MULTIPLE'        WHERE id = 6;
UPDATE valuation_models SET model_code = 'PSR_MULTIPLE'        WHERE id = 7;
UPDATE valuation_models SET model_code = 'RIM'                 WHERE id = 8;
UPDATE valuation_models SET model_code = 'ADJUSTED_BOOK_VALUE' WHERE id = 9;
UPDATE valuation_models SET model_code = 'REAL_OPTION'         WHERE id = 10;

CREATE UNIQUE INDEX IF NOT EXISTS unique_valuation_model_code
    ON valuation_models (model_code);

-- ---------------------------------------------------------------------
-- 4. 各モデルの解説文
-- ---------------------------------------------------------------------

-- ① DCF法 -------------------------------------------------------------
UPDATE valuation_models SET
  formula_description = '事業価値 = Σ FCFt ÷ (1+WACC)^t ＋ ターミナルバリュー ÷ (1+WACC)^n / 理論株価 = (事業価値 ＋ 非事業資産 − 有利子負債) ÷ 発行済株式数',
  characteristics = '企業が将来生み出すフリーキャッシュフロー（FCF）を予測し、WACC（加重平均資本コスト）で現在価値に割り引いて事業価値を求める。そこに非事業資産を加え有利子負債を差し引いて株主価値とし、発行済株式数で割って1株あたりの理論価格を導く。株価や同業他社に依存せず、企業の「稼ぐ力」そのものを直接評価する絶対評価法の代表格。',
  use_case = '営業キャッシュフローが安定してプラスで、事業計画から5〜10年先のFCFがある程度見通せる成熟企業・インフラ・製造業の評価に向く。M&Aの買収価格算定や、市場全体が過熱／悲観に傾いていて他社比較が当てにならない局面で特に有効。',
  advantages = '株式市場の水準や同業他社の株価に一切依存しないため、市場のゆがみに影響されない本源的価値を算出できる。成長率・割引率・投資計画といった前提を明示的に組み込めるので、なぜその価格になるのかを数式で説明できる。',
  disadvantages = 'ターミナルバリューが理論株価の6〜8割を占めることが多く、永久成長率やWACCを0.5%動かすだけで結果が大きく振れる。赤字企業やFCFがマイナスの企業、事業モデルが変化している最中の企業には適用できない。前提の置き方次第でいかようにも数字を作れてしまう点にも注意が必要。'
WHERE id = 1;

-- ② PERマルチプル法 ---------------------------------------------------
UPDATE valuation_models SET
  formula_description = '理論株価 = 予想EPS × 目標PER倍率',
  characteristics = '1株当たり利益（EPS）に、同業他社や過去実績から妥当と考える目標PER倍率を掛けて理論株価を求める相対評価法。「この会社の利益なら市場は何倍の値を付けるか」という発想に立つ、最も広く使われている手法。',
  use_case = '黒字が安定していて比較可能な同業上場企業が複数ある業種に向く。短時間で株価の目線を作りたいスクリーニング段階や、DCFで算出した理論株価の妥当性をクロスチェックする用途で使われる。',
  advantages = '必要な入力がEPSと目標倍率の2つだけで、計算が速く直感的に理解しやすい。市場参加者が実際に使っている物差しであるため、現在株価との乖離を説明する力が強い。',
  disadvantages = '目標PERの決め方が主観的で、比較対象の選び方ひとつで結果が変わる。市場全体が割高／割安に偏っていると、そのゆがみをそのまま引き継いでしまう。赤字企業（EPSがマイナス）には適用できず、一過性の特別損益や会計方針の違いでEPSが歪むと数値の信頼性が落ちる。'
WHERE id = 2;

-- ③ 配当割引モデル (DDM) ----------------------------------------------
UPDATE valuation_models SET
  formula_description = '理論株価 = 来期予想1株配当 ÷ (株主資本コスト − 永久配当成長率)　※ゴードン成長モデル',
  characteristics = '株主が実際に受け取るキャッシュ＝配当だけに着目し、来期予想配当を「株主資本コスト − 永久配当成長率」で割って理論株価を求める。企業価値を経由せず、最初から株主価値を直接算出するのが特徴。',
  use_case = '配当性向が安定していて減配リスクの小さい成熟企業向け。電力・ガス・通信・大手金融・REITなど、キャッシュフローの見通しが立ちやすく配当を重視する銘柄の評価や、インカム狙いの長期投資家の目線合わせに使われる。',
  advantages = '必要なデータが配当・株主資本コスト・配当成長率の3つだけで、前提が少なく透明性が高い。株主が受け取る現金そのものを評価対象にするため、会計上の利益操作の影響を受けにくい。',
  disadvantages = '無配・低配当の成長企業にはまったく使えない。分母が (r − g) であるため、配当成長率を株主資本コストに近づけるだけで理論株価が発散する。内部留保による再投資が生む価値を捉えられず、結果が保守的に出やすい。'
WHERE id = 3;

-- ⑤ EV/EBITDA倍率法 ---------------------------------------------------
UPDATE valuation_models SET
  formula_description = '理論株価 = (EBITDA × 目標EV/EBITDA倍率 − 純有利子負債) ÷ 発行済株式数',
  characteristics = '目標EV/EBITDA倍率にEBITDA（償却前営業利益）を掛けて企業価値（EV）を求め、そこから純有利子負債を差し引いて株主価値を算出する。減価償却方法・税率・資本構成の違いを取り除いて企業同士を比較できる。「買収に必要な金額を何年分の利益で回収できるか」を表す指標でもある。',
  use_case = '設備投資が重く減価償却費の影響が大きい製造業・通信・不動産の評価に向く。会計基準の異なる国をまたぐクロスボーダー比較や、負債を多く抱える企業、LBO・M&Aの買収価格算定でも標準的に使われる。',
  advantages = '資本構成（負債の多寡）と減価償却の差異を排除するため、PERよりも企業間の素の収益力を比べやすい。営業利益が赤字でもEBITDAが黒字であれば評価を継続できる。',
  disadvantages = '減価償却費を無視するため、設備更新に多額の投資が必要な企業の実力を過大評価しやすい。運転資本の増減や実際の設備投資額（CAPEX）が反映されない。純有利子負債の定義次第で結果が動く点にも注意が必要。'
WHERE id = 5;

-- ⑥ PBRマルチプル法 ---------------------------------------------------
UPDATE valuation_models SET
  formula_description = '理論株価 = 1株当たり純資産（BPS） × 目標PBR倍率',
  characteristics = '1株当たり純資産（BPS）に目標PBRを掛けて理論株価を求める。損益（フロー）ではなく貸借対照表（ストック）に着目した評価で、PBR1倍が理論上の解散価値に相当する。',
  use_case = '銀行・保険などバランスシートそのものが事業である業種、資産保有型の不動産・商社に向く。赤字転落や業績変動が激しくPERが機能しない局面で、バリュー投資の下値目処を算定する用途でも使われる。',
  advantages = '純資産は利益に比べて期ごとの振れが小さく、赤字企業にも適用できる。下値の目安（フロア）として機能し、PBR1倍割れという明確で共有しやすい割安判断の基準を提供する。',
  disadvantages = '簿価は取得原価ベースのため、含み益のある土地や、価値の落ちた設備・のれんが実態と乖離する。ブランド・人材・技術といった無形資産をほとんど計上しないIT・サービス業では実力を大幅に過小評価する。ROEが低い企業はPBR1倍割れが「適正」な場合もあり、割安とは限らない。'
WHERE id = 6;

-- ⑦ PSRマルチプル法 ---------------------------------------------------
UPDATE valuation_models SET
  formula_description = '理論株価 = (年間売上高 × 目標PSR倍率) ÷ 発行済株式数',
  characteristics = '年間売上高に目標PSR（株価売上高倍率）を掛けて時価総額を求め、発行済株式数で割って理論株価とする。利益ではなくトップライン（売上高）を評価軸に据える点が最大の特徴。',
  use_case = '先行投資で赤字だが売上が急拡大しているSaaS・スタートアップ・バイオ企業の評価に向く。上場直後で利益実績が乏しい企業や、一時的な大幅減益で他の指標が機能しない局面でも使える。',
  advantages = '売上高は赤字でも必ず存在し、会計方針による操作余地が利益より小さいため、赤字成長企業でも評価を継続できる。景気循環による利益のブレにも左右されにくい。',
  disadvantages = '利益率をまったく考慮しないため、低採算のまま売上だけ伸ばしている企業を過大評価する。粗利率30%の企業と70%の企業を同列に扱ってしまう。目標PSRの妥当水準が業種・成長率によって大きく異なり、根拠を示しにくい。'
WHERE id = 7;

-- ⑧ 残余利益モデル (RIM) ----------------------------------------------
UPDATE valuation_models SET
  formula_description = '株主価値 = 現在の純資産 ＋ Σ (ROE − 株主資本コスト) × 期首純資産 ÷ (1+r)^t ＋ 残余利益の永続価値',
  characteristics = '現在の純資産（簿価）を出発点に、将来の「残余利益＝会計利益 − 株主資本コスト × 期首純資産」の現在価値を積み上げて株主価値を求める。理論株価の大部分を既に確定している簿価が占めるため、DCFに比べてターミナルバリューへの依存度が低い。',
  use_case = 'FCFが不安定でDCFが機能しにくい企業の評価に向く。会計情報の信頼性が高い企業や、ROEが株主資本コストを上回って本当に価値を創造できているかを検証したい場面で使われる。',
  advantages = '価値の大半が観測可能な現在の純資産から来るため、遠い将来の予測誤差に対して頑健。ROEと株主資本コストの差（エクイティスプレッド）が価値の源泉として明示され、経営の良否がそのまま株価に翻訳される。無配・FCFマイナスの企業にも適用できる。',
  disadvantages = 'クリーンサープラス関係（純資産の増減＝利益−配当）の成立が前提で、その他包括利益が大きい企業ではずれる。会計方針の違いが直接結果に効くため国際比較には向かない。日本では投資家の認知度が低く、算出結果を他者と共有しにくい面もある。'
WHERE id = 8;

-- ⑨ 修正純資産法 -------------------------------------------------------
UPDATE valuation_models SET
  formula_description = '理論株価 = ((総資産 × (1 + 資産時価調整率) − 総負債) × (1 − 清算コスト率)) ÷ 発行済株式数',
  characteristics = '貸借対照表の資産・負債を時価に評価し直した「時価純資産」を株主価値とみなす評価法。簿価純資産に含み損益を反映させ、清算を想定する場合はさらに処分コストを控除する。',
  use_case = '含み益を抱えた土地や有価証券を持つ老舗企業・資産管理会社の評価、清算や廃業を前提とした価値算定に向く。継続企業としての収益力が乏しくDCFやPERでは価値が出ない企業の、下限値を把握する用途でも使われる。',
  advantages = '評価の根拠が実在する資産と負債であり、将来予測をほとんど含まないため恣意性が最も小さい。株価がこの水準を割り込んでいれば、明確な下値支持線として使える。',
  disadvantages = '将来の収益力（のれん・営業権）を一切評価しないため、成長企業や無形資産主体の企業では大幅な過小評価になる。非上場資産や特殊な設備の時価評価が難しく、評価者によって結果がぶれる。継続企業の価値を測る指標としては不完全。'
WHERE id = 9;

-- ⑩ リアル・オプション分析 --------------------------------------------
UPDATE valuation_models SET
  formula_description = '株主価値 = V × N(d1) − D × e^(−rT) × N(d2)　※企業資産Vを原資産、有利子負債Dを行使価格とするコール・オプション（マートン・モデル）',
  characteristics = 'ブラック・ショールズ式を応用し、株式を「企業資産を原資産、有利子負債を行使価格とするコール・オプション」とみなして評価する。将来の不確実性（ボラティリティ）が高いほど、事業を撤退・縮小できる経営の柔軟性の価値が上乗せされる点が他の手法と決定的に異なる。',
  use_case = '資源開発・創薬・大型設備投資など、成否が不確実で段階的に投資判断を下せるプロジェクトを抱える企業に向く。負債が重く倒産リスクが意識される企業の株式価値評価や、DCFでは価値がマイナスでも撤退オプションに価値がある案件の評価に使われる。',
  advantages = 'DCFが切り捨ててしまう「やめられる権利」「拡大できる権利」を金額として評価できる。不確実性を単に割引率の引き上げで罰するのではなく、価値の源泉として扱える点は他の手法にない強み。',
  disadvantages = '資産ボラティリティや権利行使期間といった入力値が直接観測できず、推定に強く依存する。ブラック・ショールズが前提とする対数正規分布・連続取引は実物資産に厳密には当てはまらない。計算がブラックボックス化しやすく、経営陣や投資家への説明が難しい。'
WHERE id = 10;

-- ---------------------------------------------------------------------
-- 5. 既存パラメータ（モデル 1〜3）に単位・範囲・既定値を設定
-- ---------------------------------------------------------------------
UPDATE valuation_parameters SET
  parameter_name = 'WACC (割引率)', display_order = 1,
  unit = 'PERCENT', min_value = 1.0, max_value = 20.0, step_value = 0.1, default_value = 6.0
WHERE valuation_model_id = 1 AND parameter_code = 'wacc';

UPDATE valuation_parameters SET
  parameter_name = '将来5年成長率', display_order = 2,
  unit = 'PERCENT', min_value = -10.0, max_value = 30.0, step_value = 0.1, default_value = 2.0
WHERE valuation_model_id = 1 AND parameter_code = 'growth_in_5_years';

UPDATE valuation_parameters SET
  parameter_name = '永久成長率', display_order = 3,
  unit = 'PERCENT', min_value = 0.0, max_value = 5.0, step_value = 0.1, default_value = 1.0
WHERE valuation_model_id = 1 AND parameter_code = 'terminal_growth';

UPDATE valuation_parameters SET
  parameter_name = '目標PER倍率', display_order = 1,
  unit = 'TIMES', min_value = 1.0, max_value = 60.0, step_value = 0.1, default_value = 15.0
WHERE valuation_model_id = 2 AND parameter_code = 'target_per';

UPDATE valuation_parameters SET
  parameter_name = '株主資本コスト', display_order = 1,
  unit = 'PERCENT', min_value = 1.0, max_value = 20.0, step_value = 0.1, default_value = 7.0
WHERE valuation_model_id = 3 AND parameter_code = 'cost_of_equity';

UPDATE valuation_parameters SET
  parameter_name = '永久配当成長率', display_order = 2,
  unit = 'PERCENT', min_value = 0.0, max_value = 10.0, step_value = 0.1, default_value = 1.0
WHERE valuation_model_id = 3 AND parameter_code = 'dividend_growth';

-- ---------------------------------------------------------------------
-- 6. モデル 5〜10 のパラメータ再定義
--    旧定義（EV, EBITDA, 株価, BPS ...）は「倍率を逆算する入力」であり
--    理論株価を算出できないため、目標倍率／率の入力に置き換える。
--    旧行はどの企業からも参照されていないため安全に削除できる。
-- ---------------------------------------------------------------------
DELETE FROM valuation_parameters WHERE valuation_model_id IN (5, 6, 7, 8, 9, 10);

-- id は identity 列だがシーケンスが既存最大値より手前にあり、そのまま INSERT すると
-- 主キー重複になる。採番位置を現在の最大 id に合わせてから登録する。
SELECT setval(
    pg_get_serial_sequence('valuation_parameters', 'id'),
    COALESCE((SELECT max(id) FROM valuation_parameters), 1)
);

INSERT INTO valuation_parameters
  (valuation_model_id, parameter_code, parameter_name, display_order, unit, min_value, max_value, step_value, default_value)
VALUES
  -- ⑤ EV/EBITDA倍率法
  (5,  'target_ev_ebitda',        '目標EV/EBITDA倍率',     1, 'TIMES',    1.0,  30.0, 0.1,   8.0),
  -- ⑥ PBRマルチプル法
  (6,  'target_pbr',              '目標PBR倍率',           1, 'TIMES',    0.1,  10.0, 0.01,  1.0),
  -- ⑦ PSRマルチプル法
  (7,  'target_psr',              '目標PSR倍率',           1, 'TIMES',    0.1,  20.0, 0.01,  1.5),
  -- ⑧ 残余利益モデル (RIM)
  (8,  'cost_of_equity',          '株主資本コスト',        1, 'PERCENT',  1.0,  20.0, 0.1,   8.0),
  (8,  'roe_forecast',            '予想ROE',               2, 'PERCENT', -10.0, 40.0, 0.1,   9.0),
  (8,  'residual_persistence',    '残余利益持続率',        3, 'PERCENT',   0.0,100.0, 1.0,  60.0),
  -- ⑨ 修正純資産法
  (9,  'asset_revaluation_rate',  '資産時価調整率',        1, 'PERCENT', -50.0,100.0, 0.5,   0.0),
  (9,  'liquidation_cost_rate',   '清算コスト率',          2, 'PERCENT',   0.0, 50.0, 0.5,   0.0),
  -- ⑩ リアル・オプション分析
  (10, 'asset_volatility',        '資産ボラティリティ',    1, 'PERCENT',   5.0,100.0, 1.0,  30.0),
  (10, 'risk_free_rate',          '無リスク金利',          2, 'PERCENT',   0.0, 10.0, 0.05,  1.0),
  (10, 'debt_maturity',           '負債の平均残存年数',    3, 'YEARS',     1.0, 20.0, 0.5,   5.0);

COMMIT;
