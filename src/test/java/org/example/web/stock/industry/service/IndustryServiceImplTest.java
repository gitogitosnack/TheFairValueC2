package org.example.web.stock.industry.service;

import org.example.web.dao.IndustryDao;
import org.example.web.entity.IndustryEntity;
import org.example.web.stock.industry.domain.IndustryForm;
import org.example.web.stock.industry.domain.IndustryResponseDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndustryServiceImplTest {

    @Mock
    IndustryDao industryDao;

    IndustryServiceImpl service;

    @Test
    void initialDispAll_Entityの一覧をResponseDtoの一覧に変換すること() {
        service = new IndustryServiceImpl(industryDao);
        IndustryEntity entity = new IndustryEntity();
        entity.setId(1);
        entity.setName("自動車");
        entity.setSectorName("輸送用機器");
        entity.setDescription("自動車製造業");
        entity.setAvgPer(BigDecimal.valueOf(12.5));
        when(industryDao.selectAll()).thenReturn(List.of(entity));

        List<IndustryResponseDto> result = service.initialDispAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("自動車");
        assertThat(result.get(0).getSectorName()).isEqualTo("輸送用機器");
        assertThat(result.get(0).getAvgPer()).isEqualByComparingTo("12.5");
    }

    @Test
    void insertIndustryInfo_フォームの値でEntityを組み立てて登録すること() {
        service = new IndustryServiceImpl(industryDao);
        IndustryForm form = new IndustryForm(null, "小売業", "小売", "説明文", BigDecimal.TEN);

        service.insertIndustryInfo(form);

        ArgumentCaptor<IndustryEntity> captor = ArgumentCaptor.forClass(IndustryEntity.class);
        verify(industryDao).insert(captor.capture());
        assertThat(captor.getValue().getId()).isNull();
        assertThat(captor.getValue().getName()).isEqualTo("小売業");
        assertThat(captor.getValue().getAvgPer()).isEqualByComparingTo("10");
    }

    @Test
    void updateIndustryInfo_既存データが見つかったとき上書きして更新すること() {
        service = new IndustryServiceImpl(industryDao);
        IndustryEntity existing = new IndustryEntity();
        existing.setId(1);
        when(industryDao.selectById(1)).thenReturn(Optional.of(existing));
        IndustryForm form = new IndustryForm(1, "更新後業種", "更新後セクター", "更新後説明", BigDecimal.valueOf(20));

        service.updateIndustryInfo(form);

        ArgumentCaptor<IndustryEntity> captor = ArgumentCaptor.forClass(IndustryEntity.class);
        verify(industryDao).update(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("更新後業種");
        assertThat(captor.getValue().getSectorName()).isEqualTo("更新後セクター");
        assertThat(captor.getValue().getAvgPer()).isEqualByComparingTo("20");
    }

    @Test
    void updateIndustryInfo_既存データが見つからないとき例外を投げること() {
        service = new IndustryServiceImpl(industryDao);
        when(industryDao.selectById(999)).thenReturn(Optional.empty());
        IndustryForm form = new IndustryForm(999, "存在しない業種", null, null, null);

        assertThatThrownBy(() -> service.updateIndustryInfo(form))
                .isInstanceOf(org.example.web.exception.NotFoundException.class);
        verify(industryDao, never()).update(any());
    }

    @Test
    void deleteIndustryInfoById_既存データが見つかったとき削除すること() {
        service = new IndustryServiceImpl(industryDao);
        IndustryEntity existing = new IndustryEntity();
        existing.setId(1);
        when(industryDao.selectById(1)).thenReturn(Optional.of(existing));

        service.deleteIndustryInfoById(1);

        verify(industryDao).delete(existing);
    }

    @Test
    void deleteIndustryInfoById_既存データが見つからないとき例外を投げること() {
        service = new IndustryServiceImpl(industryDao);
        when(industryDao.selectById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteIndustryInfoById(999))
                .isInstanceOf(org.example.web.exception.NotFoundException.class);
        verify(industryDao, never()).delete(any());
    }
}
