package org.example.web.stock.stockList.service;

import org.example.web.dao.CountryDao;
import org.example.web.dao.CurrencyDao;
import org.example.web.dao.IndustryDao;
import org.example.web.dao.StockListDao;
import org.example.web.entity.CountryEntity;
import org.example.web.entity.CurrencyEntity;
import org.example.web.entity.IndustryEntity;
import org.example.web.entity.StockEntity;
import org.example.web.stock.stockList.domain.StockListForm;
import org.example.web.stock.stockList.domain.StockListResponseDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class StockListServiceImpl implements StockListService {

    // Define the fields here
    private final StockListDao stockListDao;
    private final CountryDao countryDao;
    private final IndustryDao industryDao;
    private final CurrencyDao currencyDao;

    // Define the constructor
    public StockListServiceImpl(
            StockListDao stockListDao
            ,CountryDao countryDao
            ,IndustryDao industryDao
            ,CurrencyDao currencyDao
    ) {
        this.stockListDao = stockListDao;
        this.countryDao = countryDao;
        this.industryDao = industryDao;
        this.currencyDao = currencyDao;
    }

    @Override
    public List<StockListResponseDto> initialDispAll() {

        // Get all stocks
        List<StockEntity> stocksHoldingByEntity = stockListDao.findAll();

        // Get all master data (id -> name) so each company's country/industry/currency can be resolved
        Map<Integer, String> countryNames = countryDao.selectAll().stream()
                .collect(Collectors.toMap(CountryEntity::getId, CountryEntity::getName));
        Map<Integer, String> industryNames = industryDao.selectAll().stream()
                .collect(Collectors.toMap(IndustryEntity::getId, IndustryEntity::getName));
        Map<Integer, String> currencyNames = currencyDao.selectAll().stream()
                .collect(Collectors.toMap(CurrencyEntity::getId, CurrencyEntity::getCode));

        // Convert to the dto from entity
        List<StockListResponseDto> stocks = this.unloading(stocksHoldingByEntity, countryNames, industryNames, currencyNames);

        return stocks;
    }

    public List<StockListResponseDto> unloading(
            List<StockEntity> entity
            ,Map<Integer, String> countryNames
            ,Map<Integer, String> industryNames
            ,Map<Integer, String> currencyNames
    ) {
        return entity.stream().map(record -> new StockListResponseDto(
                record.getId()
                ,record.getCode()
                ,record.getName()
                ,record.getMarket_name()
                ,countryNames.get(record.getCountry_id())
                ,industryNames.get(record.getIndustry_id())
                ,currencyNames.get(record.getCurrency_id())
        )).toList();
    }

    @Override
    public void updateStockInfo(StockListForm form) {
        // 1. 現在のデータを取得
        StockEntity record = stockListDao.selectById(form.getId());

        if (record != null) {
            // 2. 画面からの入力値で上書き
        StockEntity entity = new StockEntity(
                form.getId()
                ,form.getCode()
                ,form.getName()
                ,1
                ,1
                ,form.getMarket_name()
                ,1
                ,0
                );
            // entity.setUpdatedAt(LocalDateTime.now()); // 更新日などがあれば

            // 3. 更新実行
            stockListDao.update(entity);
        }

    }

    @Override
    public void deleteStockInfoById(Integer id) {
        // 現在のデータを取得
        StockEntity record = stockListDao.selectById(id);

        if (record != null) {

            // 更新実行
            stockListDao.delete(record);
        }
    }

    @Override
    public void insertStockInfo(StockListForm form) {
            // 新規登録情報をセット
            StockEntity entity = new StockEntity(
                    null
                    ,form.getCode()
                    ,form.getName()
                    ,1
                    ,1
                    ,form.getMarket_name()
                    ,1
                    ,0
            );
            // entity.setCreatedAt(LocalDateTime.now()); // 登録日などがあれば

            // 新規登録処理実行
            stockListDao.insert(entity);


    }
}
