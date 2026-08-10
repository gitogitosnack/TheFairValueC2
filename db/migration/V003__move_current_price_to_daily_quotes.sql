-- =====================================================================
-- companies.current_price を daily_quotes に一本化する
--
-- 「現在株価」は companies にキャッシュ列として持たず、daily_quotes の
-- 最新日付（company_id, date のUNIQUE制約あり）の close_price を参照する方式に変更する。
-- market_cap / outstanding_shares は companies 側に残す（今回のスコープ外）。
--
-- 1. companies.current_price を daily_quotes へバックフィル（date = 適用日時点の日付）
-- 2. companies.current_price を DROP COLUMN
-- =====================================================================

BEGIN;

-- ---------------------------------------------------------------------
-- 1. バックフィル
-- ---------------------------------------------------------------------
INSERT INTO daily_quotes (company_id, date, close_price)
SELECT c.id, CURRENT_DATE, c.current_price
FROM companies c
WHERE c.current_price IS NOT NULL
ON CONFLICT (company_id, date)
DO UPDATE SET close_price = EXCLUDED.close_price;

-- ---------------------------------------------------------------------
-- 2. companies.current_price を削除
-- ---------------------------------------------------------------------
ALTER TABLE companies DROP COLUMN current_price;

COMMIT;
