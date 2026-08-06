package org.example.web.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

@Entity
@Table(name = "company_valuation_parameter_defaults")
public class CompanyValuationParameterDefaultsEntity {

    @Id
    @Column(name = "company_id")
    private Integer companyId;

    @Id
    @Column(name = "valuation_parameter_id")
    private Integer valuationParameterId;

    @Column(name = "default_value")
    private BigDecimal defaultValue;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    // Setters and Getters
    public Integer getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Integer companyId) {
        this.companyId = companyId;
    }

    public Integer getValuationParameterId() {
        return valuationParameterId;
    }

    public void setValuationParameterId(Integer valuationParameterId) {
        this.valuationParameterId = valuationParameterId;
    }

    public BigDecimal getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(BigDecimal defaultValue) {
        this.defaultValue = defaultValue;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

}