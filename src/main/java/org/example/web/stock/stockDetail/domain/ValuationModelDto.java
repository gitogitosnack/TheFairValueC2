package org.example.web.stock.stockDetail.domain;

import java.util.ArrayList;
import java.util.List;

public class ValuationModelDto {
    private Integer modelId;
    /** JS 側で計算メソッドを引き当てるための安定キー（例: "DCF", "PER_MULTIPLE"） */
    private String modelCode;
    private String modelName; // 例: "DCF法"
    /** 計算式の要約 */
    private String formulaDescription;
    /** モデルの特徴 */
    private String characteristics;
    /** どんな状況で使われる評価手法か */
    private String useCase;
    /** メリット */
    private String advantages;
    /** デメリット */
    private String disadvantages;
    private List<ValuationParameterDto> parameters = new ArrayList<>();

    // getters and setters
    public Integer getModelId() {
        return modelId;
    }

    public void setModelId(Integer modelId) {
        this.modelId = modelId;
    }

    public String getModelCode() {
        return modelCode;
    }

    public void setModelCode(String modelCode) {
        this.modelCode = modelCode;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getFormulaDescription() {
        return formulaDescription;
    }

    public void setFormulaDescription(String formulaDescription) {
        this.formulaDescription = formulaDescription;
    }

    public String getCharacteristics() {
        return characteristics;
    }

    public void setCharacteristics(String characteristics) {
        this.characteristics = characteristics;
    }

    public String getUseCase() {
        return useCase;
    }

    public void setUseCase(String useCase) {
        this.useCase = useCase;
    }

    public String getAdvantages() {
        return advantages;
    }

    public void setAdvantages(String advantages) {
        this.advantages = advantages;
    }

    public String getDisadvantages() {
        return disadvantages;
    }

    public void setDisadvantages(String disadvantages) {
        this.disadvantages = disadvantages;
    }

    public List<ValuationParameterDto> getParameters() {
        return parameters;
    }

    public void setParameters(List<ValuationParameterDto> parameters) {
        this.parameters = parameters;
    }

}
