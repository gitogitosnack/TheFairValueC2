package org.example.web.stock.stockList.controller;

import org.example.web.stock.stockList.domain.StockListForm;
import org.example.web.stock.stockList.domain.StockListResponseDto;
import org.example.web.stock.stockList.service.StockListService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockListRestControllerTest {

    @Mock
    StockListService stockListService;

    StockListRestController controller;

    @Test
    void export_サービスの一覧からxlsxのレスポンスを生成すること() {
        controller = new StockListRestController(stockListService);
        when(stockListService.initialDispAll())
                .thenReturn(List.of(new StockListResponseDto(1, "7203", "トヨタ自動車", "東証プライム", "日本", "自動車", "JPY")));

        ResponseEntity<byte[]> response = controller.export();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentDisposition().getFilename()).isEqualTo("stock_list.xlsx");
        assertThat(response.getBody()).isNotEmpty();
    }

    @Test
    void insert_サービスのinsertStockInfoを呼び出すこと() {
        controller = new StockListRestController(stockListService);
        StockListForm form = new StockListForm();
        form.setCode("6758");

        controller.insert(form);

        verify(stockListService).insertStockInfo(form);
    }

    @Test
    void update_成功したとき200を返すこと() {
        controller = new StockListRestController(stockListService);
        StockListForm form = new StockListForm();
        form.setId(1);

        ResponseEntity<String> response = controller.update(form);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("更新に成功しました");
    }

    @Test
    void update_例外発生時に500を返すこと() {
        controller = new StockListRestController(stockListService);
        StockListForm form = new StockListForm();
        form.setId(999);
        doThrow(new RuntimeException("error")).when(stockListService).updateStockInfo(form);

        ResponseEntity<String> response = controller.update(form);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo("更新失敗");
    }

    @Test
    void delete_成功したとき200を返すこと() {
        controller = new StockListRestController(stockListService);

        ResponseEntity<String> response = controller.delete(1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Deleted");
    }

    @Test
    void delete_例外発生時に本文なしの500を返すこと() {
        controller = new StockListRestController(stockListService);
        doThrow(new RuntimeException("error")).when(stockListService).deleteStockInfoById(999);

        ResponseEntity<String> response = controller.delete(999);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNull();
    }
}
