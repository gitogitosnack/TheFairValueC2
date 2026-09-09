package org.example.web.stock.valuationmodel.service;

import java.math.BigDecimal;

import org.example.web.entity.AnalysisIndicatorEntity;
import org.example.web.entity.FinancialStatementEntity;

/**
 * 財務諸表（{@link FinancialStatementEntity}）と市場データ（現在株価・発行済株式数）から
 * 分析指標（ROE・PER 等）を算出するロジック本体。
 *
 * 銘柄データ定期取得バッチ（{@code web.batch.marketdata}）の
 * {@code AnalysisIndicatorRecalcService} はこのロジックを呼び出すだけのオーケストレーション役に徹し、
 * 算出式自体はここに一元管理する（docs/batch-market-data-sync-design.md 4.3.3・11章 回答6 参照）。
 */
public interface FinancialIndicatorCalculationService {

    /**
     * 分析指標を算出する。company_id / fiscal_year / fiscal_quarter は呼び出し側が設定する。
     *
     * @param statement     算出対象の財務諸表1期分
     * @param currentPrice  現在株価（daily_quotes の最新終値）。無ければ PER/PBR/配当利回りは null になる
     * @param sharesOutstanding 発行済株式数（daily_quotes 由来）。無ければ配当利回り等は null になる
     */
    AnalysisIndicatorEntity calculate(
            FinancialStatementEntity statement, BigDecimal currentPrice, Long sharesOutstanding);
}
