package org.example.web.stock.stockDetail.domain;

public class StockAnalysisResponse {

    private TheoreticalPriceValuationDto theoreticalPriceValuationDto;
    private KeyFinancialIndicatorDto keyFinancialIndicatorDto;
    private FinancialIndicatorDto financialIndicatorDto;
    /** 理論株価の算出に使う財務データ（フロントの pv_calculator に渡す） */
    private FinancialDataDto financialDataDto;

    // Getter and Setter
    public FinancialDataDto getFinancialDataDto() {
        return financialDataDto;
    }

    public void setFinancialDataDto(FinancialDataDto financialDataDto) {
        this.financialDataDto = financialDataDto;
    }

    public TheoreticalPriceValuationDto getTheoreticalPriceValuationDto() {
        return theoreticalPriceValuationDto;
    }

    public void setTheoreticalPriceValuationDto(TheoreticalPriceValuationDto theoreticalPriceValuationDto) {
        this.theoreticalPriceValuationDto = theoreticalPriceValuationDto;
    }

    public KeyFinancialIndicatorDto getKeyFinancialIndicatorDto() {
        return keyFinancialIndicatorDto;
    }

    public void setKeyFinancialIndicatorDto(KeyFinancialIndicatorDto keyFinancialIndicatorDto) {
        this.keyFinancialIndicatorDto = keyFinancialIndicatorDto;
    }

    public FinancialIndicatorDto getFinancialIndicatorDto() {
        return financialIndicatorDto;
    }

    public void setFinancialIndicatorDto(FinancialIndicatorDto financialIndicatorDto) {
        this.financialIndicatorDto = financialIndicatorDto;
    }
}
