package org.example.web.batch.marketdata.reader;

import org.example.web.dao.CompanyDao;
import org.example.web.entity.CompanyEntity;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.stereotype.Component;

// ItemReader<CompanyEntity>。companies を delete_flg = 0 の条件で 1 件ずつ返す（日次株価同期用）。
// dailyQuoteSyncStep の Reader（設計書 4.2 参照）。
// @StepScope により Step 実行のたびに companies を取得し直す（内蔵スケジューラでの複数回実行に対応）。
@Component
@StepScope
public class CompanyItemReader extends ListItemReader<CompanyEntity> {

    public CompanyItemReader(CompanyDao companyDao) {
        super(companyDao.selectAllActive());
    }
}
