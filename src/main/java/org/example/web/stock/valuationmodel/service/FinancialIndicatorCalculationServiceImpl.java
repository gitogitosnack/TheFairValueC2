package org.example.web.stock.valuationmodel.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.example.web.entity.AnalysisIndicatorEntity;
import org.example.web.entity.FinancialStatementEntity;
import org.springframework.stereotype.Service;

@Service
public class FinancialIndicatorCalculationServiceImpl implements FinancialIndicatorCalculationService {

    private static final int SCALE = 4;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    @Override
    public AnalysisIndicatorEntity calculate(
            FinancialStatementEntity fs, BigDecimal currentPrice, Long sharesOutstanding) {

        AnalysisIndicatorEntity indicator = new AnalysisIndicatorEntity();
        indicator.setFiscalYear(fs.getFiscalYear());
        indicator.setFiscalQuarter(fs.getFiscalQuarter());
        indicator.setCompanyId(fs.getCompanyId());

        indicator.setGrossMargin(percentage(fs.getGrossProfit(), fs.getRevenue()));
        indicator.setNetMargin(percentage(fs.getNetIncome(), fs.getRevenue()));
        indicator.setSgaRatio(percentage(fs.getSgAndA(), fs.getRevenue()));
        indicator.setRoa(percentage(fs.getNetIncome(), fs.getTotalAssets()));
        indicator.setRoe(percentage(fs.getNetIncome(), fs.getTotalEquity()));
        indicator.setEps(fs.getEps());
        indicator.setAssetTurnover(ratio(fs.getRevenue(), fs.getTotalAssets()));
        // 売上原価がDBに無いため、簡易的に売上高ベースで代用する
        indicator.setInventoryTurnover(ratio(fs.getRevenue(), fs.getInventory()));
        indicator.setArTurnover(ratio(fs.getRevenue(), fs.getAccountsReceivable()));
        indicator.setDeRatio(ratio(fs.getTotalDebt(), fs.getTotalEquity()));
        indicator.setDebtRatio(percentage(fs.getTotalDebt(), fs.getTotalAssets()));
        indicator.setEquityRatio(percentage(fs.getTotalEquity(), fs.getTotalAssets()));
        indicator.setFinancialLeverage(ratio(fs.getTotalAssets(), fs.getTotalEquity()));
        BigDecimal ebit = fs.getEbit() != null ? fs.getEbit() : fs.getOperatingIncome();
        indicator.setInterestCoverage(ratio(ebit, fs.getInterestExpense()));
        indicator.setOpCfMargin(percentage(fs.getOperatingCashFlow(), fs.getRevenue()));
        BigDecimal fcf = fs.getFreeCashFlow() != null
                ? fs.getFreeCashFlow()
                : sum(fs.getOperatingCashFlow(), fs.getInvestingCashFlow());
        indicator.setFcf(fcf);
        indicator.setFinCf(fs.getFinancingCf());

        BigDecimal dividendsPaid = fs.getDividendsPaid() != null ? fs.getDividendsPaid().abs() : null;
        BigDecimal dividendPerShare = perShare(dividendsPaid, sharesOutstanding);
        indicator.setDividendYield(percentage(dividendPerShare, currentPrice));
        indicator.setPayoutRatio(percentage(dividendsPaid, fs.getNetIncome()));

        indicator.setPer(ratio(currentPrice, fs.getEps()));
        BigDecimal bps = fs.getBps() != null ? fs.getBps() : perShare(fs.getTotalEquity(), sharesOutstanding);
        indicator.setPbr(ratio(currentPrice, bps));

        return indicator;
    }

    private static BigDecimal sum(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) {
            return null;
        }
        return (a != null ? a : BigDecimal.ZERO).add(b != null ? b : BigDecimal.ZERO);
    }

    // 1株あたりの値。financial_statements の金額系カラムは百万円（百万通貨単位）で格納されているため、
    // 実額に戻してから発行済株式数で割る（StockDetailServiceImpl の toYen() と同じ換算規約）。
    private static BigDecimal perShare(BigDecimal millionUnitAmount, Long sharesOutstanding) {
        if (millionUnitAmount == null || sharesOutstanding == null || sharesOutstanding == 0) {
            return null;
        }
        BigDecimal actualAmount = millionUnitAmount.multiply(BigDecimal.valueOf(1_000_000));
        return actualAmount.divide(BigDecimal.valueOf(sharesOutstanding), SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal percentage(BigDecimal numerator, BigDecimal denominator) {
        BigDecimal r = ratio(numerator, denominator);
        return r == null ? null : r.multiply(HUNDRED).setScale(SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal ratio(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null || denominator.signum() == 0) {
            return null;
        }
        return numerator.divide(denominator, SCALE, RoundingMode.HALF_UP);
    }
}
