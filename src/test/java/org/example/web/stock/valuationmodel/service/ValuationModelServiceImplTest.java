package org.example.web.stock.valuationmodel.service;

import org.example.web.dao.ValuationModelsDao;
import org.example.web.entity.ValuationModelEntity;
import org.example.web.exception.NotFoundException;
import org.example.web.stock.valuationmodel.domain.ValuationModelForm;
import org.example.web.stock.valuationmodel.domain.ValuationModelResponseDto;
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
class ValuationModelServiceImplTest {

    @Mock
    ValuationModelsDao valuationModelDao;

    ValuationModelServiceImpl service;

    @Test
    void initialDispAll_Entityの一覧をResponseDtoの一覧に変換すること() {
        service = new ValuationModelServiceImpl(valuationModelDao);
        ValuationModelEntity entity = new ValuationModelEntity();
        entity.setId(1);
        entity.setModelName("DCF法");
        entity.setFormulaDescription("将来キャッシュフローを割り引く方法");
        when(valuationModelDao.selectAll()).thenReturn(List.of(entity));

        List<ValuationModelResponseDto> result = service.initialDispAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getModelName()).isEqualTo("DCF法");
    }

    @Test
    void insertValuationModelInfo_フォームの値でEntityを組み立てて登録すること() {
        service = new ValuationModelServiceImpl(valuationModelDao);
        ValuationModelForm form = new ValuationModelForm(null, "PERマルチプル法", "PERに基づく方法");

        service.insertValuationModelInfo(form);

        ArgumentCaptor<ValuationModelEntity> captor = ArgumentCaptor.forClass(ValuationModelEntity.class);
        verify(valuationModelDao).insert(captor.capture());
        assertThat(captor.getValue().getId()).isNull();
        assertThat(captor.getValue().getModelName()).isEqualTo("PERマルチプル法");
    }

    @Test
    void updateValuationModelInfo_既存データが見つかったとき上書きして更新すること() {
        service = new ValuationModelServiceImpl(valuationModelDao);
        ValuationModelEntity existing = new ValuationModelEntity();
        existing.setId(1);
        when(valuationModelDao.selectById(1)).thenReturn(Optional.of(existing));
        ValuationModelForm form = new ValuationModelForm(1, "更新後モデル", "更新後説明");

        service.updateValuationModelInfo(form);

        ArgumentCaptor<ValuationModelEntity> captor = ArgumentCaptor.forClass(ValuationModelEntity.class);
        verify(valuationModelDao).update(captor.capture());
        assertThat(captor.getValue().getModelName()).isEqualTo("更新後モデル");
    }

    @Test
    void updateValuationModelInfo_既存データが見つからないとき例外を投げること() {
        service = new ValuationModelServiceImpl(valuationModelDao);
        when(valuationModelDao.selectById(999)).thenReturn(Optional.empty());
        ValuationModelForm form = new ValuationModelForm(999, "存在しないモデル", null);

        assertThatThrownBy(() -> service.updateValuationModelInfo(form))
                .isInstanceOf(NotFoundException.class);
        verify(valuationModelDao, never()).update(any());
    }

    @Test
    void deleteValuationModelInfoById_既存データが見つかったとき削除すること() {
        service = new ValuationModelServiceImpl(valuationModelDao);
        ValuationModelEntity existing = new ValuationModelEntity();
        existing.setId(1);
        when(valuationModelDao.selectById(1)).thenReturn(Optional.of(existing));

        service.deleteValuationModelInfoById(1);

        verify(valuationModelDao).delete(existing);
    }

    @Test
    void deleteValuationModelInfoById_既存データが見つからないとき例外を投げること() {
        service = new ValuationModelServiceImpl(valuationModelDao);
        when(valuationModelDao.selectById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteValuationModelInfoById(999))
                .isInstanceOf(NotFoundException.class);
        verify(valuationModelDao, never()).delete(any());
    }
}
