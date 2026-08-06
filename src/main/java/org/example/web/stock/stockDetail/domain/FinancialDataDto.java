package org.example.web.stock.stockDetail.domain;

/**
 * 理論株価の算出に使う財務データ。
 *
 * <p>
 * 金額項目は「円」単位で保持する。DB（financial_statements / analysis_indicators）の
 * 金額は百万円単位で格納されているため、詰め替え時に 1,000,000 倍している。
 * 1 株あたりの項目（eps / bps / dividendPerShare / currentPrice）と
 * 率の項目（roe / payoutRatio）はそのままの値を保持する。
 * </p>
 *
 * <p>
 * 値が取得も推計もできなかった項目は null のままにする。
 * フロント側はどの項目が null かを見て「データ不足で算出不可」を判定する。
 * </p>
 */
public class FinancialDataDto {

    /** 参照した会計年度 */
    private Integer fiscalYear;
    /** 発行済株式総数（株） */
    private Long sharesOutstanding;
    /** 現在株価（円） */
    private Double currentPrice;

    /** フリーキャッシュフロー（円） */
    private Double fcf;
    /** 1 株当たり利益（円） */
    private Double eps;
    /** 1 株当たり純資産（円） */
    private Double bps;
    /** 1 株当たり配当（円） */
    private Double dividendPerShare;

    /** 年間売上高（円） */
    private Double revenue;
    /** EBITDA（円）。減価償却費が DB に無いため EBIT（無ければ営業利益）で代用している */
    private Double ebitda;
    /** 総資産（円） */
    private Double totalAssets;
    /** 純資産（円） */
    private Double totalEquity;
    /** 有利子負債（円） */
    private Double interestBearingDebt;
    /** 現金及び現金同等物（円） */
    private Double cashAndEquivalents;

    /** ROE（%） */
    private Double roe;
    /** 配当性向（%） */
    private Double payoutRatio;

    public Integer getFiscalYear() {
        return fiscalYear;
    }

    public void setFiscalYear(Integer fiscalYear) {
        this.fiscalYear = fiscalYear;
    }

    public Long getSharesOutstanding() {
        return sharesOutstanding;
    }

    public void setSharesOutstanding(Long sharesOutstanding) {
        this.sharesOutstanding = sharesOutstanding;
    }

    public Double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(Double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public Double getFcf() {
        return fcf;
    }

    public void setFcf(Double fcf) {
        this.fcf = fcf;
    }

    public Double getEps() {
        return eps;
    }

    public void setEps(Double eps) {
        this.eps = eps;
    }

    public Double getBps() {
        return bps;
    }

    public void setBps(Double bps) {
        this.bps = bps;
    }

    public Double getDividendPerShare() {
        return dividendPerShare;
    }

    public void setDividendPerShare(Double dividendPerShare) {
        this.dividendPerShare = dividendPerShare;
    }

    public Double getRevenue() {
        return revenue;
    }

    public void setRevenue(Double revenue) {
        this.revenue = revenue;
    }

    public Double getEbitda() {
        return ebitda;
    }

    public void setEbitda(Double ebitda) {
        this.ebitda = ebitda;
    }

    public Double getTotalAssets() {
        return totalAssets;
    }

    public void setTotalAssets(Double totalAssets) {
        this.totalAssets = totalAssets;
    }

    public Double getTotalEquity() {
        return totalEquity;
    }

    public void setTotalEquity(Double totalEquity) {
        this.totalEquity = totalEquity;
    }

    public Double getInterestBearingDebt() {
        return interestBearingDebt;
    }

    public void setInterestBearingDebt(Double interestBearingDebt) {
        this.interestBearingDebt = interestBearingDebt;
    }

    public Double getCashAndEquivalents() {
        return cashAndEquivalents;
    }

    public void setCashAndEquivalents(Double cashAndEquivalents) {
        this.cashAndEquivalents = cashAndEquivalents;
    }

    public Double getRoe() {
        return roe;
    }

    public void setRoe(Double roe) {
        this.roe = roe;
    }

    public Double getPayoutRatio() {
        return payoutRatio;
    }

    public void setPayoutRatio(Double payoutRatio) {
        this.payoutRatio = payoutRatio;
    }
}
