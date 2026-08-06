package org.example.web.stock.stockDetail.controller;

import org.example.web.stock.stockDetail.domain.ParameterDefaultUpdateForm;
import org.example.web.stock.stockDetail.service.StockDetailService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest_stock_detail")
public class StockDetailRestController {

    private final StockDetailService stockDetailService;

    public StockDetailRestController(StockDetailService stockDetailService) {
        this.stockDetailService = stockDetailService;
    }

    @PostMapping("/{code}/parameter-defaults")
    public ResponseEntity<String> updateParameterDefaults(@PathVariable String code,
            @RequestBody ParameterDefaultUpdateForm form) {
        try {
            stockDetailService.updateCompanyParameterDefaults(code, form);
            return ResponseEntity.ok("初期値を更新しました");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("初期値の更新に失敗しました");
        }
    }
}
