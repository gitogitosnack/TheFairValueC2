package org.example.web.entity;

import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.GeneratedValue;
import org.seasar.doma.GenerationType;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

@Entity(immutable = false)
@Table(name = "valuation_models")
public class ValuationModelEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    /** JS 側の計算メソッドを引き当てるための安定キー（例: DCF, PER_MULTIPLE） */
    @Column(name = "model_code")
    private String modelCode;

    @Column(name = "model_name")
    private String modelName;

    @Column(name = "formula_description")
    private String formulaDescription;

    /** モデルの特徴 */
    @Column(name = "characteristics")
    private String characteristics;

    /** どんな状況で使われる評価手法か */
    @Column(name = "use_case")
    private String useCase;

    /** メリット */
    @Column(name = "advantages")
    private String advantages;

    /** デメリット */
    @Column(name = "disadvantages")
    private String disadvantages;

    // Getter and Setter
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getModelCode() { return modelCode; }
    public void setModelCode(String modelCode) { this.modelCode = modelCode; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getFormulaDescription() { return formulaDescription; }
    public void setFormulaDescription(String formulaDescription) { this.formulaDescription = formulaDescription; }
    public String getCharacteristics() { return characteristics; }
    public void setCharacteristics(String characteristics) { this.characteristics = characteristics; }
    public String getUseCase() { return useCase; }
    public void setUseCase(String useCase) { this.useCase = useCase; }
    public String getAdvantages() { return advantages; }
    public void setAdvantages(String advantages) { this.advantages = advantages; }
    public String getDisadvantages() { return disadvantages; }
    public void setDisadvantages(String disadvantages) { this.disadvantages = disadvantages; }
}
