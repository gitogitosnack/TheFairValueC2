package org.example.web.stock.companyCompetitor.controller;

import java.util.List;

import org.example.web.stock.companyCompetitor.domain.CompanyOptionDto;
import org.example.web.stock.companyCompetitor.domain.CompetitorForm;
import org.example.web.stock.companyCompetitor.domain.CompetitorResponseDto;
import org.example.web.stock.companyCompetitor.service.CompanyCompetitorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest_company_competitors")
public class CompanyCompetitorRestController {

    private final CompanyCompetitorService companyCompetitorService;

    public CompanyCompetitorRestController(CompanyCompetitorService companyCompetitorService) {
        this.companyCompetitorService = companyCompetitorService;
    }

    @GetMapping("/by-company/{code}")
    public List<CompetitorResponseDto> getByCompany(@PathVariable String code) {
        return companyCompetitorService.getCompetitorsByCompanyCode(code);
    }

    @GetMapping("/companies")
    public List<CompanyOptionDto> getAllCompanyOptions() {
        return companyCompetitorService.getAllCompanyOptions();
    }

    @PostMapping("/insert")
    public ResponseEntity<String> insert(@RequestBody CompetitorForm form) {
        try {
            companyCompetitorService.insertCompetitor(form);
            return ResponseEntity.ok("競合を登録しました");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("競合の登録に失敗しました");
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> delete(@PathVariable Integer id) {
        try {
            companyCompetitorService.deleteCompetitorById(id);
            return ResponseEntity.ok("Deleted");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("削除に失敗しました");
        }
    }
}
