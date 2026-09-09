package org.example.web.batch.marketdata.processor;

import java.util.Optional;

import org.example.web.batch.marketdata.client.FmpFinancialDataProviderImpl;
import org.example.web.batch.marketdata.client.RateLimitException;
import org.example.web.batch.marketdata.client.dto.FinancialStatementData;
import org.example.web.dao.FinancialStatementDao;
import org.example.web.entity.CompanyEntity;
import org.example.web.entity.FinancialStatementEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

// ItemProcessor<CompanyEntity, FinancialStatementEntity>。financial_statements の最新
// (fiscal_year, fiscal_quarter) と FmpFinancialDataProviderImpl.fetchLatestStatement(code) の結果を比較し、
// API 側が新しければ FinancialStatementEntity を返す。差分がなければ null を返し chunk から除外する（設計書 4.3.1 参照）。
@Component
public class UsFinancialStatementItemProcessor implements ItemProcessor<CompanyEntity, FinancialStatementEntity> {

    private static final Logger log = LoggerFactory.getLogger(UsFinancialStatementItemProcessor.class);

    private final FmpFinancialDataProviderImpl fmpFinancialDataProvider;
    private final FinancialStatementDao financialStatementDao;

    public UsFinancialStatementItemProcessor(
            FmpFinancialDataProviderImpl fmpFinancialDataProvider,
            FinancialStatementDao financialStatementDao) {
        this.fmpFinancialDataProvider = fmpFinancialDataProvider;
        this.financialStatementDao = financialStatementDao;
    }

    @Override
    public FinancialStatementEntity process(CompanyEntity company) {
        FinancialStatementData apiData;
        try {
            apiData = fmpFinancialDataProvider.fetchLatestStatement(company.getCode());
        } catch (RateLimitException e) {
            throw e;
        } catch (Exception e) {
            log.warn("財務諸表の取得に失敗しました。次回バッチで再取得します: companyId={}, code={}",
                    company.getId(), company.getCode(), e);
            return null;
        }
        if (apiData == null || apiData.fiscalYear() == null || apiData.fiscalQuarter() == null
                || apiData.endDate() == null) {
            log.warn("財務諸表データが取得できませんでした（対象銘柄なし）: companyId={}, code={}",
                    company.getId(), company.getCode());
            return null;
        }

        Optional<FinancialStatementEntity> dbLatest = financialStatementDao.selectLatestByCompanyId(company.getId());
        if (dbLatest.isPresent() && !isNewer(apiData, dbLatest.get())) {
            // DB より新しい四半期がなければ何もしない（冪等要件、設計書 4.3.1 参照）
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
