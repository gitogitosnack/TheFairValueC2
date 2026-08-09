package org.example.web.stock.country.service;

import org.example.web.dao.CountryDao;
import org.example.web.entity.CountryEntity;
import org.example.web.exception.NotFoundException;
import org.example.web.stock.country.domain.CountryForm;
import org.example.web.stock.country.domain.CountryResponseDto;
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
class CountryServiceImplTest {

    @Mock
    CountryDao countryDao;

    CountryServiceImpl service;

    @Test
    void initialDispAll_Entityの一覧をResponseDtoの一覧に変換すること() {
        service = new CountryServiceImpl(countryDao);
        CountryEntity entity = new CountryEntity();
        entity.setId(1);
        entity.setCode("JP");
        entity.setName("日本");
        when(countryDao.selectAll()).thenReturn(List.of(entity));

        List<CountryResponseDto> result = service.initialDispAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1);
        assertThat(result.get(0).getCode()).isEqualTo("JP");
        assertThat(result.get(0).getName()).isEqualTo("日本");
    }

    @Test
    void insertCountryInfo_フォームの値でEntityを組み立てて登録すること() {
        service = new CountryServiceImpl(countryDao);
        CountryForm form = new CountryForm(null, "US", "アメリカ");

        service.insertCountryInfo(form);

        ArgumentCaptor<CountryEntity> captor = ArgumentCaptor.forClass(CountryEntity.class);
        verify(countryDao).insert(captor.capture());
        assertThat(captor.getValue().getId()).isNull();
        assertThat(captor.getValue().getCode()).isEqualTo("US");
        assertThat(captor.getValue().getName()).isEqualTo("アメリカ");
    }

    @Test
    void updateCountryInfo_既存データが見つかったとき上書きして更新すること() {
        service = new CountryServiceImpl(countryDao);
        CountryEntity existing = new CountryEntity();
        existing.setId(1);
        existing.setCode("JP");
        existing.setName("日本");
        when(countryDao.selectById(1)).thenReturn(Optional.of(existing));
        CountryForm form = new CountryForm(1, "JPN", "日本国");

        service.updateCountryInfo(form);

        ArgumentCaptor<CountryEntity> captor = ArgumentCaptor.forClass(CountryEntity.class);
        verify(countryDao).update(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo("JPN");
        assertThat(captor.getValue().getName()).isEqualTo("日本国");
    }

    @Test
    void updateCountryInfo_既存データが見つからないとき例外を投げること() {
        service = new CountryServiceImpl(countryDao);
        when(countryDao.selectById(999)).thenReturn(Optional.empty());
        CountryForm form = new CountryForm(999, "XX", "存在しない国");

        assertThatThrownBy(() -> service.updateCountryInfo(form))
                .isInstanceOf(NotFoundException.class);
        verify(countryDao, never()).update(any());
    }

    @Test
    void deleteCountryInfoById_既存データが見つかったとき削除すること() {
        service = new CountryServiceImpl(countryDao);
        CountryEntity existing = new CountryEntity();
        existing.setId(1);
        when(countryDao.selectById(1)).thenReturn(Optional.of(existing));

        service.deleteCountryInfoById(1);

        verify(countryDao).delete(existing);
    }

    @Test
    void deleteCountryInfoById_既存データが見つからないとき例外を投げること() {
        service = new CountryServiceImpl(countryDao);
        when(countryDao.selectById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteCountryInfoById(999))
                .isInstanceOf(NotFoundException.class);
        verify(countryDao, never()).delete(any());
    }
}
