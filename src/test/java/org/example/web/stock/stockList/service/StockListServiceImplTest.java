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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockListServiceImplTest {

    @Mock
    StockListDao stockListDao;
    @Mock
    CountryDao countryDao;
    @Mock
    IndustryDao industryDao;
    @Mock
    CurrencyDao currencyDao;

    StockListServiceImpl service;

    private StockEntity stock(Integer id, String code, String name, Integer countryId, Integer industryId,
            Integer currencyId) {
        return new StockEntity(id, code, name, countryId, industryId, "東証プライム", currencyId, 0);
    }

    @Test
    void initialDispAll_マスタ名を解決してResponseDtoに変換すること() {
        service = new StockListServiceImpl(stockListDao, countryDao, industryDao, currencyDao);

        when(stockListDao.findAll()).thenReturn(List.of(stock(1, "7203", "トヨタ自動車", 1, 1, 1)));

        CountryEntity country = new CountryEntity();
        country.setId(1);
        country.setName("日本");
        when(countryDao.selectAll()).thenReturn(List.of(country));

        IndustryEntity industry = new IndustryEntity();
        industry.setId(1);
        industry.setName("自動車");
        when(industryDao.selectAll()).thenReturn(List.of(industry));

        CurrencyEntity currency = new CurrencyEntity();
        currency.setId(1);
        currency.setCode("JPY");
        when(currencyDao.selectAll()).thenReturn(List.of(currency));

        List<StockListResponseDto> result = service.initialDispAll();

        assertThat(result).hasSize(1);
        StockListResponseDto dto = result.get(0);
        assertThat(dto.getCode()).isEqualTo("7203");
        assertThat(dto.getCountry_name()).isEqualTo("日本");
        assertThat(dto.getIndustry_name()).isEqualTo("自動車");
        assertThat(dto.getCurrency_name()).isEqualTo("JPY");
    }

    @Test
    void initialDispAll_マスタに存在しないIDのときnullで解決されること() {
        service = new StockListServiceImpl(stockListDao, countryDao, industryDao, currencyDao);

        when(stockListDao.findAll()).thenReturn(List.of(stock(1, "9999", "不明銘柄", 99, 99, 99)));
        when(countryDao.selectAll()).thenReturn(List.of());
        when(industryDao.selectAll()).thenReturn(List.of());
        when(currencyDao.selectAll()).thenReturn(List.of());

        List<StockListResponseDto> result = service.initialDispAll();

        assertThat(result.get(0).getCountry_name()).isNull();
        assertThat(result.get(0).getIndustry_name()).isNull();
        assertThat(result.get(0).getCurrency_name()).isNull();
    }

    @Test
    void insertStockInfo_国業種通貨を固定値1で新規登録すること() {
        service = new StockListServiceImpl(stockListDao, countryDao, industryDao, currencyDao);
        StockListForm form = new StockListForm();
        form.setCode("6758");
        form.setName("ソニーグループ");
        form.setMarket_name("東証プライム");

        service.insertStockInfo(form);

        ArgumentCaptor<StockEntity> captor = ArgumentCaptor.forClass(StockEntity.class);
        verify(stockListDao).insert(captor.capture());
        StockEntity entity = captor.getValue();
        assertThat(entity.getId()).isNull();
        assertThat(entity.getCode()).isEqualTo("6758");
        assertThat(entity.getCountry_id()).isEqualTo(1);
        assertThat(entity.getIndustry_id()).isEqualTo(1);
        assertThat(entity.getCurrency_id()).isEqualTo(1);
        assertThat(entity.getDelete_flg()).isEqualTo(0);
    }

    @Test
    void updateStockInfo_既存データが見つかったとき更新すること() {
        service = new StockListServiceImpl(stockListDao, countryDao, industryDao, currencyDao);
        when(stockListDao.selectById(1)).thenReturn(stock(1, "7203", "トヨタ自動車", 1, 1, 1));
        StockListForm form = new StockListForm();
        form.setId(1);
        form.setCode("7203");
        form.setName("トヨタ自動車");
        form.setMarket_name("東証プライム");

        service.updateStockInfo(form);

        verify(stockListDao).update(any(StockEntity.class));
    }

    @Test
    void updateStockInfo_既存データが見つからないとき何もしないこと() {
        service = new StockListServiceImpl(stockListDao, countryDao, industryDao, currencyDao);
        when(stockListDao.selectById(999)).thenReturn(null);
        StockListForm form = new StockListForm();
        form.setId(999);

        service.updateStockInfo(form);

        verify(stockListDao, never()).update(any());
    }

    @Test
    void deleteStockInfoById_既存データが見つかったとき削除すること() {
        service = new StockListServiceImpl(stockListDao, countryDao, industryDao, currencyDao);
        StockEntity entity = stock(1, "7203", "トヨタ自動車", 1, 1, 1);
        when(stockListDao.selectById(1)).thenReturn(entity);

        service.deleteStockInfoById(1);

        verify(stockListDao).delete(entity);
    }

    @Test
    void deleteStockInfoById_既存データが見つからないとき何もしないこと() {
        service = new StockListServiceImpl(stockListDao, countryDao, industryDao, currencyDao);
        when(stockListDao.selectById(999)).thenReturn(null);

        service.deleteStockInfoById(999);

        verify(stockListDao, never()).delete(any());
    }
}
