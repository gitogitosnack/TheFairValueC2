package org.example.web.stock.stockList.controller;

import org.example.web.common.excel.ExcelExportUtil;
import org.example.web.stock.stockList.domain.StockListForm;
import org.example.web.stock.stockList.domain.StockListResponseDto;
import org.example.web.stock.stockList.service.StockListService;
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

@RestController // JSONを返すためのアノテーション
@RequestMapping("/rest_stock_list")
public class StockListRestController {

    // @Autowiredは、コンストラクタ型でDIしているため、書かなくてOK！
    // Lombokがない場合は、@RequiredArgsConstructorは使えない。
    // なので、コンストラクタもちゃんと書かなければならない。
    private final StockListService stockListService;

    public StockListRestController(StockListService stockListService) {
        this.stockListService = stockListService;
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export() {
        List<StockListResponseDto> items = stockListService.initialDispAll();
        List<String> headers = List.of("ID", "コード", "銘柄名", "市場", "国", "業種", "通貨");
        List<List<Object>> rows = items.stream()
                .map(item -> List.<Object>of(
                        item.getId()
                        ,item.getCode()
                        ,item.getName()
                        ,item.getMarket_name() == null ? "" : item.getMarket_name()
                        ,item.getCountry_name() == null ? "" : item.getCountry_name()
                        ,item.getIndustry_name() == null ? "" : item.getIndustry_name()
                        ,item.getCurrency_name() == null ? "" : item.getCurrency_name()
                ))
                .toList();
        return ExcelExportUtil.export("銘柄一覧", headers, rows, "stock_list.xlsx");
    }

    @PostMapping("/insert")
    public void insert(@RequestBody StockListForm form) {
        stockListService.insertStockInfo(form);
    }

    @PostMapping("/update")
    public ResponseEntity<String> update(@RequestBody StockListForm form) {
        try {
            stockListService.updateStockInfo(form);
            return ResponseEntity.ok("更新に成功しました");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("更新失敗");
        }
    }

    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public ResponseEntity<String> delete(@PathVariable Integer id) {
        // @PathVariableは、「URLのパスに含まれる変数を、Javaのメソッドの引数として受け取るための目印」
        try {
            stockListService.deleteStockInfoById(id); // 削除ロジック
            return ResponseEntity.ok("Deleted");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
