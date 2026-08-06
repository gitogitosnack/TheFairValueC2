package org.example.web.entity;

import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.GeneratedValue;
import org.seasar.doma.GenerationType;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 財務諸表情報のエンティティ
 */
@Entity(immutable = false)
@Table(name = "financial_statements")
public class FinancialStatementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "company_id")
    private Integer companyId;

    @Column(name = "fiscal_year")
    private Integer fiscalYear;

    // DB の型は varchar(2)（'Q1'〜'Q4'）
    @Column(name = "fiscal_quarter")
    private String fiscalQuarter;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "revenue")
    private BigDecimal revenue;

    @Column(name = "operating_income")
    private BigDecimal operatingIncome;

    @Column(name = "net_income")
    private BigDecimal netIncome;

    @Column(name = "eps")
    private BigDecimal eps;

    @Column(name = "bps")
    private BigDecimal bps;

    @Column(name = "operating_cash_flow")
    private BigDecimal operatingCashFlow;

    @Column(name = "investing_cash_flow")
    private BigDecimal investingCashFlow;

    @Column(name = "free_cash_flow")
    private BigDecimal freeCashFlow;

    @Column(name = "total_assets")
    private BigDecimal totalAssets;

    @Column(name = "total_debt")
    private BigDecimal totalDebt;

    @Column(name = "cash_and_equivalents")
    private BigDecimal cashAndEquivalents;

    @Column(name = "gross_profit")
    private BigDecimal grossProfit;

    @Column(name = "sg_and_a")
    private BigDecimal sgAndA;

    @Column(name = "ebit")
    private BigDecimal ebit;

    @Column(name = "total_equity")
    private BigDecimal totalEquity;

    @Column(name = "inventory")
    private BigDecimal inventory;

    @Column(name = "accounts_receivable")
    private BigDecimal accountsReceivable;

    @Column(name = "financing_cf")
    private BigDecimal financingCf;

    @Column(name = "interest_expense")
    private BigDecimal interestExpense;

    @Column(name = "dividends_paid")
    private BigDecimal dividendsPaid;

    // --- Getter and Setter ---

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Integer companyId) {
        this.companyId = companyId;
    }

    public Integer getFiscalYear() {
        return fiscalYear;
    }

    public void setFiscalYear(Integer fiscalYear) {
        this.fiscalYear = fiscalYear;
    }

    public String getFiscalQuarter() {
        return fiscalQuarter;
    }

    public void setFiscalQuarter(String fiscalQuarter) {
        this.fiscalQuarter = fiscalQuarter;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }

    public BigDecimal getOperatingIncome() {
        return operatingIncome;
    }

    public void setOperatingIncome(BigDecimal operatingIncome) {
        this.operatingIncome = operatingIncome;
    }

    public BigDecimal getNetIncome() {
        return netIncome;
    }

    public void setNetIncome(BigDecimal netIncome) {
        this.netIncome = netIncome;
    }

    public BigDecimal getEps() {
        return eps;
    }

    public void setEps(BigDecimal eps) {
        this.eps = eps;
    }

    public BigDecimal getBps() {
        return bps;
    }

    public void setBps(BigDecimal bps) {
        this.bps = bps;
    }

    public BigDecimal getOperatingCashFlow() {
        return operatingCashFlow;
    }

    public void setOperatingCashFlow(BigDecimal operatingCashFlow) {
        this.operatingCashFlow = operatingCashFlow;
    }

    public BigDecimal getInvestingCashFlow() {
        return investingCashFlow;
    }

    public void setInvestingCashFlow(BigDecimal investingCashFlow) {
        this.investingCashFlow = investingCashFlow;
    }

    public BigDecimal getFreeCashFlow() {
        return freeCashFlow;
    }

    public void setFreeCashFlow(BigDecimal freeCashFlow) {
        this.freeCashFlow = freeCashFlow;
    }

    public BigDecimal getTotalAssets() {
        return totalAssets;
    }

    public void setTotalAssets(BigDecimal totalAssets) {
        this.totalAssets = totalAssets;
    }

    public BigDecimal getTotalDebt() {
        return totalDebt;
    }

    public void setTotalDebt(BigDecimal totalDebt) {
        this.totalDebt = totalDebt;
    }

    public BigDecimal getCashAndEquivalents() {
        return cashAndEquivalents;
    }

    public void setCashAndEquivalents(BigDecimal cashAndEquivalents) {
        this.cashAndEquivalents = cashAndEquivalents;
    }

    public BigDecimal getGrossProfit() {
        return grossProfit;
    }

    public void setGrossProfit(BigDecimal grossProfit) {
        this.grossProfit = grossProfit;
    }

    public BigDecimal getSgAndA() {
        return sgAndA;
    }

    public void setSgAndA(BigDecimal sgAndA) {
        this.sgAndA = sgAndA;
    }

    public BigDecimal getEbit() {
        return ebit;
    }

    public void setEbit(BigDecimal ebit) {
        this.ebit = ebit;
    }

    public BigDecimal getTotalEquity() {
        return totalEquity;
    }

    public void setTotalEquity(BigDecimal totalEquity) {
        this.totalEquity = totalEquity;
    }

    public BigDecimal getInventory() {
        return inventory;
    }

    public void setInventory(BigDecimal inventory) {
        this.inventory = inventory;
    }

    public BigDecimal getAccountsReceivable() {
        return accountsReceivable;
    }

    public void setAccountsReceivable(BigDecimal accountsReceivable) {
        this.accountsReceivable = accountsReceivable;
    }

    public BigDecimal getFinancingCf() {
        return financingCf;
    }

    public void setFinancingCf(BigDecimal financingCf) {
        this.financingCf = financingCf;
    }

    public BigDecimal getInterestExpense() {
        return interestExpense;
    }

    public void setInterestExpense(BigDecimal interestExpense) {
        this.interestExpense = interestExpense;
    }

    public BigDecimal getDividendsPaid() {
        return dividendsPaid;
    }

    public void setDividendsPaid(BigDecimal dividendsPaid) {
        this.dividendsPaid = dividendsPaid;
    }
}