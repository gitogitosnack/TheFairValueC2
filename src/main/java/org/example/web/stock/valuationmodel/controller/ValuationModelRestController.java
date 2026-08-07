package org.example.web.stock.valuationmodel.controller;

import org.example.web.common.excel.ExcelExportUtil;
import org.example.web.stock.valuationmodel.domain.ValuationModelForm;
import org.example.web.stock.valuationmodel.domain.ValuationModelResponseDto;
import org.example.web.stock.valuationmodel.service.ValuationModelService;
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
@RequestMapping("/rest_valuation_models")
public class ValuationModelRestController {

    private final ValuationModelService valuationModelService;

    public ValuationModelRestController(ValuationModelService valuationModelService) {
        this.valuationModelService = valuationModelService;
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export() {
        List<ValuationModelResponseDto> items = valuationModelService.initialDispAll();
        List<String> headers = List.of("ID", "モデル名", "算出方法の説明");
        List<List<Object>> rows = items.stream()
                .map(item -> List.<Object>of(item.getId(), item.getModelName(), item.getFormulaDescription()))
                .toList();
        return ExcelExportUtil.export("評価モデル", headers, rows, "valuation_models.xlsx");
    }

    @PostMapping("/insert")
    public void insert(@RequestBody ValuationModelForm form) {
        valuationModelService.insertValuationModelInfo(form);
    }

    @PostMapping("/update")
    public ResponseEntity<String> update(@RequestBody ValuationModelForm form) {
        try {
            valuationModelService.updateValuationModelInfo(form);
            return ResponseEntity.ok("更新に成功しました");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("更新失敗");
        }
    }

    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public ResponseEntity<String> delete(@PathVariable Integer id) {
        try {
            valuationModelService.deleteValuationModelInfoById(id);
            return ResponseEntity.ok("Deleted");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("削除に失敗しました");
        }
    }
}
