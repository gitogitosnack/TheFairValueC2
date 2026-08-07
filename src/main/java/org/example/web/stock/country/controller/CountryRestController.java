package org.example.web.stock.country.controller;

import org.example.web.common.excel.ExcelExportUtil;
import org.example.web.stock.country.domain.CountryForm;
import org.example.web.stock.country.domain.CountryResponseDto;
import org.example.web.stock.country.service.CountryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/rest_countries")
public class CountryRestController {

    // @Autowiredは、コンストラクタ型でDIしているため、書かなくてOK！
    private final CountryService countryService;

    public CountryRestController(CountryService countryService) {
        this.countryService = countryService;
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export() {
        List<CountryResponseDto> items = countryService.initialDispAll();
        List<String> headers = List.of("ID", "コード", "国名");
        List<List<Object>> rows = items.stream()
                .map(item -> List.<Object>of(item.getId(), item.getCode(), item.getName()))
                .toList();
        return ExcelExportUtil.export("国マスタ", headers, rows, "countries.xlsx");
    }

    @PostMapping("/insert")
    public void insert(@RequestBody CountryForm form) {
        countryService.insertCountryInfo(form);
    }

    @PostMapping("/update")
    public ResponseEntity<String> update(@RequestBody CountryForm form) {
        try {
            countryService.updateCountryInfo(form);
            return ResponseEntity.ok("更新に成功しました");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("更新失敗");
        }

    }

    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public ResponseEntity<String> delete(@PathVariable Integer id) {
        try {
            countryService.deleteCountryInfoById(id);
            return ResponseEntity.ok("Deleted");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("削除に失敗しました");
        }
    }
}
