package org.example.web.stock.stockList.controller;

import org.example.web.stock.stockList.domain.StockListResponseDto;
import org.example.web.stock.stockList.service.StockListService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockListControllerTest {

    @Mock
    StockListService stockListService;

    @Test
    void display_ビュー名とstockList属性を設定すること() {
        StockListController controller = new StockListController(stockListService);
        List<StockListResponseDto> items = List.of(
                new StockListResponseDto(1, "7203", "トヨタ自動車", "東証プライム", "日本", "自動車", "JPY"));
        when(stockListService.initialDispAll()).thenReturn(items);

        ModelAndView mav = controller.display(new ModelAndView());

        assertThat(mav.getViewName()).isEqualTo("stock-list/stock-list");
        assertThat(mav.getModel().get("stockList")).isEqualTo(items);
    }
}
