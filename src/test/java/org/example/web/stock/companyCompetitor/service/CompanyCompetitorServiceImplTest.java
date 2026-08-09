package org.example.web.stock.companyCompetitor.service;

import org.example.web.dao.CompanyCompetitorDao;
import org.example.web.dao.CompanyDao;
import org.example.web.entity.CompanyCompetitorEntity;
import org.example.web.entity.CompanyEntity;
import org.example.web.exception.NotFoundException;
import org.example.web.stock.common.service.CIMapper;
import org.example.web.stock.companyCompetitor.domain.CompanyOptionDto;
import org.example.web.stock.companyCompetitor.domain.CompetitorForm;
import org.example.web.stock.companyCompetitor.domain.CompetitorResponseDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyCompetitorServiceImplTest {

    @Mock
    CompanyCompetitorDao companyCompetitorDao;
    @Mock
    CompanyDao companyDao;
    @Mock
    CIMapper ciMapper;

    CompanyCompetitorServiceImpl service;

    private CompanyEntity company(Integer id, String code, String name) {
        CompanyEntity entity = new CompanyEntity();
        entity.setId(id);
        entity.setCode(code);
        entity.setName(name);
        return entity;
    }

    @Test
    void getCompetitorsByCompanyCode_登録済み競合企業の一覧を返すこと() {
        service = new CompanyCompetitorServiceImpl(companyCompetitorDao, companyDao, ciMapper);
        when(ciMapper.convertCodeToId("companies", "7203")).thenReturn(1);
        CompanyCompetitorEntity row = new CompanyCompetitorEntity();
        row.setId(10);
        row.setCompanyId(1);
        row.setCompetitorCompanyId(2);
        when(companyCompetitorDao.selectByCompanyId(1)).thenReturn(List.of(row));
        when(companyDao.selectById(2)).thenReturn(Optional.of(company(2, "7267", "ホンダ")));

        List<CompetitorResponseDto> result = service.getCompetitorsByCompanyCode("7203");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCompetitorCode()).isEqualTo("7267");
        assertThat(result.get(0).getCompetitorName()).isEqualTo("ホンダ");
    }

    @Test
    void getCompetitorsByCompanyCode_競合企業がマスタに見つからないとき例外を投げること() {
        service = new CompanyCompetitorServiceImpl(companyCompetitorDao, companyDao, ciMapper);
        when(ciMapper.convertCodeToId("companies", "7203")).thenReturn(1);
        CompanyCompetitorEntity row = new CompanyCompetitorEntity();
        row.setCompetitorCompanyId(999);
        when(companyCompetitorDao.selectByCompanyId(1)).thenReturn(List.of(row));
        when(companyDao.selectById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCompetitorsByCompanyCode("7203"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getAllCompanyOptions_全企業をコードと名前のDtoに変換すること() {
        service = new CompanyCompetitorServiceImpl(companyCompetitorDao, companyDao, ciMapper);
        when(companyDao.selectAll()).thenReturn(List.of(company(1, "7203", "トヨタ自動車")));

        List<CompanyOptionDto> result = service.getAllCompanyOptions();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCode()).isEqualTo("7203");
        assertThat(result.get(0).getName()).isEqualTo("トヨタ自動車");
    }

    @Test
    void insertCompetitor_自社を競合として登録しようとしたとき例外を投げること() {
        service = new CompanyCompetitorServiceImpl(companyCompetitorDao, companyDao, ciMapper);
        when(ciMapper.convertCodeToId("companies", "7203")).thenReturn(1);
        CompetitorForm form = new CompetitorForm("7203", "7203");

        assertThatThrownBy(() -> service.insertCompetitor(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("自社を競合として登録することはできません");
    }

    @Test
    void insertCompetitor_双方向に競合登録すること() {
        service = new CompanyCompetitorServiceImpl(companyCompetitorDao, companyDao, ciMapper);
        when(ciMapper.convertCodeToId("companies", "7203")).thenReturn(1);
        when(ciMapper.convertCodeToId("companies", "7267")).thenReturn(2);
        when(companyCompetitorDao.selectByCompanyIdAndCompetitorCompanyId(1, 2)).thenReturn(Optional.empty());
        CompetitorForm form = new CompetitorForm("7203", "7267");

        service.insertCompetitor(form);

        ArgumentCaptor<CompanyCompetitorEntity> captor = ArgumentCaptor.forClass(CompanyCompetitorEntity.class);
        verify(companyCompetitorDao, times(2)).insert(captor.capture());
        List<CompanyCompetitorEntity> inserted = captor.getAllValues();
        assertThat(inserted).extracting(CompanyCompetitorEntity::getCompanyId,
                CompanyCompetitorEntity::getCompetitorCompanyId)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(1, 2),
                        org.assertj.core.groups.Tuple.tuple(2, 1));
    }

    @Test
    void insertCompetitor_既に登録済みのとき何もしないこと() {
        service = new CompanyCompetitorServiceImpl(companyCompetitorDao, companyDao, ciMapper);
        when(ciMapper.convertCodeToId("companies", "7203")).thenReturn(1);
        when(ciMapper.convertCodeToId("companies", "7267")).thenReturn(2);
        CompanyCompetitorEntity existing = new CompanyCompetitorEntity();
        when(companyCompetitorDao.selectByCompanyIdAndCompetitorCompanyId(1, 2)).thenReturn(Optional.of(existing));
        CompetitorForm form = new CompetitorForm("7203", "7267");

        service.insertCompetitor(form);

        verify(companyCompetitorDao, never()).insert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deleteCompetitorById_双方向の登録を両方削除すること() {
        service = new CompanyCompetitorServiceImpl(companyCompetitorDao, companyDao, ciMapper);
        CompanyCompetitorEntity forward = new CompanyCompetitorEntity();
        forward.setId(10);
        forward.setCompanyId(1);
        forward.setCompetitorCompanyId(2);
        when(companyCompetitorDao.selectById(10)).thenReturn(Optional.of(forward));
        CompanyCompetitorEntity backward = new CompanyCompetitorEntity();
        backward.setId(11);
        when(companyCompetitorDao.selectByCompanyIdAndCompetitorCompanyId(2, 1)).thenReturn(Optional.of(backward));

        service.deleteCompetitorById(10);

        verify(companyCompetitorDao).delete(forward);
        verify(companyCompetitorDao).delete(backward);
    }

    @Test
    void deleteCompetitorById_存在しないIDのとき例外を投げること() {
        service = new CompanyCompetitorServiceImpl(companyCompetitorDao, companyDao, ciMapper);
        when(companyCompetitorDao.selectById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteCompetitorById(999))
                .isInstanceOf(NotFoundException.class);
    }
}
