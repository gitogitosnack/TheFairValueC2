package org.example.web.batch.marketdata.reader;

import org.example.web.dao.CompanyDao;
import org.example.web.entity.CompanyEntity;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.stereotype.Component;

// ItemReader<CompanyEntity>。米国株のみに絞った companies を 1 件ずつ返す（財務諸表同期・米国株用）。
// usFinancialStatementStep の Reader（設計書 4.3.1 参照）。
@Component
@StepScope
public class UsCompanyItemReader extends ListItemReader<CompanyEntity> {

    public UsCompanyItemReader(CompanyDao companyDao) {
        super(companyDao.selectActiveByCountryCode("US"));
    }
}
