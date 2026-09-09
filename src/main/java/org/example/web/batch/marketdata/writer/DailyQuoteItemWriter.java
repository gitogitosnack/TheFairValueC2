package org.example.web.batch.marketdata.writer;

import java.util.Optional;

import org.example.web.dao.DailyQuoteDao;
import org.example.web.entity.DailyQuoteEntity;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

// ItemWriter<DailyQuoteEntity>。chunk 単位で daily_quotes へ (company_id, date) キーで UPSERT する。
// 現在株価・発行済株式数・時価総額はいずれも daily_quotes に一本化されている（設計書 4.2 参照）。
@Component
public class DailyQuoteItemWriter implements ItemWriter<DailyQuoteEntity> {

    private final DailyQuoteDao dailyQuoteDao;

    public DailyQuoteItemWriter(DailyQuoteDao dailyQuoteDao) {
        this.dailyQuoteDao = dailyQuoteDao;
    }

    @Override
    public void write(Chunk<? extends DailyQuoteEntity> chunk) {
        for (DailyQuoteEntity entity : chunk) {
            Optional<DailyQuoteEntity> existing =
                    dailyQuoteDao.selectByCompanyIdAndDate(entity.getCompanyId(), entity.getDate());
            if (existing.isPresent()) {
                entity.setId(existing.get().getId());
                dailyQuoteDao.update(entity);
            } else {
                dailyQuoteDao.insert(entity);
            }
        }
    }
}
