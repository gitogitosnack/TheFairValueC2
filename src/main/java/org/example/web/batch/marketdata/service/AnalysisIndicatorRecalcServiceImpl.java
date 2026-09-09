package org.example.web.batch.marketdata.service;

import java.math.BigDecimal;
import java.util.Optional;

import org.example.web.dao.AnalysisIndicatorDao;
import org.example.web.dao.DailyQuoteDao;
import org.example.web.entity.AnalysisIndicatorEntity;
import org.example.web.entity.DailyQuoteEntity;
import org.example.web.entity.FinancialStatementEntity;
import org.example.web.stock.valuationmodel.service.FinancialIndicatorCalculationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

// AnalysisIndicatorRecalcService の実装クラス。
// FinancialStatementItemWriter から財務データ更新をトリガに呼び出され、評価モデル機能側のロジックを
// 呼び出して結果を analysis_indicators へ INSERT する（設計書 4.3.3 参照）。
@Service
public class AnalysisIndicatorRecalcServiceImpl implements AnalysisIndicatorRecalcService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisIndicatorRecalcServiceImpl.class);

    private final FinancialIndicatorCalculationService calculationService;
    private final DailyQuoteDao dailyQuoteDao;
    private final AnalysisIndicatorDao analysisIndicatorDao;

    public AnalysisIndicatorRecalcServiceImpl(
            FinancialIndicatorCalculationService calculationService,
            DailyQuoteDao dailyQuoteDao,
            AnalysisIndicatorDao analysisIndicatorDao) {
        this.calculationService = calculationService;
        this.dailyQuoteDao = dailyQuoteDao;
        this.analysisIndicatorDao = analysisIndicatorDao;
    }

    @Override
    public void recalc(FinancialStatementEntity statement) {
        Optional<DailyQuoteEntity> latestQuote = dailyQuoteDao.selectLatestByCompanyId(statement.getCompanyId());
        BigDecimal currentPrice = latestQuote.map(DailyQuoteEntity::getClosePrice).orElse(null);
        Long sharesOutstanding = latestQuote.map(DailyQuoteEntity::getSharesOutstanding).orElse(null);

        AnalysisIndicatorEntity indicator = calculationService.calculate(statement, currentPrice, sharesOutstanding);

        Optional<AnalysisIndicatorEntity> existing = analysisIndicatorDao.selectById(
                statement.getCompanyId(), statement.getFiscalYear(), statement.getFiscalQuarter());
        if (existing.isPresent()) {
            indicator.setId(existing.get().getId());
            analysisIndicatorDao.update(indicator);
        } else {
            analysisIndicatorDao.insert(indicator);
        }
        log.info("analysis_indicators を再計算しました: companyId={}, fiscalYear={}, fiscalQuarter={}",
                statement.getCompanyId(), statement.getFiscalYear(), statement.getFiscalQuarter());
    }
}
