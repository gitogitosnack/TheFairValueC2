package org.example.web.stock.companyCompetitor.service;

import java.util.List;

import org.example.web.stock.companyCompetitor.domain.CompanyOptionDto;
import org.example.web.stock.companyCompetitor.domain.CompetitorForm;
import org.example.web.stock.companyCompetitor.domain.CompetitorResponseDto;

public interface CompanyCompetitorService {

    List<CompetitorResponseDto> getCompetitorsByCompanyCode(String companyCode);

    List<CompanyOptionDto> getAllCompanyOptions();

    void insertCompetitor(CompetitorForm form);

    void deleteCompetitorById(Integer id);
}
