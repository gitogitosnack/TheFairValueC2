package org.example.web.stock.stockDetail.service;

import org.example.web.dao.AnalysisIndicatorDao;
import org.example.web.dao.CompanyDao;
import org.example.web.dao.CompanyValuationModelsDao;
import org.example.web.dao.CompanyValuationParameterDefaultsDao;
import org.example.web.dao.DailyQuoteDao;
import org.example.web.dao.FinancialStatementDao;
import org.example.web.dao.ValuationModelsDao;
import org.example.web.dao.ValuationParametersDao;
import org.example.web.entity.AnalysisIndicatorEntity;
import org.example.web.entity.CompanyEntity;
import org.example.web.entity.DailyQuoteEntity;
import org.example.web.entity.CompanyValuationModelsEntity;
import org.example.web.entity.CompanyValuationParameterDefaultsEntity;
import org.example.web.entity.FinancialStatementEntity;
import org.example.web.entity.ValuationModelEntity;
import org.example.web.entity.ValuationParametersEntity;
import org.example.web.stock.common.service.CIMapper;
import org.example.web.stock.stockDetail.domain.ParameterDefaultUpdateForm;
import org.example.web.stock.stockDetail.domain.StockAnalysisResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockDetailServiceImplTest {

    @Mock
    CIMapper ciMapper;
    @Mock
    FinancialStatementDao financialStatementDao;
    @Mock
    CompanyDao companyDao;
    @Mock
    DailyQuoteDao dailyQuoteDao;
    @Mock
    ValuationModelsDao valuationModelDao;
    @Mock
    AnalysisIndicatorDao analysisIndicatorDao;
    @Mock
    CompanyValuationParameterDefaultsDao companyValuationParameterDefaultsDao;
    @Mock
    CompanyValuationModelsDao companyValuationModelsDao;
    @Mock
    ValuationParametersDao valuationParameterDao;

    StockDetailServiceImpl service;

    @BeforeEach
    void setUp() {
        // analysisIndicatorDaoImpl はクラス内で未使用のフィールドのため null で構築する
        service = new StockDetailServiceImpl(null);
        service.ciMapper = ciMapper;
        service.financialStatementDao = financialStatementDao;
        service.companyDao = companyDao;
        service.dailyQuoteDao = dailyQuoteDao;
        service.valuationModelDao = valuationModelDao;
        service.analysisIndicatorDao = analysisIndicatorDao;
        service.companyValuationParameterDefaultsDao = companyValuationParameterDefaultsDao;
        service.companyValuationModelsDao = companyValuationModelsDao;
        service.valuationParameterDao = valuationParameterDao;
    }

    private CompanyEntity company() {
        CompanyEntity entity = new CompanyEntity();
        entity.setId(100);
        entity.setCode("7203");
        entity.setName("トヨタ自動車");
        return entity;
    }

    private DailyQuoteEntity dailyQuote(BigDecimal closePrice, Long sharesOutstanding) {
        DailyQuoteEntity entity = new DailyQuoteEntity();
        entity.setClosePrice(closePrice);
        entity.setSharesOutstanding(sharesOutstanding);
        return entity;
    }

    private AnalysisIndicatorEntity latestIndicator() {
        AnalysisIndicatorEntity entity = new AnalysisIndicatorEntity();
        entity.setFiscalYear(2025);
        entity.setFiscalQuarter("Q4");
        entity.setPer(BigDecimal.valueOf(15.2));
        entity.setPbr(BigDecimal.valueOf(1.1));
        entity.setDividendYield(BigDecimal.valueOf(2.5));
        entity.setEquityRatio(BigDecimal.valueOf(45.0));
        entity.setRoe(BigDecimal.valueOf(10.0));
        entity.setEps(BigDecimal.valueOf(120.0));
        entity.setPayoutRatio(BigDecimal.valueOf(30.0));
        return entity;
    }

    private void stubCompanyFound(Integer companyId, String code, CompanyEntity companyEntity) {
        when(ciMapper.convertCodeToId("companies", code)).thenReturn(companyId);
        when(companyDao.selectById(companyId)).thenReturn(Optional.of(companyEntity));
    }

    @Test
    void getComprehensiveAnalysis_主要指標と財務データを組み立てること() {
        CompanyEntity companyEntity = company();
        stubCompanyFound(100, "7203", companyEntity);
        when(dailyQuoteDao.selectLatestByCompanyId(100))
                .thenReturn(Optional.of(dailyQuote(BigDecimal.valueOf(2500), 1_000_000L)));

        AnalysisIndicatorEntity indicator = latestIndicator();
        when(analysisIndicatorDao.selectByCompanyId(100, 10)).thenReturn(List.of(indicator));
        when(analysisIndicatorDao.selectByCompanyId(100, 5)).thenReturn(List.of(indicator));
        when(analysisIndicatorDao.selectById(100, 2025, "Q4")).thenReturn(Optional.of(indicator));

        FinancialStatementEntity statement = new FinancialStatementEntity();
        statement.setFiscalYear(2025);
        statement.setRevenue(BigDecimal.valueOf(1000));
        statement.setTotalAssets(BigDecimal.valueOf(5000));
        statement.setTotalEquity(BigDecimal.valueOf(2000));
        statement.setTotalDebt(BigDecimal.valueOf(800));
        statement.setCashAndEquivalents(BigDecimal.valueOf(300));
        statement.setFreeCashFlow(BigDecimal.valueOf(150));
        statement.setEbit(BigDecimal.valueOf(200));
        when(financialStatementDao.selectLatestAnnualByCompanyId(100)).thenReturn(Optional.of(statement));

        when(companyValuationModelsDao.selectById(100)).thenReturn(List.of());

        StockAnalysisResponse response = service.getComprehensiveAnalysis("7203");

        assertThat(response.getKeyFinancialIndicatorDto().getCompanyCode()).isEqualTo("7203");
        assertThat(response.getKeyFinancialIndicatorDto().getCompanyName()).isEqualTo("トヨタ自動車");
        assertThat(response.getKeyFinancialIndicatorDto().getCurrentPrice()).isEqualTo(2500);
        assertThat(response.getKeyFinancialIndicatorDto().getPer()).isEqualByComparingTo("15.2");
        assertThat(response.getKeyFinancialIndicatorDto().getPbr()).isEqualByComparingTo("1.1");
        assertThat(response.getKeyFinancialIndicatorDto().getMainIndicatorYearLabel()).isEqualTo(2025);

        assertThat(response.getFinancialIndicatorDto().getFiscalYearLabels())
                .containsExactly("2021", "2022", "2023", "2024", "2025");
        assertThat(response.getFinancialIndicatorDto().getRoeList()).containsExactly(null, null, null, null, 10.0);

        // financial_statements は百万円単位のため 1,000,000 倍された値になること
        assertThat(response.getFinancialDataDto().getRevenue()).isEqualTo(1000d * 1_000_000);
        assertThat(response.getFinancialDataDto().getSharesOutstanding()).isEqualTo(1_000_000L);
        assertThat(response.getFinancialDataDto().getEps()).isEqualTo(120.0);
        // BPS はDBに無いため 現在株価 / PBR で逆算されること
        assertThat(response.getFinancialDataDto().getBps()).isEqualTo(2500.0 / 1.1);
        // 1株配当はDBに無いため 現在株価 * 配当利回り / 100 で逆算されること
        assertThat(response.getFinancialDataDto().getDividendPerShare()).isEqualTo(2500.0 * 2.5 / 100.0);

        assertThat(response.getTheoreticalPriceValuationDto().getModels()).isEmpty();
    }

    @Test
    void getComprehensiveAnalysis_企業が見つからないとき既定値のレスポンスを返すこと() {
        when(ciMapper.convertCodeToId("companies", "9999")).thenReturn(999);
        when(companyDao.selectById(999)).thenReturn(Optional.empty());
        when(analysisIndicatorDao.selectByCompanyId(999, 10)).thenReturn(List.of());
        when(analysisIndicatorDao.selectByCompanyId(999, 5)).thenReturn(List.of());

        StockAnalysisResponse response = service.getComprehensiveAnalysis("9999");

        assertThat(response.getKeyFinancialIndicatorDto().getCompanyCode()).isNull();
        assertThat(response.getTheoreticalPriceValuationDto().getModels()).isEmpty();
        assertThat(response.getFinancialDataDto().getSharesOutstanding()).isNull();
    }

    @Test
    void getComprehensiveAnalysis_パラメータ既定値は企業固有優先モデル共通ゼロの順で解決されること() {
        stubCompanyFound(100, "7203", company());
        when(analysisIndicatorDao.selectByCompanyId(100, 10)).thenReturn(List.of());
        when(analysisIndicatorDao.selectByCompanyId(100, 5)).thenReturn(List.of());

        CompanyValuationModelsEntity companyModel = new CompanyValuationModelsEntity();
        companyModel.setValuationModelId(1);
        when(companyValuationModelsDao.selectById(100)).thenReturn(List.of(companyModel));

        ValuationModelEntity modelMaster = new ValuationModelEntity();
        modelMaster.setModelName("DCF法");
        modelMaster.setModelCode("DCF");
        when(valuationModelDao.selectById(1)).thenReturn(Optional.of(modelMaster));

        ValuationParametersEntity paramWithCompanyOverride = new ValuationParametersEntity();
        paramWithCompanyOverride.setId(10);
        paramWithCompanyOverride.setParameterName("WACC");
        paramWithCompanyOverride.setDefaultValue(BigDecimal.valueOf(9.0));

        ValuationParametersEntity paramWithoutOverride = new ValuationParametersEntity();
        paramWithoutOverride.setId(11);
        paramWithoutOverride.setParameterName("成長率");
        paramWithoutOverride.setDefaultValue(BigDecimal.valueOf(5.0));

        ValuationParametersEntity paramWithNoDefaultAnywhere = new ValuationParametersEntity();
        paramWithNoDefaultAnywhere.setId(12);
        paramWithNoDefaultAnywhere.setParameterName("永久成長率");
        paramWithNoDefaultAnywhere.setDefaultValue(null);

        when(valuationParameterDao.selectById(1))
                .thenReturn(List.of(paramWithCompanyOverride, paramWithoutOverride, paramWithNoDefaultAnywhere));

        CompanyValuationParameterDefaultsEntity override = new CompanyValuationParameterDefaultsEntity();
        override.setDefaultValue(BigDecimal.valueOf(8.5));
        when(companyValuationParameterDefaultsDao.selectByCompanyIdAndParamId(100, 10)).thenReturn(override);
        when(companyValuationParameterDefaultsDao.selectByCompanyIdAndParamId(100, 11)).thenReturn(null);
        when(companyValuationParameterDefaultsDao.selectByCompanyIdAndParamId(100, 12)).thenReturn(null);

        StockAnalysisResponse response = service.getComprehensiveAnalysis("7203");

        List<org.example.web.stock.stockDetail.domain.ValuationParameterDto> params = response
                .getTheoreticalPriceValuationDto().getModels().get(0).getParameters();
        assertThat(params).hasSize(3);
        // 企業固有の既定値がある場合はそちらを優先すること
        assertThat(params.get(0).getDefaultValue()).isEqualByComparingTo("8.5");
        // 企業固有の既定値が無ければモデル共通の既定値を使うこと
        assertThat(params.get(1).getDefaultValue()).isEqualByComparingTo("5.0");
        // どちらも無ければ 0 を使うこと
        assertThat(params.get(2).getDefaultValue()).isEqualByComparingTo("0");
    }

    @Test
    void getComprehensiveAnalysis_シングルトンBeanのDTOが企業間で使い回されないこと() {
        // 1社目: モデルありのデータで呼び出す
        stubCompanyFound(100, "7203", company());
        when(analysisIndicatorDao.selectByCompanyId(100, 10)).thenReturn(List.of());
        when(analysisIndicatorDao.selectByCompanyId(100, 5)).thenReturn(List.of());
        CompanyValuationModelsEntity companyModel = new CompanyValuationModelsEntity();
        companyModel.setValuationModelId(1);
        when(companyValuationModelsDao.selectById(100)).thenReturn(List.of(companyModel));
        ValuationModelEntity modelMaster = new ValuationModelEntity();
        modelMaster.setModelName("DCF法");
        when(valuationModelDao.selectById(1)).thenReturn(Optional.of(modelMaster));
        when(valuationParameterDao.selectById(1)).thenReturn(List.of());

        StockAnalysisResponse firstResponse = service.getComprehensiveAnalysis("7203");
        assertThat(firstResponse.getTheoreticalPriceValuationDto().getModels()).hasSize(1);

        // 2社目: モデル未登録の企業を呼び出す
        CompanyEntity secondCompany = new CompanyEntity();
        secondCompany.setId(200);
        secondCompany.setCode("6758");
        secondCompany.setName("ソニーグループ");
        stubCompanyFound(200, "6758", secondCompany);
        when(analysisIndicatorDao.selectByCompanyId(200, 10)).thenReturn(List.of());
        when(analysisIndicatorDao.selectByCompanyId(200, 5)).thenReturn(List.of());
        when(companyValuationModelsDao.selectById(200)).thenReturn(List.of());

        StockAnalysisResponse secondResponse = service.getComprehensiveAnalysis("6758");

        // 2社目のレスポンスに1社目のモデルデータが残っていないこと
        assertThat(secondResponse.getTheoreticalPriceValuationDto().getModels()).isEmpty();
        assertThat(secondResponse.getKeyFinancialIndicatorDto().getCompanyCode()).isEqualTo("6758");
        // 1社目のレスポンスオブジェクト自体は書き換わっていないこと(別インスタンスであること)
        assertThat(firstResponse.getTheoreticalPriceValuationDto().getModels()).hasSize(1);
    }

    @Test
    void updateCompanyParameterDefaults_既存の既定値があるとき更新すること() {
        when(ciMapper.convertCodeToId("companies", "7203")).thenReturn(100);
        CompanyValuationParameterDefaultsEntity existing = new CompanyValuationParameterDefaultsEntity();
        when(companyValuationParameterDefaultsDao.selectByCompanyIdAndParamId(100, 10)).thenReturn(existing);
        ParameterDefaultUpdateForm form = new ParameterDefaultUpdateForm(
                List.of(new ParameterDefaultUpdateForm.ParameterDefaultItem(10, BigDecimal.valueOf(7.5))));

        service.updateCompanyParameterDefaults("7203", form);

        ArgumentCaptor<CompanyValuationParameterDefaultsEntity> captor = ArgumentCaptor
                .forClass(CompanyValuationParameterDefaultsEntity.class);
        verify(companyValuationParameterDefaultsDao).update(captor.capture());
        assertThat(captor.getValue().getDefaultValue()).isEqualByComparingTo("7.5");
        verify(companyValuationParameterDefaultsDao, never()).insert(any());
    }

    @Test
    void updateCompanyParameterDefaults_既存の既定値が無いとき新規登録すること() {
        when(ciMapper.convertCodeToId("companies", "7203")).thenReturn(100);
        when(companyValuationParameterDefaultsDao.selectByCompanyIdAndParamId(100, 10)).thenReturn(null);
        ParameterDefaultUpdateForm form = new ParameterDefaultUpdateForm(
                List.of(new ParameterDefaultUpdateForm.ParameterDefaultItem(10, BigDecimal.valueOf(7.5))));

        service.updateCompanyParameterDefaults("7203", form);

        verify(companyValuationParameterDefaultsDao).insert(any());
        verify(companyValuationParameterDefaultsDao, never()).update(any());
    }

    @Test
    void updateCompanyParameterDefaults_フォームがnullのとき何もしないこと() {
        service.updateCompanyParameterDefaults("7203", null);

        verify(companyValuationParameterDefaultsDao, never()).insert(any());
        verify(companyValuationParameterDefaultsDao, never()).update(any());
    }

    @Test
    void updateCompanyParameterDefaults_パラメータIDまたは値がnullの項目はスキップすること() {
        when(ciMapper.convertCodeToId("companies", "7203")).thenReturn(100);
        ParameterDefaultUpdateForm form = new ParameterDefaultUpdateForm(List.of(
                new ParameterDefaultUpdateForm.ParameterDefaultItem(null, BigDecimal.ONE),
                new ParameterDefaultUpdateForm.ParameterDefaultItem(10, null)));

        service.updateCompanyParameterDefaults("7203", form);

        verify(companyValuationParameterDefaultsDao, never()).insert(any());
        verify(companyValuationParameterDefaultsDao, never()).update(any());
    }
}
