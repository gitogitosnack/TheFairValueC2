package org.example.web.stock.companyCompetitor.domain;

/** 登録済み競合企業1件（比較モーダルのチップ表示用） */
public class CompetitorResponseDto {
    private final Integer id;
    private final String competitorCode;
    private final String competitorName;

    public CompetitorResponseDto(Integer id, String competitorCode, String competitorName) {
        this.id = id;
        this.competitorCode = competitorCode;
        this.competitorName = competitorName;
    }

    public Integer getId() {
        return id;
    }

    public String getCompetitorCode() {
        return competitorCode;
    }

    public String getCompetitorName() {
        return competitorName;
    }
}
