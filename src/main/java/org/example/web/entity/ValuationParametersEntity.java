package org.example.web.entity;

import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.GeneratedValue;
import org.seasar.doma.GenerationType;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "valuation_parameters")
public class ValuationParametersEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "valuation_model_id")
    private Integer valuationModelId;

    @Column(name = "parameter_code")
    private String parameterCode;

    @Column(name = "parameter_name")
    private String parameterName;

    @Column(name = "display_order")
    private Integer displayOrder;

    /** 単位。'PERCENT'（率）/ 'TIMES'（倍率）/ 'YEARS'（年数） */
    @Column(name = "unit")
    private String unit;

    /** スライダーの下限 */
    @Column(name = "min_value")
    private BigDecimal minValue;

    /** スライダーの上限 */
    @Column(name = "max_value")
    private BigDecimal maxValue;

    /** スライダーの刻み幅 */
    @Column(name = "step_value")
    private BigDecimal stepValue;

    /** 企業固有の既定値が無いときに使うモデル共通の既定値 */
    @Column(name = "default_value")
    private BigDecimal defaultValue;

    // Getters and Setters
    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getMinValue() {
        return minValue;
    }

    public void setMinValue(BigDecimal minValue) {
        this.minValue = minValue;
    }

    public BigDecimal getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(BigDecimal maxValue) {
        this.maxValue = maxValue;
    }

    public BigDecimal getStepValue() {
        return stepValue;
    }

    public void setStepValue(BigDecimal stepValue) {
        this.stepValue = stepValue;
    }

    public BigDecimal getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(BigDecimal defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getValuationModelId() {
        return valuationModelId;
    }

    public void setValuationModelId(Integer valuationModelId) {
        this.valuationModelId = valuationModelId;
    }

    public String getParameterCode() {
        return parameterCode;
    }

    public void setParameterCode(String parameterCode) {
        this.parameterCode = parameterCode;
    }

    public String getParameterName() {
        return parameterName;
    }

    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}