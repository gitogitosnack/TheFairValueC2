package org.example.web.batch.marketdata.client.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

// FinancialDataProvider が返す財務諸表 1 期分のまとまり。FinancialStatementEntity と
// ほぼ同じ項目を持つが、company_id を持たない（呼び出し側の Processor が付与する）。
// UsFinancialStatementItemProcessor / JpFinancialStatementItemProcessor がこれを
// FinancialStatementEntity へ変換する（設計書 4.3.1・4.3.2 参照）。
public record FinancialStatementData(
        Integer fiscalYear,
        String fiscalQuarter,
        LocalDate endDate,
        BigDecimal revenue,
        BigDecimal operatingIncome,
        BigDecimal netIncome,
        BigDecimal eps,
        BigDecimal bps,
        BigDecimal operatingCashFlow,
        BigDecimal investingCashFlow,
        BigDecimal freeCashFlow,
        BigDecimal totalAssets,
        BigDecimal totalDebt,
        BigDecimal cashAndEquivalents,
        BigDecimal grossProfit,
        BigDecimal sgAndA,
        BigDecimal ebit,
        BigDecimal totalEquity,
        BigDecimal inventory,
        BigDecimal accountsReceivable,
        BigDecimal financingCf,
        BigDecimal interestExpense,
        BigDecimal dividendsPaid) {
}
