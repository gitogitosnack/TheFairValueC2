package org.example.web.stock.companyCompetitor.service;

import java.util.List;
import java.util.Optional;

import org.example.web.dao.CompanyCompetitorDao;
import org.example.web.dao.CompanyDao;
import org.example.web.entity.CompanyCompetitorEntity;
import org.example.web.entity.CompanyEntity;
import org.example.web.exception.NotFoundException;
import org.example.web.stock.common.service.CIMapper;
import org.example.web.stock.companyCompetitor.domain.CompanyOptionDto;
import org.example.web.stock.companyCompetitor.domain.CompetitorForm;
import org.example.web.stock.companyCompetitor.domain.CompetitorResponseDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CompanyCompetitorServiceImpl implements CompanyCompetitorService {

    private final CompanyCompetitorDao companyCompetitorDao;
    private final CompanyDao companyDao;
    private final CIMapper ciMapper;

    public CompanyCompetitorServiceImpl(CompanyCompetitorDao companyCompetitorDao, CompanyDao companyDao,
            CIMapper ciMapper) {
        this.companyCompetitorDao = companyCompetitorDao;
        this.companyDao = companyDao;
        this.ciMapper = ciMapper;
    }

    @Override
    public List<CompetitorResponseDto> getCompetitorsByCompanyCode(String companyCode) {
        Integer companyId = ciMapper.convertCodeToId("companies", companyCode);

        return companyCompetitorDao.selectByCompanyId(companyId).stream()
                .map(row -> {
                    CompanyEntity competitor = companyDao.selectById(row.getCompetitorCompanyId())
                            .orElseThrow(() -> new NotFoundException("競合企業が見つかりません。"));
                    return new CompetitorResponseDto(row.getId(), competitor.getCode(), competitor.getName());
                })
                .toList();
    }

    @Override
    public List<CompanyOptionDto> getAllCompanyOptions() {
        return companyDao.selectAll().stream()
                .map(entity -> new CompanyOptionDto(entity.getCode(), entity.getName()))
                .toList();
    }

    @Override
    public void insertCompetitor(CompetitorForm form) {
        Integer companyId = ciMapper.convertCodeToId("companies", form.companyCode());
        Integer competitorCompanyId = ciMapper.convertCodeToId("companies", form.competitorCompanyCode());

        if (companyId.equals(competitorCompanyId)) {
            throw new IllegalArgumentException("自社を競合として登録することはできません。");
        }

        Optional<CompanyCompetitorEntity> existing = companyCompetitorDao
                .selectByCompanyIdAndCompetitorCompanyId(companyId, competitorCompanyId);
        if (existing.isPresent()) {
            return;
        }

        companyCompetitorDao.insert(buildEntity(companyId, competitorCompanyId));
        companyCompetitorDao.insert(buildEntity(competitorCompanyId, companyId));
    }

    @Override
    public void deleteCompetitorById(Integer id) {
        CompanyCompetitorEntity entity = companyCompetitorDao.selectById(id)
                .orElseThrow(() -> new NotFoundException("競合登録が見つかりません。"));

        companyCompetitorDao.delete(entity);

        companyCompetitorDao
                .selectByCompanyIdAndCompetitorCompanyId(entity.getCompetitorCompanyId(), entity.getCompanyId())
                .ifPresent(companyCompetitorDao::delete);
    }

    private CompanyCompetitorEntity buildEntity(Integer companyId, Integer competitorCompanyId) {
        CompanyCompetitorEntity entity = new CompanyCompetitorEntity();
        entity.setCompanyId(companyId);
        entity.setCompetitorCompanyId(competitorCompanyId);
        entity.setDeleteFlg(0);
        return entity;
    }
}
