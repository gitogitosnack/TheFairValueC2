package org.example.web.stock.stockList.domain;

public class StockListResponseDto {

    // Field
    private final Integer id;
    private final String code;
    private final String name;
    private final String market_name;
    private final String country_name;
    private final String industry_name;
    private final String currency_name;

    // Constructor
    public StockListResponseDto(
            Integer id
            ,String code
            ,String name
            ,String market_name
            ,String country_name
            ,String industry_name
            ,String currency_name
    ) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.market_name = market_name;
        this.country_name = country_name;
        this.industry_name = industry_name;
        this.currency_name = currency_name;
    }

    // Getter
    public Integer getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getMarket_name() {
        return market_name;
    }

    public String getCountry_name() {
        return country_name;
    }

    public String getIndustry_name() {
        return industry_name;
    }

    public String getCurrency_name() {
        return currency_name;
    }
}
