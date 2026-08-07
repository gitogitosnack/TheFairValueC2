package org.example.web.stock.currency.controller;

import org.example.web.common.excel.ExcelExportUtil;
import org.example.web.stock.currency.domain.CurrencyForm;
import org.example.web.stock.currency.domain.CurrencyResponseDto;
import org.example.web.stock.currency.service.CurrencyService;
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
@RequestMapping("/rest_currencies")
public class CurrencyRestController {

    private final CurrencyService currencyService;

    public CurrencyRestController(CurrencyService currencyService) {
        this.currencyService = currencyService;
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export() {
        List<CurrencyResponseDto> items = currencyService.initialDispAll();
        List<String> headers = List.of("ID", "コード", "シンボル");
        List<List<Object>> rows = items.stream()
                .map(item -> List.<Object>of(item.getId(), item.getCode(), item.getSymbol()))
                .toList();
        return ExcelExportUtil.export("通貨マスタ", headers, rows, "currencies.xlsx");
    }

    @PostMapping("/insert")
    public void insert(@RequestBody CurrencyForm form) {
        currencyService.insertCurrencyInfo(form);
    }

    @PostMapping("/update")
    public ResponseEntity<String> update(@RequestBody CurrencyForm form) {
        try {
            currencyService.updateCurrencyInfo(form);
            return ResponseEntity.ok("更新に成功しました");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("更新失敗");
        }
    }

    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public ResponseEntity<String> delete(@PathVariable Integer id) {
        try {
            currencyService.deleteCurrencyInfoById(id);
            return ResponseEntity.ok("Deleted");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("削除に失敗しました");
        }
    }
}
