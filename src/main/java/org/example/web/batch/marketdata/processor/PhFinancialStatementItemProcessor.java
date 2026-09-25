package org.example.web.batch.marketdata.processor;

import java.util.Optional;

import org.example.web.batch.marketdata.client.MockFinancialDataProviderImpl;
import org.example.web.batch.marketdata.client.dto.FinancialStatementData;
import org.example.web.batch.marketdata.config.MarketDataApiProperties;
import org.example.web.dao.FinancialStatementDao;
import org.example.web.entity.CompanyEntity;
import org.example.web.entity.FinancialStatementEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

// ItemProcessor<CompanyEntity, FinancialStatementEntity>。フィリピン株向けの実データ取得元
// （FinancialDataProvider実装）は現状存在しないため、モックモード（marketdata.api.mock-enabled=true）の
// ときのみ MockFinancialDataProviderImpl のサンプルデータを返し、それ以外は常に null（対象外としてスキップ）を返す。
// UsFinancialStatementItemProcessor と同じ差分比較（isNewer）ロジックを流用する。
@Component
public class PhFinancialStatementItemProcessor implements ItemProcessor<CompanyEntity, FinancialStatementEntity> {

    private static final Logger log = LoggerFactory.getLogger(PhFinancialStatementItemProcessor.class);

    private final MockFinancialDataProviderImpl mockFinancialDataProvider;
    private final FinancialStatementDao financialStatementDao;
    private final MarketDataApiProperties apiProperties;

    public PhFinancialStatementItemProcessor(
            MockFinancialDataProviderImpl mockFinancialDataProvider,
            FinancialStatementDao financialStatementDao,
            MarketDataApiProperties apiProperties) {
        this.mockFinancialDataProvider = mockFinancialDataProvider;
        this.financialStatementDao = financialStatementDao;
        this.apiProperties = apiProperties;
    }

    @Override
    public FinancialStatementEntity process(CompanyEntity company) {
        if (!apiProperties.isMockEnabled()) {
            // フィリピン株の実データ取得元は未実装のため常にスキップする
            return null;
        }

        FinancialStatementData apiData = mockFinancialDataProvider.fetchLatestStatement(company.getCode());
        if (apiData == null || apiData.fiscalYear() == null || apiData.fiscalQuarter() == null
                || apiData.endDate() == null) {
            log.warn("財務諸表データが取得できませんでした（対象銘柄なし）: companyId={}, code={}",
                    company.getId(), company.getCode());
            return null;
        }

        Optional<FinancialStatementEntity> dbLatest = financialStatementDao.selectLatestByCompanyId(company.getId());
        if (dbLatest.isPresent() && !isNewer(apiData, dbLatest.get())) {
            return null;
        }

        return FinancialStatementEntityMapper.toEntity(company.getId(), apiData);
    }

    private static boolean isNewer(FinancialStatementData apiData, FinancialStatementEntity dbLatest) {
        if (!apiData.fiscalYear().equals(dbLatest.getFiscalYear())) {
            return apiData.fiscalYear() > dbLatest.getFiscalYear();
        }
        return apiData.fiscalQuarter().compareTo(dbLatest.getFiscalQuarter()) > 0;
    }
}
