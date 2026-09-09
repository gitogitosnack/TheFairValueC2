package org.example.web.batch.marketdata.writer;

import java.util.Optional;

import org.example.web.batch.marketdata.service.AnalysisIndicatorRecalcService;
import org.example.web.dao.FinancialStatementDao;
import org.example.web.entity.FinancialStatementEntity;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

// ItemWriter<FinancialStatementEntity>。chunk 単位で financial_statements へ INSERT し、
// INSERT した各行について AnalysisIndicatorRecalcService を呼び出し analysis_indicators を再計算・INSERT する。
// usFinancialStatementStep・jpFinancialStatementStep の両方で共用する（設計書 4.3.1・4.3.2・4.3.3 参照）。
@Component
public class FinancialStatementItemWriter implements ItemWriter<FinancialStatementEntity> {

    private final FinancialStatementDao financialStatementDao;
    private final AnalysisIndicatorRecalcService analysisIndicatorRecalcService;

    public FinancialStatementItemWriter(
            FinancialStatementDao financialStatementDao,
            AnalysisIndicatorRecalcService analysisIndicatorRecalcService) {
        this.financialStatementDao = financialStatementDao;
        this.analysisIndicatorRecalcService = analysisIndicatorRecalcService;
    }

    @Override
    public void write(Chunk<? extends FinancialStatementEntity> chunk) {
        for (FinancialStatementEntity entity : chunk) {
            // 再実行時に UNIQUE 制約 (company_id, fiscal_year, fiscal_quarter) に違反しないよう、
            // 既存行があれば UPDATE、なければ INSERT する。
            Optional<FinancialStatementEntity> existing = financialStatementDao.selectByCompanyIdAndFiscalPeriod(
                    entity.getCompanyId(), entity.getFiscalYear(), entity.getFiscalQuarter());
            if (existing.isPresent()) {
                entity.setId(existing.get().getId());
                financialStatementDao.update(entity);
            } else {
                financialStatementDao.insert(entity);
            }
            analysisIndicatorRecalcService.recalc(entity);
        }
    }
}
