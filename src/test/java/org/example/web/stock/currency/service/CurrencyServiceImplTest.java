package org.example.web.stock.currency.service;

import org.example.web.dao.CurrencyDao;
import org.example.web.entity.CurrencyEntity;
import org.example.web.exception.NotFoundException;
import org.example.web.stock.currency.domain.CurrencyForm;
import org.example.web.stock.currency.domain.CurrencyResponseDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrencyServiceImplTest {

    @Mock
    CurrencyDao currencyDao;

    CurrencyServiceImpl service;

    @Test
    void initialDispAll_Entityの一覧をResponseDtoの一覧に変換すること() {
        service = new CurrencyServiceImpl(currencyDao);
        CurrencyEntity entity = new CurrencyEntity();
        entity.setId(1);
        entity.setCode("JPY");
        entity.setSymbol("¥");
        when(currencyDao.selectAll()).thenReturn(List.of(entity));

        List<CurrencyResponseDto> result = service.initialDispAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1);
        assertThat(result.get(0).getCode()).isEqualTo("JPY");
        assertThat(result.get(0).getSymbol()).isEqualTo("¥");
    }

    @Test
    void insertCurrencyInfo_フォームの値でEntityを組み立てて登録すること() {
        service = new CurrencyServiceImpl(currencyDao);
        CurrencyForm form = new CurrencyForm(null, "USD", "$");

        service.insertCurrencyInfo(form);

        ArgumentCaptor<CurrencyEntity> captor = ArgumentCaptor.forClass(CurrencyEntity.class);
        verify(currencyDao).insert(captor.capture());
        assertThat(captor.getValue().getId()).isNull();
        assertThat(captor.getValue().getCode()).isEqualTo("USD");
        assertThat(captor.getValue().getSymbol()).isEqualTo("$");
    }

    @Test
    void updateCurrencyInfo_既存データが見つかったとき上書きして更新すること() {
        service = new CurrencyServiceImpl(currencyDao);
        CurrencyEntity existing = new CurrencyEntity();
        existing.setId(1);
        existing.setCode("JPY");
        existing.setSymbol("¥");
        when(currencyDao.selectById(1)).thenReturn(Optional.of(existing));
        CurrencyForm form = new CurrencyForm(1, "JPY2", "円");

        service.updateCurrencyInfo(form);

        ArgumentCaptor<CurrencyEntity> captor = ArgumentCaptor.forClass(CurrencyEntity.class);
        verify(currencyDao).update(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo("JPY2");
        assertThat(captor.getValue().getSymbol()).isEqualTo("円");
    }

    @Test
    void updateCurrencyInfo_既存データが見つからないとき例外を投げること() {
        service = new CurrencyServiceImpl(currencyDao);
        when(currencyDao.selectById(999)).thenReturn(Optional.empty());
        CurrencyForm form = new CurrencyForm(999, "XXX", "?");

        assertThatThrownBy(() -> service.updateCurrencyInfo(form))
                .isInstanceOf(NotFoundException.class);
        verify(currencyDao, never()).update(any());
    }

    @Test
    void deleteCurrencyInfoById_既存データが見つかったとき削除すること() {
        service = new CurrencyServiceImpl(currencyDao);
        CurrencyEntity existing = new CurrencyEntity();
        existing.setId(1);
        when(currencyDao.selectById(1)).thenReturn(Optional.of(existing));

        service.deleteCurrencyInfoById(1);

        verify(currencyDao).delete(existing);
    }

    @Test
    void deleteCurrencyInfoById_既存データが見つからないとき例外を投げること() {
        service = new CurrencyServiceImpl(currencyDao);
        when(currencyDao.selectById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteCurrencyInfoById(999))
                .isInstanceOf(NotFoundException.class);
        verify(currencyDao, never()).delete(any());
    }
}
