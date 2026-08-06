package org.example.web.stock.stockDetail.domain;

import java.math.BigDecimal;

public class ValuationParameterDto {
    private Integer parameterId;
    private String parameterCode;
    private String parameterName; // 例: "WACC", "加重平均資本コスト"
    private Integer displayOrder;
    /** 単位。'PERCENT'（率）/ 'TIMES'（倍率）/ 'YEARS'（年数） */
    private String unit;
    /** スライダーの下限 */
    private BigDecimal minValue;
    /** スライダーの上限 */
    private BigDecimal maxValue;
    /** スライダーの刻み幅 */
    private BigDecimal stepValue;
    /** 画面の初期表示値。企業固有の既定値 → モデル共通の既定値 の順で解決した結果 */
    private BigDecimal defaultValue;

    // getter and setter
    public Integer getParameterId() {
        return parameterId;
    }

    public void setParameterId(Integer parameterId) {
        this.parameterId = parameterId;
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
}
