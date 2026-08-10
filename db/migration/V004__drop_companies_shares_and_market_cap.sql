-- =====================================================================
-- companies.outstanding_shares / companies.market_cap の整理
--
-- - outstanding_shares: 理論株価計算（DCF・EV/EBITDA・PSR倍率法・RIM・修正純資産法等、
--   pv_calculator.js の _requireFields）で必須のため、current_price と同じ方針で
--   daily_quotes.shares_outstanding へ移設してから companies から削除する。
-- - market_cap: アプリケーションコードから一切参照されていない未使用列のため、
--   移設せずそのまま削除する。
--
-- 1. companies.outstanding_shares を daily_quotes へバックフィル（date = 適用日時点の日付）
-- 2. companies.outstanding_shares / market_cap を DROP COLUMN
-- =====================================================================

BEGIN;

-- ---------------------------------------------------------------------
-- 1. バックフィル
-- ---------------------------------------------------------------------
INSERT INTO daily_quotes (company_id, date, shares_outstanding)
SELECT c.id, CURRENT_DATE, c.outstanding_shares
FROM companies c
WHERE c.outstanding_shares IS NOT NULL AND c.outstanding_shares <> 0
ON CONFLICT (company_id, date)
DO UPDATE SET shares_outstanding = EXCLUDED.shares_outstanding;

-- ---------------------------------------------------------------------
-- 2. companies.outstanding_shares / market_cap を削除
-- ---------------------------------------------------------------------
ALTER TABLE companies DROP COLUMN outstanding_shares;
ALTER TABLE companies DROP COLUMN market_cap;

COMMIT;
