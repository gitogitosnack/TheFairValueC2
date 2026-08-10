-- =====================================================================
-- 銘柄詳細画面「競合・業界平均と比較」対応（手動登録ロジックのUI実装分）
--
-- 1. company_competitors : 企業ごとの手動登録競合を保持する中間テーブル。
--    A→B を登録したら B→A も同時に1組として保存する（双方向）。
-- 2. 動作確認用のダミー企業3社と、花王(4452)への手動競合登録を投入する。
--    ダミー企業は名前・コードで一目でわかるようにしている。
-- =====================================================================

BEGIN;

-- ---------------------------------------------------------------------
-- 1. company_competitors
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS company_competitors (
    id                     serial PRIMARY KEY,
    company_id             integer NOT NULL REFERENCES companies(id),
    competitor_company_id  integer NOT NULL REFERENCES companies(id),
    delete_flg             integer NOT NULL DEFAULT 0,
    CONSTRAINT company_competitors_no_self CHECK (company_id <> competitor_company_id),
    CONSTRAINT company_competitors_unique UNIQUE (company_id, competitor_company_id)
);

-- ---------------------------------------------------------------------
-- 2. ダミーデータ
-- ---------------------------------------------------------------------
-- ダミー競合企業（コード・企業名だけでダミーとわかるようにしている）
-- 現在株価・発行済株式数は companies ではなく daily_quotes 側で保持する
-- （current_price は V003、outstanding_shares は V004 参照）ため、
-- companies には market_cap を含め投入せず、下の daily_quotes INSERT でまとめて投入する。
INSERT INTO companies
  (code, name, country_id, industry_id, market_name, currency_id, delete_flg)
VALUES
  ('DUMMY01', '【ダミー】競合商事A', 1, 1, 'ダミー市場', 1, 0),
  ('DUMMY02', '【ダミー】競合工業B', 1, 1, 'ダミー市場', 1, 0),
  ('DUMMY03', '【ダミー】競合HD C', 1, 1, 'ダミー市場', 1, 0)
ON CONFLICT (code) DO NOTHING;

-- ダミー競合企業の現在株価・発行済株式数（daily_quotes 側に一本化。V003・V004 参照）
INSERT INTO daily_quotes (company_id, date, close_price, shares_outstanding)
SELECT c.id, CURRENT_DATE, v.close_price, v.shares_outstanding
FROM companies c
JOIN (VALUES
    ('DUMMY01', 1000, 1000000),
    ('DUMMY02', 1500,  800000),
    ('DUMMY03', 2000,  500000)
  ) AS v(code, close_price, shares_outstanding)
  ON c.code = v.code
ON CONFLICT (company_id, date) DO NOTHING;

-- 花王(4452)に対してダミー競合3社を双方向で手動登録しておく
INSERT INTO company_competitors (company_id, competitor_company_id)
SELECT k.id, d.id
FROM companies k, companies d
WHERE k.code = '4452' AND d.code IN ('DUMMY01', 'DUMMY02', 'DUMMY03')
UNION ALL
SELECT d.id, k.id
FROM companies k, companies d
WHERE k.code = '4452' AND d.code IN ('DUMMY01', 'DUMMY02', 'DUMMY03')
ON CONFLICT (company_id, competitor_company_id) DO NOTHING;

COMMIT;
