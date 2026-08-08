package org.example.web.stock.companyCompetitor.domain;

/** 競合追加時の検索候補1件（全企業） */
public class CompanyOptionDto {
    private final String code;
    private final String name;

    public CompanyOptionDto(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
