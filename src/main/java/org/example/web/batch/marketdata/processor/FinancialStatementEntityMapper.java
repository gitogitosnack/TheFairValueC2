package org.example.web.batch.marketdata.processor;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.example.web.batch.marketdata.client.dto.FinancialStatementData;
import org.example.web.entity.FinancialStatementEntity;

// UsFinancialStatementItemProcessor / JpFinancialStatementItemProcessor で共用する変換ヘルパー。
// financial_statements の金額系カラムは百万円（百万通貨単位）・bigint で格納する規約
// （StockDetailServiceImpl の toYen() 相当の逆変換。既知の課題: 通貨単位は区別していない）のため、
// FMP・EDINET から取得した実額（円・ドル単位）はここで百万単位に丸めてから Entity にセットする。
// eps / bps は1株あたりの値のため変換しない。
final class FinancialStatementEntityMapper {

    private static final BigDecimal MILLION = BigDecimal.valueOf(1_000_000);

    private FinancialStatementEntityMapper() {
    }

    static FinancialStatementEntity toEntity(Integer companyId, FinancialStatementData d) {
        FinancialStatementEntity e = new FinancialStatementEntity();
        e.setCompanyId(companyId);
        e.setFiscalYear(d.fiscalYear());
        e.setFiscalQuarter(d.fiscalQuarter());
        e.setEndDate(d.endDate());
        e.setRevenue(toMillion(d.revenue()));
        e.setOperatingIncome(toMillion(d.operatingIncome()));
        e.setNetIncome(toMillion(d.netIncome()));
        e.setEps(d.eps());
        e.setBps(d.bps());
        e.setOperatingCashFlow(toMillion(d.operatingCashFlow()));
        e.setInvestingCashFlow(toMillion(d.investingCashFlow()));
        e.setFreeCashFlow(toMillion(d.freeCashFlow()));
        e.setTotalAssets(toMillion(d.totalAssets()));
        e.setTotalDebt(toMillion(d.totalDebt()));
        e.setCashAndEquivalents(toMillion(d.cashAndEquivalents()));
        e.setGrossProfit(toMillion(d.grossProfit()));
        e.setSgAndA(toMillion(d.sgAndA()));
        e.setEbit(toMillion(d.ebit()));
        e.setTotalEquity(toMillion(d.totalEquity()));
        e.setInventory(toMillion(d.inventory()));
        e.setAccountsReceivable(toMillion(d.accountsReceivable()));
        e.setFinancingCf(toMillion(d.financingCf()));
        e.setInterestExpense(toMillion(d.interestExpense()));
        e.setDividendsPaid(toMillion(d.dividendsPaid()));
        return e;
    }

    private static BigDecimal toMillion(BigDecimal raw) {
        return raw == null ? null : raw.divide(MILLION, 0, RoundingMode.HALF_UP);
    }
}
