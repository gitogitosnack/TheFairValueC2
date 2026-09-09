package org.example.web.batch.marketdata.processor;

import java.util.Map;
import java.util.Optional;

import org.example.web.batch.marketdata.client.RateLimitException;
import org.example.web.batch.marketdata.client.StockPriceProvider;
import org.example.web.batch.marketdata.client.dto.StockQuoteData;
import org.example.web.dao.CountryDao;
import org.example.web.entity.CompanyEntity;
import org.example.web.entity.CountryEntity;
import org.example.web.entity.DailyQuoteEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

// ItemProcessor<CompanyEntity, DailyQuoteEntity>。StockPriceProvider を呼び出して当日の
// 始値・高値・安値・終値・出来高・時価総額・発行済株式数を取得し変換する。
// 取得失敗（404・タイムアウト等）はログを残し null を返してそのアイテムを chunk から除外する。
// 429（レート制限）は RateLimitException を throw し Step 側の faultTolerant().retry() に委ねる（設計書 4.2 参照）。
@Component
public class DailyQuoteItemProcessor implements ItemProcessor<CompanyEntity, DailyQuoteEntity> {

    private static final Logger log = LoggerFactory.getLogger(DailyQuoteItemProcessor.class);

    // country_id → countries.code → StockPriceProvider の Bean 名（設計書 4.1 参照）。
    // if/else の分岐を増やさず Map ルックアップで実装を切り替える。
    private static final Map<String, String> COUNTRY_CODE_TO_PROVIDER_BEAN = Map.of(
            "US", "fmpStockPriceProvider",
            "JP", "yahooFinanceStockPriceProvider");

    private final CountryDao countryDao;
    private final Map<String, StockPriceProvider> stockPriceProviders;

    public DailyQuoteItemProcessor(CountryDao countryDao, Map<String, StockPriceProvider> stockPriceProviders) {
        this.countryDao = countryDao;
        this.stockPriceProviders = stockPriceProviders;
    }

    @Override
    public DailyQuoteEntity process(CompanyEntity company) {
        StockPriceProvider provider = resolveProvider(company);
        if (provider == null) {
            return null;
        }

        StockQuoteData quote;
        try {
            quote = provider.fetchLatestQuote(company.getCode());
        } catch (RateLimitException e) {
            throw e;
        } catch (Exception e) {
            log.warn("株価取得に失敗しました。次回バッチで再取得します: companyId={}, code={}",
                    company.getId(), company.getCode(), e);
            return null;
        }
        if (quote == null) {
            log.warn("株価データが取得できませんでした（対象銘柄なし）: companyId={}, code={}",
                    company.getId(), company.getCode());
            return null;
        }

        DailyQuoteEntity entity = new DailyQuoteEntity();
        entity.setCompanyId(company.getId());
        entity.setDate(quote.date());
        entity.setOpenPrice(quote.openPrice());
        entity.setHighPrice(quote.highPrice());
        entity.setLowPrice(quote.lowPrice());
        entity.setClosePrice(quote.closePrice());
        entity.setVolume(quote.volume());
        entity.setMarketCap(quote.marketCap());
        entity.setSharesOutstanding(quote.sharesOutstanding());
        return entity;
    }

    private StockPriceProvider resolveProvider(CompanyEntity company) {
        if (company.getCountryId() == null) {
            log.warn("country_id が未設定のためスキップします: companyId={}", company.getId());
            return null;
        }
        Optional<CountryEntity> country = countryDao.selectById(company.getCountryId());
        String countryCode = country.map(CountryEntity::getCode).orElse(null);
        String beanName = COUNTRY_CODE_TO_PROVIDER_BEAN.get(countryCode);
        if (beanName == null) {
            log.warn("対応する StockPriceProvider が無い国コードのためスキップします: companyId={}, countryCode={}",
                    company.getId(), countryCode);
            return null;
        }
        return stockPriceProviders.get(beanName);
    }
}
