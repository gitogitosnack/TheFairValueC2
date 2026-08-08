package org.example.web.stock.stockDetail.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.example.web.dao.AnalysisIndicatorDao;
import org.example.web.dao.AnalysisIndicatorDaoImpl;
import org.example.web.dao.CompanyDao;
import org.example.web.dao.CompanyValuationModelsDao;
import org.example.web.dao.CompanyValuationParameterDefaultsDao;
import org.example.web.dao.FinancialStatementDao;
import org.example.web.dao.ValuationModelsDao;
import org.example.web.dao.ValuationParametersDao;
import org.example.web.entity.AnalysisIndicatorEntity;
import org.example.web.entity.CompanyEntity;
import org.example.web.entity.CompanyValuationModelsEntity;
import org.example.web.entity.CompanyValuationParameterDefaultsEntity;
import org.example.web.entity.FinancialStatementEntity;
import org.example.web.entity.ValuationModelEntity;
import org.example.web.entity.ValuationParametersEntity;
import org.example.web.stock.common.service.CIMapper;
import org.example.web.stock.stockDetail.domain.FinancialDataDto;
import org.example.web.stock.stockDetail.domain.FinancialIndicatorDto;
import org.example.web.stock.stockDetail.domain.KeyFinancialIndicatorDto;
import org.example.web.stock.stockDetail.domain.ParameterDefaultUpdateForm;
import org.example.web.stock.stockDetail.domain.StockAnalysisResponse;
import org.example.web.stock.stockDetail.domain.TheoreticalPriceValuationDto;
import org.example.web.stock.stockDetail.domain.ValuationModelDto;
import org.example.web.stock.stockDetail.domain.ValuationParameterDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StockDetailServiceImpl implements StockDetailService {

    private final AnalysisIndicatorDaoImpl analysisIndicatorDaoImpl;
    // 本日の日付から「年」を取得して入れる
    private static final int CURRENT_YEAR = LocalDate.now().getYear();
    // private static final int CURRENT_YEAR = 2025;
    private static final String FISCAL_QUARTER_Q4 = "Q4";
    private static final int DEFAULT_DISPLAY_YEARS_COUNT = 5;
    // financial_statements の金額系カラムは百万円単位で格納されているため、円に変換する係数
    private static final BigDecimal MILLION = BigDecimal.valueOf(1_000_000);

    // initialize the TheoreticalPriceValuationDto
    TheoreticalPriceValuationDto theoreticalPriceValuationDto = new TheoreticalPriceValuationDto();

    // initialize the keyFinancialIndicatorDto
    KeyFinancialIndicatorDto keyFinancialIndicatorDto = new KeyFinancialIndicatorDto();

    // initialize the FinancialIndicatorDto
    FinancialIndicatorDto financialIndicatorDto = new FinancialIndicatorDto();

    // initialize the FinancialDataDto (理論株価の算出に使う財務データ)
    FinancialDataDto financialDataDto = new FinancialDataDto();

    @Autowired
    CIMapper ciMapper;

    @Autowired
    FinancialStatementDao financialStatementDao;

    @Autowired
    CompanyDao companyDao;

    @Autowired
    ValuationModelsDao valuationModelDao;

    @Autowired
    AnalysisIndicatorDao analysisIndicatorDao;

    @Autowired
    CompanyValuationParameterDefaultsDao companyValuationParameterDefaultsDao;

    @Autowired
    CompanyValuationModelsDao companyValuationModelsDao;

    @Autowired
    ValuationParametersDao valuationParameterDao;

    StockDetailServiceImpl(AnalysisIndicatorDaoImpl analysisIndicatorDaoImpl) {
        this.analysisIndicatorDaoImpl = analysisIndicatorDaoImpl;
    }

    // ====================================================
    // --- 0. main process for getting company details indicators ---
    // ====================================================

    public StockAnalysisResponse getComprehensiveAnalysis(String code) {

        // このサービスは Spring のシングルトンBeanであり、DTOをフィールドとして
        // 使い回しているため、リクエストごとに必ず作り直して前回のリクエスト
        // （別企業）のデータが残らないようにする。
        theoreticalPriceValuationDto = new TheoreticalPriceValuationDto();
        keyFinancialIndicatorDto = new KeyFinancialIndicatorDto();
        financialIndicatorDto = new FinancialIndicatorDto();
        financialDataDto = new FinancialDataDto();

        // Get data in the upper side of the detail display.

        // どのテーブルのIDが欲しいかに合わせて、テーブル名を固定値で渡す
        Integer companyId = ciMapper.convertCodeToId("companies", code);

        // Get the company code and name
        // Get the current_price
        this.getCompanyCodeAndName(companyId);

        // Get the parameters for each model and their default values from the
        // company_valuation_parameter_defaults table.
        this.getDefaultModelsAndParamValues(companyId);

        // Get the per, pbr, dividend_yield, equity_ratio
        this.getMarketIndicators(companyId);

        // Get data in the lower side of the detail display called
        // FinancialIndicatorDto.
        // Get each analysis indicators for 5 years
        this.getAnalysisIndicators(companyId);

        // Get the financial data used by the front-end pv_calculator to
        // compute the theoretical fair price for each valuation model.
        this.getFinancialDataForValuation(companyId);

        // Set data1 and data2 to the StockAnalysisResponse class.
        StockAnalysisResponse response = new StockAnalysisResponse();
        response.setTheoreticalPriceValuationDto(theoreticalPriceValuationDto);
        response.setKeyFinancialIndicatorDto(keyFinancialIndicatorDto);
        response.setFinancialIndicatorDto(financialIndicatorDto);
        response.setFinancialDataDto(financialDataDto);
        return response;

    }

    // 画面で操作したスライダーの値を、その銘柄のパラメータ初期値としてDBに保存する。
    // 既に企業固有の既定値が存在するパラメータは更新、存在しないパラメータは新規登録する。
    @Override
    public void updateCompanyParameterDefaults(String code, ParameterDefaultUpdateForm form) {
        Integer companyId = ciMapper.convertCodeToId("companies", code);

        if (form == null || form.parameters() == null) {
            return;
        }

        for (ParameterDefaultUpdateForm.ParameterDefaultItem item : form.parameters()) {
            if (item.parameterId() == null || item.value() == null) {
                continue;
            }

            CompanyValuationParameterDefaultsEntity existing = companyValuationParameterDefaultsDao
                    .selectByCompanyIdAndParamId(companyId, item.parameterId());

            CompanyValuationParameterDefaultsEntity entity = new CompanyValuationParameterDefaultsEntity();
            entity.setCompanyId(companyId);
            entity.setValuationParameterId(item.parameterId());
            entity.setDefaultValue(item.value());
            entity.setLastUpdated(LocalDateTime.now());

            if (existing != null) {
                companyValuationParameterDefaultsDao.update(entity);
            } else {
                companyValuationParameterDefaultsDao.insert(entity);
            }
        }
    }

    // ====================================================
    // --- 1. Upper Side Data (KeyFinancialIndicatorDto) ---
    // ====================================================

    // This function gets the company code and name.
    // Get the current_price
    void getCompanyCodeAndName(Integer id) {
        Optional<CompanyEntity> company = companyDao.selectById(id);
        if (company.isPresent()) {
            // Set the company code to dto
            keyFinancialIndicatorDto.setCompanyCode(company.get().getCode());
            // Set the company name to dto
            keyFinancialIndicatorDto.setCompanyName(company.get().getName());
            // Set the current price to dto
            keyFinancialIndicatorDto.setCurrentPrice(company.get().getCurrentPrice());
        }
    }

    // Get the parameters to display in the company detail page.
    void getDefaultModelsAndParamValues(Integer id) {
        // get company id from company master table.
        Optional<CompanyEntity> company = companyDao.selectById(id);
        // 企業が存在しない場合 ➔ 空リストをセットして終了
        if (company.isEmpty()) {
            theoreticalPriceValuationDto.setModels(Collections.emptyList());
            return;
        }
        if (company.isPresent()) {
            // get models related to the company from company_valuation_models table.
            List<CompanyValuationModelsEntity> models = companyValuationModelsDao.selectById(id);
            // DBからnullまたは空リストが返ってきた場合 ➔ 空リストをセットして終了
            if (models == null || models.isEmpty()) {
                theoreticalPriceValuationDto.setModels(Collections.emptyList());
                return;
            }
            // prepare the valuation model DTOs to send to the front-end.
            List<ValuationModelDto> valuationModelDtoList = new ArrayList<>();

            for (CompanyValuationModelsEntity model : models) {
                // prepare the valuation model DTO for each model and add it to the list.
                ValuationModelDto valuationModelDto = new ValuationModelDto();
                valuationModelDto.setModelId(model.getValuationModelId());

                // get the model name and description texts from the valuation_model master
                // table using the model id.
                Optional<ValuationModelEntity> modelMaster = valuationModelDao.selectById(model.getValuationModelId());
                if (modelMaster.isPresent()) {
                    ValuationModelEntity master = modelMaster.get();
                    valuationModelDto.setModelName(master.getModelName());
                    valuationModelDto.setModelCode(master.getModelCode());
                    valuationModelDto.setFormulaDescription(master.getFormulaDescription());
                    valuationModelDto.setCharacteristics(master.getCharacteristics());
                    valuationModelDto.setUseCase(master.getUseCase());
                    valuationModelDto.setAdvantages(master.getAdvantages());
                    valuationModelDto.setDisadvantages(master.getDisadvantages());
                } else {
                    valuationModelDto.setModelName("Unknown Model Name");
                }
                // get the default parameters making up the model.
                List<ValuationParametersEntity> params = valuationParameterDao
                        .selectById(model.getValuationModelId());

                // prepare the param DTOs to send to the front-end.
                List<ValuationParameterDto> paramDtoList = new ArrayList<>();
                if (params != null && !params.isEmpty()) {
                    for (ValuationParametersEntity param : params) {
                        // Process each default parameter
                        ValuationParameterDto paramDto = new ValuationParameterDto();
                        paramDto.setParameterId(param.getId());
                        paramDto.setParameterName(param.getParameterName());
                        paramDto.setParameterCode(param.getParameterCode());
                        paramDto.setDisplayOrder(param.getDisplayOrder());
                        paramDto.setUnit(param.getUnit());
                        paramDto.setMinValue(param.getMinValue());
                        paramDto.setMaxValue(param.getMaxValue());
                        paramDto.setStepValue(param.getStepValue());
                        // get the default value of the param.
                        // 企業固有の既定値 → モデル共通の既定値 → 0 の優先順位で解決する。
                        CompanyValuationParameterDefaultsEntity paramDefault = companyValuationParameterDefaultsDao
                                .selectByCompanyIdAndParamId(id, param.getId());
                        if (paramDefault != null && paramDefault.getDefaultValue() != null) {
                            paramDto.setDefaultValue(paramDefault.getDefaultValue());
                        } else if (param.getDefaultValue() != null) {
                            paramDto.setDefaultValue(param.getDefaultValue());
                        } else {
                            paramDto.setDefaultValue(BigDecimal.ZERO);
                        }
                        paramDtoList.add(paramDto);
                    }
                }
                valuationModelDto.setParameters(paramDtoList);

                valuationModelDtoList.add(valuationModelDto);
            }
            // Set the valuation model DTO list to the keyFinancialIndicatorDto
            theoreticalPriceValuationDto.setModels(valuationModelDtoList);
        }
    }

    // Get the per, pbr, dividend_yield, equity_ratio

    void getMarketIndicators(Integer id) {
        // Get the latest fiscal year that has Q4 data in DB
        int row_count = 10;
        List<AnalysisIndicatorEntity> indicators = analysisIndicatorDao.selectByCompanyId(id, row_count);

        // Find the latest fiscal year with Q4 data
        Integer latestYear = indicators.stream()
                .filter(indicator -> FISCAL_QUARTER_Q4.equals(indicator.getFiscalQuarter()))
                .map(AnalysisIndicatorEntity::getFiscalYear)
                .max(Integer::compareTo)
                .orElse(CURRENT_YEAR);

        // Get market indicators for the latest Q4 year
        Optional<AnalysisIndicatorEntity> indicator = analysisIndicatorDao.selectById(id, latestYear,
                FISCAL_QUARTER_Q4);
        if (indicator.isPresent()) {
            keyFinancialIndicatorDto.setPer(indicator.get().getPer());
            keyFinancialIndicatorDto.setPbr(indicator.get().getPbr());
            keyFinancialIndicatorDto.setDividendYield(indicator.get().getDividendYield());
            keyFinancialIndicatorDto.setEquityRatio(indicator.get().getEquityRatio());
            // Set the fiscal year for reference
            keyFinancialIndicatorDto.setMainIndicatorYearLabel(latestYear);
        }
    }

    // ====================================================
    // --- 2. Lower Side Data (FinancialIndicatorDto - 5 Years History) ---
    // ====================================================
    // Get the analysis indicators for 5 years to use those data in lower tables.
    void getAnalysisIndicators(Integer id) {
        // Data fetch from DAO.
        List<AnalysisIndicatorEntity> indicators = analysisIndicatorDao.selectByCompanyId(id,
                DEFAULT_DISPLAY_YEARS_COUNT);

        // Set label names
        this.setupLabels(financialIndicatorDto);

        // make the map with key as fiscal year and value as the entity for easy access
        // when filling the lists for each indicator.
        Map<Integer, AnalysisIndicatorEntity> dataMap = new HashMap<>();
        for (AnalysisIndicatorEntity entity : indicators) {
            Integer year = entity.getFiscalYear();

            if (!dataMap.containsKey(year)) {
                dataMap.put(year, entity);
            }
        }

        // Determine the latest year in data; fallback to CURRENT_YEAR
        int latestYear = indicators.stream()
                .map(AnalysisIndicatorEntity::getFiscalYear)
                .max(Integer::compareTo)
                .orElse(CURRENT_YEAR);

        // For missing latest 5-year, use whatever the latest available year is.
        int endYear = latestYear;
        int startYear = endYear - (DEFAULT_DISPLAY_YEARS_COUNT - 1);

        List<String> fiscalYearLabels = new ArrayList<>();
        List<Double> roeList = new ArrayList<>();
        List<Double> grossMarginList = new ArrayList<>();
        List<Double> sgaRatioList = new ArrayList<>();
        List<Double> netMarginList = new ArrayList<>();
        List<Double> roaList = new ArrayList<>();
        List<Double> epsList = new ArrayList<>();
        List<Double> assetTurnoverList = new ArrayList<>();
        List<Double> inventoryTurnoverList = new ArrayList<>();
        List<Double> receivablesTurnoverList = new ArrayList<>();
        List<Double> equityRatioList = new ArrayList<>();
        List<Double> debtEquityRatioList = new ArrayList<>();
        List<Double> debtRatioList = new ArrayList<>();
        List<Double> interestCoverageRatioList = new ArrayList<>();
        List<Double> operationCfMarginList = new ArrayList<>();
        List<Double> fcfList = new ArrayList<>();
        List<Double> finCfList = new ArrayList<>();

        for (int year = startYear; year <= endYear; year++) {
            fiscalYearLabels.add(String.valueOf(year));
            AnalysisIndicatorEntity entity = dataMap.get(year);

            if (entity != null) {
                roeList.add(this.toDouble(entity.getRoe()));
                grossMarginList.add(this.toDouble(entity.getGrossMargin()));
                sgaRatioList.add(this.toDouble(entity.getSgaRatio()));
                netMarginList.add(this.toDouble(entity.getNetMargin()));
                roaList.add(this.toDouble(entity.getRoa()));
                epsList.add(this.toDouble(entity.getEps()));
                assetTurnoverList.add(this.toDouble(entity.getAssetTurnover()));
                inventoryTurnoverList.add(this.toDouble(entity.getInventoryTurnover()));
                receivablesTurnoverList.add(this.toDouble(entity.getArTurnover()));
                equityRatioList.add(this.toDouble(entity.getEquityRatio()));
                debtEquityRatioList.add(this.toDouble(entity.getDeRatio()));
                debtRatioList.add(this.toDouble(entity.getDebtRatio()));
                interestCoverageRatioList.add(this.toDouble(entity.getInterestCoverage()));
                operationCfMarginList.add(this.toDouble(entity.getOpCfMargin()));
                fcfList.add(this.toDouble(entity.getFcf()));
                finCfList.add(this.toDouble(entity.getFinCf()));
            } else {
                roeList.add(null);
                grossMarginList.add(null);
                sgaRatioList.add(null);
                netMarginList.add(null);
                roaList.add(null);
                epsList.add(null);
                assetTurnoverList.add(null);
                inventoryTurnoverList.add(null);
                receivablesTurnoverList.add(null);
                equityRatioList.add(null);
                debtEquityRatioList.add(null);
                debtRatioList.add(null);
                interestCoverageRatioList.add(null);
                operationCfMarginList.add(null);
                fcfList.add(null);
                finCfList.add(null);
            }
        }

        financialIndicatorDto.setFiscalYearLabels(fiscalYearLabels);
        financialIndicatorDto.setRoeList(roeList);
        financialIndicatorDto.setGrossMarginList(grossMarginList);
        financialIndicatorDto.setSgaRatioList(sgaRatioList);
        financialIndicatorDto.setNetMarginList(netMarginList);
        financialIndicatorDto.setRoaList(roaList);
        financialIndicatorDto.setEpsList(epsList);
        financialIndicatorDto.setAssetTurnoverList(assetTurnoverList);
        financialIndicatorDto.setInventoryTurnoverList(inventoryTurnoverList);
        financialIndicatorDto.setReceivablesTurnoverList(receivablesTurnoverList);
        financialIndicatorDto.setEquityRatioList(equityRatioList);
        financialIndicatorDto.setDebtEquityRatioList(debtEquityRatioList);
        financialIndicatorDto.setDebtRatioList(debtRatioList);
        financialIndicatorDto.setInterestCoverageRatioList(interestCoverageRatioList);
        financialIndicatorDto.setOperationCfMarginList(operationCfMarginList);
        financialIndicatorDto.setFcfList(fcfList);
        financialIndicatorDto.setFinCfList(finCfList);
    } // end of this method

    // ====================================================
    // --- 3. Financial Data for Theoretical Price Calculation (FinancialDataDto) ---
    // ====================================================
    // 理論株価の算出に使う財務データを組み立てる。
    // financial_statements（最新通期）と analysis_indicators（最新Q4）の値を組み合わせ、
    // 一方に無い項目はもう一方から補完・派生（BPS・1株配当など）する。
    void getFinancialDataForValuation(Integer id) {
        Optional<CompanyEntity> company = companyDao.selectById(id);
        if (company.isEmpty()) {
            return;
        }
        CompanyEntity companyEntity = company.get();
        financialDataDto.setSharesOutstanding(companyEntity.getOutstandingShares());
        financialDataDto.setCurrentPrice(
                companyEntity.getCurrentPrice() != null ? companyEntity.getCurrentPrice().doubleValue() : null);

        // financial_statements（最新通期）から企業規模を表す絶対額の項目を取得する。
        Optional<FinancialStatementEntity> statement = financialStatementDao.selectLatestAnnualByCompanyId(id);
        statement.ifPresent(fs -> {
            financialDataDto.setFiscalYear(fs.getFiscalYear());
            financialDataDto.setRevenue(this.toYen(fs.getRevenue()));
            financialDataDto.setTotalAssets(this.toYen(fs.getTotalAssets()));
            financialDataDto.setTotalEquity(this.toYen(fs.getTotalEquity()));
            financialDataDto.setInterestBearingDebt(this.toYen(fs.getTotalDebt()));
            financialDataDto.setCashAndEquivalents(this.toYen(fs.getCashAndEquivalents()));
            financialDataDto.setFcf(this.toYen(fs.getFreeCashFlow()));
            // 減価償却費がDBに無いため EBIT（無ければ営業利益）で EBITDA を代用する。
            BigDecimal ebit = fs.getEbit() != null ? fs.getEbit() : fs.getOperatingIncome();
            financialDataDto.setEbitda(this.toYen(ebit));
        });

        // analysis_indicators（最新Q4）から1株あたり項目・率の項目を取得する。
        int row_count = 10;
        List<AnalysisIndicatorEntity> indicators = analysisIndicatorDao.selectByCompanyId(id, row_count);
        Integer latestYear = indicators.stream()
                .filter(indicator -> FISCAL_QUARTER_Q4.equals(indicator.getFiscalQuarter()))
                .map(AnalysisIndicatorEntity::getFiscalYear)
                .max(Integer::compareTo)
                .orElse(null);
        if (latestYear != null) {
            Optional<AnalysisIndicatorEntity> indicator = analysisIndicatorDao.selectById(id, latestYear,
                    FISCAL_QUARTER_Q4);
            indicator.ifPresent(ind -> {
                if (financialDataDto.getFiscalYear() == null) {
                    financialDataDto.setFiscalYear(latestYear);
                }
                financialDataDto.setEps(this.toDouble(ind.getEps()));
                financialDataDto.setRoe(this.toDouble(ind.getRoe()));
                financialDataDto.setPayoutRatio(this.toDouble(ind.getPayoutRatio()));
                // financial_statements に FCF が無ければ analysis_indicators の値で補完する。
                if (financialDataDto.getFcf() == null) {
                    financialDataDto.setFcf(this.toYen(ind.getFcf()));
                }
                // BPS はDBに直接の列が無いため、現在株価とPBRから逆算する。
                if (financialDataDto.getCurrentPrice() != null && ind.getPbr() != null
                        && ind.getPbr().compareTo(BigDecimal.ZERO) != 0) {
                    financialDataDto.setBps(financialDataDto.getCurrentPrice() / ind.getPbr().doubleValue());
                }
                // 1株配当はDBに直接の列が無いため、現在株価と配当利回りから逆算する。
                if (financialDataDto.getCurrentPrice() != null && ind.getDividendYield() != null) {
                    financialDataDto.setDividendPerShare(
                            financialDataDto.getCurrentPrice() * ind.getDividendYield().doubleValue() / 100.0);
                }
            });
        }
    }

    // 百万円単位のDB値を円に変換する補助メソッド
    private Double toYen(BigDecimal millionYenValue) {
        return millionYenValue == null ? null : millionYenValue.multiply(MILLION).doubleValue();
    }

    // function to set the label name for each table and their rows.
    private void setupLabels(FinancialIndicatorDto dto) {
        // dto.setTableTitle("財務分析指標（5期推移）");
        // 収益・成長性タブ（①〜⑥）
        dto.setRoeLabel("① ROE (%)");
        dto.setGrossMarginLabel("② 売上高総利益率 (%)");
        dto.setSgaRatioLabel("③ 売上高販管費率 (%)");
        dto.setNetMarginLabel("④ 売上高純利益率 (%)");
        dto.setRoaLabel("⑤ ROA (%)");
        dto.setEpsLabel("⑥ EPS (円)");
        // 効率性タブ（⑦〜⑨）
        dto.setAssetTurnoverLabel("⑦ 総資産回転率 (回)");
        dto.setInventoryTurnoverLabel("⑧ 棚卸資産回転率 (回)");
        dto.setReceivablesTurnoverLabel("⑨ 売上債権回転率 (回)");
        // 安全性タブ（⑩〜⑬）
        dto.setEquityRatioLabel("⑩ 自己資本比率 (%)");
        dto.setDebtEquityRatioLabel("⑪ D/Eレシオ (倍)");
        dto.setDebtRatioLabel("⑫ 負債比率 (%)");
        dto.setInterestCoverageRatioLabel("⑬ インタレスト・カバレッジ・レシオ (倍)");
        // キャッシュフロータブ（⑭〜⑯）
        dto.setOperationCfMarginLabel("⑭ 営業CFマージン (%)");
        dto.setFcfLabel("⑮ フリーキャッシュフロー (百万円)");
        dto.setFinCfLabel("⑯ 財務キャッシュフロー (百万円)");
    }

    // BigDecimalをDoubleに安全に変換する補助メソッド
    private Double toDouble(BigDecimal val) {
        return val == null ? null : val.doubleValue();
    }

} // end of this class
