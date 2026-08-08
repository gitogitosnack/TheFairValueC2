package org.example.web.entity;

import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;
import org.seasar.doma.Column;
import org.seasar.doma.GeneratedValue;
import org.seasar.doma.GenerationType;

/**
 * 企業ごとに手動登録された競合企業のエンティティ。
 * A→B を登録すると B→A も1組として保存される（双方向）。
 */
@Entity(immutable = false)
@Table(name = "company_competitors")
public class CompanyCompetitorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "company_id")
    private Integer companyId;

    @Column(name = "competitor_company_id")
    private Integer competitorCompanyId;

    @Column(name = "delete_flg")
    private Integer deleteFlg;

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

    public Integer getCompetitorCompanyId() {
        return competitorCompanyId;
    }

    public void setCompetitorCompanyId(Integer competitorCompanyId) {
        this.competitorCompanyId = competitorCompanyId;
    }

    public Integer getDeleteFlg() {
        return deleteFlg;
    }

    public void setDeleteFlg(Integer deleteFlg) {
        this.deleteFlg = deleteFlg;
    }
}
