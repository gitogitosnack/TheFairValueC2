package org.example.web.batch.marketdata.reader;

import org.example.web.dao.CompanyDao;
import org.example.web.entity.CompanyEntity;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.stereotype.Component;

// ItemReader<CompanyEntity>。フィリピン株のみに絞った companies を 1 件ずつ返す（財務諸表同期・比株用）。
// phFinancialStatementStep の Reader。フィリピン株には実データ取得元（FinancialDataProvider実装）が無いため、
// モックモード（marketdata.api.mock-enabled=true）以外では PhFinancialStatementItemProcessor が
// 常に null を返し実質何もしない（UsCompanyItemReader と同じ構成）。
@Component
@StepScope
public class PhCompanyItemReader extends ListItemReader<CompanyEntity> {

    public PhCompanyItemReader(CompanyDao companyDao) {
        super(companyDao.selectActiveByCountryCode("PH"));
    }
}
