package org.example.web.batch.marketdata.writer;

// ItemWriter<DailyQuoteEntity>。chunk 単位で daily_quotes へ (company_id, date) キーで UPSERT する。
// 現在株価・発行済株式数・時価総額はいずれも daily_quotes に一本化されている（設計書 4.2 参照）。
