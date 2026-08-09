package org.example.web.stock.stockDetail.controller;

import org.example.web.stock.stockDetail.domain.StockAnalysisResponse;
import org.example.web.stock.stockDetail.domain.StockDetailForm;
import org.example.web.stock.stockDetail.service.StockDetailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.ModelAndView;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockDetailControllerTest {

    @Mock
    StockDetailService stockDetailService;

    @Test
    void display_ビュー名とanalysisData属性を設定すること() {
        StockDetailController controller = new StockDetailController(stockDetailService);
        StockAnalysisResponse analysisData = new StockAnalysisResponse();
        when(stockDetailService.getComprehensiveAnalysis("7203")).thenReturn(analysisData);

        ModelAndView mav = controller.display("7203", new StockDetailForm(), new ModelAndView());

        assertThat(mav.getViewName()).isEqualTo("stock-detail/stock-detail002");
        assertThat(mav.getModel().get("analysisData")).isEqualTo(analysisData);
    }
}
