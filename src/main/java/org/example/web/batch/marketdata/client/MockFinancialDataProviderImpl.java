package org.example.web.batch.marketdata.client;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.Map;

import org.example.web.batch.marketdata.client.dto.FinancialStatementData;
import org.springframework.stereotype.Component;

// FinancialDataProvider のモック実装。外部API（EDINET / FMP）を一切呼び出さず、
// MockSampleCompanies に登録した日米比 2 社ずつ・計 6 社分の四半期決算サンプルを返す。
// marketdata.api.mock-enabled=true のとき UsFinancialStatementItemProcessor・
// PhFinancialStatementItemProcessor がこの Bean（"mockFinancialDataProvider"）を使う。
// 日本株（EDINET の2段構成）はこのメソッドを直接使わず、EdinetDocumentListTasklet /
// JpFinancialStatementItemProcessor がモック時に fetchByCode() 経由で参照する（設計上 EDINET は
// 銘柄コード単発取得APIを持たないため）。
@Component("mockFinancialDataProvider")
public class MockFinancialDataProviderImpl implements FinancialDataProvider {

    private record SampleStatement(
            BigDecimal revenue, BigDecimal operatingIncome, BigDecimal netIncome, BigDecimal eps, BigDecimal bps,
            BigDecimal operatingCashFlow, BigDecimal investingCashFlow, BigDecimal freeCashFlow,
            BigDecimal totalAssets, BigDecimal totalDebt, BigDecimal cashAndEquivalents,
            BigDecimal grossProfit, BigDecimal sgAndA, BigDecimal ebit, BigDecimal totalEquity,
            BigDecimal inventory, BigDecimal accountsReceivable, BigDecimal financingCf,
            BigDecimal interestExpense, BigDecimal dividendsPaid) {
    }

    private static final Map<String, SampleStatement> SAMPLE_STATEMENTS = Map.of(
            // トヨタ自動車（百万円単位ではなく円単位、JPY）
            "7203", new SampleStatement(
                    new BigDecimal("11800000000000"), new BigDecimal("1200000000000"), new BigDecimal("950000000000"),
                    new BigDecimal("68.5"), new BigDecimal("3200"),
                    new BigDecimal("1500000000000"), new BigDecimal("-900000000000"), new BigDecimal("600000000000"),
                    new BigDecimal("74000000000000"), new BigDecimal("45000000000000"), new BigDecimal("6500000000000"),
                    new BigDecimal("2400000000000"), new BigDecimal("1100000000000"), new BigDecimal("1200000000000"),
                    new BigDecimal("29000000000000"), new BigDecimal("3800000000000"), new BigDecimal("3100000000000"),
                    new BigDecimal("-400000000000"), new BigDecimal("15000000000"), new BigDecimal("-280000000000")),
            // ソニーグループ（JPY）
            "6758", new SampleStatement(
                    new BigDecimal("3000000000000"), new BigDecimal("330000000000"), new BigDecimal("250000000000"),
                    new BigDecimal("40.2"), new BigDecimal("2100"),
                    new BigDecimal("400000000000"), new BigDecimal("-150000000000"), new BigDecimal("250000000000"),
                    new BigDecimal("27000000000000"), new BigDecimal("12000000000000"), new BigDecimal("1800000000000"),
                    new BigDecimal("900000000000"), new BigDecimal("500000000000"), new BigDecimal("330000000000"),
                    new BigDecimal("10300000000000"), new BigDecimal("900000000000"), new BigDecimal("800000000000"),
                    new BigDecimal("-100000000000"), new BigDecimal("8000000000"), new BigDecimal("-35000000000")),
            // Apple（USD）
            "AAPL", new SampleStatement(
                    new BigDecimal("90000000000"), new BigDecimal("27000000000"), new BigDecimal("23000000000"),
                    new BigDecimal("1.45"), new BigDecimal("4.25"),
                    new BigDecimal("28000000000"), new BigDecimal("-3000000000"), new BigDecimal("25000000000"),
                    new BigDecimal("335000000000"), new BigDecimal("108000000000"), new BigDecimal("32000000000"),
                    new BigDecimal("41000000000"), new BigDecimal("6500000000"), new BigDecimal("27000000000"),
                    new BigDecimal("65000000000"), new BigDecimal("6800000000"), new BigDecimal("22000000000"),
                    new BigDecimal("-24000000000"), new BigDecimal("950000000"), new BigDecimal("-3800000000")),
            // Microsoft（USD）
            "MSFT", new SampleStatement(
                    new BigDecimal("62000000000"), new BigDecimal("27500000000"), new BigDecimal("22000000000"),
                    new BigDecimal("2.95"), new BigDecimal("24.10"),
                    new BigDecimal("30000000000"), new BigDecimal("-9000000000"), new BigDecimal("20000000000"),
                    new BigDecimal("480000000000"), new BigDecimal("105000000000"), new BigDecimal("75000000000"),
                    new BigDecimal("43000000000"), new BigDecimal("7000000000"), new BigDecimal("27500000000"),
                    new BigDecimal("245000000000"), new BigDecimal("2700000000"), new BigDecimal("40000000000"),
                    new BigDecimal("-11000000000"), new BigDecimal("600000000"), new BigDecimal("-5700000000")),
            // BDOユニバンク（PHP、銀行のため inventory/accountsReceivable は対象外で null）
            "BDO", new SampleStatement(
                    new BigDecimal("45000000000"), new BigDecimal("17000000000"), new BigDecimal("14500000000"),
                    new BigDecimal("3.35"), new BigDecimal("95.00"),
                    new BigDecimal("20000000000"), new BigDecimal("-8000000000"), new BigDecimal("12000000000"),
                    new BigDecimal("4300000000000"), new BigDecimal("3800000000000"), new BigDecimal("350000000000"),
                    new BigDecimal("30000000000"), new BigDecimal("13000000000"), new BigDecimal("17000000000"),
                    new BigDecimal("460000000000"), null, null,
                    new BigDecimal("-2000000000"), new BigDecimal("9000000000"), new BigDecimal("-3200000000")),
            // フィリピン諸島銀行（PHP、銀行のため inventory/accountsReceivable は対象外で null）
            "BPI", new SampleStatement(
                    new BigDecimal("32000000000"), new BigDecimal("13500000000"), new BigDecimal("11200000000"),
                    new BigDecimal("2.55"), new BigDecimal("78.00"),
                    new BigDecimal("15000000000"), new BigDecimal("-5500000000"), new BigDecimal("9000000000"),
                    new BigDecimal("2950000000000"), new BigDecimal("2600000000000"), new BigDecimal("260000000000"),
                    new BigDecimal("21000000000"), new BigDecimal("9000000000"), new BigDecimal("13500000000"),
                    new BigDecimal("300000000000"), null, null,
                    new BigDecimal("-1500000000"), new BigDecimal("6000000000"), new BigDecimal("-2400000000")));

    private static final String FISCAL_QUARTER = "Q2";

    @Override
    public FinancialStatementData fetchLatestStatement(String code) {
        SampleStatement s = SAMPLE_STATEMENTS.get(code);
        if (s == null) {
            return null;
        }
        int fiscalYear = LocalDate.now().getYear();
        LocalDate endDate = LocalDate.of(fiscalYear, Month.JUNE, 30);
        return new FinancialStatementData(
                fiscalYear, FISCAL_QUARTER, endDate,
                s.revenue(), s.operatingIncome(), s.netIncome(), s.eps(), s.bps(),
                s.operatingCashFlow(), s.investingCashFlow(), s.freeCashFlow(),
                s.totalAssets(), s.totalDebt(), s.cashAndEquivalents(),
                s.grossProfit(), s.sgAndA(), s.ebit(), s.totalEquity(),
                s.inventory(), s.accountsReceivable(), s.financingCf(),
                s.interestExpense(), s.dividendsPaid());
    }
}
